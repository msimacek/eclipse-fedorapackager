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
package org.fedoraproject.eclipse.packager.koji;

import java.net.URL;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Helper dealing with task URLs.
 * 
 */
public class KojiUtils {

	/**
	 * Construct the correct URL to a task on koji.
	 * 
	 * @param taskId
	 * @param kojiWebUrl
	 * @return The URL as a string.
	 */
	public static String constructTaskUrl(int taskId, URL kojiWebUrl) {
		return kojiWebUrl.toString() + "/taskinfo?taskID=" + taskId; //$NON-NLS-1$
	}

	/**
	 * Load Koji server info from a preference store.
	 * 
	 * @param preferenceStore
	 *            The preference store in which to look for server info.
	 * @return Two-dimensional array with each the ith entry of entry 0 being a
	 *         server name and the ith entry of entry 1 being the corresponding
	 *         comma-delineated string listing server info
	 */
	public static String[][] loadServerInfo(IPreferenceStore preferenceStore) {
		String[] totalServerInfo = preferenceStore.getString(
				KojiPreferencesConstants.PREF_SERVER_LIST).split(";"); //$NON-NLS-1$
		String[][] serverMapping = new String[2][totalServerInfo.length];
		int i = 0;
		for (String serverInfoSet : totalServerInfo) {
			String[] serverInfo = serverInfoSet.split(",", 2); //$NON-NLS-1$
			serverMapping[0][i] = serverInfo[0];
			serverMapping[1][i] = serverInfo[1];
			i++;
		}
		return serverMapping;
	}

	/**
	 * Find the address of the currently selected server in the given server
	 * mapping
	 * 
	 * @param serverMapping
	 *            Server mapping of the form returned by loadServerInfo
	 * @param currentInfo
	 *            The comma-delineated string listing the server info.
	 * @return The address in the mapping to find the server that has the info
	 *         currentInfo.
	 */
	public static int getSelectionAddress(String[][] serverMapping,
			String currentInfo) {
		int selectionAddress = -1;
		for (int i = 0; i < serverMapping[1].length; i++) {
			if (serverMapping[1][i].contentEquals(currentInfo)) {
				selectionAddress = i;
				break;
			}
		}
		return selectionAddress;
	}
}
