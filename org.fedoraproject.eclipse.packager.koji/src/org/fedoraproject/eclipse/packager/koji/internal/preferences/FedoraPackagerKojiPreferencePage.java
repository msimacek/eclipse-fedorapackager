/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
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

	private Button addButton;
	private Button removeButton;
	private Button editButton;

	private Composite composite;
	private Table table;
	private String[][] serverMapping;

	private String backupPreference;
	private ScopedPreferenceStore prefStore;

	/**
	 * Constructor.
	 *
	 */
	public FedoraPackagerKojiPreferencePage() {
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, KojiPlugin.PLUGIN_ID));
		setDescription(KojiText.FedoraPackagerKojiPreferencePage_KojiPreferenceInformation);
		noDefaultAndApplyButton();
		prefStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, KojiPlugin.PLUGIN_ID);
		backupPreference = prefStore.getString(KojiPreferencesConstants.PREF_SERVER_LIST);
	}

	@Override
	public void init(IWorkbench workbench) {}

	/**
	 * Create a button to be added onto preference page.
	 *
	 * @param parent The parent composite to attach the created button to.
	 * @param buttonText The text to be put onto the button.
	 * @return The newly created button.
	 */
	private Button createPushButton(Composite parent, String buttonText) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(buttonText);
		button.setFont(parent.getFont());
		GridData gd = new GridData();
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		gd.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(gd);
		return button;
	}

	/**
	 * Create a column to be added onto the table viewer.
	 *
	 * @param tableViewer The table viewer to attached the created column to.
	 * @param columnText The name of the column.
	 * @return The newly created column.
	 */
	private TableViewerColumn createTableViewerColumn(TableViewer tableViewer, String columnText) {
		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
		column.getColumn().setWidth(200);
		column.getColumn().setText(columnText);
		return column;
	}

	/**
	 * Uncheck everything but the item passed in.
	 *
	 * @param item The item to leave checked.
	 */
	private void uncheckEverythingBut(TableItem item) {
		for (TableItem ti : table.getItems()) {
			if (!ti.equals(item)) {
				ti.setChecked(false);
			}
		}
	}

	/**
	 * Refresh the table by refreshing the table with the saved server info.
	 *
	 */
	private void refreshTableItems() {
		if (table != null) {
			serverMapping = KojiUtils.loadServerInfo(getPreferenceStore());
			table.removeAll();
			for (int i = 0; i < serverMapping[0].length; i++) {
				String[] properties = serverMapping[1][i].split(","); //$NON-NLS-1$
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, serverMapping[0][i]);
				item.setText(1, properties[0]);
				item.setText(2, properties[1]);
				item.setText(3, properties[2]);
			}
		}
	}

	/**
	 * Set the table's selection and also set its check status to true.
	 *
	 * @param index The index to set the selection to.
	 */
	private void changeSelection(int index) {
		if (index >= 0 && table != null) {
			TableItem tableItem = table.getItem(index);
			table.setSelection(index);
			if (tableItem != null) {
				tableItem.setChecked(true);
			}
		}
	}

	/**
	 * Apply changed to the preference store.
	 *
	 * @param preferences The preferences to apply.
	 */
	public void applyChanges(String preferences) {
		prefStore.setValue(KojiPreferencesConstants.PREF_SERVER_LIST, preferences);
	}

	@Override
	protected Control createContents(Composite parent) {
		composite = new Composite(parent, SWT.NULL);
		FedoraPackagerKojiPreferencePageUtils fptc = new FedoraPackagerKojiPreferencePageUtils(composite, prefStore);
		ButtonAdapter buttonAdapter = new ButtonAdapter(fptc);

		GridData gridData;
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginHeight = 10;
		composite.setLayout(gridLayout);

		Label tableTitle = new Label(composite, SWT.NONE);
		tableTitle.setText(KojiText.FedoraPackagerKojiPreferencePage_KojiServers);
		tableTitle.setFont(parent.getFont());
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		tableTitle.setLayoutData(gridData);

		TableViewer tableViewer = new TableViewer(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setFont(parent.getFont());
		table.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem tableItem = (TableItem) e.item;
				uncheckEverythingBut(tableItem);
				tableItem.setChecked(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		// Create the table columns
		createTableViewerColumn(tableViewer, KojiText.FedoraPackagerKojiPreferencePage_ColumnName);
		createTableViewerColumn(tableViewer, KojiText.FedoraPackagerKojiPreferencePage_ColumnWebURL);
		createTableViewerColumn(tableViewer, KojiText.FedoraPackagerKojiPreferencePage_ColumnXMLRPCURL);
		createTableViewerColumn(tableViewer, KojiText.FedoraPackagerKojiPreferencePage_ColumnCustomBuildTargets);

		// Make table expand to fill empty space
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		tableViewer.getControl().setLayoutData(gridData);

		// Create a list of buttons
		Composite buttonList = new Composite(composite, SWT.NONE);
		gridLayout = new GridLayout();
		gridData = new GridData();
		gridData.verticalAlignment = GridData.BEGINNING;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		buttonList.setLayout(gridLayout);
		buttonList.setLayoutData(gridData);

		// Create the buttons and add listeners
		addButton = createPushButton(buttonList, KojiText.FedoraPackagerKojiPreferencePage_ButtonAdd);
		addButton.addSelectionListener(buttonAdapter);

		editButton = createPushButton(buttonList, KojiText.FedoraPackagerKojiPreferencePage_ButtonEdit);
		editButton.addSelectionListener(buttonAdapter);

		removeButton = createPushButton(buttonList, KojiText.FedoraPackagerKojiPreferencePage_ButtonRemove);
		removeButton.addSelectionListener(buttonAdapter);

		refreshTableItems();
		int currentSelection = KojiUtils.getSelectionAddress(serverMapping, getPreferenceStore().getString(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO));
		changeSelection(currentSelection);
		return composite;
	}

	@Override
	public boolean performOk() {
		int selection = table.getSelectionIndex();
		if (selection != -1 && table.getItem(selection).getChecked()) {
			getPreferenceStore().setValue(KojiPreferencesConstants.PREF_KOJI_SERVER_INFO, serverMapping[1][selection]);
			return true;
		} else if (selection == -1 || !table.getItem(selection).getChecked() ) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openInformation(getShell(), "Default Server", "Please choose a default server"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
		}
		return false;
	}

	@Override
	public boolean performCancel() {
		String preferences = prefStore.getString(KojiPreferencesConstants.PREF_SERVER_LIST);
		if (!backupPreference.equals(preferences)) {
			applyChanges(backupPreference);
		}
		return true;
	}

	/**
	 * Simple class to handle the button press of Add, Edit, and Remove.
	 *
	 */
	public class ButtonAdapter extends SelectionAdapter {
		FedoraPackagerKojiPreferencePageUtils fptc;

		/**
		 * Constructor.
		 *
		 * @param fptc FedoraPackager handling the adding, editing, and removing.
		 */
		public ButtonAdapter(FedoraPackagerKojiPreferencePageUtils fptc) {
			this.fptc = fptc;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			Widget widget = e.widget;
			String preferences = ""; //$NON-NLS-1$
			int currentSelection = table.getSelectionIndex();
			if (widget == addButton) {
				preferences = fptc.addInstance();
				if (table.getItems().length > 1) {
					removeButton.setEnabled(true);
				}
				// currentSelection is the new tableItem added
				if (!preferences.isEmpty()) {
					currentSelection = table.getItemCount();
				}
			} else if (widget == editButton && table.getSelectionIndex() != -1) {
				preferences = fptc.editInstance(table);
			} else if (widget == removeButton && table.getSelectionIndex() != -1) {
				preferences = fptc.removeInstance(table);
				if (table.getItems().length <= 1) {
					removeButton.setEnabled(false);
				}
				// change currentSelection to last item so removing can be faster
				currentSelection = table.getItemCount()-1;
			}
			if (!preferences.isEmpty()) {
				applyChanges(preferences);
			}
			refreshTableItems();
			if (currentSelection != -1 && table.getItem(currentSelection) != null) {
				changeSelection(currentSelection);
			}
		}
	}
}
