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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.ChecksumValidListener;
import org.fedoraproject.eclipse.packager.api.DownloadSourceCommand;
import org.fedoraproject.eclipse.packager.api.UploadSourceCommand;
import org.fedoraproject.eclipse.packager.api.UploadSourceResult;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.DownloadFailedException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.SourcesUpToDateException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.SRPMImportCommandException;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class SRPMImportCommandTest implements
		ISRPMImportCommandSLLPolicyCallback {
	
	private static final String MOCK_DOWNLOAD_FILE = "resources/eclipse-mylyn-tasks-R_3_7_0-fetched-src.tar.bz2"; //$NON-NLS-1$

	// project under test
	private IProject testProject;
	// main interface class
	private String uploadURLForTesting;
	private String srpmPath;
	private Git git;
	private String badSrpmPath;

	private File mockDownloadFile;

	@Before
	public void setup() throws IOException, CoreException {
		String exampleGitdirPath = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(MOCK_DOWNLOAD_FILE), null)).getFile();

		mockDownloadFile = new File(exampleGitdirPath);

		this.uploadURLForTesting = "https://pkgs.fedoraproject.org/repo/pkgs/"; //$NON-NLS-1$
		srpmPath = FileLocator
				.toFileURL(
						FileLocator.find(
								FrameworkUtil.getBundle(this.getClass()),
								new Path(
										"resources/eclipse-mylyn-tasks-3.7.0-3.fc17.src.rpm"), //$NON-NLS-1$
								null)).getFile();
		badSrpmPath = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path("resources/ed-1.5-2.fc16.src.rpm"), null)) //$NON-NLS-1$
				.getFile();

		testProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("eclipse-mylyn-tasks"); //$NON-NLS-1$
		testProject.create(null);
		testProject.open(null);
		testProject
				.setPersistentProperty(PackagerPlugin.PROJECT_PROP, "true" /* unused value */); //$NON-NLS-1$
		InitCommand ic = new InitCommand();
		ic.setDirectory(testProject.getLocation().toFile());
		ic.setBare(false);
		Repository repository = ic.call().getRepository();
		git = new Git(repository);
		ConnectProviderOperation connect = new ConnectProviderOperation(
				testProject);
		connect.execute(null);
	}

	@After
	public void tearDown() throws CoreException {
		this.testProject.delete(true, null);
	}

	@Test
	public void canImportSRPM() throws SRPMImportCommandException,
			InvalidProjectRootException, NoWorkTreeException, IOException,
			CoreException, FedoraPackagerCommandInitializationException,
			SourcesUpToDateException, DownloadFailedException,
			CommandMisconfiguredException, CommandListenerException {

		SRPMImportCommand srpmImport = new SRPMImportCommand(
				srpmPath, testProject, testProject, uploadURLForTesting, this){
			@Override
			protected UploadSourceCommand getUploadSourceCommand() {
				return new UploadSourceCommand() {
					@Override
					public UploadSourceResult call(IProgressMonitor subMonitor) throws CommandListenerException {
						callPreExecListeners();
						callPostExecListeners();
						return null;
					}
					
					@Override
					public UploadSourceCommand setUploadURL(String uploadURL){
						return this;
					}
				};
			}
		};
		
		SRPMImportResult result = srpmImport.call(new NullProgressMonitor());
		final IProjectRoot fpr = FedoraPackagerUtils.getProjectRoot(testProject);

		assertTrue(fpr.getContainer().getLocation()
				.append("/redhat-bugzilla-custom-transitions.txt").toFile() //$NON-NLS-1$
				.exists());
		assertTrue(fpr.getContainer().getLocation()
				.append("/eclipse-mylyn-tasks-R_3_7_0-fetched-src.tar.bz2") //$NON-NLS-1$
				.toFile().exists());
		assertTrue(fpr.getContainer().getLocation()
				.append("/eclipse-mylyn-tasks-3.7.0-3.fc17.src.rpm").toFile() //$NON-NLS-1$
				.exists());
		// ensure files are added to git
		Set<String> unaddedSet = git.status().call().getUntracked();
		assertTrue(!unaddedSet.contains(fpr.getSourcesFile().getName()));
		assertTrue(!unaddedSet.contains(fpr.getSpecFile().getName()));
		assertTrue(!unaddedSet.contains(FedoraPackagerUtils.getVcsHandler(fpr)
				.getIgnoreFileName()));
		// ensure files uploaded
		fpr.getSourcesFile().deleteSource(
				"eclipse-mylyn-tasks-R_3_7_0-fetched-src.tar.bz2"); //$NON-NLS-1$
		fpr.getProject().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		assertTrue(!fpr.getContainer().getLocation()
				.append("/eclipse-mylyn-tasks-R_3_7_0-fetched-src.tar.bz2") //$NON-NLS-1$
				.toFile().exists());
		
		DownloadSourceCommand download = new DownloadSourceCommand(){
			@Override
			protected void download(IProgressMonitor subMonitor, IFile fileToDownload, java.net.URL fileURL) throws IOException, CoreException {
				TestsUtils.copyFileContents(mockDownloadFile, new File(fileToDownload.getParent().getLocationURI()), false);
				fpr.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			};
		};
		download.initialize(fpr);

		ChecksumValidListener md5sumListener = new ChecksumValidListener(fpr);
		download.addCommandListener(md5sumListener); // want md5sum checking
		download.setDownloadURL(uploadURLForTesting);
		download.call(new NullProgressMonitor());
		assertTrue(fpr.getContainer().getLocation()
				.append("/eclipse-mylyn-tasks-R_3_7_0-fetched-src.tar.bz2") //$NON-NLS-1$
				.toFile().exists());
		assertTrue(fpr.getSourcesFile().getMissingSources().isEmpty());
		// ensure files are tracked
		for (String file : result.getUploaded()) {
			assertTrue(fpr.getSourcesFile().getSources().keySet()
					.contains(file));
		}
		assertTrue(fpr.getSourcesFile().getSources().keySet()
				.contains("eclipse-mylyn-tasks-R_3_7_0-fetched-src.tar.bz2")); //$NON-NLS-1$
		// ensure spec is named correctly
		assertTrue(fpr.getSpecFile().getName()
				.equals("eclipse-mylyn-tasks.spec")); //$NON-NLS-1$
	}

	@Test(expected = SRPMImportCommandException.class)
	public void incorrectSpecFails() throws SRPMImportCommandException {
		SRPMImportCommand srpmImport = new SRPMImportCommand(badSrpmPath,
				testProject, testProject, uploadURLForTesting, this);
		srpmImport.call(new NullProgressMonitor());
	}

	@Override
	public void setSSLPolicy(UploadSourceCommand uploadCmd, String uploadUrl) {
		// enable SLL authentication
		uploadCmd.setFedoraSSLEnabled(true);
	}
}
