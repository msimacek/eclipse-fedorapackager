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
package org.fedoraproject.eclipse.packager.koji.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.TagSourcesListener;
import org.fedoraproject.eclipse.packager.api.UnpushedChangesListener;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.api.errors.BuildAlreadyExistsException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.fedoraproject.eclipse.packager.koji.internal.ui.KojiTargetDialog;
import org.fedoraproject.eclipse.packager.api.errors.TagSourcesException;
import org.fedoraproject.eclipse.packager.api.errors.UnpushedChangesException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.fedoraproject.eclipse.packager.utils.RPMUtils;

/**
 * Job to make a Koji Build.
 * 
 */
public class KojiBuildJob extends Job {

	private IProjectRoot fedoraProjectRoot;
	private boolean isScratch;
	private Shell shell;
	protected BuildResult buildResult;
	protected String[] kojiInfo;

	/**
	 * @param name
	 *            The name of the job.
	 * @param shell
	 *            The shell the job is run in.
	 * @param fpr
	 *            The root of the project being built.
	 * @param kojiInfo
	 *            The information for the server being used.
	 * @param scratch
	 *            True if scratch, false otherwise.
	 */
	public KojiBuildJob(String name, Shell shell, IProjectRoot fpr,
			String[] kojiInfo, boolean scratch) {
		super(name);
		fedoraProjectRoot = fpr;
		isScratch = scratch;
		this.shell = shell;
		this.kojiInfo = kojiInfo;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IFpProjectBits projectBits = FedoraPackagerUtils
				.getVcsHandler(fedoraProjectRoot);
		BranchConfigInstance bci = projectBits.getBranchConfig();
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		FedoraPackager fp = new FedoraPackager(fedoraProjectRoot);
		KojiBuildCommand kojiBuildCmd;
		try {
			kojiBuildCmd = (KojiBuildCommand) fp
					.getCommandInstance(KojiBuildCommand.ID);
		} catch (FedoraPackagerCommandNotFoundException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell, fedoraProjectRoot
					.getProductStrings().getProductName(), e.getMessage());
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					e.getMessage());
		} catch (FedoraPackagerCommandInitializationException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell, fedoraProjectRoot
					.getProductStrings().getProductName(), e.getMessage());
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					e.getMessage());
		}
		monitor.beginTask(NLS.bind(KojiText.KojiBuildHandler_pushBuildToKoji,
				fedoraProjectRoot.getProductStrings().getBuildToolName()), 100);
		monitor.worked(5);
		UnpushedChangesListener unpushedChangesListener = new UnpushedChangesListener(
				fedoraProjectRoot, monitor);
		// check for unpushed changes prior calling command
		kojiBuildCmd.addCommandListener(unpushedChangesListener);
		// tag sources if user wishes; TagSourcesListener takes care of this
		TagSourcesListener tagSources = new TagSourcesListener(
				fedoraProjectRoot, monitor, shell, bci);
		kojiBuildCmd.addCommandListener(tagSources);
		IKojiHubClient kojiClient;
		try {
			kojiClient = getHubClient();
		} catch (MalformedURLException e) {
			logger.logError(NLS.bind(
					KojiText.KojiBuildHandler_invalidHubUrl,
					fedoraProjectRoot.getProductStrings().getBuildToolName()), e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					NLS.bind(KojiText.KojiBuildHandler_invalidHubUrl,
							fedoraProjectRoot.getProductStrings().getBuildToolName()),
							e);
		}
		kojiBuildCmd.setKojiClient(kojiClient);
		List<String> sourceLocation = new ArrayList<String>();
		sourceLocation.add(projectBits
				.getScmUrlForKoji(bci));
		kojiBuildCmd.sourceLocation(sourceLocation);
		String nvr;
		try {
			nvr = RPMUtils.getNVR(fedoraProjectRoot, bci);
		} catch (IOException e) {
			logger.logError(KojiText.KojiBuildHandler_errorGettingNVR,
					e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID, NLS
					.bind(KojiText.KojiBuildHandler_invalidHubUrl,
							fedoraProjectRoot.getProductStrings()
									.getBuildToolName()), e);
		}
		kojiBuildCmd.nvr(new String[]{nvr})
				.isScratchBuild(isScratch);
		logger.logDebug(NLS.bind(FedoraPackagerText.callingCommand,
				KojiBuildCommand.class.getName()));
		try {
			// login
			kojiClient.login();
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			if (!kojiInfo[2].contentEquals("true")) { //$NON-NLS-1$
				kojiBuildCmd.buildTarget(bci.getBuildTarget());
			} else {
				final Set<String> targetSet = new HashSet<String>();
				for (HashMap<?, ?> targetInfo : kojiClient.listTargets()) {
					targetSet.add(targetInfo.get("name").toString()); //$NON-NLS-1$
				}

				FutureTask<String> tagTask = new FutureTask<String>(
						new Callable<String>() {

							@Override
							public String call() {
								return new KojiTargetDialog(shell, targetSet)
										.openForTarget();
							}

						});
				Display.getDefault().syncExec(tagTask);
				String buildTarget = null;
				buildTarget = tagTask.get();
				if (buildTarget == null) {
					throw new OperationCanceledException();
				}
				kojiBuildCmd.buildTarget(buildTarget);
			}
			logger.logDebug(NLS.bind(FedoraPackagerText.callingCommand,
					KojiBuildCommand.class.getName()));

			// Call build command.
			// Make sure to set the buildResult variable, since it is used
			// by getBuildResult() which is in turn called from the handler
			buildResult = kojiBuildCmd.call(monitor);
		} catch (CommandMisconfiguredException e) {
			// This shouldn't happen, but report error anyway
			logger.logError(e.getMessage(), e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (BuildAlreadyExistsException e) {
			// log in any case
			logger.logInfo(e.getMessage());
			FedoraHandlerUtils.showInformationDialog(shell, fedoraProjectRoot
					.getProductStrings().getProductName(), e.getMessage());
			return Status.OK_STATUS;
		} catch (UnpushedChangesException e) {
			logger.logDebug(e.getMessage(), e);
			FedoraHandlerUtils.showInformationDialog(shell, fedoraProjectRoot
					.getProductStrings().getProductName(), e.getMessage());
			return Status.OK_STATUS;
		} catch (TagSourcesException e) {
			// something failed while tagging sources
			logger.logError(e.getMessage(), e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (CommandListenerException e) {
			// This shouldn't happen, but report error anyway
			logger.logError(e.getMessage(), e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (ExecutionException e) {
			// This shouldn't happen, but report error anyway
			logger.logError(e.getMessage(), e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (InterruptedException e) {
			// This shouldn't happen, but report error anyway
			logger.logError(e.getMessage(), e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (KojiHubClientLoginException e) {
			e.printStackTrace();
			// Check if certs were missing
			if (e.isCertificateMissing()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_missingCertificatesMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
				logger.logError(msg, e);
				return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
						msg, e);
			}
			if (e.isCertificateExpired()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_certificateExpriredMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
				logger.logError(msg, e);
				return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
						msg, e);
			}
			if (e.isCertificateRevoked()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_certificateRevokedMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
				logger.logError(msg, e);
				return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
						msg, e);
			}
			// return some generic error
			logger.logError(e.getMessage(), e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (KojiHubClientException e) {
			// return some generic error
			String msg = NLS.bind(KojiText.KojiBuildHandler_unknownBuildError,
					e.getMessage());
			logger.logError(msg, e);
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID, msg, e);
		}
		// success
		return Status.OK_STATUS;
	}

	/**
	 * Create a hub client based on set preferences.
	 * 
	 * @throws MalformedURLException
	 *             If the koji hub URL preference was invalid.
	 * @return The koji client.
	 */
	protected IKojiHubClient getHubClient() throws MalformedURLException {
		return new KojiSSLHubClient(kojiInfo[1]);
	}

	/**
	 * Get the underlying result of the call to {@link KojiBuildCommand}. This
	 * method should only be called after the job has been executed.
	 * 
	 * @return The result of the build.
	 */
	public BuildResult getBuildResult() {
		return this.buildResult;
	}

}
