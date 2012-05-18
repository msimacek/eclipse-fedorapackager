package org.fedoraproject.eclipse.packager.koji.internal.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.fedoraproject.eclipse.packager.IProjectRoot;

/**
 * Handler for waiting for a Koji repo to be created.
 */
public class KojiWaitForRepoHandler extends KojiHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		setKojiInfo(event);

		final IProjectRoot projectRoot = getProjectRoot(event);
		if (projectRoot != null){
			Job job = new KojiWaitForRepoJob(projectRoot
					.getProductStrings().getProductName(), getShell(event),
					projectRoot, kojiInfo);
			job.setUser(true);
			job.schedule();
		}

		return null;
	}

}
