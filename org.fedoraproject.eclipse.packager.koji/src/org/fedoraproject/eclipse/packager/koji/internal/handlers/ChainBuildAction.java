package org.fedoraproject.eclipse.packager.koji.internal.handlers;

import java.util.List;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;
import org.fedoraproject.eclipse.packager.koji.api.KojiChainBuildJob;
import org.fedoraproject.eclipse.packager.koji.internal.ui.ChainBuildDialog;

/**
 * Action for pushing a chain build to Koji.
 *
 */
public class ChainBuildAction implements IWorkbenchWindowActionDelegate {

	private Shell shell;

	@Override
	public void run(IAction action) {
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
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// no op
	}

	@Override
	public void dispose() {
		// no op
	}

	@Override
	public void init(IWorkbenchWindow window) {
		shell = window.getShell();
	}

}
