package org.fedoraproject.eclipse.packager.koji.internal.core;

/**
 * 
 * Exception raised if some Koji hub-client initialization fails.
 *
 */
public class MalformedURLException extends Exception {

	/**
	 * @param e
	 */
	public MalformedURLException(final Exception e) {
		super(e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}