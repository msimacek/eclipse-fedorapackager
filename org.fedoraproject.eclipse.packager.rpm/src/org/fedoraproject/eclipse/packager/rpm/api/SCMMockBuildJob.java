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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.DownloadSourceCommand;
import org.fedoraproject.eclipse.packager.api.DownloadSourcesJob;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.rpm.RPMPlugin;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Job that configures and calls SCMMockBuildCommand.
 *
 */
public class SCMMockBuildJob extends AbstractMockJob {

	private boolean useRepoSource = false;
	private final FedoraPackagerLogger logger = FedoraPackagerLogger
			.getInstance();
	private SCMMockBuildCommand mockBuild;
	private DownloadSourceCommand download;
	private String downloadUrlPreference;

	/**
	 * Constructor for a job that defaults to using source downloaded separately
	 *
	 * @param name
	 *            The name of the Job
	 * @param shell
	 *            The shell the Job is in
	 * @param fpRoot
	 *            The root of the project the Job is run in
	 * @param downloadUrlPreference
	 *            The preference for the download URL.
	 */
	public SCMMockBuildJob(String name, Shell shell, IProjectRoot fpRoot,
			String downloadUrlPreference) {
		super(name, shell, fpRoot);
		this.downloadUrlPreference = downloadUrlPreference;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		FedoraPackager fp = new FedoraPackager(fpr);
		try {
			download = (DownloadSourceCommand) fp
					.getCommandInstance(DownloadSourceCommand.ID);
			mockBuild = (SCMMockBuildCommand) fp
					.getCommandInstance(SCMMockBuildCommand.ID);
		} catch (FedoraPackagerAPIException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell, fpr.getProductStrings()
					.getProductName(), e.getMessage());
			return new Status(IStatus.ERROR, RPMPlugin.PLUGIN_ID,
					e.getMessage(), e);
		}
		// sources need to be downloaded
		if (!useRepoSource) {
			Job downloadSourcesJob = new DownloadSourcesJob(
					RpmText.MockBuildHandler_downloadSourcesForMockBuild,
					download, fpr, downloadUrlPreference, true);
			downloadSourcesJob.setUser(true);
			downloadSourcesJob.schedule();
			try {
				// wait for download job to finish
				downloadSourcesJob.join();
			} catch (InterruptedException e1) {
				throw new OperationCanceledException();
			}
			if (!downloadSourcesJob.getResult().isOK()) {
				// bail if something failed
				return downloadSourcesJob.getResult();
			}
			mockBuild.useDownloadedSourceDirectory(download
					.getDownloadFolderPath());
			mockBuild.useSpec(fpr.getSpecFile().getName());
		}
		mockBuild.branchConfig(bci);
		// set repo type
		mockBuild.useRepoPath(fpr.getContainer().getParent()
				.getRawLocation().toString());
		
		mockBuild.usePackage(fpr.getPackageName());
		mockBuild.useBranch(FedoraPackagerUtils.getVcsHandler(fpr)
				.getRawCurrentBranchName());

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
						fpr.getProject().refreshLocal(IResource.DEPTH_INFINITE,
								monitor);
					} catch (UserNotInMockGroupException e) {
						// nothing critical, advise the user what to do.
						FedoraHandlerUtils.showInformationDialog(shell, fpr
								.getProductStrings().getProductName(), e
								.getMessage());
						IStatus status = new Status(IStatus.INFO,
								PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
						return status;
					} catch (MockBuildCommandException | CoreException
							| CommandListenerException e) {
						// Some unknown or unexpected error occurred
						logger.logError(e.getMessage(), e.getCause());
						return new Status(IStatus.ERROR, RPMPlugin.PLUGIN_ID,
								e.getMessage(), e.getCause());
					} catch (MockNotInstalledException e) {
						// nothing critical, advise the user what to do.
						FedoraHandlerUtils.showInformationDialog(shell, fpr
								.getProductStrings().getProductName(), e
								.getMessage());
						IStatus status = new Status(IStatus.INFO,
								PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
						return status;
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
}
