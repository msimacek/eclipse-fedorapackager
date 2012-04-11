package org.fedoraproject.eclipse.packager.rpm.api;

import org.eclipse.core.resources.IFile;

/**
 * Result class for FedoraReviewCommand.
 *
 */
public class FedoraReviewResult extends Result {
	
	private boolean success;
	private IFile review = null;

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
	
	/**
	 * Set the file containing the review.
	 * @param review The file.
	 */
	public void setReview(IFile review){
		this.review = review;
	}
	
	/**
	 * Get the file containing the reivew.
	 * @return The review file.
	 */
	public IFile getReview(){
		return review;
	}

}
