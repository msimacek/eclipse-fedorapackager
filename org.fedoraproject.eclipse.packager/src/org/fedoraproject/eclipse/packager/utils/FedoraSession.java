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
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;
import com.jcraft.jsch.UserInfo;

/**
 * A class to wrap the jsch object Session into an ISession.
 * 
 */
public class FedoraSession implements ISession {
	private Session session;

	/**
	 * @param session
	 *            The session object to wrap into ISession
	 */
	public FedoraSession(Session session) {
		this.session = session;
	}

	@Override
	public void connect() throws JSchException {
		session.connect();
	}

	@Override
	public void connect(int connectTimeout) throws JSchException {
		session.connect(connectTimeout);
	}

	@Override
	public void delPortForwardingL(int lport) throws JSchException {
		session.delPortForwardingL(lport);
	}

	@Override
	public void delPortForwardingL(String boundaddress, int lport)
			throws JSchException {
		session.delPortForwardingL(boundaddress, lport);
	}

	@Override
	public void delPortForwardingR(int rport) throws JSchException {
		session.delPortForwardingR(rport);
	}

	@Override
	public void disconnect() {
		session.disconnect();
	}

	@Override
	public void encode(Packet packet) throws Exception {
		session.encode(packet);
	}

	@Override
	public String getClientVersion() {
		return session.getClientVersion();
	}

	@Override
	public String getConfig(String key) {
		return session.getConfig(key);
	}

	@Override
	public String getHost() {
		return session.getHost();
	}

	@Override
	public HostKey getHostKey() {
		return session.getHostKey();
	}

	@Override
	public String getHostKeyAlias() {
		return session.getHostKeyAlias();
	}

	@Override
	public int getPort() {
		return session.getPort();
	}

	@Override
	public String[] getPortForwardingL() throws JSchException {
		return session.getPortForwardingL();
	}

	@Override
	public int getServerAliveCountMax() {
		return session.getServerAliveCountMax();
	}

	@Override
	public int getServerAliveInterval() {
		return session.getServerAliveInterval();
	}

	@Override
	public String getServerVersion() {
		return session.getServerVersion();
	}

	@Override
	public int getTimeout() {
		return session.getTimeout();
	}

	@Override
	public UserInfo getUserInfo() {
		return session.getUserInfo();
	}

	@Override
	public String getUserName() {
		return session.getUserName();
	}

	@Override
	public boolean isConnected() {
		return session.isConnected();
	}

	@Override
	public Channel openChannel(String arg0) throws JSchException {
		return session.openChannel(arg0);
	}

	@Override
	public Buffer read(Buffer type) throws Exception {
		return session.read(type);
	}

	@Override
	public void rekey() throws Exception {
		session.rekey();
	}

	@Override
	public void run() {
		session.run();
	}

	@Override
	public void sendIgnore() throws Exception {
		session.sendIgnore();
	}

	@Override
	public void sendKeepAliveMsg() throws Exception {
		session.sendKeepAliveMsg();
	}

	@Override
	public void setClientVersion(String cv) {
		session.setClientVersion(cv);
	}

	@Override
	public void setConfig(Properties newconf) {
		session.setConfig(newconf);
	}

	@Override
	public void setConfig(@SuppressWarnings("rawtypes") Hashtable arg0) {
		session.setConfig(arg0);
	}

	@Override
	public void setConfig(String key, String value) {
		session.setConfig(key, value);
	}

	@Override
	public void setDaemonThread(boolean enable) {
		session.setDaemonThread(enable);
	}

	@Override
	public void setHost(String host) {
		session.setHost(host);
	}

	@Override
	public void setHostKeyAlias(String hostKeyAlias) {
		session.setHostKeyAlias(hostKeyAlias);
	}

	@Override
	public void setInputStream(InputStream in) {
		session.setInputStream(in);
	}

	@Override
	public void setOutputStream(OutputStream out) {
		session.setOutputStream(out);
	}

	@Override
	public void setPassword(String password) {
		session.setPassword(password);
	}

	@Override
	public void setPassword(byte[] password) {
		session.setPassword(password);
	}

	@Override
	public void setPort(int port) {
		session.setPort(port);
	}

	@Override
	public int setPortForwardingL(int lport, String host, int rport)
			throws JSchException {
		return session.setPortForwardingL(lport, host, rport);
	}

	@Override
	public int setPortForwardingL(String boundaddress, int lport, String host,
			int rport) throws JSchException {
		return session.setPortForwardingL(boundaddress, lport, host, rport);
	}

	@Override
	public int setPortForwardingL(String boundaddress, int lport, String host,
			int rport, ServerSocketFactory ssf) throws JSchException {
		return session
				.setPortForwardingL(boundaddress, lport, host, rport, ssf);
	}

	@Override
	public void setPortForwardingR(int rport, String daemon)
			throws JSchException {
		session.setPortForwardingR(rport, daemon);
	}

	@Override
	public void setPortForwardingR(int rport, String host, int lport)
			throws JSchException {
		session.setPortForwardingR(rport, host, lport);
	}

	@Override
	public void setPortForwardingR(int rport, String daemon, Object[] arg)
			throws JSchException {
		session.setPortForwardingR(rport, daemon, arg);
	}

	@Override
	public void setPortForwardingR(String bind_address, int rport, String host,
			int lport) throws JSchException {
		session.setPortForwardingR(bind_address, rport, host, lport);
	}

	@Override
	public void setPortForwardingR(int rport, String host, int lport,
			SocketFactory sf) throws JSchException {
		session.setPortForwardingR(rport, host, lport, sf);
	}

	@Override
	public void setPortForwardingR(String bind_address, int rport,
			String daemon, Object[] arg) throws JSchException {
		session.setPortForwardingR(bind_address, rport, daemon, arg);
	}

	@Override
	public void setPortForwardingR(String bind_address, int rport, String host,
			int lport, SocketFactory sf) throws JSchException {
		session.setPortForwardingR(bind_address, rport, host, lport, sf);
	}

	@Override
	public void setProxy(Proxy proxy) {
		session.setProxy(proxy);
	}

	@Override
	public void setServerAliveCountMax(int count) {
		session.setServerAliveCountMax(count);
	}

	@Override
	public void setServerAliveInterval(int interval) throws JSchException {
		session.setServerAliveInterval(interval);
	}

	@Override
	public void setSocketFactory(SocketFactory sfactory) {
		session.setSocketFactory(sfactory);
	}

	@Override
	public void setTimeout(int arg0) throws JSchException {
		session.setTimeout(arg0);
	}

	@Override
	public void setUserInfo(UserInfo userinfo) {
		session.setUserInfo(userinfo);
	}

	@Override
	public void setX11Cookie(String cookie) {
		session.setX11Cookie(cookie);
	}

	@Override
	public void setX11Host(String host) {
		session.setX11Host(host);
	}

	@Override
	public void setX11Port(int port) {
		session.setX11Port(port);
	}

	@Override
	public void write(Packet arg0) throws Exception {
		session.write(arg0);
	}

}
