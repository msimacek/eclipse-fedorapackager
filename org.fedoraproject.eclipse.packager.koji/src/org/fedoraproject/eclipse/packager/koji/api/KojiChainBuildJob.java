package org.fedoraproject.eclipse.packager.koji.api;

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
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.UnpushedChangesListener;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.api.errors.TagSourcesException;
import org.fedoraproject.eclipse.packager.api.errors.UnpushedChangesException;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.api.errors.BuildAlreadyExistsException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.fedoraproject.eclipse.packager.koji.internal.ui.KojiTargetDialog;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.RPMUtils;

/**
 * Koji build job for chain builds.
 *
 */
public class KojiChainBuildJob extends KojiBuildJob {

	private IProjectRoot hostRoot;
	private Shell shell;
	private List<List<String>> sourceLocations;
	private IProjectRoot[] projectRoots;
	private final BranchConfigInstance RAWHIDECONFIG = new BranchConfigInstance(
			".fc18", "18", "fedora", "rawhide", "master"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	/**
	 * @param name
	 *            The name of the job.
	 * @param shell
	 *            The shell the job is run in.
	 * @param fprs
	 *            The chain build format list of lists of source locations.
	 * @param kojiInfo
	 *            The information for the server being used.
	 * @param sourceLocations
	 *            A list of lists of package source urls such that packages in
	 *            the same list in parallel and sublists are built in the order
	 *            they appear in the main list.
	 */
	public KojiChainBuildJob(String name, Shell shell, IProjectRoot[] fprs,
			String[] kojiInfo, List<List<String>> sourceLocations) {
		super(name, shell, fprs[0], kojiInfo, true);
		hostRoot = fprs[0];
		this.shell = shell;
		this.sourceLocations = sourceLocations;
		projectRoots = fprs;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		BranchConfigInstance bci = RAWHIDECONFIG;
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		FedoraPackager fp = new FedoraPackager(hostRoot);
		KojiBuildCommand kojiBuildCmd;
		try {
			kojiBuildCmd = (KojiBuildCommand) fp
					.getCommandInstance(KojiBuildCommand.ID);
		} catch (FedoraPackagerAPIException e) {
			logger.logError(e.getMessage(), e);
			FedoraHandlerUtils.showErrorDialog(shell, hostRoot
					.getProductStrings().getProductName(), e.getMessage());
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage());
		}
		monitor.beginTask(NLS.bind(KojiText.KojiBuildHandler_pushBuildToKoji,
				hostRoot.getProductStrings().getBuildToolName()), 100);
		monitor.worked(5);
		UnpushedChangesListener unpushedChangesListener = new UnpushedChangesListener(
				hostRoot, monitor);
		// check for unpushed changes prior calling command
		kojiBuildCmd.addCommandListener(unpushedChangesListener);
		IKojiHubClient kojiClient;
		try {
			kojiClient = getHubClient();
		} catch (KojiHubClientException e) {
			return e.getStatus();
		}

		kojiBuildCmd.setKojiClient(kojiClient);
		kojiBuildCmd.sourceLocation(sourceLocations);
		String[] nvr = new String[projectRoots.length];
		for (int i = 0; i < projectRoots.length; i++) {
			nvr[i] = RPMUtils.getNVR(projectRoots[i], bci);
		}
		kojiBuildCmd.nvr(nvr).isScratchBuild(false);
		logger.logDebug(NLS.bind(FedoraPackagerText.callingCommand,
				KojiBuildCommand.class.getName()));
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
			logger.logDebug(NLS.bind(FedoraPackagerText.callingCommand,
					KojiBuildCommand.class.getName()));

			// Call build command.
			// Make sure to set the buildResult variable, since it is used
			// by getBuildResult() which is in turn called from the handler
			buildResult = kojiBuildCmd.call(monitor);
		} catch (BuildAlreadyExistsException|UnpushedChangesException e) {
			// log in any case
			logger.logInfo(e.getMessage());
			FedoraHandlerUtils.showInformationDialog(shell, hostRoot
					.getProductStrings().getProductName(), e.getMessage());
			return Status.OK_STATUS;
		} catch (TagSourcesException e) {
			// something failed while tagging sources
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (CommandMisconfiguredException|CommandListenerException|ExecutionException|InterruptedException e) {
			// This shouldn't happen, but report error anyway
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (KojiHubClientLoginException e) {
			// Check if certs were missing
			if (e.isCertificateMissing()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_missingCertificatesMsg,
						hostRoot.getProductStrings().getDistributionName());
				logger.logError(msg, e);
				return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, msg, e);
			}
			if (e.isCertificateExpired()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_certificateExpriredMsg,
						hostRoot.getProductStrings().getDistributionName());
				logger.logError(msg, e);
				return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, msg, e);
			}
			if (e.isCertificateRevoked()) {
				String msg = NLS.bind(
						KojiText.KojiBuildHandler_certificateRevokedMsg,
						hostRoot.getProductStrings().getDistributionName());
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
}
