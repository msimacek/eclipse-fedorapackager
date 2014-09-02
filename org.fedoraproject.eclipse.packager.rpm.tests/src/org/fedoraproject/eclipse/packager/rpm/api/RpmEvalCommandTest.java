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
package org.fedoraproject.eclipse.packager.rpm.api;

import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.junit.Test;

/**
 * Tests for the RPM eval command.
 */
public class RpmEvalCommandTest extends FedoraPackagerTest {

	/**
	 * Test method for input validation.
	 * Should have thrown an exception. Command is not properly configured.
	 */
	@Test(expected=CommandMisconfiguredException.class)
	public void testCheckConfiguration() throws FedoraPackagerAPIException {
		RpmEvalCommand eval = (RpmEvalCommand) packager
				.getCommandInstance(RpmEvalCommand.ID);
		eval.call(new NullProgressMonitor());
	}

	/**
	 *  This illustrates proper usage of {@link RpmEvalCommand}.
	 */
	@Test
	public void canEvalArchitecture() throws FedoraPackagerAPIException {
		RpmEvalCommand eval = (RpmEvalCommand) packager
				.getCommandInstance(RpmEvalCommand.ID);
		EvalResult result;
			result = eval.variable(RpmEvalCommand.ARCH).call(new NullProgressMonitor());
		assertTrue(result.isSuccessful());
	}

}
