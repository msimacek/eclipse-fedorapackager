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
package org.fedoraproject.eclipse.packager.rpm.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.DownloadSourceCommand;
import org.fedoraproject.eclipse.packager.api.DownloadSourcesJob;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.IPreferenceHandler;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand;
import org.fedoraproject.eclipse.packager.rpm.api.SRPMBuildJob;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Handler for the creating an SRPM
 * 
 */
public class SRPMBuildHandler extends AbstractHandler implements
		IPreferenceHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Shell shell =  HandlerUtil.getActiveShellChecked(event);
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		try {
			IResource eventResource = FedoraHandlerUtils.getResource(event);
			final IProjectRoot projectRoot = FedoraPackagerUtils
					.getProjectRoot(eventResource);

			FedoraPackager fp = new FedoraPackager(projectRoot);
			final RpmBuildCommand srpmBuild;
			final DownloadSourceCommand download;
			try {
				// need to get sources for an SRPM build
				download = (DownloadSourceCommand) fp
						.getCommandInstance(DownloadSourceCommand.ID);
				// get RPM build command in order to produce an SRPM
				srpmBuild = (RpmBuildCommand) fp
						.getCommandInstance(RpmBuildCommand.ID);
			} catch (FedoraPackagerCommandNotFoundException e) {
				logger.logError(e.getMessage(), e);
				FedoraHandlerUtils.showErrorDialog(shell, projectRoot
						.getProductStrings().getProductName(), e.getMessage());
				return null;
			} catch (FedoraPackagerCommandInitializationException e) {
				logger.logError(e.getMessage(), e);
				FedoraHandlerUtils.showErrorDialog(shell, projectRoot
						.getProductStrings().getProductName(), e.getMessage());
				return null;
			}
			// Need to nest jobs into this job for it to show up properly in the
			// UI
			Job job = new Job(projectRoot.getProductStrings().getProductName()) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// Make sure we have sources locally
					final String downloadUrlPreference = getPreference();
					Job downloadSourcesJob = new DownloadSourcesJob(
							RpmText.SRPMBuildHandler_downloadSourcesForSRPMBuild,
							download, projectRoot, downloadUrlPreference, true);
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
					// Kick off the SRPM job
					SRPMBuildJob srpmBuildJob = new SRPMBuildJob(
							RpmText.SRPMBuildHandler_buildingSRPM, srpmBuild,
							projectRoot);
					srpmBuildJob.setUser(true);
					srpmBuildJob.schedule();
					try {
						// wait for job to finish
						srpmBuildJob.join();
					} catch (InterruptedException e1) {
						throw new OperationCanceledException();
					}
					return srpmBuildJob.getResult();
				}

			};
			job.setSystem(true); // avoid UI for this job
			job.schedule();
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
		}
		return null;
	}

	@Override
	public String getPreference() {
		return PackagerPlugin
				.getStringPreference(FedoraPackagerPreferencesConstants.PREF_LOOKASIDE_DOWNLOAD_URL);
	}

}
