package org.fedoraproject.eclipse.packager;

/**
 * A disposable configuration for a single operation on a branch.
 * 
 */
public class BranchConfigInstance {
	private String dist;
	private String distVal;
	private String distVariable;
	private String buildTarget;
	private String equivalentBranch;

	/**
	 * @param dist
	 *            The rpm dist for the build.
	 * @param distVal
	 *            The number of the distribution the branch is intended for.
	 * @param distVariable
	 *            The name of the distribution the branch is intended for.
	 * @param buildTarget
	 *            The Koji build target for this branch.
	 * @param equivalentBranch
	 *            A mapped branch name that corresponds to this configuration,
	 *            irrespective of the actual name of the branch.
	 */
	public BranchConfigInstance(String dist, String distVal,
			String distVariable, String buildTarget, String equivalentBranch) {
		this.dist = dist;
		this.distVal = distVal;
		this.distVariable = distVariable;
		this.buildTarget = buildTarget;
		this.equivalentBranch = equivalentBranch;
	}

	/**
	 * @return The Koji build target for this branch.
	 */
	public String getBuildTarget() {
		return buildTarget;
	}

	/**
	 * @param buildTarget
	 *            The Koji build target for this branch.
	 */
	public void setBuildTarget(String buildTarget) {
		this.buildTarget = buildTarget;
	}

	/**
	 * @return The rpm dist for the build.
	 */
	public String getDist() {
		return dist;
	}

	/**
	 * @param dist
	 *            The rpm dist for the build.
	 */
	public void setDist(String dist) {
		this.dist = dist;
	}

	/**
	 * @return The number of the distribution the branch is intended for.
	 */
	public String getDistVal() {
		return distVal;
	}

	/**
	 * @param distVal
	 *            The number of the distribution the branch is intended for.
	 */
	public void setDistVal(String distVal) {
		this.distVal = distVal;
	}

	/**
	 * @return The name of the distribution the branch is intended for.
	 */
	public String getDistVariable() {
		return distVariable;
	}

	/**
	 * @param distVariable
	 *            The name of the distribution the branch is intended for.
	 */
	public void setDistVariable(String distVariable) {
		this.distVariable = distVariable;
	}

	/**
	 * @return A mapped branch name that corresponds to this configuration,
	 *         irrespective of the actual name of the branch.
	 */
	public String getEquivalentBranch() {
		return equivalentBranch;
	}

	/**
	 * @param equivalentBranch
	 *            A mapped branch name that corresponds to this configuration,
	 *            irrespective of the actual name of the branch.
	 */
	public void setEquivalentBranch(String equivalentBranch) {
		this.equivalentBranch = equivalentBranch;
	}
}
