/*******************************************************************************
 * Copyright (c) 2010 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.cvs.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.fedoraproject.eclipse.packager.FedoraProjectRoot;
import org.fedoraproject.eclipse.packager.SourcesFile;
import org.fedoraproject.eclipse.packager.handlers.UploadHandler;

/**
 * Common functionality of CVS handlers
 * 
 * @author RedHat Inc.
 * 
 */
@SuppressWarnings("restriction")
public abstract class CVSHandler extends UploadHandler {

	/**
	 * Update the ignore file .cvsignore or .gitignore file. Appends to file.
	 * 
	 * @param ignoreFile
	 * @param toAdd
	 * @return
	 */
	protected IStatus updateIgnoreFile(File ignoreFile, File toAdd) {
		return updateIgnoreFile(ignoreFile, toAdd, false);
	}

	/**
	 * Run CVS update/add on sources, and .cvsignore file
	 * 
	 * @param sources
	 * @param cvsignore
	 * @param monitor
	 * @return Status of the operation performed.
	 */
	protected IStatus updateCVS(FedoraProjectRoot projectRoot, File cvsignore,
			IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		IFile specfile = projectRoot.getSpecFile();
		SourcesFile sources = projectRoot.getSourcesFile();
		// get CVSProvider
		CVSTeamProvider provider = (CVSTeamProvider) RepositoryProvider
				.getProvider(specfile.getProject(),
						CVSProviderPlugin.getTypeId());

		try {
			ICVSRepositoryLocation location = provider.getRemoteLocation();

			// get CVSROOT
			CVSWorkspaceRoot cvsRoot = provider.getCVSWorkspaceRoot();
			ICVSFolder rootFolder = cvsRoot.getLocalRoot();

			// get Branch
			ICVSFolder branchFolder = rootFolder.getFolder(specfile.getParent()
					.getName());
			if (branchFolder != null) {
				ICVSFile cvsSources = branchFolder.getFile(sources.getName());
				if (cvsSources != null) {
					// if 'sources' is not shared with CVS, add it
					Session session = new Session(location, branchFolder, true);
					session.open(monitor, true);
					if (!cvsSources.isManaged()) {
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
						String[] arguments = new String[] { sources.getName() };
						status = Command.ADD.execute(session,
								Command.NO_GLOBAL_OPTIONS,
								Command.NO_LOCAL_OPTIONS, arguments, null,
								monitor);
					}
					if (status.isOK()) {
						// everything has passed so far
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
						// perform update on sources and .cvsignore
						String[] arguments = new String[] { sources.getName(),
								cvsignore.getName() };
						status = Command.UPDATE.execute(session,
								Command.NO_GLOBAL_OPTIONS,
								Command.NO_LOCAL_OPTIONS, arguments, null,
								monitor);
					}
				} else {
					status = handleError(org.fedoraproject.eclipse.packager.Messages
							.getString("UploadHandler.22")); //$NON-NLS-1$
				}
			} else {
				status = handleError(org.fedoraproject.eclipse.packager.Messages
						.getString("UploadHandler.23")); //$NON-NLS-1$
			}

		} catch (CVSException e) {
			e.printStackTrace();
			status = handleError(e.getMessage());
		}
		return status;
	}

	/**
	 * Actually writes to .cvsignore. ATM this method is never called with
	 * <code>forceOverwrite</code> set to true.
	 * 
	 * @param cvsignore
	 * @param toAdd
	 * @param forceOverwrite
	 * @return Status of the performed operation.
	 */
	private IStatus updateIgnoreFile(File ignoreRile, File toAdd,
			boolean forceOverwrite) {
		IStatus status;
		String filename = toAdd.getName();
		ArrayList<String> ignoreFiles = new ArrayList<String>();
		BufferedReader br = null;
		PrintWriter pw = null;
		try {
			if (forceOverwrite) {
				pw = new PrintWriter(new FileWriter(ignoreRile, false));
				pw.println(filename);
				status = Status.OK_STATUS;
			} else {
				// only append to file if not already present
				br = new BufferedReader(new FileReader(ignoreRile));

				String line = br.readLine();
				while (line != null) {
					ignoreFiles.add(line);
					line = br.readLine();
				}

				if (!ignoreFiles.contains(filename)) {
					pw = new PrintWriter(new FileWriter(ignoreRile, true));
					pw.println(filename);
				}
				status = Status.OK_STATUS;
			}
		} catch (IOException e) {
			e.printStackTrace();
			status = handleError(e);
		} finally {
			if (pw != null) {
				pw.close();
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					status = handleError(e);
				}
			}
		}
		return status;
	}
}
