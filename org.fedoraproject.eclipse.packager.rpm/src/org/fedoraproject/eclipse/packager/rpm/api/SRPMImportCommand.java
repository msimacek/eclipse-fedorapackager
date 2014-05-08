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
package org.fedoraproject.eclipse.packager.rpm.api;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.linuxtools.rpm.core.RPMProjectLayout;
import org.eclipse.linuxtools.rpm.ui.SRPMImportOperation;
import org.eclipse.linuxtools.tools.launch.core.factory.RuntimeProcessFactory;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.SourcesFileUpdater;
import org.fedoraproject.eclipse.packager.api.UploadSourceCommand;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.FileAvailableInLookasideCacheException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.errors.SRPMImportCommandException;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Command for importing SRPM to a project.
 *
 */
public class SRPMImportCommand {

	private String srpm = null;
	private IProject project = null;
	private IContainer fprContainer;
	private String uploadUrl;
	private ISRPMImportCommandSLLPolicyCallback sslPolicyCallback;

	/**
	 * @param srpm
	 *            The srpm being imported.
	 * @param project
	 *            The project that is receiving the import.
	 * @param fprContainer
	 *            The container containing the folder to which the SRPM is being
	 *            imported.
	 * @param uploadUrl
	 *            The url of the lookaside cache.
	 * @param sslPolicyCallback
	 *            A callback for setting the proper SSL policy for lookaside
	 *            uploads.
	 */
	public SRPMImportCommand(String srpm, IProject project,
			IContainer fprContainer, String uploadUrl, ISRPMImportCommandSLLPolicyCallback sslPolicyCallback) {
		this.srpm = srpm;
		this.project = project;
		this.fprContainer = fprContainer;
		this.uploadUrl = uploadUrl;
		this.sslPolicyCallback = sslPolicyCallback;
	}

	protected UploadSourceCommand getUploadSourceCommand()
			throws InvalidProjectRootException,
			FedoraPackagerCommandInitializationException,
			FedoraPackagerCommandNotFoundException {
		IProjectRoot fpr = FedoraPackagerUtils.getProjectRoot(fprContainer);
		FedoraPackager fp = new FedoraPackager(fpr);
		return (UploadSourceCommand) fp
				.getCommandInstance(UploadSourceCommand.ID);
	}

	/**
	 * Calling method for this command.
	 *
	 * @param monitor
	 *            Monitor for this command's runtime.
	 * @return The result of calling this command.
	 * @throws SRPMImportCommandException If the import fails, the message contains the details.
	 */
	public SRPMImportResult call(IProgressMonitor monitor)
			throws SRPMImportCommandException {
		Set<String> stageSet = new HashSet<>();
		Set<String> uploadedFiles = new HashSet<>();
		// install rpm to the project folder
		SRPMImportOperation sio = new SRPMImportOperation(project, new File(srpm),
					RPMProjectLayout.FLAT);
		sio.run(new NullProgressMonitor());
		if (!sio.getStatus().isOK()) {
			Throwable e = sio.getStatus().getException();
			if (e != null) {
				throw new SRPMImportCommandException(e.getMessage(), e);
			} else {
				throw new SRPMImportCommandException(NLS.bind(
						RpmText.SRPMImportCommand_ImportError, sio.getStatus()
								.getMessage()));
			}
		}
		String[] cmdList = null;
		List<String> uploadList = new ArrayList<>();
		String[] moveFiles;
		String[] uploadFiles;
		try {
			// get files in the srpm
			cmdList = new String[] { "rpm", "-qpl", srpm }; //$NON-NLS-1$ //$NON-NLS-2$
			Process child = RuntimeProcessFactory.getFactory().exec(cmdList, null);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new BufferedInputStream(child.getInputStream())));

			try {
				if (child.waitFor() != 0) {
					throw new SRPMImportCommandException(NLS.bind(
							RpmText.SRPMImportCommand_NonZeroQueryExit,
							child.exitValue()));
				}
			} catch (InterruptedException e) {
				throw new OperationCanceledException();
			}

			String line = br.readLine();
			while (line != null) {
				uploadList.add(line);
				line = br.readLine();
			}
			// SRPM needs to be moved but not uploaded, use existing list to
			// build array of files that need to be moved
			uploadList.add((new File(srpm)).getName());
			moveFiles = uploadList.toArray(new String[0]);
			uploadList.remove((new File(srpm)).getName());
		} catch (IOException e) {
			throw new SRPMImportCommandException(NLS.bind(
					RpmText.SRPMImportCommand_IOError, srpm), e);
		}
		SRPMImportResult result = new SRPMImportResult(cmdList);
		// Move imported files to packager root as workaround for
		// rpm editor only building to a limited number of project
		// layouts; flat layout may not be suitable to for CVS repositories or
		// Git repositories with subfolders
		if (!fprContainer.getProject().getFullPath().toOSString()
				.equals(fprContainer.getFullPath().toOSString())) {
			for (String file : moveFiles) {
				IResource source = fprContainer.getProject().findMember(file);
				if (file.endsWith(".spec")) { //$NON-NLS-1$
					file = fprContainer.getProject().getName().concat(".spec"); //$NON-NLS-1$
				}
				IFile resultFile = fprContainer.getFile(new Path(file));
				// no way to force overwrite workaround
				if (resultFile.exists()) {
					try {
						resultFile.delete(true, monitor);
					} catch (CoreException e) {
						throw new SRPMImportCommandException(NLS.bind(
								RpmText.SRPMImportCommand_OverwriteError,
								resultFile.getProjectRelativePath()), e);
					}
				}
				try {
					source.move(resultFile.getProjectRelativePath(), true,
							monitor);
				} catch (CoreException e) {
					throw new SRPMImportCommandException(NLS.bind(
							RpmText.SRPMImportCommand_MoveError, file), e);
				}
			}
		} else {
			for (String file : moveFiles) {
				if (file.endsWith(".spec") && !file.startsWith(fprContainer.getProject().getName())) { //$NON-NLS-1$
					IResource source = fprContainer.getProject().findMember(file);
					file = fprContainer.getProject().getName().concat(".spec"); //$NON-NLS-1$
					IFile resultFile = fprContainer.getFile(new Path(file));
					try {
						source.move(resultFile.getProjectRelativePath(), true,
								monitor);
					} catch (CoreException e) {
						throw new SRPMImportCommandException(NLS.bind(
								RpmText.SRPMImportCommand_MoveError, file), e);
					}
				}
			}
		}
		uploadFiles = uploadList.toArray(new String[0]);
		try {
			// create sources file if one does not exist
			fprContainer
					.getFile(new Path("sources")).getLocation().toFile().createNewFile(); //$NON-NLS-1$
			fprContainer.getProject().refreshLocal(IResource.DEPTH_INFINITE,
					monitor);
			// Should now have a valid project root, so get it.
			IProjectRoot fpr = FedoraPackagerUtils.getProjectRoot(fprContainer);

			// Make sure if the imported SRPM makes sense
			if (!fpr.getSpecfileModel().getName()
					.equals(fprContainer.getProject().getName())) {
				String errorMsg = NLS.bind(
						RpmText.SRPMImportCommand_PackageNameSpecNameMismatchError,
						fprContainer.getProject().getName(), fpr
								.getSpecfileModel().getName());
				throw new SRPMImportCommandException(errorMsg);
			}

			UploadSourceCommand upload = getUploadSourceCommand();

			boolean firstUpload = true;
			IFpProjectBits projectBits = FedoraPackagerUtils.getVcsHandler(fpr);
			for (String file : uploadFiles) {
				// This won't find the .spec file since it has been removed
				// above.
				// We don't care, since it shouldn't get uploaded anyway.
				IResource candidate = fprContainer.findMember(new Path(file));
				if (candidate != null
						&& FedoraPackagerUtils.isValidUploadFile(candidate
								.getLocation().toFile())) {
					File newUploadFile = candidate.getLocation().toFile();
					SourcesFileUpdater sourcesUpdater = new SourcesFileUpdater(
							fpr, newUploadFile);
					// replace existing source, but not other files from this
					// SRPM
					sourcesUpdater.setShouldReplace(firstUpload);
					// Note that ignore file may not exist, yet
					projectBits.ignoreResource(candidate);
					if (uploadUrl != null) {
						upload.setUploadURL(uploadUrl);
					}
					upload.setFileToUpload(newUploadFile);
					// set SSL policy via the callback
					sslPolicyCallback.setSSLPolicy(upload, uploadUrl);
					upload.addCommandListener(sourcesUpdater);
					try {
						upload.call(new NullProgressMonitor());
						uploadedFiles.add(file);
						if (firstUpload) {
							firstUpload = false;
						}
					} catch (FileAvailableInLookasideCacheException e) {
						// ignore, imports that update an existing repo can have
						// identical files in an update, but these files don't
						// really need to be uploaded
					}

				} else {
					stageSet.add(file);
				}
			}
			result.setUploaded(uploadedFiles.toArray(new String[0]));
			monitor.subTask(RpmText.SRPMImportCommand_StagingChanges);
			// Do VCS update
			if (projectBits.updateVCS(monitor)
					.isOK()) {
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			// stage changes
			stageSet.add(fpr.getSourcesFile().getName());
			stageSet.add(projectBits.getIgnoreFileName());
			FedoraPackagerUtils.getVcsHandler(fpr).stageChanges(
					stageSet.toArray(new String[0]));
		} catch (CoreException | FedoraPackagerAPIException | IOException e) {
			throw new SRPMImportCommandException(e.getMessage(), e);
		}
		result.setSuccessful(true);
		return result;
	}

}
