package org.fedoraproject.eclipse.packager.koji.api;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.junit.Test;

public class TestAbstractKojiHubBaseClientTest {

	@Test
	public void testGetRepo() throws KojiHubClientException,
			MalformedURLException {
		final String tag = "f18-build";

		final HashMap<String, Object> testMap = new HashMap<>();
		testMap.put("id", 55);
		testMap.put("creation_time", "yesterday");

		ArrayList<Object> params = new ArrayList<>();
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

	@Test
	public void testListBuildTags() throws MalformedURLException, KojiHubClientException{

		// Create a list of build targets
		HashMap<String, String> tag1 = new HashMap<>();
		tag1.put("build_tag_name", "tag1-build");
		HashMap<String, String> tag2 = new HashMap<>();
		tag2.put("build_tag_name", "tag2-build");
		final HashMap<?,?>[] testMapArray = {tag1,tag2};

		// Create an xmlRpcClient which returns the above list.
		final XmlRpcClient mockXmlRpcClient = new XmlRpcClient(){
			@Override
			public Object execute(String method, Object[] params) {
				if (method.equals("getBuildTargets"))
					return testMapArray;

				return null;
			}
		};

		// Create a Koji client which uses the above client
		AbstractKojiHubBaseClient client = new AbstractKojiHubBaseClient("http://example.com"){
			public HashMap<?,?> login(){
				return null;
			}
			
			protected void setupXmlRpcClient(){
				this.xmlRpcClient = mockXmlRpcClient;
			}
		};

		// Test listBuildTags
		Set<String> list = client.listBuildTags();
		assertTrue("first build root found", list.contains("tag1-build"));
		assertTrue("second build root found", list.contains("tag2-build"));
		
	}
}
