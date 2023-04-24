package com._4point.aem.formsfeeder.plugins.example;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.springframework.beans.factory.annotation.Autowired;

import com._4point.aem.docservices.rest_services.client.helpers.AemServerType;
import com._4point.aem.docservices.rest_services.client.helpers.StandardFormsFeederUrlFilters;
import com._4point.aem.docservices.rest_services.client.html5.Html5FormsService;
import com._4point.aem.docservices.rest_services.client.html5.Html5FormsService.Html5FormsServiceException;
import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.api.DocumentFactory;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.MimeType;

@Extension
public class ExampleHtml5Plugin implements NamedFeedConsumer, ExtensionPoint {

	private static final String PLUGIN_NAME = "RenderHtml5";

	@Autowired
	AemConfig aemConfig;

	// Creation of TraditionalxxxxxService is redirected through these lambdas so that we can replace them with mocks during unit testing.
	private static final Supplier<DocumentFactory> docFactorySupplier = SimpleDocumentFactoryImpl::getFactory;

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		
		ExampleHtml5PluginInputParameters params = ExampleHtml5PluginInputParameters.from(dataSources, docFactorySupplier.get());
		
		try {
			final AemServerType serverType = AemServerType.StandardType.from(aemConfig.serverType().toString()).get();
			Html5FormsService html5Service = Html5FormsService.builder()
															  .machineName(aemConfig.host())
															  .port(aemConfig.port())
															  .basicAuthentication(aemConfig.username(), aemConfig.secret())
															  .useSsl(false)
															  .aemServerType(serverType)
															  // Formsfeeder acts as reverse proxy for AEM, so this fixes up URLs to match the proxied location.
															  .addRenderResultFilter(StandardFormsFeederUrlFilters.getStandardInputStreamFilter(serverType))	
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
			throw new FeedConsumerInternalErrorException("Error while reading rendered Html.(" + (msg == null ? e.getClass().getName() : msg) + ").", e);
		}
	}

	@Override
	public String name() {
		return PLUGIN_NAME;
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
