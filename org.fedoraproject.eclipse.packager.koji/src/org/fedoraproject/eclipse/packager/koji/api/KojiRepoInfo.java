package org.fedoraproject.eclipse.packager.koji.api;

import java.util.HashMap;

/**
 * A class representing Repository Information. Constructed from the attribute
 * map returned by XML-RPC call getRepo
 */
public class KojiRepoInfo {

	private static final String KEY_ID = "id"; //$NON-NLS-1$
	private static final String KEY_CREATION_TIME = "creation_time"; //$NON-NLS-1$

	private int id;
	private String creationTime;

	/**
	 * Creates a new object with no attributes
	 */
	public KojiRepoInfo() {
		this.creationTime = new String();
	}

	KojiRepoInfo(HashMap<String, Object> attributes) {
		this.id = ((Integer) attributes.get(KEY_ID)).intValue();
		this.creationTime = (String) attributes.get(KEY_CREATION_TIME);
	}

	/**
	 * @param otherRepo
	 *            the repo to compare this one to.
	 * @return true if the two objects represent the same repo
	 */
	public boolean equals(KojiRepoInfo otherRepo) {
		return this.creationTime.equals(otherRepo.creationTime)
				&& this.id == otherRepo.id;
	}

	@Override
	public String toString() {
		return "KojiRep id " + id + " creation_time: " + this.creationTime; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
