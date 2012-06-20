package org.fedoraproject.eclipse.packager.git.api.errors;

import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;

/**
 * Thrown if there is a remote uri already added to the project
 *
 */
public class RemoteAlreadyExistsException extends FedoraPackagerAPIException {

	private static final long serialVersionUID = 5372585618816074471L;
	
	/**
	 * @param message The message associated with this exception.
	 * @param cause The Throwable that caused this exception.
	 */
	public RemoteAlreadyExistsException(String message,	Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message The message associated with this exception.
	 */
	public RemoteAlreadyExistsException(String message) {
		super(message);
	}
}
