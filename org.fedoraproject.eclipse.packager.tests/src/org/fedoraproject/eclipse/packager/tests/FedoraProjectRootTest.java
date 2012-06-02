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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.linuxtools.rpm.ui.editor.parser.Specfile;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.FedoraProjectRoot;
import org.fedoraproject.eclipse.packager.ILookasideCache;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.SourcesFile;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class FedoraProjectRootTest {

	private IProject projectResource;
	private IProjectRoot fpRoot;
	private GitTestProject gitTestProject;

	private static final String SOURCE_FILE_NAME = "project_sources.zip";
	private static final String PACKAGE_NAME = "example-fedora-project";
	private static final String EXAMPLE_FEDORA_PROJECT_ROOT = "resources/example-fedora-project"; // $NON-NLS-1$

	@Before
	public void setUp() throws IOException, CoreException {
		String dirName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_FEDORA_PROJECT_ROOT), null)).getFile();
		File copySource = new File(dirName);

		projectResource = TestsUtils.createProjectFromTemplate(copySource);
		// Users should really use FedoraPackagerUtils.getProjectRoot(), but
		// this
		// doesn't work for this case.
		fpRoot = new FedoraProjectRoot();
		fpRoot.initialize(projectResource);
		assertNotNull(fpRoot);
	}

	@After
	public void tearDown() throws Exception {
		projectResource.delete(true, true, null);
		fpRoot = null;
		if (gitTestProject != null) {
			gitTestProject.dispose();
		}
	}

	@Test
	public void canCreateFedoraProjectRoot() throws InterruptedException,
			InvalidProjectRootException {
		// Dummy Fedora project root
		fpRoot = null;
		assertNull(fpRoot);
		fpRoot = new FedoraProjectRoot();
		fpRoot.initialize(projectResource);
		assertNotNull(fpRoot);

		// Git case
		fpRoot = null;
		gitTestProject = new GitTestProject("apache-commons-lang");
		fpRoot = FedoraPackagerUtils
				.getProjectRoot(gitTestProject.getProject());
		assertNotNull(fpRoot);

	}

	@Test
	public void testGetContainer() throws InterruptedException,
			InvalidProjectRootException {
		assertNotNull(fpRoot.getContainer());
		assertSame(projectResource, fpRoot.getContainer());

		// Git case
		gitTestProject = new GitTestProject("apache-commons-math");
		fpRoot = FedoraPackagerUtils
				.getProjectRoot(gitTestProject.getProject());
		assertNotNull(fpRoot);
		// Project is container
		assertSame(gitTestProject.getProject(), fpRoot.getContainer());

	}

	@Test
	public void testGetProject() throws InterruptedException,
			InvalidProjectRootException {
		// Git case
		fpRoot = null;
		gitTestProject = new GitTestProject("plexus-utils");
		fpRoot = FedoraPackagerUtils
				.getProjectRoot(gitTestProject.getProject());
		assertNotNull(fpRoot);
		// Project is container
		assertSame(gitTestProject.getProject(), fpRoot.getProject());

	}

	@Test
	public void testGetSourcesFile() {
		SourcesFile sourcesFile = fpRoot.getSourcesFile();
		assertNotNull(sourcesFile.getCheckSum(SOURCE_FILE_NAME));
	}

	@Test
	public void testGetSpecFile() {
		IFile specFile = fpRoot.getSpecFile();
		assertNotNull(specFile);
		assertEquals("spec", specFile.getFileExtension());
	}

	@Test
	public void testGetSpecfileModel() {
		Specfile specModel = fpRoot.getSpecfileModel();
		assertEquals(PACKAGE_NAME, specModel.getName());
	}

	@Test
	public void testGetProjectType() throws InterruptedException,
			InvalidProjectRootException {
		// Git case
		fpRoot = null;
		gitTestProject = new GitTestProject("maven-site-plugin");
		fpRoot = FedoraPackagerUtils
				.getProjectRoot(gitTestProject.getProject());
		assertNotNull(fpRoot);

	}

	@Test
	public void testGetLookAsideCache() {
		ILookasideCache lookasideCache = fpRoot.getLookAsideCache();
		assertNotNull(lookasideCache);
		// should be initialized with default values
		assertEquals(FedoraPackagerPreferencesConstants.DEFAULT_LOOKASIDE_DOWNLOAD_URL, lookasideCache
				.getDownloadUrl().toString());
		assertEquals(FedoraPackagerPreferencesConstants.DEFAULT_LOOKASIDE_UPLOAD_URL, lookasideCache
				.getUploadUrl().toString());
	}

	@Test
	public void canRetrieveNVRs() throws InterruptedException,
			JGitInternalException, GitAPIException, CoreException,
			InvalidProjectRootException {
		fpRoot = null;
		gitTestProject = new GitTestProject("eclipse-mylyn-tasks");
		gitTestProject.checkoutBranch("f15");
		fpRoot = FedoraPackagerUtils
				.getProjectRoot(gitTestProject.getProject());
		assertNotNull(fpRoot);
		String[] nvrs = fpRoot.getPackageNVRs(FedoraPackagerUtils
				.getVcsHandler(fpRoot).getBranchConfig());
		// expected list
		String[] expectedNvrs = new String[] {
				"eclipse-mylyn-tasks-3.5.1-4.fc15",
				"eclipse-mylyn-tasks-bugzilla-3.5.1-4.fc15",
				"eclipse-mylyn-tasks-trac-3.5.1-4.fc15",
				"eclipse-mylyn-tasks-web-3.5.1-4.fc15" };
		assertEquals(4, nvrs.length);
		assertArrayEquals(expectedNvrs, nvrs);
	}

}
