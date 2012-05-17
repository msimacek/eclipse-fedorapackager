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
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.DownloadSourceCommand;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.DownloadFailedException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.SourcesUpToDateException;
import org.fedoraproject.eclipse.packager.api.errors.TagSourcesException;
import org.fedoraproject.eclipse.packager.api.errors.UnpushedChangesException;
import org.fedoraproject.eclipse.packager.koji.api.IKojiHubClient;
import org.fedoraproject.eclipse.packager.koji.api.KojiBuildCommand;
import org.fedoraproject.eclipse.packager.koji.api.KojiUploadSRPMCommand;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientLoginException;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildResult;
import org.fedoraproject.eclipse.packager.rpm.api.SRPMBuildJob;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.fedoraproject.eclipse.packager.utils.RPMUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KojiScratchWithSRPMTest {

	// project under test
	private GitTestProject testProject;
	// Fedora packager root
	private IProjectRoot fpRoot;
	// main interface class
	private FedoraPackager packager;
	// srpm build command command
	private RpmBuildCommand srpmBuild;
	// result of building srpm
	private RpmBuildResult srpmBuildResult;
	private BranchConfigInstance bci;

	@Before
	public void setUp() throws InterruptedException, JGitInternalException,
			RefAlreadyExistsException, RefNotFoundException,
			InvalidRefNameException, CoreException,
			InvalidProjectRootException,
			FedoraPackagerCommandInitializationException,
			FedoraPackagerCommandNotFoundException, MalformedURLException,
			SourcesUpToDateException, DownloadFailedException, CommandMisconfiguredException, CommandListenerException  {
		this.testProject = new GitTestProject("ed");
		testProject.checkoutBranch("f15");
		this.fpRoot = FedoraPackagerUtils.getProjectRoot(this.testProject
				.getProject());
		this.packager = new FedoraPackager(fpRoot);
		srpmBuild = (RpmBuildCommand) packager
				.getCommandInstance(RpmBuildCommand.ID);
		DownloadSourceCommand download = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
		download.setDownloadURL("http://pkgs.fedoraproject.org/repo/pkgs");
		download.call(new NullProgressMonitor());
		bci = FedoraPackagerUtils.getVcsHandler(fpRoot).getBranchConfig();
		SRPMBuildJob srpmBuildJob = new SRPMBuildJob(NLS.bind(
				RpmText.MockBuildHandler_creatingSRPMForMockBuild,
				fpRoot.getPackageName()), srpmBuild, fpRoot);
		srpmBuildJob.setUser(false);
		srpmBuildJob.schedule();
		srpmBuildJob.join();
		srpmBuildResult = srpmBuildJob.getSRPMBuildResult();
	}

	@After
	public void tearDown() throws CoreException {
		this.testProject.dispose();
	}

	/**
	 * In order for this test to work, koji test certificates need to be at
	 * location: ~/.eclipse-fedorapackager/testing/koji-certs
	 * 
	 * Also, it is required to set Java System property
	 * "org.fedoraproject.eclipse.packager.tests.koji.testInstanceURL" to point
	 * to the koji test instance.
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws KojiHubClientLoginException 
	 * @throws KojiHubClientException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 * @throws IOException 
	 * @throws TagSourcesException 
	 * @throws UnpushedChangesException 
	 * 
	 */
	@Test
	public void canUploadSRPMAndRequestBuild() throws FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, KojiHubClientLoginException, KojiHubClientException, CommandMisconfiguredException, CommandListenerException, UnpushedChangesException, TagSourcesException  {
		KojiUploadSRPMCommand uploadSRPMCommand = (KojiUploadSRPMCommand) packager
				.getCommandInstance(KojiUploadSRPMCommand.ID);
		final String uploadPath = "cli-build/"
				+ FedoraPackagerUtils.getUniqueIdentifier(); //$NON-NLS-1$
		IKojiHubClient kojiClient = createMock(IKojiHubClient.class);
		expect(kojiClient.login()).andReturn(null);
		expect(
				kojiClient.uploadFile((String) anyObject(),
						eq("ed-1.5-2.fc15.src.rpm"), and(gt(0), leq(1000000)),
						(String) anyObject(), anyInt(), (String) anyObject()))
				.andReturn(true).atLeastOnce();
		expect(
				kojiClient.build(eq("f15-candidate"), (List<?>) anyObject(),
						aryEq(new String[] { "ed-1.5-2.fc15" }), eq(true)))
				.andReturn(new int[] { 0xdead });
		kojiClient.logout();
		replay(kojiClient);

		assertTrue(uploadSRPMCommand.setKojiClient(kojiClient)
				.setRemotePath(uploadPath)
				.setSRPM(srpmBuildResult.getAbsoluteSRPMFilePath())
				.call(new NullProgressMonitor()).isSuccessful());
		KojiBuildCommand kojiBuildCmd = (KojiBuildCommand) packager
				.getCommandInstance(KojiBuildCommand.ID);
		kojiBuildCmd.setKojiClient(kojiClient);
		List<String> sourceLocation = new ArrayList<String>();
		sourceLocation
				.add(uploadPath
						+ "/"
						+ new File(srpmBuildResult.getAbsoluteSRPMFilePath())
								.getName());
		kojiBuildCmd.sourceLocation(sourceLocation); //$NON-NLS-1$
		String nvr = RPMUtils.getNVR(fpRoot, bci);
		kojiBuildCmd.buildTarget(bci.getBuildTarget())
				.nvr(new String[] { nvr }).isScratchBuild(true);
		assertTrue(kojiBuildCmd.call(new NullProgressMonitor()).isSuccessful());
		verify(kojiClient);
	}
}
