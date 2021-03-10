package com._4point.aem.formsfeeder.plugins.example;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.springframework.beans.factory.annotation.Autowired;

import com._4point.aem.docservices.rest_services.client.forms.RestServicesFormsServiceAdapter;
import com._4point.aem.docservices.rest_services.client.helpers.AemServerType;
import com._4point.aem.docservices.rest_services.client.output.RestServicesOutputServiceAdapter;
import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.api.DocumentFactory;
import com._4point.aem.fluentforms.api.forms.FormsService;
import com._4point.aem.fluentforms.api.forms.FormsService.FormsServiceException;
import com._4point.aem.fluentforms.api.output.OutputService;
import com._4point.aem.fluentforms.api.output.OutputService.OutputServiceException;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.fluentforms.impl.UsageContext;
import com._4point.aem.fluentforms.impl.forms.FormsServiceImpl;
import com._4point.aem.fluentforms.impl.forms.TraditionalFormsService;
import com._4point.aem.fluentforms.impl.output.OutputServiceImpl;
import com._4point.aem.fluentforms.impl.output.TraditionalOutputService;
import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.MimeType;

/**
 * This is an example plug-in that demonstrates how to call AEM from a plug-in.
 *
 */
/**
 * This is the extension that is called by the FormsFeeder server.
 *
 */
@Extension
public class ExamplePdfPlugin implements NamedFeedConsumer, ExtensionPoint {

	private static final String FEED_CONSUMER_NAME = "RenderPdf";
	
	private static final Supplier<DocumentFactory> docFactorySupplier = SimpleDocumentFactoryImpl::getFactory; 
	
	@Autowired
	AemConfig aemConfig;

	// The services get created on first use and then cached here.
	private FormsService formsService;
	private OutputService outputService;
	
	// Creation of TraditionalxxxxxService is redirected through these lambdas so that we can replace them with mocks during unit testing.
	private Supplier<TraditionalFormsService> tradFormsServiceSupplier = this::createRestServicesTraditionalFormsService;		// Replaced for unit testing.
	private Supplier<TraditionalOutputService> tradOutputServiceSupplier = this::createRestServicesTraditionalOutputService;	// Replaced for unit testing.

	/**
	 *  Implementation of the NamedFeedConsumer accept() method.
	 *  
	 *  This is the main method called by the FormsFeeder server to invoke this plug-in. 
	 *
	 */
	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		// Parse out the arguments to this plug-in.
		ExamplePluginInputParameters params = ExamplePluginInputParameters.from(dataSources, docFactorySupplier.get());

		// Call an AEM Server
		try {
			Document result;
			if (params.isInteractive()) {
				result = formsService().renderPDFForm()
									   .executeOn(params.getTemplate(), params.getData());
			} else {
				result = outputService().generatePDFOutput()
										.executeOn(params.getTemplate(), params.getData());
			}
		
			// return the result 
			return DataSourceList.builder()
								 .add("Result", result.getInputStream().readAllBytes(), MimeType.of(result.getContentType()))
								 .build();
		} catch (OutputServiceException e) {
			String msg = e.getMessage();
			throw new FeedConsumerInternalErrorException("Error while generating PDF (" + (msg == null ? e.getClass().getName() : msg) + ").", e);
		} catch (IOException e) {
			String msg = e.getMessage();
			throw new FeedConsumerInternalErrorException("Error while reading generating PDF (" + (msg == null ? e.getClass().getName() : msg) + ").", e);
		} catch (FormsServiceException e) {
			String msg = e.getMessage();
			throw new FeedConsumerInternalErrorException("Error while rendering PDF (" + (msg == null ? e.getClass().getName() : msg) + ").", e);
		}
	}

	/**
	 * Instantiate the AEM Forms FormsService and cache it.
	 * 
	 * @return AEM FormsService
	 */
	private FormsService formsService() {
		if (this.formsService == null) {
			this.formsService = new FormsServiceImpl(tradFormsServiceSupplier().get(), UsageContext.CLIENT_SIDE);
		}
		return this.formsService;
	}

	/**
	 * Instantiate a rest-services.client RestServicesFormsServiceAdapter.
	 * 
	 * This is called via the tradFormsServiceSupplier member, if that member's value has not been overridden.
	 * 
	 * @return
	 */
	private TraditionalFormsService createRestServicesTraditionalFormsService() {
		TraditionalFormsService formsAdapter = RestServicesFormsServiceAdapter.builder()
					.machineName(aemConfig.host())
					.port(aemConfig.port())
					.basicAuthentication(aemConfig.username(), aemConfig.secret())
					.useSsl(false)
					.aemServerType(AemServerType.StandardType.from(aemConfig.serverType().toString()).get())
					.build();
		return formsAdapter;
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
					.useSsl(false)
					.aemServerType(AemServerType.StandardType.from(aemConfig.serverType().toString()).get())
					.build();
		return outputAdapter;
	}
	
	/**
	 * Getter for tradFormsServiceSupplier function.
	 * 
	 * @return
	 */
	private final Supplier<TraditionalFormsService> tradFormsServiceSupplier() {
		return tradFormsServiceSupplier;
	}

	/**
	 * Setter for tradFormsServiceSupplier lambda.
	 * 
	 * Used for unit testing to replace the call to REST services with a mock object.
	 * 
	 * @return
	 */
	/* package */ final void tradFormsServiceSupplier(Supplier<TraditionalFormsService> tradFormsServiceSupplier) {
		this.tradFormsServiceSupplier = tradFormsServiceSupplier;
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

	/**
	 * Name Getter from NamedFeedConsumer, used to get the name of the Feed Consumer.
	 * 
	 * This name is matched to the URL of incoming requests.  If the URL matches this name, then this plug-in is called.
	 */
	@Override
	public String name() {
		return FEED_CONSUMER_NAME;
	}

	/**
	 * Internal object used for deconstructing the incoming DataSourceList into individual Java objects. 
	 *
	 * It has package visibility so that it can be unit tested separately.
	 */
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
			Document templateDoc = docFactory.create(Paths.get(deconstructor.getStringByName(TEMPLATE_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + TEMPLATE_PARAM_NAME + "' Parameter must be supplied."))));
			Document dataDoc = docFactory.create(Paths.get(deconstructor.getStringByName(DATA_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + DATA_PARAM_NAME + "' Parameter must be supplied."))));
			Boolean interactiveBool = deconstructor.getBooleanByName(INTERACTIVE_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + INTERACTIVE_PARAM_NAME + "' Parameter must be supplied."));
			// Use what we've pulled out to construct the ExampleParameters object.
			return new ExamplePluginInputParameters(templateDoc, dataDoc, interactiveBool);
		}
	}
}
