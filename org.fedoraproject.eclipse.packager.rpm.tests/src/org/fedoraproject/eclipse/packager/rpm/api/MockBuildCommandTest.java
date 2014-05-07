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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand.BuildType;
import org.junit.Before;
import org.junit.Test;

/**
 * Some basic tests for the mock build command.
 */
public class MockBuildCommandTest extends FedoraPackagerTest {

	private String srpmPath;

	@Override
	@Before
	public void setUp() throws InterruptedException, JGitInternalException, GitAPIException, CoreException, IOException, FedoraPackagerAPIException  {
		super.setUp();
		testProject.checkoutBranch("f15"); //$NON-NLS-1$
		testProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

		// build fresh SRPM
		RpmBuildResult srpmBuildResult = createSRPM();
		this.srpmPath = srpmBuildResult.getAbsoluteSRPMFilePath();
	}

	/**
	 * This test may take >= 15 mins to run. Be patient :)
	 * @throws FileNotFoundException 
	 * @throws IllegalArgumentException 
	 * @throws CoreException 
	 * 
	 */
	@Test
	public void canCreateMockBuild() throws FedoraPackagerAPIException, FileNotFoundException, CoreException {
		MockBuildCommand mockBuild = (MockBuildCommand) packager
				.getCommandInstance(MockBuildCommand.ID);
		MockBuildResult result = mockBuild.pathToSRPM(srpmPath)
				.branchConfig(bci).call(new NullProgressMonitor());
		assertTrue(result.isSuccessful());
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
	 */
	private RpmBuildResult createSRPM() throws FedoraPackagerAPIException  {
		List<String> nodeps = new ArrayList<>(1);
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
