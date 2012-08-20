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
package org.fedoraproject.eclipse.packager.tests.utils.git;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

@Ignore
public class GitTestCase {

	private GitTestProject project;
	private IProject iProject;
	private IProjectRoot fedoraprojectRoot;

	@Before
	public void setUp() throws InterruptedException,
			InvalidProjectRootException {
		project = new GitTestProject("itext");
		iProject = project.getProject();
		// create a fedoraprojectRoot for this project
		fedoraprojectRoot = FedoraPackagerUtils.getProjectRoot((iProject));
	}

	/**
	 * @return the fedoraprojectRoot
	 */
	public IProjectRoot getFedoraprojectRoot() {
		return fedoraprojectRoot;
	}

	@After
	public void tearDown() throws CoreException {
		project.dispose();
		org.eclipse.egit.core.Activator.getDefault().getRepositoryCache()
				.clear();
	}

	/**
	 * @return the project
	 */
	public GitTestProject getProject() {
		return project;
	}

	/**
	 * @return the iProject
	 */
	public IProject getiProject() {
		return iProject;
	}

}
