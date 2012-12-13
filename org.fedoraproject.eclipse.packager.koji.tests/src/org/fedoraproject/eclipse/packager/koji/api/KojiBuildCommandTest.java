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
/**
 * 
 */
package org.fedoraproject.eclipse.packager.koji.api;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.gt;
import static org.easymock.EasyMock.leq;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.TagSourcesException;
import org.fedoraproject.eclipse.packager.api.errors.UnpushedChangesException;
import org.fedoraproject.eclipse.packager.koji.api.errors.BuildAlreadyExistsException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for Koji build command.
 *
 */
public class KojiBuildCommandTest {

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
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws InterruptedException, InvalidProjectRootException {
		this.testProject = new GitTestProject("eclipse-fedorapackager");
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);		
	}

	/**
	 * @throws CoreException 
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws CoreException  {
		this.testProject.dispose();
	}

	/**
	 * Test method for 
	 * {@link org.fedoraproject.eclipse.packager.koji.api.KojiBuildCommand#checkConfiguration()}.
	 * Should have thrown an exception. Command is not properly configured.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws KojiHubClientException 
	 * @throws CommandListenerException 
	 * @throws TagSourcesException 
	 * @throws UnpushedChangesException 
	 * @throws CommandMisconfiguredException 
	 * @throws BuildAlreadyExistsException 
	 */
	@Test(expected=CommandMisconfiguredException.class)
	public void testCheckConfiguration() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, BuildAlreadyExistsException, CommandMisconfiguredException, UnpushedChangesException, TagSourcesException, CommandListenerException, KojiHubClientException  {
		KojiBuildCommand buildCommand = (KojiBuildCommand) packager
				.getCommandInstance(KojiBuildCommand.ID);
		buildCommand.call(new NullProgressMonitor());
	}

	/**
	 *  This illustrates proper usage of {@link KojiBuildCommand}. Since
	 *  it's using a stubbed client it does not actually push a build to koji.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws KojiHubClientLoginException 
	 * @throws KojiHubClientException 
	 * @throws CommandListenerException 
	 * @throws TagSourcesException 
	 * @throws UnpushedChangesException 
	 * @throws CommandMisconfiguredException 
	 */
	@Test
	public void canPushFakeScratchBuild() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, KojiHubClientLoginException, KojiHubClientException, CommandMisconfiguredException, UnpushedChangesException, TagSourcesException, CommandListenerException {
		KojiBuildCommand buildCommand = (KojiBuildCommand) packager
				.getCommandInstance(KojiBuildCommand.ID);
		IKojiHubClient kojiClient = createMock(IKojiHubClient.class);
		expect(kojiClient.login()).andReturn(null);
		expect(
				kojiClient.uploadFile((String) anyObject(),
						eq("eclipse-fedorapackager-0.1.12-1.fc15.src.rpm"),
						and(gt(0), leq(1000000)), (String) anyObject(),
						anyInt(), (String) anyObject())).andReturn(true)
				.atLeastOnce();
		expect(
				kojiClient.build(eq("dist-rawhide"),
						(List<?>) anyObject(),
						aryEq(new String[] { "eclipse-fedorapackager-0.1.12-1.fc15" }), eq(true)))
				.andReturn(new int[] { 0xdead });
		kojiClient.logout();
		replay(kojiClient);
		buildCommand.setKojiClient(kojiClient);
		buildCommand.buildTarget("dist-rawhide").nvr(new String[] {"eclipse-fedorapackager-0.1.12-1.fc15"});
		List<String> sourceLocation = new ArrayList<>();
		sourceLocation.add("git://pkgs.stg.fedoraproject.org/eclipse-fedorapackager.git?#7526fb6c2c150dcc3480a9838540426a501d0553");
		buildCommand.sourceLocation(sourceLocation);
		buildCommand.isScratchBuild(true).call(new NullProgressMonitor());
	}

}
