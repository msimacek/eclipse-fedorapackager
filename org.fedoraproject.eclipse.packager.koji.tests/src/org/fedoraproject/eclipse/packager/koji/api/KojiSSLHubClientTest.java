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
package org.fedoraproject.eclipse.packager.koji.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.junit.Test;

/**
 * Unit test for the Koji hub client class. Note that for a successful run of
 * these tests, one has to have valid Fedora certificates in ~/
 * 
 */
public class KojiSSLHubClientTest extends KojiClientTest {
	
	/**
	 * Name-version-release of some successful build.
	 */
	private static final String EFP_NVR = "eclipse-fedorapackager-0.1.12-1.fc15";
	/**
	 * Some known to be working scm-URL
	 */
	private static final String EFP_SCM_URL = "git://pkgs.fedoraproject.org/eclipse-fedorapackager.git?#302d36c1427a0d8578d0a1d88b4c9337a4407dde";

	/**
	 * Log on to Koji using SSL authentication.
	 * This test requires proper certificates to be set up.
	 * @throws KojiHubClientLoginException 
	 * 
	 */
	@Test
	public void canLoginUsingSSLCertificate() throws KojiHubClientLoginException  {
		// Mock session data
		final HashMap<String, Object> mockSessionData = new HashMap<>();
		mockSessionData.put("session-id", new Integer(99));
		mockSessionData.put("session-key", "sessionKey");
		
		// Create a mock XML-RPC client
		final XmlRpcClient mockXmlRpcClinet = new XmlRpcClient(){
			@Override
			public Object execute(String methodName, @SuppressWarnings("rawtypes") List params) {
				if (methodName.equals("sslLogin"))
					return mockSessionData;

				return null;
			};
		};

		kojiClient.setXmlRpcClient(mockXmlRpcClinet);
		
		// Logging in to koji should return session data
		HashMap<?, ?> sessionData = kojiClient.login();
		assertNotNull(sessionData);
		assertNotNull(sessionData.get("session-id"));
		assertTrue(sessionData.get("session-id") instanceof Integer);
		assertNotNull(sessionData.get("session-key"));
	}
	
	/**
	 * Get build info test.
	 * @throws KojiHubClientException 
	 * 
	 */
	@Test
	public void canGetBuildInfo() throws KojiHubClientException  {
		
		final HashMap<String, Object> mockBuildInfo = new HashMap<>();
		mockBuildInfo.put(KojiBuildInfo.KEY_TASK_ID, new Integer(99));
		mockBuildInfo.put(KojiBuildInfo.KEY_RELEASE, "1.fc15");
		mockBuildInfo.put(KojiBuildInfo.KEY_PACKAGE_NAME, "eclipse-fedorapackager");
		mockBuildInfo.put(KojiBuildInfo.KEY_PACKAGE_ID, new Integer(99));
		mockBuildInfo.put(KojiBuildInfo.KEY_NVR, EFP_NVR);
		mockBuildInfo.put(KojiBuildInfo.KEY_VERSION, "0.1.12");
		mockBuildInfo.put(KojiBuildInfo.KEY_STATE, new Integer(1));
		
		// Create a mock XML-RPC client
		final XmlRpcClient mockXmlRpcClinet = new XmlRpcClient(){
			@Override
			public Object execute(String methodName, @SuppressWarnings("rawtypes") List params) {
				if (methodName.equals("getBuild") && params.get(0).equals(EFP_NVR))
					return mockBuildInfo;

				return null;
			};
		};
		this.kojiClient.setXmlRpcClient(mockXmlRpcClinet);

		// get build info for eclipse-fedorapackager-0.1.12-1.fc15
		KojiBuildInfo info = kojiClient.getBuild(EFP_NVR);
		assertNotNull(info);
		assertTrue(info.getRelease().equals("1.fc15"));
		assertTrue(info.getPackageName().equals("eclipse-fedorapackager"));
		assertTrue(info.getVersion().equals("0.1.12"));
		assertTrue(info.isComplete());
	}
	
	/**
	 * Push scratch build test.
	 * @throws KojiHubClientException 
	 * 
	 */
	@Test
	public void canPushScratchBuild() throws KojiHubClientException  {
		// Create a mock XML-RPC client
		final XmlRpcClient mockXmlRpcClinet = new XmlRpcClient(){
			@Override
			public Object execute(String methodName, @SuppressWarnings("rawtypes") List params) {
				if (methodName.equals("build"))
					return "99";

				return null;
			};
		};
		this.kojiClient.setXmlRpcClient(mockXmlRpcClinet);

		// get build info for eclipse-fedorapackager-0.1.13-fc15
		boolean isScratchBuild = true;
		List<String> sourceLocation = new ArrayList<>();
		sourceLocation.add(EFP_SCM_URL);
		int taskId = kojiClient.build("dist-rawhide", sourceLocation, new String[] {EFP_NVR}, isScratchBuild)[0];
		assertNotNull(taskId);
	}

}
