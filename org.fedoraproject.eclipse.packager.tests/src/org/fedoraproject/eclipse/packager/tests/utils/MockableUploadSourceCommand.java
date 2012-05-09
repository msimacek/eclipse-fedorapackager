package org.fedoraproject.eclipse.packager.tests.utils;

import org.apache.http.client.HttpClient;
import org.fedoraproject.eclipse.packager.api.UploadSourceCommand;

public class MockableUploadSourceCommand extends UploadSourceCommand {
	
	protected HttpClient client;
	public static String ID = "MockableUploadSourceCommand";
	
	@Override
	protected HttpClient getClient() {
		return client;
	}
	
	public MockableUploadSourceCommand setClient(HttpClient client){
		this.client = client;
		return this;
	}
}
