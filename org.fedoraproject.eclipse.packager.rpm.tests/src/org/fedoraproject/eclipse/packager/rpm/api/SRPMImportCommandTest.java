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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
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
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.UploadSourceCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.DownloadFailedException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.SourcesUpToDateException;
import org.fedoraproject.eclipse.packager.rpm.api.ISRPMImportCommandSLLPolicyCallback;
import org.fedoraproject.eclipse.packager.rpm.api.SRPMImportCommand;
import org.fedoraproject.eclipse.packager.rpm.api.SRPMImportResult;
import org.fedoraproject.eclipse.packager.rpm.api.errors.SRPMImportCommandException;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class SRPMImportCommandTest implements ISRPMImportCommandSLLPolicyCallback {
	// project under test
	private IProject testProject;
	// main interface class
	private static final String UPLOAD_URL_PROP = "org.fedoraproject.eclipse.packager.tests.LookasideUploadUrl"; //$NON-NLS-1$
	private String uploadURLForTesting;
	private String srpmPath;
	private Git git;
	private String badSrpmPath;

	@Before
	public void setup() throws IOException, CoreException  {
		String uploadURL = System.getProperty(UPLOAD_URL_PROP);
		if (uploadURL == null) {
			fail(UPLOAD_URL_PROP + " not set"); //$NON-NLS-1$
		}
		srpmPath = FileLocator
				.toFileURL(
						FileLocator
								.find(FrameworkUtil.getBundle(this.getClass()),
										new Path(
												"resources/eclipse-mylyn-tasks-3.6.0-2.fc17.src.rpm"), //$NON-NLS-1$
										null)).getFile();
		badSrpmPath = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path("resources/ed-1.5-2.fc16.src.rpm"), null)) //$NON-NLS-1$
				.getFile();
		this.uploadURLForTesting = uploadURL;
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
	public void tearDown() throws CoreException  {
		this.testProject.delete(true, null);
	}

	@Test
	public void canImportSRPM() throws SRPMImportCommandException, InvalidProjectRootException, NoWorkTreeException, IOException, CoreException, FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, URISyntaxException, SourcesUpToDateException, DownloadFailedException, CommandMisconfiguredException, CommandListenerException  {
		SRPMImportCommand srpmImport = new SRPMImportCommand(srpmPath,
				testProject, testProject, uploadURLForTesting, this);
		SRPMImportResult result = srpmImport.call(new NullProgressMonitor());
		IProjectRoot fpr;
		fpr = FedoraPackagerUtils.getProjectRoot(testProject);
		FedoraPackager packager = new FedoraPackager(fpr);
		assertTrue(packager.getFedoraProjectRoot().getContainer().getLocation()
				.append("/redhat-bugzilla-custom-transitions.txt").toFile() //$NON-NLS-1$
				.exists());
		assertTrue(packager.getFedoraProjectRoot().getContainer().getLocation()
				.append("/eclipse-mylyn-tasks-R_3_6_0-fetched-src.tar.bz2") //$NON-NLS-1$
				.toFile().exists());
		assertTrue(packager.getFedoraProjectRoot().getContainer().getLocation()
				.append("/eclipse-mylyn-tasks-3.6.0-2.fc17.src.rpm").toFile() //$NON-NLS-1$
				.exists());
		// ensure files are added to git
		Set<String> unaddedSet = git.status().call().getUntracked();
		assertTrue(!unaddedSet.contains(packager.getFedoraProjectRoot()
				.getSourcesFile().getName()));
		assertTrue(!unaddedSet.contains(packager.getFedoraProjectRoot()
				.getSpecFile().getName()));
		assertTrue(!unaddedSet.contains(packager.getFedoraProjectRoot()
				.getIgnoreFile().getName()));
		// ensure files uploaded
		fpr.getSourcesFile().deleteSource(
				"eclipse-mylyn-tasks-R_3_6_0-fetched-src.tar.bz2"); //$NON-NLS-1$
		fpr.getProject().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		assertTrue(!packager.getFedoraProjectRoot().getContainer()
				.getLocation()
				.append("/eclipse-mylyn-tasks-R_3_6_0-fetched-src.tar.bz2") //$NON-NLS-1$
				.toFile().exists());
		DownloadSourceCommand download = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		ChecksumValidListener md5sumListener = new ChecksumValidListener(fpr);
		download.addCommandListener(md5sumListener); // want md5sum checking
		download.setDownloadURL("http://" //$NON-NLS-1$
				+ new URI(uploadURLForTesting).getHost());
		download.call(new NullProgressMonitor());
		assertTrue(packager.getFedoraProjectRoot().getContainer().getLocation()
				.append("/eclipse-mylyn-tasks-R_3_6_0-fetched-src.tar.bz2") //$NON-NLS-1$
				.toFile().exists());
		assertTrue(packager.getFedoraProjectRoot().getSourcesFile()
				.getMissingSources().isEmpty());
		// ensure files are tracked
		for (String file : result.getUploaded()) {
			assertTrue(packager.getFedoraProjectRoot().getSourcesFile()
					.getSources().keySet().contains(file));
		}
		assertTrue(packager.getFedoraProjectRoot().getSourcesFile()
				.getSources().keySet()
				.contains("eclipse-mylyn-tasks-R_3_6_0-fetched-src.tar.bz2")); //$NON-NLS-1$
		// ensure spec is named correctly
		assertTrue(packager.getFedoraProjectRoot().getSpecFile().getName()
				.equals("eclipse-mylyn-tasks.spec")); //$NON-NLS-1$
	}

	@Test
	public void incorrectSpecFails() {
		SRPMImportCommand srpmImport = new SRPMImportCommand(badSrpmPath,
				testProject, testProject, uploadURLForTesting, this);
		try {
			srpmImport.call(new NullProgressMonitor());
		} catch (SRPMImportCommandException e) {
			assertTrue(e.getCause() instanceof SRPMImportCommandException);
			return;
		}
		fail("Should not reach here."); //$NON-NLS-1$
	}

	@Override
	public void setSSLPolicy(UploadSourceCommand uploadCmd, String uploadUrl) {
		// enable SLL authentication
		uploadCmd.setFedoraSSLEnabled(true);
	}
}
