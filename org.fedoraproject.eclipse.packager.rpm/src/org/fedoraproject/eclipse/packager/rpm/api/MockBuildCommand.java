/*******************************************************************************
 * Copyright (c) 2010-2011 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.rpm.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Observer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.linuxtools.rpm.core.utils.Utils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.IProjectRoot;
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
import org.fedoraproject.eclipse.packager.rpm.internal.core.MockBuildCommandSuccessObserver;
import org.fedoraproject.eclipse.packager.rpm.internal.core.MockBuildStatusObserver;
import org.fedoraproject.eclipse.packager.rpm.utils.MockUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.RPMUtils;

/**
 * Command for building a package in a chroot'ed environment using {@code mock}.
 * 
 */
public class MockBuildCommand extends FedoraPackagerCommand<MockBuildResult> {

	/**
	 * The unique ID of this command.
	 */
	public static final String ID = "MockBuildCommand"; //$NON-NLS-1$

	protected static final String MOCK_BINARY = "/usr/bin/mock"; //$NON-NLS-1$
	protected static final String MOCK_CHROOT_CONFIG_OPTION = "-r"; //$NON-NLS-1$
	protected static final String MOCK_REBUILD_OPTION = "--rebuild"; //$NON-NLS-1$
	protected static final String MOCK_RESULT_DIR_OPTION = "--resultdir"; //$NON-NLS-1$
	protected static final String MOCK_NO_CLEANUP_AFTER_OPTION = "--no-cleanup-after"; //$NON-NLS-1$

	protected String localArchitecture; // set in initialize()
	protected String mockConfig; // user may set this explicitly
	// path to SRPM which gets rebuild in the chrooted env.
	private String srpmAbsPath;
	protected String resultDir;
	private BranchConfigInstance bci;

	/**
	 * Set the mock config.
	 * 
	 * @param mockConfig
	 *            The name of the configuration for this build.
	 * @return This instance.
	 * @throws InvalidMockConfigurationException
	 *             If the config was invalid.
	 */
	public MockBuildCommand mockConfig(String mockConfig)
			throws InvalidMockConfigurationException {
		if (!isSupportedMockConfig(mockConfig)) {
			throw new InvalidMockConfigurationException(
					NLS.bind(RpmText.MockBuildCommand_invalidMockConfigError,
							mockConfig));
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

	/**
	 * @param bci
	 *            The branch configuration for this command.
	 * @return This instance.
	 * @throws IllegalArgumentException
	 *             If the config was {@code null}.
	 */
	public MockBuildCommand branchConfig(BranchConfigInstance bci)
			throws IllegalArgumentException {
		if (bci == null) {
			throw new IllegalArgumentException(
					RpmText.MockBuildCommand_branchConfigNullError);
		}
		this.bci = bci;
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
		if (bci == null) {
			throw new CommandMisconfiguredException(
					RpmText.MockBuildCommand_branchConfigNullError);
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
	 *             If a command listener threw an exception.
	 * @throws MockBuildCommandException
	 *             If some other error occurred.
	 * @throws MockNotInstalledException
	 *             If mock is not installed (i.e. /usr/sbin/mock not found).
	 */
	@Override
	public MockBuildResult call(IProgressMonitor monitor)
			throws CommandMisconfiguredException, UserNotInMockGroupException,
			CommandListenerException, MockBuildCommandException,
			MockNotInstalledException {
		try {
			callPreExecListeners();
		} catch (CommandListenerException e) {
			if (e.getCause() instanceof CommandMisconfiguredException) {
				// explicitly throw the specific exception
				throw (CommandMisconfiguredException) e.getCause();
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

		monitor.subTask(NLS.bind(RpmText.MockBuildCommand_callMockBuildMsg,
				this.srpmAbsPath, this.mockConfig));

		// Make sure mock is installed
		if (!isMockInstalled()) {
			throw new MockNotInstalledException();
		}
		MockUtils.checkMockGroupMembership();

		String[] cmdList = buildMockCLICommand();

		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		MockBuildThread mbt = new MockBuildThread(cmdList, resultDir, monitor);
		mbt.start();
		while (mbt.getState() != Thread.State.TERMINATED) {
			if (monitor.isCanceled()) {
				mbt.interrupt();
				throw new OperationCanceledException();
			}
		}
		callPostExecListeners();
		setCallable(false); // reuse of instance's call() not allowed
		return mbt.getResult();
	}

	/**
	 * Set the result directory as to where the build results/logs will be put.
	 */
	private void setResultDir() {
		resultDir = new String();
		resultDir += projectRoot.getContainer().getLocation().toOSString();
		resultDir += IPath.SEPARATOR;
		resultDir += RPMUtils.getNVR(projectRoot, bci);
		resultDir += "-" + this.mockConfig; //$NON-NLS-1$
	}

	/**
	 * If mock config wasn't set by the user, get a default mock config and set
	 * it appropriately.
	 */
	private void setMockConfig() {
		if (this.mockConfig == null) {
			this.mockConfig = getDefaultMockcfg();
		}
	}

	/**
	 * Get a default mock config for the configured build architecture.
	 * 
	 * @return The configuration name.
	 */
	private String getDefaultMockcfg() {
		assert this.mockConfig == null;
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		logger.logDebug(RpmText.MockBuildCommand_usingDefaultMockConfig);
		String distvar = bci.getDistVariable();
		String distval = bci.getDistVal();
		String mockcfg = null;
		if (distvar.equals("rhel")) { //$NON-NLS-1$
			mockcfg = "epel-" + distval + "-" + this.localArchitecture; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			mockcfg = "fedora-" + distval + "-" + this.localArchitecture; //$NON-NLS-1$ //$NON-NLS-2$
			if (distval.equals("4") || distval.equals("5") //$NON-NLS-1$ //$NON-NLS-2$
					|| distval.equals("6")) { //$NON-NLS-1$
				mockcfg += "-core"; //$NON-NLS-1$
			}

			if (bci.getEquivalentBranch().equals("devel")) { //$NON-NLS-1$
				mockcfg = "fedora-devel-" + this.localArchitecture; //$NON-NLS-1$
			}

			if (bci.getEquivalentBranch().equals("devel")) { //$NON-NLS-1$
				if (!isSupportedMockConfig(mockcfg)) {
					// If the mockcfg as determined from above does not exist,
					// do something reasonable.
					mockcfg = "fedora-devel-" + this.localArchitecture; //$NON-NLS-1$
				}
			}
		}
		return mockcfg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand#initialize
	 * (org.fedoraproject.eclipse.packager.FedoraProjectRoot)
	 */
	@Override
	public void initialize(IProjectRoot fp)
			throws FedoraPackagerCommandInitializationException {
		super.initialize(fp);
		// set the local architecture
		EvalResult archResult;
		try {
			FedoraPackager packager = new FedoraPackager(this.projectRoot);
			RpmEvalCommand eval = (RpmEvalCommand) packager
					.getCommandInstance(RpmEvalCommand.ID);
			archResult = eval.variable(RpmEvalCommand.ARCH).call(
					new NullProgressMonitor());
		} catch (FedoraPackagerAPIException e) {
			throw new FedoraPackagerCommandInitializationException(
					e.getMessage(), e);
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
	 * Determine if the given mock config is valid. I.e. a config file exists in
	 * /etc/mock
	 * 
	 * @param candidate
	 *            The name of the candidate config.
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
	 * Builds the mock command line command. It either uses mock config as set
	 * by the user or as determined by
	 * {@link MockBuildCommand#getDefaultMockcfg()} if it was not set.
	 * 
	 * @return The complete mock CLI command.
	 */
	protected String[] buildMockCLICommand() {
		String resDirOpt = MOCK_RESULT_DIR_OPTION;
		resDirOpt += "="; //$NON-NLS-1$
		resDirOpt += this.resultDir;
		String[] mockCmd;
		// default non-SCM flags
		assert this.srpmAbsPath != null;
		mockCmd = new String[] { MOCK_BINARY, MOCK_CHROOT_CONFIG_OPTION,
				this.mockConfig, MOCK_NO_CLEANUP_AFTER_OPTION, resDirOpt,
				MOCK_REBUILD_OPTION, srpmAbsPath };
		return mockCmd;
	}

	private class MockBuildThread extends Thread {

		private String[] cmdList;
		private MockBuildResult result;
		private String resultDir;
		private IProgressMonitor monitor;

		public MockBuildThread(String[] cmdList, String resultDir,
				IProgressMonitor monitor) {
			super();
			this.cmdList = cmdList;
			this.resultDir = resultDir;
			this.monitor = monitor;
		}

		@Override
		public void run() {
			result = new MockBuildResult(cmdList, resultDir);
			// log the mock call
			FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
			logger.logDebug(NLS.bind(RpmText.MockBuildCommand_mockCommandLog,
					MockUtils.convertCLICmd(cmdList)));
			try {
				if (MockUtils.runCommand(cmdList, new Observer[] {
						new MockBuildStatusObserver(monitor),
						new MockBuildCommandSuccessObserver(result) }, null) == 0) {
					result.setSuccessful(true);
				} else {
					result.setSuccessful(false);
				}
			} catch (InterruptedException e) {
				result.setSuccessful(false);
			} catch (IOException e) {
				FedoraHandlerUtils.showErrorDialog(new Shell(),
						RpmText.RpmBuildCommand_BuildFailure,
						RpmText.RpmBuildCommand_BuildDidNotStart);
			}
		}

		public MockBuildResult getResult() {
			return result;
		}

	}
}
