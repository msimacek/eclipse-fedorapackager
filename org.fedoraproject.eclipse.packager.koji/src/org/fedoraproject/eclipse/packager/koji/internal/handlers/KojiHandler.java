package org.fedoraproject.eclipse.packager.koji.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * A root KojiHandler which sets up information used be all KojiHandlers.
 * 
 */
public abstract class KojiHandler extends AbstractHandler {

	protected Shell shell;
	protected String[] kojiInfo;

	protected String[] setKojiInfo(ExecutionEvent event)
			throws ExecutionException {
		this.shell =  HandlerUtil.getActiveShellChecked(event);
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
		if (kojiInfoString.contentEquals(KojiText.FedoraPackagerKojiPreferencePage_DefaultPlaceholder)) {
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
	
	/**
	 * Return the project root and handle errors
	 * 
	 * @param event
	 *            the event to be used for retrieving the relevant resource.
	 * @return the project root or null if a valide root is not found.
	 */
	IProjectRoot getProjectRoot(ExecutionEvent event) {
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		IResource eventResource = FedoraHandlerUtils.getResource(event);
		try {
			final IProjectRoot projectRoot = FedoraPackagerUtils
					.getProjectRoot(eventResource);
			return projectRoot;
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
		}

		return null;
	}

}
