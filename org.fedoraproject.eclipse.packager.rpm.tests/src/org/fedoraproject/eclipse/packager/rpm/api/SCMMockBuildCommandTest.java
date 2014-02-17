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
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.fedoraproject.eclipse.packager.api.DownloadSourceCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.DownloadFailedException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.SourcesUpToDateException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.MockNotInstalledException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.RpmBuildCommandException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.UserNotInMockGroupException;
import org.junit.Before;
import org.junit.Test;

public class SCMMockBuildCommandTest extends FedoraPackagerTest{
	// download source command
	private DownloadSourceCommand download;

	@Override
	@Before
	public void setUp() throws InterruptedException, JGitInternalException, GitAPIException, CoreException, InvalidProjectRootException, FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandMisconfiguredException, CommandListenerException, SourcesUpToDateException, DownloadFailedException, RpmBuildCommandException, IOException  {
		super.setUp();
		// need to have sources ready
		download = (DownloadSourceCommand) packager
				.getCommandInstance(DownloadSourceCommand.ID);
	}


	@Test
	public void canCreateF15SCMMockBuild() throws CoreException,
			FedoraPackagerCommandInitializationException,
			FedoraPackagerCommandNotFoundException,
			CommandMisconfiguredException, UserNotInMockGroupException,
			CommandListenerException, MockBuildCommandException,
			MockNotInstalledException {
		SCMMockBuildCommand mockBuild = (SCMMockBuildCommand) packager
				.getCommandInstance(SCMMockBuildCommand.ID);
		MockBuildResult result = mockBuild
				.useDownloadedSourceDirectory(download.getDownloadFolderPath())
				.useBranch("f19") //$NON-NLS-1$
				.usePackage("example") //$NON-NLS-1$
				.useRepoPath(
						fpRoot.getContainer().getParent().getRawLocation()
								.toString())
				.useSpec(fpRoot.getSpecFile().getName()).branchConfig(bci)
				.call(new NullProgressMonitor());
		assertTrue(result.isSuccessful());
		String resultDirectoryPath = result.getResultDirectoryPath().getFullPath().toOSString();
		assertNotNull(resultDirectoryPath);
		// should have created RPMs in the result directory
		boolean rpmfound = false;
		boolean srpmfound = false;
		File resultPath = new File(resultDirectoryPath);
		this.testProject.getProject().refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		IContainer container = (IContainer) this.testProject.getProject()
				.findMember(new Path(resultPath.getName()));
		for (IResource file : container.members()) {
			if (file.getName().endsWith(".rpm")) { //$NON-NLS-1$
				// not interested in source RPMs
				if (!file.getName().endsWith(".src.rpm")) { //$NON-NLS-1$
					rpmfound = true;
				} else {
					srpmfound = true;
				}
			}
		}
		assertTrue(rpmfound);
		assertTrue(srpmfound);
	}
}
