package org.fedoraproject.eclipse.packager.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.JSchException;

/**
 * 
 * Interface to wrap jsch object Channel in an interface for testing purposes.
 * For Javadocs see jsch documentation.
 * 
 */
@SuppressWarnings("javadoc")
public interface IChannel {

	 public  void connect() throws JSchException;

	 public  void disconnect() ;

	 public InputStream getInputStream() throws IOException ;

	 public OutputStream getOutputStream() throws IOException ;
}
