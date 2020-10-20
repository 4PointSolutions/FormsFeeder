package com._4point.aem.formsfeeder.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.server.PluginInvoker.PluginInvokerBadRequestException;
import com._4point.aem.formsfeeder.server.PluginInvoker.PluginInvokerInternalErrorException;
import com._4point.aem.formsfeeder.server.PluginInvoker.PluginInvokerPluginNotFoundException;
import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com._4point.aem.formsfeeder.server.support.DataSourceListJaxRsUtils;
import com._4point.aem.formsfeeder.server.support.FfLoggerFactory;
import com._4point.aem.formsfeeder.server.support.FormsFeederData;
import com._4point.aem.formsfeeder.server.support.XmlDataFile;


/**
 * Html5SubmitProxy is the class that handles submissions from HTML5 forms.
 *
 */
@Path("/content/xfaforms/profiles")
public class Html5SubmitProxy extends AbstractSubmitProxy {
	private final static Logger baseLogger = LoggerFactory.getLogger(Html5SubmitProxy.class);
	
	private static final String SUBMITTED_DATA_DS_NAME = PluginInvoker.FORMSFEEDER_PREFIX + "SubmittedData";
	private static final String TEMPLATE_DS_NAME = PluginInvoker.FORMSFEEDER_PREFIX + "Template";
	private static final String CONTENT_ROOT_DS_NAME = PluginInvoker.FORMSFEEDER_PREFIX + "ContentRoot";
	private static final String SUBMIT_URL_DS_NAME = PluginInvoker.FORMSFEEDER_PREFIX + "SubmitUrl";
	private static final String REDIRECT_LOCATION_DS_NAME = PluginInvoker.FORMSFEEDER_PREFIX + "RedirectLocation";

	private static final String AEM_URL = "/content/xfaforms/profiles/";
	private static final String AEM_APP_PREFIX = "/";
	private final AemConfig aemConfig = Objects.requireNonNull(Objects.requireNonNull(Application.getApplicationContext(), "Application Context cannot be null.")
																	.getBean(AemConfig.class), "AemConfig cannot be null");

	private Logger logger;
	private final Client httpClient;
	
	private String template;	// Template location (as specified in the incoming data) - pass in as formsfeeder: variable
	private String contentRoot;	// Content Root (as specified in the incoming data) - pass in as formsfeeder: variable
	private String submitUrl;	// SubmitUrl (as specified in the incoming data) - pass in as formsfeeder: variable

	private PluginInvoker pluginInvoker;

	@Autowired
    public Html5SubmitProxy(PluginInvoker pluginInvoker) {
		super();
		this.pluginInvoker = pluginInvoker;
    	HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(aemConfig.username(), aemConfig.secret());
		httpClient = ClientBuilder.newClient().register(feature).register(MultiPartFeature.class);
	}

    @Path("{remainder : .+}")
    @HEAD
    public Response proxySubmitGet() {
    	return Response.ok().build();
    }
	/**
     * This function accepts an HTML5 form submission, calls AEM to decode it and then generates the requested response.
     * 
     * It takes the multipart/form-data post contents, runs them through some transformations then transfers them over to AEM.  AEM responds
     * with the XML data from the form.  We then use that data to generate whatever returns were requested.
     * 
     * @param remainder
     * @return
	 * @throws IOException 
	 * @throws EmailServiceException 
	 * @throws ParserConfigurationException 
     */
    @Path("{remainder : .+}")
    @POST
    public /* ChunkedOutput<byte[]> */ Response proxySubmitPost(@PathParam("remainder") String remainder, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @HeaderParam("Content-Type") String contentType, final FormDataMultiPart inFormData) throws IOException, ParserConfigurationException {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.debug("Proxying HTML5 Form Submit POST request.");
    	
		WebTarget webTarget = httpClient.target(aemConfig.url())
										.path(AEM_URL + remainder);
		logger.info("Proxying Submit HTML5 POST request for target '" + webTarget.getUri().toString() + "'.");
		logger.info("Content-Type= '" + contentType + "'.");
		
		// Transform some of the parts.
    	final Map<String, Function<byte[], byte[]>> fieldFunctions = 		// Create a table of functions that will be called to transform specific fields in the incoming AF submission.
        								Map.of(
        										"submitUrl", 	(su)->{submitUrl = new String(su, StandardCharsets.UTF_8); return null;},	// Squirrel away the submitUrl, and return null so that no submit Url is passed to AEM
        										"template",		(t)->{ this.template = new String(t); return t;},		// Squirrel away a copy
        										"contentRoot",	(cr)->{ this.contentRoot = new String(cr); return cr;}	// Squirrel away a copy
        									  );		// Specify functions that transform parts of the incoming multi-part transaction.
    	FormDataMultiPart outFormData = SubmitProxyUtils.transformFormData(inFormData, fieldFunctions, logger);	// transform any fields with associated functions.

    	// Transfer to AEM
		Response result = webTarget.request()
					   .post(Entity.entity(outFormData, contentType));
		
		logger.debug("AEM Response = " + result.getStatus());
		logger.debug("AEM Response Location = " + result.getLocation());

		// Get back the XML data from AEM
		byte[] xmlData = SubmitProxyUtils.transferFromAem(result, logger);
		logger.debug("AEM Response data retrieved.");
		logger.trace("AEM Response data is '" + new String(xmlData, StandardCharsets.UTF_8) + "'.");
		
		// Generate the various returns and then return them.
		if (result.getStatus() == Response.Status.OK.getStatusCode() && xmlData != null) {
			// Remove the XFA xdp: wrapper.
			byte[] extractedXmlData = XmlDataFile.extractData(xmlData, logger);

			ResponseBuilder pluginResult = extractPluginName(extractedXmlData)	// Extract the plugin name from the xml dat.
												.map(name->invokePluginCreateResponse(name, createInputDsl(extractedXmlData), logger))	// Invoke the plugin and get a ResponseBuilder
												.orElse(Response.status(Response.Status.NOT_FOUND).entity("Plugin name could not be found.").type(MediaType.TEXT_PLAIN_TYPE));;
			
			return buildResponse(pluginResult, correlationId);
		} else  { 
			logger.debug("AEM returned status code '" + result.getStatus() + "'. Returning response from AEM.");
			return Response.fromResponse(result).build();
		}
    }

    private DataSourceList createInputDsl(byte[] xmlData) {
		return DataSourceList.build(b->this.populateInputDsl(b, xmlData));
    }

    private DataSourceList.Builder populateInputDsl(DataSourceList.Builder builder, byte[] xmlData) {
    	return builder.add(SUBMITTED_DATA_DS_NAME, xmlData, StandardMimeTypes.APPLICATION_XML_TYPE)
    				  .add(TEMPLATE_DS_NAME, this.template)
    				  .add(CONTENT_ROOT_DS_NAME, this.contentRoot)
    				  .add(SUBMIT_URL_DS_NAME, this.submitUrl);
    }
    
	private Optional<String> extractPluginName(byte[] xmlData) {
		return FormsFeederData.from(xmlData).flatMap(FormsFeederData::pluginName);
	}

	/**
	 * Determines if there is a plug-in associated with an Url provided and, if so, then invokes that plug-in.  Also
	 * captures any exceptions that a plugin throws and converts it to a response.
	 * 
	 * We're intentionally sparse in the information about exceptions that we return to the client for security reasons.
	 * We just pass back the exception message.  Full details (and a stack trace) are written to the log.  That's where
	 * someone should go in order to get a fuller picture of what the issue is.
	 * 
	 * @param consumerName  Name of the consumer (i.e. plugin) to invoke.
	 * @param dataSourceList  DataSources that are the inputs to the consumer (i.e plugin)
	 * @param logger   Logger for method to log to.
	 * @param multiReturnConverter  Function for converting multiple DataSources into a single Response.  Can be null when multiple DataSource responses is not allowed (for example, when processing a submission).
	 * @return
	 */
	protected final ResponseBuilder invokePluginCreateResponse(final String consumerName, final DataSourceList dataSourceList, final Logger logger) {
		try {
			return convertToResponseBuilder(pluginInvoker.invokePlugin(consumerName, dataSourceList, logger), logger);
		} catch (PluginInvokerPluginNotFoundException e) {
			String msg = e.getMessage();
			logger.error(msg + " Returning \"Not Found\" status code.");
			return Response.status(Response.Status.NOT_FOUND).entity(msg).type(MediaType.TEXT_PLAIN_TYPE);
		} catch (PluginInvokerInternalErrorException e) {
			String msg = e.getMessage();
			logger.error(msg + ", Returning \"Internal Server Error\" status code.", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).type(MediaType.TEXT_PLAIN_TYPE);
		} catch (PluginInvokerBadRequestException e) {
			String msg = e.getMessage();
			logger.error(msg + ", Returning \"Bad Request\" status code.", e);
			return Response.status(Response.Status.BAD_REQUEST).entity(msg).type(MediaType.TEXT_PLAIN_TYPE);
		}
	}
	
	/**
	 * Converts the DataSourceList returned by a plug-in to a Response that will get sent back to the client.
	 * 
	 * Note: The assumption here is that the client is a user using a browser, so we return something (rather than no content) even if there is nothing in the output DataSourceList
	 * 
	 * @param outputs  List of DataSources that will be returned in the Response.
	 * @param logger   Logger for method to log to.
	 * @param multiReturnConverter  Function for converting multiple DataSources into a single Response.  Can be null when multiple DataSource responses is not allowed (for example, when processing a submission).
	 * @return
	 */
	private static final ResponseBuilder convertToResponseBuilder(final DataSourceList outputs, final Logger logger) {
		try {
			List<DataSource> dsList = Objects.requireNonNull(outputs, "Plugin returned null DataSourceList!").list();
			if (dsList.isEmpty()) {
				// Nothing in the response, so return OK and a test message so that the .
				logger.debug("Returning thank you message because there were no datasources in the output.");
				return Response.ok("Form submission processed.", MediaType.TEXT_PLAIN_TYPE);
			} else if (dsList.size() == 1 && !dsList.get(0).contentType().equals(StandardMimeTypes.APPLICATION_VND_4POINT_DATASOURCELIST_TYPE)) {
				Optional<String> redirectUrl = outputs.deconstructor().getStringByName(REDIRECT_LOCATION_DS_NAME);
				if (redirectUrl.isPresent()) {
					// The plugin is asking for a redirect.
					return Response.seeOther(new URI(redirectUrl.get()));
				} else {
					// One data source that is not a DataSourceList and not a redirect, so return the contents in the body of the response.
					return DataSourceListJaxRsUtils.asResponseBuilder(dsList.get(0), logger);
				}
			} else { // More than one return or a single DataSourceList return.
				String msg = "Plugin returned multiple datasources which cannot be turned into a valid response.";
				logger.error(msg);
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).type(MediaType.TEXT_PLAIN_TYPE);
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid Redirect URI returned from plugin.",e);
		}
	}

	

	
	/**
	 * Build a response from a ResponseBuilder.  This is mainly to make sure that all responses contain the correlationId in them.
	 * 
	 * @param builder
	 * @param correlationId
	 * @return
	 */
	private static final Response buildResponse(final ResponseBuilder builder, final String correlationId) {
		builder.header(CorrelationId.CORRELATION_ID_HDR, correlationId);
		return builder.build();
	}

}
