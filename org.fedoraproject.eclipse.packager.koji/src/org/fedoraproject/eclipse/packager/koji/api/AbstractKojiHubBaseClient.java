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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.api.errors.BuildAlreadyExistsException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.fedoraproject.eclipse.packager.koji.internal.utils.KojiTypeFactory;

/**
 * Koji Base client.
 */
public abstract class AbstractKojiHubBaseClient implements IKojiHubClient {

	/**
	 * Default constructor to set up a basic client.
	 * 
	 * @param kojiHubUrl
	 *            The koji hub URL.
	 * @throws MalformedURLException
	 *             If the hub URL was invalid.
	 */
	public AbstractKojiHubBaseClient(String kojiHubUrl)
			throws MalformedURLException {
		this.kojiHubUrl = new URL(kojiHubUrl);
		setupXmlRpcConfig();
		setupXmlRpcClient();
	}

	/**
	 * URL of the Koji Hub/XMLRPC interface
	 */
	protected URL kojiHubUrl;
	protected XmlRpcClientConfigImpl xmlRpcConfig;
	protected XmlRpcClient xmlRpcClient;

	/**
	 * Store session info in XMLRPC configuration.
	 * 
	 * @param sessionKey
	 * @param sessionID
	 */
	protected void saveSessionInfo(String sessionKey, String sessionID) {
		try {
			xmlRpcConfig.setServerURL(new URL(this.kojiHubUrl.toString()
					+ "?session-key=" + sessionKey //$NON-NLS-1$
					+ "&session-id=" + sessionID)); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			// ignore, URL should be valid
		}
	}

	/**
	 * Discard session info previously stored in server URL via
	 * {@link AbstractKojiHubBaseClient#saveSessionInfo(String, String)}.
	 */
	protected void discardSession() {
		xmlRpcConfig.setServerURL(this.kojiHubUrl);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fedoraproject.eclipse.packager.IKojiHubClient#sslLogin()
	 */
	@Override
	public abstract HashMap<?, ?> login() throws KojiHubClientLoginException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fedoraproject.eclipse.packager.IKojiHubClient#logout()
	 */
	@Override
	public void logout() throws KojiHubClientException {
		ArrayList<String> params = new ArrayList<String>();
		try {
			xmlRpcClient.execute("logout", params); //$NON-NLS-1$
		} catch (XmlRpcException e) {
			throw new KojiHubClientException(e);
		}
		discardSession();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fedoraproject.eclipse.packager.koji.api.IKojiHubClient#build(java
	 * .lang.String, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public int[] build(String target, List<?> scmURLs, String[] nvrs,
			boolean scratch) throws KojiHubClientException {
		ArrayList<Object> params;
		Map<String, Boolean> scratchParam = new HashMap<String, Boolean>();
		scratchParam.put("scratch", true); //$NON-NLS-1$

		if (nvrs != null && !scratch) {
			for (String nvr : nvrs) {
				KojiBuildInfo buildInfo = getBuild(nvr);
				if (buildInfo != null && buildInfo.isComplete()) {
					throw new BuildAlreadyExistsException(buildInfo.getTaskId());
				}
			}
		}
		int[] taskIds;
		Object result;
		try {
			if (scmURLs.get(0) instanceof String) {
				taskIds = new int[scmURLs.size()];
				for (int i = 0; i < scmURLs.size(); i++) {
					params = new ArrayList<Object>();
					params.add(scmURLs.get(i));
					params.add(target);
					if (scratch) {
						params.add(scratchParam);
					}
					result = xmlRpcClient.execute("build", params); //$NON-NLS-1$
					taskIds[i] = Integer.parseInt(result.toString());
				}
			} else {
				params = new ArrayList<Object>();
				params.add(scmURLs);
				params.add(target);
				result = xmlRpcClient.execute("chainBuild", params); //$NON-NLS-1$
				taskIds = new int[] { Integer.parseInt(result.toString()) };
			}
		} catch (XmlRpcException e) {
			throw new KojiHubClientException(e);
		}
		return taskIds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fedoraproject.eclipse.packager.koji.api.IKojiHubClient#getBuild(java
	 * .lang.String)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public KojiBuildInfo getBuild(String nvr) throws KojiHubClientException {
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(nvr);
		Map<String, Object> rawBuildInfo;
		try {
			rawBuildInfo = (Map<String, Object>) xmlRpcClient.execute(
					"getBuild", params); //$NON-NLS-1$
		} catch (XmlRpcException e) {
			throw new KojiHubClientException(e);
		}
		if (rawBuildInfo != null) {
			return new KojiBuildInfo(rawBuildInfo);
		} else {
			return null;
		}
	}

	/**
	 * Configure XMLRPC connection
	 */
	protected void setupXmlRpcConfig() {
		xmlRpcConfig = new XmlRpcClientConfigImpl();
		xmlRpcConfig.setServerURL(this.kojiHubUrl);
		xmlRpcConfig.setEnabledForExtensions(true);
		xmlRpcConfig.setConnectionTimeout(30000);
	}

	/**
	 * Set up XMLRPC client.
	 * 
	 * @throws IllegalStateException
	 *             If XMLRPC configuration hasn't been properly set up.
	 */
	protected void setupXmlRpcClient() throws IllegalStateException {
		if (xmlRpcConfig == null) {
			throw new IllegalStateException(KojiText.xmlRPCconfigNotInitialized);
		}
		xmlRpcClient = new XmlRpcClient();
		xmlRpcClient.setTypeFactory(new KojiTypeFactory(this.xmlRpcClient));
		xmlRpcClient.setConfig(this.xmlRpcConfig);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fedoraproject.eclipse.packager.koji.api.IKojiHubClient#uploadFile
	 * (java.lang.String, java.lang.String, int, java.lang.String, int,
	 * java.lang.String)
	 */
	@Override
	public boolean uploadFile(String path, String name, int size,
			String md5sum, int offset, String data)
			throws KojiHubClientException {
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(path);
		params.add(name);
		params.add(size);
		params.add(md5sum);
		params.add(offset);
		params.add(data);
		Object result;
		try {
			result = xmlRpcClient.execute("uploadFile", params); //$NON-NLS-1$
		} catch (XmlRpcException e) {
			throw new KojiHubClientException(e);
		}
		boolean success = Boolean.parseBoolean(result.toString());
		return success;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fedoraproject.eclipse.packager.koji.api.IKojiHubClient#listTags()
	 */
	@Override
	public HashMap<?, ?>[] listTargets() throws KojiHubClientException {
		try {
			// workaround for http://ws.apache.org/xmlrpc/faq.html#arrays
			Object[] genericTargets = (Object[]) xmlRpcClient.execute(
					"getBuildTargets", new Object[0]); //$NON-NLS-1$
			HashMap<?, ?>[] targetArray = new HashMap<?, ?>[genericTargets.length];
			for (int i = 0; i < genericTargets.length; i++) {
				targetArray[i] = (HashMap<?, ?>) genericTargets[i];
			}
			return targetArray;
		} catch (XmlRpcException e) {
			throw new KojiHubClientException(e);
		}
	}
}
