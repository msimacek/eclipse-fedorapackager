package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;

/**
 * Class for initialization of Eclipse Fedora Packager preferences.
 */
public class FedoraPackagerKojiPreferenceInitializer extends
		AbstractPreferenceInitializer {

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
		String oldWeb = PackagerPlugin.getStringPreference("kojiWebURL"); //$NON-NLS-1$
		String oldXml = PackagerPlugin.getStringPreference("kojiHubURL"); //$NON-NLS-1$
		boolean existingSettings = false;
		if (oldWeb != null && oldXml != null && oldWeb.length() > 0 && oldXml.length() > 0){
			serverList = serverList.concat(NLS.bind(KojiText.ServerEntryTemplate, new String[] {
					"Existing Koji Settings", oldWeb, oldXml })); //$NON-NLS-1$
			node.put(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, oldWeb
					+ "," + oldXml + ",false"); //$NON-NLS-1$ //$NON-NLS-2$
			existingSettings = true;
		}
		for (IConfigurationElement instance : config) {
			String serverName = instance.getAttribute("name"); //$NON-NLS-1$
			String webUrl = instance.getAttribute("webUrl"); //$NON-NLS-1$
			String xmlrpcUrl = instance.getAttribute("xmlrpcUrl"); //$NON-NLS-1$
			serverList = serverList.concat(NLS.bind(KojiText.ServerEntryTemplate, new String[] {
					serverName, webUrl, xmlrpcUrl, "false" })); //$NON-NLS-1$
			if (!existingSettings && serverName.contentEquals("Default Fedora Koji Instance")) { //$NON-NLS-1$
				node.put(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, webUrl
						+ "," + xmlrpcUrl + ",false"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		node.put(KojiPreferencesConstants.PREF_SERVER_LIST, serverList);
	}

}
