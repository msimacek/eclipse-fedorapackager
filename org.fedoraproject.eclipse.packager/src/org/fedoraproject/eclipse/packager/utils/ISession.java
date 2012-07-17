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

import com.jcraft.jsch.JSchException;

/**
 * 
 * Interface to wrap jsch object Session in an interface for testing purposes.
 * For Javadocs see jsch documentation.
 * 
 */
@SuppressWarnings("javadoc")
public interface ISession {
	
	public void connect() throws JSchException;

	public void disconnect();

	/**
	 * Open an Sftp channel directly.
	 * 
	 * @return The interface-casted channel.
	 * @throws JSchException
	 *             If channel could not be opened.
	 */
	public IChannelSftp openChannelSftp() throws JSchException;

	/**
	 * Open an Exec channel directly.
	 * 
	 * @return The interface-casted channel.
	 * @throws JSchException
	 *             If channel could not be opened.
	 */
	public IChannelExec openChannelExec() throws JSchException;

	public void setConfig(String key, String value);
}
