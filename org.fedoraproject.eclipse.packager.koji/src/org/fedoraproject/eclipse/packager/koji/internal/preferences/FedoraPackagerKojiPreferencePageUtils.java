/*******************************************************************************
 * Copyright (c) 2010-2011 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.fedoraproject.eclipse.packager.koji.internal.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.fedoraproject.eclipse.packager.koji.KojiPreferencesConstants;
import org.fedoraproject.eclipse.packager.koji.KojiText;
import org.fedoraproject.eclipse.packager.koji.internal.ui.KojiServerDialog;
import org.fedoraproject.eclipse.packager.utils.FedoraHandlerUtils;

/**
 * Koji preference page for advanced settings.
 *
 */
public class FedoraPackagerKojiPreferencePageUtils {

	private Map<String, String[]> pendingServers = new HashMap<>();
	private Composite contents;
	// buffer for unpushed server changes done in style of preference String
	private String listPreferenceBuffer;

	/**
	 * Constructor.
	 *
	 * @param parent The composite.
	 * @param prefStore The preference store.
	 */
	public FedoraPackagerKojiPreferencePageUtils(Composite parent, ScopedPreferenceStore prefStore) {
		contents = parent;
		listPreferenceBuffer = prefStore.getString(KojiPreferencesConstants.PREF_SERVER_LIST);

		for (String serverInfoSet : prefStore.getString(
				KojiPreferencesConstants.PREF_SERVER_LIST).split(";")) { //$NON-NLS-1$
			String[] serverInfo = serverInfoSet.split(","); //$NON-NLS-1$
			if (!addServerItem(serverInfo)) {
				FedoraHandlerUtils
						.showErrorDialog(
								contents.getShell(),
								KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningTitle,
								KojiText.FedoraPackagerAdvancedKojiDialogPage_namespaceWarningMsg);
			}
		}
	}

	/**
	 * Remove the selected instance.
	 *
	 * @param table The table to remove the instance from.
	 * @return The new preferences.
	 */
	public String removeInstance(Table table) {
		TableItem toRemove = table.getSelection()[0];
		String name = toRemove.getText();
		String[] info = pendingServers.get(name);
		listPreferenceBuffer = listPreferenceBuffer.replace(NLS
				.bind(KojiText.ServerEntryTemplate, new String[] {
						name, info[0], info[1], info[2] }), ""); //$NON-NLS-1$
		pendingServers.remove(toRemove.getText(0));
		toRemove.dispose();
		return listPreferenceBuffer;
	}

	/**
	 * Add a new instance.
	 *
	 * @return The new preferences.
	 */
	public String addInstance() {
		String rc = ""; //$NON-NLS-1$
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
				rc = listPreferenceBuffer;
			}
		}
		return rc;
	}

	/**
	 * Edit the selected instance.
	 *
	 * @param table The table to edit the instance from.
	 * @return The new preferences.
	 */
	public String editInstance(Table table) {
		TableItem tableItem = table.getSelection()[0];
		String rc = ""; //$NON-NLS-1$
		String name = tableItem.getText();
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
					for (TableItem item : table.getItems()) {
						if (item.getText().contentEquals(name)) {
							item.setText(newInstance[0]);
						}
					}
					listPreferenceBuffer = listPreferenceBuffer.replace(NLS
							.bind(KojiText.ServerEntryTemplate, new String[] {
									name, info[0], info[1], info[2] }), NLS.bind(
							KojiText.ServerEntryTemplate, newInstance));
					rc = listPreferenceBuffer;
				}
			}
		}
		return rc;
	}

	/**
	 * Add an item to the list of servers.
	 *
	 * @param serverInfo Array containing Strings for server name, and both server URLs.
	 * @return true if successfully added, false otherwise.
	 */
	private boolean addServerItem(String[] serverInfo) {
		if (!pendingServers.containsKey(serverInfo[0])) {
			pendingServers.put(serverInfo[0], new String[] { serverInfo[1],
					serverInfo[2], serverInfo[3] });
			return true;
		} else {
			return false;
		}
	}
}