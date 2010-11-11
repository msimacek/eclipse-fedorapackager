/*******************************************************************************
 * Copyright (c) 2010 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.koji;

import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.xmlrpc.XmlRpcException;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.fedoraproject.eclipse.packager.FedoraProjectRoot;
import org.fedoraproject.eclipse.packager.IFpProjectBits;
import org.fedoraproject.eclipse.packager.handlers.CommonHandler;
import org.fedoraproject.eclipse.packager.handlers.FedoraHandlerUtils;
import org.fedoraproject.eclipse.packager.koji.stubs.KojiHubClientStub;

/**
 * Handler to perform a Koji build.
 * 
 */
public class KojiBuildHandler extends CommonHandler {
	@SuppressWarnings("unused")
	private String dist;
	private IKojiHubClient koji;
	private Job job;
	
	///////////////////////////////////////////////////////////////
	// FIXME: This is used for testing only and is super-ugly, but mocking
	//        things in conjunction with SWTBotTests doesn't work. We really
	//        need to refactor this.
	//        Better ideas very welcome!
	/**
	 * Indicates if stub or real client should be returned by getKoji()
	 */
	public static boolean inTestingMode = false;
	private static IKojiHubClient kojiStub = new KojiHubClientStub();

	@Override
	public Object execute(final ExecutionEvent e) throws ExecutionException {
		final FedoraProjectRoot fedoraProjectRoot = FedoraHandlerUtils
				.getValidRoot(e);
		final IFpProjectBits projectBits = FedoraHandlerUtils
				.getVcsHandler(fedoraProjectRoot);
		// Fixes Trac ticket #35; Need to have shell variable on heap not
		// on a thread's stack.
		shell = getShell(e);

		// Send the build
		job = new Job(Messages.kojiBuildHandler_jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(Messages.kojiBuildHandler_sendBuildToKoji,
						IProgressMonitor.UNKNOWN);
				dist = fedoraProjectRoot.getSpecFile().getParent().getName();
				
				// Initialize koji client. Type of client instantiated is
				// determined by command id.
				setKojiClient(e.getCommand().getId());
				try {
					getKoji().setUrlsFromPreferences();
				} catch (KojiHubClientInitException e) {
					FedoraHandlerUtils.handleError(e);
				}
				
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				IStatus status = Status.OK_STATUS;
				if (projectBits.needsTag()) {
					// Do VCS tagging
					promptForTag();
					status = projectBits.tagVcs(fedoraProjectRoot, monitor);
				}
				if (status.isOK()) {
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					status = newBuild(fedoraProjectRoot, monitor);
					if (status.isOK()) {
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
					}
				}
				monitor.done();
				return status;
			}
		};
		// Create job listener (for event done)
		IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				final IStatus jobStatus = event.getResult();
				PlatformUI.getWorkbench().getDisplay().asyncExec(
						new Runnable() {
							@Override
							public void run() {
								// Only show something on success
								if (jobStatus.isOK()) {
									final String taskId = jobStatus
											.getMessage(); // IStatus message is
															// task ID
									ImageDescriptor descriptor = KojiPlugin
											.getImageDescriptor("icons/Artwork_DesignService_koji-icon-16.png"); //$NON-NLS-1$
									Image titleImage = descriptor.createImage();
									if (shell != null && !shell.isDisposed()) {
										KojiMessageDialog msgDialog = new KojiMessageDialog(
												shell,
												Messages.kojiBuildHandler_kojiBuild,
												titleImage,
												MessageDialog.NONE,
												new String[] { IDialogConstants.OK_LABEL },
												0, getKoji(), taskId);
										msgDialog.open();
									} else { // fall back to console print
										getKoji()
												.writeToConsole(NLS
														.bind(
																Messages.kojiBuildHandler_fallbackBuildMsg,
																taskId));
									}
								} else if (shell != null && !shell.isDisposed()) {
									// Try to show error
									MessageDialog
											.openError(
													shell,
													Messages.kojiBuildHandler_kojiBuild,
													NLS
															.bind(
																	Messages.kojiBuildHandler_buildTaskIdError,
																	jobStatus
																			.getMessage()));
								} else {
									getKoji()
											.writeToConsole(NLS
													.bind(
															Messages.kojiBuildHandler_buildTaskIdError,
															jobStatus
																	.getMessage()));
								}
							}
						});
			}
		};
		job.addJobChangeListener(listener);
		job.setUser(true);
		job.schedule();
		return null;
	}

	private boolean promptForTag() {
		if (debug) {
			// don't worry about tagging for debug mode
			return false;
		}
		YesNoRunnable op = new YesNoRunnable(
				Messages.kojiBuildHandler_tagBeforeSendingBuild); //$NON-NLS-1$
		Display.getDefault().syncExec(op);
		return op.isOkPressed();
	}

	protected IStatus newBuild(FedoraProjectRoot fedoraProjectRoot,
			IProgressMonitor monitor) {
		IStatus status;
		IFpProjectBits projectBits = FedoraHandlerUtils
				.getVcsHandler(fedoraProjectRoot);
		try {
			// for testing use the stub instead
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			monitor.subTask(Messages.kojiBuildHandler_connectKojiMsg);

			String scmURL = projectBits.getScmUrlForKoji(fedoraProjectRoot);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			// login via SSL
			monitor.subTask(Messages.kojiBuildHandler_kojiLogin);
			HashMap<?, ?> sessionData = null;
			try {
				sessionData = getKoji().login();
			} catch (KojiHubClientLoginException e) {
				e.printStackTrace();
				return FedoraHandlerUtils.handleError(e);
			}
			// store session in URL
			try {
				// FIXME: Are we always getting an int?
				Object sessionId = sessionData.get("session-id");
				String sid = null;
				if (sessionId instanceof Integer) {
					sid = ((Integer)sessionId).toString();
				}
				getKoji().saveSessionInfo((String)sessionData.get("session-key"), sid);
			} catch (MalformedURLException e) {
				return FedoraHandlerUtils.handleError(e);
			} catch (ClassCastException e) {
				return FedoraHandlerUtils.handleError(e);
			}

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			// push build
			monitor.subTask(Messages.kojiBuildHandler_sendBuildCmd);
			String result = getKoji().build(projectBits.getTarget(), scmURL, isScratch());
			// if we get an int (that is our taskId)
			int taskId = -1;
			try {
				taskId = Integer.parseInt(result);
			} catch (NumberFormatException e) {
				// ignore
			}
			if (taskId != -1) {
				status = new Status(IStatus.OK, KojiPlugin.PLUGIN_ID,
						new Integer(taskId).toString());
			} else { // Error
				status = new Status(IStatus.ERROR, KojiPlugin.PLUGIN_ID, result);
			}
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			// logout
			monitor.subTask(Messages.kojiBuildHandler_kojiLogout);
			getKoji().logout();
		} catch (XmlRpcException e) {
			e.printStackTrace();
			status = FedoraHandlerUtils.handleError(e);
		}
		return status;
	}

	/**
	 * ATM this is the preferred way to get the koji client instance.
	 * When in testing mode, this will return the stubbed client.
	 * 
	 * @return The real or the stubbed koji client depending on the
	 *         mode (inTestingMode)
	 */
	public IKojiHubClient getKoji() {
		if (!inTestingMode) {
			// return actual client
			return koji;
		} else {
			// return stub for testing
			return kojiStub;
		}
	}

	/**
	 * Set the koji client. Pass in hint in order to be able to use different
	 * client depending on actual cmdId.
	 * 
	 * @param cmdId Command ID which triggered this handler. This is useful
	 *              for Koji client implementations for different authentication
	 *              schemes. E.g. username and password authentication client.
	 *              We only need to override this method if there's more than one
	 *              authentication scheme supported.
	 */
	public void setKojiClient(String cmdId) {
		this.koji = new KojiHubClient();
	}

	protected boolean isScratch() {
		return false;
	}

	/**
	 * @param event
	 * @return the shell
	 * @throws ExecutionException
	 */
	private Shell getShell(ExecutionEvent event) throws ExecutionException {
		return HandlerUtil.getActiveShellChecked(event);
	}
}
