package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ListDialog;
import org.fedoraproject.eclipse.packager.api.errors.InvalidProjectRootException;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.utils.FedoraPackagerUtils;

/**
 * Main preference page for Koji preferences.
 * 
 */
public class FedoraPackagerKojiPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private String[][] serverMapping;
	private Combo serverCombo;
	private Table projectTable;
	private int initSelection = -1;

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

	private void loadOptions() {
		String[] totalServerInfo = getPreferenceStore().getString(
				KojiPreferencesConstants.PREF_SERVER_LIST).split(";"); //$NON-NLS-1$
		serverMapping = new String[2][totalServerInfo.length];
		int i = 0;
		String currentInfo = getPreferenceStore().getString(
				KojiPreferencesConstants.PREF_KOJI_SERVER_INFO);
		for (String serverInfoSet : totalServerInfo) {
			String[] serverInfo = serverInfoSet.split(",", 2); //$NON-NLS-1$
			serverMapping[0][i] = serverInfo[0];
			serverMapping[1][i] = serverInfo[1];
			if (serverInfo[1].contentEquals(currentInfo)) {
				initSelection = i;
			}
			i++;
		}
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
		loadOptions();
		serverCombo.setItems(serverMapping[0]);
		serverCombo.select(initSelection);
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

		loadOptions();
		// default settings drop-down window
		serverCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		serverCombo.setItems(serverMapping[0]);
		serverCombo.select(initSelection);
		serverCombo.pack();
		// per-project settings label
		Label tableDescription = new Label(composite, SWT.LEFT);
		tableDescription
				.setText(KojiText.FedoraPackagerKojiPreferencePage_TableDescription);
		tableDescription.pack();
		// per-project settings table
		projectTable = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION);
		projectTable.setHeaderVisible(true);
		projectTable.setLayout(new GridLayout());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		projectTable.setLayoutData(data);
		TableColumn[] projectColumns = {
				new TableColumn(projectTable, SWT.NONE, 0),
				new TableColumn(projectTable, SWT.NONE, 1) };
		projectColumns[0]
				.setText(KojiText.FedoraPackagerKojiPreferencePage_ProjectColumnTitle);
		projectColumns[1]
				.setText(KojiText.FedoraPackagerKojiPreferencePage_InstanceColumnTitle);
		// load existing per-project settings
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects()) {
			try {
				FedoraPackagerUtils.getProjectRoot(project);

				TableItem projectItem = new TableItem(projectTable, SWT.NONE);
				projectItem.setText(0, project.getName());
				String kojiInfo = new ProjectScope(project)
						.getNode(
								KojiPlugin.getDefault().getBundle()
										.getSymbolicName())
						.get(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, ""); //$NON-NLS-1$
				if (kojiInfo.contentEquals("")) { //$NON-NLS-1$
					projectItem.setText(1, ""); //$NON-NLS-1$
				} else {
					int i;
					for (i = 0; i < serverMapping[1].length; i++) {
						if (serverMapping[1][i].contentEquals(kojiInfo)) {
							projectItem.setText(1, serverMapping[0][i]);
							break;
						}
					}
					if (i == serverMapping[1].length) {
						projectItem
								.setText(
										1,
										KojiText.FedoraPackagerKojiPreferencePage_CustomEntryTitle);
					}
				}
			} catch (InvalidProjectRootException e) {
				// skip this project
			}
		}
		projectTable.getColumn(0).pack();
		projectTable.getColumn(1).pack();
		// button to set per-project settings
		Button changeButton = new Button(composite, SWT.NONE);
		changeButton
				.setText(KojiText.FedoraPackagerKojiPreferencePage_ChangeInstanceLabel);
		changeButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FutureTask<String> promptTask = new FutureTask<String>(
						new Callable<String>() {
							@Override
							public String call() {
								ListDialog ld = new ListDialog(new Shell(
										Display.getDefault()));
								ld.setInput(serverMapping[0]);
								ld.setContentProvider(new ArrayContentProvider());
								ld.setLabelProvider(new LabelProvider());
								ld.open();
								return ld.getResult()[0].toString();
							}
						});
				Display.getDefault().syncExec(promptTask);
				try {
					projectTable.getSelection()[0].setText(1, promptTask.get());
				} catch (Exception e1) {
					// If this occurs we have a problem with creating windows
					// and thus can't inform user; just print stack trace.
					e1.printStackTrace();
				}
			}

		});
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
		for (int i = 0; i < projectTable.getItemCount(); i++) {
			String projectName = projectTable.getItem(i).getText(0);
			String projectInfo = projectTable.getItem(i).getText(1);
			if (!(projectInfo.contentEquals("") || projectInfo //$NON-NLS-1$
					.contentEquals(KojiText.FedoraPackagerKojiPreferencePage_CustomEntryTitle))) {
				for (int j = 0; j < serverMapping[1].length; j++) {
					if (serverMapping[0][j].contentEquals(projectInfo)) {
						new ProjectScope(ResourcesPlugin.getWorkspace()
								.getRoot().getProject(projectName)).getNode(
								KojiPlugin.getDefault().getBundle()
										.getSymbolicName()).put(
								KojiPreferencesConstants.PREF_KOJI_SERVER_INFO,
								serverMapping[1][j]);
						break;
					}
				}
			}
		}
		return true;
	}
}
