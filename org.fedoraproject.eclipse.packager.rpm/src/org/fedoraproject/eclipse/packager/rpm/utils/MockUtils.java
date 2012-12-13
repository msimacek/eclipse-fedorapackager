package org.fedoraproject.eclipse.packager.rpm.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Observer;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.rpm.core.utils.Utils;
import org.eclipse.linuxtools.tools.launch.core.factory.RuntimeProcessFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.FedoraPackagerConsole;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.rpm.internal.core.ConsoleWriter;

/**
 * Utility class for Mock-related things.
 * 
 */
public class MockUtils {

	protected static final String MOCK_GROUP_NAME = "mock"; //$NON-NLS-1$

	/**
	 * User needs to be member of the mock system group in order to be able to
	 * run a mock build.
	 * 
	 * @throws UserNotInMockGroupException
	 *             If user is not in usergroup 'mock'.
	 * @throws MockBuildCommandException
	 *             If command fails for an unexpected reason.
	 */
	public static void checkMockGroupMembership()
			throws UserNotInMockGroupException, MockBuildCommandException {
		String grpCheckCmd[] = { "groups" }; //$NON-NLS-1$
		InputStream is = null;
		try {
			is = Utils.runCommandToInputStream(grpCheckCmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer groupsOutput = new StringBuffer();
			while ((line = br.readLine()) != null) {
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
	 * Convenience method to convert the command list into a String.
	 * 
	 * @param cmdList
	 *            The command and arguments as a list of Strings.
	 * @return The command list in String format.
	 */
	public static String convertCLICmd(String[] cmdList) {
		String cmd = new String();
		for (String token : cmdList) {
			cmd += token + " "; //$NON-NLS-1$
		}
		return cmd.trim();
	}

	/**
	 * Run a command on the system.
	 * 
	 * @param command
	 *            The command to be run.
	 * @param packageName The name of the package(SRPM).
	 * @param observers
	 *            Command observers.
	 * @param location
	 *            The location on the system on which to run the command.
	 * @return The return code of the process.
	 * @throws IOException
	 *             If the command running process cannot be built.
	 * @throws InterruptedException
	 *             If the command is interrupted when run.
	 * @throws CoreException
	 * 	           If the given file does not have a valid URI.
	 */
	public static int runCommand(String[] command, String packageName, Observer[] observers,
			File location) throws IOException, InterruptedException, CoreException {
		
		IFileStore fileStore = null;
		
		if (location != null)
			fileStore = EFS.getStore(location.toURI());

		Process child = RuntimeProcessFactory.getFactory().exec(command, null, fileStore, null);

		try {
			BufferedInputStream is = new BufferedInputStream(
					child.getInputStream());
			final MessageConsole console = FedoraPackagerConsole.getConsole(packageName);
			IConsoleManager manager = ConsolePlugin.getDefault()
					.getConsoleManager();
			manager.addConsoles(new IConsole[] { console });
			console.activate();

			final MessageConsoleStream outStream = console.newMessageStream();
			ConsoleWriter worker = new ConsoleWriter(is, outStream);
			Thread consoleWriterThread = new Thread(worker);

			// Observe what is printed on the console and update status in
			// prog monitor.
			for (Observer observer : observers) {
				worker.addObserver(observer);
			}
			consoleWriterThread.start();
			consoleWriterThread.join();
		} catch (InterruptedException e) {
			child.destroy();
			throw e;
		}
		return child.waitFor();
	}
}
