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
package org.fedoraproject.eclipse.packager.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.SourcesFile;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.git.FpGitProjectBits;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class FedoraPackagerUtilsTest {

	private static final String EXAMPLE_FEDORA_PROJECT_ROOT = "resources/example-fedora-project"; // $NON-NLS-1$
	private IProject packagerProject;
	private File origSourceDir;
	private GitTestProject gitProject;

	@Before
	public void setUp() throws IOException, CoreException {
		String dirName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_FEDORA_PROJECT_ROOT), null)).getFile();
		origSourceDir = new File(dirName);

		packagerProject = TestsUtils.createProjectFromTemplate(origSourceDir,
				TestsUtils.getRandomUniqueName());
		// we need the property set otherwise instantiation of the project root
		// fails.
		packagerProject.setPersistentProperty(PackagerPlugin.PROJECT_PROP,
				"true" /* unused value */);
	}

	@After
	public void tearDown() throws CoreException {
		this.packagerProject.delete(true, true, null);
		if (gitProject != null) {
			gitProject.dispose();
		}
	}

	@Test
	public void canGetProjectRootFromResource() throws CoreException {
		try {
			IProjectRoot fpRoot = FedoraPackagerUtils
					.getProjectRoot(packagerProject);
			assertNotNull(fpRoot);
		} catch (InvalidProjectRootException e) {
			fail("Should have been a valid project root");
		}
		// delete spec file so that project root becomes invalid
		String specFileName = null;
		for (IResource file : packagerProject.members()) {
			if (file.getName().endsWith(".spec")) {
				specFileName = file.getName();
				file.delete(true, null);
			}
		}
		assertNotNull(specFileName);
		assertNull(packagerProject.findMember(new Path(specFileName)));
		try {
			FedoraPackagerUtils.getProjectRoot(packagerProject);
			fail("Not valid due to missing spec file.");
		} catch (InvalidProjectRootException e) {
			// pass
		}
		// Re-add spec file
		File specFile = new File(origSourceDir.getAbsolutePath()
				+ File.separatorChar + origSourceDir.getName() + ".spec");
		IFile newSpecFile = packagerProject.getFile(packagerProject.getName()
				+ ".spec");
		try (FileInputStream in = new FileInputStream(specFile)) {
			newSpecFile.create(in, false, null);
		} catch (IOException e) {
			// ignore
		}
		assertNotNull(packagerProject.findMember(new Path(specFileName)));
		// delete "sources" file so that project root becomes invalid
		for (IResource file : packagerProject.members()) {
			if (file.getName().equals(SourcesFile.SOURCES_FILENAME)) {
				file.delete(true, null);
			}
		}
		assertNull(packagerProject.findMember(new Path(
				SourcesFile.SOURCES_FILENAME)));
		try {
			FedoraPackagerUtils.getProjectRoot(packagerProject);
			fail("Not valid due to missing sources file.");
		} catch (InvalidProjectRootException e) {
			// pass
		}
	}

	@Test
	public void testGetProjectType() throws InterruptedException,
			InvalidProjectRootException {
		gitProject = new GitTestProject("jsch");
		IProjectRoot fproot = FedoraPackagerUtils.getProjectRoot(gitProject
				.getProject());
		assertNotNull(fproot);
	}

	@Test
	public void testGetVcsHandler() throws InterruptedException,
			InvalidProjectRootException {
		gitProject = new GitTestProject("jzlib");
		IProjectRoot fproot = FedoraPackagerUtils.getProjectRoot(gitProject
				.getProject());
		assertNotNull(fproot);
		IFpProjectBits projectBits = FedoraPackagerUtils.getVcsHandler(fproot);
		assertTrue(projectBits instanceof FpGitProjectBits);
	}

}
