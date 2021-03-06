/*******************************************************************************
 * Copyright (c) 2010-2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.op.IgnoreOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.FedoraSSL;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;

/**
 * Git specific project bits (branches management and such). Implementation of
 * org.fedoraproject.eclipse.packager.vcsContribution extension point.
 *
 * @author Red Hat Inc.
 *
 */
public class FpGitProjectBits implements IFpProjectBits {

	/** The project root as passed in to {@link FpGitProjectBits#initialize(IProjectRoot)} */
	protected IProjectRoot projectRoot;
	private IResource project; // The underlying project
	private HashMap<String, String> branches; // All branches
	private Git git; // The Git repository abstraction for this project
	private boolean initialized = false; // keep track if instance is
											// initialized
	private String currentBranch = null;
	// String regexp pattern used for branch mapping this should basically be
	// the
	// same pattern as fedpkg uses. ATM this pattern is:
	// BRANCHFILTER = 'f\d\d\/master|master|el\d\/master|olpc\d\/master'
	// Severin, 2011-01-11: Make '/master' postfix of branch name optional.
	private final Pattern BRANCH_PATTERN = Pattern
			.compile(".*(fc?)(\\d\\d?).*|" + //$NON-NLS-1$
					".*(master).*|.*(rhel)-(\\d(?:\\.\\d)?).*|.*(el)(\\d).*|" + //$NON-NLS-1$
					".*(olpc)(\\d).*" //$NON-NLS-1$
			);


	/**
	 * See {@link IFpProjectBits#getBranchName(String)}
	 */
	@Override
	public String getBranchName(String branchName) {
		if (!isInitialized()) {
			return null;
		}
		return this.branches.get(branchName);
	}

	/**
	 * Parse current branch from active local branch.
	 *
	 * See {@link IFpProjectBits#getCurrentBranchName()}
	 */
	@Override
	public String getCurrentBranchName() {
		if (!isInitialized()) {
			return null;
		}
		currentBranch = null;
		try {
			// make sure it's a named branch
			if (!isNamedBranch(this.git.getRepository().getFullBranch())) {
				return null; // unknown branch!
			}
			// get the current head target
			currentBranch = this.git.getRepository().getBranch();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mapBranchName(currentBranch);
	}

	@Override
	public String getRawCurrentBranchName() {
		getCurrentBranchName();
		return currentBranch;
	}

	/**
	 * See {@link IFpProjectBits#getScmUrl()}
	 */
	@Override
	public String getScmUrl() {
		if (!isInitialized()) {
			return null;
		}
		String username = new FedoraSSL().getUsernameFromCert();
		String packageName = this.project.getProject().getName();
		if (username.equals("anonymous")) { //$NON-NLS-1$
			return "git://pkgs.fedoraproject.org/" + packageName + ".git"; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			return "ssh://" + username + "@pkgs.fedoraproject.org/" //$NON-NLS-1$ //$NON-NLS-2$
					+ packageName + ".git"; //$NON-NLS-1$
		}
	}

	/**
	 * Git should always return anonymous checkout with git protocol for koji.
	 *
	 * @see org.fedoraproject.eclipse.packager.IFpProjectBits#getScmUrlForKoji(BranchConfigInstance)
	 */
	@Override
	public String getScmUrlForKoji(BranchConfigInstance bci) {
		if (!isInitialized()) {
			return null;
		}
		String packageName = this.project.getProject().getName();
		return "git://pkgs.fedoraproject.org/" + packageName + "?#" + getCommitHash(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Get the SHA1 representing the current branch.
	 *
	 * @return The SHA1 as hex in String form.
	 */
	protected String getCommitHash() {
		String commitHash = null;
		try {
			String currentBranchRefString = git.getRepository().getFullBranch();
			Ref ref = git.getRepository().getRef(currentBranchRefString);
			commitHash = ref.getObjectId().getName();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		return commitHash;
	}

	/**
	 * Parse available branch names from Git remote branches.
	 *
	 * @return The branch map.
	 */
	private HashMap<String, String> getBranches() {
		HashMap<String, String> branches = new HashMap<>();
		try {
			Map<String, Ref> remotes = git.getRepository().getRefDatabase()
					.getRefs(Constants.R_REMOTES);
			Set<String> keyset = remotes.keySet();
			String branch, mappedBranch;
			for (String key : keyset) {
				// use shortenRefName() to get rid of refs/*/ prefix
				branch = Repository.shortenRefName(remotes.get(key).getName());
				mappedBranch = mapBranchName(branch); // do the branch name
														// mapping
				if (mappedBranch != null) {
					branches.put(mappedBranch, mappedBranch);
				} else {
					branches.put(branch, branch);
				}
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		return branches;
	}

	/**
	 * Do instance specific initialization.
	 *
	 * See {@link IFpProjectBits#initialize(IProjectRoot)}
	 */
	@Override
	public void initialize(IProjectRoot fedoraprojectRoot) {
		this.projectRoot = fedoraprojectRoot;
		this.project = fedoraprojectRoot.getProject();
		// now set Git Repository object
		this.git = new Git(getGitRepository());
		this.branches = getBranches();
		this.initialized = true;
	}

	/**
	 * Determine if instance has been properly initialized
	 */
	private boolean isInitialized() {
		return this.initialized;
	}

	/**
	 * Maps branch names to the internal format used by all IFpProjectBits
	 * implementations. For example <code>mapBranchName("f8")</code> would
	 * return <code>"F-8"</code> and <code>mapBranchName("master")</code> would
	 * return <code>"devel"</code>.
	 *
	 * @param from
	 *            The original raw branch name with "refs/something" prefixes
	 *            omitted.
	 * @return The mapped branch name.
	 */
	private String mapBranchName(String from) {
		String prefix, version;
		Matcher branchMatcher = BRANCH_PATTERN.matcher(from);
		// loop throws exception if no matches
		if (!branchMatcher.matches()) {
			return null;
		}
		for (int i = 1; i < branchMatcher.groupCount(); i++) {
			prefix = branchMatcher.group(i);
			version = branchMatcher.group(i + 1);
			if (version == null && prefix != null
					&& prefix.equals(Constants.MASTER)) {
				// matched master
				return "master"; //$NON-NLS-1$
			} else if (version != null && prefix != null) {
				// F, EPEL, OLPC matches
				return prefix + version;
			}
		}
		// not caught and no exception, something's fishy
		return null;
	}

	/**
	 * Returns true if given branch name is NOT an ObjectId in string format.
	 * I.e. if branchName has been created by doing repo.getBranch(), it would
	 * return SHA1 Strings for remote branches. We don't want that.
	 *
	 * @param branchName The branch name being examined.
	 * @return true if given branch name is NOT an ObjectId in string format, false otherwise.
	 */
	private static boolean isNamedBranch(String branchName) {
		if (branchName.startsWith(Constants.R_HEADS)
				|| branchName.startsWith(Constants.R_TAGS)
				|| branchName.startsWith(Constants.R_REMOTES)) {
			return true;
		}
		return false;
	}

	/**
	 * See {@link IFpProjectBits#updateVCS(IProgressMonitor)}
	 */
	@Override
	public IStatus updateVCS(IProgressMonitor monitor) {
		// FIXME: Not working just, yet. Use projectRoot and monitor!.
		// return performPull();
		// Return OK status to not see NPEs
		return Status.OK_STATUS;
	}

	/**
	 * Get the JGit repository.
	 */
	private Repository getGitRepository() {
		RepositoryMapping repoMapping = RepositoryMapping.getMapping(project);
		return repoMapping.getRepository();
	}

	/**
	 * Determine what the next release number (in terms of the distribution)
	 * will be.
	 *
	 * @return The next release number in String representation
	 */
	private String determineNextReleaseNumber() {
		if (!isInitialized()) {
			return null;
		}
		// Try to guess the next release number based on existing branches
		Set<String> keySet = this.branches.keySet();
		String branchName;
		int maxRelease = -1;
		for (String key : keySet) {
			branchName = this.branches.get(key);
			if (branchName.startsWith("fc")) { //$NON-NLS-1$
				// fedora
				maxRelease = Math.max(maxRelease,
						Integer.parseInt(branchName.substring("fc".length()))); //$NON-NLS-1$
			} else if (branchName.startsWith("f")) { //$NON-NLS-1$
				// fedora
				maxRelease = Math.max(maxRelease,
						Integer.parseInt(branchName.substring("f".length()))); //$NON-NLS-1$
			} else if (branchName.startsWith("el")) { //$NON-NLS-1$
				// EPEL
				maxRelease = Math.max(maxRelease,
						Integer.parseInt(branchName.substring("el".length()))); //$NON-NLS-1$
			} else if (branchName.startsWith("olpc")) { //$NON-NLS-1$
				// OLPC
				maxRelease = Math.max(maxRelease, Integer.parseInt(branchName
						.substring("olpc".length()))); //$NON-NLS-1$
			}
			// ignore
		}
		if (maxRelease == -1) {
			// most likely a new package. ATM this is F-18
			return "21"; //$NON-NLS-1$
		} else {
			return Integer.toString(maxRelease + 1);
		}
	}

	@Override
	public IStatus ignoreResource(IResource resourceToIgnore) {
		try {
			new IgnoreOperation(Arrays.asList(resourceToIgnore.getLocation())).execute(new NullProgressMonitor());
		} catch (CoreException e) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	@Override
	public String getIgnoreFileName(){
		return Constants.GITIGNORE_FILENAME;
	}

	/**
	 * Fedora git doesn't need to tag because commit hashes are used.
	 *
	 * @see org.fedoraproject.eclipse.packager.IFpProjectBits#needsTag()
	 */
	@Override
	public boolean needsTag() {
		return false;
	}

	/**
	 * Determine if there are unpushed changes on the current branch.
	 *
	 * @return If there are unpushed changes.
	 */
	@Override
	public boolean hasLocalChanges() {
		if (!isInitialized()) {
			// FIXME: raise exception instead.
			return true; // If we are not initialized we can't go any further!
		}
		try {
			// get remote ref from config
			String branchName = git.getRepository().getBranch();
			String trackingRemoteBranch = git
					.getRepository()
					.getConfig()
					.getString(ConfigConstants.CONFIG_BRANCH_SECTION,
							branchName, ConfigConstants.CONFIG_KEY_MERGE);
			// Make sure that remotes/origin/<branchname> is up to date with what's in the
			// repo remotely, so that we have the correct revision to compare.
			FetchCommand fetch = git.fetch();
			fetch.setRemote("origin"); //$NON-NLS-1$
			fetch.setTimeout(0);
			// Fetch refs for current branch; account for f14 + f14/master like
			// branch names.
			String fetchBranchSpec = Constants.R_HEADS + branchName + ":" + //$NON-NLS-1$
			                Constants.R_REMOTES + "origin/" + branchName; //$NON-NLS-1$
			if (trackingRemoteBranch != null) {
			        // have f14/master like branch
			        trackingRemoteBranch = trackingRemoteBranch
			                        .substring(Constants.R_HEADS.length());
			        fetchBranchSpec = Constants.R_HEADS + trackingRemoteBranch
			                        + ":" + //$NON-NLS-1$
			                        Constants.R_REMOTES + "origin/" + trackingRemoteBranch; //$NON-NLS-1$
			}
			RefSpec spec = new RefSpec(fetchBranchSpec);
			fetch.setRefSpecs(spec);
			try {
			        fetch.call();
			} catch (JGitInternalException e) {
			        e.printStackTrace();
			}
			RevWalk rw = new RevWalk(git.getRepository());
			ObjectId objHead = git.getRepository().resolve(branchName);
			if (trackingRemoteBranch == null) {
				// no config yet, assume plain brach name.
				trackingRemoteBranch = branchName;
			} else {
				// EGit backwards compatibility. Prior 1.2 tracking branch was refs/heads/master
				// and is "master" for >= 1.2
				if (trackingRemoteBranch.startsWith(Constants.R_HEADS)) {
					trackingRemoteBranch = trackingRemoteBranch.substring(Constants.R_HEADS.length());
				}
			}
			RevCommit commitHead = rw.parseCommit(objHead);
			ObjectId objRemoteTrackingHead = git.getRepository().resolve(
					"origin/" + //$NON-NLS-1$
							trackingRemoteBranch);
			RevCommit remoteCommitHead = rw.parseCommit(objRemoteTrackingHead);
			return !commitHead.equals(remoteCommitHead);
		} catch (GitAPIException|IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void stageChanges(Set<String> files) {
		try {
			for (String filePattern : files) {
				git.add().addFilepattern(filePattern).call();
			}
		} catch (GitAPIException e) {
			// ignore, allow adds with no files
		}
	}

	@Override
	public BranchConfigInstance getBranchConfig() {
		String branchName = getCurrentBranchName();
		if (branchName == null) {
			HashMap<String, String> branchMap = new HashMap<>();
			BufferedReader br;
			try {
				br = new BufferedReader(new InputStreamReader(FileLocator.find(Platform
						.getBundle(projectRoot.getPluginID()),
						new Path("resources/branchinfo.txt"), null).openStream())); //$NON-NLS-1$
				while (br.ready()) {
					String line = br.readLine();
					branchMap.put(line.split(",")[1], line.split(",")[0]); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (IOException e) {
				// should not occur
				e.printStackTrace();
			}
			final String[] entries = branchMap.keySet().toArray(new String[0]);
			Arrays.sort(entries);
			FutureTask<String> promptTask = new FutureTask<>(
					new Callable<String>() {
						@Override
						public String call() {
							Shell shell = new Shell(Display.getDefault());
							ListDialog ld = new ListDialog(shell);
							ld.setInput(entries);
							ld.setContentProvider(new ArrayContentProvider());
							ld.setLabelProvider(new LabelProvider());
							ld.setMessage(FedoraPackagerGitText.FpGitProjectBits_OSDialogTitle);
							ld.open();
							return ld.getResult()[0].toString();
						}
					});
			Display.getDefault().syncExec(promptTask);
			try {
				branchName = branchMap.get(promptTask.get());
			} catch (InterruptedException e) {
				return null;
			} catch (ExecutionException e) {
				// ExecutionException may be thrown if user clicked "Cancel"
				e.printStackTrace();
				return null;
			}
		}
		String version;
		if (branchName.equals("master")) { //$NON-NLS-1$
			version = determineNextReleaseNumber();
		} else {
			version = branchName.replaceAll("[a-zA-Z]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String distro = null;
		String distroSuffix = null;
		String buildTarget = null;
		if (branchName.startsWith("f")||branchName.startsWith("fc")) { //$NON-NLS-1$ //$NON-NLS-2$
			distro = "fedora"; //$NON-NLS-1$"
			distroSuffix = ".fc" + version; //$NON-NLS-1$
			buildTarget = "f" + version + "-candidate"; //$NON-NLS-1$" //$NON-NLS-2$
		} else if (branchName.startsWith("olpc")) { //$NON-NLS-1$
			distro = "olpc"; //$NON-NLS-1$
			distroSuffix = ".olpc" + version; //$NON-NLS-1$
			buildTarget = "dist-olpc" + version; //$NON-NLS-1$
		} else if (branchName.equals("master")) { //$NON-NLS-1$
			distro = "fedora"; //$NON-NLS-1$
			distroSuffix = ".fc" + version; //$NON-NLS-1$
			buildTarget = "rawhide"; //$NON-NLS-1$
		} else if (branchName.startsWith("el")) { //$NON-NLS-1$
			distro = "rhel"; //$NON-NLS-1$
			distroSuffix = ".el" + version; //$NON-NLS-1$
			buildTarget = "dist-" + version + "E-epel-testing-candidate"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new BranchConfigInstance(distroSuffix, version, distro,
				buildTarget, branchName);
	}

}
