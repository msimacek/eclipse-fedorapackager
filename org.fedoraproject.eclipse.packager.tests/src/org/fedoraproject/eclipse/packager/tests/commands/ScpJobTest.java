package org.fedoraproject.eclipse.packager.tests.commands;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.ScpCommand;
import org.fedoraproject.eclipse.packager.api.ScpJob;
import org.fedoraproject.eclipse.packager.api.ScpResult;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class ScpJobTest {

	private static final String EXAMPLE_FEDORA_PROJECT_ROOT = "resources/example-fedora-project"; // $NON-NLS-1$
	// project under test
	private IProject testProject;
	// main interface class
	private FedoraPackager packager;
	// Fedora packager root
	private IProjectRoot fpRoot;

	/**
	 * Set up a Fedora project and run the command.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		String dirName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_FEDORA_PROJECT_ROOT), null)).getFile();
		File origSourceDir = new File(dirName);
		testProject = TestsUtils.createProjectFromTemplate(origSourceDir,
				"example-fedora-project");
		testProject
				.setPersistentProperty(PackagerPlugin.PROJECT_PROP, "true" /*
																			 * unused
																			 * value
																			 */);
		fpRoot = FedoraPackagerUtils.getProjectRoot(testProject);
		assertNotNull(fpRoot);
		packager = new FedoraPackager(fpRoot);
		assertNotNull(packager);
	}

	@Test
	public void testScpJob() {
		ScpJob job = new ScpJob("Test Job", fpRoot,
				"example-fedora-project-0.1.11-1.fc18.src.rpm",
				new ScpCommand() {
					@Override
					public ScpResult call(IProgressMonitor monitor) {
						result = new ScpResult(specFile, srpmFile);
						result.setSuccessful(true);
						return result;
					}
				});

		job.setUser(true);
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			// ignore
		}
		assertTrue(job.getResult() == Status.OK_STATUS);
	}
}
