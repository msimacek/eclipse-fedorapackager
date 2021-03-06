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
package org.fedoraproject.eclipse.packager.internal.handlers;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.SourcesFile;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.IPreferenceHandler;
import org.fedoraproject.eclipse.packager.api.SourcesFileUpdater;
import org.fedoraproject.eclipse.packager.api.UploadSourceCommand;
import org.fedoraproject.eclipse.packager.api.UploadSourceResult;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.api.errors.FileAvailableInLookasideCacheException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidUploadFileException;
import org.fedoraproject.eclipse.packager.api.errors.UploadFailedException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Class responsible for uploading source files.
 *
 * @see UploadSourceCommand
 * @see SourcesFileUpdater
 */
public class UploadHandler extends AbstractHandler implements
		IPreferenceHandler {

	/**
	 * Performs upload of sources (independent of VCS used), updates "sources"
	 * file and performs necessary CVS operations to bring branch in sync.
	 * Checks if sources have changed.
	 *
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final boolean  shouldReplaceSources = Boolean.valueOf(event
		        .getParameter("shouldReplaceSources")); //$NON-NLS-1$
		final Shell shell =  HandlerUtil.getActiveShellChecked(event);
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		final IResource resource = FedoraHandlerUtils.getResource(event);
		try {
			final IProjectRoot projectRoot = FedoraPackagerUtils
					.getProjectRoot(resource);

			FedoraPackager packager = new FedoraPackager(projectRoot);
			final UploadSourceCommand uploadCmd;
			try {
				// Get DownloadSourceCommand from Fedora packager registry
				uploadCmd = (UploadSourceCommand) packager
						.getCommandInstance(UploadSourceCommand.ID);
			} catch (FedoraPackagerAPIException e) {
				logger.logError(e.getMessage(), e);
				FedoraHandlerUtils.showErrorDialog(shell, projectRoot
						.getProductStrings().getProductName(), e.getMessage());
				return null;
			}
			final IFpProjectBits projectBits = FedoraPackagerUtils
					.getVcsHandler(projectRoot);
			// Do the uploading
			Job job = new Job(FedoraPackagerText.UploadHandler_taskName) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {

					monitor.beginTask(
							FedoraPackagerText.UploadHandler_taskName, 1);

					File newUploadFile = resource.getLocation().toFile();
					SourcesFile sourceFile = projectRoot.getSourcesFile();
					if (sourceFile.getSources().containsKey(resource.getName())) {
						String checksum = SourcesFile
								.calculateChecksum(newUploadFile);
						if (checksum.equals(sourceFile.getSources().get(
								resource.getName()))) {
							// Candidate file already in sources and up-to-date
							FedoraHandlerUtils
									.showInformationDialog(
											shell,
											projectRoot.getProductStrings()
													.getProductName(),
											NLS.bind(
													FedoraPackagerText.UploadHandler_versionOfFileExistsAndUpToDate,
													resource.getName()));
							return Status.OK_STATUS;
						}
					}

					SourcesFileUpdater sourcesUpdater = new SourcesFileUpdater(
							projectRoot, newUploadFile);
					sourcesUpdater.setShouldReplace(shouldReplaceSources);
					// Note that ignore file may not exist, yet
					projectBits.ignoreResource(resource);

					UploadSourceResult result = null;
					try {
						String uploadUrl = getPreference();
						if (uploadUrl != null) {
							// "http://upload-cgi/cgi-bin/upload.cgi"
							uploadCmd.setUploadURL(uploadUrl);
						}
						uploadCmd.setFileToUpload(newUploadFile);
						// Set the SSL policy. We have different policies for
						// Fedora and
						// RHEL. This should be kept in placed as it is
						// overridden in the Red Hat version.
						setSSLPolicy(uploadCmd);
						uploadCmd.addCommandListener(sourcesUpdater);
						try {
							result = uploadCmd.call(new SubProgressMonitor(
									monitor, 1));
						} catch (FileAvailableInLookasideCacheException e) {
							// File already in lookaside cache. This means we do
							// not
							// need to upload, but we should still update
							// sources files
							// and vcs ignore files as required.
							sourcesUpdater.postExecution();
							// report that there was no upload required.
							FedoraHandlerUtils.showInformationDialog(shell,
									projectRoot.getProductStrings()
											.getProductName(), e.getMessage());
							return Status.OK_STATUS;
						}
					} catch (CommandListenerException e) {
						// Something else failed
						logger.logError(e.getMessage(), e);
						return new Status(IStatus.ERROR,
								PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
					} catch (UploadFailedException e) {
						// Check if cert has expired, give some more
						// meaningful error in that case
						if (e.isCertificateExpired()) {
							String msg = NLS
									.bind(FedoraPackagerText.UploadHandler_expiredCertificateError,
											projectRoot.getProductStrings()
													.getDistributionName());
							logger.logError(msg, e);
							return new Status(IStatus.ERROR,
									PackagerPlugin.PLUGIN_ID, msg, e);
						}
						// Check if cert has been revoked, give some more
						// meaningful error in that case
						if (e.isCertificateRevoked()) {
							String msg = NLS
									.bind(FedoraPackagerText.UploadHandler_revokedCertificateError,
											projectRoot.getProductStrings()
													.getDistributionName());
							logger.logError(msg, e);
							return new Status(IStatus.ERROR,
									PackagerPlugin.PLUGIN_ID, msg, e);
						}
						// something else failed
						logger.logError(e.getMessage(), e);
						return new Status(IStatus.ERROR,
								PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
					} catch (InvalidUploadFileException e) {
						FedoraHandlerUtils.showInformationDialog(shell,
								projectRoot.getProductStrings()
										.getProductName(), e.getMessage());
						return Status.OK_STATUS;
					} catch (MalformedURLException e) {
						// Upload URL was invalid, something is wrong with
						// preferences.
						String message = NLS
								.bind(FedoraPackagerText.UploadHandler_invalidUrlError,
										e.getMessage());
						FedoraHandlerUtils.showInformationDialog(shell,
								projectRoot.getProductStrings()
										.getProductName(), message);
						return Status.OK_STATUS;
					}

					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}

					// result may be null if upload file was already in
					// lookaside
					// cache.
					if (result != null && !result.isSuccessful()) {
						// probably a 404 or some such
						String message = result.getErrorString();
						return new Status(IStatus.ERROR,
								PackagerPlugin.PLUGIN_ID, message);
					}

					// Do VCS update
					IStatus res = projectBits.updateVCS(monitor);
					if (res.isOK()) {
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
					}

					// Refresh project
					IProject project = projectRoot.getProject();
					if (project != null) {
						try {
							project.refreshLocal(IResource.DEPTH_INFINITE,
									monitor);
						} catch (CoreException e) {
							logger.logError(FedoraPackagerText.FedoraProjectRoot_failedToRefreshResource, e);
						}
					}

					return res;
				}

			};
			job.setUser(true);
			job.schedule();
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
		}
		return null; // must be null
	}

	/**
	 * Sets the SSL policy for this handler.
	 */
	protected void setSSLPolicy(UploadSourceCommand uploadCmd) {
		// enable SLL authentication
		uploadCmd.setFedoraSSLEnabled(true);
	}

	@Override
	public String getPreference() {
		return PackagerPlugin
				.getStringPreference(FedoraPackagerPreferencesConstants.PREF_LOOKASIDE_UPLOAD_URL);
	}
}
