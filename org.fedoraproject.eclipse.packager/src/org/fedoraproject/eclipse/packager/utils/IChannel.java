package org.fedoraproject.eclipse.packager.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * 
 * Interface to wrap jsch object Channel in an interface for testing purposes.
 * For Javadocs see jsch documentation.
 * 
 */
@SuppressWarnings("javadoc")
public interface IChannel {

	 public  void connect() throws JSchException;

	 public  void connect(int connectTimeout) throws JSchException ;

	 public  void disconnect() ;

	 public int getExitStatus() ;

	 public InputStream getExtInputStream() throws IOException ;

	 public int getId() ;

	 public InputStream getInputStream() throws IOException ;

	 public OutputStream getOutputStream() throws IOException ;

	 public Session getSession() throws JSchException ;

	 public boolean isClosed() ;

	 public boolean isConnected() ;

	 public boolean isEOF() ;

	 public  void run() ;

	 public  void sendSignal(String signal) throws Exception ;

	 public  void setExtOutputStream(OutputStream out) ;

	 public  void setExtOutputStream(OutputStream out,
	    boolean dontclose) ;

	 public  void setInputStream(InputStream in) ;

	 public  void setInputStream(InputStream in,
	    boolean dontclose) ;

	 public  void setOutputStream(OutputStream out) ;

	 public  void setOutputStream(OutputStream out,
	    boolean dontclose) ;

	 public  void setXForwarding(boolean foo) ;

	 public  void start() throws JSchException ;
}
