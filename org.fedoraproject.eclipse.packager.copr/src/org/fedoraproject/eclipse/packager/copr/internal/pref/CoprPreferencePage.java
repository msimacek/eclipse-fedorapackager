package org.fedoraproject.eclipse.packager.copr.internal.pref;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.fedoraproject.eclipse.packager.copr.CoprConfigurationConstants;
import org.fedoraproject.eclipse.packager.copr.CoprPlugin;
import org.fedoraproject.eclipse.packager.copr.CoprText;

/**
 * Global preferences for Copr plugin
 *
 * @author msimacek
 *
 */
public class CoprPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	private StringFieldEditor coprUrlEditor;
	private StringFieldEditor coprLoginEditor;
	private StringFieldEditor coprTokenEditor;

	// /**
	// * Default constructor
	// */
	// public CoprPreferencePage() {
	// super(GRID);
	// }

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE,
				CoprPlugin.PLUGIN_ID));
		setDescription(CoprText.CoprPreferencePage_Description);
	}

	@Override
	protected void createFieldEditors() {
		Composite composite = getFieldEditorParent();
		// Group mainGroup = new Group(composite, GRID);
		// mainGroup.setText("Global Copr plugin preferences");
		coprUrlEditor = new StringFieldEditor(
				CoprConfigurationConstants.COPR_URL,
				CoprText.CoprPreferencePage_URLField, composite);
		coprUrlEditor.load();
		coprUrlEditor.setPropertyChangeListener(this);
		addField(coprUrlEditor);
		coprLoginEditor = new StringFieldEditor(
				CoprConfigurationConstants.COPR_API_LOGIN,
				CoprText.CoprPreferencePage_LoginField, composite);
		coprLoginEditor.load();
		coprLoginEditor.setPropertyChangeListener(this);
		addField(coprLoginEditor);
		coprTokenEditor = new StringFieldEditor(
				CoprConfigurationConstants.COPR_API_TOKEN,
				CoprText.CoprPreferencePage_TokenField, composite);
		coprTokenEditor.load();
		coprTokenEditor.setPropertyChangeListener(this);
		addField(coprTokenEditor);
	}
}
