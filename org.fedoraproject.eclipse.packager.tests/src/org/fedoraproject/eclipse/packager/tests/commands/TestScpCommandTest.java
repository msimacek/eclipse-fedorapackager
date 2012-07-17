/*******************************************************************************
 * Copyright (c) 2010-2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.tests.commands;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;

import java.util.Vector;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.ScpCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.ScpFailedException;
import org.fedoraproject.eclipse.packager.tests.utils.git.GitTestProject;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.fedoraproject.eclipse.packager.utils.IChannelSftp;
import org.fedoraproject.eclipse.packager.utils.ISession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class TestScpCommandTest {

	// project under test
	private GitTestProject testProject;
	// main interface class
	private FedoraPackager packager;
	// Fedora packager root
	private IProjectRoot fpRoot;
	private ScpCommand command;

	@Before
	public void setUp() throws Exception {
		this.testProject = new GitTestProject("eclipse-fedorapackager");
		this.fpRoot = FedoraPackagerUtils.getProjectRoot((this.testProject
				.getProject()));
		this.packager = new FedoraPackager(fpRoot);
		command = (ScpCommand) packager.getCommandInstance(ScpCommand.ID);
	}
	
	@Test(expected = CommandMisconfiguredException.class)
	public void testCheckConfig() throws CommandMisconfiguredException, CommandListenerException, ScpFailedException{
		command.call(new NullProgressMonitor());
	}
	
	@Test
	public void testScpCommandFileExists() throws JSchException, SftpException{
		ISession session = createMock(ISession.class);
		IChannelSftp channel = createMock(IChannelSftp.class);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		expect(session.openChannelSftp()).andReturn(channel);
		Vector<String> folderVector = new Vector<String>();
		folderVector.add("fpe-rpm-review");
		expect(channel.stringLs("public_html")).andReturn(folderVector);
	}

	@After
	public void tearDown() throws Exception {
	}

}
