package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;

/**
 * Main preference page for Koji preferences.
 * 
 */
public class FedoraPackagerKojiPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private String[][] serverMapping;
	private Combo serverCombo;

	/**
	 * default constructor
	 */
	public FedoraPackagerKojiPreferencePage() {
		super();
		setPreferenceStore(KojiPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
		// no op
	}

	private void doAdvancedOptions() {
		Dialog advDialog = new Dialog(getShell()) {

			private FedoraPackagerAdvancedKojiDialogPage page;

			@Override
			public Control createContents(Composite parent) {
				page = new FedoraPackagerAdvancedKojiDialogPage();
				page.createControl(parent);
				super.createContents(parent);
				return parent;
			}

			@Override
			protected void okPressed() {
				page.applyChanges();
				super.okPressed();
			}
		};
		advDialog.open();
		serverMapping = KojiUtils.loadServerInfo(getPreferenceStore());
		serverCombo.setItems(serverMapping[0]);
		serverCombo.select(KojiUtils.getSelectionAddress(
				serverMapping,
				getPreferenceStore().getString(
						KojiPreferencesConstants.PREF_KOJI_SERVER_INFO)));
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		// default settings label
		Label description = new Label(composite, SWT.LEFT);
		description
				.setText(KojiText.FedoraPackagerKojiPreferencePage_KojiSelect);
		description.pack();

		serverMapping = KojiUtils.loadServerInfo(getPreferenceStore());
		// default settings drop-down window
		serverCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		serverCombo.setItems(serverMapping[0]);
		serverCombo.select(KojiUtils.getSelectionAddress(
				serverMapping,
				getPreferenceStore().getString(
						KojiPreferencesConstants.PREF_KOJI_SERVER_INFO)));
		serverCombo.pack();
		// button to launch Koji instance editor
		Button advancedButton = new Button(composite, SWT.NONE);
		advancedButton
				.setText(KojiText.FedoraPackagerKojiPreferencePage_AdvancedButtonText);
		advancedButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdvancedOptions();
			}
		});
		advancedButton.pack();
		return composite;
	}

	@Override
	public boolean performOk() {
		int selection = serverCombo.getSelectionIndex();
		if (selection != -1) {
			getPreferenceStore().setValue(
					KojiPreferencesConstants.PREF_KOJI_SERVER_INFO,
					serverMapping[1][serverCombo.getSelectionIndex()]);
		}
		return true;
	}
}
