package org.fedoraproject.eclipse.packager.rpm.api;

import java.io.IOException;
import java.util.Observer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.linuxtools.rpm.core.utils.Utils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.errors.FedoraReviewNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.rpm.internal.core.MockBuildStatusObserver;
import org.fedoraproject.eclipse.packager.rpm.utils.MockUtils;
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
				projectRoot.getPackageName() + "-review.txt"); //$NON-NLS-1$
		try {
			review.delete(true, new NullProgressMonitor());
		} catch (CoreException e1) {
			// ignore
		}
		String[] reviewCommand = new String[] { "fedora-review", //$NON-NLS-1$
				"-n", projectRoot.getPackageName() }; //$NON-NLS-1$
		FedoraPackagerLogger.getInstance().logDebug(
				NLS.bind(RpmText.FedoraReviewCommand_CommandLog,
						MockUtils.convertCLICmd(reviewCommand)));
		FedoraReviewResult result = new FedoraReviewResult(reviewCommand);
		MockUtils.checkMockGroupMembership();
		try {
			MockUtils.runCommand(reviewCommand,
					new Observer[] { new MockBuildStatusObserver(monitor) },
					projectRoot.getProject().getLocation().toFile());
			result.setReview(review);
		} catch (IOException e1) {
			FedoraHandlerUtils.showErrorDialog(new Shell(),
					RpmText.FedoraReviewCommand_IOErrorTitle,
					RpmText.FedoraReviewCommand_IOErrorText);
		} catch (InterruptedException e) {
			result.setFailure();
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
}
