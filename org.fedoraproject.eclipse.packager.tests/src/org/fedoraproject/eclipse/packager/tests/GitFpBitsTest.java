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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.fedoraproject.eclipse.packager.git.FpGitProjectBits;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestCase;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.Test;

public class GitFpBitsTest extends GitTestCase {

	@Test
	public void testGetCurrentBranchName() throws JGitInternalException, GitAPIException, CoreException  {
		FpGitProjectBits projectBits = (FpGitProjectBits) FedoraPackagerUtils
				.getVcsHandler(getFedoraprojectRoot());
		assertNotNull(projectBits);
		// make sure we meet pre-condition (we should be on master)
		assertEquals("master", projectBits.getCurrentBranchName());
		GitTestProject testProject = getProject();
		// switch to branch f13
		testProject.checkoutBranch("f13");
		assertEquals("f13", projectBits.getCurrentBranchName());
		// switch to branch fc6
		testProject.checkoutBranch("fc6");
		assertEquals("fc6", projectBits.getCurrentBranchName());
	}

	@Test
	public void testGetBranchName() {
		// this should do initialization
		FpGitProjectBits projectBits = (FpGitProjectBits) FedoraPackagerUtils
				.getVcsHandler(getFedoraprojectRoot());
		assertNotNull(projectBits);
		assertNotNull(projectBits.getBranchName("f7")); // should be there
		assertNotNull(projectBits.getBranchName("master")); // master mapped to
															// devel
	}

	@Test
	public void testGetDistVal() throws JGitInternalException, GitAPIException, CoreException  {
		FpGitProjectBits projectBits = (FpGitProjectBits) FedoraPackagerUtils
				.getVcsHandler(getFedoraprojectRoot());
		assertNotNull(projectBits);
		// make sure we meet pre-condition (we should be on master)
		assertEquals("master", projectBits.getCurrentBranchName());
		// ATM this will change with the next Fedora release, so expect this to
		// fail
		assertEquals("22", projectBits.getBranchConfig().getDistVal());
		GitTestProject testProject = getProject();
		// switch to remote branch f13
		testProject.checkoutBranch("f13");
		assertEquals(projectBits.getBranchConfig().getDistVal(), "13");
	}

	@Test
	public void testNonExactNamedBranches() throws JGitInternalException,
			GitAPIException, CoreException {
		FpGitProjectBits projectBits = (FpGitProjectBits) FedoraPackagerUtils
				.getVcsHandler(getFedoraprojectRoot());
		assertNotNull(projectBits);
		GitTestProject testProject = getProject();
		// create branch name containing f16
		testProject.getGitRepo().branchCreate()
				.setName("fedora_betaf16_testbranch").call();
		// check out created branch
		testProject.checkoutBranch("fedora_betaf16_testbranch");
		assertEquals(projectBits.getBranchConfig().getEquivalentBranch(), "f16");
	}
}
