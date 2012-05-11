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
package org.fedoraproject.eclipse.packager.tests;

import static org.junit.Assert.*;

import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestCase;
import org.junit.Test;

public class GitProjectTypeTest extends GitTestCase {
	
	@Test
	public void testAdaptToFpProject()  {
		// Should return FpProject instance with GIT type
		assertEquals(FedoraPackagerUtils.getProjectType(getiProject()) , FedoraPackagerUtils.ProjectType.GIT);
	}

}
