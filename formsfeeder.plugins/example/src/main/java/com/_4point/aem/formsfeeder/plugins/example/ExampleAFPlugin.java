package com._4point.aem.formsfeeder.plugins.example;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.springframework.beans.factory.annotation.Autowired;

import com._4point.aem.docservices.rest_services.client.af.AdaptiveFormsService;
import com._4point.aem.docservices.rest_services.client.af.AdaptiveFormsService.AdaptiveFormsServiceException;
import com._4point.aem.docservices.rest_services.client.helpers.StandardFormsFeederUrlFilters;
import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.api.DocumentFactory;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.MimeType;

@Extension
public class ExampleAFPlugin implements NamedFeedConsumer, ExtensionPoint {

	private static final String PLUGIN_NAME = "RenderAdaptiveForm";

	@Autowired
	AemConfig aemConfig;

	// Creation of TraditionalxxxxxService is redirected through these lambdas so that we can replace them with mocks during unit testing.
	private static final Supplier<DocumentFactory> docFactorySupplier = SimpleDocumentFactoryImpl::getFactory;

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		
		ExampleAFPluginInputParameters params= ExampleAFPluginInputParameters.from(dataSources, docFactorySupplier.get());
		
		try {
			AdaptiveFormsService afService = AdaptiveFormsService.builder()
																  .machineName(aemConfig.host())
																  .port(aemConfig.port())
																  .basicAuthentication(aemConfig.username(), aemConfig.secret())
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
