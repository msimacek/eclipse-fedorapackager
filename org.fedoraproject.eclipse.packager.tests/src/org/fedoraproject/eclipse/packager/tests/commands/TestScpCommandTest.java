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
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.ScpCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.ScpFailedException;
import org.fedoraproject.eclipse.packager.tests.utils.TestsUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;
import org.fedoraproject.eclipse.packager.utils.IChannelExec;
import org.fedoraproject.eclipse.packager.utils.IChannelSftp;
import org.fedoraproject.eclipse.packager.utils.ISession;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class TestScpCommandTest {

	private static final String EXAMPLE_FEDORA_PROJECT_ROOT = "resources/example-fedora-project"; // $NON-NLS-1$
	// project under test
	private IProject testProject;
	// main interface class
	private FedoraPackager packager;
	// Fedora packager root
	private IProjectRoot fpRoot;
	private ScpCommand command;

	@Before
	public void setUp() throws Exception {
		String dirName = FileLocator.toFileURL(
				FileLocator.find(FrameworkUtil.getBundle(this.getClass()),
						new Path(EXAMPLE_FEDORA_PROJECT_ROOT), null)).getFile();
		File origSourceDir = new File(dirName);
		testProject = TestsUtils.createProjectFromTemplate(origSourceDir, "example-fedora-project");
		testProject.setPersistentProperty(PackagerPlugin.PROJECT_PROP,
				"true" /* unused value */);
		fpRoot = FedoraPackagerUtils.getProjectRoot(testProject);
		assertNotNull(fpRoot);
		packager = new FedoraPackager(fpRoot);
		assertNotNull(packager);
		command = (ScpCommand) packager.getCommandInstance(ScpCommand.ID);
	}

	@Test(expected = CommandMisconfiguredException.class)
	public void testCheckConfig() throws CommandMisconfiguredException,
			CommandListenerException, ScpFailedException {
		command.call(new NullProgressMonitor());
	}

	@Test
	public void testScpCommand() throws JSchException, SftpException,
			IOException, CommandMisconfiguredException,
			CommandListenerException, ScpFailedException {
		ISession session = createMock(ISession.class);
		IChannelSftp channelSftp = createMock(IChannelSftp.class);
		IChannelExec channelExec = createMock(IChannelExec.class);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		expect(session.openChannelSftp()).andReturn(channelSftp);
		channelSftp.connect();
		Vector<String> folderVector = new Vector<String>();
		expect(channelSftp.stringLs("public_html")).andReturn(folderVector);
		channelSftp.mkdir("public_html/fpe-rpm-review");
		expect(channelSftp.stringLs("public_html/fpe-rpm-review")).andReturn(
				folderVector);
		channelSftp.disconnect();
		expect(session.openChannelExec()).andReturn(channelExec).times(2);
		channelExec
				.setCommand("scp -p -t public_html/fpe-rpm-review/example-fedora-project.spec");
		channelExec
				.setCommand("scp -p -t public_html/fpe-rpm-review/example-fedora-project-0.1.11-1.fc18.src.rpm");
		expect(channelExec.getOutputStream()).andReturn(new OutputStream() {
			@SuppressWarnings("unused")
			@Override
			public void write(int b) throws IOException {
				// do nothing
			}
		}).times(2);
		expect(channelExec.getInputStream()).andReturn(new InputStream() {

			@SuppressWarnings("unused")
			@Override
			public int read() throws IOException {
				// always success
				return 0;
			}

		}).times(2);
		channelExec.connect();
		channelExec.connect();
		channelExec.disconnect();
		channelExec.disconnect();
		session.disconnect();
		replay(session);
		replay(channelSftp);
		replay(channelExec);
		command.session(session);
		command.specFile("example-fedora-project.spec");
		command.srpmFile("example-fedora-project-0.1.11-1.fc18.src.rpm");
		assertTrue(command.call(new NullProgressMonitor()).isSuccessful());
		verify(session);
		verify(channelSftp);
		verify(channelExec);
	}
}
