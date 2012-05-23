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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand.BuildType;
import org.fedoraproject.eclipse.packager.rpm.api.errors.RpmBuildCommandException;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests for the RPM build command. This includes source RPM and prep tests.
 * 
 */
public class RpmBuildCommandTest {

	private static final String EXAMPLE_GIT_PROJECT_ROOT = "resources/example-git-project"; //$NON-NLS-1$

	// project under test
	private GitTestProject testProject;
	// main interface class
	private FedoraPackager packager;
	// Fedora packager root
	private IProjectRoot fpRoot;
	private BranchConfigInstance bci;

	/**
	 * Clone a test project to be used for testing.
	 * @throws InterruptedException 
	 * @throws CoreException 
	 * @throws InvalidRefNameException 
	 * @throws RefNotFoundException 
	 * @throws RefAlreadyExistsException 
	 * @throws JGitInternalException 
	 * @throws InvalidProjectRootException 
	 * @throws IOException 
	 * 
	 */
	@Before
	public void setUp() throws InterruptedException, JGitInternalException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CoreException, InvalidProjectRootException, IOException  {
		String dirName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_GIT_PROJECT_ROOT), null)).getFile();

		this.testProject = new GitTestProject("example", dirName); //$NON-NLS-1$
		testProject.checkoutBranch("f17"); //$NON-NLS-1$
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);
		bci = FedoraPackagerUtils.getVcsHandler(fpRoot).getBranchConfig();
	}

	/**
	 * @throws CoreException 
	 */
	@After
	public void tearDown() throws CoreException  {
		this.testProject.dispose();
	}

	/**
	 * Test method for
	 * {@link org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand#checkConfiguration()}
	 * . Should have thrown an exception. Command is not properly configured.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws RpmBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test(expected = CommandMisconfiguredException.class)
	public void testCheckConfiguration() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		build.call(new NullProgressMonitor());
	}

	/**
	 * This illustrates proper usage of {@link RpmEvalCommand}. This may take a
	 * long time.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws CoreException 
	 * @throws RpmBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test
	public void canBuildForLocalArchitecture() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CoreException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		RpmBuildResult result;
		List<String> distDefines = new ArrayList<String>();
		distDefines.add("--define"); //$NON-NLS-1$
		distDefines.add("dist .fc17"); //$NON-NLS-1$
		distDefines.add("--define"); //$NON-NLS-1$
		distDefines.add("fedora 17"); //$NON-NLS-1$
		build.buildType(BuildType.BINARY).branchConfig(bci);
		result = build.call(new NullProgressMonitor());
		assertTrue(result.isSuccessful());
		fpRoot.getContainer().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		IResource noArchFolder = fpRoot.getContainer().findMember(
				new Path("noarch")); //$NON-NLS-1$
		assertNotNull(noArchFolder);
		// there should be one RPM
		assertTrue(((IContainer) noArchFolder).members().length == 1);
	}

	/**
	 * Test preparing sources.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws CoreException 
	 * @throws IllegalArgumentException 
	 * @throws RpmBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test
	public void canPrepareSources() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CoreException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException, IllegalArgumentException  {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		List<String> nodeps = new ArrayList<String>(1);
		nodeps.add(RpmBuildCommand.NO_DEPS);
		RpmBuildResult result;
			result = build.buildType(BuildType.PREP).flags(nodeps)
					.branchConfig(bci).call(new NullProgressMonitor());
		assertTrue(result.isSuccessful());
		fpRoot.getContainer().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		IResource expandedSourcesFolder = fpRoot.getContainer().findMember(
				new Path("example-1")); //$NON-NLS-1$
		assertNotNull(expandedSourcesFolder);
		// there should be some files in that folder
		assertTrue(((IContainer) expandedSourcesFolder).members().length > 0);
		// put some confidence into returned result
		assertTrue(result.getBuildCommand().contains(RpmBuildCommand.NO_DEPS));
	}
	
	/**
	 * Test compiling.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws CoreException 
	 * @throws RpmBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test
	public void canRpmCompile() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CoreException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException  {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		RpmBuildResult result;
			result = build.buildType(BuildType.COMPILE).branchConfig(bci).call(new NullProgressMonitor());
		assertTrue(result.isSuccessful());
		fpRoot.getContainer().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		IResource expandedSourcesFolder = fpRoot.getContainer().findMember(
				new Path("example-1")); //$NON-NLS-1$
		assertNotNull(expandedSourcesFolder);
		// there should be some files in that folder
		assertTrue(((IContainer) expandedSourcesFolder).members().length > 0);
		// put some confidence into returned result
		assertTrue(result.getBuildCommand().contains("-bc")); //$NON-NLS-1$
		// should have created a binary example.out 
		assertNotNull(fpRoot.getContainer().findMember(
				new Path("example-1/build/example.out"))); //$NON-NLS-1$
		// should not have produced any RPMs
		assertNull(fpRoot.getContainer().findMember(
				new Path("noarch"))); //$NON-NLS-1$
	}
	
	/**
	 * Test install.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws CoreException 
	 * @throws RpmBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test
	public void canRpmInstall() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CoreException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		RpmBuildResult result;
			result = build.buildType(BuildType.INSTALL).branchConfig(bci).call(new NullProgressMonitor());
		assertTrue(result.isSuccessful());
		fpRoot.getContainer().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		IResource expandedSourcesFolder = fpRoot.getContainer().findMember(
				new Path("example-1")); //$NON-NLS-1$
		assertNotNull(expandedSourcesFolder);
		// there should be some files in that folder
		assertTrue(((IContainer) expandedSourcesFolder).members().length > 0);
		// put some confidence into returned result
		assertTrue(result.getBuildCommand().contains("-bi")); //$NON-NLS-1$
		// should have binary example.out
		assertNotNull(fpRoot.getContainer().findMember(
				new Path("example-1/build/example.out"))); //$NON-NLS-1$
		// should not have produced any RPMs
		assertNull(fpRoot.getContainer().findMember(
				new Path("noarch"))); //$NON-NLS-1$
	}

	/**
	 * Test create SRPM.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws CoreException 
	 * @throws IllegalArgumentException 
	 * @throws RpmBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test
	public void canCreateSRPM() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CoreException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException, IllegalArgumentException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		List<String> nodeps = new ArrayList<String>(1);
		nodeps.add(RpmBuildCommand.NO_DEPS);
		RpmBuildResult result;
			result = build.buildType(BuildType.SOURCE).flags(nodeps)
					.branchConfig(bci).call(new NullProgressMonitor());
		assertTrue(result.isSuccessful());
		// should contain at least one SRPM
		fpRoot.getContainer().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		boolean found = false;
		for (IResource res : fpRoot.getContainer().members()) {
			if (res.getName().contains("src.rpm")) { //$NON-NLS-1$
				found = true;
			}
		}
		assertTrue(found);
		String srpm = result.getAbsoluteSRPMFilePath();
		assertTrue(srpm.endsWith("src.rpm")); //$NON-NLS-1$
	}
}
