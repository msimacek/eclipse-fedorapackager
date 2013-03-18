/*******************************************************************************
 * Copyright (c) 2010-2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.utils;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.PackagerPlugin;

/**
 * Utility class for UI related items
 * 
 */
public class UiUtils {

	/**
	 * This preference key denotes a preference that says whether
	 * a perspective should be switched after a successful project clone.
	 * 
	 * @see #openPerspective(Shell,String)
	 */
	public static String afterProjectClonePerspectiveSwitch = "switchPerspectiveAfterImport"; //$NON-NLS-1$
	
	/**
	 * This preference key denotes a preference that says whether
	 * a perspective should be switched after a successful project creation.
	 * 
	 * @see #openPerspective(Shell,String)
	 */
	public static String afterLocalProjectCreationPerspectiveSwitch = "switchPerspectiveAfterProjectCreation"; //$NON-NLS-1$
	
	/**
	 * Opens Fedora Packaging specific perspective dependening on the key param.
	 * if key denotes a preference that is MessageDialogWithToggle.ALWAYS, then the perspective will be open without
	 * showing the dialog.
	 * if key denotes a preference that is MessageDialogWithToggle.NEVER, then the perspective will never will open,
	 * and the dialog will not be presented.
	 * In all other cases, a dialog will ask the user whether the perspective should be open. 
	 * The choice may be made permanent by the user.
	 * 
	 * @param shell
	 *            The shell the perspective is to be opened in.
	 * @param key
	 * 			  A key of the preference in which user preference may be stored.
	 * @throws WorkbenchException
	 *             If the active workbench could not be accessed.
	 */
	public static void openPerspective(Shell shell, String key) throws WorkbenchException {
		// Finally ask if the Fedora Packaging perspective should be opened
		// if not already open.
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IPerspectiveDescriptor perspective = window.getActivePage()
				.getPerspective();
		if (perspective.getId().equals(
				PackagerPlugin.FEDORA_PACKAGING_PERSPECTIVE_ID)) {
			//perspective is already opened, exit;
			return;
		}
		
		if (key != null) {
			String preferenceValue = PackagerPlugin.getDefault()
					.getPreferenceStore().getString(key);

			if (MessageDialogWithToggle.NEVER.equals(preferenceValue)) {
				// perspective should be never changed
				return;
			}

			if (MessageDialogWithToggle.ALWAYS.equals(preferenceValue)) {
				workbench.showPerspective(
						PackagerPlugin.FEDORA_PACKAGING_PERSPECTIVE_ID, window);
				return;
			}
		}
		
		// Ask if Fedora Packager perspective should be opened.
		MessageDialogWithToggle openYesNoQuestion = MessageDialogWithToggle
				.openYesNoQuestion(
						shell,
						FedoraPackagerText.UiUtils_switchPerspectiveQuestionTitle,
						FedoraPackagerText.UiUtils_switchPerspectiveQuestionMsg,
						FedoraPackagerText.UiUtils_RememberChoice, false,
						PackagerPlugin.getDefault().getPreferenceStore(),
						afterProjectClonePerspectiveSwitch);

		if (IDialogConstants.YES_ID == openYesNoQuestion.getReturnCode()) {
			// open the perspective
			workbench.showPerspective(
					PackagerPlugin.FEDORA_PACKAGING_PERSPECTIVE_ID, window);
		}
	}
}
