package org.fedoraproject.eclipse.packager.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * A class to wrap the jsch object Channel into an IChannel.
 * 
 */
public abstract class WrappedChannel implements IChannel {
	
	private Channel channel;

	/**
	 * @param channel
	 *            The ChannelSftp to wrap into IChannelSftp.
	 */
	public WrappedChannel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void connect() throws JSchException {
		channel.connect();
	}

	@Override
	public void connect(int connectTimeout) throws JSchException {
		channel.connect(connectTimeout);
	}

	@Override
	public void disconnect() {
		channel.disconnect();
	}

	@Override
	public int getExitStatus() {
		return channel.getExitStatus();
	}

	@Override
	public InputStream getExtInputStream() throws IOException {
		return channel.getExtInputStream();
	}

	@Override
	public int getId() {
		return channel.getId();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return channel.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return channel.getOutputStream();
	}

	@Override
	public Session getSession() throws JSchException {
		return channel.getSession();
	}

	@Override
	public boolean isClosed() {
		return channel.isClosed();
	}

	@Override
	public boolean isConnected() {
		return channel.isConnected();
	}

	@Override
	public boolean isEOF() {
		return channel.isEOF();
	}

	@Override
	public void run() {
		channel.run();
	}

	@Override
	public void sendSignal(String signal) throws Exception {
		channel.sendSignal(signal);
	}

	@Override
	public void setExtOutputStream(OutputStream out) {
		channel.setExtOutputStream(out);
	}

	@Override
	public void setExtOutputStream(OutputStream out, boolean dontclose) {
		channel.setExtOutputStream(out, dontclose);
	}

	@Override
	public void setInputStream(InputStream in) {
		channel.setInputStream(in);
	}

	@Override
	public void setInputStream(InputStream in, boolean dontclose) {
		channel.setInputStream(in, dontclose);
	}

	@Override
	public void setOutputStream(OutputStream out) {
		channel.setOutputStream(out);
	}

	@Override
	public void setOutputStream(OutputStream out, boolean dontclose) {
		channel.setOutputStream(out, dontclose);
	}

	@Override
	public void setXForwarding(boolean foo) {
		channel.setXForwarding(foo);
	}

	@Override
	public void start() throws JSchException {
		channel.start();
	}

}
