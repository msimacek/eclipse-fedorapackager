package org.fedoraproject.eclipse.packager.koji.api;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.fedoraproject.eclipse.packager.koji.internal.utils.KojiClientFactory;

/**
 * Generic base class for a koji job. All Koji job object should extend this.
 * 
 */
public abstract class KojiJob extends Job {

	protected Shell shell;
	protected String[] kojiInfo;
	protected IProjectRoot fedoraProjectRoot;

	/**
	 * @param name
	 *            The name of the job.
	 * @param shell
	 *            The shell the job is run in.
	 * @param fpr
	 *            The root of the project being built.
	 * @param kojiInfo
	 *            The information for the server being used to run this job.
	 * 
	 */
	public KojiJob(String name, Shell shell, String[] kojiInfo, IProjectRoot fpr) {
		super(name);
		this.shell = shell;
		this.kojiInfo = kojiInfo;
		fedoraProjectRoot = fpr;
	}

	/**
	 * Create a hub client based on set preferences.
	 * 
	 * @return The koji client.
	 * @throws KojiHubClientException
	 *             Thrown if login is unsuccessful. This Exception is provided
	 *             with an {@link IStatus} object which callers should return.
	 */
	protected IKojiHubClient getHubClient() throws KojiHubClientException {
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		try {
			return KojiClientFactory.getHubClient(kojiInfo[1]);
		} catch (MalformedURLException e) {
			logger.logError(NLS.bind(KojiText.KojiBuildHandler_invalidHubUrl,
					fedoraProjectRoot.getProductStrings().getBuildToolName()),
					e);
			IStatus status = new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID,
					NLS.bind(KojiText.KojiBuildHandler_invalidHubUrl,
							fedoraProjectRoot.getProductStrings()
									.getBuildToolName()), e);

			throw new KojiHubClientException(
					KojiText.KojiBuildHandler_invalidHubUrl, e, status);
		}
	}

	/**
	 * Calls login on the given client and handles errors appropriately
	 * 
	 * @param client
	 *            The client to login.
	 * 
	 * @return the status of the operation .
	 */
	protected IStatus loginHubClient(IKojiHubClient client) {
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();

		try {
			client.login();
		} catch (KojiHubClientLoginException e) {
			String msg = KojiText.KojiBuildHandler_unknownLoginErrorMsg;
			e.printStackTrace();
			// Check if certs were missing
			if (e.isCertificateMissing()) {
				msg = NLS.bind(
						KojiText.KojiBuildHandler_missingCertificatesMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
			}
			if (e.isCertificateExpired()) {
				msg = NLS.bind(
						KojiText.KojiBuildHandler_certificateExpriredMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
			}
			if (e.isCertificateRevoked()) {
				msg = NLS.bind(KojiText.KojiBuildHandler_certificateRevokedMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
			}

			logger.logError(msg, e);
			return new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, msg, e);
		}

		return Status.OK_STATUS;
	}
}
