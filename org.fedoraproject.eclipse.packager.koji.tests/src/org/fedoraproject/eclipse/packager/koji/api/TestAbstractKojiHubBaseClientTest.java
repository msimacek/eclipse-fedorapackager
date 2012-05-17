package org.fedoraproject.eclipse.packager.koji.api;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.junit.Test;

public class TestAbstractKojiHubBaseClientTest {

	@Test
	public void testGetRepo() throws KojiHubClientException,
			MalformedURLException {
		final String tag = "f18-build";

		final HashMap<String, Object> testMap = new HashMap<String, Object>();
		testMap.put("id", 55);
		testMap.put("creation_time", "yesterday");

		ArrayList<Object> params = new ArrayList<Object>();
		params.add(tag);

		// Mock xmlRpcClient
		final XmlRpcClient mockXmlRpcClient = new XmlRpcClient() {
			@Override
			public Object execute(String methodName,
					@SuppressWarnings("rawtypes") List params) {
				if (methodName.equals("getRepo") && params.get(0).equals(tag))
					return testMap;

				return null;
			}
		};

		// Create a koji client using the mock xmlRpcClient
		AbstractKojiHubBaseClient mockKojiClient = new AbstractKojiHubBaseClient(
				"http://example.com") {
			public HashMap<?, ?> login() {
				return null;
			}

			protected void setupXmlRpcClient() {
				xmlRpcClient = mockXmlRpcClient;
			}
		};

		// Test getRepo
		KojiRepoInfo info = mockKojiClient.getRepo(tag);
		KojiRepoInfo testInfo = new KojiRepoInfo(testMap);
		assertTrue("Repository information was retrieved correctly",
				info.equals(testInfo));
	}

}
