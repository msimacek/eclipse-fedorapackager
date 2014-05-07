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
package org.fedoraproject.eclipse.packager.api;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jsch.internal.core.IConstants;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.eclipse.jsch.internal.core.PreferenceInitializer;
import org.eclipse.jsch.ui.UserInfoPrompter;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.FedoraSSLFactory;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.ScpFailedException;
import org.fedoraproject.eclipse.packager.utils.WrappedSession;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Job to perform Scp Command.
 *
 */
@SuppressWarnings("restriction")
public class ScpJob extends Job {

	private IProjectRoot projectRoot;
	private String srpm;
	private static final String FEDORAHOST = "fedorapeople.org"; //$NON-NLS-1$
	private ScpCommand scpCmd;
	private String finalMessage;

	/**
	 * Default constructor.
	 *
	 * @param name
	 *            Name of the job.
	 * @param root
	 *            The project root the job is run under.
	 * @param srpm
	 *            The name of the srpm to be uploaded.
	 * @param scp
	 *            The ScpCommand to be run.
	 */
	public ScpJob(String name, IProjectRoot root, String srpm,
			ScpCommand scp) {
		super(name);
		projectRoot = root;
		this.srpm = srpm;
		scpCmd = scp;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(FedoraPackagerText.ScpHandler_taskName,
				IProgressMonitor.UNKNOWN);
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		IStatus result;
		scpCmd.specFile(projectRoot.getSpecFile().getName());
		scpCmd.srpmFile(srpm);
		// scpCmd.srpmFile(((IResource) ld.getResult()[0])
		// .getName());

		String fasAccount = FedoraSSLFactory.getInstance()
				.getUsernameFromCert();
		JSch jsch = new JSch();

		IPreferencesService service = Platform.getPreferencesService();
		String ssh_home = service.getString(JSchCorePlugin.ID,
				IConstants.KEY_SSH2HOME,
				PreferenceInitializer.SSH_HOME_DEFAULT, null);
		String ssh_keys = service.getString(JSchCorePlugin.ID,
				IConstants.KEY_PRIVATEKEY, "id_rsa", null); //$NON-NLS-1$

		String[] ssh_key = ssh_keys.split(","); //$NON-NLS-1$
		try {
			String privateKeyFile = ssh_home.concat("/").concat(ssh_key[1]); //$NON-NLS-1$
			if (privateKeyFile != null && new File(privateKeyFile).exists()) {
				jsch.addIdentity(privateKeyFile);
			}

			Session session;
			session = jsch.getSession(fasAccount, FEDORAHOST, 22);
			UserInfo userInfo = session.getUserInfo();
			if (userInfo == null || userInfo.getPassword() == null) {
				@SuppressWarnings("unused")
				UserInfoPrompter userInfoPrompt = new UserInfoPrompter(session);
			}
			scpCmd.session(new WrappedSession(session));
			result = scpCmd.call(monitor);
			if (result.isOK()) {
				String message = null;
				message = NLS.bind(
						FedoraPackagerText.ScpHandler_scpFilesNotifier,
						fasAccount);
				finalMessage = message.concat("\n*" + projectRoot.getSpecFile().getName() + "\n*" + srpm); //$NON-NLS-1$ //$NON-NLS-2$

			}
			return result;

		} catch (CommandMisconfiguredException|CommandListenerException e) {
			logger.logError(e.getMessage(), e);
			return new Status(IStatus.ERROR, PackagerPlugin.PLUGIN_ID,
					e.getMessage(), e);
		} catch (ScpFailedException|JSchException e) {
			logger.logError(e.getCause().getMessage(), e);
			return new Status(IStatus.ERROR, PackagerPlugin.PLUGIN_ID,
					NLS.bind(FedoraPackagerText.ScpHandler_failToScp, e
							.getCause().getMessage()));
		}
	}

	/**
	 * Get a message from the completed job.
	 *
	 * @return The message.
	 */
	public String getMessage() {
		if (getState() == Job.NONE) {
			return finalMessage;
		}
		return null;
	}

}
