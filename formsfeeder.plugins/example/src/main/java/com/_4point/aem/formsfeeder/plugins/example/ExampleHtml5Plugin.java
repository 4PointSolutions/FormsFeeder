package com._4point.aem.formsfeeder.plugins.example;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.springframework.core.env.Environment;

import com._4point.aem.docservices.rest_services.client.html5.Html5FormsService;
import com._4point.aem.docservices.rest_services.client.html5.Html5FormsService.Html5FormsServiceException;
import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.api.DocumentFactory;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.pf4j.spring.EnvironmentConsumer;

@Extension
public class ExampleHtml5Plugin implements NamedFeedConsumer, EnvironmentConsumer, ExtensionPoint {

	private static final String PLUGIN_NAME = "RenderHtml5";

	private Environment environment;	// Initialized when the plugin is loaded by FeedConsumers.
	private String aemHostName;			// Pulled from the Environment
	private Integer aemHostPort;		// Pulled from the Environment
	private String aemUsername;			// Currently hard-coded to "admin"
	private String aemPassword;			// Currently hard-coded to "admin"

	// Creation of TraditionalxxxxxService is redirected through these lambdas so that we can replace them with mocks during unit testing.
	private static final Supplier<DocumentFactory> docFactorySupplier = SimpleDocumentFactoryImpl::getFactory;

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		
		ExampleHtml5PluginInputParameters params = ExampleHtml5PluginInputParameters.from(dataSources, docFactorySupplier.get());
		
		try {
			Html5FormsService html5Service = Html5FormsService.builder()
															  .machineName(aemHostName())
															  .port(aemHostPort())
															  .basicAuthentication(aemUsername(), aemPassword())
															  .useSsl(false)
															  .build();

			Document result = params.getData().isEmpty() ? html5Service.renderHtml5Form(params.getTemplate())
														 : html5Service.renderHtml5Form(params.getTemplate(), params.getData().get());
			
			return DataSourceList.builder()
								 .add("Result", result.getInputStream().readAllBytes(), MimeType.of(result.getContentType()))
								 .build();
		} catch (Html5FormsServiceException e) {
			String msg = e.getMessage();
			throw new FeedConsumerInternalErrorException("Error while rendering Html. (" + (msg == null ? e.getClass().getName() : msg) + ").", e);
		} catch (IOException e) {
			String msg = e.getMessage();
			throw new FeedConsumerInternalErrorException("Error while reading rendering Html.(" + (msg == null ? e.getClass().getName() : msg) + ").", e);
		}
	}

	@Override
	public void accept(Environment environment) {
		this.environment = environment;
	}

	@Override
	public String name() {
		return PLUGIN_NAME;
	}

	/**
	 * Pull the Host name from the environment and cache it.
	 * 
	 * @return AEM Host Name
	 */
	private String aemHostName() {
		if (this.aemHostName == null) {
			this.aemHostName = Objects.requireNonNull(environment, "Plug-in's Environment has not been populated!").getRequiredProperty(EnvironmentConsumer.AEM_HOST_ENV_PARAM);
		}
		return this.aemHostName;
	}

	/**
	 * Pull the Host port from the environment and cache it.
	 * 
	 * @return AEM Host Port
	 */
	private int aemHostPort() {
		if (this.aemHostPort == null) {
			this.aemHostPort = Integer.valueOf(Objects.requireNonNull(environment, "Plug-in's Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_PORT_ENV_PARAM, "4502"));
		}
		return this.aemHostPort;
	}

	/**
	 * Pull the AEM user name we will use to call AEM.
	 * 
	 * This is currently hardcoded but could just as easily be pulled from the environment.
	 * 
	 * @return AEM Host Port
	 */
	private String aemUsername() {
		if (this.aemUsername == null) {
			this.aemUsername = "admin";		// Hardcoded for now, may change this later 
		}
		return this.aemUsername;
	}
	
	/**
	 * Pull the AEM user name we will use to call AEM.
	 * 
	 * This is currently hardcoded but could just as easily be pulled from the environment.  If pulled from the
	 * environment it should probably be encrypted in the environment and decrypted here.
	 * 
	 * @return AEM Host Port
	 */
	private String aemPassword() {
		if (this.aemPassword == null) {
			this.aemPassword = "admin";		// Hardcoded for now, may change this later 
		}
		return this.aemPassword;
	}

	/**
	 * Internal object used for deconstructing the incoming DataSourceList into individual Java objects. 
	 *
	 * It has package visibility so that it can be unit tested separately.
	 */
	/* package */ static class ExampleHtml5PluginInputParameters {
		private static final String TEMPLATE_PARAM_NAME = "template";
		private static final String DATA_PARAM_NAME = "data";
		private static final String CONTENT_ROOT_PARAM_NAME = "contentRoot";
		// private static final String SUBMIT_URL_PARAM_NAME = "submitUrl"; // Not currently supported.

		private final String template;
		private final Optional<Document> data;
		private final Optional<String> contentRoot;

		private ExampleHtml5PluginInputParameters(String template, Optional<Document> data, Optional<String> contentRoot) {
			super();
			this.template = template;
			this.data = data;
			this.contentRoot = contentRoot;
		}

		public final String getTemplate() {
			return template;
		}

		public final Optional<Document> getData() {
			return data;
		}

		public final Optional<String> getContentRoot() {
			return contentRoot;
		}

		public static ExampleHtml5PluginInputParameters from(DataSourceList dataSourceList, DocumentFactory docFactory) throws FeedConsumerBadRequestException {
			Deconstructor deconstructor = dataSourceList.deconstructor();
			// Pull the parameters out of the DataSourceList and throw a BadRequestException if they're not there.
			String templateDoc = deconstructor.getStringByName(TEMPLATE_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + TEMPLATE_PARAM_NAME + "' Parameter must be supplied."));
			Optional<Document> dataDoc = deconstructor.getStringByName(DATA_PARAM_NAME).map(Paths::get).map(docFactory::create);
			Optional<String> contentRoot = deconstructor.getStringByName(CONTENT_ROOT_PARAM_NAME);
			// Use what we've pulled out to construct the ExampleParameters object.
			return new ExampleHtml5PluginInputParameters(templateDoc, dataDoc, contentRoot);
		}
		
	}

}
