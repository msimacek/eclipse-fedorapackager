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
package org.fedoraproject.eclipse.packager.bodhi;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.httpclient.HttpException;
import org.json.JSONException;
import org.json.JSONObject;

public interface IBodhiClient {

	public abstract JSONObject login(String username, String password)
			throws IOException, HttpException, ParseException, JSONException;

	public abstract JSONObject newUpdate(String buildName, String release,
			String type, String request, String bugs, String notes, String csrf_token)
			throws IOException, HttpException, ParseException, JSONException;

	public abstract void logout() throws IOException, HttpException, ParseException;

}