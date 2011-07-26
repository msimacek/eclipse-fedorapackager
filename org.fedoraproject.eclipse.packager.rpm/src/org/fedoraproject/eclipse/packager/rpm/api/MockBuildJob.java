package org.fedoraproject.eclipse.packager.rpm.api;

import java.io.FileNotFoundException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.rpm.RPMPlugin;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * Job for doing a standard Mock build.
 *
 */
public class MockBuildJob extends Job {

	private Shell shell;
	private final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
	private IProjectRoot fpr;
	private MockBuildResult result;
	private MockBuildCommand mockBuild;
	private IPath srpmPath;
	/** 
	 * @param name The name of the job.
	 * @param shell The shell the job is run in.
	 * @param fpRoot The root of the Fedora project being built.
	 * @param srpmPath The path to the built SRPM/
	 */
	public MockBuildJob(String name, Shell shell, IProjectRoot fpRoot, IPath srpmPath) {
		super(name);
		this.shell = shell;
		fpr = fpRoot;
		this.srpmPath = srpmPath;
	}
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		FedoraPackager fp = new FedoraPackager(fpr);
		try {
			mockBuild = (MockBuildCommand) fp
				.getCommandInstance(MockBuildCommand.ID);
		} catch (FedoraPackagerCommandNotFoundException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell,
					fpr.getProductStrings().getProductName(), e.getMessage());
			return FedoraHandlerUtils
			.errorStatus(
					RPMPlugin.PLUGIN_ID,
					e.getMessage(),
					e);
		} catch (FedoraPackagerCommandInitializationException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell,
					fpr.getProductStrings().getProductName(), e.getMessage());
			return FedoraHandlerUtils
			.errorStatus(
					RPMPlugin.PLUGIN_ID,
					e.getMessage(),
					e);
		}
		logger.logDebug(NLS.bind(
				FedoraPackagerText.callingCommand,
				MockBuildCommand.class.getName()));
		try {
			mockBuild.pathToSRPM(srpmPath.toOSString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// catch error when creating the SRPM failed.
			logger.logError(
					RpmText.MockBuildHandler_srpmBuildFailed,
					e);
			return FedoraHandlerUtils
					.errorStatus(
							RPMPlugin.PLUGIN_ID,
							RpmText.MockBuildHandler_srpmBuildFailed,
							e);
		}
		Job mockJob = new Job(fpr.getProductStrings().getProductName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(
							RpmText.MockBuildHandler_testLocalBuildWithMock,
							IProgressMonitor.UNKNOWN);
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					try {
						result = mockBuild.call(monitor);
					} catch (CommandMisconfiguredException e) {
						// This shouldn't happen, but report error
						// anyway
						logger.logError(e.getMessage(), e);
						return FedoraHandlerUtils.errorStatus(
								RPMPlugin.PLUGIN_ID, e.getMessage(), e);
					} catch (UserNotInMockGroupException e) {
						// nothing critical, advise the user what to do.
						logger.logDebug(e.getMessage());
						FedoraHandlerUtils
						.showInformationDialog(shell,
								fpr.getProductStrings().getProductName(), e
								.getMessage());
						return Status.OK_STATUS;
					} catch (CommandListenerException e) {
						// There are no command listeners registered, so
						// shouldn't
						// happen. Do something reasonable anyway.
						logger.logError(e.getMessage(), e);
						return FedoraHandlerUtils.errorStatus(
								RPMPlugin.PLUGIN_ID, e.getMessage(), e);
					} catch (MockBuildCommandException e) {
						// Some unknown error occurred
						logger.logError(e.getMessage(), e.getCause());
						return FedoraHandlerUtils.errorStatus(
								RPMPlugin.PLUGIN_ID, e.getMessage(),
								e.getCause());
					} catch (MockNotInstalledException e) {
						// nothing critical, advise the user what to do.
						logger.logDebug(e.getMessage());
						FedoraHandlerUtils
						.showInformationDialog(shell,
								fpr.getProductStrings().getProductName(), e
								.getMessage());
						return Status.OK_STATUS;
					}
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		mockJob.addJobChangeListener(getMockJobFinishedJobListener());
		mockJob.setUser(true);
		mockJob.schedule();
		try {
			// wait for job to finish
			mockJob.join();
		} catch (InterruptedException e1) {
			throw new OperationCanceledException();
		}
		return mockJob.getResult();
	}
	/**
	 * 
	 * @return A job listener for the {@code done} event.
	 */
	protected IJobChangeListener getMockJobFinishedJobListener() {
		IJobChangeListener listener = new JobChangeAdapter() {

			// We are only interested in the done event
			@Override
			public void done(IJobChangeEvent event) {
				FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
				if (result.wasSuccessful()) {
					// TODO: Make this a link to the directory
					String msg = NLS.bind(
							RpmText.MockBuildHandler_mockSucceededMsg,
							result.getResultDirectoryPath());
					logger.logDebug(msg);
					FedoraHandlerUtils.showInformationDialog(
							shell,
							fpr.getProductStrings().getProductName(), msg);
				} else {
					String msg = NLS.bind(
							RpmText.MockBuildHandler_mockFailedMsg,
							result.getResultDirectoryPath());
					logger.logDebug(msg);
					FedoraHandlerUtils.showInformationDialog(
							shell,
							fpr.getProductStrings().getProductName(),
							msg);
				}
			}
		};
		return listener;
	}
}