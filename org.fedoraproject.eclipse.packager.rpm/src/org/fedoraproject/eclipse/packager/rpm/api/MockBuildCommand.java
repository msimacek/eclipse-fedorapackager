package org.fedoraproject.eclipse.packager.rpm.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.linuxtools.rpm.core.utils.Utils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraProjectRoot;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.errors.InvalidMockConfigurationException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.rpm.internal.core.ConsoleWriter;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.fedoraproject.eclipse.packager.utils.RPMUtils;

/**
 * Command for building a package in a chroot'ed environment
 * using {@code mock}.
 *
 */
public class MockBuildCommand extends FedoraPackagerCommand<MockBuildResult> {

	/**
	 *  The unique ID of this command.
	 */
	public static final String ID = "MockBuildCommand"; //$NON-NLS-1$
	
	private static final String MOCK_GROUP_NAME = "mock"; //$NON-NLS-1$
	private static final String MOCK_BINARY = "/usr/bin/mock"; //$NON-NLS-1$
	private static final String MOCK_CHROOT_CONFIG_OPTION = "-r"; //$NON-NLS-1$
	private static final String MOCK_REBUILD_OPTION = "--rebuild"; //$NON-NLS-1$
	private static final String MOCK_RESULT_DIR_OPTION = "--resultdir"; //$NON-NLS-1$
	
	private String localArchitecture; // set in initialize()
	private String mockConfig; // user may set this explicitly
	// path to SRPM which gets rebuild in the chrooted env.
	private String srpmAbsPath;
	private String resultDir;
	
	/**
	 * Set the mock config.
	 * 
	 * @param mockConfig
	 * @return This instance.
	 * @throws InvalidMockConfigurationException If the config was invalid.
	 */
	public MockBuildCommand mockConfig(String mockConfig) throws InvalidMockConfigurationException {
		if (!isSupportedMockConfig(mockConfig)) {
			throw new InvalidMockConfigurationException(NLS.bind(
					RpmText.MockBuildCommand_invalidMockConfigError, mockConfig));
		}
		this.mockConfig = mockConfig;
		return this;
	}

	/**
	 * Sets the path to the SRPM which should get rebuild using mock. File must
	 * exist.
	 * 
	 * @param absolutePath
	 *            The absolute path to the SRPM to be rebuilt.
	 * @return this instance.
	 * @throws FileNotFoundException
	 *             If provided path to the SRPM does not exist.
	 * @throws IllegalArgumentException
	 *             If the provided path was {@code null}.
	 */
	public MockBuildCommand pathToSRPM(String absolutePath)
			throws FileNotFoundException, IllegalArgumentException {
		if (absolutePath == null) {
			throw new IllegalArgumentException();
		}
		File srpmPath = new File(absolutePath);
		if (!srpmPath.exists()) {
			throw new FileNotFoundException(
					NLS.bind(RpmText.MockBuildCommand_srpmPathDoesNotExist,
							absolutePath));
		}
		this.srpmAbsPath = absolutePath;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand#
	 * checkConfiguration()
	 */
	@Override
	protected void checkConfiguration() throws CommandMisconfiguredException {
		// require path to SRPM to be set
		if (srpmAbsPath == null) {
			throw new CommandMisconfiguredException(
					RpmText.MockBuildCommand_srpmNullError);
		}
	}

	/**
	 * Implementation of the mock build command.
	 * 
	 * @throws CommandMisconfiguredException
	 *             If the command wasn't properly configured when called.
	 * @throws UserNotInMockGroupException
	 *             If the current user was not member of the system group
	 *             "mock".
	 * @throws CommandListenerException
	 *             If a command listener throwed an exception.
	 * @throws MockBuildCommandException
	 *             If some other error occurred.
	 * @throws MockNotInstalledException
	 *             If mock is not installed (i.e. /usr/sbin/mock not found).
	 */
	@Override
	public MockBuildResult call(IProgressMonitor monitor)
			throws CommandMisconfiguredException, UserNotInMockGroupException,
			CommandListenerException, MockBuildCommandException, MockNotInstalledException {
		try {
			callPreExecListeners();
		} catch (CommandListenerException e) {
			if (e.getCause() instanceof CommandMisconfiguredException) {
				// explicitly throw the specific exception
				throw (CommandMisconfiguredException)e.getCause();
			}
			throw e;
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		
		// Make sure mock config is set
		setMockConfig();
		// Set the result dir
		setResultDir();
		
		assert this.mockConfig != null && this.srpmAbsPath != null
				&& this.resultDir != null;
		
		monitor.subTask(NLS.bind(
				RpmText.MockBuildCommand_callMockBuildMsg, this.srpmAbsPath, this.mockConfig));
		
		// Make sure mock is installed
		if (!isMockInstalled()) {
			throw new MockNotInstalledException();
		}
		checkMockGroupMembership();
		
		String[] cmdList = buildMockCLICommand();
		MockBuildResult result = new MockBuildResult(cmdList, resultDir);
		// log the mock call
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		logger.logInfo(NLS.bind(RpmText.MockBuildCommand_mockCommandLog, convertMockCLICmd(cmdList)));
		InputStream is;
		try {
			is = Utils.runCommandToInputStream(cmdList);
		} catch (IOException e) {
			throw new MockBuildCommandException(e.getMessage(), e);
		}
		
		final MessageConsole console = FedoraPackagerConsole.getConsole();
		IConsoleManager manager = ConsolePlugin.getDefault()
				.getConsoleManager();
		manager.addConsoles(new IConsole[] { console });
		console.activate();

		final MessageConsoleStream outStream = console.newMessageStream();
		ConsoleWriter worker = new ConsoleWriter(is, outStream);
		Thread consoleWriterThread = new Thread(worker);
		
		consoleWriterThread.start();
		try {
			while (!monitor.isCanceled()) {
				try {
					// Don't waste system resources
					Thread.sleep(300);
					break;
				} catch (IllegalThreadStateException e) {
					// Do nothing
				}
			}

			if (monitor.isCanceled()) {
				worker.stop(); // Stop the worker thread
				throw new OperationCanceledException();
			}

			// finish reading whatever's left in the buffers
			consoleWriterThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// refresh containing folder
		try {
			this.projectRoot.getContainer().refreshLocal(IResource.DEPTH_INFINITE,
					monitor);
		} catch (CoreException ignored) {
			// ignore
		}
		
		callPostExecListeners();
		setCallable(false); // reuse of instance's call() not allowed
		monitor.done();
		result.setSuccess();
		return result;
	}
	
	/**
	 * Set the result directory as to where the build results/logs will be
	 * put.
	 */
	private void setResultDir() throws MockBuildCommandException {
		resultDir = new String();
		try {
			resultDir += RPMUtils.getNVR(projectRoot);
		} catch (IOException e) {
			throw new MockBuildCommandException(e.getMessage(), e);
		}
		resultDir += "-" + this.mockConfig; //$NON-NLS-1$
	}

	/**
	 * If mock config wasn't set by the user, get a default mock config and
	 * set it appropriately.
	 */
	private void setMockConfig() {
		if (this.mockConfig == null) {
			this.mockConfig = getDefaultMockcfg();
		}
	}

	/**
	 * Get a default mock config for the configured build architecture.
	 * 
	 * @param projectRoot
	 * @param buildarch
	 * @return
	 */
	private String getDefaultMockcfg() {
		assert this.mockConfig == null;
		IFpProjectBits projectBits =  FedoraPackagerUtils.getVcsHandler(projectRoot);
		String distvar = projectBits.getDistVariable(); 
		String distval = projectBits.getDistVal(); 
		String mockcfg = null;
		if (distvar.equals("rhel")) { //$NON-NLS-1$
			mockcfg = "epel-" + distval + "-" + this.localArchitecture; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			mockcfg = "fedora-" + distval + "-" + this.localArchitecture; //$NON-NLS-1$ //$NON-NLS-2$
			if (distval.equals("4") || distval.equals("5") //$NON-NLS-1$ //$NON-NLS-2$
					|| distval.equals("6")) { //$NON-NLS-1$
				mockcfg += "-core"; //$NON-NLS-1$
			}
			
			if (projectBits.getCurrentBranchName().equals("devel")) { //$NON-NLS-1$
				mockcfg = "fedora-devel-" + this.localArchitecture; //$NON-NLS-1$
			}
			
			if (projectBits.getCurrentBranchName().equals("devel")) { //$NON-NLS-1$
				if (!isSupportedMockConfig(mockcfg)) {
					// If the mockcfg as determined from above does not exist,
					// do something reasonable.
					mockcfg = "fedora-devel-" + this.localArchitecture;  //$NON-NLS-1$
				}
			}
		}
		return mockcfg;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand#initialize(org.fedoraproject.eclipse.packager.FedoraProjectRoot)
	 */
	@Override
	public void initialize(FedoraProjectRoot fp) throws FedoraPackagerCommandInitializationException {
		super.initialize(fp);
		// set the local architecture
		EvalResult archResult;
		try {
			FedoraPackager packager = new FedoraPackager(this.projectRoot);
			RpmEvalCommand eval = (RpmEvalCommand) packager.getCommandInstance(RpmEvalCommand.ID);
			archResult = eval.variable(RpmEvalCommand.ARCH).call(new NullProgressMonitor());
		} catch (FedoraPackagerAPIException e) {
			throw new FedoraPackagerCommandInitializationException(e.getMessage(), e);
		}
		this.localArchitecture = archResult.getEvalResult();
	}
	
	/**
	 * Determine if mock program is available
	 * 
	 * @return {@code true} if mock is available, {@code false} otherwise.
	 */
	private boolean isMockInstalled() {
		if (Utils.fileExist(MOCK_BINARY)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Determine if the given mock config is valid. I.e. a config file
	 * exists in /etc/mock
	 * 
	 * @param candidate
	 * @return {@code true} if the mock config exists on the local system for
	 *         the given string, {@code false} otherwise.
	 */
	private boolean isSupportedMockConfig(String candidate) {
		File file = new File("/etc/mock/" + candidate + ".cfg"); //$NON-NLS-1$ //$NON-NLS-2$
		if (file.exists()) {
			return true;
		}
		return false;
	}
	
	/**
	 * User needs to be member of the mock system group in order to be able to
	 * run a mock build.
	 * 
	 * @throws UserNotInMockGroupException
	 */
	private void checkMockGroupMembership() throws UserNotInMockGroupException, MockBuildCommandException {
		String grpCheckCmd[] = { "groups" }; //$NON-NLS-1$
		InputStream is = null;
		try {
			is = Utils.runCommandToInputStream(grpCheckCmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer groupsOutput = new StringBuffer();
			while ( (line = br.readLine()) != null ) {
				groupsOutput.append(line);
			}
			br.close();
			// groups command output should list the mock group
			String outputString = groupsOutput.toString();
			if (!outputString.contains(MOCK_GROUP_NAME)) {
				throw new UserNotInMockGroupException(NLS.bind(
						RpmText.MockBuildCommand_userNotInMockGroupMsg,
						outputString));
			}
		} catch (IOException e) {
			throw new MockBuildCommandException(e.getMessage(), e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * Builds the mock command line command. It either uses mock config as set
	 * by the user or as determined by {@link MockBuildCommand#getDefaultMockcfg()} if
	 * it was not set.
	 * 
	 * @return The complete mock CLI command.
	 */
	private String[] buildMockCLICommand() {
		assert srpmAbsPath != null && this.mockConfig != null;
		// if mockconfig was not set, get the default one for the local arch
		
		String resDirOpt = MOCK_RESULT_DIR_OPTION;
		resDirOpt += "="; //$NON-NLS-1$
		resDirOpt += this.resultDir;
		
		String[] mockCmd = { MOCK_BINARY, MOCK_CHROOT_CONFIG_OPTION, this.mockConfig,
				resDirOpt, MOCK_REBUILD_OPTION, srpmAbsPath };
		return mockCmd;
	}
	
	/**
	 * Convenience method to convert the command list into a String.
	 * 
	 * @param cmdList
	 * @return The command list in String format.
	 */
	private String convertMockCLICmd(String[] cmdList) {
		String cmd = new String();
		for (String token: cmdList) {
			cmd += token + " "; //$NON-NLS-1$
		}
		return cmd.trim();
	}
}
