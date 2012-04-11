package org.fedoraproject.eclipse.packager.rpm.internal.handlers.local;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerAbstractHandler;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.rpm.RPMPlugin;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.FedoraReviewCommand;
import org.fedoraproject.eclipse.packager.rpm.api.FedoraReviewResult;
import org.fedoraproject.eclipse.packager.rpm.api.errors.FedoraReviewNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Handler for using Fedora Review tool integration.
 * 
 */
public class FedoraReviewHandler extends FedoraPackagerAbstractHandler {

	private static IFile review;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell shell = getShell(event);
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		try {
			IResource eventResource = FedoraHandlerUtils.getResource(event);
			setProjectRoot(FedoraPackagerUtils.getProjectRoot(eventResource));
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
			return null;
		}
		Job job = new Job(RpmText.FedoraReviewHandler_TaskName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				FedoraReviewCommand reviewCommand;
				IProjectRoot fpr = getProjectRoot();
				FedoraPackager packager = new FedoraPackager(fpr);
				FedoraReviewResult result;
				try {
					monitor.beginTask(RpmText.FedoraReviewHandler_TaskName,
							IProgressMonitor.UNKNOWN);
					reviewCommand = (FedoraReviewCommand) packager
							.getCommandInstance(FedoraReviewCommand.ID);
					result = reviewCommand.call(monitor);
				} catch (FedoraPackagerCommandInitializationException e) {
					logger.logError(e.getMessage(), e);
					FedoraHandlerUtils.showErrorDialog(shell, fpr
							.getProductStrings().getProductName(), e
							.getMessage());
					return FedoraHandlerUtils.errorStatus(RPMPlugin.PLUGIN_ID,
							e.getMessage(), e);
				} catch (FedoraPackagerCommandNotFoundException e) {
					// nothing critical, advise the user what to do.
					logger.logDebug(e.getMessage());
					FedoraHandlerUtils.showInformationDialog(shell, fpr
							.getProductStrings().getProductName(), e
							.getMessage());
					IStatus status = new Status(IStatus.INFO,
							PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
					return status;
				} catch (CommandMisconfiguredException e) {
					// This shouldn't happen, but report error
					// anyway
					logger.logError(e.getMessage(), e);
					return FedoraHandlerUtils.errorStatus(RPMPlugin.PLUGIN_ID,
							e.getMessage(), e);
				} catch (UserNotInMockGroupException e) {
					// nothing critical, advise the user what to do.
					logger.logDebug(e.getMessage());
					FedoraHandlerUtils.showInformationDialog(shell, fpr
							.getProductStrings().getProductName(), e
							.getMessage());
					IStatus status = new Status(IStatus.INFO,
							PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
					return status;
				} catch (CommandListenerException e) {
					// There are no command listeners registered, so
					// shouldn't
					// happen. Do something reasonable anyway.
					logger.logError(e.getMessage(), e);
					return FedoraHandlerUtils.errorStatus(RPMPlugin.PLUGIN_ID,
							e.getMessage(), e);
				} catch (MockBuildCommandException e) {
					// Some unknown error occurred
					logger.logError(e.getMessage(), e.getCause());
					return FedoraHandlerUtils.errorStatus(RPMPlugin.PLUGIN_ID,
							e.getMessage(), e.getCause());
				} catch (FedoraReviewNotInstalledException e) {
					// nothing critical, advise the user what to do.
					logger.logDebug(e.getMessage());
					FedoraHandlerUtils.showInformationDialog(shell, fpr
							.getProductStrings().getProductName(), e
							.getMessage());
					IStatus status = new Status(IStatus.INFO,
							PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
					return status;
				} catch (OperationCanceledException e) {
					// command cancelled
					return Status.CANCEL_STATUS;
				}
				review = result.getReview();
				if (review != null && review.exists()) {
					return Status.OK_STATUS;
				} else {
					return new Status(IStatus.WARNING, RPMPlugin.PLUGIN_ID,
							RpmText.FedoraReviewHandler_NoReview);
				}
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				event.getResult();
				IStatus jobStatus = event.getResult();
				if (jobStatus.getSeverity() == IStatus.CANCEL) {
					// cancelled log this in any case
					logger.logInfo(RpmText.AbstractMockJob_mockCancelledMsg);
					FedoraHandlerUtils.showInformationDialog(shell,
							getProjectRoot().getProductStrings()
									.getProductName(),
							RpmText.AbstractMockJob_mockCancelledMsg);
					return;
				}
				if (jobStatus.getSeverity() == IStatus.OK) {
					final IFile constReview = review;
					final IWorkbenchPage page = PlatformUI.getWorkbench()
							.getWorkbenchWindows()[0].getActivePage();
					PlatformUI.getWorkbench().getDisplay()
							.syncExec(new Runnable() {

								@Override
								public void run() {
									try {
										IDE.openEditor(page, constReview);
									} catch (PartInitException e) {
										// ignore failure
									}
								}

							});
				}

			}
		});
		job.setUser(true);
		job.schedule();
		return null;
	}

}
