/*******************************************************************************
 * Copyright (c) 2010-2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.bodhi.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.bodhi.api.errors.BodhiClientException;
import org.fedoraproject.eclipse.packager.bodhi.api.errors.BodhiClientLoginException;
import org.fedoraproject.eclipse.packager.bodhi.deserializers.DateTimeDeserializer;
import org.fedoraproject.eclipse.packager.bodhi.fas.DateTime;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Bodhi JSON over HTTP client.
 */
public class BodhiClient {

	// Use 30 sec connection timeout
	private static final int CONNECTION_TIMEOUT = 30000;
	// Delimiter for pushing several builds as one update
	private static final String BUILDS_DELIMITER = ","; //$NON-NLS-1$

	// Parameter name constants for login
	private static final String LOGIN_PARAM_NAME = "login"; //$NON-NLS-1$
	private static final String LOGIN_PARAM_VALUE = "Login"; //$NON-NLS-1$
	private static final String USERNAME_PARAM_NAME = "user_name"; //$NON-NLS-1$
	private static final String PASSWORD_PARAM_NAME = "password"; //$NON-NLS-1$
	// Parameter name constants for login
	private static final String BUILDS_PARAM_NAME = "builds"; //$NON-NLS-1$
	private static final String TYPE_PARAM_NAME = "type_"; //$NON-NLS-1$
	private static final String REQUEST_PARAM_NAME = "request"; //$NON-NLS-1$
	private static final String BUGS_PARAM_NAME = "bugs"; //$NON-NLS-1$
	private static final String CSRF_PARAM_NAME = "_csrf_token"; //$NON-NLS-1$
	private static final String AUTOKARMA_PARAM_NAME = "autokarma"; //$NON-NLS-1$
	private static final String NOTES_PARAM_NAME = "notes"; //$NON-NLS-1$
	private static final String SUGGEST_REBOOT = "suggest_reboot"; //$NON-NLS-1$
	private static final String STABLE_KARMA = "stable_karma"; //$NON-NLS-1$
	private static final String UNSTABLE_KARMA = "unstable_karma"; //$NON-NLS-1$
	private static final String CLOSE_BUGS_WHEN_STABLE = "close_bugs"; //$NON-NLS-1$

	/**
	 * URL of the Bodhi server to which to connect to.
	 */
	public static final String BODHI_URL = "https://admin.fedoraproject.org/updates/"; //$NON-NLS-1$

	// We want JSON responses from the server. Use these constants in order
	// to set the "Accept: application/json" HTTP header accordingly.
	private static final String ACCEPT_HTTP_HEADER_NAME = "Accept"; //$NON-NLS-1$
	private static final String MIME_JSON = "application/json"; //$NON-NLS-1$

	// The http client to use for transport
	protected CloseableHttpClient httpclient;

	// The base URL to use for connections
	protected URL bodhiServerUrl;

	/**
	 * Create a Bodhi client instance. Establishes HTTP connection.
	 * 
	 * @param bodhiServerURL
	 *            The base URL to the Bodhi server.
	 * 
	 */
	public BodhiClient(URL bodhiServerURL) {
		this.httpclient = getClient();
		this.bodhiServerUrl = bodhiServerURL;
	}

	protected BodhiLoginResponse parseResult(HttpEntity resEntity)
			throws IOException {
		// Got a 200, response body is the JSON passed on from the
		// server.
		String jsonString = ""; //$NON-NLS-1$
		if (resEntity != null) {
			try {
				jsonString = parseResponse(resEntity);
			} catch (IOException e) {
				// ignore
			} finally {
				EntityUtils.consume(resEntity); // clean up resources
			}
		}
		// Deserialize from JSON
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(DateTime.class,
				new DateTimeDeserializer());
		Gson gson = gsonBuilder.create();
		return gson.fromJson(jsonString, BodhiLoginResponse.class);
	}

	/**
	 * Bodhi login with username and password.
	 * 
	 * @param username
	 *            The FAS username used to log in to Bodhi.
	 * @param password
	 *            The FAS password used to log in to Bodhi.
	 * @return The parsed response from the server or {@code null}.
	 * @throws BodhiClientLoginException
	 *             If some error occurred.
	 */
	public BodhiLoginResponse login(String username, String password)
			throws BodhiClientLoginException {
		BodhiLoginResponse result = null;
		try {
			HttpPost post = new HttpPost(getLoginUrl());
			// Add "Accept: application/json" HTTP header
			post.addHeader(ACCEPT_HTTP_HEADER_NAME, MIME_JSON);

			// Construct the multipart POST request body.
			MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
			reqEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			
			reqEntity.addTextBody(LOGIN_PARAM_NAME, LOGIN_PARAM_VALUE);
			reqEntity.addTextBody(USERNAME_PARAM_NAME, username);
			reqEntity.addTextBody(PASSWORD_PARAM_NAME, password);

			post.setEntity(reqEntity.build());

			HttpResponse response = httpclient.execute(post);
			HttpEntity resEntity = response.getEntity();
			int returnCode = response.getStatusLine().getStatusCode();

			if (returnCode != HttpURLConnection.HTTP_OK) {
				throw new BodhiClientLoginException(NLS.bind(
						"{0} {1}", response.getStatusLine().getStatusCode(), //$NON-NLS-1$
						response.getStatusLine().getReasonPhrase()), response);
			} else {
				result = parseResult(resEntity);
			}
		} catch (IOException e) {
			throw new BodhiClientLoginException(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Log out from Bodhi server.
	 * 
	 * @throws BodhiClientException
	 *             If an error occurred.
	 * 
	 */
	public void logout() throws BodhiClientException {
		try {
			HttpPost post = new HttpPost(getLogoutUrl());
			post.addHeader(ACCEPT_HTTP_HEADER_NAME, MIME_JSON);

			HttpResponse response = httpclient.execute(post);
			HttpEntity resEntity = response.getEntity();
			int returnCode = response.getStatusLine().getStatusCode();

			if (returnCode >= 400) {
				throw new BodhiClientException(NLS.bind(
						"{0} {1}", response.getStatusLine().getStatusCode(), //$NON-NLS-1$
						response.getStatusLine().getReasonPhrase()), response);
			} else {
				if (resEntity != null) {
					EntityUtils.consume(resEntity); // clean up resources
				}
			}
		} catch (IOException e) {
			throw new BodhiClientException(e.getMessage(), e);
		} finally {
			shutDownConnection();
		}
	}

	/**
	 * Shut down the connection of this client.
	 */
	public void shutDownConnection() {
		// When HttpClient instance is no longer needed,
		// shut down the connection manager to ensure
		// immediate deallocation of all system resources
		try {
			httpclient.close();
		} catch (IOException e) {
		    //ignore as there isn't much to be done
		}
	}

	/**
	 * Push a new Bodhi update for one or more builds (i.e. N-V-Rs).
	 * 
	 * @param builds
	 *            N-V-R's for which to push an update for
	 * @param type
	 *            One of "bugfix", "security", "enhancement", "newpackage".
	 * @param request
	 *            {@code testing} or {@code stable}.
	 * @param bugs
	 *            Numbers of bugs to close automatically (comma separated).
	 * @param notes
	 *            The comment for this update.
	 * @param csrfToken
	 *            The CSRF token for communicating with the Bodhi instance.
	 * @param suggestReboot
	 *            If a reboot is suggested after this update.
	 * @param enableKarmaAutomatism
	 *            If Karma automatism should be enabled.
	 * @param stableKarmaThreshold
	 *            The lower unpushing Karma threshold.
	 * @param unstableKarmaThreshold
	 *            The upper stable Karma threshold.
	 * @param closeBugsWhenStable
	 *            Flag which determines if bugs should get closed when the
	 *            update hits stable.
	 * @return The update response.
	 * @throws BodhiClientException
	 *             If some error occurred.
	 */
	public BodhiUpdateResponse createNewUpdate(String[] builds, String type, String request, String bugs, String notes,
			String csrfToken, boolean suggestReboot,
			boolean enableKarmaAutomatism, int stableKarmaThreshold,
			int unstableKarmaThreshold, boolean closeBugsWhenStable)
			throws BodhiClientException {
		try {
			HttpPost post = new HttpPost(getPushUpdateUrl());
			post.addHeader(ACCEPT_HTTP_HEADER_NAME, MIME_JSON);

			StringBuffer buildsNVR = new StringBuffer();
			for (int i = 0; i < (builds.length - 1); i++) {
				buildsNVR.append(builds[i]);
				buildsNVR.append(BUILDS_DELIMITER);
			}
			buildsNVR.append(builds[(builds.length - 1)]);
			String buildsParamValue = buildsNVR.toString();

			// Construct the multipart POST request body.
			MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
			reqEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			reqEntity.addTextBody(BUILDS_PARAM_NAME, buildsParamValue);
			reqEntity.addTextBody(TYPE_PARAM_NAME, type);
			reqEntity.addTextBody(REQUEST_PARAM_NAME, request);
			reqEntity.addTextBody(BUGS_PARAM_NAME, bugs);
			reqEntity.addTextBody(CSRF_PARAM_NAME, csrfToken);
			reqEntity.addTextBody(AUTOKARMA_PARAM_NAME, String.valueOf(enableKarmaAutomatism));
			reqEntity.addTextBody(NOTES_PARAM_NAME, notes);
			reqEntity.addTextBody(SUGGEST_REBOOT,String.valueOf(suggestReboot));
			reqEntity.addTextBody(STABLE_KARMA,String.valueOf(stableKarmaThreshold));
			reqEntity.addTextBody(UNSTABLE_KARMA,String.valueOf(unstableKarmaThreshold));
			reqEntity.addTextBody(CLOSE_BUGS_WHEN_STABLE, String.valueOf(closeBugsWhenStable));

			post.setEntity(reqEntity.build());

			HttpResponse response = httpclient.execute(post);
			HttpEntity resEntity = response.getEntity();
			int returnCode = response.getStatusLine().getStatusCode();

			if (returnCode != HttpURLConnection.HTTP_OK) {
				throw new BodhiClientException(NLS.bind(
						"{0} {1}", response.getStatusLine().getStatusCode(), //$NON-NLS-1$
						response.getStatusLine().getReasonPhrase()), response);
			} else {
				String rawJsonString = ""; //$NON-NLS-1$
				if (resEntity != null) {
					try {
						rawJsonString = parseResponse(resEntity);
					} catch (IOException e) {
						// ignore
					}
					EntityUtils.consume(resEntity); // clean up resources
				}
				// deserialize the result from the JSON response
				GsonBuilder gsonBuilder = new GsonBuilder();
				Gson gson = gsonBuilder.create();
				BodhiUpdateResponse result = gson.fromJson(rawJsonString,
						BodhiUpdateResponse.class);
				return result;
			}
		} catch (IOException e) {
			throw new BodhiClientException(e.getMessage(), e);
		}
	}

	/**
	 * @return A properly configured HTTP client instance
	 */
	protected CloseableHttpClient getClient() {
		try {
			return FedoraPackagerUtils.trustAllSslEnable();
		} catch (GeneralSecurityException e) {
			// Set up client with proper timeout
			HttpClientBuilder builder = HttpClientBuilder.create();
			RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(CONNECTION_TIMEOUT).build();
			builder.setDefaultRequestConfig(config);
			return builder.build();
		}
	}

	/**
	 * Helper to read response from response entity.
	 * 
	 * @param responseEntity
	 *            The response from the Bodhi instance.
	 * @return The parsed response.
	 * @throws IOException
	 *             If response could not be read or is read improperly.
	 */
	private static String parseResponse(HttpEntity responseEntity) throws IOException {

		String responseText = ""; //$NON-NLS-1$
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				responseEntity.getContent()))){
			String line;
			line = br.readLine();
			while (line != null) {
				responseText += line + "\n"; //$NON-NLS-1$
				line = br.readLine();
			}
		} 
		return responseText.trim();
	}

	/**
	 * 
	 * @return The login URL.
	 */
	private String getLoginUrl() {
		return this.bodhiServerUrl.toString() + "login"; //$NON-NLS-1$
	}

	/**
	 * 
	 * @return The URL to be used for pushing updates or {@code null}.
	 */
	private String getPushUpdateUrl() {
		return this.bodhiServerUrl.toString() + "save"; //$NON-NLS-1$		
	}

	/**
	 * 
	 * @return The URL to be used for pushing updates or {@code null}.
	 */
	private String getLogoutUrl() {
		return this.bodhiServerUrl.toString() + "logout"; //$NON-NLS-1$		
	}
}
