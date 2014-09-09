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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand.BuildType;
import org.junit.Test;

/**
 * Tests for the RPM build command. This includes source RPM and prep tests.
 * 
 */
public class RpmBuildCommandTest extends FedoraPackagerTest {


	/**
	 * Test method for
	 * input validation. 
	 * Should have thrown an exception. Command is not properly configured.
	 */
	@Test(expected = CommandMisconfiguredException.class)
	public void testCheckConfiguration() throws FedoraPackagerAPIException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		build.call(new NullProgressMonitor());
	}

	/**
	 * This illustrates proper usage of {@link RpmEvalCommand}. This may take a
	 * long time.
	 * @throws CoreException 
	 */
	@Test
	public void canBuildForLocalArchitecture() throws FedoraPackagerAPIException, CoreException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		List<String> distDefines = new ArrayList<>();
		distDefines.add("--define"); //$NON-NLS-1$
		distDefines.add("dist .fc17"); //$NON-NLS-1$
		distDefines.add("--define"); //$NON-NLS-1$
		distDefines.add("fedora 17"); //$NON-NLS-1$
		build.buildType(BuildType.BINARY).branchConfig(bci);
		RpmBuildResult result = build.call(new NullProgressMonitor());
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
	 * @throws CoreException 
	 */
	@Test
	public void canPrepareSources() throws FedoraPackagerAPIException, CoreException  {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		List<String> nodeps = new ArrayList<>(1);
		nodeps.add(RpmBuildCommand.NO_DEPS);
		RpmBuildResult result = build.buildType(BuildType.PREP).flags(nodeps)
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
	 * @throws CoreException 
	 */
	@Test
	public void canRpmCompile() throws FedoraPackagerAPIException, CoreException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		RpmBuildResult result = build.buildType(BuildType.COMPILE).branchConfig(bci).call(new NullProgressMonitor());
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
	 * @throws CoreException 
	 */
	@Test
	public void canRpmInstall() throws FedoraPackagerAPIException, CoreException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		RpmBuildResult result = build.buildType(BuildType.INSTALL).branchConfig(bci).call(new NullProgressMonitor());
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
	 * @throws CoreException 
	 */
	@Test
	public void canCreateSRPM() throws FedoraPackagerAPIException, CoreException {
		RpmBuildCommand build = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		List<String> nodeps = new ArrayList<>(1);
		nodeps.add(RpmBuildCommand.NO_DEPS);
		RpmBuildResult result = build.buildType(BuildType.SOURCE).flags(nodeps)
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
