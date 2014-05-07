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
package org.fedoraproject.eclipse.packager.tests.commands;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.ChecksumValidListener;
import org.fedoraproject.eclipse.packager.api.DownloadSourceCommand;
import org.fedoraproject.eclipse.packager.api.DownloadSourcesJob;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.tests.utils.CorruptDownload;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Eclipse plug-in test for DownloadSourceCommand.
 */
public class DownloadSourceCommandTest {

	// project under test
	private GitTestProject testProject;
	// main interface class
	private FedoraPackager packager;
	// Fedora packager root
	private IProjectRoot fpRoot;

	/**
	 * Set up a Fedora project and run the command.
	 * 
	 */
	@Before
	public void setUp() throws Exception {
		this.testProject = new GitTestProject("eclipse-moreunit");
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);
	}

	@After
	public void tearDown() throws Exception {
		this.testProject.dispose();
	}

	@Test(expected = MalformedURLException.class)
	public void shouldThrowMalformedURLException()
			throws FedoraPackagerCommandInitializationException,
			FedoraPackagerCommandNotFoundException, MalformedURLException {
		DownloadSourceCommand downloadCmd = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		downloadCmd.setDownloadURL("very bad url");
	}

	/**
	 * Positive results test. Should work fine. Since this is downloading
	 * Eclipse sources it might take a while.
	 * 
	 * @throws CoreException
	 * @throws InterruptedException
	 * 
	 */
	@Test
	public void canDownloadSeveralFilesWithoutErrors() throws CoreException,
			InterruptedException, FedoraPackagerAPIException {
		// not using eclipse-fedorapackager for this test
		this.testProject.dispose();
		// The jpackage-utils package usually has 2 source files. That's why we
		// use the jpackage-utils package for testing
		this.testProject = new GitTestProject("javapackages-tools");
		this.fpRoot = FedoraPackagerUtils.getProjectRoot(this.testProject
				.getProject());
		this.packager = new FedoraPackager(fpRoot);
		DownloadSourceCommand downloadCmd = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		DownloadSourcesJob dsj = new DownloadSourcesJob("Download Job", downloadCmd, fpRoot,
				"http://pkgs.fedoraproject.org/repo/pkgs");
		dsj.schedule();
		dsj.join();
		assertTrue(dsj.getResult().equals(Status.OK_STATUS));
	}

	/**
	 * Test checksums of source files.
	 */
	@Test(expected = CommandListenerException.class)
	public void canDetectChecksumErrors()
			throws FedoraPackagerAPIException {
		DownloadSourceCommand downloadCmd = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		CorruptDownload checksumDestroyer = new CorruptDownload(fpRoot);
		ChecksumValidListener md5sumListener = new ChecksumValidListener(fpRoot);
		// Add checksum destroyer first, checksum checker after.
		downloadCmd.addCommandListener(checksumDestroyer); // should corrupt MD5
		downloadCmd.addCommandListener(md5sumListener); // want md5sum checking
		downloadCmd.call(new NullProgressMonitor());
	}

}
