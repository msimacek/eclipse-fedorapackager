package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.fedoraproject.eclipse.packager.FedoraPackagerPreferencesConstants;
import org.fedoraproject.eclipse.packager.PackagerPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;

/**
 * Project property page for Koji-related Fedora Packager settings.
 *
 */
public class KojiProjectPropertyPage extends PropertyPage {

	private static final String KOJI_PREFERENCE_ID = "org.fedoraproject.eclipse.packager.preferences.koji"; //$NON-NLS-1$
	private static final String linkTags = "<a>{0}</a>"; //$NON-NLS-1$

	private Combo serverCombo;
	private IProject project;
	private String[][] serverMapping;
	private Button useCustomTargetsCheck;
	private Link lnWorkspaceSettings;
	private Button btnProjectSettings;
	private GridData gridData;
	private GridLayout gridLayout;
	private Group optionsGroup;
	private Label description;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		if (getElement() instanceof IResource) {
			project = ((IResource) getElement()).getProject();
		} else {
			Object adapter = getElement().getAdapter(IResource.class);
			if (adapter instanceof IResource) {
				project = ((IResource) adapter).getProject();
			}
		}
		setPreferenceStore(new ScopedPreferenceStore(new ProjectScope(project),
				KojiPlugin.PLUGIN_ID));
		noDefaultAndApplyButton();
		Composite composite = new Composite(parent, SWT.NONE);

		gridLayout = new GridLayout(2, false);
		composite.setLayout(gridLayout);

		btnProjectSettings = new Button(composite, SWT.CHECK);
		btnProjectSettings.setText(KojiText.KojiProjectPropertyPage_ProjectSettings);
		btnProjectSettings.setFont(parent.getFont());
		btnProjectSettings.setSelection(getPreferenceStore().getBoolean(
				KojiPreferencesConstants.PREF_PROJECT_SETTINGS));
		btnProjectSettings.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				toggleEnabled();
			}
		});
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		btnProjectSettings.setLayoutData(gridData);

		lnWorkspaceSettings = new Link(composite, SWT.NONE);
		lnWorkspaceSettings.setText(NLS.bind(linkTags, KojiText.KojiProjectPropertyPage_WorkspaceSettings));
		lnWorkspaceSettings.setFont(parent.getFont());
		lnWorkspaceSettings.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(getShell(), KOJI_PREFERENCE_ID, new String[] {KOJI_PREFERENCE_ID}, null);
				preferenceDialog.open();
				updateComboBoxContents();
			}
		});

		optionsGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		optionsGroup.setText(KojiText.KojiProjectPropertyPage_optionsGroup);
		gridData = new GridData();
		gridLayout = new GridLayout(2, false);
		gridData.horizontalSpan = 2;
		gridData.verticalIndent = 5;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		optionsGroup.setLayoutData(gridData);
		optionsGroup.setLayout(gridLayout);

		description = new Label(optionsGroup, SWT.LEFT);
		description.setText(KojiText.KojiProjectPropertyPage_ProjectKojiSelect);
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		description.setLayoutData(gridData);

		serverCombo = new Combo(optionsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		updateComboBoxContents();

		useCustomTargetsCheck = new Button(optionsGroup, SWT.CHECK);
		useCustomTargetsCheck.setText(KojiText.KojiProjectPropertyPage_forceCustomTitle);
		useCustomTargetsCheck.setSelection(Boolean.parseBoolean(getPreferenceStore().getString(
				KojiPreferencesConstants.PREF_FORCE_CUSTOM_BUILD)));

		toggleEnabled();

		return composite;
	}

	/**
	 * Update the combo box in case the user decides to add a
	 * new koji server in the preference page.
	 *
	 */
	private void updateComboBoxContents() {
		serverMapping = KojiUtils.loadServerInfo(new ScopedPreferenceStore(
				InstanceScope.INSTANCE, KojiPlugin.PLUGIN_ID));
		if (serverCombo != null) {
			serverCombo.removeAll();
			serverCombo.setItems(serverMapping[0]);
			serverCombo.add(KojiText.FedoraPackagerKojiPreferencePage_DefaultPlaceholder);
			int selectionAddress = KojiUtils.getSelectionAddress(serverMapping, getPreferenceStore().getString(
					KojiPreferencesConstants.PREF_KOJI_SERVER_INFO));
			if (selectionAddress > 0) {
				serverCombo.select(selectionAddress);
			} else {
				serverCombo.select(serverCombo.getItemCount() - 1);
			}
		}
	}

	/**
	 * If "Enable project specific settings" is true, so will
	 * the options below it. The workspace settings link will be opposite
	 * to what value the checkbox is.
	 *
	 */
	private void toggleEnabled() {
		boolean enabled = btnProjectSettings.getSelection();
		description.setEnabled(enabled);
		serverCombo.setEnabled(enabled);
		useCustomTargetsCheck.setEnabled(enabled);
		lnWorkspaceSettings.setEnabled(!enabled);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		// use the server info from the preference store if .conf is being used
		if (PackagerPlugin.isConfEnabled()) {
			String prefServerInfo = PackagerPlugin.getStringPreference(FedoraPackagerPreferencesConstants.PREF_KOJI_SERVER_INFO);
			getPreferenceStore().setValue(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, prefServerInfo);
		// else use the selected server
		} else {
			int selection = serverCombo.getSelectionIndex();
			if (selection >= serverMapping[1].length) {
				getPreferenceStore()
						.setValue(
								KojiPreferencesConstants.PREF_KOJI_SERVER_INFO,
								KojiText.FedoraPackagerKojiPreferencePage_DefaultPlaceholder);
			} else if (selection != -1) {
				String newVal = serverMapping[1][serverCombo.getSelectionIndex()];
				getPreferenceStore().setValue(
						KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, newVal);
			}
		}
		getPreferenceStore().setValue(
				KojiPreferencesConstants.PREF_FORCE_CUSTOM_BUILD,
				Boolean.toString(useCustomTargetsCheck.getSelection()));
		getPreferenceStore().setValue(
				KojiPreferencesConstants.PREF_PROJECT_SETTINGS, btnProjectSettings.getSelection());
		return true;
	}
}
