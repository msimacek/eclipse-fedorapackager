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
package org.fedoraproject.eclipse.packager.koji.internal.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.widgets.FormText;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiText;

/**
 * Message dialog showing the link to the koji page showing build info
 *
 */
public class KojiMessageDialog extends MessageDialog {
	private String taskNo;
	private String kojiWebUrl;

	/**
	 * Creates the message dialog with the given index.
	 * 
	 * @param parentShell
	 * @param dialogTitle
	 * @param dialogTitleImage
	 * @param dialogImageType
	 * @param dialogButtonLabels
	 * @param defaultIndex
	 * @param kojiWebURL 
	 * @param taskId 
	 */
	public KojiMessageDialog(Shell parentShell, String dialogTitle,
			Image dialogTitleImage, int dialogImageType,
			String[] dialogButtonLabels, int defaultIndex, String kojiWebURL, String taskId) {
		super(parentShell, dialogTitle, dialogTitleImage,
				NLS.bind(KojiText.KojiMessageDialog_buildNumberMsg, taskId),
				dialogImageType, dialogButtonLabels, defaultIndex);
		this.kojiWebUrl = kojiWebURL;
		this.taskNo = taskId;
	}

	@Override
	public Image getImage() {
		return KojiPlugin.getImageDescriptor("icons/koji.png") //$NON-NLS-1$
				.createImage();
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		FormText taskLink = new FormText(parent, SWT.NONE);
		final String url = kojiWebUrl + "/taskinfo?taskID=" //$NON-NLS-1$
				+ taskNo;
		taskLink.setText("<form><p>" +  //$NON-NLS-1$
				KojiText.KojiMessageDialog_buildResponseMsg + "</p><p>"+ url //$NON-NLS-1$
						+ "</p></form>", true, true); //$NON-NLS-1$
		taskLink.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				try {
					IWebBrowser browser = PlatformUI
							.getWorkbench()
							.getBrowserSupport()
							.createBrowser(
									IWorkbenchBrowserSupport.NAVIGATION_BAR
											| IWorkbenchBrowserSupport.LOCATION_BAR
											| IWorkbenchBrowserSupport.STATUS,
									"koji_task", null, null); //$NON-NLS-1$
					browser.openURL(new URL(url));
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		return taskLink;
	}
}