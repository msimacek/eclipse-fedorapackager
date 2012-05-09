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
package org.fedoraproject.eclipse.packager.tests.units;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.fedoraproject.eclipse.packager.bodhi.api.BodhiClient;
import org.fedoraproject.eclipse.packager.bodhi.api.BodhiLoginResponse;
import org.fedoraproject.eclipse.packager.bodhi.api.BodhiUpdateResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the bodhi client. For this test too succeed, you'll need to have
 * the following Java system properties set:
 * <ul>
 * <li>{@code org.fedoraproject.eclipse.packager.tests.bodhi.fasUsername} a
 * valid FAS username to test logins</li>
 * <li>{@code org.fedoraproject.eclipse.packager.tests.bodhi.fasPassword}
 * corresponding password to above login</li>
 * <li>{@code org.fedoraproject.eclipse.packager.tests.bodhi.testInstanceURL}
 * URL to a local bodhi instance where updates pushing can be tested</li>
 * </ul>
 */
public class BodhiClientTest {

	private BodhiClient client;
	private URL bodhiUrl;
	
	// URL should end with '/'
	private static final String BODHI_STAGING = "https://admin.stg.fedoraproject.org/updates/"; //$NON-NLS-1$
	private static final String BODHI_ADMIN_USERNAME = "guest"; //$NON-NLS-1$
	private static final String BODHI_ADMIN_PASSWORD = "guest"; //$NON-NLS-1$
	private static final String PACKAGE_UPDATE_NVR = "ed-1.5-3.fc15"; //$NON-NLS-1$
	
	@Before
	public void setUp() throws Exception {
		try {
			bodhiUrl = new URL(BODHI_STAGING);
		} catch (MalformedURLException e) {
			// ignore
		}
	}
	
	@After
	public void tearDown() {
		try {
			client.logout();
		} catch (Exception e) {
			// don't care
		}
	}
	
	@Test
	public void testLogin() throws Exception {
		// TODO: implement matcher for multipart entity, override parseResult to skip result parsing 
		final HttpClient mockClient = createMock(HttpClient.class);
		HttpResponse mockResponse = createMock(HttpResponse.class);
		client = new BodhiClient(bodhiUrl) {
			@Override
			protected HttpClient getClient() {
				return mockClient;
			}
		};
		client.login(BODHI_ADMIN_USERNAME, BODHI_ADMIN_PASSWORD);
		client.shutDownConnection();
		client = new BodhiClient(this.bodhiUrl);
	}

	@Test
	public void testLogout() throws Exception {
		// TODO: implement matcher for multipart entity
		final HttpClient mockClient = createMock(HttpClient.class);
		client = new BodhiClient(bodhiUrl) {
			@Override
			protected HttpClient getClient() {
				return mockClient;
			}
		};		
		client.login(BODHI_ADMIN_USERNAME, BODHI_ADMIN_PASSWORD);
		client.shutDownConnection();
		client = new BodhiClient(this.bodhiUrl);
		client.logout();
	}

	/**
	 * Test updates pushing. This will only work if the build in question has
	 * been built in the Koji instance which is connected to the Bodhi test
	 * instance.
	 * 
	 * @throws Exception
	 */
	@Test
	public void canCreateNewUpdate() throws Exception {
		// TODO: implement matcher for multipart entity
		final HttpClient mockClient = createMock(HttpClient.class);
		client = new BodhiClient(bodhiUrl){
			@Override
			protected HttpClient getClient() {
				return mockClient;
			}
		};
		
		BodhiLoginResponse resp = client.login(BODHI_ADMIN_USERNAME, BODHI_ADMIN_PASSWORD);
		// sanity check
		assertNotNull(BODHI_ADMIN_PASSWORD, resp.getUser().getPassword());
		// push the update
		String[] builds = { PACKAGE_UPDATE_NVR };
		BodhiUpdateResponse updateResponse = client.createNewUpdate(builds,
				"F15", "enhancement", "testing", "",
				"This is a test. Please disregard", "", false, true, 3, -3, true);
		assertEquals("Update successfully created", updateResponse.getFlashMsg());
		assertEquals("ed", updateResponse.getUpdates()[0].getBuilds()[0].getPkg().getName());
	}

}
