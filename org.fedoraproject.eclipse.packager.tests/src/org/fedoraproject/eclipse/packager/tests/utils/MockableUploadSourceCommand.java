package org.fedoraproject.eclipse.packager.tests.utils;

import org.apache.http.impl.client.CloseableHttpClient;
import org.fedoraproject.eclipse.packager.api.UploadSourceCommand;

public class MockableUploadSourceCommand extends UploadSourceCommand {
	
	protected CloseableHttpClient client;
	public static String ID = "MockableUploadSourceCommand";
	
	@Override
	protected CloseableHttpClient getClient() {
		return client;
	}
	
	public MockableUploadSourceCommand setClient(CloseableHttpClient client){
		this.client = client;
		return this;
	}
}
