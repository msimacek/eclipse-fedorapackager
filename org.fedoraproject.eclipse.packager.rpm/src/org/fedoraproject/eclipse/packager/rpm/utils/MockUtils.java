package org.fedoraproject.eclipse.packager.rpm.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Observer;

import org.eclipse.linuxtools.rpm.core.utils.Utils;
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
	 * @throws MockBuildCommandException
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
	 * @param observers
	 *            Command observers.
	 * @param location
	 *            The location on the system on which to run the command.
	 * @return The return code of the process.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int runCommand(String[] command, Observer[] observers,
			File location) throws IOException, InterruptedException {
		ProcessBuilder pBuilder = new ProcessBuilder(command);
		pBuilder = pBuilder.redirectErrorStream(true);
		if (location != null){
			pBuilder.directory(location);
		}
		Process child;
		child = pBuilder.start();
		try {
			BufferedInputStream is = new BufferedInputStream(
					child.getInputStream());
			final MessageConsole console = FedoraPackagerConsole.getConsole();
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