/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.internal.preferences;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.fedoraproject.eclipse.packager.ConfigKeys;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.KojiConfigKeys;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.internal.config.ConfigParser;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Handle configuration parsing results and whether or not to commit into
 * PreferenceStore.
 *
 */
public class FedoraPackagerConfigPreference {

	private ConfigParser configParser;
	private Map<String, String> configurationProperties;

	// The required settings to change in eclipse preferences
	private static final Map<String, String> REQ_SETTINGS;
	static {
		Map<String, String> aMap = new HashMap<>();
		aMap.put(
				FedoraPackagerPreferencesConstants.PREF_LOOKASIDE_DOWNLOAD_URL,
				ConfigKeys.LOOKASIDE);
		aMap.put(FedoraPackagerPreferencesConstants.PREF_LOOKASIDE_UPLOAD_URL,
				ConfigKeys.LOOKASIDE_CGI);
		REQ_SETTINGS = Collections.unmodifiableMap(aMap);
	}

	// The required koji settings to change in eclipse preferences
	private static final Map<String, String> REQ_KOJI_SETTINGS;
	static {
		Map<String, String> aMap = new HashMap<>();
		aMap.put(FedoraPackagerPreferencesConstants.PREF_KOJI_WEB_URL,
				KojiConfigKeys.WEB_URL);
		aMap.put(FedoraPackagerPreferencesConstants.PREF_KOJI_HUB_URL,
				KojiConfigKeys.SERVER);
		REQ_KOJI_SETTINGS = Collections.unmodifiableMap(aMap);
	}

	/**
	 * The constructor will instantiate a ConfigParser to grab the information
	 * from the given file.
	 *
	 * @param filePath
	 *            The path to the configuration file.
	 */
	public FedoraPackagerConfigPreference(String filePath) {
		try {
			configParser = new ConfigParser(new File(filePath));
			configurationProperties = configParser.getConfig();
		} catch (IOException e) {
			FedoraPackagerLogger
					.getInstance()
					.logError(
							FedoraPackagerText.FedoraPackagerConfigPreference_parsingFileError,
							e);
		}
	}

	/**
	 * Go through the parsed configuration settings and set the changes into
	 * the InstanceScope preferences.
	 *
	 * @return The preferences that was just put into the InstanceScope.
	 */
	public Preferences getConfigPreference() {
		// store enabled status of using .conf file
		// if getting information from the .conf was successful
		if (configParser != null) {
			// save the enabled setting to the workspace
			Preferences instance = InstanceScope.INSTANCE.getNode(PackagerPlugin.PLUGIN_ID);
			instance.putBoolean(
					FedoraPackagerPreferencesConstants.PREF_FEDPKG_CONFIG_ENABLED,
					true);
			try {
				instance.flush();
			} catch (BackingStoreException e) {
				// nothing to do
			}
			// save the .conf settings into the configuration scope
			Preferences prefs = ConfigurationScope.INSTANCE
					.getNode(PackagerPlugin.PLUGIN_ID);
			// store the required settings from the .conf file
			for (String key : REQ_SETTINGS.keySet()) {
				String newSettings = configurationProperties.get(REQ_SETTINGS.get(key));
				prefs.put(key, newSettings);
			}
			// this sets the preference for the pref gitBaseURL (should be
			// usable
			// from git preference page)
			prefs.put(FedoraPackagerPreferencesConstants.PREF_CLONE_BASE_URL,
					configParser.getCloneBaseURL());
			// store the koji settings found from the koji .conf
			Map<String, String> kojiMap = configParser.getKojiConfig();
			for (String key : REQ_KOJI_SETTINGS.keySet()) {
				String newSettings = kojiMap.get(REQ_KOJI_SETTINGS.get(key));
				prefs.put(key, newSettings);
			}
			String prefKojiInfo = kojiMap.get(KojiConfigKeys.WEB_URL)
					+ "," + kojiMap.get(KojiConfigKeys.SERVER) //$NON-NLS-1$
					+ ",false"; //$NON-NLS-1$
			prefs.put(FedoraPackagerPreferencesConstants.PREF_KOJI_SERVER_INFO,
					prefKojiInfo);
			return prefs;
		}
		// else set the preference to the default
		restoreDefaults();
		return null;
	}

	/**
	 * Set the instance scope preferences to that of the default preferences.
	 * If there is no default preference, the current is used.
	 *
	 */
	public static void restoreDefaults() {
		Preferences prefs = InstanceScope.INSTANCE
				.getNode(PackagerPlugin.PLUGIN_ID);
		Preferences defaultPrefs = DefaultScope.INSTANCE
				.getNode(PackagerPlugin.PLUGIN_ID);
		try {
			for (String key : prefs.keys()) {
				String previousSetting = prefs.get(key, ""); //$NON-NLS-1$
				String defaultSetting = defaultPrefs.get(key, previousSetting);
				prefs.put(key, defaultSetting);
			}
			prefs.flush();
		} catch (BackingStoreException e) {
			FedoraPackagerLogger.getInstance()
					.logError(FedoraPackagerText.FedoraPackagerConfigPreference_settingPreferenceError,
							e);
		}
	}
}
