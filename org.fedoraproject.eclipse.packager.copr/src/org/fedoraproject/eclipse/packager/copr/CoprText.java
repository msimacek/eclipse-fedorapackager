package org.fedoraproject.eclipse.packager.copr;

import org.eclipse.osgi.util.NLS;

/**
 * Externalized strings for Copr plugin
 *
 * @author msimacek
 *
 */
public class CoprText extends NLS {
	private static final String BUNDLE_NAME = "org.fedoraproject.eclipse.packager.copr.coprtext"; //$NON-NLS-1$

	/****/ public static String CoprHandler_NoLoginToken;
	/****/ public static String CoprHandler_NoName;
	/****/ public static String CoprHandler_NoURL;
	/****/ public static String CoprHandler_NoUsername;

	/****/ public static String CoprPreferencePage_DefaultUsername;
	/****/ public static String CoprPreferencePage_Description;
	/****/ public static String CoprPreferencePage_LoginField;
	/****/ public static String CoprPreferencePage_TokenField;
	/****/ public static String CoprPreferencePage_URLField;

	/****/ public static String CoprProjectProperties_Description;
	/****/ public static String CoprProjectProperties_NameField;
	/****/ public static String CoprProjectProperties_UsernameField;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoprText.class);
	}

	private CoprText() {
	}
}
