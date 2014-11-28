package org.fedoraproject.eclipse.packager.copr.internal.pref;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.fedoraproject.eclipse.packager.copr.CoprConfigurationConstants;
import org.fedoraproject.eclipse.packager.copr.CoprPlugin;
import org.fedoraproject.eclipse.packager.copr.CoprText;

/**
 * Per-project preferences for Copr plugin, storing Copr name and overrides for
 * global preferences
 *
 * @author msimacek
 *
 */
public class CoprProjectProperties extends PropertyPage {

	private Label description;
	private Label coprUserNameLabel;
	private Text coprUserNameField;
	private Label coprNameLabel;
	private Text coprNameField;

	@Override
	protected Control createContents(Composite parent) {
		IProject project = ((IResource) getElement()).getProject();
		IPreferenceStore prefs = new ScopedPreferenceStore(new ProjectScope(
				project), CoprPlugin.PLUGIN_ID);
		setPreferenceStore(prefs);

		noDefaultAndApplyButton();

		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);

		description = new Label(parent, SWT.NULL);
		description.setText(CoprText.CoprProjectProperties_Description);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		description.setLayoutData(gridData);
		coprUserNameLabel = new Label(parent, SWT.NULL);
		coprUserNameLabel.setText(CoprText.CoprProjectProperties_UsernameField);
		coprUserNameField = new Text(parent, SWT.SINGLE | SWT.BORDER);
		coprUserNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		coprUserNameField.setText(prefs.getString(CoprConfigurationConstants.COPR_USERNAME));
		coprNameLabel = new Label(parent, SWT.NULL);
		coprNameLabel.setText(CoprText.CoprProjectProperties_NameField);
		coprNameField = new Text(parent, SWT.SINGLE | SWT.BORDER);
		coprNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		coprNameField.setText(prefs.getString(CoprConfigurationConstants.COPR_NAME));
		return parent;
	}

	@Override
	public boolean performOk() {
		IPreferenceStore prefs = getPreferenceStore();
		prefs.setValue(CoprConfigurationConstants.COPR_USERNAME,
				coprUserNameField.getText());
		prefs.setValue(CoprConfigurationConstants.COPR_NAME,
				coprNameField.getText());
		return true;
	}

}
