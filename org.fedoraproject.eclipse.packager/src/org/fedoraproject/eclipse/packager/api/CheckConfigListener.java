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
package org.fedoraproject.eclipse.packager.api;

import org.fedoraproject.eclipse.packager.api.errors.CommandListenerException;

/**
 * A simple listener which makes sure that each command is properly configured
 * before it is called.
 *
 */
public class CheckConfigListener implements ICommandListener {

	private final FedoraPackagerCommand<?> cmd;
	
	/**
	 * @param cmd The command for which the configuration should be checked.
	 */
	public CheckConfigListener(FedoraPackagerCommand<?> cmd) {
		this.cmd = cmd;
	}
	
	/**
	 * Make sure we can call and execute the command.
	 */
	@Override
	public void preExecution() throws CommandListenerException {
		cmd.checkCallable();
		cmd.checkConfiguration();
	}

	@Override
	public void postExecution() {
		// nothing to do for this listener
	}

}
