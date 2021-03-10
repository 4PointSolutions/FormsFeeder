package com._4point.aem.formsfeeder.plugins.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.springframework.beans.factory.annotation.Autowired;

import com._4point.aem.docservices.rest_services.client.helpers.AemServerType;
import com._4point.aem.docservices.rest_services.client.output.RestServicesOutputServiceAdapter;
import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.api.DocumentFactory;
import com._4point.aem.fluentforms.api.PathOrUrl;
import com._4point.aem.fluentforms.api.output.OutputService;
import com._4point.aem.fluentforms.api.output.OutputService.OutputServiceException;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.fluentforms.impl.UsageContext;
import com._4point.aem.fluentforms.impl.output.OutputServiceImpl;
import com._4point.aem.fluentforms.impl.output.TraditionalOutputService;
import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;

@Extension
public class ExampleHtml5SubmitPlugin implements NamedFeedConsumer, ExtensionPoint {
	private static final String PLUGIN_NAME = "Html5Submit";

	@Autowired
	AemConfig aemConfig;

	private static final Supplier<DocumentFactory> docFactorySupplier = SimpleDocumentFactoryImpl::getFactory; 
	private OutputService outputService;
	// Creation of TraditionalxxxxxService is redirected through these lambdas so that we can replace them with mocks during unit testing.
	private Supplier<TraditionalOutputService> tradOutputServiceSupplier = this::createRestServicesTraditionalOutputService;	// Replaced for unit testing.

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		InputParameters params = InputParameters.from(dataSources);
			
		try {
			final Document result =  outputService().generatePDFOutput()
													.setContentRoot(PathOrUrl.from(params.contentRoot()))
													.executeOn(PathOrUrl.from(params.template()), docFactorySupplier.get().create(params.xmlData()));

			final byte[] inlineData = result.getInlineData();
			final MimeType mimeType = MimeType.of(result.getContentType());
			
			return DataSourceList.build(b->b.add("output", inlineData, mimeType));
		} catch (OutputServiceException | IOException e) {
			String msg = e.getMessage();
			throw new FeedConsumerInternalErrorException("Error occurred while generating PDF (" + (msg != null ? msg : e.getClass().getName()) + ").", e);
		}
	}

	@Override
	public String name() {
		return PLUGIN_NAME;
	}

	/**
	 * Instantiate the AEM Forms OutputService and cache it.
	 * 
	 * @return AEM OutputService
	 */
	private OutputService outputService() {
		if (this.outputService == null) {
			this.outputService = new OutputServiceImpl(tradOutputServiceSupplier().get(), UsageContext.CLIENT_SIDE);
		}
		return this.outputService;
	}

	/**
	 * Instantiate a rest-services.client RestServicesOutputServiceAdapter.
	 * 
	 * This is called via the tradOutputServiceSupplier member, if that member's value has not been overridden.
	 * 
	 * @return
	 */
	private TraditionalOutputService createRestServicesTraditionalOutputService() {
		TraditionalOutputService outputAdapter = RestServicesOutputServiceAdapter.builder()
					.machineName(aemConfig.host())
					.port(aemConfig.port())
					.basicAuthentication(aemConfig.username(), aemConfig.secret())
					.useSsl(aemConfig.protocol() == AemConfig.Protocol.HTTPS)
					.aemServerType(AemServerType.StandardType.from(aemConfig.serverType().toString()).get())
					.build();
		return outputAdapter;
	}
	
	/**
	 * Getter for tradOutputServiceSupplier function.
	 * 
	 * @return
	 */
	private final Supplier<TraditionalOutputService> tradOutputServiceSupplier() {
		return tradOutputServiceSupplier;
	}

	/**
	 * Setter for tradOutputServiceSupplier lambda.
	 * 
	 * Used for unit testing to replace the call to REST services with a mock object.
	 * 
	 * @return
	 */
	/* package */ final void tradOutputServiceSupplier(Supplier<TraditionalOutputService> tradOutputServiceSupplier) {
		this.tradOutputServiceSupplier = tradOutputServiceSupplier;
	}

	private static class InputParameters {
		private static final String FORMSFEEDER_PREFIX = "formsfeeder:";
		private static final String SUBMITTED_DATA_DS_NAME = FORMSFEEDER_PREFIX + "SubmittedData";
		private static final String TEMPLATE_DS_NAME = FORMSFEEDER_PREFIX + "Template";
		private static final String CONTENT_ROOT_DS_NAME = FORMSFEEDER_PREFIX + "ContentRoot";

		private final InputStream xmlData;
		private final String template;
		private final String contentRoot;

		private InputParameters(InputStream xmlData, String template, String contentRoot) {
			super();
			this.xmlData = xmlData;
			this.template = template;
			this.contentRoot = contentRoot;
		}
		
		public static InputParameters from(DataSourceList dsl) throws FeedConsumerBadRequestException {
			final Deconstructor d11r = dsl.deconstructor();
			DataSource xmlDataDs = d11r.getDataSourceByName(SUBMITTED_DATA_DS_NAME)
									   .orElseThrow(()->new FeedConsumerBadRequestException("No data was provided to the plugin."));
			if (!StandardMimeTypes.APPLICATION_XML_TYPE.equals(xmlDataDs.contentType())) {
				throw new FeedConsumerBadRequestException("Data provided to the plugin was no in XML format.");
			}
			
			String template = d11r.getStringByName(TEMPLATE_DS_NAME)
					   			  .orElseThrow(()->new FeedConsumerBadRequestException("No template was provided to the plugin."));
			String contentRoot  = d11r.getStringByName(CONTENT_ROOT_DS_NAME)
		   			  				  .orElseThrow(()->new FeedConsumerBadRequestException("No content root was provided to the plugin."));
			return new InputParameters(xmlDataDs.inputStream(), template, contentRoot);
		}

		final InputStream xmlData() {
			return xmlData;
		}

		final String template() {
			return template;
		}

		final String contentRoot() {
			return contentRoot;
		}
	}

}
