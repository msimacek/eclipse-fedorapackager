package org.fedoraproject.eclipse.packager.rpm.api.errors;

import org.fedoraproject.eclipse.packager.api.errors.FedoraPackagerAPIException;
import org.fedoraproject.eclipse.packager.rpm.RpmText;
import org.fedoraproject.eclipse.packager.rpm.api.FedoraReviewCommand;

/**
 * Thrown if {@link FedoraReviewCommand} determined that the fedora-review
 * program is not installed.
 * 
 */
public class FedoraReviewNotInstalledException extends
		FedoraPackagerAPIException {
	private static final long serialVersionUID = 3116215444241551532L;
	/**
	 * Default constructor.
	 */
	public FedoraReviewNotInstalledException() {
		super(RpmText.FedoraReviewNotInstalledException_msg);
	}
}
