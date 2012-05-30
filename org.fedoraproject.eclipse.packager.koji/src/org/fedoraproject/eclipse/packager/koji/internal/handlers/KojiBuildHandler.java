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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.handlers.HandlerUtil;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;
import org.fedoraproject.eclipse.packager.koji.api.KojiBuildJob;

/**
 * Handler to kick off a remote Koji build.
 * 
 */
public class KojiBuildHandler extends KojiHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		setKojiInfo(event);
		final IProjectRoot projectRoot = getProjectRoot(event);
		if (projectRoot != null) {
			Job job = new KojiBuildJob(projectRoot.getProductStrings()
					.getProductName(),  HandlerUtil.getActiveShellChecked(event), projectRoot, kojiInfo,
					isScratchBuild());
			job.addJobChangeListener(KojiUtils.getJobChangeListener(kojiInfo,
					projectRoot));
			job.setUser(true);
			job.schedule();
		}
		return null; // must be null
	}

	protected boolean isScratchBuild() {
		return false;
	}
}
