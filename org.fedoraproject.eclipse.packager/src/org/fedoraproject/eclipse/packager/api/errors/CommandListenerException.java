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
package org.fedoraproject.eclipse.packager.api.errors;

/**
 * Common exception superclass for command listeners.
 *
 */
public class CommandListenerException extends FedoraPackagerAPIException {

	private static final long serialVersionUID = -8660306046206103772L;

	/**
	 * Wrap a listener exception in a CommandListenerException. 
	 * 
	 * @param cause The throwable that caused the exception to be thrown.
	 */
	public CommandListenerException(Throwable cause) {
		super("unused", cause); //$NON-NLS-1$
	}

	/**
	 * Wrap an exception with additional message.
	 * @param message The additional message
	 * @param cause The exception to wrap.
	 */
	public CommandListenerException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a message only exception.
	 * @param message The message.
	 */
	public CommandListenerException(String message) {
		super(message);
	}
}
