package org.fedoraproject.eclipse.packager.rpm.api;

import org.eclipse.core.resources.IFile;

/**
 * Result class for FedoraReviewCommand.
 *
 */
public class FedoraReviewResult extends Result {
	
	private boolean successful;
	private IFile review = null;

	/**
	 * Default constructor.
	 * @param cmdList The command run.
	 */
	public FedoraReviewResult(String[] cmdList) {
		super(cmdList);
		successful = true;
	}

	@Override
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * Set result to successful.
	 * @param success The new state.
	 */
	public void setSuccessful(boolean success) {
		this.successful = success;
		
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
