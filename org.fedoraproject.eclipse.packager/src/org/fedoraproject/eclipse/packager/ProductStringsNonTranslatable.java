package org.fedoraproject.eclipse.packager;

/**
 * Class to retrieve plug-in specific names.
 */
public class ProductStringsNonTranslatable implements IProductStrings {

	@SuppressWarnings("unused")
	private IProjectRoot root;
	
	// NOTE:
	// This has been implemented this way for a reason. If you think this must
	// absolutely change, please ask first.
	
	/**
	 * No-arg default constructor.
	 */
	public ProductStringsNonTranslatable() {
		// empty
	}
	
	/**
	 * @return The name of this product.
	 */
	@Override
	public String getProductName() {
		return getDistributionName() + " Packager"; //$NON-NLS-1$
	}
	
	/**
	 * @return The name of this distribution.
	 */
	@Override
	public String getDistributionName() {
		return "Fedora"; //$NON-NLS-1$
	}
	
	/**
	 * @return The name of the build infrastructure.
	 */
	@Override
	public String getBuildToolName() {
		return "Koji"; //$NON-NLS-1$
	}
	
	/**
	 * @return The name of the update infrastructure. 
	 */
	@Override
	public String getUpdateToolName() {
		return "Bodhi"; //$NON-NLS-1$
	}

	@Override
	public void initialize(IProjectRoot root) {
		this.root = root;
	}
}
