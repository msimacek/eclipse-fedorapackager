package org.fedoraproject.eclipse.packager.rpm.api;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.DownloadFailedException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.SourcesUpToDateException;
import org.fedoraproject.eclipse.packager.rpm.api.errors.RpmBuildCommandException;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.FrameworkUtil;

public class FedoraPackagerTest {

	private static final String EXAMPLE_GIT_PROJECT_ROOT = "resources/example-git-project"; //$NON-NLS-1$

	// project under test
	protected GitTestProject testProject;
	// main interface class
	protected FedoraPackager packager;
	// Fedora packager root
	protected IProjectRoot fpRoot;
	protected BranchConfigInstance bci;

	/**
	 * Clone a test project to be used for testing.
	 * @throws InterruptedException 
	 * @throws CoreException 
	 * @throws InvalidRefNameException 
	 * @throws RefNotFoundException 
	 * @throws RefAlreadyExistsException 
	 * @throws JGitInternalException 
	 * @throws InvalidProjectRootException 
	 * @throws IOException 
	 * @throws RpmBuildCommandException 
	 * @throws CommandListenerException 
	 * @throws CommandMisconfiguredException 
	 * @throws FedoraPackagerCommandNotFoundException 
	 * @throws FedoraPackagerCommandInitializationException 
	 * @throws DownloadFailedException 
	 * @throws SourcesUpToDateException 
	 * 
	 */
	@SuppressWarnings("unused")
	@Before
	public void setUp() throws InterruptedException, JGitInternalException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CoreException, InvalidProjectRootException, IOException, FedoraPackagerCommandInitializationException, FedoraPackagerCommandNotFoundException, CommandMisconfiguredException, CommandListenerException, RpmBuildCommandException, SourcesUpToDateException, DownloadFailedException  {
		String exampleGitdirPath = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_GIT_PROJECT_ROOT), null)).getFile();

		this.testProject = new GitTestProject("example", TestsUtils.prepLocalGitTestProject(exampleGitdirPath)); //$NON-NLS-1$

		testProject.checkoutBranch("f17"); //$NON-NLS-1$
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);
		bci = FedoraPackagerUtils.getVcsHandler(fpRoot).getBranchConfig();
	}

	/**
	 * @throws CoreException 
	 */
	@After
	public void tearDown() throws CoreException  {
		this.testProject.dispose();
	}

}
