package com._4point.aem.formsfeeder.core.api;

/**
 * Enhances the FeedConsumer interface to add a name attribute. 
 *
 */
public interface NamedFeedConsumer extends FeedConsumer {

	/**
	 * Returns a name for this FeedConsumer.  This interface is used when multiple FeedConsumers may
	 * be available and you need to differentiate between them.
	 * 
	 * @return
	 */
	public String name();
}
