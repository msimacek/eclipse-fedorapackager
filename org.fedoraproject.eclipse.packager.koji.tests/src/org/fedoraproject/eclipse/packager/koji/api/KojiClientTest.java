package org.fedoraproject.eclipse.packager.koji.api;

import java.net.MalformedURLException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.junit.Before;

public class KojiClientTest {

	protected static class MockKojiSSLHubClient extends KojiSSLHubClient {

		public MockKojiSSLHubClient()
				throws MalformedURLException {
			super("http://www.example.com");
		}
		
		@Override
		protected void setupXmlRpcClient() {
			// Do nothing the mock client was set in the constructor.
		};
		
		@Override
		protected SSLContext getInitializedSSLContext()
				throws GeneralSecurityException {
			return SSLContext.getInstance("SSL");
		}
		
		@Override
		protected void initSSLConnection(){}

		public void setXmlRpcClient(XmlRpcClient client){
			this.xmlRpcClient = client;
		}
		
		@Override
		public void logout() {}
	};
	
	protected MockKojiSSLHubClient kojiClient;

	@Before
	public void setUp() throws MalformedURLException{
		kojiClient = new MockKojiSSLHubClient();
	}
}
