package org.fedoraproject.eclipse.packager.rpm.api;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.linuxtools.rpm.core.utils.Utils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.ide.IDE;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.errors.FedoraReviewNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.rpm.internal.core.ConsoleWriter;
import org.fedoraproject.eclipse.packager.rpm.internal.core.MockBuildStatusObserver;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * Wrapper command for Fedora Review tool.
 * 
 */
public class FedoraReviewCommand extends
		FedoraPackagerCommand<FedoraReviewResult> {

	/**
	 * The unique ID of this command.
	 */
	public static final String ID = "FedoraReviewCommand"; //$NON-NLS-1$
	private final String FEDORA_REVIEW_BINARY = "/usr/share/fedora-review"; //$NON-NLS-1$
	protected static final String MOCK_GROUP_NAME = "mock"; //$NON-NLS-1$

	@Override
	protected void checkConfiguration() throws CommandMisconfiguredException {
		// no prereqs
	}

	@Override
	public FedoraReviewResult call(IProgressMonitor monitor)
			throws CommandMisconfiguredException, UserNotInMockGroupException,
			CommandListenerException, MockBuildCommandException,
			FedoraReviewNotInstalledException {
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
		// Make sure mock is installed
		if (!isFedoraReviewInstalled()) {
			throw new FedoraReviewNotInstalledException();
		}
		IFile review = projectRoot.getProject().getFile(
				projectRoot.getPackageName()
						+ RpmText.FedoraReviewCommand_ReviewSuffix);
		try {
			review.delete(true, monitor);
		} catch (CoreException e1) {
			// ignore
		}
		String[] reviewCommand = new String[] {
				RpmText.FedoraReviewCommand_CommandName,
				RpmText.FedoraReviewCommand_NFlag, projectRoot.getPackageName() };
		FedoraReviewResult result = new FedoraReviewResult(reviewCommand);
		checkMockGroupMembership();
		ProcessBuilder pBuilder = new ProcessBuilder(reviewCommand);
		pBuilder = pBuilder.redirectErrorStream(true);
		pBuilder.directory(projectRoot.getProject().getLocation().toFile());
		Process child;
		try {
			child = pBuilder.start();
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
			worker.addObserver(new MockBuildStatusObserver(monitor));

			consoleWriterThread.start();
			try {
				consoleWriterThread.join();
				if (child.waitFor() != 0) {
					result.setFailure();
				}
			} catch (InterruptedException e) {
				child.destroy();
				result.setFailure();
			}
			projectRoot.getProject().refreshLocal(IResource.DEPTH_INFINITE,
					monitor);
			if (review.exists()) {
				final IFile constReview = review;
				final IWorkbenchPage page = PlatformUI.getWorkbench()
						.getWorkbenchWindows()[0].getActivePage();
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

					@Override
					public void run() {
						try {
							IDE.openEditor(page, constReview);
						} catch (PartInitException e) {
							// ignore failure
						}
					}

				});

			} else {
				result.setFailure();
			}
		} catch (CoreException e) {
			// ignore
		} catch (IOException e1) {
			FedoraHandlerUtils.showErrorDialog(new Shell(),
					RpmText.FedoraReviewCommand_IOErrorTitle,
					RpmText.FedoraReviewCommand_IOErrorText);
		}
		return result;
	}

	/**
	 * Determine if fedora-review program is available
	 * 
	 * @return {@code true} if mock is available, {@code false} otherwise.
	 */
	private boolean isFedoraReviewInstalled() {
		if (Utils.fileExist(FEDORA_REVIEW_BINARY)) {
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
	private void checkMockGroupMembership() throws UserNotInMockGroupException,
			MockBuildCommandException {
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
}
