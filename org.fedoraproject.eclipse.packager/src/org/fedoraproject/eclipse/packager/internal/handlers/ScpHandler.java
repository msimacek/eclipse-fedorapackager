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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.IProjectRoot;
import org.fedoraproject.eclipse.packager.api.FedoraPackager;
import org.fedoraproject.eclipse.packager.api.ScpCommand;
import org.fedoraproject.eclipse.packager.api.ScpJob;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandInitializationException;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerCommandNotFoundException;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Class responsible for copying necessary files for review to fedorapeople.org
 */
public class ScpHandler extends AbstractHandler {

	/**
	 * Copies existing .spec and .src.rpm files from the local location of the
	 * project to remote (fedorapeople.org) It will retrieve the fas account of
	 * the user from the .fedora.cert if exists otherwise, anonymous.
	 * 
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		FedoraPackagerLogger logger = FedoraPackagerLogger.getInstance();
		final Shell shell = HandlerUtil.getActiveShellChecked(event);
		try {
			IResource eventResource = FedoraHandlerUtils.getResource(event);
			final IProjectRoot projectRoot = FedoraPackagerUtils
					.getProjectRoot(eventResource);
			HashSet<IResource> options = new HashSet<>();
			for (IResource resource : projectRoot.getProject().members()) {
				if (resource.getName().endsWith(".src.rpm")) { //$NON-NLS-1$
					options.add(resource);
				}
			}
			if (options.isEmpty()) {
				return null;
			}
			final IResource[] syncOptions = options.toArray(new IResource[0]);
			final ListDialog ld = new ListDialog(shell);
			shell.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					ld.setContentProvider(new ArrayContentProvider());
					ld.setLabelProvider(new WorkbenchLabelProvider());
					ld.setInput(syncOptions);
					ld.setMessage(FedoraPackagerText.ScpHandler_FilesDialogTitle);
					ld.open();
				}
			});
			if (ld.getReturnCode() == Window.CANCEL) {
				throw new OperationCanceledException();
			}
			ScpCommand scpCmd = null;
			FedoraPackager packager = new FedoraPackager(projectRoot);
			try {
				// Get ScpCommand from Fedora packager registry
				scpCmd = (ScpCommand) packager
						.getCommandInstance(ScpCommand.ID);
			} catch (FedoraPackagerCommandNotFoundException|FedoraPackagerCommandInitializationException e) {
				logger.logError(e.getMessage(), e);
				FedoraHandlerUtils.showErrorDialog(shell, projectRoot
						.getProductStrings().getProductName(), e.getMessage());
				return null;
			}
			// Do the copying to remote - scp
			ScpJob job = new ScpJob(FedoraPackagerText.ScpHandler_taskName,
					projectRoot, ((IResource) ld.getResult()[0]).getName(),
					scpCmd);

			job.setUser(true);
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				// ignore
			}
			if (job.getResult() == Status.OK_STATUS) {
				FedoraHandlerUtils.showInformationDialog(shell,
						FedoraPackagerText.ScpHandler_notificationTitle,
						job.getMessage());
			}
		} catch (InvalidProjectRootException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", //$NON-NLS-1$
					FedoraPackagerText.invalidFedoraProjectRootError);
		} catch (CoreException e) {
			logger.logError(FedoraPackagerText.invalidFedoraProjectRootError, e);
			FedoraHandlerUtils.showErrorDialog(shell, "Error", e.getMessage()); //$NON-NLS-1$
		}
		return null;
	}

}
