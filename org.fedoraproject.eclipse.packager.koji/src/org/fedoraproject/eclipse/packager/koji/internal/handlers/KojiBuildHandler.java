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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerAbstractHandler;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;
import org.fedoraproject.eclipse.packager.koji.api.BuildResult;
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
	protected URL kojiWebUrl;
	protected IProjectRoot fedoraProjectRoot;
	protected String[] kojiInfo;
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		this.shell = getShell(event);
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		IResource eventResource = FedoraHandlerUtils.getResource(event);
		try {
			setProjectRoot(FedoraPackagerUtils
					.getProjectRoot(eventResource));
			fedoraProjectRoot = FedoraPackagerUtils
					.getProjectRoot(eventResource);
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
			return null;
		}
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
			kojiInfo = KojiPlugin.getDefault().getPreferenceStore()
					.getString(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO)
					.split(","); //$NON-NLS-1$
		} else {
			kojiInfo = kojiInfoString.split(","); //$NON-NLS-1$
		}

		if (kojiInfo[2].contentEquals("false") && projectPreferences.getBoolean(KojiPreferencesConstants.PREF_FORCE_CUSTOM_BUILD, false)) { //$NON-NLS-1$
			kojiInfo[2] = "true"; //$NON-NLS-1$
		}
		Job job = new KojiBuildJob(fedoraProjectRoot.getProductStrings()
				.getProductName(), getShell(event), fedoraProjectRoot,
				kojiInfo, isScratchBuild());
		job.addJobChangeListener(getJobChangeListener());
		job.setUser(true);
		job.schedule();
		return null; // must be null
	}

	protected boolean isScratchBuild() {
		return false;
	}

	/**
	 * Create a job listener for the event {@code done}.
	 * 
	 * @return The job change listener.
	 */
	protected IJobChangeListener getJobChangeListener() {
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		String webUrl = kojiInfo[0];
		try {
			kojiWebUrl = new URL(webUrl);
		} catch (MalformedURLException e) {
			// nothing critical, use default koji URL instead and log the bogus
			// Web url set in preferences.
			logger.logError(NLS.bind(
					KojiText.KojiBuildHandler_invalidKojiWebUrl,
					fedoraProjectRoot.getProductStrings().getBuildToolName(),
					webUrl), e);
			try {
				kojiWebUrl = new URL(
						FedoraPackagerPreferencesConstants.DEFAULT_KOJI_WEB_URL);
			} catch (MalformedURLException ignored) {
			}
			;
		}
		IJobChangeListener listener = new JobChangeAdapter() {

			// We are only interested in the done event
			@Override
			public void done(IJobChangeEvent event) {
				// get the BuildResult from the underlying job
				KojiBuildJob job = (KojiBuildJob) event.getJob();
				final BuildResult buildResult = job.getBuildResult();
				final IStatus jobStatus = event.getResult();
				PlatformUI.getWorkbench().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								// Only show response message dialog on success
								if (jobStatus.isOK() && buildResult != null
										&& buildResult.wasSuccessful()) {
									FedoraPackagerLogger logger = FedoraPackagerLogger
											.getInstance();
									// unconditionally log so that users get a
									// second chance to see the
									// koji-web URL
									logger.logInfo(NLS
											.bind(KojiText.KojiMessageDialog_buildResponseMsg,
													fedoraProjectRoot
															.getProductStrings()
															.getBuildToolName())
											+ " " //$NON-NLS-1$
											+ KojiUtils.constructTaskUrl(
													buildResult.getTaskId(),
													kojiWebUrl));
									// opens browser with URL to task ID
									openBrowser(buildResult.getTaskId(), kojiWebUrl);
								}
							}
						});
			}
		};
		return listener;
	}

	/** 
	 * @param taskId
	 *            The task ID to use for the URL.
	 * @param kojiWebUrl
	 *            The url to Koji Web without any parameters.
	 */
	protected void openBrowser(int taskId, URL kojiWebUrl) {
		try {
			final String url = KojiUtils.constructTaskUrl(taskId, kojiWebUrl);
			IWebBrowser browser = PlatformUI
					.getWorkbench()
					.getBrowserSupport()
					.createBrowser(
							IWorkbenchBrowserSupport.NAVIGATION_BAR
									| IWorkbenchBrowserSupport.LOCATION_BAR
									| IWorkbenchBrowserSupport.STATUS,
							"koji_task", null, null); //$NON-NLS-1$
			browser.openURL(new URL(url));
		} catch (PartInitException e) {
			FedoraPackagerLogger logger = FedoraPackagerLogger
					.getInstance();
			logger.logError(e.getMessage(), e);
		} catch (MalformedURLException e) {
			FedoraPackagerLogger logger = FedoraPackagerLogger
					.getInstance();
			logger.logError(e.getMessage(), e);
		}
	}
}
