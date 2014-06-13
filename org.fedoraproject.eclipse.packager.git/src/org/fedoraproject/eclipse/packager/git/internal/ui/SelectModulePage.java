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
package org.fedoraproject.eclipse.packager.git.internal.ui;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.fedoraproject.eclipse.packager.FedoraSSL;
import org.fedoraproject.eclipse.packager.git.FedoraPackagerGitText;

/**
 * Page for selecting the module to clone.
 *
 */
public class SelectModulePage extends WizardPage {

	private Text projectText;
	private Button anonymousCloneBtn;
	private String fasUser;

	private WorkingSetGroup workingSetGroup;
	private IStructuredSelection selection;

	private static final int GROUP_SPAN = 2;

	/**
	 * @param fasUser
	 *            Either FedoraSSL.UNKNOWN_USER or the extracted FAS user name
	 * @param selection
	 *            the current object selection
	 */
	protected SelectModulePage(String fasUser, IStructuredSelection selection) {
		super(FedoraPackagerGitText.SelectModulePage_packageSelection);
		setTitle(FedoraPackagerGitText.SelectModulePage_packageSelection);
		setDescription(FedoraPackagerGitText.SelectModulePage_choosePackage); 
		this.setImageDescriptor(ImageDescriptor.createFromFile(getClass(),
				"/icons/wizban/newconnect_wizban.png")); //$NON-NLS-1$

		setPageComplete(false);
		this.fasUser = fasUser;
		this.selection = selection;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);

		// Package name controls
		Label label = new Label(composite, SWT.NONE);
		label.setText(FedoraPackagerGitText.SelectModulePage_packageName); 
		projectText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		projectText.setLayoutData(gd);
		projectText.setFocus();
		projectText.addModifyListener(new ModifyListener(){

			@Override
			public void modifyText(ModifyEvent e) {
				if (projectText.getText() == null || projectText.getText().equals("")){ //$NON-NLS-1$
					setPageComplete(false);
					setErrorMessage(null);
				} else if (projectText.getText().trim().equals("")){ //$NON-NLS-1$
					setPageComplete(false);
					setErrorMessage(FedoraPackagerGitText.SelectModulePage_badPackageName);
				} else {
					setPageComplete(true);
					setErrorMessage(null);
				}
			}
		});

		final boolean isUnknownUser = fasUser.equals(FedoraSSL.UNKNOWN_USER);
		
		// Options group
		Group optionsGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		optionsGroup.setText(FedoraPackagerGitText.SelectModulePage_optionsGroup);
		optionsGroup.setLayout(new GridLayout());
		// Clone anonymously button
		anonymousCloneBtn = new Button(optionsGroup, SWT.CHECK);
		anonymousCloneBtn.setText(FedoraPackagerGitText.SelectModulePage_anonymousCheckout);
		anonymousCloneBtn.setSelection(isUnknownUser);
		// disable checkbox if there is no choice of cloning non-anonymously
		anonymousCloneBtn.setEnabled(!isUnknownUser);
		anonymousCloneBtn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (anonymousCloneBtn.getSelection() && !isUnknownUser) {
					setMessage(
							FedoraPackagerGitText.SelectModulePage_userSelectedAnonymousCloneInfoMsg,
							IMessageProvider.INFORMATION);
				} else if (isUnknownUser) {
					setMessage(
							FedoraPackagerGitText.SelectModulePage_anonymousCloneInfoMsg,
							IMessageProvider.INFORMATION);
				} else {
					setMessage(
							NLS.bind(
									FedoraPackagerGitText.SelectModulePage_sshCloneInfoMsg,
									fasUser), IMessageProvider.INFORMATION);
				}
			}

		});
		GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
		.applyTo(optionsGroup);
		updateMargins(optionsGroup);

		// Working set controls
		String[] workingSetTypeIds = new String[] { "org.eclipse.ui.resourceWorkingSetPage" }; //$NON-NLS-1$
		workingSetGroup = new WorkingSetGroup(composite, selection, workingSetTypeIds);

		// Set info message indicating which kind of clone we are about
		// to perform
		if (isUnknownUser) {
			setMessage(FedoraPackagerGitText.SelectModulePage_anonymousCloneInfoMsg, IMessageProvider.INFORMATION);
		} else {
			setMessage(NLS.bind(FedoraPackagerGitText.SelectModulePage_sshCloneInfoMsg, fasUser), IMessageProvider.INFORMATION);
		}
		
		setControl(composite);
	}
	
	/**
	 * 
	 * @return {code true} if the user chose an anonymous clone, {code false} otherwise.
	 */
	public boolean getCloneAnonymousButtonChecked() {
		return anonymousCloneBtn.getSelection();
		
	}

	/**
	 * The names of the packages to clone.
	 * 
	 * @return a list of package names
	 */
	public String[] getPackageNames() {
		final String packages = projectText.getText();
		String[] names = packages.split(","); //$NON-NLS-1$
		return names;
	}

	/**
	 * Returns the working sets to which the new project should be added.
	 *
	 * @return the selected working sets to which the new project should be added
	 */
	public IWorkingSet[] getSelectedWorkingSets() {
		return workingSetGroup.getSelectedWorkingSets();
	}

	private static void updateMargins(Group group) {
		// make sure there is some room between the group border
		// and the controls in the group
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}
}
