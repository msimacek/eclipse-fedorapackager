package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;

/**
 * Class for initialization of Eclipse Fedora Packager preferences.
 *
 */
public class FedoraPackagerKojiPreferenceInitializer extends
		AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		// set default preferences for this plug-in
		IEclipsePreferences node = DefaultScope.INSTANCE
				.getNode(KojiPlugin.PLUGIN_ID);
		// don't use preset default, instead find Koji instance objects and
		// construct a default
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(
						"org.fedoraproject.eclipse.packager.koji.instance"); //$NON-NLS-1$
		String serverList = ""; //$NON-NLS-1$
		// import old settings if upgrade from old version
		String oldWeb = PackagerPlugin.getStringPreference(FedoraPackagerPreferencesConstants.PREF_KOJI_WEB_URL);
		String oldXml = PackagerPlugin.getStringPreference(FedoraPackagerPreferencesConstants.PREF_KOJI_HUB_URL);
		boolean existingSettings = false;
		if (oldWeb != null && oldXml != null && oldWeb.length() > 0 && oldXml.length() > 0){
			existingSettings = true;
		}
		String backupDefaultWeb = "", backupDefaultXml = "";  //$NON-NLS-1$ //$NON-NLS-2$
		for (IConfigurationElement instance : config) {
			String serverName = instance.getAttribute("name"); //$NON-NLS-1$
			String webUrl = instance.getAttribute("webUrl"); //$NON-NLS-1$
			String xmlrpcUrl = instance.getAttribute("xmlrpcUrl"); //$NON-NLS-1$
			String customTargets = instance.getAttribute("customTargets"); //$NON-NLS-1$
			serverList = serverList.concat(NLS.bind(KojiText.ServerEntryTemplate, new String[] {
					serverName, webUrl, xmlrpcUrl, customTargets }));
			if (existingSettings && webUrl.contentEquals(oldWeb) && xmlrpcUrl.contentEquals(oldXml)){
				existingSettings = false;
			}
			if (serverName.contentEquals("Default Fedora Koji Instance")) { //$NON-NLS-1$
				backupDefaultWeb = webUrl;
				backupDefaultXml = xmlrpcUrl;
			}
		}
		// modify the PREF_KOJI_SERVER_INFO here based on the enabled status of .conf in general preferences
		if (PackagerPlugin.isConfEnabled()) {
			// if enabled, set PREF_KOJI_SERVER_INFO as the configuration from .conf
			String prefServerInfo = PackagerPlugin.getStringPreference(FedoraPackagerPreferencesConstants.PREF_KOJI_SERVER_INFO);
			node.put(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, prefServerInfo);
		} else {
			if (existingSettings){
				serverList = serverList.concat(NLS.bind(KojiText.ServerEntryTemplate, new String[] {
						"Existing Koji Settings", oldWeb, oldXml })); //$NON-NLS-1$
				node.put(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, oldWeb
						+ "," + oldXml + ",false"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				node.put(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, backupDefaultWeb
						+ "," + backupDefaultXml + ",false"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		node.put(KojiPreferencesConstants.PREF_SERVER_LIST, serverList);

		// initializing DefaultScope preferences here that will be used in ProjectScopes
		// will prohibit any modifications to the preferences in the project. The
		// property will not appear in the project .prefs file unless it has not been
		// set a default value via the DefaultScope. (Figure out why later)
		// tried and tested by setting the PREF_PROJECT_SETTINGS default to true
	}
}
