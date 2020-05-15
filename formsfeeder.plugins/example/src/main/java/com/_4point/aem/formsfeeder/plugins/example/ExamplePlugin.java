package com._4point.aem.formsfeeder.plugins.example;

import java.util.Objects;
import java.util.function.Supplier;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.springframework.core.env.Environment;

import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.api.DocumentFactory;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.pf4j.spring.EnvironmentConsumer;

public class ExamplePlugin extends Plugin {

	public ExamplePlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static class ExampleFeedConsumerExtension implements NamedFeedConsumer, EnvironmentConsumer, ExtensionPoint {

		private static final String FEED_CONSUMER_NAME = "Example";
		private static final String AEM_HOST_NAME_ENV = "formsfeeder.plugins.example.aem-host"; 
		private static final String AEM_PORT_NAME_ENV = "formsfeeder.plugins.example.aem-port"; 
		
		private static final Supplier<DocumentFactory> docFactorySupplier = DocumentFactory::getDefault; 
		
		private Environment environment;

		@Override
		public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
			final String aemHostName = Objects.requireNonNull(environment, "").getRequiredProperty(AEM_HOST_NAME_ENV);
			final int aemHostPort = Integer.valueOf(environment.getProperty(AEM_PORT_NAME_ENV, "4502"));
			// Parse out the arguments to this plug-in.
			ExampleParameters params = ExampleParameters.from(dataSources, docFactorySupplier.get());
			// TODO: Add code to call an AEM Server
			// TODO: Return the resulting PDF.
			return null;
		}

		@Override
		public String name() {
			return FEED_CONSUMER_NAME;
		}

		@Override
		public void accept(Environment environment) {
			this.environment = environment;
		}

		// Package visibility for testing.
		/* package */ static class ExampleParameters {
			private static final String TEMPLATE_PARAM_NAME = "template";
			private static final String DATA_PARAM_NAME = "data";
			private static final String INTERACTIVE_PARAM_NAME = "interactive";

			private final Document template;
			private final Document data;
			private final boolean interactive;

			private ExampleParameters(Document template, Document data, boolean interactive) {
				super();
				this.template = template;
				this.data = data;
				this.interactive = interactive;
			}

			public final Document getTemplate() {
				return template;
			}

			public final Document getData() {
				return data;
			}

			public final boolean isInteractive() {
				return interactive;
			}
			
			public static ExampleParameters from(DataSourceList dataSourceList, DocumentFactory docFactory) throws FeedConsumerBadRequestException {
				Deconstructor deconstructor = dataSourceList.deconstructor();
				// Pull the parameters out of the DataSourceList and throw a BadRequestException if they're not there.
				Document templateDoc = docFactory.create(deconstructor.getByteArrayByName(TEMPLATE_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + TEMPLATE_PARAM_NAME + "' Parameter must be supplied.")));
				Document dataDoc = docFactory.create(deconstructor.getByteArrayByName(DATA_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + DATA_PARAM_NAME + "' Parameter must be supplied.")));
				Boolean interactiveBool = deconstructor.getBooleanByName(INTERACTIVE_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + INTERACTIVE_PARAM_NAME + "' Parameter must be supplied."));
				// Use what we've pulled out to construct the ExampleParameters object.
				return new ExampleParameters(templateDoc, dataDoc, interactiveBool);
			}
 		}
	}
}
