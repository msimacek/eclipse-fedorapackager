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
/**
 * 
 */
package org.fedoraproject.eclipse.packager.bodhi.api;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.bodhi.api.BodhiClient;
import org.fedoraproject.eclipse.packager.bodhi.api.PushUpdateCommand;
import org.fedoraproject.eclipse.packager.bodhi.api.PushUpdateCommand.RequestType;
import org.fedoraproject.eclipse.packager.bodhi.api.PushUpdateCommand.UpdateType;
import org.fedoraproject.eclipse.packager.bodhi.api.PushUpdateResult;
import org.fedoraproject.eclipse.packager.bodhi.api.errors.BodhiClientException;
import org.fedoraproject.eclipse.packager.bodhi.api.errors.BodhiClientLoginException;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link PushUpdateCommand}. For this test too succeed, you'll need to have
 * the following Java system properties set:
 * <ul>
 * <li>{@code org.fedoraproject.eclipse.packager.tests.bodhi.testInstanceURL}
 * URL to a local bodhi instance where updates pushing can be tested</li>
 * </ul>
 */
public class PushUpdateCommandTest {

	private static final String BODHI_ADMIN_USERNAME = "guest"; //$NON-NLS-1$
	private static final String BODHI_ADMIN_PASSWORD = "guest"; //$NON-NLS-1$
	private static final String PACKAGE_UPDATE_NVR = "ed-1.5-3.fc15"; //$NON-NLS-1$
	
	// project under test
	private GitTestProject testProject;
	// main interface class
	private FedoraPackager packager;
	// Fedora packager root
	private IProjectRoot fpRoot;
	
	/**
	 * Clone a test project to be used for testing.
	 * @throws InterruptedException 
	 * @throws InvalidProjectRootException 
	 * 
	 */
	@Before
	public void setUp() throws InterruptedException, InvalidProjectRootException  {
		this.testProject = new GitTestProject("eclipse-fedorapackager");
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);
	}

	/**
	 * @throws CoreException 
	 */
	@After
	public void tearDown() throws CoreException  {
		this.testProject.dispose();
	}

	/**
	 * Test method for 
	 * {@link PushUpdateCommand#checkConfiguration()}.
	 * Should have thrown an exception. Command is not properly configured.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws BodhiClientException 
	 * @throws BodhiClientLoginException 
	 * @throws CommandMisconfiguredException 
	 * @throws CommandListenerException 
	 */
	@Test(expected=CommandMisconfiguredException.class)
	public void testCheckConfiguration() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandListenerException, CommandMisconfiguredException, BodhiClientLoginException, BodhiClientException  {
		PushUpdateCommand pushUpdateCommand = (PushUpdateCommand) packager
				.getCommandInstance(PushUpdateCommand.ID);
		pushUpdateCommand.call(new NullProgressMonitor());
	}
	
	/**
	 * Basic test for {@link PushUpdateCommand}.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws BodhiClientException 
	 * @throws BodhiClientLoginException 
	 * @throws CommandMisconfiguredException 
	 * @throws CommandListenerException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * 
	 */
	@Test
	public void canPushUpdate() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandListenerException, CommandMisconfiguredException, BodhiClientLoginException, BodhiClientException, ClientProtocolException, IOException  {
		PushUpdateCommand pushUpdateCommand = (PushUpdateCommand) packager
				.getCommandInstance(PushUpdateCommand.ID);
		URL bodhiServerURL = new URL("http://admin.stg.fedoraproject.org/updates");
		
		final HttpClient mockClient = createMock(HttpClient.class);
		HttpResponse mockResponse = createMock(HttpResponse.class);
		StatusLine mockStatus = createMock(StatusLine.class);
		HttpEntity mockEntity = createMock(HttpEntity.class);
		BodhiClient client = new BodhiClient(bodhiServerURL) {
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
				new ByteArrayInputStream("{tg_flash:\"Update successfully created\" , updates: [ {builds: [{package:{name=\"ed\"} , nvr=\"ed-1.5-3.fc15\"}] }] }"
						.getBytes())).anyTimes();
		expect(mockEntity.isStreaming()).andReturn(false).anyTimes();
		expect(mockClient.getConnectionManager()).andReturn(
				createNiceMock(ClientConnectionManager.class)).anyTimes();
		replay(mockClient);
		replay(mockResponse);
		replay(mockStatus);
		replay(mockEntity);
		
		String[] builds = { PACKAGE_UPDATE_NVR };
		// setup the command and call it
		PushUpdateResult result = pushUpdateCommand.client(client).bugs(PushUpdateCommand.NO_BUGS)
				.usernamePassword(BODHI_ADMIN_USERNAME, BODHI_ADMIN_PASSWORD)
				.comment("Test update. Please disregard!").release("F15").requestType(RequestType.TESTING)
				.updateType(UpdateType.ENHANCEMENT).builds(builds).call(new NullProgressMonitor());
		assertNotNull(result.getUpdateResponse());
		assertTrue(result.wasSuccessful());
		assertEquals("ed", result.getUpdateResponse().getUpdates()[0].getBuilds()[0].getPkg().getName());
		assertEquals(PACKAGE_UPDATE_NVR, result.getUpdateName());
	}

}
