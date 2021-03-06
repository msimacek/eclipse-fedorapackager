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

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.fedoraproject.eclipse.packager.FedoraProjectRoot;
import org.fedoraproject.eclipse.packager.api.DownloadSourceCommand;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class FedoraPackagerTest {

	private static final String EXAMPLE_FEDORA_PROJECT_ROOT = "resources/example-fedora-project"; // $NON-NLS-1$
	private FedoraPackager packager;
	private IProject packagerProject;
	private FedoraProjectRoot fpRoot;

	@Before
	public void setUp() throws IOException, CoreException {
		String dirName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_FEDORA_PROJECT_ROOT), null)).getFile();
		File origSourceDir = new File(dirName);

		packagerProject = TestsUtils.createProjectFromTemplate(origSourceDir,
				TestsUtils.getRandomUniqueName());
		fpRoot = new FedoraProjectRoot();
		fpRoot.initialize(packagerProject);
		assertNotNull(fpRoot);
		packager = new FedoraPackager(fpRoot);
		assertNotNull(packager);
	}

	@After
	public void tearDown() throws CoreException {
		this.packagerProject.delete(true, true, null);
	}

	/**
	 * Should be able to successfully instantiate a
	 * {@link DownloadSourceCommand} object.
	 * 
	 * @throws FedoraPackagerCommandNotFoundException
	 * @throws FedoraPackagerCommandInitializationException
	 */
	@Test
	public void canGetCommandInstance()
			throws FedoraPackagerCommandInitializationException,
			FedoraPackagerCommandNotFoundException {
		DownloadSourceCommand download = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		assertNotNull(download);
	}

	/**
	 * Should throw {@link FedoraPackagerCommandNotFoundException} for an
	 * unknown command.
	 * 
	 * @throws FedoraPackagerCommandNotFoundException
	 * @throws FedoraPackagerCommandInitializationException
	 */
	@Test(expected = FedoraPackagerCommandNotFoundException.class)
	public void shouldThrowFedoraPackagerCommandNotFoundException()
			throws FedoraPackagerCommandNotFoundException,
			FedoraPackagerCommandInitializationException {
		packager.getCommandInstance("SomeCommandWhichShouldNotBeThere");
	}

	/**
	 * @throws FedoraPackagerCommandNotFoundException
	 * 
	 */
	@Test(expected = FedoraPackagerCommandInitializationException.class)
	public void shouldThrowInitializationException()
			throws FedoraPackagerCommandInitializationException,
			FedoraPackagerCommandNotFoundException {
		DownloadSourceCommand download = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		// this should throw initialization exception
		download.initialize(fpRoot);
	}

}
