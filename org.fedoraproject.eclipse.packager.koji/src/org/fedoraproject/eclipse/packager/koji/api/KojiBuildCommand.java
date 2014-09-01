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
package org.fedoraproject.eclipse.packager.koji.api;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerCommand;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.api.errors.BuildAlreadyExistsException;
import org.fedoraproject.eclipse.packager.koji.api.errors.KojiHubClientException;

/**
 * Fedora Packager koji build command. Supports scratch builds and regular
 * builds.
 */
public class KojiBuildCommand extends FedoraPackagerCommand<BuildResult> {

	/**
	 * The unique ID of this command.
	 */
	public static final String ID = "KojiBuildCommand"; //$NON-NLS-1$
	/**
	 * The XMLRPC based client to use for Koji interaction.
	 */
	protected IKojiHubClient kojiClient;
	/**
	 * Set to true if scratch build should be pushed instead of a regular build.
	 */
	protected boolean scratchBuild = false;
	/**
	 * The URL into the VCS repo which should be used for the build.
	 */
	protected List<?> location;
	/**
	 * The distribution tag (e.g. dist-rawhide)
	 */
	protected String buildTarget;
	/**
	 * The name-version-release token to push a build for
	 */
	protected String[] nvr;

	@Override
	protected void checkConfiguration() throws CommandMisconfiguredException {
		// require a client
		if (kojiClient == null) {
			throw new CommandMisconfiguredException(NLS.bind(
					KojiText.KojiBuildCommand_configErrorNoClient,
					this.projectRoot.getProductStrings().getBuildToolName()));
		}
		// we also require scmURL to be set
		if (location == null
				|| location.isEmpty()
				|| !(location.get(0) instanceof String || location.get(0) instanceof List<?>)) {
			throw new CommandMisconfiguredException(
					KojiText.KojiBuildCommand_configErrorNoScmURL);
		}
		// distribution can't be null
		if (buildTarget == null) {
			throw new CommandMisconfiguredException(
					KojiText.KojiBuildCommand_configErrorNoBuildTarget);
		}
		// nvr can't be null
		if (nvr == null || nvr.length == 0) {
			throw new CommandMisconfiguredException(
					KojiText.KojiBuildCommand_configErrorNoNVR);
		}
	}

	/**
	 * Sets the XMLRPC based client, which will be used for Koji interaction.
	 *
	 * @param client
	 *            The client to be used.
	 * @return This instance.
	 */
	public KojiBuildCommand setKojiClient(IKojiHubClient client) {
		this.kojiClient = client;
		return this;
	}

	/**
	 * Set this to {@code true} if a scratch build should be pushed instead of a
	 * regular build.
	 *
	 * @param newValue
	 *            True if the build is a scratch build, false otherwise.
	 * @return This instance.
	 */
	public KojiBuildCommand isScratchBuild(boolean newValue) {
		this.scratchBuild = newValue;
		return this;
	}

	/**
	 * Sets the URL into the source control management system, in order to be
	 * able to determine which tag/revision to build.
	 *
	 * @param location
	 *            The location of the source: either an SCM location with a
	 *            specfile and a tarball or the location of an uploaded srpm on
	 *            the Koji server.
	 * @return This instance.
	 */
	public KojiBuildCommand sourceLocation(List<?> location) {
		this.location = location;
		return this;
	}

	/**
	 * Sets the build target for which to push the build for.
	 *
	 * @param buildTarget
	 *            The target to build for.
	 * @return This instance.
	 */
	public KojiBuildCommand buildTarget(String buildTarget) {
		this.buildTarget = buildTarget;
		return this;
	}

	/**
	 * Sets the name-version-release token for which a build should be pushed.
	 *
	 * @param nvr
	 *            The array of name, version and release Strings.
	 * @return This instance.
	 */
	public KojiBuildCommand nvr(String[] nvr) {
		this.nvr = nvr;
		return this;
	}

	/**
	 * Implementation of the {@code KojiBuildCommand}.
	 *
	 * @param monitor
	 *            The main progress monitor. Each other task is executed as a
	 *            subtask.
	 * @throws BuildAlreadyExistsException
	 *             If a build which would have otherwise be pushed already
	 *             existed in Koji.
	 * @throws CommandListenerException
	 *             If some listener detected a problem.
	 * @throws KojiHubClientException
	 *             If some other error occured while pushing a build.
	 * @return The result of this command.
	 */
	@Override
	public BuildResult call(IProgressMonitor monitor)
			throws BuildAlreadyExistsException,
			CommandListenerException, KojiHubClientException {
		callPreExecListeners();
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		// main monitor worked for 30
		BuildResult result = new BuildResult();
		monitor.subTask(KojiText.KojiBuildCommand_sendBuildCmd);
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		if (this.scratchBuild) {
			logger.logDebug(KojiText.KojiBuildCommand_scratchBuildLogMsg);
		} else {
			logger.logDebug(KojiText.KojiBuildCommand_buildLogMsg);
		}
		// attempt to push build
		int taskId = this.kojiClient.build(buildTarget, location, nvr,
				scratchBuild)[0];
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		result.setTaskId(taskId);
		monitor.worked(80);
		monitor.subTask(KojiText.KojiBuildCommand_kojiLogoutTask);
		this.kojiClient.logout();
		monitor.worked(90);
		callPostExecListeners();
		setCallable(false); // reuse of instance's call() not allowed
		result.setSuccessful(true);
		monitor.done();
		return result;
	}

}
