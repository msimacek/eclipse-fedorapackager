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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.git.FedoraPackagerGitCloneOperation;
import org.fedoraproject.eclipse.packager.git.GitUtils;

/**
 * Fixture for Git based projects.
 */
public class GitTestProject {
	protected IProject project;
	protected Git git;

	public GitTestProject(String packageName) throws InterruptedException {
		this(packageName, GitUtils.getFullGitURL(GitUtils.getAnonymousGitBaseUrl(),
				packageName));
	}

	/**
	 * Use this constructor if you are cloning a local repo
	 *
	 * @param packageName
	 *            name of the package.
	 * @param URI
	 *            where to clone from.
	 * @throws InterruptedException
	 */
	public GitTestProject(final String packageName, final String URI) throws InterruptedException {
		Job cloneProjectJob = new Job(packageName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				FedoraPackagerGitCloneOperation cloneOp = new FedoraPackagerGitCloneOperation();
				try {
					cloneOp.setCloneURI(URI)
							.setPackageName(packageName);
				} catch (URISyntaxException e1) {
					// ignore
				}
				try {
					git = cloneOp.run(monitor);
				} catch (InvocationTargetException | InterruptedException
						| IllegalStateException | IOException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		cloneProjectJob.schedule();
		// wait for it to finish
		cloneProjectJob.join();

		project = ResourcesPlugin.getWorkspace().getRoot()
		.getProject(packageName);
		try {
			project.create(null);
			project.open(null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		setPersistentProperty();
		ConnectProviderOperation connect = new ConnectProviderOperation(
				project);
		try {
			connect.execute(null);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void dispose() throws CoreException {
		project.delete(true, true, null);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	/**
	 * Get underlying IProject
	 *
	 */
	public IProject getProject() {
		return this.project;
	}

	/**
	 * @return the gitRepo
	 */
	public Git getGitRepo() {
		return this.git;
	}

	/**
	 * Checkouts branch
	 *
	 * @param refName
	 *            full name of branch
	 * @throws CoreException
	 * @throws GitAPIException
	 * @throws JGitInternalException
	 */
	public void checkoutBranch(String branchName) throws JGitInternalException, GitAPIException, CoreException {
		boolean branchExists = false;
			ListBranchCommand lsBranchCmd = this.git.branchList();
			for (Ref branch: lsBranchCmd.call()) {
				if (Repository.shortenRefName(branch.getName()).equals(branchName)) {
					branchExists = true;
					break; // short circuit
				}
			}
			if (!branchExists) {
				System.err.println("Branch: '" + branchName + "' does not exist!");
				return;
			}
		CheckoutCommand checkoutCmd = this.git.checkout();
		checkoutCmd.setName(Constants.R_HEADS + branchName);
		checkoutCmd.call();
		// refresh after checkout
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	protected void setPersistentProperty() {
		try {
			project.setPersistentProperty(PackagerPlugin.PROJECT_PROP,
			"true" /* unused value */);
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
	}
}
