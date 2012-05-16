/*******************************************************************************
 * Copyright (c) 2010-2012 Red Hat Inc. and others.
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerAbstractHandler;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;
import org.fedoraproject.eclipse.packager.koji.api.KojiBuildJob;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Handler to kick off a remote Koji build.
 * 
 */
public class KojiBuildHandler extends FedoraPackagerAbstractHandler {

	/**
	 * Shell for message dialogs, etc.
	 */
	protected Shell shell;
	protected String[] kojiInfo;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		this.shell = getShell(event);
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		IResource eventResource = FedoraHandlerUtils.getResource(event);
		try {
			final IProjectRoot projectRoot = FedoraPackagerUtils
					.getProjectRoot(eventResource);

			IEclipsePreferences projectPreferences = new ProjectScope(
					eventResource.getProject()).getNode(KojiPlugin.getDefault()
					.getBundle().getSymbolicName());
			String kojiInfoString = projectPreferences
					.get(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO,
							KojiPlugin
									.getDefault()
									.getPreferenceStore()
									.getString(
											KojiPreferencesConstants.PREF_KOJI_SERVER_INFO));
			if (kojiInfoString == KojiText.FedoraPackagerKojiPreferencePage_DefaultPlaceholder) {
				kojiInfo = KojiPlugin
						.getDefault()
						.getPreferenceStore()
						.getString(
								KojiPreferencesConstants.PREF_KOJI_SERVER_INFO)
						.split(","); //$NON-NLS-1$
			} else {
				kojiInfo = kojiInfoString.split(","); //$NON-NLS-1$
			}

			if (kojiInfo[2].contentEquals("false") && projectPreferences.getBoolean(KojiPreferencesConstants.PREF_FORCE_CUSTOM_BUILD, false)) { //$NON-NLS-1$
				kojiInfo[2] = "true"; //$NON-NLS-1$
			}
			Job job = new KojiBuildJob(projectRoot.getProductStrings()
					.getProductName(), getShell(event), projectRoot, kojiInfo,
					isScratchBuild());
			job.addJobChangeListener(KojiUtils.getJobChangeListener(kojiInfo,
					projectRoot));
			job.setUser(true);
			job.schedule();
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
			return null;
		}
		return null; // must be null
	}

	protected boolean isScratchBuild() {
		return false;
	}
}
