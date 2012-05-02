package org.fedoraproject.eclipse.packager.koji.api;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.IProjectRoot;

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

}
