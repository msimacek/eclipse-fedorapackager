/*******************************************************************************
 * Copyright (c) 2010 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.tests.utils.git;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.fedoraproject.eclipse.packager.git.CloneOperation2;

/**
 * Fixture for Git based projects.
 */
public class GitTestProject {	
	private IProject project;
	private Git gitRepo;
	
	public GitTestProject(final String packageName) throws InterruptedException {
		Job cloneProjectJob = new Job(packageName) {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				URIish uri = null;
				try {
					uri = new URIish(getGitURL(packageName));
				} catch (URISyntaxException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				final CloneOperation2 clone = new CloneOperation2(uri, true,
						new ArrayList<Ref>(), new File(ResourcesPlugin
								.getWorkspace().getRoot().getLocation().toFile(),
								packageName), Constants.R_HEADS + Constants.MASTER,
								"origin", 0);
				try {
					clone.run(null);
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // clone project
				// Add cloned repository to the list of Git repositories so that it
				// shows up in the Git repositories view.
				final RepositoryUtil config = org.eclipse.egit.core.Activator.getDefault().getRepositoryUtil();
				config.addConfiguredRepository(clone.getGitDir());
				project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(packageName);
				try {
					project.create(null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					project.open(null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ConnectProviderOperation connect = new ConnectProviderOperation(
						project);
				try {
					connect.execute(null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// find repo we've just created and set gitRepo
				RepositoryCache repoCache = org.eclipse.egit.core.Activator
				.getDefault().getRepositoryCache();
				try {
					gitRepo = new Git(repoCache.lookupRepository(new File(project
							.getProject().getLocation().toOSString()
							+ "/.git")));
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				// Do the local branches limbo
				try {
					createLocalBranches(new NullProgressMonitor());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		};
		cloneProjectJob.schedule();
		// wait for it to finish
		cloneProjectJob.join();
	}
	
	public void dispose() throws Exception {
		project.delete(true, true, null);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}
	
	/**
	 * Get underlying IProject
	 * 
	 * @return
	 */
	public IProject getProject() {
		return this.project;
	}
	
	/**
	 * @return the gitRepo
	 */
	public Git getGitRepo() {
		return this.gitRepo;
	}
	
	/**
	 * Checkouts branch
	 *
	 * @param refName
	 *            full name of branch
	 * @throws CoreException
	 * @throws InvalidRefNameException 
	 * @throws RefNotFoundException 
	 * @throws RefAlreadyExistsException 
	 * @throws JGitInternalException 
	 */
	public void checkoutBranch(String branchName) throws JGitInternalException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CoreException {
		boolean branchExists = false;
			ListBranchCommand lsBranchCmd = this.gitRepo.branchList();
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
		CheckoutCommand checkoutCmd = this.gitRepo.checkout();
		checkoutCmd.setName(Constants.R_HEADS + branchName);
		checkoutCmd.call();
		// refresh after checkout
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	/**
	 * Get Git URL for given packageName. 
	 * 
	 * @param packageName
	 * @return
	 */
	private String getGitURL(String packageName) {
		return "git://pkgs.fedoraproject.org/" + packageName + ".git";
	}
	
	/**
	 * Create local branches based on remotes. Don't do checkouts.
	 * 
	 * @param monitor
	 * @throws CoreException
	 */
	private void createLocalBranches(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Creating local branches",
				IProgressMonitor.UNKNOWN);

		try {
			// get a list of remote branches
			ListBranchCommand branchList = this.gitRepo.branchList();
			branchList.setListMode(ListMode.REMOTE); // want all remote branches
			List<Ref> remoteRefs = branchList.call();
			for (Ref remoteRef: remoteRefs) {
				String name = remoteRef.getName();
				int index = (Constants.R_REMOTES + "origin/").length(); //$NON-NLS-1$
				// Remove "refs/remotes/origin/" part in branch name
				name = name.substring(index);
				// Use "f14"-like branch naming, yet have the f14 branch
				// use the "f14/master"-like branch naming scheme.
				// We want to make sure both work and don't produce NPEs later on.
				if (name.endsWith("/" + Constants.MASTER) && !name.startsWith("f14")) { //$NON-NLS-1$
					index = name.indexOf("/" + Constants.MASTER); //$NON-NLS-1$
					name = name.substring(0, index);
				}
				// Create all remote branches, except "master"
				if (!name.equals(Constants.MASTER)) {
					CreateBranchCommand branchCreateCmd = this.gitRepo.branchCreate();
					branchCreateCmd.setName(name);
					// Need to set starting point this way in order for tracking
					// to work properly. See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=333899
					branchCreateCmd.setStartPoint(remoteRef.getName());
					// Add remote tracking config in order to not confuse
					// fedpkg
					branchCreateCmd.setUpstreamMode(SetupUpstreamMode.TRACK);
					branchCreateCmd.call();
				}
			}
		} catch (JGitInternalException e) {
			e.printStackTrace();
		} catch (RefAlreadyExistsException e) {
			e.printStackTrace();
		} catch (RefNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidRefNameException e) {
			e.printStackTrace();
		}
		
		monitor.done();
	}
}
