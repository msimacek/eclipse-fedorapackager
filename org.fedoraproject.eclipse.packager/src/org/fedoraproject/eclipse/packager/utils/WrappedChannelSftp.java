package org.fedoraproject.eclipse.packager.utils;

import java.util.Iterator;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * A class to wrap the jsch object ChannelSftp into an IChannelSftp.
 * 
 */
public class WrappedChannelSftp extends WrappedChannel implements IChannelSftp {

	private ChannelSftp channel;

	/**
	 * @param channel
	 *            The ChannelSftp to wrap into IChannelSftp.
	 */
	public WrappedChannelSftp(ChannelSftp channel) {
		super(channel);
		this.channel = channel;
	}

	@Override
	public void mkdir(String arg0) throws SftpException {
		channel.mkdir(arg0);
	}

	@Override
	public Vector<String> stringLs(String path) throws SftpException {
		Vector<String> nameVector = new Vector<>();
		Vector<?> existDir = channel.ls(path);
		Iterator<?> it = existDir.iterator();
		while (it.hasNext()) {
			LsEntry entry = (LsEntry) it.next();
			nameVector.add(entry.getFilename());
		}
		return nameVector;
	}



}
