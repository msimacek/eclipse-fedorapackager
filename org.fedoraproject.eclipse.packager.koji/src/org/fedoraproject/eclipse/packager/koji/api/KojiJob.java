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
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

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
	 * @throws MalformedURLException
	 *             If the koji hub URL preference was invalid.
	 * @return The koji client.
	 */
	protected IKojiHubClient getHubClient() throws MalformedURLException {
		return new KojiSSLHubClient(kojiInfo[1]);
	}

	/**
	 * Calls login on the given client and handles errors appropriately
	 * 
	 * @param client The client to login.
	 * 
	 * @return 
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
				msg = NLS.bind(
						KojiText.KojiBuildHandler_certificateRevokedMsg,
						fedoraProjectRoot.getProductStrings()
								.getDistributionName());
			}
			
			logger.logError(msg, e);			
			return FedoraHandlerUtils.errorStatus(KojiPlugin.PLUGIN_ID,
					msg, e);
		}
		
		return Status.OK_STATUS;
	}
}
