/*******************************************************************************
 * Copyright (c) 2010, 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.git;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.FedoraSSL;
import org.fedoraproject.eclipse.packager.FedoraSSLFactory;
import org.fedoraproject.eclipse.packager.PackagerPlugin;

/**
 * Utility class for Fedora Git related things.
 *
 * @since 0.5.0
 */
public class GitUtils {

	/**
	 * @param gitBaseUrl
	 *            The url of the git host.
	 * @param packageName
	 *            The name of the package that the remote repo contains.
	 * @return The full clone URL for the given package.
	 */
	public static String getFullGitURL(String gitBaseUrl, String packageName) {
		return gitBaseUrl + packageName + GitConstants.GIT_REPO_SUFFIX;
	}

	/**
	 * @return The anonymous base URL to clone from.
	 */
	public static String getAnonymousGitBaseUrl() {
		return GitConstants.ANONYMOUS_PROTOCOL + getPreferenceURL();
	}

	/**
	 * @param username
	 *            The username used for SSH authentication.
	 * @return The SSH base URL to clone from.
	 */
	public static String getAuthenticatedGitBaseUrl(String username) {
		return GitConstants.AUTHENTICATED_PROTOCOL + username
				+ GitConstants.USERNAME_SEPARATOR + getPreferenceURL();
	}

	/**
	 * Get the URL stored in the preferences. Gets the .conf clone base url if
	 * using it is enabled, otherwise it will take the clone base url from the
	 * Git preference page.
	 *
	 * @return The URL stored in the preferences depending on whether or not the
	 *         user is using settings from the .conf file.
	 * @since 0.5.0
	 */
	public static String getPreferenceURL() {
		String URL = ""; //$NON-NLS-1$
		// if .conf is enabled, use the base url from .conf
		if (PackagerPlugin.isConfEnabled()) {
			URL = PackagerPlugin.getStringPreference(FedoraPackagerPreferencesConstants.PREF_CLONE_BASE_URL);
		}
		// if not enabled, use the base url from the git preference page
		if (URL.isEmpty()) {
			URL = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).get(
					GitPreferencesConstants.PREF_CLONE_BASE_URL, DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID).get(
							GitPreferencesConstants.PREF_CLONE_BASE_URL, "")); //$NON-NLS-1$
		}
		return URL;
	}

	/**
	 * Determine the default Git base URL for cloning. Based on ~/.fedora.cert
	 *
	 * @return The default Git base URL for cloning.
	 */
	public static String getDefaultGitBaseUrl() {
		// Figure out if we have an anonymous or a FAS user
		String user = FedoraSSLFactory.getInstance().getUsernameFromCert();
		String gitURL;
		if (!user.equals(FedoraSSL.UNKNOWN_USER)) {
			gitURL = getAuthenticatedGitBaseUrl(user);
		} else {
			gitURL = getAnonymousGitBaseUrl();
		}
		return gitURL;
	}

	/**
	 * Create local branches based on existing remotes (uses the JGit API).
	 *
	 * @param git
	 *            The JGit api access for the desired repo.
	 * @param monitor
	 *            The monitor to show progress.
	 */
	public static void createLocalBranches(Git git, IProgressMonitor monitor) {
		monitor.beginTask(
				FedoraPackagerGitText.FedoraPackagerGitCloneWizard_createLocalBranchesJob,
				IProgressMonitor.UNKNOWN);
		try {
			// get a list of remote branches
			ListBranchCommand branchList = git.branchList();
			branchList.setListMode(ListMode.REMOTE); // want all remote branches
			List<Ref> remoteRefs = branchList.call();
			for (Ref remoteRef : remoteRefs) {
				String name = remoteRef.getName();
				int index = (Constants.R_REMOTES + "origin/").length(); //$NON-NLS-1$
				// Remove "refs/remotes/origin/" part in branch name
				name = name.substring(index);
				// Use "f14"-like branch naming
				if (name.endsWith("/" + Constants.MASTER)) { //$NON-NLS-1$
					index = name.indexOf("/" + Constants.MASTER); //$NON-NLS-1$
					name = name.substring(0, index);
				}
				// Create all remote branches, except "master"
				if (!name.equals(Constants.MASTER)) {
					CreateBranchCommand branchCreateCmd = git.branchCreate();
					branchCreateCmd.setName(name);
					// Need to set starting point this way in order for tracking
					// to work properly. See:
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=333899
					branchCreateCmd.setStartPoint(remoteRef.getName());
					// Add remote tracking config in order to not confuse
					// fedpkg
					branchCreateCmd.setUpstreamMode(SetupUpstreamMode.TRACK);
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					branchCreateCmd.call();
				}
			}
		} catch (JGitInternalException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the folder in which the clone operation will be performed.
	 *
	 * Use the Git directory as set by the preferences, or simply use the
	 * default workspace location.
	 *
	 * @return The folder (path absolute) in which to perform the clone.
	 */
	public static IPath getGitCloneDir() {
		String cloneDirStr = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).get(
				GitPreferencesConstants.PREF_CLONE_DIR, DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID).get(
						GitPreferencesConstants.PREF_CLONE_DIR, "")); //$NON-NLS-1$
		return new Path(cloneDirStr);
	}
}
