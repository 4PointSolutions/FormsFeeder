package com._4point.aem.formsfeeder.core.api;

/**
 * Enhances the FeedSupplier interface to add a name attribute. 
 *
 */
public interface NamedFeedSupplier extends FeedSupplier {

	/**
	 * Returns a name for this FeedSupplier.  This interface is used when multiple FeedSuppliers may
	 * be available and you need to differentiate between them.
	 * 
	 * @return
	 */
	public String name();
}
