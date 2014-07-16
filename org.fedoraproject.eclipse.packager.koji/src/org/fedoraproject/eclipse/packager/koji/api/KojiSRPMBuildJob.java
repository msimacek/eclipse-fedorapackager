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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
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
 * Job that uploads an SRPM to Koji and has Koji build an RPM from the SRPM.
 *
 */
public class KojiSRPMBuildJob extends KojiBuildJob {

	private IPath srpmPath;

	/**
	 * @param name
	 *            The name of the job.
	 * @param shell
	 *            The shell the job runs in.
	 * @param fedoraProjectRoot
	 *            The root of the project containing the SRPM being used.
	 * @param kojiInfo
	 *            The information for the server being used.
	 * @param srpmPath
	 *            Path of the SRPM locally
	 */
	public KojiSRPMBuildJob(String name, Shell shell,
			IProjectRoot fedoraProjectRoot, String[] kojiInfo, IPath srpmPath) {
		super(name, shell, fedoraProjectRoot, kojiInfo, true);
		this.srpmPath = srpmPath;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		FedoraPackager fp = new FedoraPackager(fedoraProjectRoot);
		final IFpProjectBits projectBits = FedoraPackagerUtils
				.getVcsHandler(fedoraProjectRoot);
		BranchConfigInstance bci = projectBits.getBranchConfig();
		KojiBuildCommand kojiBuildCmd;
		KojiUploadSRPMCommand uploadSRPMCommand;
		subMonitor.setTaskName(KojiText.KojiSRPMBuildJob_ConfiguringClient);
		try {
			uploadSRPMCommand = (KojiUploadSRPMCommand) fp
					.getCommandInstance(KojiUploadSRPMCommand.ID);
			kojiBuildCmd = (KojiBuildCommand) fp
					.getCommandInstance(KojiBuildCommand.ID);
		} catch (FedoraPackagerCommandNotFoundException|FedoraPackagerCommandInitializationException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell, fedoraProjectRoot
					.getProductStrings().getProductName(), e.getMessage());
			return null;
		}
		IKojiHubClient kojiClient;
		try {
			kojiClient = getHubClient();
			kojiClient.login();
			final Set<String> targetSet = new HashSet<>();
			for (HashMap<?, ?> targetInfo : kojiClient.listTargets()) {
				targetSet.add(targetInfo.get("name").toString()); //$NON-NLS-1$
			}
			if (!kojiInfo[2].contentEquals("true") || !targetSet.contains(bci.getBuildTarget())) { //$NON-NLS-1$
				kojiBuildCmd.buildTarget(bci.getBuildTarget());
				logger.logDebug(NLS.bind(KojiText.KojiSRPMBuildJob_logTarget,
						bci.getBuildTarget()));
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
				logger.logDebug(NLS.bind(KojiText.KojiSRPMBuildJob_logTarget,
						buildTarget));
			}
		} catch (KojiHubClientException e) {
			return e.getStatus();
		} catch (ExecutionException|InterruptedException|KojiHubClientLoginException e) {
			// This shouldn't happen, but report error anyway
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		}

		subMonitor.worked(5);
		subMonitor.setTaskName(KojiText.KojiSRPMBuildJob_UploadingSRPM);
		final String uploadPath = "cli-build/" + FedoraPackagerUtils.getUniqueIdentifier(); //$NON-NLS-1$
		try {
			uploadSRPMCommand.setKojiClient(kojiClient)
					.setRemotePath(uploadPath).setSRPM(srpmPath.toOSString())
					.call(subMonitor.newChild(80));
		} catch (CommandListenerException e) {
			// This shouldn't happen, but report error anyway
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (KojiHubClientException e) {
			// return some generic error
			String msg = NLS.bind(KojiText.KojiBuildHandler_unknownBuildError,
					e.getMessage());
			logger.logError(msg, e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, msg, e);
		} catch (KojiHubClientLoginException e) {
			e.printStackTrace();
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
		}
		subMonitor.worked(5);
		kojiBuildCmd.setKojiClient(kojiClient);
		List<String> sourceLocation = new ArrayList<>();
		sourceLocation.add(uploadPath + "/" + srpmPath.lastSegment()); //$NON-NLS-1$
		kojiBuildCmd.sourceLocation(sourceLocation);
		String nvr = RPMUtils.getNVR(fedoraProjectRoot, bci);
		kojiBuildCmd.nvr(new String[] { nvr }).isScratchBuild(true);
		logger.logDebug(NLS.bind(FedoraPackagerText.callingCommand,
				KojiBuildCommand.class.getName()));
		try {

			logger.logDebug(NLS.bind(FedoraPackagerText.callingCommand,
					KojiBuildCommand.class.getName()));

			// Call build command
			buildResult = kojiBuildCmd.call(subMonitor.newChild(10));
		} catch (BuildAlreadyExistsException|UnpushedChangesException e) {
			logger.logDebug(e.getMessage(), e);
			FedoraHandlerUtils.showInformationDialog(shell, fedoraProjectRoot
					.getProductStrings().getProductName(), e.getMessage());
			return Status.OK_STATUS;
		} catch (CommandListenerException e) {
			// This shouldn't happen, but report error anyway
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
}
