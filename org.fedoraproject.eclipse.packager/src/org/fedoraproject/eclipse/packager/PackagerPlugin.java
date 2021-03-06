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
package org.fedoraproject.eclipse.packager;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 *
 * @since 0.5.0
 */
public class PackagerPlugin extends AbstractUIPlugin {

	/** The symbolic plugin ID. */
	public static final String PLUGIN_ID = "org.fedoraproject.eclipse.packager"; //$NON-NLS-1$

	/** The Fedora Packaging perspective ID */
	public static final String FEDORA_PACKAGING_PERSPECTIVE_ID = PLUGIN_ID + ".perspective"; //$NON-NLS-1$
	
	// The shared instance
	private static PackagerPlugin plugin;
	
	// Persistent property things
	
	// Type values for persistent property types
	/** Git type for persistent property types */
	public static final String PROJECT_KEY = "project"; //$NON-NLS-1$
	/** Qualified name for the type property */
	public static final QualifiedName PROJECT_PROP = new QualifiedName(PLUGIN_ID, PROJECT_KEY);

	/** Local type for persistent property types */
	public static final String PROJECT_LOCAL_KEY = "localproject"; //$NON-NLS-1$
	/** Qualified name for the type property */
	public static final QualifiedName PROJECT_LOCAL_PROP = new QualifiedName(PLUGIN_ID, PROJECT_LOCAL_KEY);

	/** The constructor */
	public PackagerPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static PackagerPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	/**
	 * Get a String preference related to this plug-in.
	 *
	 * @param prefrenceIdentifier
	 *            The identifier of the preference to retrieve.
	 * @return The value of the prefrence in question, or {@code null} if not
	 *         set.
	 */
	public static String getStringPreference(final String prefrenceIdentifier) {
		IPreferenceStore store = getDefault().getPreferenceStore();
		String candidate = store.getString(prefrenceIdentifier);
		if (isConfEnabled()) {
			return ConfigurationScope.INSTANCE.getNode(PLUGIN_ID).get(prefrenceIdentifier,
					candidate.isEmpty() ? null : candidate);
		}
		if (candidate.equals(IPreferenceStore.STRING_DEFAULT_DEFAULT)) {
			return null;
		}
		return candidate;
	}

	/**
	 * Get the enabled status of using the .conf file found in /etc/rpkg/.
	 *
	 * @return True if it is being used, false otherwise.
	 *
	 * @since 0.5
	 */
	public static boolean isConfEnabled() {
		return getDefault().getPreferenceStore().getBoolean(FedoraPackagerPreferencesConstants.PREF_FEDPKG_CONFIG_ENABLED);
	}
}
