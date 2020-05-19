package com._4point.aem.formsfeeder.core.api;

import java.util.List;

/**
 * Implementing this interface in a plug-in allows that plug-in other plugins.
 *
 */
@FunctionalInterface
public interface PluginsConsumer {

	public void accept(List<NamedFeedConsumer> consumers); 
}
