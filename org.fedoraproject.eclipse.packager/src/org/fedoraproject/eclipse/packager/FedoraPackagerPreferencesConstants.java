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

/**
 * Constants for preferences.
 *
 * @since 0.5
 */
public final class FedoraPackagerPreferencesConstants {

	/*
	 * ------------------------------------------------- Prefences keys
	 * -------------------------------------------------
	 */
	/***/
	public static final String PREF_DEBUG_MODE = "debug"; //$NON-NLS-1$
	/***/
	public static final String PREF_LOOKASIDE_DOWNLOAD_URL = "lookasideDownloadURL"; //$NON-NLS-1$
	/***/
	public static final String PREF_LOOKASIDE_UPLOAD_URL = "lookasideUploadURL"; //$NON-NLS-1$
	/***/
	public static final String PREF_KOJI_WEB_URL = "kojiWebURL"; //$NON-NLS-1$
	/***/
	public static final String PREF_KOJI_HUB_URL = "kojiHubURL"; //$NON-NLS-1$
	/**@since 0.5*/
	public static final String PREF_FEDPKG_CONFIG_ENABLED = "config"; //$NON-NLS-1$
	/**@since 0.5*/
	public static final String PREF_FEDPKG_CONFIG = "fedpkgConfig"; //$NON-NLS-1$
	/**@since 0.5*/
	public static final String PREF_CLONE_BASE_URL = "gitCloneBaseURL"; //$NON-NLS-1$
	/**@since 0.5*/
	public static final String PREF_KOJI_SERVER_INFO = "kojiServerInfo"; //$NON-NLS-1$

	/*
	 * ------------------------------------------------- Default values for preferences
	 * -------------------------------------------------
	 */
	/***/
	public static final boolean DEFAULT_DEBUG_MODE = false;
	/**
	 * Default download URL for the Fedora lookaside cache
	 */
	public static final String DEFAULT_LOOKASIDE_DOWNLOAD_URL = "http://pkgs.fedoraproject.org/repo/pkgs"; //$NON-NLS-1$
	/**
	 * Default upload URL for the Fedora lookaside cache
	 */
	public static final String DEFAULT_LOOKASIDE_UPLOAD_URL = "https://pkgs.fedoraproject.org/repo/pkgs/upload.cgi"; //$NON-NLS-1$
	/**
	 * Default koji web url
	 */
	public static final String DEFAULT_KOJI_WEB_URL = "http://koji.fedoraproject.org/koji"; //$NON-NLS-1$
	/**
	 * Default koji hub url
	 */
	public static final String DEFAULT_KOJI_HUB_URL = "https://koji.fedoraproject.org/kojihub"; //$NON-NLS-1$
	/**
	 * Default enabled status of using the .conf file
	 * @since 0.5
	 */
	public static final boolean DEFAULT_CONFIG_ENABLED = true;
	/**
	 * Default location of the .conf file
	 * @since 0.5
	 */
	public static final String DEFAULT_FEDPKG_CONFIG = "/etc/rpkg/fedpkg.conf"; //$NON-NLS-1$
	/**
	 * Default clone base url
	 * @since 0.5
	 */
	public static final String DEFAULT_CLONE_BASE_URL = "pkgs.fedoraproject.org/"; //$NON-NLS-1$
}
