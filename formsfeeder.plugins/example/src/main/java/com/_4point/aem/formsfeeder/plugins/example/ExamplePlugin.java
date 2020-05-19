package com._4point.aem.formsfeeder.plugins.example;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.springframework.core.env.Environment;

import com._4point.aem.docservices.rest_services.client.forms.RestServicesFormsServiceAdapter;
import com._4point.aem.docservices.rest_services.client.output.RestServicesOutputServiceAdapter;
import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.api.DocumentFactory;
import com._4point.aem.fluentforms.api.forms.FormsService;
import com._4point.aem.fluentforms.api.output.OutputService;
import com._4point.aem.fluentforms.api.output.OutputService.OutputServiceException;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.fluentforms.impl.UsageContext;
import com._4point.aem.fluentforms.impl.forms.FormsServiceImpl;
import com._4point.aem.fluentforms.impl.output.OutputServiceImpl;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
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
		
		private static final Supplier<DocumentFactory> docFactorySupplier = SimpleDocumentFactoryImpl::getFactory; 
		
		private Environment environment;	// Initialized when the plugin is loaded by FeedConsumers.
		private String aemHostName;
		private Integer aemHostPort;
		private String aemUsername;
		private String aemPassword;
		private FormsService formsService;
		private OutputService outputService;

		@Override
		public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
			// Parse out the arguments to this plug-in.
			ExamplePluginInputParameters params = ExamplePluginInputParameters.from(dataSources, docFactorySupplier.get());

			// Call an AEM Server
			try {
				Document result;
				if (params.isInteractive()) {
//					formsService().renderPDFForm();
//					result = null;
					throw new UnsupportedOperationException("Interactive Rendering not supported yet!");
				} else {
					result = outputService().generatePDFOutput()
											.executeOn(params.getTemplate(), params.getData());
				}
			
				// return the result 
				return DataSourceList.builder()
									 .add("Result", result.getInputStream().readAllBytes(), MimeType.of(result.getContentType()))
									 .build();
			} catch (OutputServiceException e) {
				throw new FeedConsumerInternalErrorException("Error while generating PDF.", e);
			} catch (IOException e) {
				throw new FeedConsumerInternalErrorException("Error while reading generating PDF.", e);
			}
		}

		private String aemHostName() {
			if (this.aemHostName == null) {
				this.aemHostName = Objects.requireNonNull(environment, "Plug-in's Environment has not been populated!").getRequiredProperty(EnvironmentConsumer.AEM_HOST_ENV_PARAM);
			}
			return this.aemHostName;
		}

		private int aemHostPort() {
			if (this.aemHostPort == null) {
				this.aemHostPort = Integer.valueOf(Objects.requireNonNull(environment, "Plug-in's Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_PORT_ENV_PARAM, "4502"));
			}
			return this.aemHostPort;
		}

		private FormsService formsService() {
			if (this.formsService == null) {
				RestServicesFormsServiceAdapter formsAdapter = RestServicesFormsServiceAdapter.builder()
							.machineName(aemHostName())
							.port(aemHostPort())
							.basicAuthentication(aemUsername(), aemPassword())
							.useSsl(false)
							.build();
				this.formsService = new FormsServiceImpl(formsAdapter, UsageContext.CLIENT_SIDE);
			}
			return this.formsService;
		}

		private OutputService outputService() {
			if (this.outputService == null) {
				RestServicesOutputServiceAdapter outputAdapter = RestServicesOutputServiceAdapter.builder()
							.machineName(aemHostName())
							.port(aemHostPort())
							.basicAuthentication(aemUsername(), aemPassword())
							.useSsl(false)
							.build();
					
				this.outputService = new OutputServiceImpl(outputAdapter, UsageContext.CLIENT_SIDE);
			}
			return this.outputService;
		}
		
		private String aemUsername() {
			if (this.aemUsername == null) {
				this.aemUsername = "admin";		// Hardcoded for now, may change this later 
			}
			return this.aemUsername;
		}
		
		private String aemPassword() {
			if (this.aemPassword == null) {
				this.aemPassword = "admin";		// Hardcoded for now, may change this later 
			}
			return this.aemPassword;
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
		/* package */ static class ExamplePluginInputParameters {
			private static final String TEMPLATE_PARAM_NAME = "template";
			private static final String DATA_PARAM_NAME = "data";
			private static final String INTERACTIVE_PARAM_NAME = "interactive";

			private final Document template;
			private final Document data;
			private final boolean interactive;

			private ExamplePluginInputParameters(Document template, Document data, boolean interactive) {
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
			
			public static ExamplePluginInputParameters from(DataSourceList dataSourceList, DocumentFactory docFactory) throws FeedConsumerBadRequestException {
				Deconstructor deconstructor = dataSourceList.deconstructor();
				// Pull the parameters out of the DataSourceList and throw a BadRequestException if they're not there.
				Document templateDoc = docFactory.create(deconstructor.getByteArrayByName(TEMPLATE_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + TEMPLATE_PARAM_NAME + "' Parameter must be supplied.")));
				Document dataDoc = docFactory.create(deconstructor.getByteArrayByName(DATA_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + DATA_PARAM_NAME + "' Parameter must be supplied.")));
				Boolean interactiveBool = deconstructor.getBooleanByName(INTERACTIVE_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + INTERACTIVE_PARAM_NAME + "' Parameter must be supplied."));
				// Use what we've pulled out to construct the ExampleParameters object.
				return new ExamplePluginInputParameters(templateDoc, dataDoc, interactiveBool);
			}
 		}
	}
}
