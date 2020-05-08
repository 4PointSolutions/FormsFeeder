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

		// List of valid scenarios
		private static final String SCENARIO_NAME_BAD_REQUEST_EXCEPTION = "BadRequestException";
		private static final String SCENARIO_NAME_INTERNAL_ERROR_EXCEPTION = "InternalErrorException";
		private static final String SCENARIO_NAME_UNCHECKED_EXCEPTION = "UncheckedException";
		private static final String SCENARIO_OTHER_FEED_CONSUMER_EXCEPTION = "OtherFeedConsumerException";
		
		@Override
		public String name() {
			return FEED_CONSUMER_NAME;
		}

		@SuppressWarnings("serial")
		@Override
		public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
			
			Deconstructor deconstructor = dataSources.deconstructor();
			
			String scenario = deconstructor.getStringByName(DS_NAME_SCENARIO).orElse(SCENARIO_NAME_UNKNOWN);	// retrieve the unit testing scenario
			logger.info("MockPlugin scenario is {}", scenario);
			switch(scenario)
			{
			case SCENARIO_OTHER_FEED_CONSUMER_EXCEPTION:
				throw new FeedConsumerException() {

					@Override
					public String getMessage() {
						return "Throwing anonymous FeedConsumerException because scenario was '" + scenario + "'.";
					}
					
				};
			case SCENARIO_NAME_BAD_REQUEST_EXCEPTION:
				throw new FeedConsumerBadRequestException("Throwing FeedConsumerBadRequestException because scenario was '" + scenario + "'.");
			case SCENARIO_NAME_INTERNAL_ERROR_EXCEPTION:
				throw new FeedConsumerInternalErrorException("Throwing FeedConsumerInternalErrorException because scenario was '" + scenario + "'.");
			case SCENARIO_NAME_UNCHECKED_EXCEPTION:
				throw new IllegalStateException("Throwing IllegalStateException because scenario was '" + scenario + "'.");
			case SCENARIO_NAME_UNKNOWN:
				throw new FeedConsumerBadRequestException("No scenario name was provided.");
			default:
				throw new FeedConsumerBadRequestException("Unexpected scenario name was provided (" + scenario + ").");
			}
		}

	}

}
