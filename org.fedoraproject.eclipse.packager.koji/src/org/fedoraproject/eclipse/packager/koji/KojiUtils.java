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
package org.fedoraproject.eclipse.packager.koji;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.koji.api.BuildResult;
import org.fedoraproject.eclipse.packager.koji.api.KojiBuildJob;

/**
 * Helper dealing with task URLs.
 * 
 */
public class KojiUtils {

	/**
	 * Construct the correct URL to a task on koji.
	 * 
	 * @param taskId The id of the koji task
	 * @param kojiWebUrl The URL to koji.
	 * @return The URL as a string.
	 */
	public static String constructTaskUrl(int taskId, URL kojiWebUrl) {
		return kojiWebUrl.toString() + "/taskinfo?taskID=" + taskId; //$NON-NLS-1$
	}

	/**
	 * Load Koji server info from a preference store.
	 * 
	 * @param preferenceStore
	 *            The preference store in which to look for server info.
	 * @return Two-dimensional array with each the ith entry of entry 0 being a
	 *         server name and the ith entry of entry 1 being the corresponding
	 *         comma-delineated string listing server info
	 */
	public static String[][] loadServerInfo(IPreferenceStore preferenceStore) {
		String[] totalServerInfo = preferenceStore.getString(
				KojiPreferencesConstants.PREF_SERVER_LIST).split(";"); //$NON-NLS-1$
		String[][] serverMapping = new String[2][totalServerInfo.length];
		int i = 0;
		for (String serverInfoSet : totalServerInfo) {
			String[] serverInfo = serverInfoSet.split(",", 2); //$NON-NLS-1$
			serverMapping[0][i] = serverInfo[0];
			serverMapping[1][i] = serverInfo[1];
			i++;
		}
		return serverMapping;
	}

	/**
	 * Find the address of the currently selected server in the given server
	 * mapping
	 * 
	 * @param serverMapping
	 *            Server mapping of the form returned by loadServerInfo
	 * @param currentInfo
	 *            The comma-delineated string listing the server info.
	 * @return The address in the mapping to find the server that has the info
	 *         currentInfo.
	 */
	public static int getSelectionAddress(String[][] serverMapping,
			String currentInfo) {
		int selectionAddress = -1;
		for (int i = 0; i < serverMapping[1].length; i++) {
			if (serverMapping[1][i].contentEquals(currentInfo)) {
				selectionAddress = i;
				break;
			}
		}
		return selectionAddress;
	}

	/**
	 * Create a job listener for the event {@code done}.
	 * 
	 * @param kojiInfo
	 *            Comma-delineated string listing server info.
	 * @param projectRoot
	 *            Root to run listened job in.
	 * 
	 * @return The job change listener.
	 */
	public static IJobChangeListener getJobChangeListener(String[] kojiInfo,
			final IProjectRoot projectRoot) {
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		String webUrl = kojiInfo[0];
		URL kojiWebUrl = null;
		try {
			kojiWebUrl = new URL(webUrl);
		} catch (MalformedURLException e) {
			// nothing critical, use default koji URL instead and log the bogus
			// Web url set in preferences.
			logger.logError(NLS.bind(
					KojiText.KojiBuildHandler_invalidKojiWebUrl, projectRoot
							.getProductStrings().getBuildToolName(), webUrl), e);
			try {
				kojiWebUrl = new URL(
						FedoraPackagerPreferencesConstants.DEFAULT_KOJI_WEB_URL);
			} catch (MalformedURLException ignored) {
			}
		}
		final URL staticWebUrl = kojiWebUrl;
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
										&& buildResult.isSuccessful()) {
									FedoraPackagerLogger logger = FedoraPackagerLogger
											.getInstance();
									// unconditionally log so that users get a
									// second chance to see the
									// koji-web URL
									logger.logInfo(NLS
											.bind(KojiText.KojiMessageDialog_buildResponseMsg,
													projectRoot
															.getProductStrings()
															.getBuildToolName())
											+ " " //$NON-NLS-1$
											+ KojiUtils.constructTaskUrl(
													buildResult.getTaskId(),
													staticWebUrl));
									// opens browser with URL to task ID
									openBrowser(buildResult.getTaskId(),
											staticWebUrl);
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
	private static void openBrowser(int taskId, URL kojiWebUrl) {
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
			FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
			logger.logError(e.getMessage(), e);
		} catch (MalformedURLException e) {
			FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
			logger.logError(e.getMessage(), e);
		}
	}
}
