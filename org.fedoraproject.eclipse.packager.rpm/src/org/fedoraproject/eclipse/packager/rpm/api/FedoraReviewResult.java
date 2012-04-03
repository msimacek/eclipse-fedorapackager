package org.fedoraproject.eclipse.packager.rpm.api;

/**
 * Result class for FedoraReviewCommand.
 *
 */
public class FedoraReviewResult extends Result {
	
	private boolean success;

	/**
	 * Default constructor.
	 * @param cmdList The command run.
	 */
	public FedoraReviewResult(String[] cmdList) {
		super(cmdList);
		success = true;
	}

	@Override
	public boolean wasSuccessful() {
		return success;
	}

	/**
	 * Set result to success.
	 */
	public void setSuccess() {
		success = true;
		
	}

	/**
	 * Set result to failure.
	 */
	public void setFailure() {
		success = false;
	}

}
