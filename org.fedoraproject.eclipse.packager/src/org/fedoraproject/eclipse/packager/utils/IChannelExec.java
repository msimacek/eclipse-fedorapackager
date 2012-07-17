package org.fedoraproject.eclipse.packager.utils;

/**
 * 
 * Interface to wrap jsch object ChannelExec in an interface for testing
 * purposes. For Javadocs see jsch documentation.
 * 
 */
@SuppressWarnings("javadoc")
public interface IChannelExec extends IChannel {

	public void setCommand(String command);

}
