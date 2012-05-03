package org.fedoraproject.eclipse.packager.koji.internal.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Shell;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerAbstractHandler;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * A root KojiHandler which sets up information used be all KojiHandlers.
 * 
 */
public abstract class KojiHandler extends FedoraPackagerAbstractHandler {

	protected Shell shell;
	protected String[] kojiInfo;

	protected String[] setKojiInfo(ExecutionEvent event)
			throws ExecutionException {
		this.shell = getShell(event);
		IResource eventResource = FedoraHandlerUtils.getResource(event);

		IEclipsePreferences projectPreferences = new ProjectScope(
				eventResource.getProject()).getNode(KojiPlugin.getDefault()
				.getBundle().getSymbolicName());
		String kojiInfoString = projectPreferences
				.get(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO,
						KojiPlugin
								.getDefault()
								.getPreferenceStore()
								.getString(
										KojiPreferencesConstants.PREF_KOJI_SERVER_INFO));
		if (kojiInfoString == KojiText.FedoraPackagerKojiPreferencePage_DefaultPlaceholder) {
			kojiInfo = KojiPlugin.getDefault().getPreferenceStore()
					.getString(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO)
					.split(","); //$NON-NLS-1$
		} else {
			kojiInfo = kojiInfoString.split(","); //$NON-NLS-1$
		}

		if (kojiInfo[2].contentEquals("false") //$NON-NLS-1$
				&& projectPreferences
						.getBoolean(
								KojiPreferencesConstants.PREF_FORCE_CUSTOM_BUILD,
								false)) { // $NON-NLS
			kojiInfo[2] = "true"; //$NON-NLS-1$
		}

		return kojiInfo;
	}
}
