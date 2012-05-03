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
import org.eclipse.core.runtime.jobs.Job;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;
import org.fedoraproject.eclipse.packager.koji.api.KojiBuildJob;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Handler to kick off a remote Koji build.
 * 
 */
public class KojiBuildHandler extends KojiHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		IResource eventResource = FedoraHandlerUtils.getResource(event);
		try {
			final IProjectRoot projectRoot = FedoraPackagerUtils
					.getProjectRoot(eventResource);

			setKojiInfo(event);

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
