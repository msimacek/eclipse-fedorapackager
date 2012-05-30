package org.fedoraproject.eclipse.packager.koji.internal.handlers;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerAbstractHandler;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;
import org.fedoraproject.eclipse.packager.koji.api.KojiChainBuildJob;
import org.fedoraproject.eclipse.packager.koji.internal.ui.ChainBuildDialog;

/**
 * Action for pushing a chain build to Koji.
 *
 */
public class ChainBuildHandler extends FedoraPackagerAbstractHandler {

	private Shell shell;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		shell = getShell(event);
		ChainBuildDialog dialog = new ChainBuildDialog(shell);
		List<List<String>> buildList = dialog.open();
		final IProjectRoot[] roots = dialog.getRoots();
		if (dialog.getResult() == Window.OK) {
			String[] kojiInfo = KojiPlugin.getDefault().getPreferenceStore()
					.getString(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO)
					.split(","); //$NON-NLS-1$
			Job job = new KojiChainBuildJob(roots[0].getProductStrings()
					.getProductName(), shell, roots, kojiInfo, buildList);
			job.addJobChangeListener(KojiUtils.getJobChangeListener(kojiInfo,
					roots[0]));
			job.setUser(true);
			job.schedule();
		}
		return null;
	}
}
