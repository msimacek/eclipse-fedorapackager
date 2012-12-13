package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.fedoraproject.eclipse.packager.koji.KojiPlugin;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.internal.ui.KojiServerDialog;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * Koji preference page for advanced settings.
 * 
 */
public class FedoraPackagerAdvancedKojiDialogPage extends DialogPage {

	private Table instanceTable;
	private Button addButton;
	private Button removeButton;
	private Button editButton;
	private Map<String, String[]> pendingServers = new HashMap<>();
	private Composite contents;
	// buffer for unpushed server changes done in style of preference String
	private String listPreferenceBuffer;

	/**
	 * Default constructor.
	 */
	public FedoraPackagerAdvancedKojiDialogPage() {
		super();
		listPreferenceBuffer = KojiPlugin.getDefault().getPreferenceStore()
				.getString(KojiPreferencesConstants.PREF_SERVER_LIST);

	}

	@Override
	public void createControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		instanceTable = new Table(contents, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd.heightHint = 200;
		gd.widthHint = 300;
		instanceTable.setLayoutData(gd);
		Composite buttons = new Composite(contents, SWT.NONE);
		buttons.setLayout(new GridLayout());
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		addButton = new Button(buttons, SWT.NONE);
		addButton.setText(KojiText.FedoraPackagerAdvancedKojiDialogPage_AddButton);
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
		editButton.setText(KojiText.FedoraPackagerAdvancedKojiDialogPage_EditButton);
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
		removeButton.setText(KojiText.FedoraPackagerAdvancedKojiDialogPage_RemoveButton);
		removeButton
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		removeButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
					TableItem toRemove = instanceTable.getSelection()[0];
					String name = toRemove.getText();
					String[] info = pendingServers.get(name);
					listPreferenceBuffer = listPreferenceBuffer.replace(NLS
							.bind(KojiText.ServerEntryTemplate, new String[] {
									name, info[0], info[1] }), ""); //$NON-NLS-1$
					pendingServers.remove(toRemove.getText(0));
					toRemove.dispose();
					if (instanceTable.getItems().length <= 1) {
						removeButton.setEnabled(false);
					}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no op
			}

		});
		@SuppressWarnings("unused")
		TableColumn column = new TableColumn(instanceTable, SWT.NONE);
		boolean warningShown = false;
		for (String serverInfoSet : KojiPlugin.getDefault()
				.getPreferenceStore()
				.getString(KojiPreferencesConstants.PREF_SERVER_LIST)
				.split(";")) { //$NON-NLS-1$
			String[] serverInfo = serverInfoSet.split(","); //$NON-NLS-1$
			if (!addServerItem(serverInfo) && !warningShown) {
				FedoraHandlerUtils
						.showErrorDialog(
								parent.getShell(),
								KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningTitle,
								KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningMsg);
			}
		}
		instanceTable.getColumn(0).pack();
		if (instanceTable.getItems().length <= 1) {
			removeButton.setEnabled(false);
		}
		buttons.pack();
	}

	private void addInstance() {
		contents.getShell().setEnabled(false);
		String[] newInstance = new KojiServerDialog(contents.getShell(), null,
				KojiText.FedoraPackagerAdvancedKojiDialogPage_serverDialogTitle)
				.open();
		contents.getShell().setEnabled(true);
		if (newInstance != null) {
			if (newInstance[0].contentEquals(KojiText.FedoraPackagerKojiPreferencePage_DefaultPlaceholder)) {
				FedoraHandlerUtils
				.showErrorDialog(
						contents.getShell(),
						KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningTitle,
						KojiText.FedoraPackagerAdvancedKojiDialogPage_placeholderWarningMsg);
			} else if (!addServerItem(newInstance)) {
				FedoraHandlerUtils
						.showErrorDialog(
								contents.getShell(),
								KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningTitle,
								KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningMsg);
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
		if (info != null && !(info.length < 3)) {
			contents.getShell().setEnabled(false);
			String[] newInstance = new KojiServerDialog(contents.getShell(),
					new String[] { name, info[0], info[1], info[2] },
					KojiText.FedoraPackagerAdvancedKojiDialogPage_editDialogTitle)
					.open();
			contents.getShell().setEnabled(true);
			if (newInstance != null) {
				// allow redundant keys if name is unchanged
				if (pendingServers.keySet().contains(newInstance[0])
						&& !name.contentEquals(newInstance[0])) {
					FedoraHandlerUtils
							.showErrorDialog(
									contents.getShell(),
									KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningTitle,
									KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningMsg);
				} else if (name.contentEquals(KojiText.FedoraPackagerKojiPreferencePage_DefaultPlaceholder)) {
					FedoraHandlerUtils
					.showErrorDialog(
							contents.getShell(),
							KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningTitle,
							KojiText.FedoraPackagerAdvancedKojiDialogPage_placeholderWarningMsg);
				} else {
					// replace existing item
					pendingServers.remove(name);
					pendingServers.put(newInstance[0], new String[] {
							newInstance[1], newInstance[2], newInstance[3] });
					for (TableItem item : instanceTable.getItems()) {
						if (item.getText().contentEquals(name)) {
							item.setText(newInstance[0]);
						}
					}
					listPreferenceBuffer = listPreferenceBuffer.replace(NLS
							.bind(KojiText.ServerEntryTemplate, new String[] {
									name, info[0], info[1], info[2] }), NLS.bind(
							KojiText.ServerEntryTemplate, newInstance));
				}
			}
		}
	}

	/**
	 * Add an item to the list of servers.
	 * 
	 * @param serverInfo
	 *            Array containing Strings for server name, and both server URLs
	 * @return true if successfully added, false otherwise.
	 */
	private boolean addServerItem(String[] serverInfo) {
		if (!pendingServers.containsKey(serverInfo[0])) {
			pendingServers.put(serverInfo[0], new String[] { serverInfo[1],
					serverInfo[2], serverInfo[3] });
			TableItem serverItem = new TableItem(instanceTable, SWT.NONE);
			serverItem.setText(0, serverInfo[0]);
			if (instanceTable.getItems().length > 1) {
				removeButton.setEnabled(true);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Apply changes made to server info.
	 */
	public void applyChanges() {
		KojiPlugin
				.getDefault()
				.getPreferenceStore()
				.setValue(KojiPreferencesConstants.PREF_SERVER_LIST,
						listPreferenceBuffer);
	}
}