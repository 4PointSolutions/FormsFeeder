package com._4point.aem.formsfeeder.plugins.example;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;

public class ExamplePlugin extends Plugin {

	public ExamplePlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static class ExampleFeedConsumerExtension implements NamedFeedConsumer, ExtensionPoint {

		public static final String FEED_CONSUMER_NAME = "Example";

		@Override
		public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
			// TODO: Add code to parse out the arguments to this plug-in.
			// TODO: Add code to call an AEM Server
			// TODO: Return the resulting PDF.
			return null;
		}

		@Override
		public String name() {
			return FEED_CONSUMER_NAME;
		}

	}

}
