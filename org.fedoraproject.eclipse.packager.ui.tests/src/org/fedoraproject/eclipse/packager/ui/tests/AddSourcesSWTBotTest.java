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
package org.fedoraproject.eclipse.packager.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.ui.tests.utils.ContextMenuHelper;
import org.fedoraproject.eclipse.packager.ui.tests.utils.PackageExplorer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
 
@RunWith(SWTBotJunit4ClassRunner.class)
public class AddSourcesSWTBotTest {
 
	private static SWTWorkbenchBot bot;
	private GitTestProject efpProject;
	
	// the successful upload test requires ~/.fedora.cert
	private File fedoraCert = new File(System.getProperty("user.home") + 
		IPath.SEPARATOR + ".fedora.cert");
	private File tmpExistingFedoraCert; // temporary reference to already existing ~/.fedora.cert
	private boolean fedoraCertExisted = false;
	
	// Filenames used for this test
	private final String VALID_SOURCE_FILENAME_NON_EMPTY = "REMOVE_ME.tar.bz2";
 
	@BeforeClass
	public static void beforeClass() throws Exception {
		bot = new SWTWorkbenchBot();
		try {
			bot.viewByTitle("Welcome").close();
			// hide Subclipse Usage stats popup if present/installed
			bot.shell("Subclipse Usage").activate();
			bot.button("Cancel").click();
		} catch (WidgetNotFoundException e) {
			// ignore
		}
		// Make sure we have the Package Explorer view open and shown
		PackageExplorer.openView();
	}
	
	@Before
	public void setUp() throws Exception {
		// Import eclipse-fedorapackager
		efpProject = new GitTestProject("eclipse-fedorapackager");
		IResource efpSpec = efpProject.getProject().findMember(new Path("eclipse-fedorapackager.spec"));
		assertNotNull(efpSpec);
	}
 	
	
	@After
	public void tearDown() throws Exception {
		this.efpProject.dispose();
		// clean up some temp .fedora.cert
		if (tmpExistingFedoraCert != null) {
			reestablishFedoraCert();
		}
		// remove ~/.fedora.cert if it didn't exist
		if (!fedoraCertExisted && fedoraCert.exists()) {
			fedoraCert.delete();
		}
	}
	
	/**
	 * Context menu click helper. Click on "Add to existing sources".
	 * 
	 * @param Tree of Package Explorer view.
	 * @throws Exception
	 */
	private void clickOnAddNewSources(SWTBotTree packagerTree) {
		String subMenu = "Upload This File";
		String menuItem = "Add to existing sources";
		ContextMenuHelper.clickContextMenu(packagerTree, "Fedora Packager",
				subMenu, menuItem);
	}
	
	/**
	 * Create a file with given name in project as created in setUp().
	 * 
	 * @param name Name of newly created file.
	 * @param contents Null or integer contents of file.
	 * @return A reference to the newly created file.
	 * @throws IOException
	 * @throws CoreException
	 */
	private IResource createNewFile(String name, Integer contents) throws IOException, CoreException {
		IProject project = efpProject.getProject();
		File newSource = new File(project.getLocation().toOSString() +
				IPath.SEPARATOR + name);
		newSource.createNewFile();
		if (contents != null) {
			FileWriter out = new FileWriter(newSource);
			out.write(contents);
			out.close();
		}
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource result = project.findMember(name);
		if (result == null) {
			throw new IOException("Could not create file: '" + name +
					"' for test.");
		}
		return result;
	}
	
	/**
	 * Convenience for readFile("sources").
	 * 
	 * @return Contents of sources file.
	 */
	private String readSourcesFile() {
		return readFile("sources");
	}
	
	/**
	 * Convenience method for readfile(".gitignore").
	 * 
	 * @return Contents of .gitignore file.
	 */
	private String readGitIgnore() {
		return readFile(".gitignore");
	}
	
	/**
	 * Read contents of a text file and return its contents
	 * as a String.
	 * 
	 * @param name The name of the file to read from.
	 * @return The contents of that file.
	 */
	private String readFile(String name) {
		StringBuffer result = new StringBuffer();
		File file = efpProject.getProject().findMember(name).getLocation().toFile();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			do {
				line = br.readLine();
				if (line != null) {
					result.append(line + "\n");
				}
			} while (line != null);
			br.close();
		} catch (IOException e) {
			fail("Could not read from file " + name);
		}
		return result.toString();
	}
	
	/**
	 * We need a valid .fedora.cert for doing a successful upload.
	 * This will first look for a system property called
	 * "eclipseFedoraPackagerTestsCertificate" and if set uses the
	 * file pointed to by it as ~/.fedora.cert. If this property
	 * is not set, ~/.fedora.cert_tests will be used instead. If
	 * nothing succeeds, fail.
	 * 
	 * @return A File handle to a copy of an already existing
	 * 			~/.fedora.cert or null if there wasn't any.
	 */
	private File setupFedoraCert() {
		// move away potentially existing ~/.fedora.cert
		File oldFedoraCertMovedAway = null;
		if (fedoraCert.exists()) {
			fedoraCertExisted = true;
			try {
				oldFedoraCertMovedAway = File.createTempFile(".fedora", ".cert");
				FileInputStream fsIn = new FileInputStream(fedoraCert);
				FileOutputStream fsOut = new FileOutputStream(oldFedoraCertMovedAway);
				int buf;
				// copy stuff
				while ( (buf = fsIn.read()) != -1 ) {
					fsOut.write(buf);
				}
				fsIn.close();
				fsOut.close();
			} catch (IOException e) {
				fail("Unable to setup test: (~/.fedora.cert)!");
			}
		}
		// Use template cert to copy to ~/.fedora.cert
		String certTemplatePath = System.getProperty("eclipseFedoraPackagerTestsCertificate");
		if (certTemplatePath == null) {
			// try ~/.fedora.cert_tests
			File fedoraCertTests = new File(System.getProperty("user.home") +
					IPath.SEPARATOR + ".fedora.cert_tests");
			if (fedoraCertTests.exists()) {
				certTemplatePath = fedoraCertTests.getAbsolutePath();
			} else {
				// can't continue - fail
				fail("System property \"eclipseFedoraPackagerTestsCertificate\" " +
						"needs to be configured or ~/.fedora.cert_tests be present" +
						" in order for this test to work.");
			}
		}
		// certTemplatePath must not be null at this point
		assertNotNull(certTemplatePath);
		
		// Copy things over
		File fedoraCertTests = new File(certTemplatePath);
		try {
			FileInputStream fsIn = new FileInputStream(fedoraCertTests);
			FileOutputStream fsOut = new FileOutputStream(fedoraCert);
			int buf;
			// copy stuff
			while ( (buf = fsIn.read()) != -1 ) {
				fsOut.write(buf);
			}
			fsIn.close();
			fsOut.close();
		} catch (IOException e) {
			fail("Unable to setup test: (~/.fedora.cert)!");
		}
		
		// if there was a ~/.fedora.cert return a File handle to it,
		// null otherwise
		if (fedoraCertExisted) {
			return oldFedoraCertMovedAway;
		} else {
			return null;
		}
	}
	
	/**
	 * Reestablish moved away ~/.fedora.cert
	 * 
	 * @param oldFedoraCertMovedAway The File handle to a copy of ~/.fedora.cert
	 * 								 before any tests were run.
	 */
	private void reestablishFedoraCert() {
		// Do this only if old file still exists
		if (tmpExistingFedoraCert.exists()) {
			try {
				FileInputStream fsIn = new FileInputStream(tmpExistingFedoraCert);
				FileOutputStream fsOut = new FileOutputStream(fedoraCert);
				int buf;
				// copy stuff
				while ( (buf = fsIn.read()) != -1 ) {
					fsOut.write(buf);
				}
				fsIn.close();
				fsOut.close();
				// remove temorary file
				tmpExistingFedoraCert.delete();				
			} catch (IOException e) {
				fail("copying back ~/.fedora.cert");
			}
		}
	}
 
}