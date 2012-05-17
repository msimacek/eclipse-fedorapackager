package org.fedoraproject.eclipse.packager.koji.internal.utils;

import java.net.MalformedURLException;
import java.util.HashMap;

import org.fedoraproject.eclipse.packager.koji.api.IKojiHubClient;
import org.fedoraproject.eclipse.packager.koji.api.KojiSSLHubClient;

/**
 * This is the factory for creating Koji Clients
 *
 */
public class KojiClientFactory {

	/**
	 * A hash map to keep the koji clients created indexed by the URL they are
	 * connected to. In most cases there will only be one client created.
	 */
	private static HashMap<String, IKojiHubClient> clients = new HashMap<String, IKojiHubClient>(
			1);

	/**
	 * Create a hub client connected to the given URL.
	 *
	 * @param serverURL
	 *            The URL of the Koji server.
	 * @throws MalformedURLException
	 *             If the koji hub URL preference was invalid.
	 * @return The koji client.
	 */
	public static IKojiHubClient getHubClient(String serverURL)
			throws MalformedURLException {

		IKojiHubClient client = clients.get(serverURL);

		if (client == null) {
			client = new KojiSSLHubClient(serverURL);
			clients.put(serverURL, client);
		}

		return client;
	}

}
