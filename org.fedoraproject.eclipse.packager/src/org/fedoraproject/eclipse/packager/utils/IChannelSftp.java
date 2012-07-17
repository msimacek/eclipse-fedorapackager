package org.fedoraproject.eclipse.packager.utils;

import java.util.Vector;

import com.jcraft.jsch.SftpException;

/**
 * 
 * Interface to wrap jsch object ChannelSftp in an interface for testing
 * purposes. For Javadocs see jsch documentation.
 * 
 */
@SuppressWarnings("javadoc")
public interface IChannelSftp extends IChannel {
	
	/**
	 * Do an ls and just return filename data. 
	 * @param path The path to perform ls on
	 * @return A Vector of the filenames.
	 * @throws SftpException
	 */
	public Vector<String> stringLs(String path) throws SftpException;

	public void mkdir(String path) throws SftpException;
}
