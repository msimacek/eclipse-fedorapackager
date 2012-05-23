package org.fedoraproject.eclipse.packager;

import java.io.File;

/**
 * A Mockable class with a public constructor for testing purposes.
 */
public class MockableFedoraSSL extends FedoraSSL {

	/**
	 * Creates a dummy object for testing purposes.
	 */
	public MockableFedoraSSL() {
		super(new File(""), new File(""), new File("")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
