/**
 * 
 */
package com._4point.aem.formsfeeder.server.pf4j;

import org.pf4j.ExtensionPoint;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;

/**
 * Sets up an ExtensionPoint for Plugins to use.  Each Plug-in will implement this interface.
 *
 */
public interface FeedConsumerExtensionPoint extends FeedConsumer, ExtensionPoint {

}
