package org.fedoraproject.eclipse.packager.koji.internal.handlers;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.api.IKojiHubClient;
import org.fedoraproject.eclipse.packager.koji.api.KojiJob;
import org.fedoraproject.eclipse.packager.koji.api.KojiRepoInfo;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.internal.ui.KojiTargetDialog;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * A Koji job which calls executes Koji repo-wait.
 * 
 */
public class KojiWaitForRepoJob extends KojiJob {

	private IKojiHubClient kojiClient;

	/**
	 * @param name
	 *            The name of the job
	 * @param shell
	 *            The shell the job will run in.
	 * @param kojiInfo
	 *            Koji server information.
	 * @param fedoraProjectRoot
	 *            The project root for this job.
	 */
	public KojiWaitForRepoJob(String name, Shell shell,
			IProjectRoot fedoraProjectRoot, String[] kojiInfo) {
		super(name, shell, kojiInfo, fedoraProjectRoot);
	}

	private IStatus initializeKojiClient() {
		// Get koji client
		try {
			kojiClient = getHubClient();
		} catch (KojiHubClientException e) {
			return e.getStatus();
		}

		return loginHubClient(kojiClient);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IStatus status = this.initializeKojiClient();
		if (status != Status.OK_STATUS)
			return status;

		String tag = null;

		try {
			monitor.beginTask(KojiText.KojiWaitForRepoJob_collectingRepoTags,
					IProgressMonitor.UNKNOWN);
			final Set<String> targetSet = kojiClient.listBuildTags();
			FutureTask<String> tagTask = new FutureTask<>(
					new Callable<String>() {

						@Override
						public String call() {
							return new KojiTargetDialog(shell, targetSet, false)
									.openForTarget();
						}

					});
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;

			Display.getDefault().syncExec(tagTask);
			tag = tagTask.get();
			monitor.done();
			if (tag == null) {
				return Status.CANCEL_STATUS;
			}

		} catch (KojiHubClientException e) {
			FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (InterruptedException e) {
			FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (ExecutionException e) {
			FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					e.getMessage(), e);
		}

		
		KojiRepoInfo oldRepo = getRepo(tag);

		monitor.beginTask(NLS.bind(
				KojiText.KojiWaitForRepoJob_WaitingForUpdateMessage, tag),
				IProgressMonitor.UNKNOWN);

		while (!monitor.isCanceled()) {
			try {
				// Sleep for 60 seconds but check for cancellation
				// every 10 seconds.
				int slept = 0;
				while (slept < 60000 && !monitor.isCanceled()) {
					slept += 10000;
					Thread.sleep(10000);
				}
			} catch (InterruptedException e) {
				// Ignore.
			}

			if (!oldRepo.equals(getRepo(tag))) {
				// The repository has been updated.
				FedoraHandlerUtils
						.showInformationDialog(
								shell,
								KojiText.KojiWaitForRepoJob_repoUpdatedDialogTitle,
								NLS.bind(
										KojiText.KojiWaitForRepoJob_repoUpdatedDialogText,
										tag));
				break;
			}
		}

		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		monitor.done();
		return Status.OK_STATUS;
	}

	private KojiRepoInfo getRepo(String tag) {
		try {
			KojiRepoInfo repo = kojiClient.getRepo(tag);
			return repo;
		} catch (KojiHubClientException e) {
			// return some generic error
			String msg = NLS.bind(
					KojiText.KojiWaitForRepoHandler_errorGettingRepoInfo,
					e.getMessage());
			FedoraPackagerLogger.getInstance().logError(msg, e);
			return null;
		}
	}
}
