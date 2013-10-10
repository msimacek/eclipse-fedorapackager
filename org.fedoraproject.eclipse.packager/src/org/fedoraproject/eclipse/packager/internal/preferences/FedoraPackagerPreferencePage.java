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

package org.fedoraproject.eclipse.packager.internal.preferences;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.fedoraproject.eclipse.packager.FedoraPackagerLogger;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.FedoraPackagerText;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.utils.UiUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Eclipse Fedora Packager main preference page.
 *
 */
public class FedoraPackagerPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private static final int GROUP_SPAN = 2;
	private static final String HTTP_PREFIX = "http"; //$NON-NLS-1$
	private static final String DIR = "/"; //$NON-NLS-1$

	// Lookaside cache
	private StringFieldEditor lookasideUploadURLEditor;
	private StringFieldEditor lookasideDownloadURLEditor;
	private StringFieldEditor fedpkgConfigDirEditor;

	// FedoraPackager configuration file
	private FedoraPackagerConfigPreference configPreferences;
	private BooleanFieldEditor fedpkgConfigEnabledSwitch;
	private Group fedpkgConfigGroup;
	private Group lookasideGroup;

	// The preferences to flush when OK is pressed
	private Preferences preferencesToSave;

	/** Default constructor */
	public FedoraPackagerPreferencePage() {
		super(GRID);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(PackagerPlugin.getDefault().getPreferenceStore());
		setDescription(FedoraPackagerText.FedoraPackagerPreferencePage_description);
	}

	/**
	 * Validate fields for sane values.
	 *
	 */
	@Override
	public void checkState() {
		super.checkState();
		// Upload URL has to be https
		if (lookasideUploadURLEditor.getStringValue() != null
				&& !lookasideUploadURLEditor.getStringValue().startsWith(HTTP_PREFIX)) {
			setErrorMessage(FedoraPackagerText.FedoraPackagerPreferencePage_invalidUploadURLMsg);
			setValid(false);
		} else if (lookasideDownloadURLEditor.getStringValue() != null
				&& !lookasideDownloadURLEditor.getStringValue().startsWith(HTTP_PREFIX)) {
			setErrorMessage(FedoraPackagerText.FedoraPackagerPreferencePage_invalidDownloadURLMsg);
			setValid(false);
		} else if (fedpkgConfigDirEditor.getStringValue() != null
				&& fedpkgConfigDirEditor.getStringValue().endsWith(DIR)) {
			setErrorMessage(FedoraPackagerText.FedoraPackagerPreferencePage_invalidConfigurationFile);
			setValid(false);
		} else if (fedpkgConfigDirEditor.getStringValue() != null
				&& !new File(fedpkgConfigDirEditor.getStringValue()).exists()) {
			setErrorMessage(FedoraPackagerText.FedoraPackagerPreferencePage_invalidConfigurationFileMissing);
			setValid(false);
		} else {
			// use .conf file = enable to change location of .conf file
			fedpkgConfigDirEditor.setEnabled(
					fedpkgConfigEnabledSwitch.getBooleanValue(),
					fedpkgConfigGroup);
			// use .conf file = disable changing the settings of lookaside cache
			lookasideUploadURLEditor.setEnabled(
					!fedpkgConfigEnabledSwitch.getBooleanValue(),
					lookasideGroup);
			lookasideDownloadURLEditor.setEnabled(
					!fedpkgConfigEnabledSwitch.getBooleanValue(),
					lookasideGroup);
			setErrorMessage(null);
			setValid(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			checkState();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		Composite composite = getFieldEditorParent();
		// General prefs
		Group generalGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).span(GROUP_SPAN, 1).applyTo(generalGroup);
		generalGroup.setText(FedoraPackagerText.FedoraPackagerPreferencePage_generalGroupName);
		ComboFieldEditor clone = new ComboFieldEditor(
				UiUtils.afterProjectClonePerspectiveSwitch,
				FedoraPackagerText.FedoraPackagerPreferencePage_switchPerspectiveAfterProjectCheckout,
				new String[][] {
						// note - the first one will be also displayed if the preference is missing
						new String[] {
								FedoraPackagerText.FedoraPackagerPreferencePage_Ask,
								"Ask" },//$NON-NLS-1$
						new String[] {
								FedoraPackagerText.FedoraPackagerPreferencePage_Always,
								MessageDialogWithToggle.ALWAYS },
						new String[] {
								FedoraPackagerText.FedoraPackagerPreferencePage_Never,
								MessageDialogWithToggle.NEVER } }, generalGroup);
		((GridData)clone.getLabelControl(generalGroup).getLayoutData()).horizontalAlignment = SWT.FILL;
		((GridData)clone.getLabelControl(generalGroup).getLayoutData()).grabExcessHorizontalSpace = true;
		addField(clone);
		ComboFieldEditor create = new ComboFieldEditor(
				UiUtils.afterLocalProjectCreationPerspectiveSwitch,
				FedoraPackagerText.FedoraPackagerPreferencePage_switchPerspectiveAfterLocalProjectCreation,
				new String[][] {
						// note - the first one will be also displayed if the preference is missing
						new String[] {
								FedoraPackagerText.FedoraPackagerPreferencePage_Ask,
								"Ask" },//$NON-NLS-1$
						new String[] {
								FedoraPackagerText.FedoraPackagerPreferencePage_Always,
								MessageDialogWithToggle.ALWAYS },
						new String[] {
								FedoraPackagerText.FedoraPackagerPreferencePage_Never,
								MessageDialogWithToggle.NEVER }, },
				generalGroup);
		((GridData)create.getLabelControl(generalGroup).getLayoutData()).horizontalAlignment = SWT.FILL;
		((GridData)create.getLabelControl(generalGroup).getLayoutData()).grabExcessHorizontalSpace = true;
		addField(create);
		addField(new BooleanFieldEditor(
				FedoraPackagerPreferencesConstants.PREF_DEBUG_MODE,
				FedoraPackagerText.FedoraPackagerPreferencePage_debugSwitchLabel,
				generalGroup));
		fedpkgConfigEnabledSwitch = new BooleanFieldEditor(
				FedoraPackagerPreferencesConstants.PREF_FEDPKG_CONFIG_ENABLED,
				FedoraPackagerText.FedoraPackagerPreferencePage_fedpkgConfigSwitch,
				generalGroup);
		fedpkgConfigEnabledSwitch.load();
		fedpkgConfigEnabledSwitch.setPropertyChangeListener(this);
		addField(fedpkgConfigEnabledSwitch);
		updateMargins(generalGroup);
		((GridLayout) generalGroup.getLayout()).numColumns = 3;

		lookasideGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		lookasideGroup.setText(FedoraPackagerText.FedoraPackagerPreferencePage_lookasideGroupName);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(GROUP_SPAN, 1)
			.hint(10, SWT.DEFAULT).applyTo(lookasideGroup);
		lookasideUploadURLEditor = new StringFieldEditor(
				FedoraPackagerPreferencesConstants.PREF_LOOKASIDE_UPLOAD_URL,
				FedoraPackagerText.FedoraPackagerPreferencePage_lookasideUploadURLLabel,
				lookasideGroup);
		lookasideUploadURLEditor.load();
		lookasideUploadURLEditor.setPropertyChangeListener(this);
		addField(lookasideUploadURLEditor);
		lookasideDownloadURLEditor = new StringFieldEditor(
				FedoraPackagerPreferencesConstants.PREF_LOOKASIDE_DOWNLOAD_URL,
				FedoraPackagerText.FedoraPackagerPreferencePage_lookasideDownloadURLLabel,
				lookasideGroup);
		lookasideDownloadURLEditor.load();
		lookasideDownloadURLEditor.setPropertyChangeListener(this);
		addField(lookasideDownloadURLEditor);
		updateMargins(lookasideGroup);
		((GridLayout) lookasideGroup.getLayout()).numColumns = 3;


		fedpkgConfigGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		fedpkgConfigGroup.setLayout(new GridLayout(2, true));
		fedpkgConfigGroup
				.setText(FedoraPackagerText.FedoraPackagerPreferencePage_fedpkgConfigGroupName);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(GROUP_SPAN, 1)
			.hint(10, SWT.DEFAULT).applyTo(fedpkgConfigGroup);
		fedpkgConfigDirEditor = new StringButtonFieldEditor(
				FedoraPackagerPreferencesConstants.PREF_FEDPKG_CONFIG,
				FedoraPackagerText.FedoraPackagerPreferencePage_fedpkgConfigStringLabel,
				fedpkgConfigGroup) {
			@Override
			protected String changePressed() {
				FileDialog dialog = new FileDialog(getShell());
				dialog.setText(getLabelText());
				return dialog.open();
			}
		};
		fedpkgConfigDirEditor.setPropertyChangeListener(this);
		fedpkgConfigDirEditor.load();
		addField(fedpkgConfigDirEditor);
		updateMargins(fedpkgConfigGroup);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		super.performOk();
		FedoraPackagerLogger.getInstance().refreshConfig();
		if (getConfigPreference()) {
			try {
				preferencesToSave.flush();
			} catch (BackingStoreException e) {
				FedoraPackagerLogger.getInstance()
				.logError(FedoraPackagerText.FedoraPackagerConfigPreference_settingPreferenceError,
						e);
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	public void performApply() {
		// will not save the preferences unless OK is pressed
		// other pages will also not see the current .conf enabled status
		getConfigPreference();
	}

	/**
	 * Make sure there is some room between the group border
	 * and the controls in the group.
	 *
	 * @param group The group to update the margins of.
	 */
	private void updateMargins(Group group) {
		GridLayout layout = (GridLayout) group.getLayout();
		GridData data = (GridData) group.getLayoutData();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		data.verticalIndent = 20;
	}

	/**
	 * Get all the information settings from the configuration file if
	 * it exists.
	 *
	 */
	private boolean getConfigPreference() {
		if (fedpkgConfigEnabledSwitch.getBooleanValue()) {
			configPreferences = new FedoraPackagerConfigPreference(fedpkgConfigDirEditor.getStringValue());
			if (configPreferences != null) {
				preferencesToSave = configPreferences.getConfigPreference();
				return true;
			}
		}
		return false;
	}
}
