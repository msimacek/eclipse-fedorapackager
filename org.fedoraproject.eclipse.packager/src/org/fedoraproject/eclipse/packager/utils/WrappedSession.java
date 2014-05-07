/*******************************************************************************
 * Copyright (c) 2010-2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.utils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * A class to wrap the jsch object Session into an ISession.
 * 
 */
public class WrappedSession implements ISession {
	private Session session;

	/**
	 * @param session
	 *            The session object to wrap into ISession
	 */
	public WrappedSession(Session session) {
		this.session = session;
	}

	@Override
	public void connect() throws JSchException {
		session.connect();
	}

	@Override
	public void disconnect() {
		session.disconnect();
	}

	@Override
	public IChannelSftp openChannelSftp() throws JSchException {
		return new WrappedChannelSftp((ChannelSftp) session.openChannel("sftp")); //$NON-NLS-1$
	}

	@Override
	public IChannelExec openChannelExec() throws JSchException {
		return new WrappedChannelExec((ChannelExec) session.openChannel("exec")); //$NON-NLS-1$
	}

	@Override
	public void setConfig(String key, String value) {
		session.setConfig(key, value);
	}
}
