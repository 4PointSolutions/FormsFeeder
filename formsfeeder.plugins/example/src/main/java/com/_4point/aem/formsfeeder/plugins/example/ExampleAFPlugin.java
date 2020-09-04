package com._4point.aem.formsfeeder.plugins.example;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.springframework.core.env.Environment;

import com._4point.aem.docservices.rest_services.client.af.AdaptiveFormsService;
import com._4point.aem.docservices.rest_services.client.af.AdaptiveFormsService.AdaptiveFormsServiceException;
import com._4point.aem.docservices.rest_services.client.helpers.StandardFormsFeederUrlFilters;
import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.api.DocumentFactory;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerInternalErrorException;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.pf4j.spring.EnvironmentConsumer;

@Extension
public class ExampleAFPlugin implements NamedFeedConsumer, EnvironmentConsumer, ExtensionPoint {

	private static final String PLUGIN_NAME = "RenderAdaptiveForm";

	private Environment environment;	// Initialized when the plugin is loaded by FeedConsumers.
	private String aemHostName;			// Pulled from the Environment
	private Integer aemHostPort;		// Pulled from the Environment
	private String aemUsername;			// Currently hard-coded to "admin"
	private String aemPassword;			// Currently hard-coded to "admin"

	// Creation of TraditionalxxxxxService is redirected through these lambdas so that we can replace them with mocks during unit testing.
	private static final Supplier<DocumentFactory> docFactorySupplier = SimpleDocumentFactoryImpl::getFactory;

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		
		ExampleAFPluginInputParameters params= ExampleAFPluginInputParameters.from(dataSources, docFactorySupplier.get());
		
		try {
			AdaptiveFormsService afService = AdaptiveFormsService.builder()
																  .machineName(aemHostName())
																  .port(aemHostPort())
																  .basicAuthentication(aemUsername(), aemPassword())
																  .useSsl(false)
																  // Formsfeeder acts as reverse proxy for AEM, so this fixes up URLs to match the proxied location.
																  .addRenderResultFilter(StandardFormsFeederUrlFilters::replaceAemUrls)
																  .build();

			Document result = params.getData().isEmpty() ? afService.renderAdaptiveForm(params.getTemplate())
														 : afService.renderAdaptiveForm(params.getTemplate(), params.getData().get());

			return DataSourceList.builder()
					 .add("Result", result.getInputStream().readAllBytes(), MimeType.of(result.getContentType()))
					 .build();
		} catch (AdaptiveFormsServiceException e) {
			String msg = e.getMessage();
			throw new FeedConsumerInternalErrorException("Error while rendering Adaptive Form. (" + (msg == null ? e.getClass().getName() : msg) + ").", e);
		} catch (IOException e) {
			String msg = e.getMessage();
			throw new FeedConsumerInternalErrorException("Error while reading rendered Html.(" + (msg == null ? e.getClass().getName() : msg) + ").", e);
		}
	}

	@Override
	public String name() {
		return PLUGIN_NAME;
	}

	@Override
	public void accept(Environment environment) {
		this.environment = environment;
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
	/* package */ static class ExampleAFPluginInputParameters {
		private static final String TEMPLATE_PARAM_NAME = "template";
		private static final String DATA_PARAM_NAME = "data";

		private final String template;
		private final Optional<Document> data;

		private ExampleAFPluginInputParameters(String template, Optional<Document> data) {
			super();
			this.template = template;
			this.data = data;
		}

		public final String getTemplate() {
			return template;
		}

		public final Optional<Document> getData() {
			return data;
		}

		public static ExampleAFPluginInputParameters from(DataSourceList dataSourceList, DocumentFactory docFactory) throws FeedConsumerBadRequestException {
			Deconstructor deconstructor = dataSourceList.deconstructor();
			// Pull the parameters out of the DataSourceList and throw a BadRequestException if they're not there.
			String templateDoc = deconstructor.getStringByName(TEMPLATE_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + TEMPLATE_PARAM_NAME + "' Parameter must be supplied."));
			Optional<Document> dataDoc = deconstructor.getStringByName(DATA_PARAM_NAME).map(Paths::get).map(docFactory::create);
			// Use what we've pulled out to construct the ExampleParameters object.
			return new ExampleAFPluginInputParameters(templateDoc, dataDoc);
		}
		
	}

}
