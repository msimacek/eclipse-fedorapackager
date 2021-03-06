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
package org.fedoraproject.eclipse.packager.api;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.SourcesUpToDateException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * Job for downloading sources.
 */
public class DownloadSourcesJob extends Job {

	private DownloadSourceCommand download;
	private IProjectRoot fedoraProjectRoot;
	private FedoraPackagerLogger logger;
	private Shell shell;
	private boolean suppressSourcesUpToDateInfo = false;
	private String downloadUrlPreference = null;

	/**
	 * @param jobName
	 *            The name of this job.
	 * @param download
	 *            The download command to use
	 * @param downloadUrlPreference
	 *            A preference to use as download URL or {@code null}.
	 * @param fedoraProjectRoot
	 *            The project root associated with this job.
	 */
	public DownloadSourcesJob(String jobName, DownloadSourceCommand download,
			IProjectRoot fedoraProjectRoot, String downloadUrlPreference) {
		super(jobName);
		this.download = download;
		this.fedoraProjectRoot = fedoraProjectRoot;
		this.logger = FedoraPackagerLogger.getInstance();
		this.downloadUrlPreference = downloadUrlPreference;
	}

	/**
	 * @param jobName
	 *            The name of this job.
	 * @param download
	 *            The download command to use
	 * @param fedoraProjectRoot
	 *            The project root associated with this job.
	 * @param downloadUrlPreference
	 *            A preference to use as download URL or {@code null}.
	 * @param suppressSourcesUpToDateInfo
	 *            Indicating if information message dialog reporting sources are
	 *            up-to-date should be suppressed.
	 */
	public DownloadSourcesJob(String jobName, DownloadSourceCommand download,
			IProjectRoot fedoraProjectRoot, String downloadUrlPreference,
			boolean suppressSourcesUpToDateInfo) {
		super(jobName);
		this.download = download;
		this.fedoraProjectRoot = fedoraProjectRoot;
		this.logger = FedoraPackagerLogger.getInstance();
		this.suppressSourcesUpToDateInfo = suppressSourcesUpToDateInfo;
		this.downloadUrlPreference = downloadUrlPreference;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(
				FedoraPackagerText.DownloadHandler_downloadSourceTask,
				fedoraProjectRoot.getSourcesFile().getMissingSources().size());
		ChecksumValidListener md5sumListener = new ChecksumValidListener(
				fedoraProjectRoot);
		download.addCommandListener(md5sumListener); // want md5sum checking
		try {
			if (downloadUrlPreference != null) {
				// Only set URL explicitly if set in preferences. Lookaside
				// cache falls back to the default URL if not set.
				download.setDownloadURL(downloadUrlPreference);
			}
			return download.call(monitor);
		} catch (final SourcesUpToDateException e) {
			if (!suppressSourcesUpToDateInfo) {
				FedoraHandlerUtils.showInformationDialog(shell,
						fedoraProjectRoot.getProductStrings().getProductName(),
						e.getMessage());
			}
			return Status.OK_STATUS;
		} catch (MalformedURLException|CommandListenerException e) {
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, PackagerPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} finally {
			monitor.done();
		}
	}

}
