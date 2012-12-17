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
package org.fedoraproject.eclipse.packager.rpm.api;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.fedoraproject.eclipse.packager.rpm.RPMPlugin;

/**
 * MessageConsole related code for Eclipse Fedora Packager
 *
 */
public class FedoraPackagerConsole {
	private static final String PROJECT_HOLDER = "%projectName"; //$NON-NLS-1$
	private static final String CONSOLE_NAME = "Packager Console (%projectName)"; //$NON-NLS-1$
	
	/**
	 * @param packageName The name of the package(RPM) this console will be for.
	 * @return A console instance.
	 */
	public static MessageConsole getConsole(String packageName) {
		String projectConsoleName = CONSOLE_NAME.replace(PROJECT_HOLDER, packageName);
		MessageConsole ret = null;
		for (IConsole cons : ConsolePlugin.getDefault().getConsoleManager()
				.getConsoles()) {
			if (cons.getName().equals(projectConsoleName)) {
				ret = (MessageConsole) cons;
			}
		}
		// no existing console, create new one
		if (ret == null) {
			ret = new MessageConsole(projectConsoleName,
					AbstractUIPlugin.imageDescriptorFromPlugin(RPMPlugin.PLUGIN_ID, "icons/rpm.gif")); //$NON-NLS-1$
		}
		ret.clearConsole();
		return ret;
	}
}
