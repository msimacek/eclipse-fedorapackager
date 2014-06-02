/*******************************************************************************
 * Copyright (c) 2010, 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.git.internal.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.git.Activator;
import org.fedoraproject.eclipse.packager.git.FedoraPackagerGitText;
import org.fedoraproject.eclipse.packager.git.GitPreferencesConstants;

/**
 * Add Git preferences to Fedora packager preferences.
 *
 */
public class FedoraPackagerGitPreferencePage extends
		FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	protected static final int GROUP_SPAN = 2;
	private StringFieldEditor gitCloneURLEditor;
	private StringFieldEditor gitCloneDir;

	// disabled when .conf is being used
	private Group gitGroup;

	/**
	 * default constructor
	 */
	public FedoraPackagerGitPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE,Activator.PLUGIN_ID));
		setDescription(FedoraPackagerGitText.FedoraPackagerGitPreferencePage_description);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			checkState();
		}
	}

	@Override
	public void checkState() {
		super.checkState();
		// base URL should end with "/"
		if (!gitCloneURLEditor.getStringValue().endsWith("/")) { //$NON-NLS-1$
			setErrorMessage(FedoraPackagerGitText.FedoraPackagerGitPreferencePage_invalidBaseURLMsg);
			setValid(false);
		} else {
			setErrorMessage(null);
			setValid(true);
		}
	}

	@Override
	public boolean isValid() {
		// disable modifying the URL if .conf is being used
		gitCloneURLEditor.setEnabled(!PackagerPlugin.isConfEnabled(), gitGroup);
		return super.isValid();
	}

	@Override
	public void createFieldEditors() {
		Composite composite = getFieldEditorParent();
		gitGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		gitGroup.setText(FedoraPackagerGitText.FedoraPackagerGitPreferencePage_gitGroupName);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(gitGroup);
		//GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1).applyTo(gitGroup);
		/* Preference for git clone base url */
		gitCloneURLEditor = new StringFieldEditor(
				GitPreferencesConstants.PREF_CLONE_BASE_URL,
				FedoraPackagerGitText.FedoraPackagerGitPreferencePage_cloneBaseURLLabel,
				gitGroup);
		gitCloneURLEditor.load();
		addField(gitCloneURLEditor);

		gitCloneDir = new StringButtonFieldEditor(GitPreferencesConstants.PREF_CLONE_DIR,
				FedoraPackagerGitText.FedoraPackagerGitPreferencePage_destination,
				gitGroup) {
			@Override
			protected String changePressed() {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText(getLabelText());
				return dialog.open();
			}
		};
		gitCloneDir.load();
		addField(gitCloneDir);
		updateMargins(gitGroup);
	}

	private static void updateMargins(Group group) {
		// make sure there is some room between the group border
		// and the controls in the group
		GridLayout layout = (GridLayout) group.getLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	}
}
