/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.rpm.api;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.LinkedMessageDialog;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Superclass for mock jobs, which share the same job listener.
 * 
 */
public abstract class AbstractMockJob extends Job {

	protected MockBuildResult result;
	protected Shell shell;
	protected IProjectRoot fpr;
	protected BranchConfigInstance bci;

	/**
	 * @param name
	 *            The name of this job.
	 * @param shell
	 *            The shell the job is run in.
	 * @param fedoraProjectRoot
	 *            The root the job is run under.
	 */
	public AbstractMockJob(String name, Shell shell,
			IProjectRoot fedoraProjectRoot) {
		super(name);
		this.shell = shell;
		this.fpr = fedoraProjectRoot;
		this.bci = FedoraPackagerUtils.getVcsHandler(fedoraProjectRoot)
				.getBranchConfig();
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
				FedoraPackagerLogger logger = FedoraPackagerLogger
						.getInstance();
				IStatus jobStatus = event.getResult();
				if (jobStatus.getSeverity() == IStatus.CANCEL) {
					// cancelled log this in any case
					logger.logInfo(RpmText.AbstractMockJob_mockCancelledMsg);
					FedoraHandlerUtils.showInformationDialog(shell, fpr
							.getProductStrings().getProductName(),
							RpmText.AbstractMockJob_mockCancelledMsg);
					return;
				}
				// Handle NPE case of the result when user is not in mock group
				// of when mock is not installed. Just return in that case,
				// since
				// The job will show appropriate messages to the user.
				if (jobStatus.getSeverity() == IStatus.INFO
						&& jobStatus.getException() != null
						&& (jobStatus.getException() instanceof UserNotInMockGroupException || jobStatus
								.getException() instanceof MockNotInstalledException)) {
					return;
				}
				if (result.isSuccessful()) {
					showMessageDialog(NLS.bind(
							RpmText.AbstractMockJob_mockSucceededMsgHTML,
							result.getResultDirectoryPath().getFullPath()
									.toOSString()));
				} else {
					showMessageDialog(NLS.bind(
							RpmText.AbstractMockJob_mockFailedMsgHTML, result
									.getResultDirectoryPath().getFullPath()
									.toOSString()));
				}
			}
		};
		return listener;
	}

	/**
	 * Helper method for showing the custom message dialog with a link to the
	 * build result directory.
	 * 
	 * @param htmlMsg
	 *            The pseudohtml String to be converted into a message displayed
	 *            to the user.
	 */
	private void showMessageDialog(String htmlMsg) {
		final LinkedMessageDialog messageDialog = new LinkedMessageDialog(
				shell, fpr.getProductStrings().getProductName(), htmlMsg,
				result.getResultDirectoryPath());
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				messageDialog.open();
			}
		});
	}
}
