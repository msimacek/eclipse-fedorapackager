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
package org.fedoraproject.eclipse.packager.internal.handlers;

import java.util.HashSet;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.FedoraSSLFactory;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.FedoraPackagerAbstractHandler;
import org.fedoraproject.eclipse.packager.api.ScpCommand;
import org.fedoraproject.eclipse.packager.api.ScpResult;
import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;
import org.fedoraproject.eclipse.packager.api.errors.CommandMisconfiguredException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.api.errors.ScpFailedException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Class responsible for copying necessary files for review to fedorapeople.org
 */
public class ScpHandler extends FedoraPackagerAbstractHandler {

	/**
	 * Copies existing .spec and .src.rpm files from the local location of the project
	 * to remote (fedorapeople.org) It will retrieve the fas account of the user
	 * from the .fedora.cert if exists otherwise, anonymous.
	 *
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Shell shell = getShell(event);
		final FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		try {
			IResource eventResource = FedoraHandlerUtils.getResource(event);
			setProjectRoot(FedoraPackagerUtils.getProjectRoot(eventResource));
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
			return null;
		}

		final FedoraPackager packager = new FedoraPackager(
				getProjectRoot());

		// Do the copying to remote - scp
		Job job = new Job(FedoraPackagerText.ScpHandler_taskName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(FedoraPackagerText.ScpHandler_taskName,
						IProgressMonitor.UNKNOWN);

				ScpCommand scpCmd = null;
				ScpResult result;

				try {
					// Get ScpCommand from Fedora packager registry
					scpCmd = (ScpCommand) packager
							.getCommandInstance(ScpCommand.ID);
				} catch (FedoraPackagerCommandNotFoundException e) {
					logger.logError(e.getMessage(), e);
					FedoraHandlerUtils.showErrorDialog(shell,
							getProjectRoot().getProductStrings()
									.getProductName(), e.getMessage());
					return null;
				} catch (FedoraPackagerCommandInitializationException e) {
					logger.logError(e.getMessage(), e);
					FedoraHandlerUtils.showErrorDialog(shell,
							getProjectRoot().getProductStrings()
									.getProductName(), e.getMessage());
					return null;
				}


				try {

					scpCmd.specFile(getProjectRoot().getSpecFile().getName());

					HashSet<IResource> options = new HashSet<IResource>();
					for (IResource resource : getProjectRoot().getProject().members()){
						if (resource.getName().endsWith(".src.rpm")){ //$NON-NLS-1$
							options.add(resource);
						}
					}
					if (options.size() == 0){
						return null;
					}
					final IResource[] syncOptions = options.toArray(new IResource[0]);
					final ListDialog ld = new ListDialog(shell);
					shell.getDisplay().syncExec(new Runnable() {
						@Override
						public void run(){
							ld.setContentProvider(new ArrayContentProvider());
							ld.setLabelProvider(new WorkbenchLabelProvider());
							ld.setInput(syncOptions);
							ld.setMessage(FedoraPackagerText.ScpHandler_FilesDialogTitle);
							ld.open();
						}
					});
					if (ld.getReturnCode() == Window.CANCEL){
						throw new OperationCanceledException();
					}

					scpCmd.srpmFile(((IResource)ld.getResult()[0]).getName());

				} catch (CoreException e) {
					e.printStackTrace();
				}

				 String fasAccount =
					 FedoraSSLFactory.getInstance().getUsernameFromCert();

				try {
					scpCmd.fasAccount(fasAccount);
					result = scpCmd.call(monitor);
					if (result.wasSuccessful()) {
						String message = null;
						message = NLS.bind(
								FedoraPackagerText.ScpHandler_scpFilesNotifier,
								fasAccount);
						String finalMessage = result
								.getHumanReadableMessage(message);
						FedoraHandlerUtils.showInformationDialog(shell,
								FedoraPackagerText.ScpHandler_notificationTitle,
								finalMessage);
					}
					return Status.OK_STATUS;

				} catch (CommandMisconfiguredException e) {
					logger.logError(e.getMessage(), e);
					return FedoraHandlerUtils.errorStatus(
							PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
				} catch (CommandListenerException e) {
					logger.logError(e.getMessage(), e);
					return FedoraHandlerUtils.errorStatus(
							PackagerPlugin.PLUGIN_ID, e.getMessage(), e);
				} catch (ScpFailedException e) {
					logger.logError(e.getCause().getMessage(), e);
					return FedoraHandlerUtils.errorStatus(
							PackagerPlugin.PLUGIN_ID,
							NLS.bind(
									FedoraPackagerText.ScpHandler_failToScp,
									e.getCause().getMessage()));
				}
			}
		};

		job.setUser(true);
		job.schedule();
		return null;
	}

}
