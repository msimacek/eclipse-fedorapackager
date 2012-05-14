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


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

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
import org.fedoraproject.eclipse.packager.rpm.api.MockBuildCommand;
import org.fedoraproject.eclipse.packager.rpm.api.MockBuildResult;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand.BuildType;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildResult;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.RpmBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Some basic tests for the mock build command.
 */
public class MockBuildCommandTest {

	// project under test
	private GitTestProject testProject;
	// main interface class
	private FedoraPackager packager;
	// Fedora packager root
	private IProjectRoot fpRoot;
	// Path to SRPM
	private String srpmPath;
	private BranchConfigInstance bci;
	
	@Before
	public void setUp() throws InterruptedException, JGitInternalException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CoreException, InvalidProjectRootException, FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, SourcesUpToDateException, DownloadFailedException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException  {
		this.testProject = new GitTestProject("eclipse-fedorapackager"); //$NON-NLS-1$
		// switch to F15
		testProject.checkoutBranch("f15"); //$NON-NLS-1$
		testProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);
		// need to have sources ready
		DownloadSourceCommand download = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		download.call(new NullProgressMonitor());
		this.bci = FedoraPackagerUtils.getVcsHandler(fpRoot).getBranchConfig();
		// build fresh SRPM
		RpmBuildResult srpmBuildResult = createSRPM();
		this.srpmPath = srpmBuildResult.getAbsoluteSRPMFilePath();
	}

	@After
	public void tearDown() throws CoreException {
		this.testProject.dispose();
	}

	/**
	 * This test may take >= 15 mins to run. Be patient :)
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws FileNotFoundException 
	 * @throws IllegalArgumentException 
	 * @throws MockNotInstalledException 
	 * @throws MockBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws UserNotInMockGroupException 
	 * @throws CommandMisconfiguredException 
	 * @throws CoreException 
	 * 
	 */
	@Test
	public void canCreateF15MockBuild() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandMisconfiguredException, UserNotInMockGroupException, CommandListenerException, MockBuildCommandException, MockNotInstalledException, IllegalArgumentException, FileNotFoundException, CoreException {
		MockBuildCommand mockBuild = (MockBuildCommand) packager
				.getCommandInstance(MockBuildCommand.ID);
		MockBuildResult result = mockBuild.pathToSRPM(srpmPath)
				.branchConfig(bci).call(new NullProgressMonitor());
		assertTrue(result.wasSuccessful());
		String resultDirectoryPath = result.getResultDirectoryPath().getFullPath().toOSString();
		assertNotNull(resultDirectoryPath);
		// should have created RPMs in the result directory
		boolean found = false;
		this.testProject.getProject().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		File resultPath = new File(resultDirectoryPath);
		IContainer container = (IContainer) this.testProject.getProject()
				.findMember(new Path(resultPath.getName()));
		for (IResource file: container.members()) {
			if (file.getName().endsWith(".rpm")) { //$NON-NLS-1$
				// not interested in source RPMs
				if (!file.getName().endsWith(".src.rpm")) { //$NON-NLS-1$
					found = true;
				}
			}
		}
		assertTrue(found);
	}
	
	/**
	 * Helper to create an SRPM which we can use for a mock build.
	 * 
	 * @return the built result.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws RpmBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	private RpmBuildResult createSRPM() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException  {
		List<String> nodeps = new ArrayList<String>(1);
		nodeps.add(RpmBuildCommand.NO_DEPS);
		// get RPM build command in order to produce an SRPM
		RpmBuildCommand srpmBuild = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		// want SRPM build
		srpmBuild.buildType(BuildType.SOURCE).flags(nodeps);
		// set branch config
		srpmBuild.branchConfig(bci);
		return srpmBuild.call(new NullProgressMonitor());
	}
}
