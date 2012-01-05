package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.internal.ui.KojiServerDialog;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * Fedora Packager for Eclipse Koji preference page.
 * 
 */
public class FedoraPackagerKojiPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Table instanceTable;
	private Button addButton;
	private Button removeButton;
	private Button editButton;
	private TableItem checkedItem;
	private Map<String, String[]> pendingServers = new HashMap<String, String[]>();
	private Composite contents;
	// buffer for unpushed server changes done in style of preference String
	private String listPreferenceBuffer;

	/**
	 * Default constructor.
	 */
	public FedoraPackagerKojiPreferencePage() {
		super();
		setPreferenceStore(KojiPlugin.getDefault().getPreferenceStore());
		listPreferenceBuffer = getPreferenceStore().getString(
				KojiPreferencesConstants.PREF_SERVER_LIST);

	}

	@Override
	public void init(IWorkbench workbench) {
		// no op
	}

	@Override
	protected Control createContents(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		Label description = new Label(parent, SWT.NONE);
		description.setData(new GridData(SWT.FILL, SWT.FILL, true, false));
		description.setText(KojiText.FedoraPackagerKojiPreferencePage_KojiPreferenceDescription);
		description.pack();
		contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		instanceTable = new Table(contents, SWT.SINGLE | SWT.CHECK | SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd.heightHint = 200;
		gd.widthHint = 300;
		instanceTable.setLayoutData(gd);
		@SuppressWarnings("unused")
		TableColumn column = new TableColumn(instanceTable, SWT.NONE);
		String currentInstance = getPreferenceStore().getString(
				KojiPreferencesConstants.PREF_KOJI_SERVER_NAME);
		boolean warningShown = false;
		for (String serverInfoSet : getPreferenceStore().getString(
				KojiPreferencesConstants.PREF_SERVER_LIST).split(";")) { //$NON-NLS-1$
			String[] serverInfo = serverInfoSet.split(","); //$NON-NLS-1$
			if (!addServerItem(serverInfo, currentInstance) && !warningShown) {
				FedoraHandlerUtils
						.showErrorDialog(
								parent.getShell(),
								KojiText.FedoraPackagerKojiPreferencePage_namespaceWarningTitle,
								KojiText.FedoraPackagerKojiPreferencePage_namespaceWarningMsg);
			}
		}
		instanceTable.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// emulate radio buttons with table check boxes
				if (e.detail == SWT.CHECK) {
					if (checkedItem == (TableItem) e.item){
						checkedItem.setChecked(true);
					} else if (checkedItem != null) {
						checkedItem.setChecked(false);
					}
					checkedItem = (TableItem) e.item;
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no op
			}
		});
		instanceTable.getColumn(0).pack();
		Composite buttons = new Composite(contents, SWT.NONE);
		buttons.setLayout(new GridLayout());
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		addButton = new Button(buttons, SWT.NONE);
		addButton.setText("Add..."); //$NON-NLS-1$
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		addButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				addInstance();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no op
			}

		});
		editButton = new Button(buttons, SWT.NONE);
		editButton.setText("Edit..."); //$NON-NLS-1$
		editButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		editButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				editInstance(instanceTable.getSelection()[0]);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no op
			}

		});
		removeButton = new Button(buttons, SWT.NONE);
		removeButton.setText("Remove"); //$NON-NLS-1$
		removeButton
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		removeButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem toRemove = instanceTable.getSelection()[0];
				if (checkedItem == toRemove) {
					checkedItem = null;
				}
				pendingServers.remove(toRemove.getText(0));
				toRemove.dispose();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no op
			}

		});
		buttons.pack();
		return parent;
	}

	private void addInstance() {
		String[] newInstance = new KojiServerDialog(contents.getShell(), null,
				KojiText.FedoraPackagerKojiPreferencePage_serverDialogTitle)
				.open();
		if (newInstance != null) {
			if (!addServerItem(newInstance, checkedItem.getText())) {
				FedoraHandlerUtils
						.showErrorDialog(
								contents.getShell(),
								KojiText.FedoraPackagerKojiPreferencePage_namespaceWarningTitle,
								KojiText.FedoraPackagerKojiPreferencePage_namespaceWarningMsg);
			} else {
				// add new server to unpushed preference
				listPreferenceBuffer = listPreferenceBuffer.concat(NLS.bind(
						KojiText.ServerEntryTemplate, newInstance));
			}
		}
	}

	private void editInstance(TableItem i) {
		String name = i.getText();
		String[] info = pendingServers.get(name);
		String[] newInstance = new KojiServerDialog(contents.getShell(),
				new String[] { name, info[0], info[1] }, "Add New Koji Server") //$NON-NLS-1$
				.open();
		if (newInstance != null) {
			//allow redundant keys if name is unchanged
			if (pendingServers.keySet().contains(newInstance[0]) && !name.contentEquals(newInstance[0])) {
				FedoraHandlerUtils
						.showErrorDialog(
								contents.getShell(),
								KojiText.FedoraPackagerKojiPreferencePage_namespaceWarningTitle,
								KojiText.FedoraPackagerKojiPreferencePage_namespaceWarningMsg);
			} else {
				//replace existing item
				pendingServers.remove(name);
				pendingServers.put(newInstance[0], new String[] {
						newInstance[1], newInstance[2] });
				for (TableItem item : instanceTable.getItems()) {
					if (item.getText().contentEquals(name)) {
						item.setText(newInstance[0]);
					}
				}
				listPreferenceBuffer = listPreferenceBuffer.replace(
						NLS.bind(KojiText.ServerEntryTemplate, new String[] {
								name, info[0], info[1] }),
						NLS.bind(KojiText.ServerEntryTemplate, newInstance));
			}
		}
	}

	@Override
	public void performDefaults() {
		String currentInstance = getPreferenceStore().getDefaultString(
				KojiPreferencesConstants.PREF_KOJI_SERVER_NAME);
		for (TableItem item : instanceTable.getItems()) {
			try {
				item.setChecked(item.getText().contentEquals(currentInstance));
				checkedItem = item.getText().contentEquals(currentInstance) ? item
						: checkedItem;
			} catch (Exception e) {
				// ignore, skip instance
			}
		}
	}
	/**
	 * Add an item to the list of servers.
	 * 
	 * @param serverInfo Array containing Strings for server name, and both server URLs
	 * @param currentInstance The name of the currently checked server.
	 * @return true if successfully added, false otherwise.
	 */
	private boolean addServerItem(String[] serverInfo, String currentInstance) {
		if (!pendingServers.containsKey(serverInfo[0])) {
			pendingServers.put(serverInfo[0], new String[] { serverInfo[1],
					serverInfo[2] });
			TableItem serverItem = new TableItem(instanceTable, SWT.NONE);
			serverItem.setText(0, serverInfo[0]);
			serverItem.setChecked(serverInfo[0].contentEquals(currentInstance));
			checkedItem = serverInfo[0].contentEquals(currentInstance) ? serverItem
					: checkedItem;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean performOk() {
		getPreferenceStore()
				.setValue(KojiPreferencesConstants.PREF_SERVER_LIST,
						listPreferenceBuffer);
		getPreferenceStore().setValue(
				KojiPreferencesConstants.PREF_KOJI_SERVER_NAME,
				checkedItem.getText());
		return true;
	}
}
