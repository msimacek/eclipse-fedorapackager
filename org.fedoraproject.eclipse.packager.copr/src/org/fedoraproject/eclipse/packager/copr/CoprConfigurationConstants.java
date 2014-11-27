package org.fedoraproject.eclipse.packager.copr;

/**
 * Constants for Copr plugin preferences
 *
 * @author msimacek
 *
 */
public class CoprConfigurationConstants {
	/**
	 * URL pointing to Copr instance on the Internet
	 */
	public static final String COPR_URL = "coprURL"; //$NON-NLS-1$

	/**
	 * Copr username
	 */
	public static final String COPR_USERNAME = "coprUser"; //$NON-NLS-1$

	/**
	 * Copr API token used to communicate with Copr instance
	 */
	public static final String COPR_API_TOKEN = "coprAPIToken"; //$NON-NLS-1$

	/**
	 * Login string used to communicate with Copr instance. Different from username, not human-readable
	 */
	public static final String COPR_API_LOGIN = "coprAPILogin"; //$NON-NLS-1$

	/**
	 * Name of Copr project
	 */
	public static final String COPR_NAME = "coprName"; //$NON-NLS-1$
}
