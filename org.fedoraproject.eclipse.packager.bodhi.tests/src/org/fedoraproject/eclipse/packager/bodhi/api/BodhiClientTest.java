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
package org.fedoraproject.eclipse.packager.bodhi.api;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.fedoraproject.eclipse.packager.bodhi.api.errors.BodhiClientException;
import org.fedoraproject.eclipse.packager.bodhi.api.errors.BodhiClientLoginException;
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

	private URL bodhiUrl;
	
	// URL should end with '/'
	private static final String BODHI_STAGING = "https://admin.stg.fedoraproject.org/updates/"; //$NON-NLS-1$
	private static final String BODHI_ADMIN_USERNAME = "guest"; //$NON-NLS-1$
	private static final String BODHI_ADMIN_PASSWORD = "guest"; //$NON-NLS-1$
	private static final String PACKAGE_UPDATE_NVR = "ed-1.5-3.fc15"; //$NON-NLS-1$
	
	@Before
	public void setUp() throws MalformedURLException  {
		bodhiUrl = new URL(BODHI_STAGING);
	}
	
	@Test
	public void testLogin() throws ClientProtocolException, IOException, BodhiClientLoginException  {
		final HttpClient mockClient = createMock(HttpClient.class);
		HttpResponse mockResponse = createMock(HttpResponse.class);
		StatusLine mockStatus = createMock(StatusLine.class);
		HttpEntity mockEntity = createMock(HttpEntity.class);
		BodhiClient client = new BodhiClient(bodhiUrl) {
			@Override
			protected HttpClient getClient() {
				return mockClient;
			}
		};
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse);
		expect(mockResponse.getStatusLine()).andReturn(mockStatus).anyTimes();
		expect(mockStatus.getStatusCode()).andReturn(HttpURLConnection.HTTP_OK)
				.anyTimes();
		expect(mockResponse.getEntity()).andReturn(mockEntity).anyTimes();
		expect(mockEntity.getContent()).andReturn(
				new ByteArrayInputStream("{ }"
						.getBytes())).anyTimes();
		expect(mockEntity.isStreaming()).andReturn(false);
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class)).anyTimes();
		replay(mockClient);
		replay(mockResponse);
		replay(mockStatus);
		replay(mockEntity);
		client.login(BODHI_ADMIN_USERNAME, BODHI_ADMIN_PASSWORD);
		client.shutDownConnection();
	}
	
	@Test(expected=BodhiClientLoginException.class)
	public void testLoginUnsuccessfull() throws ClientProtocolException, IOException, BodhiClientLoginException  {
		final HttpClient mockClient = createMock(HttpClient.class);
		HttpResponse mockResponse = createMock(HttpResponse.class);
		StatusLine mockStatus = createMock(StatusLine.class);
		HttpEntity mockEntity = createMock(HttpEntity.class);
		BodhiClient client = new BodhiClient(bodhiUrl) {
			@Override
			protected HttpClient getClient() {
				return mockClient;
			}
		};
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse);
		expect(mockResponse.getStatusLine()).andReturn(mockStatus).anyTimes();
		expect(mockStatus.getStatusCode()).andReturn(HttpURLConnection.HTTP_FORBIDDEN)
				.anyTimes();
		expect(mockStatus.getReasonPhrase()).andReturn("Forbidden")
		.anyTimes();
		expect(mockResponse.getEntity()).andReturn(mockEntity).anyTimes();
		expect(mockEntity.isStreaming()).andReturn(false);
		replay(mockClient);
		replay(mockResponse);
		replay(mockStatus);
		replay(mockEntity);
		client.login(BODHI_ADMIN_USERNAME, "");
		client.shutDownConnection();
	}

	@Test
	public void testLogout() throws ClientProtocolException, IOException, BodhiClientLoginException, BodhiClientException {
		final HttpClient mockClient = createMock(HttpClient.class);
		HttpResponse mockResponse = createMock(HttpResponse.class);
		StatusLine mockStatus = createMock(StatusLine.class);
		HttpEntity mockEntity = createMock(HttpEntity.class);
		BodhiClient client = new BodhiClient(bodhiUrl) {
			@Override
			protected HttpClient getClient() {
				return mockClient;
			}
		};
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse).anyTimes();
		expect(mockResponse.getStatusLine()).andReturn(mockStatus).anyTimes();
		expect(mockStatus.getStatusCode()).andReturn(HttpURLConnection.HTTP_OK)
				.anyTimes();
		expect(mockResponse.getEntity()).andReturn(mockEntity).anyTimes();
		expect(mockEntity.getContent()).andReturn(
				new ByteArrayInputStream("{ }"
						.getBytes())).anyTimes();
		expect(mockEntity.isStreaming()).andReturn(false).anyTimes();
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class)).anyTimes();
		replay(mockClient);
		replay(mockResponse);
		replay(mockStatus);
		replay(mockEntity);
		client.login(BODHI_ADMIN_USERNAME, BODHI_ADMIN_PASSWORD);
		client.shutDownConnection();
		client.logout();
	}

	/**
	 * Test updates pushing. This will only work if the build in question has
	 * been built in the Koji instance which is connected to the Bodhi test
	 * instance.
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws BodhiClientLoginException 
	 * @throws BodhiClientException 
	 * 
	 */
	@Test
	public void canCreateNewUpdate() throws ClientProtocolException, IOException, BodhiClientLoginException, BodhiClientException  {
		final HttpClient mockClient = createMock(HttpClient.class);
		HttpResponse mockResponse = createMock(HttpResponse.class);
		StatusLine mockStatus = createMock(StatusLine.class);
		HttpEntity mockEntity = createMock(HttpEntity.class);
		BodhiClient client = new BodhiClient(bodhiUrl) {
			@Override
			protected HttpClient getClient() {
				return mockClient;
			}
		};
		expect(mockClient.execute((HttpUriRequest) anyObject())).andReturn(
				mockResponse).anyTimes();
		expect(mockResponse.getStatusLine()).andReturn(mockStatus).anyTimes();
		expect(mockStatus.getStatusCode()).andReturn(HttpURLConnection.HTTP_OK)
				.anyTimes();
		expect(mockResponse.getEntity()).andReturn(mockEntity).anyTimes();
		expect(mockEntity.getContent()).andReturn(
				new ByteArrayInputStream("{user:{password:\"guest\"} }"
						.getBytes()));
		expect(mockEntity.getContent()).andReturn(
				new ByteArrayInputStream("{tg_flash:\"Update successfully created\" , updates: [ {builds: [{package:{name=\"ed\"}}] }] }"
						.getBytes()));
		expect(mockEntity.isStreaming()).andReturn(false).anyTimes();
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class)).anyTimes();
		replay(mockClient);
		replay(mockResponse);
		replay(mockStatus);
		replay(mockEntity);
		
		BodhiLoginResponse resp = client.login(BODHI_ADMIN_USERNAME, BODHI_ADMIN_PASSWORD);
		// sanity check
		assertNotNull(BODHI_ADMIN_PASSWORD, resp.getUser().getPassword());
		// push the update
		String[] builds = { PACKAGE_UPDATE_NVR };
		BodhiUpdateResponse updateResponse = client.createNewUpdate(builds,
				"enhancement", "testing", "",
				"This is a test. Please disregard", "", false, true, 3, -3, true);
		assertEquals("Update successfully created", updateResponse.getFlashMsg());
		assertEquals("ed", updateResponse.getUpdates()[0].getBuilds()[0].getPkg().getName());
	}

}
