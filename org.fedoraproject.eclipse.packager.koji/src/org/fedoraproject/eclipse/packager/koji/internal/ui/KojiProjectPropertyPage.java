package org.fedoraproject.eclipse.packager.koji.internal.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.KojiUtils;

/**
 * Project property page for Koji-related Fedora Packager settings.
 * 
 */
public class KojiProjectPropertyPage extends PropertyPage {

	private Combo serverCombo;
	private IProject project;
	private String[][] serverMapping;

	@Override
	protected Control createContents(Composite parent) {
		project = (IProject) getElement().getAdapter(IProject.class);
		setPreferenceStore(new ScopedPreferenceStore(new ProjectScope(project),
				KojiPlugin.getDefault().getBundle().getSymbolicName()));
		noDefaultAndApplyButton();
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		// project settings label
		Label description = new Label(composite, SWT.LEFT);
		description.setText(KojiText.KojiProjectPropertyPage_ProjectKojiSelect);
		description.pack();

		serverMapping = KojiUtils.loadServerInfo(KojiPlugin.getDefault()
				.getPreferenceStore());
		// project settings drop-down window
		serverCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		serverCombo.setItems(serverMapping[0]);
		serverCombo
				.add(KojiText.FedoraPackagerKojiPreferencePage_DefaultPlaceholder);
		int selectionAddress = KojiUtils.getSelectionAddress(
				serverMapping,
				getPreferenceStore().getString(
						KojiPreferencesConstants.PREF_KOJI_SERVER_INFO));
		if (selectionAddress > 0) {
			serverCombo.select(selectionAddress);
		} else {
			serverCombo.select(serverCombo.getItemCount() - 1);
		}
		serverCombo.pack();
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
