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
package org.fedoraproject.eclipse.packager.koji.internal.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FileDialogRunable;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;
import org.fedoraproject.eclipse.packager.koji.api.KojiSRPMBuildJob;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Class that handles KojiBuildCommand in conjunction with KojiUploadSRPMCommand
 * 
 */
public class KojiSRPMScratchBuildHandler extends KojiBuildHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		this.shell = getShell(event);
		IResource eventResource = FedoraHandlerUtils.getResource(event);
		try {
			final IProjectRoot projectRoot = FedoraPackagerUtils
					.getProjectRoot(eventResource);

			IPath srpmPath = null;
			if (eventResource instanceof IFile
					&& eventResource.getName().endsWith(".src.rpm")) { //$NON-NLS-1$
				srpmPath = eventResource.getLocation();
			}
			if (srpmPath == null) {
				try {
					srpmPath = FedoraHandlerUtils.chooseRootFileOfType(shell,
							projectRoot, ".src.rpm", //$NON-NLS-1$
							KojiText.KojiSRPMBuildJob_ChooseSRPM);
				} catch (OperationCanceledException e) {
					return null;
				} catch (CoreException e) {
					logger.logError(e.getMessage(), e);
					return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
							e.getMessage(), e);
				}
			}
			if (srpmPath == null) {
				FileDialogRunable fdr = new FileDialogRunable(
						"*.src.rpm", //$NON-NLS-1$
						KojiText.KojiSRPMScratchBuildHandler_UploadFileDialogTitle);
				shell.getDisplay().syncExec(fdr);
				String srpm = fdr.getFile();
				if (srpm == null) {
					return Status.CANCEL_STATUS;
				}
				srpmPath = new Path(srpm);
			}
			kojiInfo = new ProjectScope(eventResource.getProject())
					.getNode(
							KojiPlugin.getDefault().getBundle()
									.getSymbolicName())
					.get(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO,
							KojiPlugin
									.getDefault()
									.getPreferenceStore()
									.getString(
											KojiPreferencesConstants.PREF_KOJI_SERVER_INFO))
					.split(","); //$NON-NLS-1$
			Job job = new KojiSRPMBuildJob(projectRoot.getProductStrings()
					.getProductName(), getShell(event), projectRoot, kojiInfo,
					srpmPath);
			job.addJobChangeListener(KojiUtils.getJobChangeListener(kojiInfo,
					projectRoot));
			job.setUser(true);
			job.schedule();
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
		}
		return null; // must be null
	}

}
