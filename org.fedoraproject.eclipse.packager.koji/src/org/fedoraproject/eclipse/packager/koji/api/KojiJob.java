package org.fedoraproject.eclipse.packager.koji.api;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;

/**
 * Generic base class for a koji job. All Koji job object should extend this.
 * 
 */
public abstract class KojiJob extends Job {

	protected Shell shell;

	/**
	 * @param name
	 *            The name of the job.
	 * @param shell
	 *            The shell the job is run in.
	 * 
	 */
	public KojiJob(String name, Shell shell) {
		super(name);
		this.shell = shell;
	}

}
