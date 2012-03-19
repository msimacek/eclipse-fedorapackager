package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
	private Button useCustomTargetsCheck;

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
		useCustomTargetsCheck = new Button(composite, SWT.CHECK);
		useCustomTargetsCheck
				.setText(KojiText.KojiProjectPropertyPage_forceCustomTitle);
		useCustomTargetsCheck.setSelection(Boolean
				.parseBoolean(getPreferenceStore().getString(
						KojiPreferencesConstants.PREF_FORCE_CUSTOM_BUILD)));
		useCustomTargetsCheck.pack();

		return composite;
	}

	@Override
	public boolean performOk() {
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
		getPreferenceStore().setValue(
				KojiPreferencesConstants.PREF_FORCE_CUSTOM_BUILD,
				Boolean.toString(useCustomTargetsCheck.getSelection()));
		return true;
	}
}
