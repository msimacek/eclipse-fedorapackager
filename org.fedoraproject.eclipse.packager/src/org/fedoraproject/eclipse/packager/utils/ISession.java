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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Properties;
import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Packet;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ServerSocketFactory;
import com.jcraft.jsch.SocketFactory;
import com.jcraft.jsch.UserInfo;

/**
 * 
 * Interface to wrap jsch object Session in an interface for testing purposes.
 * For Javadocs see jsch documentation.
 * 
 */
@SuppressWarnings("javadoc")
public interface ISession {
	public void connect() throws JSchException;

	public void connect(int connectTimeout) throws JSchException;

	public void delPortForwardingL(int lport) throws JSchException;

	public void delPortForwardingL(String boundaddress, int lport)
			throws JSchException;

	public void delPortForwardingR(int rport) throws JSchException;

	public void disconnect();

	public void encode(Packet packet) throws Exception;

	public String getClientVersion();

	public String getConfig(String key);

	public String getHost();

	public HostKey getHostKey();

	public String getHostKeyAlias();

	public int getPort();

	public String[] getPortForwardingL() throws JSchException;

	public int getServerAliveCountMax();

	public int getServerAliveInterval();

	public String getServerVersion();

	public int getTimeout();

	public UserInfo getUserInfo();

	public String getUserName();

	public boolean isConnected();

	public Channel openChannel(String type) throws JSchException;

	public Buffer read(Buffer buf) throws Exception;

	public void rekey() throws Exception;

	public void run();

	public void sendIgnore() throws Exception;

	public void sendKeepAliveMsg() throws Exception;

	public void setClientVersion(String cv);

	public void setConfig(Properties newconf);

	public void setConfig(@SuppressWarnings("rawtypes") Hashtable newconf);

	public void setConfig(String key, String value);

	public void setDaemonThread(boolean enable);

	public void setHost(String host);

	public void setHostKeyAlias(String hostKeyAlias);

	public void setInputStream(InputStream in);

	public void setOutputStream(OutputStream out);

	public void setPassword(String password);

	public void setPassword(byte[] password);

	public void setPort(int port);

	public int setPortForwardingL(int lport, String host, int rport)
			throws JSchException;

	public int setPortForwardingL(String boundaddress, int lport, String host,
			int rport) throws JSchException;

	public int setPortForwardingL(String boundaddress, int lport, String host,
			int rport, ServerSocketFactory ssf) throws JSchException;

	public void setPortForwardingR(int rport, String daemon)
			throws JSchException;

	public void setPortForwardingR(int rport, String host, int lport)
			throws JSchException;

	public void setPortForwardingR(int rport, String daemon, Object[] arg)
			throws JSchException;

	public void setPortForwardingR(String bind_address, int rport, String host,
			int lport) throws JSchException;

	public void setPortForwardingR(int rport, String host, int lport,
			SocketFactory sf) throws JSchException;

	public void setPortForwardingR(String bind_address, int rport,
			String daemon, Object[] arg) throws JSchException;

	public void setPortForwardingR(String bind_address, int rport, String host,
			int lport, SocketFactory sf) throws JSchException;

	public void setProxy(Proxy proxy);

	public void setServerAliveCountMax(int count);

	public void setServerAliveInterval(int interval) throws JSchException;

	public void setSocketFactory(SocketFactory sfactory);

	public void setTimeout(int timeout) throws JSchException;

	public void setUserInfo(UserInfo userinfo);

	public void setX11Cookie(String cookie);

	public void setX11Host(String host);

	public void setX11Port(int port);

	public void write(Packet packet) throws Exception;
}
