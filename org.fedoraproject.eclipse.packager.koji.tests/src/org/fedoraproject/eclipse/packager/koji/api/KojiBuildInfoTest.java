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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for KojiBuildInfo. Note that for a successful run of
 * these tests, one has to have valid Fedora certificates in ~/
 * 
 */
public class KojiBuildInfoTest extends KojiClientTest {
	
	/**
	 * Name-version-release of some successful build.
	 */
	private static final String EFP_NVR = "eclipse-fedorapackager-0.1.12-1.fc14";
	
	/**
	 * Name-version-release of some successful build.
	 */
	private static final String NON_EXISTING_NVR = "eclipse-fedorapackager-0.3.54-1.fc13";

	/**
	 * Raw map fixture
	 */
	private HashMap<String, Object> rawBuildinfoMap;
	
	@Before
	public void setUp() throws MalformedURLException  {
		super.setUp();
		rawBuildinfoMap = new HashMap<String, Object>();
		rawBuildinfoMap.put("state", new Integer(2));
		rawBuildinfoMap.put("task_id", new Integer(3333));
		rawBuildinfoMap.put("package_id", new Integer(9999));
		rawBuildinfoMap.put("package_name", "eclipse-fedorapackager");
		rawBuildinfoMap.put("epoch", new Integer(1));
		rawBuildinfoMap.put("version", "0.1.12");
		rawBuildinfoMap.put("release", "2.fc15");
		rawBuildinfoMap.put("nvr", "eclipse-fedorapackager-0.1.12-2.fc15");
	}
	
	/**
	 * Get build info from koji via XMLRPC. This test requires
	 * a valid .fedora.cert in ~/
	 * @throws KojiHubClientException 
	 * 
	 */
	@Test
	public void canGetBuildInfoFromKoji() throws KojiHubClientException {
		// Create a mock XML-RPC client
		final XmlRpcClient mockXmlRpcClinet = new XmlRpcClient(){
			@Override
			public Object execute(String methodName, @SuppressWarnings("rawtypes") List params) {
				if (methodName.equals("getBuild") && params.get(0).equals(EFP_NVR))
					return rawBuildinfoMap;

				return null;
			};
		};
		this.kojiClient.setXmlRpcClient(mockXmlRpcClinet);
		KojiBuildInfo info = kojiClient.getBuild(EFP_NVR);
		assertNotNull(info);
		assertEquals("2.fc15", info.getRelease());
		assertEquals("eclipse-fedorapackager", info.getPackageName());
		assertEquals("0.1.12", info.getVersion());
		assertFalse(info.isComplete());
		assertEquals(3333, info.getTaskId());
		assertEquals(2, info.getState());
		assertEquals(1, info.getEpoch());
		assertEquals("eclipse-fedorapackager-0.1.12-2.fc15", info.getNvr());
		assertEquals(9999, info.getPackageId());
		kojiClient.logout();
	}
	
	/**
	 * Get build non-existent build from koji via XMLRPC. This test requires
	 * a valid .fedora.cert in ~/
	 * @throws KojiHubClientException 
	 * 
	 */
	@Test
	public void canGetNonExistingBuildInfoFromKoji() throws KojiHubClientException {
		final XmlRpcClient mockXmlRpcClinet = new XmlRpcClient(){
			@Override
			public Object execute(String methodName, @SuppressWarnings("rawtypes") List params) {
				if (methodName.equals("getBuild") && params.get(0).equals(EFP_NVR))
					return rawBuildinfoMap;

				return null;
			};
		};
		this.kojiClient.setXmlRpcClient(mockXmlRpcClinet);
		KojiBuildInfo info = kojiClient.getBuild(NON_EXISTING_NVR);
		assertNull(info);
		kojiClient.logout();
	}
	
	
	/**
	 * Sanity Check. Offline KojiBuildInfo test using rawBuildInfoMap fixture.
	 * 
	 */
	@Test
	public void canParseBuildInfo() {
		KojiBuildInfo info = new KojiBuildInfo(rawBuildinfoMap);
		assertNotNull(info);
		assertEquals("2.fc15", info.getRelease());
		assertEquals("eclipse-fedorapackager", info.getPackageName());
		assertEquals("0.1.12", info.getVersion());
		assertTrue(!info.isComplete());
		assertEquals(3333, info.getTaskId());
		assertEquals(2, info.getState());
		assertEquals(1, info.getEpoch());
		assertEquals("eclipse-fedorapackager-0.1.12-2.fc15", info.getNvr());
		assertEquals(9999, info.getPackageId());
	}
}
