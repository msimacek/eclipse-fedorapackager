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
package org.fedoraproject.eclipse.packager.copr.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.fedoraproject.copr.client.CoprConfiguration;
import org.fedoraproject.copr.client.CoprService;
import org.fedoraproject.copr.client.CoprSession;
import org.fedoraproject.copr.client.ProjectId;
import org.fedoraproject.copr.client.impl.DefaultCoprService;
import org.fedoraproject.eclipse.packager.copr.CoprConfigurationConstants;
import org.fedoraproject.eclipse.packager.copr.CoprPlugin;
import org.fedoraproject.eclipse.packager.copr.CoprText;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * Base class for Copr plugin command handlers
 *
 * @author msimacek
 *
 */
public abstract class CoprHandler extends AbstractHandler {

	protected Shell shell;
	protected CoprConfiguration coprConfiguration;
	protected IEclipsePreferences projectPrefs;
	protected IPreferenceStore globalPrefs;
	protected CoprService coprService;
	protected CoprSession coprSession;
	protected ProjectId coprProject;

	protected void prepareService(ExecutionEvent event)
			throws ExecutionException {
		this.shell = HandlerUtil.getActiveShellChecked(event);
		IResource eventResource = FedoraHandlerUtils.getResource(event);
		projectPrefs = new ProjectScope(eventResource.getProject())
				.getNode(CoprPlugin.PLUGIN_ID);
		globalPrefs = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				CoprPlugin.PLUGIN_ID);
		coprConfiguration = new CoprConfiguration();
		coprService = new DefaultCoprService();
		String coprUrl = globalPrefs
				.getString(CoprConfigurationConstants.COPR_URL);
		if ("".equals(coprUrl)) { //$NON-NLS-1$
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					CoprText.CoprHandler_NoURL);
		}
		coprConfiguration.setUrl(coprUrl);
	}

	protected void prepareSession(ExecutionEvent event)
			throws ExecutionException {
		prepareService(event);
		String coprLogin = globalPrefs
				.getString(CoprConfigurationConstants.COPR_API_LOGIN);
		String coprToken = globalPrefs
				.getString(CoprConfigurationConstants.COPR_API_TOKEN);
		if ("".equals(coprLogin) || "".equals(coprToken)) { //$NON-NLS-1$ //$NON-NLS-2$
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					NLS.bind(CoprText.CoprHandler_NoLoginToken,
							coprConfiguration.getUrl()));

		}
		coprConfiguration.setLogin(coprLogin);
		coprConfiguration.setToken(coprToken);
		coprSession = coprService.newSession(coprConfiguration);
	}

	protected void prepareProject(ExecutionEvent event)
			throws ExecutionException {
		prepareSession(event);
		String projectUsername = projectPrefs.get(
				CoprConfigurationConstants.COPR_USERNAME, ""); //$NON-NLS-1$
		if ("".equals(projectUsername)) { //$NON-NLS-1$
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					CoprText.CoprHandler_NoUsername);
		}
		String projectName = projectPrefs
				.get(CoprConfigurationConstants.COPR_NAME, ""); //$NON-NLS-1$
		if ("".equals(projectName)) { //$NON-NLS-1$
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					CoprText.CoprHandler_NoName);
		}
		coprProject = new ProjectId();
		coprProject.setUserName(projectUsername);
		coprProject.setProjectName(projectName);
	}
}
