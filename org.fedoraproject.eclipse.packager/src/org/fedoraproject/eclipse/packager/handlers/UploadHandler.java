/*******************************************************************************
 * Copyright (c) 2010 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.ssl.HttpSecureProtocol;
import org.apache.commons.ssl.TrustMaterial;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.FedoraProjectRoot;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.Messages;
import org.fedoraproject.eclipse.packager.SSLUtils;
import org.fedoraproject.eclipse.packager.SourcesFile;

/**
 * Class responsible for uploading source files (VCS independent bits).
 * 
 * @author Red Hat inc.
 *
 */
public class UploadHandler extends WGetHandler {

	@Override
	/**
	 *  Performs upload of sources (independent of VCS used), updates "sources"
	 *  file and performs necessary CVS operations to bring branch in sync.
	 *  Checks if sources have changed.
	 *  
	 */
	public Object execute(final ExecutionEvent e) throws ExecutionException {

		final IResource resource = FedoraHandlerUtils.getResource(e);
		final FedoraProjectRoot fedoraProjectRoot = FedoraHandlerUtils.getValidRoot(resource);
		final SourcesFile sourceFile = fedoraProjectRoot.getSourcesFile();
		final IFpProjectBits projectBits = FedoraHandlerUtils.getVcsHandler(resource);
		// do tasks as job
		Job job = new Job(Messages.getString("UploadHandler.taskName")) { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				monitor.beginTask(Messages.getString("UploadHandler.taskName"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

				// ensure file has changed if already listed in sources
				Map<String, String> sources = sourceFile.getSources();
				String filename = resource.getName();
				if (sources.containsKey(filename)
						&& SourcesFile
								.checkMD5(sources.get(filename), resource)) {
					// file already in sources
					return handleOK(NLS.bind(Messages
							.getString("UploadHandler.versionExists"), filename) //$NON-NLS-1$
							, true);
				}

				// Do file sanity checks (non-empty, file extensions etc.)
				final File toAdd = resource.getLocation().toFile();
				if (!FedoraHandlerUtils.isValidUploadFile(toAdd)) {
					return handleOK(NLS.bind(org.fedoraproject.eclipse.packager.Messages
							.getString("UploadHandler.invalidFile"), //$NON-NLS-1$
							toAdd.getName()), true);
				}

				// Do the file uploading
				IStatus result = performUpload(toAdd, filename, monitor,
						fedoraProjectRoot);

				if (result.isOK()) {
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
				}

				// Update sources file
				result = updateSources(sourceFile, toAdd);
				if (!result.isOK()) {
					// fail updating sources file
					return handleError(Messages
							.getString("UploadHandler.failUpdatSourceFile")); //$NON-NLS-1$
				}

				// Handle CVS specific stuff; Update .cvsignore
				result = updateIgnoreFile(fedoraProjectRoot.getIgnoreFile(), toAdd);
				if (!result.isOK()) {
					// fail updating sources file
					return handleError(Messages
							.getString("UploadHandler.failVCSUpdate")); //$NON-NLS-1$
				}

				// Do CVS update
				result = projectBits.updateVCS(fedoraProjectRoot, monitor);
				if (result.isOK()) {
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
				}
				return result;
			}

		};
		job.setUser(true);
		job.schedule();
		return null;
	}
	
	/**
	 * Upload source files as job.
	 * 
	 * @param toAdd
	 * @param filename
	 * @param monitor
	 * @param fedoraProjectRoot
	 * @return
	 */
	protected IStatus performUpload(final File toAdd, final String filename,
			IProgressMonitor monitor, FedoraProjectRoot fedoraProjectRoot) {
		IStatus status;
		try {
			registerProtocol();

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			// first check remote status to see if file is already uploaded
			monitor.subTask(NLS.bind(
					Messages.getString("UploadHandler.3"), filename)); //$NON-NLS-1$
			HttpClient client = new HttpClient();
			client.getHttpConnectionManager().getParams()
					.setConnectionTimeout(30000);
			PostMethod postMethod = new PostMethod(uploadURL);
			NameValuePair[] data = {
					new NameValuePair(
							"name", fedoraProjectRoot.getSpecfileModel().getName()), //$NON-NLS-1$
					new NameValuePair("md5sum", SourcesFile.getMD5(toAdd)), //$NON-NLS-1$
					new NameValuePair("filename", filename) }; //$NON-NLS-1$
			postMethod.setRequestBody(data);
			int returnCode = client.executeMethod(postMethod);
			if (returnCode != HttpURLConnection.HTTP_OK) {
				status = handleError(NLS.bind(
						Messages.getString("UploadHandler.33"), filename, returnCode)); //$NON-NLS-1$
			} else {
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				InputStream responseStream = postMethod
						.getResponseBodyAsStream();
				String response = parseResponse(responseStream);

				// if we're in debug mode, forget this check
				if (response.toLowerCase().equals("available") && !debug) { //$NON-NLS-1$
					status = handleOK(
							NLS.bind(
									Messages.getString("UploadHandler.6"), filename), true); //$NON-NLS-1$
				} else if (response.toLowerCase().equals("missing") || debug) { //$NON-NLS-1$
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					monitor.subTask(NLS.bind(
							Messages.getString("UploadHandler.9"), filename)); //$NON-NLS-1$
					status = upload(toAdd, fedoraProjectRoot);
				} else {
					status = handleError(response);
				}
			}
		} catch (HttpException e) {
			e.printStackTrace();
			status = handleError(e);
		} catch (IOException e) {
			e.printStackTrace();
			status = handleError(e);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			status = handleError(e);
		}

		return status;
	}

	protected void registerProtocol() throws GeneralSecurityException,
			IOException, NoSuchAlgorithmException, KeyStoreException,
			KeyManagementException, CertificateException {
		HttpSecureProtocol protocol = new HttpSecureProtocol();
		protocol.setKeyMaterial(SSLUtils.getKeyMaterial());
		protocol.setTrustMaterial(TrustMaterial.TRUST_ALL);
		Protocol.registerProtocol("https", new Protocol("https", //$NON-NLS-1$ //$NON-NLS-2$
				(ProtocolSocketFactory) protocol, 443));
	}

	protected IStatus upload(File file, FedoraProjectRoot fedoraProjectRoot) {
		IStatus status;
		byte[] bytes = new byte[(int) file.length()];
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);

			String filename = file.getName();

			fis.read(bytes);

			HttpClient client = new HttpClient();
			client.getHttpConnectionManager().getParams()
					.setConnectionTimeout(30000);
			PostMethod postMethod = new PostMethod(uploadURL);

			Part[] data = { new StringPart("name", fedoraProjectRoot.getSpecfileModel().getName()), //$NON-NLS-1$
					new StringPart("md5sum", SourcesFile.getMD5(file)), //$NON-NLS-1$
					new FilePart("file", file) }; //$NON-NLS-1$

			postMethod.setRequestEntity(new MultipartRequestEntity(data,
					postMethod.getParams()));

			int code = client.executeMethod(postMethod);
			if (code != HttpURLConnection.HTTP_OK) {
				status = handleError(NLS
						.bind(Messages.getString("UploadHandler.33"), filename, postMethod.getStatusLine())); //$NON-NLS-1$
			} else {
				status = Status.OK_STATUS;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			status = handleError(e);
		} catch (HttpException e) {
			e.printStackTrace();
			status = handleError(e);
		} catch (IOException e) {
			e.printStackTrace();
			status = handleError(e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
					status = handleError(e);
				}
			}
		}
		return status;
	}

	protected String parseResponse(InputStream responseStream)
			throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				responseStream));

		String responseText = ""; //$NON-NLS-1$
		String line;
		try {
			line = br.readLine();
			while (line != null) {
				responseText += line + "\n"; //$NON-NLS-1$
				line = br.readLine();
			}
		} finally {
			br.close();
		}

		return responseText.trim();
	}
	
	/**
	 * Update the ignore file .cvsignore or .gitignore file. Appends to file.
	 * 
	 * @param ignoreFile
	 * @param toAdd
	 * @return
	 */
	protected IStatus updateIgnoreFile(File ignoreFile, File toAdd) {
		return updateIgnoreFile(ignoreFile, toAdd, false);
	}

	/**
	 * Actually writes to .cvsignore. ATM this method is never called with
	 * <code>forceOverwrite</code> set to true.
	 * 
	 * @param cvsignore
	 * @param toAdd
	 * @param forceOverwrite
	 * @return Status of the performed operation.
	 */
	private IStatus updateIgnoreFile(File ignoreRile, File toAdd,
			boolean forceOverwrite) {
		IStatus status;
		String filename = toAdd.getName();
		ArrayList<String> ignoreFiles = new ArrayList<String>();
		BufferedReader br = null;
		PrintWriter pw = null;
		try {
			if (forceOverwrite) {
				pw = new PrintWriter(new FileWriter(ignoreRile, false));
				pw.println(filename);
				status = Status.OK_STATUS;
			} else {
				// only append to file if not already present
				br = new BufferedReader(new FileReader(ignoreRile));

				String line = br.readLine();
				while (line != null) {
					ignoreFiles.add(line);
					line = br.readLine();
				}

				if (!ignoreFiles.contains(filename)) {
					pw = new PrintWriter(new FileWriter(ignoreRile, true));
					pw.println(filename);
				}
				status = Status.OK_STATUS;
			}
		} catch (IOException e) {
			e.printStackTrace();
			status = handleError(e);
		} finally {
			if (pw != null) {
				pw.close();
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					status = handleError(e);
				}
			}
		}
		return status;
	}
}