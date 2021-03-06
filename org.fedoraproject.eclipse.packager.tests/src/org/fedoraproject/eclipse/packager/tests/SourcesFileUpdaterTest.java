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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.SourcesFile;
import org.fedoraproject.eclipse.packager.api.SourcesFileUpdater;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/**
 * Test for {@code sources} file updater, {@link SourcesFileUpdater}.
 *
 */
public class SourcesFileUpdaterTest {

	private IProjectRoot fpRoot;
	private File uploadedFile;
	private IProject testProject;

	private static final String EXAMPLE_FEDORA_PROJECT_ROOT = "resources/example-fedora-project"; // $NON-NLS-1$
	private static final String EXAMPLE_UPLOAD_FILE = "resources/callgraph-factorial.zip"; // $NON-NLS-1$

	@Before
	public void setUp() throws IOException, CoreException,
			InvalidProjectRootException {
		String dirName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_FEDORA_PROJECT_ROOT), null)).getFile();
		File copySource = new File(dirName);

		testProject = TestsUtils.createProjectFromTemplate(copySource,
				TestsUtils.getRandomUniqueName());
		// we need the property set otherwise instantiation of the project root
		// fails.
		testProject
				.setPersistentProperty(PackagerPlugin.PROJECT_PROP, "true" );// unused value
		fpRoot = FedoraPackagerUtils.getProjectRoot(testProject);
		assertNotNull(fpRoot);

		String fileName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_UPLOAD_FILE), null)).getFile();
		uploadedFile = new File(fileName);
		assertNotNull(uploadedFile);
	}

	@After
	public void tearDown() throws CoreException {
		this.testProject.delete(true, true, null);
	}

	@Test
	public void canReplaceSourcesFile() throws IOException,
			CommandListenerException {
		// sources file pre-update
		File sourcesFile = new File(testProject.getLocation().toFile()
				.getAbsolutePath()
				+ File.separatorChar + SourcesFile.SOURCES_FILENAME);
		final String sourcesFileContentPre = TestsUtils
				.readContents(sourcesFile);
		// sanity check
		assertEquals("20a16942e761f9281591891834997fe5  project_sources.zip",
				sourcesFileContentPre);
		SourcesFileUpdater sourcesUpdater = new SourcesFileUpdater(fpRoot,
				uploadedFile);
		// want to replace :)
		sourcesUpdater.setShouldReplace(true);
		// this should update the sources file
		sourcesUpdater.postExecution();
		final String sourcesFileContentPost = TestsUtils
				.readContents(sourcesFile);
		assertNotSame(sourcesFileContentPre, sourcesFileContentPost);
		assertEquals(SourcesFile.calculateChecksum(uploadedFile) + "  "
				+ uploadedFile.getName(), sourcesFileContentPost);
	}

	@Test
	public void canUpdateSourcesFile() throws IOException,
			CommandListenerException {
		// sources file pre-update
		File sourcesFile = new File(testProject.getLocation().toFile()
				.getAbsolutePath()
				+ File.separatorChar + SourcesFile.SOURCES_FILENAME);
		final String sourcesFileContentPre = TestsUtils
				.readContents(sourcesFile);
		// sanity check
		assertEquals("20a16942e761f9281591891834997fe5  project_sources.zip",
				sourcesFileContentPre);
		SourcesFileUpdater sourcesUpdater = new SourcesFileUpdater(fpRoot,
				uploadedFile);
		// this should update the sources file
		sourcesUpdater.postExecution();
		final String sourcesFileContentPost = TestsUtils
				.readContents(sourcesFile);
		assertNotSame(sourcesFileContentPre, sourcesFileContentPost);
		final String expectedSourcesFileContentPost = sourcesFileContentPre
				+ "\n" + SourcesFile.calculateChecksum(uploadedFile) + "  "
				+ uploadedFile.getName();
		assertEquals(expectedSourcesFileContentPost, sourcesFileContentPost);
	}

}
