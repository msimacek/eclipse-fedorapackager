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
package org.fedoraproject.eclipse.packager.git.internal.ui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.linuxtools.rpm.core.RPMProjectNature;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.fedoraproject.eclipse.packager.FedoraSSL;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.QuestionMessageDialog;
import org.fedoraproject.eclipse.packager.git.Activator;
import org.fedoraproject.eclipse.packager.git.FedoraPackagerGitCloneOperation;
import org.fedoraproject.eclipse.packager.git.FedoraPackagerGitText;
import org.fedoraproject.eclipse.packager.git.GitPreferencesConstants;
import org.fedoraproject.eclipse.packager.git.GitUtils;
import org.fedoraproject.eclipse.packager.utils.UiUtils;

/**
 * Wizard to checkout package content from Fedora Git.
 *
 */
public class FedoraPackagerGitCloneWizard extends Wizard implements
		IImportWizard {

	private SelectModulePage page;
	private IWorkbench workbench;
	private IStructuredSelection selection;
	private String fasUserName;

	/**
	 * Simple visitor to delete a directory and it's contents.
	 */
	private static class DeleteDirectoryVisitor extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			Files.delete(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException e)
				throws IOException {
			if (e == null) {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			} else {
				throw e;
			}
		}
	}

	/**
	 * Creates the wizards and sets that it needs progress monitor.
	 */
	public FedoraPackagerGitCloneWizard() {
		super();
		// Set title of wizard window
		setWindowTitle(FedoraPackagerGitText.FedoraPackagerGitCloneWizard_wizardTitle);
		// required to show progress info of clone job
		setNeedsProgressMonitor(true);
		// retrieve FAS username
		this.fasUserName = new FedoraSSL().getUsernameFromCert();
	}

	@Override
	public void addPages() {
		// get Fedora username from cert
		page = new SelectModulePage(fasUserName, selection);
		addPage(page);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	@Override
	public boolean performFinish() {
		try {
			// Bail out if all projects already exist and user did not want to
			// overwrite them
			List<String> packages = performOverwriteChecks();
			if (packages.isEmpty()) {
				return performCancel();
			}

			for (String pkg : packages) {
				// Prepare the clone operation
				final FedoraPackagerGitCloneOperation cloneOp = new FedoraPackagerGitCloneOperation();
				cloneOp.setCloneURI(getGitCloneURL(pkg))
						.setCloneDir(GitUtils.getGitCloneDir())
						.setPackageName(pkg);

				// Make sure we report a nice error if repo not found
				try {
					// Perform clone in ModalContext thread with progress
					// reporting on the wizard.
					getContainer().run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								cloneOp.run(monitor);
							} catch (IOException | IllegalStateException e) {
								throw new InvocationTargetException(e);
							}
							if (monitor.isCanceled())
								throw new InterruptedException();
						}
					});
				} catch (InvocationTargetException e) {
					// if repo wasn't found make this apparent
					if (e.getTargetException().getCause() instanceof NoRemoteRepositoryException
							|| e.getTargetException().getCause() instanceof InvalidRemoteException) {
						// Refuse to clone, give user a chance to correct
						final String errorMessage = NLS
								.bind(FedoraPackagerGitText.FedoraPackagerGitCloneWizard_repositoryNotFound,
										pkg);
						cloneFailChecked(errorMessage);
						return false; // let user correct
					} else if (e.getTargetException().getCause().getCause() != null
							&& e.getTargetException().getCause().getCause()
									.getMessage() == "Auth fail") { //$NON-NLS-1$
						cloneFailChecked(FedoraPackagerGitText.FedoraPackagerGitCloneWizard_authFail);
						return false;
						// Caused by: org.eclipse.jgit.errors.NotSupportedException:
						// URI not supported:
						// ssh:///jeraal@alkldal.test.comeclipse-callgraph.git
					} else if (e.getTargetException().getCause() instanceof NotSupportedException
							|| e.getTargetException().getCause() instanceof TransportException) {
						final String errorMessage = NLS
								.bind(FedoraPackagerGitText.FedoraPackagerGitCloneWizard_badURIError,
										GitUtils.getDefaultGitBaseUrl());
						cloneFailChecked(errorMessage);
						return false; // let user correct
					}
					throw e;
				}
	
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				IProject newProject = root.getProject(pkg);
	
				IPath gitCloneDir = GitUtils.getGitCloneDir();
				if (!gitCloneDir.toOSString().equals(
						DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID).get(
								GitPreferencesConstants.PREF_CLONE_DIR, ""))) { //$NON-NLS-1$
					IProjectDescription description = workspace
							.newProjectDescription(pkg);
					description.setLocation(gitCloneDir.append(pkg));
					newProject.create(description, null);
				} else {
					newProject.create(null);
				}
	
				newProject.open(null);
				RPMProjectNature
						.addRPMNature(newProject, new NullProgressMonitor());
				// Set persistent property so that we know when to show the context
				// menu item.
				newProject.setPersistentProperty(PackagerPlugin.PROJECT_PROP,
						"true" /* unused value */); //$NON-NLS-1$
				ConnectProviderOperation connect = new ConnectProviderOperation(
						newProject);
				connect.execute(null);
				configureRpmlintBuilder(newProject);
	
				// Add new project to working sets, if requested
				IWorkingSet[] workingSets = page.getSelectedWorkingSets();
				workbench.getWorkingSetManager().addToWorkingSets(newProject,
						workingSets);
			}

			// Finally ask if the Fedora Packaging perspective should be opened
			// if not already open.
			UiUtils.openPerspective(getShell(),
					UiUtils.afterProjectClonePerspectiveSwitch);
			return true;
		} catch (InterruptedException e) {
			MessageDialog
					.openInformation(
							getShell(),
							FedoraPackagerGitText.FedoraPackagerGitCloneWizard_cloneFail,
							FedoraPackagerGitText.FedoraPackagerGitCloneWizard_cloneCancel);
			return false;
		} catch (CoreException | InvocationTargetException | URISyntaxException
				| IOException e) {
			Activator
					.handleError(
							FedoraPackagerGitText.FedoraPackagerGitCloneWizard_cloneFail,
							e, true);
			return false;
		}
	}

	/**
	 * Checks the user-entered list of projects before a clone is attempted so
	 * that we don't unexpectedly overwrite any other resources the user might
	 * care about.
	 * 
	 * @return the list of projects that we can clone after the user has made
	 *         decisions about overwriting existing resources
	 * @throws CoreException
	 *             if a workspace project resource that the user wants to
	 *             overwrite could not be deleted first
	 */
	private List<String> performOverwriteChecks() throws CoreException,
			IOException {
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		final List<String> packages = new ArrayList<>();
		for (String packageName : page.getPackageNames()) {
			packageName = packageName.trim();
			if (packageName.isEmpty()) {
				continue;
			}
			// Get confirmation before overwriting existing projects
			IResource project = wsRoot.findMember(packageName);
			if (project != null && project.exists()) {
				final String confirmOverwriteProjectMessage = NLS
						.bind(FedoraPackagerGitText.FedoraPackagerGitCloneWizard_confirmOverwirteProjectExists,
								project.getName());
				if (!confirmOverwriteQuestion(confirmOverwriteProjectMessage)) {
					continue;
				} else {
					// delete project
					project.delete(true, null);
				}
			}
			// Get confirmation before overwriting pre-existing directories
			File newDir = new File(wsRoot.getLocation().toOSString(),
					packageName);
			if (newDir.exists() && newDir.isDirectory()) {
				String contents[] = newDir.list();
				if (contents.length != 0) {
					// ask for confirmation before we overwrite
					final String confirmOverwriteQuestion = NLS
							.bind(FedoraPackagerGitText.FedoraPackagerGitCloneWizard_filesystemResourceExistsQuestion,
									packageName);
					if (!confirmOverwriteQuestion(confirmOverwriteQuestion)) {
						continue;
					} else {
						Files.walkFileTree(newDir.toPath(),
								new DeleteDirectoryVisitor());
					}
				}
			}
			packages.add(packageName);
		}
		return packages;
	}

	/**
	 * Prompt for confirmation if a resource exists, either the project already
	 * exists, or a folder exists in the workspace and would conflict with the
	 * newly created project.
	 *
	 * @param errorMessage
	 *            The error message to be displayed.
	 * @return {@code true} if the user confirmed, {@code false} otherwise.
	 */
	private boolean confirmOverwriteQuestion(String errorMessage) {
		QuestionMessageDialog op = new QuestionMessageDialog(
				FedoraPackagerGitText.FedoraPackagerGitCloneWizard_confirmDialogTitle,
				errorMessage, getShell());
		Display.getDefault().syncExec(op);
		return op.isOkPressed();
	}

	/**
	 * Opens error dialog with provided reason in error message.
	 *
	 * @param errorMsg
	 *            The error message to use.
	 */
	private void cloneFailChecked(String errorMsg) {
		ErrorDialog
				.openError(
						getShell(),
						getWindowTitle()
								+ FedoraPackagerGitText.FedoraPackagerGitCloneWizard_problem,
						FedoraPackagerGitText.FedoraPackagerGitCloneWizard_cloneFail,
						new Status(
								IStatus.ERROR,
								org.fedoraproject.eclipse.packager.git.Activator.PLUGIN_ID,
								0, errorMsg, null));
	}

	/**
	 * Determine the Git clone URL for the given package in the following order:
	 * <ol>
	 * <li>Use the Git base URL as set by the preference (if any) or</li>
	 * <li>Check if ~/.fedora.cert is present, and if so retrieve the user name
	 * from it.</li>
	 * <li>If all else fails, or anonymous checkout is specified, construct an
	 * anonymous clone URL</li>
	 * </ol>
	 * 
	 * @param packageName
	 *            the name of the package we wish to clone
	 * @return The full clone URL based on the given package name.
	 */
	private String getGitCloneURL(final String packageName) {
		// if the fas username is not unknown and if the clone is not anonymous
		if (!fasUserName.equals(FedoraSSL.UNKNOWN_USER)
				&& !page.getCloneAnonymousButtonChecked()) {
			return GitUtils.getFullGitURL(
					GitUtils.getAuthenticatedGitBaseUrl(fasUserName),
					packageName);
		} else {
			// anonymous
			return GitUtils.getFullGitURL(GitUtils.getAnonymousGitBaseUrl(),
					packageName);
		}
	}

	private static void configureRpmlintBuilder(IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();
		String[] natures = desc.getNatureIds();

		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = "org.eclipse.linuxtools.rpm.rpmlint.rpmlintNature"; //$NON-NLS-1$
		desc.setNatureIds(newNatures);
		project.setDescription(desc, null);
	}
}
