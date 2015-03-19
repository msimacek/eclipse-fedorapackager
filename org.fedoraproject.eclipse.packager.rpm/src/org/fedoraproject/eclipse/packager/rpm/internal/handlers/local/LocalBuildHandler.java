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
package org.fedoraproject.eclipse.packager.rpm.internal.handlers.local;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.fedoraproject.eclipse.packager.BranchConfigInstance;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.rpm.RPMPlugin;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand;
import org.fedoraproject.eclipse.packager.rpm.api.RpmBuildCommand.BuildType;
import org.fedoraproject.eclipse.packager.rpm.api.errors.RpmBuildCommandException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Handler for building locally. This is the modified version of
 * org.fedoraproject
 * .eclipse.packager.rpm.internal.handlers.LocalBuildHandler.java to make it
 * work with Local Fedora Packager Project since in the local version
 * downloading source from lookaside cache is not applicable
 */
public class LocalBuildHandler extends LocalHandlerDispatcher {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Perhaps need to dispatch to non-local handler
		if (checkDispatch(event, getDispatchee())) {
			// dispatched, so return
			return null;
		}
		final Shell shell =  HandlerUtil.getActiveShellChecked(event);
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		try {
			IResource eventResource = FedoraHandlerUtils.getResource(event);
			final IProjectRoot projectRoot = FedoraPackagerUtils
					.getProjectRoot(eventResource);

			FedoraPackager fp = new FedoraPackager(projectRoot);
			final RpmBuildCommand rpmBuild;
			try {
				// get RPM build command in order to produce an SRPM
				rpmBuild = (RpmBuildCommand) fp
						.getCommandInstance(RpmBuildCommand.ID);
			} catch (FedoraPackagerCommandNotFoundException e) {
				logger.logError(e.getMessage(), e);
				FedoraHandlerUtils.showErrorDialog(shell, projectRoot
						.getProductStrings().getProductName(), e.getMessage());
				return null;
			} catch (FedoraPackagerCommandInitializationException e) {
				logger.logError(e.getMessage(), e);
				FedoraHandlerUtils.showErrorDialog(shell, projectRoot
						.getProductStrings().getProductName(), e.getMessage());
				return null;
			}
			Job job = new Job(projectRoot.getProductStrings().getProductName()) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {

					// Do the local build
					Job rpmBuildjob = new Job(projectRoot.getProductStrings()
							.getProductName()) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								monitor.beginTask(getTaskName(),
										IProgressMonitor.UNKNOWN);
								IFpProjectBits projectBits = FedoraPackagerUtils
										.getVcsHandler(projectRoot);
								BranchConfigInstance bci = projectBits
										.getBranchConfig();
								try {
									rpmBuild.buildType(getBuildType())
											.branchConfig(bci).call(monitor);
									projectRoot.getProject().refreshLocal(
											IResource.DEPTH_INFINITE, monitor);
								} catch (CommandListenerException|RpmBuildCommandException|IllegalArgumentException e) {
									logger.logError(e.getMessage(), e);
									return new Status(IStatus.ERROR,
											RPMPlugin.PLUGIN_ID,
											e.getMessage(), e);
								} catch (CoreException e) {
									// should not occur
									logger.logError(e.getMessage(),
											e.getCause());
									return new Status(IStatus.ERROR,
											RPMPlugin.PLUGIN_ID,
											e.getMessage(), e.getCause());
								} catch (OperationCanceledException e) {
									FedoraHandlerUtils
											.showErrorDialog(
													shell,
													RpmText.LocalBuildHandler_buildCanceled,
													RpmText.LocalBuildHandler_buildCancelationResponse);
								}
							} finally {
								monitor.done();
							}
							return Status.OK_STATUS;
						}
					};
					rpmBuildjob.setUser(true);
					rpmBuildjob.schedule();
					try {
						// wait for job to finish
						rpmBuildjob.join();
					} catch (InterruptedException e1) {
						throw new OperationCanceledException();
					}
					return rpmBuildjob.getResult();
				}

			};
			// Suppress UI progress reporting. This is done by sub-jobs within.
			job.setSystem(true);
			job.schedule();
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
		}
		return null;
	}

	/**
	 * Set the build type, overridden by compile/install handler
	 */
	protected BuildType getBuildType() {
		return BuildType.BINARY;
	}

	/**
	 * Set the task name, overridden by compile/install handler
	 */
	protected String getTaskName() {
		return RpmText.LocalBuildHandler_buildForLocalArch;
	}

	@Override
	protected AbstractHandler getDispatchee() {
		return new org.fedoraproject.eclipse.packager.rpm.internal.handlers.LocalBuildHandler();
	}

}
