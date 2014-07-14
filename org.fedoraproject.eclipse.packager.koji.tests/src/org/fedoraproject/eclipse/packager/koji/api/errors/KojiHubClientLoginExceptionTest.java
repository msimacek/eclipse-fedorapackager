/*******************************************************************************
 * Copyright (c) 2010-2011 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.koji.api.errors;

import static org.junit.Assert.assertFalse;

import java.security.GeneralSecurityException;

import org.fedoraproject.eclipse.packager.FedoraSSL;
import org.fedoraproject.eclipse.packager.MockableFedoraSSL;
import org.junit.Before;
import org.junit.Test;

public class KojiHubClientLoginExceptionTest {

	private KojiHubClientLoginException exceptionUnderTest;

	private static class MockFedoraSSL extends MockableFedoraSSL {
		@Override
		public boolean isFedoraCertValid() {
			// Mock this function here as this function is unit tested in
			// FedoraSSLTest
			return true;
		}
	}

	@Before
	public void setUp() {
		final MockFedoraSSL mockFedoraSSL = new MockFedoraSSL();
		exceptionUnderTest = new KojiHubClientLoginException(
				new GeneralSecurityException()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected FedoraSSL getFedoraSSLInstance() {
				return mockFedoraSSL;
			}
		};
	}

	/**
	 * We should be able to determine if login exception was due to an expired
	 * certificate. This test requires a valid ~/.fedora.cert
	 * 
	 */
	@Test
	public void testIsCertificateExpired() {
		assertFalse(exceptionUnderTest.isCertificateExpired());
	}

}
