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
package org.fedoraproject.eclipse.packager.koji.api.errors;

import org.eclipse.core.runtime.IStatus;
import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;

/**
 * Exceptions thrown by koji clients.
 */
public class KojiHubClientException extends FedoraPackagerAPIException {

	private static final long serialVersionUID = -3622365505538797782L;

	private IStatus status;

	/**
	 * Default constructor
	 */
	public KojiHubClientException() {
		// empty
	}

	/**
	 * @param msg
	 *            The message associated with this exception.
	 * @param cause
	 *            The throwable that caused the exception to be thrown.
	 */
	public KojiHubClientException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Create a new KojiHubClientException with a status object to be used for
	 * functions which are required to return a status object.
	 *
	 * @param msg
	 *            The error message.
	 * @param cause
	 *            The causing Throwable
	 * @param status
	 *            The status object representing the result of the requested
	 *            operation
	 */
	public KojiHubClientException(String msg, Throwable cause, IStatus status) {
		this(msg, cause);
		this.status = status;
	}

	/**
	 * @param cause
	 *            The throwable that caused the exception to be thrown.
	 */
	public KojiHubClientException(Throwable cause) {
		super("Client error", cause); //$NON-NLS-1$
	}

	/**
	 * @return The status object representing the error for which this exception
	 *         was created.
	 */
	public IStatus getStatus() {
		return this.status;
	}
}
