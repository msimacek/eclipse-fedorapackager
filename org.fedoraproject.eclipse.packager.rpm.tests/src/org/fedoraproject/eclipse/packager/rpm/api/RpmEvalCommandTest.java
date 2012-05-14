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

import static org.junit.Assert.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.rpm.api.EvalResult;
import org.fedoraproject.eclipse.packager.rpm.api.RpmEvalCommand;
import org.fedoraproject.eclipse.packager.rpm.api.errors.RpmEvalCommandException;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the RPM eval command.
 */
public class RpmEvalCommandTest {

	// project under test
	private GitTestProject testProject;
	// main interface class
	private FedoraPackager packager;
	// Fedora packager root
	private IProjectRoot fpRoot;
	
	/**
	 * Clone a test project to be used for testing.
	 * @throws InterruptedException 
	 * @throws InvalidProjectRootException 
	 * 
	 */
	@Before
	public void setUp() throws InterruptedException, InvalidProjectRootException {
		this.testProject = new GitTestProject("eclipse-fedorapackager"); //$NON-NLS-1$
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);
	}

	/**
	 * @throws CoreException 
	 */
	@After
	public void tearDown() throws CoreException {
		this.testProject.dispose();
	}

	/**
	 * Test method for 
	 * {@link org.fedoraproject.eclipse.packager.rpm.api.RpmEvalCommand#checkConfiguration()}.
	 * Should have thrown an exception. Command is not properly configured.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws RpmEvalCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test(expected=CommandMisconfiguredException.class)
	public void testCheckConfiguration() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandMisconfiguredException, CommandListenerException, RpmEvalCommandException {
		RpmEvalCommand eval = (RpmEvalCommand) packager
				.getCommandInstance(RpmEvalCommand.ID);
		eval.call(new NullProgressMonitor());
	}

	/**
	 *  This illustrates proper usage of {@link RpmEvalCommand}.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws RpmEvalCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test
	public void canEvalArchitecture() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandMisconfiguredException, CommandListenerException, RpmEvalCommandException {
		RpmEvalCommand eval = (RpmEvalCommand) packager
				.getCommandInstance(RpmEvalCommand.ID);
		EvalResult result;
			result = eval.variable(RpmEvalCommand.ARCH).call(new NullProgressMonitor());
		assertTrue(result.wasSuccessful());
	}

}
