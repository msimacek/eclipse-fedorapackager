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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.UnpushedChangesListener;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.api.errors.UnpushedChangesException;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.api.errors.BuildAlreadyExistsException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.fedoraproject.eclipse.packager.koji.internal.ui.KojiTargetDialog;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.fedoraproject.eclipse.packager.utils.RPMUtils;

/**
 * Job to make a Koji Build.
 *
 */
public class KojiBuildJob extends KojiJob {

	private boolean isScratch;
	protected BuildResult buildResult;

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
		super(name, shell, kojiInfo, fpr);
		isScratch = scratch;
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
		} catch (FedoraPackagerAPIException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell, fedoraProjectRoot
					.getProductStrings().getProductName(), e.getMessage());
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage());
		}
		monitor.beginTask(NLS.bind(KojiText.KojiBuildHandler_pushBuildToKoji,
				fedoraProjectRoot.getProductStrings().getBuildToolName()), 100);
		monitor.worked(5);
		UnpushedChangesListener unpushedChangesListener = new UnpushedChangesListener(
				fedoraProjectRoot, monitor);
		// check for unpushed changes prior calling command
		kojiBuildCmd.addCommandListener(unpushedChangesListener);
		IKojiHubClient kojiClient;
		try {
			kojiClient = getHubClient();
		} catch (KojiHubClientException e) {
			return e.getStatus();
		}
		kojiBuildCmd.setKojiClient(kojiClient);
		List<String> sourceLocation = new ArrayList<>();
		sourceLocation.add(projectBits.getScmUrlForKoji(bci));
		kojiBuildCmd.sourceLocation(sourceLocation);
		String nvr = RPMUtils.getNVR(fedoraProjectRoot, bci);
		kojiBuildCmd.nvr(new String[] { nvr }).isScratchBuild(isScratch);
		try {
			// login
			kojiClient.login();
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			final Set<String> targetSet = new HashSet<>();
			for (HashMap<?, ?> targetInfo : kojiClient.listTargets()) {
				targetSet.add(targetInfo.get("name").toString()); //$NON-NLS-1$
			}
			if (!kojiInfo[2].contentEquals("true") || !targetSet.contains(bci.getBuildTarget())) { //$NON-NLS-1$
				kojiBuildCmd.buildTarget(bci.getBuildTarget());
			} else {
				FutureTask<String> targetTask = new FutureTask<>(
						new Callable<String>() {

							@Override
							public String call() {
								return new KojiTargetDialog(shell, targetSet)
										.openForTarget();
							}

						});
				Display.getDefault().syncExec(targetTask);
				String buildTarget = null;
				buildTarget = targetTask.get();
				if (buildTarget == null) {
					throw new OperationCanceledException();
				}
				kojiBuildCmd.buildTarget(buildTarget);
			}
			// Call build command.
			// Make sure to set the buildResult variable, since it is used
			// by getBuildResult() which is in turn called from the handler
			buildResult = kojiBuildCmd.call(monitor);
		} catch (BuildAlreadyExistsException|UnpushedChangesException e) {
			// log in any case
			logger.logInfo(e.getMessage());
			FedoraHandlerUtils.showInformationDialog(shell, fedoraProjectRoot
					.getProductStrings().getProductName(), e.getMessage());
			return Status.OK_STATUS;
		} catch (CommandListenerException|ExecutionException|InterruptedException e) {
			// This shouldn't happen, but report error anyway
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (KojiHubClientLoginException e) {
			// Check if certs were missing
			if (e.isCertificateMissing()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_missingCertificatesMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
				logger.logError(msg, e);
				return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, msg, e);
			}
			if (e.isCertificateExpired()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_certificateExpriredMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
				logger.logError(msg, e);
				return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, msg, e);
			}
			if (e.isCertificateRevoked()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_certificateRevokedMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
				logger.logError(msg, e);
				return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, msg, e);
			}
			// return some generic error
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (KojiHubClientException e) {
			// return some generic error
			String msg = NLS.bind(KojiText.KojiBuildHandler_unknownBuildError,
					e.getMessage());
			logger.logError(msg, e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, msg, e);
		}
		// success
		return Status.OK_STATUS;
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
