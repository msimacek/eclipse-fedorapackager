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
package org.fedoraproject.eclipse.packager.tests.units;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UploadFileValidityTest {

	private File emptyFileRegular;
	private File emptyFileInvalid;
	private File regularFileNonEmpty;
	private File regularFileNonEmptyInvalid;
	
	@Before
	public void setUp() {
		try {
			// Create files
			emptyFileRegular = File.createTempFile("eclipse-fed-packager-test", ".rpm");
			emptyFileInvalid = File.createTempFile("eclipse-fed-packager-test", ".exe");
			regularFileNonEmpty = File.createTempFile("eclipse-fed-packager-test", ".cpio");
			regularFileNonEmptyInvalid = File.createTempFile("eclipse-fed-packager-test", ".invalid");
			
			// Make it empty
			BufferedWriter out = new BufferedWriter(new FileWriter(emptyFileRegular));
			out.write("");
			out.close();
			// Make it empty, again
			out = new BufferedWriter(new FileWriter(emptyFileInvalid));
			out.write("");
			out.close();
			// Put text into file
			out = new BufferedWriter(new FileWriter(regularFileNonEmpty));
			out.write("I'm not empty!");
			out.close();
			// write something into file
			out = new BufferedWriter(new FileWriter(regularFileNonEmptyInvalid));
			out.write("I'm not empty!");
			out.close();
		} catch (IOException e) { }
	}
	
	@After
	public void tearDown() {
		// tear down the house
		if (emptyFileRegular.exists()) {
			emptyFileRegular.delete();
		}
		if (emptyFileInvalid.exists()) {
			emptyFileInvalid.delete();
		}
		if (regularFileNonEmpty.exists()) {
			regularFileNonEmpty.delete();
		}
		if (regularFileNonEmptyInvalid.exists()) {
			regularFileNonEmptyInvalid.delete();
		}
	}
	
	/**
	 * Test FedoraHandlerUtils.isValidUploadFile().
	 */
	@Test
	public void testIsValidUploadFile() {
		assertFalse(FedoraPackagerUtils.isValidUploadFile(emptyFileRegular));
		assertFalse(FedoraPackagerUtils.isValidUploadFile(emptyFileInvalid));
		assertTrue(FedoraPackagerUtils.isValidUploadFile(regularFileNonEmpty));
		assertFalse(FedoraPackagerUtils.isValidUploadFile(regularFileNonEmptyInvalid));
	}

}
