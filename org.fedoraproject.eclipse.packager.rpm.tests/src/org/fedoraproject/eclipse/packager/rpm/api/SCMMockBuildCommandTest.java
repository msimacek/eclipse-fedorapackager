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

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.DownloadSourceCommand;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.DownloadFailedException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.SourcesUpToDateException;
import org.fedoraproject.eclipse.packager.rpm.api.MockBuildResult;
import org.fedoraproject.eclipse.packager.rpm.api.SCMMockBuildCommand;
import org.fedoraproject.eclipse.packager.rpm.api.SCMMockBuildCommand.RepoType;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SCMMockBuildCommandTest {
	// project under test
	private GitTestProject testProject;
	// Fedora packager root
	private IProjectRoot fpRoot;
	// main interface class
	private FedoraPackager packager;
	// download source command
	private DownloadSourceCommand download;
	private BranchConfigInstance bci;

	@Before
	public void setUp() throws InterruptedException, JGitInternalException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CoreException, InvalidProjectRootException, FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, SourcesUpToDateException, DownloadFailedException, CommandMisconfiguredException, CommandListenerException  {
		this.testProject = new GitTestProject("eclipse-fedorapackager"); //$NON-NLS-1$
		// switch to F15
		testProject.checkoutBranch("f15"); //$NON-NLS-1$
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);
		// need to have sources ready
		download = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		download.call(new NullProgressMonitor());
		bci = FedoraPackagerUtils.getVcsHandler(fpRoot).getBranchConfig();
	}

	@After
	public void tearDown() throws CoreException  {
		this.testProject.dispose();
	}

	@Test
	public void canCreateF15SCMMockBuild() throws CoreException,
			FedoraPackagerCommandInitializationException,
			FedoraPackagerCommandNotFoundException,
			CommandMisconfiguredException, UserNotInMockGroupException,
			CommandListenerException, MockBuildCommandException,
			MockNotInstalledException {
		SCMMockBuildCommand mockBuild = (SCMMockBuildCommand) packager
				.getCommandInstance(SCMMockBuildCommand.ID);
		MockBuildResult result = mockBuild
				.useDownloadedSourceDirectory(download.getDownloadFolderPath())
				.useBranch("f15") //$NON-NLS-1$
				.usePackage("eclipse-fedorapackager") //$NON-NLS-1$
				.useRepoPath(
						fpRoot.getContainer().getParent().getRawLocation()
								.toString()).useRepoType(RepoType.GIT)
				.useSpec(fpRoot.getSpecFile().getName()).branchConfig(bci)
				.call(new NullProgressMonitor());
		assertTrue(result.wasSuccessful());
		String resultDirectoryPath = result.getResultDirectoryPath().getFullPath().toOSString();
		assertNotNull(resultDirectoryPath);
		// should have created RPMs in the result directory
		boolean rpmfound = false;
		boolean srpmfound = false;
		File resultPath = new File(resultDirectoryPath);
		this.testProject.getProject().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		IContainer container = (IContainer) this.testProject.getProject()
				.findMember(new Path(resultPath.getName()));
		for (IResource file : container.members()) {
			if (file.getName().endsWith(".rpm")) { //$NON-NLS-1$
				// not interested in source RPMs
				if (!file.getName().endsWith(".src.rpm")) { //$NON-NLS-1$
					rpmfound = true;
				} else {
					srpmfound = true;
				}
			}
		}
		assertTrue(rpmfound);
		assertTrue(srpmfound);
	}
}
