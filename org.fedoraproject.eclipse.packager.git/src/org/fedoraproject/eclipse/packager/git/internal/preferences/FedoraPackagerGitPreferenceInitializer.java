/*******************************************************************************
 * Copyright (c) 2010, 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.git.internal.preferences;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.fedoraproject.eclipse.packager.git.Activator;
import org.fedoraproject.eclipse.packager.git.GitPreferencesConstants;

/**
 * Class for initialization of Eclipse Fedora Packager preferences.
 */
public class FedoraPackagerGitPreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		// field gets prefilled
		IEclipsePreferences node = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		node.put(GitPreferencesConstants.PREF_CLONE_BASE_URL,
				GitPreferencesConstants.DEFAULT_CLONE_BASE_URL);
		node.put(GitPreferencesConstants.PREF_CLONE_DIR,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());

		// set to default, the base URL that is not the .conf
		IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		prefStore.setDefault(GitPreferencesConstants.PREF_CLONE_BASE_URL,
				GitPreferencesConstants.DEFAULT_CLONE_BASE_URL);
		prefStore.setDefault(GitPreferencesConstants.PREF_CLONE_DIR,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
	}

}
