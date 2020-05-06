package com._4point.aem.formsfeeder.plugins.mock;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;

public class MockPlugin extends Plugin {

	public MockPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	/**
	 * Mock plugin is used by the unit testing code to test various plug-in behaviours and scenarios. 
	 *
	 */
	@Extension
	public static class MockExtension implements NamedFeedConsumer, ExtensionPoint {
		Logger logger = LoggerFactory.getLogger(this.getClass());

		public static final String FEED_CONSUMER_NAME = "Mock";
		private static final String DS_NAME_SCENARIO = "scenario";
		
		private static final String SCENARIO_NAME_UNKNOWN = "Unknown";
		
		@Override
		public String name() {
			return FEED_CONSUMER_NAME;
		}

		@Override
		public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
			
			Deconstructor deconstructor = dataSources.deconstructor();
			
			String scenario = deconstructor.getStringByName(DS_NAME_SCENARIO).orElse(SCENARIO_NAME_UNKNOWN);	// retrieve the unit testing scenario
			logger.info("MockPlugin scenario is {}", scenario);
			switch(scenario)
			{
				case SCENARIO_NAME_UNKNOWN:
					throw new FeedConsumerBadRequestException("No scenario name was provided.");
				default:
					throw new FeedConsumerBadRequestException("Unexpected scenario name was provided (" + scenario + ").");
			}
		}

	}

}
