package com._4point.aem.formsfeeder.plugins.mock;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.pf4j.spring.ApplicationContextConsumer;
import com._4point.aem.formsfeeder.pf4j.spring.EnvironmentConsumer;

@Component
public class MockPlugin extends Plugin {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	public MockPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	/**
	 * Mock plugin is used by the unit testing code to test various plug-in behaviours and scenarios. 
	 *
	 */
	@Component
	@Extension
	public static class MockExtension implements NamedFeedConsumer, EnvironmentConsumer, ApplicationContextConsumer, ExtensionPoint {
		Logger logger = LoggerFactory.getLogger(this.getClass());

		public static final String FEED_CONSUMER_NAME = "Mock";
		private static final String DS_NAME_SCENARIO = "scenario";
		
		private static final String SCENARIO_NAME_UNKNOWN = "Unknown";

		// List of valid scenarios
		private static final String SCENARIO_NAME_BAD_REQUEST_EXCEPTION = "BadRequestException";
		private static final String SCENARIO_NAME_INTERNAL_ERROR_EXCEPTION = "InternalErrorException";
		private static final String SCENARIO_NAME_UNCHECKED_EXCEPTION = "UncheckedException";
		private static final String SCENARIO_OTHER_FEED_CONSUMER_EXCEPTION = "OtherFeedConsumerException";
		private static final String SCENARIO_RETURN_PDF = "ReturnPdf";
		private static final String SCENARIO_RETURN_XML = "ReturnXml";
		private static final String SCENARIO_RETURN_PDF_AND_XML = "ReturnPdfAndXml";
		private static final String SCENARIO_RETURN_CONFIG_VALUE = "ReturnConfigValue";
		private static final String SCENARIO_RETURN_APPLICATION_CONTEXT_CONFIG_VALUE = "ReturnApplicationContextConfigValue";

		private FileSystem zipfs = null;	// Used to hold ZipFs so that we can read our .jar resources using FileSystem
		
		private Environment environment;
		
		private ApplicationContext applicationContext;
		
		public MockExtension() {
			super();
			logger.info("inside MockExtension constructor");
		}
		
		public final Environment getEnvironment() {
			return environment;
		}

		public final void setEnvironment(Environment environment) {
			logger.debug("setting environment is " + (environment == null ? "" : "not ") + "null");
			this.environment = environment;
			logger.debug("setting formsfeeder.plugins.mock.configValue is " + (environment.getProperty("formsfeeder.plugins.mock.configValue") == null ? " null." : "'" + environment.getProperty("formsfeeder.plugins.mock.configValue") + "'."));
		}
		
		public final ApplicationContext getApplicationContext() {
			return applicationContext;
		}

		public final void setApplicationContext(ApplicationContext applicationContext) {
			logger.debug("setting applicationContext is " + (applicationContext == null ? "" : "not ") + "null");
			this.applicationContext = applicationContext;
		}

		@Override
		public String name() {
			return FEED_CONSUMER_NAME;
		}

		@SuppressWarnings("serial")
		@Override
		public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
			
			Deconstructor deconstructor = dataSources.deconstructor();
			Builder builder = DataSourceList.builder();
			
			String scenario = deconstructor.getStringByName(DS_NAME_SCENARIO).orElse(SCENARIO_NAME_UNKNOWN);	// retrieve the unit testing scenario
			logger.info("MockPlugin scenario is {}", scenario);
			switch(scenario)
			{
			case SCENARIO_RETURN_APPLICATION_CONTEXT_CONFIG_VALUE:
				logger.debug("applicationContext is " + (this.getApplicationContext() == null ? "" : "not ") + "null.");
				String ctxConfigValue = this.getApplicationContext().getBean(Environment.class).getProperty("formsfeeder.plugins.mock.configValue");
				logger.debug("setting ConfigValue to '" + ctxConfigValue + "'.");
				if (ctxConfigValue == null) {
					ctxConfigValue = "substitutedValue";
				}
				builder.add("ConfigValue", ctxConfigValue);
				break;
			case SCENARIO_RETURN_CONFIG_VALUE:
				logger.debug("environment is " + (this.getEnvironment() == null ? "" : "not ") + "null.");
				String envConfigValue = this.getEnvironment().getProperty("formsfeeder.plugins.mock.configValue");
				logger.debug("setting ConfigValue to '" + envConfigValue + "'.");
				if (envConfigValue == null) {
					envConfigValue = "substitutedValue";
				}
				builder.add("ConfigValue", envConfigValue);
				break;
			case SCENARIO_RETURN_PDF_AND_XML:
				builder.add("PdfResult", getResourcePath("SampleForm.pdf"));
				builder.add("XmlResult", getResourcePath("SampleForm_data.xml"));
				break;
			case SCENARIO_RETURN_XML:
				builder.add("XmlResult", getResourcePath("SampleForm_data.xml"));
				break;
			case SCENARIO_RETURN_PDF:
				builder.add("PdfResult", getResourcePath("SampleForm.pdf"));
				break;
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
			
			return builder.build(); 
		}

		private final Path getResourcePath(String resourceName) throws FeedConsumerInternalErrorException {
			URL jarResource = getClass().getClassLoader().getResource(resourceName);
			if (jarResource == null) {
				throw new FeedConsumerInternalErrorException("Problem locating pdf Resource.");
			} else {
				try {
					URI uri = jarResource.toURI();
					if (zipfs == null && (uri.toString().startsWith("/") || uri.toString().startsWith("jar:"))) {
						try {
							zipfs = FileSystems.getFileSystem(uri);
						} catch (FileSystemNotFoundException e) {
							// File system doesn't exist, so create it.
							zipfs = FileSystems.newFileSystem(uri, Map.of("create", "true"));
						}
						return Path.of(uri);
					} else {
						return Path.of(uri);
					}
				} catch (URISyntaxException | IOException e) {
					throw new FeedConsumerInternalErrorException("Problem with converting jar resource to path.", e);
				}
			}
		}

		@Override
		public void accept(Environment environment) {
			setEnvironment(environment);
		}

		@Override
		public void accept(ApplicationContext ctx) {
			setApplicationContext(ctx);
		}
	}

}
