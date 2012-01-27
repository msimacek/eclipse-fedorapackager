package org.fedoraproject.eclipse.packager.koji;

/**
 * Constants for Koji related preferences.
 */
public class KojiPreferencesConstants {

	/*
	 * -------------------------------------------------
	 *                Prefences keys
	 * -------------------------------------------------
	 */
	/***/ public static final String PREF_KOJI_SERVER_INFO =
			"kojiServerInfo"; //$NON-NLS-1$
	
	/***/ public static final String PREF_SERVER_LIST =
			"serverList"; //$NON-NLS-1$
	
	/*
	 * -------------------------------------------------
	 *          Default values for preferences
	 * -------------------------------------------------
	 */
	
	/***/ public static final String DEFAULT_KOJI_SERVER_INFO = 
		""; //$NON-NLS-1$
	
	/**
	 * Default servers provided by plugin. 
	 */ 
	public static final String DEFAULT_SERVER_LIST = 
			""; //$NON-NLS-1$
}
