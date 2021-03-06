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
 * Thrown if copying spec and srpm files to the fedorapeople.org was not
 * successful
 * 
 */
public class ScpFailedException extends FedoraPackagerAPIException {

	private static final long serialVersionUID = 9098621863061603960L;

	/**
	 * @param message
	 *            The message associated with this exception.
	 */
	public ScpFailedException(String message) {
		super(message);
	}

	/**
	 * @param message
	 *            The message associated with this exception.
	 * @param cause
	 *            The throwable that caused the exception to be thrown.
	 */
	public ScpFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
