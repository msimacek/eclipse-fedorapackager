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
package org.fedoraproject.eclipse.packager.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.linuxtools.rpm.core.utils.RPMQuery;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.FedoraSSL;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.SourcesFile;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FileAvailableInLookasideCacheException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidUploadFileException;
import org.fedoraproject.eclipse.packager.api.errors.UploadFailedException;
import org.fedoraproject.eclipse.packager.internal.utils.httpclient.CoutingRequestEntity;
import org.fedoraproject.eclipse.packager.internal.utils.httpclient.IRequestProgressListener;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * A class used to execute a {@code upload sources} command. It has setters for
 * all supported options and arguments of this command and a
 * {@link #call(IProgressMonitor)} method to finally execute the command. Each
 * instance of this class should only be used for one invocation of the command
 * (meaning: one call to {@link #call(IProgressMonitor)})
 *
 */
public class UploadSourceCommand extends
		FedoraPackagerCommand<UploadSourceResult> {

	/**
	 * Response body text if a resource is available in the lookaside cache.
	 */
	public static final String RESOURCE_AVAILABLE = "available"; //$NON-NLS-1$
	/**
	 * Response body text if a resource is missing from the lookaside cache.
	 * I.e. would need to be uploaded.
	 */
	public static final String RESOURCE_MISSING = "missing"; //$NON-NLS-1$
	/**
	 * The unique ID of this command.
	 */
	public static final String ID = "UploadSourceCommand"; //$NON-NLS-1$

	// Parameter name constants
	private static final String FILENAME_PARAM_NAME = "filename"; //$NON-NLS-1$
	private static final String CHECKSUM_PARAM_NAME = "md5sum"; //$NON-NLS-1$
	private static final String PACKAGENAME_PARAM_NAME = "name"; //$NON-NLS-1$
	private static final String FILE_PARAM_NAME = "file"; //$NON-NLS-1$

	// Use 30 sec connection timeout
	private static final int CONNECTION_TIMEOUT = 30000;

	// The file to upload
	private File fileToUpload;
	// State info if Fedora SSL should be used or not
	private boolean fedoraSslEnabled = false;
	// State info if a basic all trusting https enabled client
	// should be used or not
	private boolean trustAllSSLEnabled = false;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand#initialize
	 * (org.fedoraproject.eclipse.packager.FedoraProjectRoot)
	 */
	@Override
	public void initialize(IProjectRoot projectRoot)
			throws FedoraPackagerCommandInitializationException {
		super.initialize(projectRoot);
	}

	/**
	 * @param uploadURL
	 *            the uploadURL to set. Optional.
	 * @return this instance.
	 * @throws MalformedURLException
	 *             If the provided URL was not well formed.
	 */
	public UploadSourceCommand setUploadURL(String uploadURL)
			throws MalformedURLException {
		this.projectRoot.getLookAsideCache().setUploadUrl(uploadURL);
		return this;
	}

	/**
	 * Set to true if upload host requires Fedora SSL authentication.
	 *
	 * @param newValue
	 *            True if SSL is required, false otherwise.
	 * @return this instance.
	 */
	public UploadSourceCommand setFedoraSSLEnabled(boolean newValue) {
		this.fedoraSslEnabled = newValue;
		return this;
	}

	/**
	 * Set to true if a basic accept-all hostname verifier should be used.
	 * Useful for {@code https} based connections, which do not require
	 * authentication via SSL.
	 *
	 * @param newValue
	 *            True if a basic accept-all hostname verifier should be used,
	 *            false otherwise.
	 * @return this instance.
	 */
	public UploadSourceCommand setAcceptAllSSLEnabled(boolean newValue) {
		this.trustAllSSLEnabled = newValue;
		return this;
	}

	/**
	 * Setter for the file to be uploaded.
	 *
	 * @param fileToUpload
	 *            The file to be uploaded.
	 * @return this instance.
	 * @throws InvalidUploadFileException
	 *             If the upload file candidate is an invalid file.
	 */
	public UploadSourceCommand setFileToUpload(File fileToUpload)
			throws InvalidUploadFileException {
		if (!FedoraPackagerUtils.isValidUploadFile(fileToUpload)) {
			throw new InvalidUploadFileException(NLS.bind(
					FedoraPackagerText.UploadSourceCommand_uploadFileInvalid,
					fileToUpload.getName()));
		}
		this.fileToUpload = fileToUpload;
		return this;
	}

	/**
	 * Implementation of the {@code UploadSources} command.
	 *
	 * @throws FileAvailableInLookasideCacheException
	 *             If the to-be-uploaded file is already available in the
	 *             lookaside cache.
	 * @throws CommandListenerException
	 *             If a listener caused an error.
	 * @throws UploadFailedException
	 *             If the upload failed for some reason.
	 */
	@Override
	public UploadSourceResult call(IProgressMonitor subMonitor)
			throws FileAvailableInLookasideCacheException,
			CommandListenerException, UploadFailedException {
		callPreExecListeners();
		if (this.fileToUpload == null) {
			throw new IllegalStateException(
					FedoraPackagerText.UploadSourceCommand_uploadFileUnspecified);
		}
		// Check if source is available, first.
		checkSourceAvailable();
		// Ok, file is missing. Perform the actual upload.
		UploadSourceResult result = upload(subMonitor);
		callPostExecListeners();
		return result;
	}

	/**
	 * Check if upload file has already been uploaded. Do nothing, if file is
	 * missing.
	 *
	 * @throws UploadFailedException
	 *             If something went wrong sending/receiving the request to/from
	 *             the lookaside cache.
	 * @throws FileAvailableInLookasideCacheException
	 *             If the upload candidate file is already present in the
	 *             lookaside cache.
	 */
	private void checkSourceAvailable()
			throws FileAvailableInLookasideCacheException,
			UploadFailedException {
		try (CloseableHttpClient client = getClient()){
			String uploadURI = null;
			uploadURI = this.projectRoot.getLookAsideCache().getUploadUrl()
					.toString();
			assert uploadURI != null;

			HttpPost post = new HttpPost(uploadURI);

			// Construct the multipart POST request body.
			MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
			reqEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			reqEntity.addTextBody(FILENAME_PARAM_NAME, fileToUpload.getName());
			reqEntity.addTextBody(PACKAGENAME_PARAM_NAME, RPMQuery.eval(projectRoot.getSpecfileModel().getName()).trim());
			reqEntity.addTextBody(CHECKSUM_PARAM_NAME, SourcesFile.calculateChecksum(fileToUpload));

			post.setEntity(reqEntity.build());

			HttpResponse response = client.execute(post);
			HttpEntity resEntity = response.getEntity();
			int returnCode = response.getStatusLine().getStatusCode();

			if (returnCode != HttpURLConnection.HTTP_OK) {
				throw new UploadFailedException(response.getStatusLine()
						.getReasonPhrase());
			}
			String resString = ""; //$NON-NLS-1$
			if (resEntity != null) {
				try {
					resString = parseResponse(resEntity);
				} catch (IOException e) {
					// ignore
				}
				EntityUtils.consume(resEntity); // clean up resources
			}
			// If this file has already been uploaded bail out
			if (resString.toLowerCase().equals(RESOURCE_AVAILABLE)) {
				throw new FileAvailableInLookasideCacheException(
						fileToUpload.getName());
			} else if (resString.toLowerCase().equals(RESOURCE_MISSING)) {
				// check passed
				return;
			} else {
				// something is fishy
				throw new UploadFailedException(
						FedoraPackagerText.somethingUnexpectedHappenedError);
			}

		} catch (IOException|CoreException e) {
			throw new UploadFailedException(e.getMessage(), e);
		}
	}

	/**
	 * Upload a missing file to the lookaside cache.
	 *
	 * Pre: upload file is missing as determined by
	 * {@link UploadSourceCommand#checkSourceAvailable()}.
	 *
	 * @param subMonitor
	 *            Monitor to show progress.
	 * @return The result of the upload.
	 */
	private UploadSourceResult upload(final IProgressMonitor subMonitor)
			throws UploadFailedException {
		try (CloseableHttpClient client = getClient()) {
			String uploadUrl = projectRoot.getLookAsideCache().getUploadUrl()
					.toString();

			HttpPost post = new HttpPost(uploadUrl);
			// For the actual upload we must not provide the
			// "filename" parameter (FILENAME_PARAM_NAME). Otherwise,
			// the file won't be stored in the lookaside cache.
			MultipartEntityBuilder builder = MultipartEntityBuilder.create(); 
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.addBinaryBody(FILE_PARAM_NAME, fileToUpload);
			builder.addTextBody(PACKAGENAME_PARAM_NAME, projectRoot.getSpecfileModel().getName());
			builder.addTextBody(CHECKSUM_PARAM_NAME, SourcesFile.calculateChecksum(fileToUpload));
			HttpEntity reqEntity = builder.build();
			// Not sure why it's ~ content-length * 2, but that's what it is...
			final long totalsize = reqEntity.getContentLength() * 2;
			subMonitor
					.beginTask(
							NLS.bind(
									FedoraPackagerText.UploadSourceCommand_uploadingFileSubTaskName,
									fileToUpload.getName()), 100 /*
																 * use
																 * percentage
																 */);
			subMonitor.worked(0);

			// Custom listener for progress reporting of the file upload
			IRequestProgressListener progL = new IRequestProgressListener() {

				private int count = 0;
				private int worked = 0;
				private int updatedWorked = 0;

				@Override
				public void transferred(final long bytesWritten) {
					count++;
					worked = updatedWorked;
					if (subMonitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					// Since this listener may be called *a lot*, don't
					// do the calculation to often.
					if ((count % 1024) == 0) {
						updatedWorked =
						// multiply by 85 (instead of 100) since upload
						// progress cannot be 100% accurate.
						(int) ((double) bytesWritten / totalsize * 85);
						if (updatedWorked > worked) {
							worked = updatedWorked;
							subMonitor.worked(updatedWorked);
						}
					}
				}
			};
			// We need to wrap the entity which we want to upload in our
			// custom entity, which allows for progress listening.
			CoutingRequestEntity countingEntity = new CoutingRequestEntity(
					reqEntity, progL);
			post.setEntity(countingEntity);

			// TODO: This may throw some certificate exception. We should
			// handle this case and throw a specific exception in order to
			// report this to the user. I.e. advise to use use $ fedora-cert -n
			HttpResponse response = client.execute(post);

			subMonitor.done();
			return new UploadSourceResult(response);
		} catch (IOException e) {
			throw new UploadFailedException(e.getMessage(), e);
		}
	}

	/**
	 * @return A properly configured HTTP client instance
	 * @throws UploadFailedException If an IO or security exception appeared.
	 */
	protected CloseableHttpClient getClient() throws UploadFailedException {
		try {
			if (fedoraSslEnabled) {
				// user requested a Fedora SSL enabled client
				return fedoraSslEnable();
			} else if (trustAllSSLEnabled) {
				// use an trust-all SSL enabled client
				return FedoraPackagerUtils.trustAllSslEnable();
			}
		} catch (GeneralSecurityException | IOException e) {
			throw new UploadFailedException(e.getMessage(), e);
		}
		// Set up client with proper timeout
		HttpClientBuilder builder = HttpClientBuilder.create();
		RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(CONNECTION_TIMEOUT).build();
		builder.setDefaultRequestConfig(config);
		return builder.build();
	}

	/**
	 * Wrap a basic HttpClient object in a Fedora SSL enabled HttpClient (which
	 * includes Fedora SSL authentication cert) object.
	 *
	 * @return The SSL wrapped HttpClient.
	 * @throws GeneralSecurityException
	 *             The method fails for security reasons.
	 * @throws IOException
	 *             If method cannot use streams properly.
	 */
	private static CloseableHttpClient fedoraSslEnable()
			throws GeneralSecurityException, FileNotFoundException, IOException {

		// Get a SSL related instance for setting up SSL connections.
		FedoraSSL fedoraSSL = new FedoraSSL();
		HttpClientBuilder builder = HttpClientBuilder.create();
		SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(
				fedoraSSL.getInitializedSSLContext(),
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		builder.setSSLSocketFactory(sslConnectionFactory);
		Registry<ConnectionSocketFactory> registry = RegistryBuilder
				.<ConnectionSocketFactory> create()
				.register("https", sslConnectionFactory) //$NON-NLS-1$
				.build();

		HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(
				registry);

		builder.setConnectionManager(ccm);
		return builder.build();
	}

	/**
	 * Helper to read response from response entity.
	 *
	 * @param responseEntity
	 *            The response being parsed.
	 * @return The parsed response.
	 * @throws IOException
	 *             If method cannot use streams properly.
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

}
