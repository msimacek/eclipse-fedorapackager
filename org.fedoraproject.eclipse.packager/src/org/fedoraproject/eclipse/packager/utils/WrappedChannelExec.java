package org.fedoraproject.eclipse.packager.utils;

import com.jcraft.jsch.ChannelExec;

/**
 * A class to wrap the jsch object ChannelExec into an IChannelExec.
 * 
 */
public class WrappedChannelExec extends WrappedChannel implements IChannelExec {

	private ChannelExec channel;

	/**
	 * @param channel
	 *            The ChannelExec to wrap into IChannelExec.
	 */
	public WrappedChannelExec(ChannelExec channel) {
		super(channel);
		this.channel = channel;
	}

	@Override
	public void setCommand(String command) {
		channel.setCommand(command);
	}
}
