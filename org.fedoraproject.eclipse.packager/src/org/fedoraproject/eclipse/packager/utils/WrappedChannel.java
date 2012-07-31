package org.fedoraproject.eclipse.packager.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;

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
	public void disconnect() {
		channel.disconnect();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return channel.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return channel.getOutputStream();
	}

}
