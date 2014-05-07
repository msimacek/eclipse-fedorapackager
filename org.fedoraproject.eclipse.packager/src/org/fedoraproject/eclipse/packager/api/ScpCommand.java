/*******************************************************************************
 * Copyright (c) 2010-2011 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.ScpFailedException;
import org.fedoraproject.eclipse.packager.utils.IChannelExec;
import org.fedoraproject.eclipse.packager.utils.IChannelSftp;
import org.fedoraproject.eclipse.packager.utils.ISession;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

/**
 * A class used to execute a {@code Scp} command. It has setters for all
 * supported options and arguments of this command and a
 * {@link #call(IProgressMonitor)} method to finally execute the command. Each
 * instance of this class should only be used for one invocation of the command
 * (means: one call to {@link #call(IProgressMonitor)})
 */
public class ScpCommand extends FedoraPackagerCommand<IStatus> {

	/**
	 * The unique ID of this command.
	 */
	public static final String ID = "ScpCommand"; //$NON-NLS-1$
	private static final String PUBLIC_HTML = "public_html"; //$NON-NLS-1$
	private static final String REMOTE_DIR = "fpe-rpm-review"; //$NON-NLS-1$

	protected String specFile;
	protected String srpmFile;
	private ISession session = null;
	private boolean scpconfirmed = true;
	private String fileScpConfirm;

	final static FedoraPackagerLogger logger = FedoraPackagerLogger
			.getInstance();

	/*
	 * Implementation of the {@code ScpCommand}.
	 *
	 * @param monitor
	 *
	 * @throws CommandMisconfiguredException If the command was not properly
	 * configured when it was called.
	 *
	 * @throws CommandListenerException If some listener detected a problem.
	 *
	 * @throws ScpFailedException if .src.rpm file does not exist to be copied
	 *
	 * @return The result of this command.
	 */
	@Override
	public IStatus call(IProgressMonitor monitor)
			throws CommandMisconfiguredException, CommandListenerException,
			ScpFailedException {
		try {
			callPreExecListeners();
		} catch (CommandListenerException e) {
			if (e.getCause() instanceof CommandMisconfiguredException) {
				// explicitly throw the specific exception
				throw (CommandMisconfiguredException) e.getCause();
			}
			throw e;
		}

		try {
			session.setConfig("StrictHostKeyChecking", "no"); //$NON-NLS-1$ //$NON-NLS-2$
			session.connect();

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			if (srpmFile.isEmpty()) {
				throw new ScpFailedException(
						FedoraPackagerText.ScpCommand_filesToScpMissing);
			}

			// Creates the remote 'fpe-rpm-review' directory in public_html
			createRemoteDir(session);

			// if files don't exist in the remote directory or
			// if the user is willing to replace them, copy those files remotely
			if (scpconfirmed) {
				copyFileToRemote(specFile, session);
				copyFileToRemote(srpmFile, session);
			}

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			session.disconnect();

		} catch (JSchException e) {
			throw new ScpFailedException(e.getMessage(), e);
		}

		// Call post-exec listeners
		callPostExecListeners();

		setCallable(false);
		if (scpconfirmed) {
			return Status.OK_STATUS;
		} 
		return Status.CANCEL_STATUS;
	}

	/**
	 * check the public_html and create the remote directory if it doesn't exist
	 * If it exists, make sure the files to copy don't already exist If they do,
	 * check with the user to replace or cancel the operation
	 *
	 * @param session
	 *            of the current operation
	 * @throws ScpFailedException
	 *             If transfer of directory to remote is unsuccessful.
	 *
	 */
	private void createRemoteDir(ISession session) throws ScpFailedException {
		boolean dirFound = false;
		boolean fileFound = false;

		IChannelSftp channelSftp;

		try {
			channelSftp = session.openChannelSftp();
			channelSftp.connect();
			// check if the remote directory exists
			// if not, create the proper directory in public_html
			Vector<String> existDir = channelSftp.stringLs(PUBLIC_HTML);
			for (String dirName : existDir){
				if (dirName.contentEquals(REMOTE_DIR)){
					dirFound = true;
					break;
				}
			}
			if (!dirFound)
				channelSftp.mkdir(PUBLIC_HTML + IPath.SEPARATOR + REMOTE_DIR);

			// check if the files to scp already exist in the remote directory
			// if yes, ask for confirmation
			Vector<String> existFile = channelSftp.stringLs(PUBLIC_HTML + IPath.SEPARATOR
					+ REMOTE_DIR);
			for (String dirName : existFile){
				if (dirName.contentEquals(srpmFile)){
					fileFound = true;
					break;
				}
			}
			if (fileFound) {
				fileScpConfirm = NLS
						.bind(FedoraPackagerText.ScpCommand_filesToScpExist,
								srpmFile);
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						scpconfirmed = MessageDialog
								.openConfirm(
										null,
										FedoraPackagerText.ScpCommand_notificationTitle,
										fileScpConfirm);
					}
				});

			} else {
				scpconfirmed = true;
			}

			channelSftp.disconnect();

		} catch (JSchException|SftpException e) {
			throw new ScpFailedException(e.getMessage(), e);
		}

	}

	/**
	 * Copies the localFile to remote location at remoteFile
	 *
	 * @param fileToScp
	 *            to be copied remotely
	 * @param session
	 *            of the current operation
	 * @throws ScpFailedException
	 *             If transfer of file to remote is unsuccessful.
	 *
	 */
	private void copyFileToRemote(String fileToScp, ISession session)
			throws ScpFailedException {
		FileInputStream fis = null;

		// exec 'scp -t remoteFile' remotely
		String remoteFile = PUBLIC_HTML + IPath.SEPARATOR + REMOTE_DIR
				+ IPath.SEPARATOR + fileToScp;
		String command = "scp -p -t " + remoteFile; //$NON-NLS-1$

		IChannelExec channel;
		try {
			channel = session.openChannelExec();
			channel.setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				throw new ScpFailedException(
						FedoraPackagerText.ScpCommand_filesToScpNonReadable);
			}

			// send "C0644 filesize filename", where filename should not include
			// '/'
			String localFile = projectRoot.getProject().getLocation()
					.toString()
					+ IPath.SEPARATOR + fileToScp;
			long filesize = (new File(localFile)).length();
			command = "C0644 " + filesize + " "; //$NON-NLS-1$ //$NON-NLS-2$
			if (localFile.lastIndexOf('/') > 0) {
				command += localFile.substring(localFile.lastIndexOf('/') + 1);
			} else {
				command += localFile;
			}
			command += "\n"; //$NON-NLS-1$

			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				throw new ScpFailedException(
						FedoraPackagerText.ScpCommand_filesToScpNonReadable);
			}

			// send a content of localFile
			fis = new FileInputStream(localFile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;

			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				throw new ScpFailedException(
						FedoraPackagerText.ScpCommand_filesToScpNonReadable);
			}
			out.close();

			channel.disconnect();

		} catch (JSchException|IOException e) {
			throw new ScpFailedException(e.getMessage(), e);
		}
	}

	@Override
	protected void checkConfiguration() throws CommandMisconfiguredException {
		if (this.session == null) {
			throw new CommandMisconfiguredException(
					FedoraPackagerText.ScpCommand_NoSession);
		}

		if ((this.specFile == null) || (this.srpmFile == null)) {
			throw new CommandMisconfiguredException(
					FedoraPackagerText.ScpCommand_filesToScpMissing);
		}
	}

	/**
	 * @param specFile
	 *            The specfile to be copied.
	 */
	public void specFile(String specFile) {
		this.specFile = specFile;
	}

	/**
	 * @param srpmFile
	 *            The srpm to be copied.
	 */
	public void srpmFile(String srpmFile) {
		this.srpmFile = srpmFile;
	}

	/**
	 * @param session
	 *            The JSch session to be used.
	 */
	public void session(ISession session) {
		this.session = session;
	}

	/*
	 * @param in
	 *
	 * @throws IOException
	 *
	 * @return 0 if successful
	 */
	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success, 1 for error,
		// 2 for fatal error, -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				logger.logError(sb.toString());
			}
			if (b == 2) { // fatal error
				logger.logError(sb.toString());
			}
		}
		return b;
	}

}