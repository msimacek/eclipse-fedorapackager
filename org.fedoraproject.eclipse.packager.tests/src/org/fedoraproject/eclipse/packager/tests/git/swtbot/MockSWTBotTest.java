package org.fedoraproject.eclipse.packager.tests.git.swtbot;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertTextContains;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.fedoraproject.eclipse.packager.tests.git.utils.GitTestProject;
import org.fedoraproject.eclipse.packager.tests.utils.swtbot.ContextMenuHelper;
import org.fedoraproject.eclipse.packager.tests.utils.swtbot.PackageExplorer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
 
@RunWith(SWTBotJunit4ClassRunner.class)
public class MockSWTBotTest {
	
	private static SWTWorkbenchBot	bot;
	private GitTestProject edProject;
	private Map<String, Boolean> expectedResources = new HashMap<String, Boolean>();
 
	@BeforeClass
	public static void beforeClass() throws Exception {
		bot = new SWTWorkbenchBot();
		bot.viewByTitle("Welcome").close();
		PackageExplorer.openView();
	}
	
	@Before
	public void setUp() throws Exception {
		// Import ed
		edProject = new GitTestProject("ed");
		// use F13 branch of ed
		edProject.checkoutBranch(Constants.R_HEADS + "f13/master");
		IResource edSpec = edProject.getProject().findMember(new Path("ed.spec"));
		assertNotNull(edSpec);
		// set up expectations for resources produced
		expectedResources.put("root.log", false);
		expectedResources.put("state.log", false);
		expectedResources.put("build.log", false);
		expectedResources.put("RPM", false);
	}
 
	/**
	 * Test mock builds on local architecture. This assumes mock to be installed
	 * and PAM configured (look in /etc/pam.d/mock) so that the test-executing
	 * user is not required to enter the root password.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("static-access")
	@Test
	public void canMockBuildOnLocalArchitecture() throws Exception {
		
		// get tree of Package Explorer view
		SWTBotTree packagerTree = PackageExplorer.getTree();
		
		// Select spec file
		final SWTBotTreeItem edItem = PackageExplorer.getProjectItem(
				packagerTree, "ed");
		edItem.expand();
		edItem.select("ed.spec");
		
		// Click mock build context menu item
		clickOnMockBuild(packagerTree);
		// Wait for fedora packager job to start
		bot.waitUntil(Conditions.shellIsActive(org.fedoraproject.
				eclipse.packager.rpm.Messages.mockBuildHandler_jobName));
		SWTBotShell efpJobWindow = bot.shell(org.fedoraproject.
				eclipse.packager.rpm.Messages.mockBuildHandler_jobName);
		assertNotNull(efpJobWindow);
		// Wait for mock build to finish, this takes a while so increase timeout
		SWTBotPreferences.TIMEOUT = 30000;
		bot.waitUntil(Conditions.shellCloses(efpJobWindow));
		// reset timeout to default
		SWTBotPreferences.TIMEOUT = 5000;
		
		// Assert success
		// Make sure "INFO: done" is printed on Console
		SWTBotView consoleView = bot.viewByTitle("Console");
		Widget consoleViewComposite = consoleView.getWidget();
		StyledText consoleText = bot.widget(WidgetMatcherFactory.widgetOfType(
				StyledText.class),
				consoleViewComposite);
		SWTBotStyledText styledTextConsole = new SWTBotStyledText(consoleText);
		assertTextContains("INFO: Done", styledTextConsole);
		
		// Make sure we have build.log, root.log, and state.log +
		// rpms/srpms have been created
		IProject project = this.edProject.getProject();
		IFolder edBuildFolder = null;
		// Find build folder
		for (IResource item: project.members()) {
			if (item.getName().startsWith("ed-1_1") && item.getName().endsWith("fc13")) {
				edBuildFolder = (IFolder)item;
				break;
			}
		}
		assertNotNull(edBuildFolder);
		// Search build folder for expected resources
		boolean rpmFound = false;
		for (IResource item: edBuildFolder.members()) {
			if (item.getName().equals("build.log")
					|| item.getName().equals("root.log")
					|| item.getName().equals("state.log")) {
				// erase item from expectedResources
				expectedResources.remove(item.getName());
			} else if ( !rpmFound && item.getName().endsWith(".rpm")) {
				expectedResources.remove("RPM");
				// there may be more than one rpms
				rpmFound = true;
			}
		}
		// We are good if expectedResources is empty, should have been
		// removed earlier
		assertTrue("The map of expected resources should have been empty.",
				expectedResources.isEmpty());
	}
 
	@After
	public void tearDown() throws Exception {
		this.edProject.dispose();
	}
	
	/**
	 * Context menu click helper. Click on "Local Build Using Mock".
	 * 
	 * @param Tree of Package Explorer view.
	 * @throws Exception
	 */
	private void clickOnMockBuild(SWTBotTree packagerTree) throws Exception {
		String menuItem = "Local Build Using Mock";
		ContextMenuHelper.clickContextMenu(packagerTree, "Fedora Packager",
				menuItem);
	}
 
}