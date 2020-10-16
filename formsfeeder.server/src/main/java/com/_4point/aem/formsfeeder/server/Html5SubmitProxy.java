package com._4point.aem.formsfeeder.server;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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

import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com._4point.aem.formsfeeder.server.support.DataSourceListJaxRsUtils;
import com._4point.aem.formsfeeder.server.support.FfLoggerFactory;
import com._4point.aem.formsfeeder.server.support.XmlDataFile;


/**
 * Html5SubmitProxy is the class that handles submissions from HTML5 forms.
 *
 */
@Path("/content/xfaforms/profiles")
public class Html5SubmitProxy {
	private final static Logger baseLogger = LoggerFactory.getLogger(Html5SubmitProxy.class);
	private static final String AEM_URL = "/content/xfaforms/profiles/";
	
	private static final String AEM_APP_PREFIX = "/";
	
	private static final MediaType APPLICATION_PDF_TYPE = new MediaType("application", "pdf");

	private final AemConfig aemConfig = Objects.requireNonNull(Objects.requireNonNull(Application.getApplicationContext(), "Application Context cannot be null.")
																	.getBean(AemConfig.class), "AemConfig cannot be null");

	private Logger logger;
	private Client httpClient;
	private URI redirectLocation;
	
	private String template;	// Template location (as specified in the incoming data) - pass in as formsfeeder: variable
	private String contentRoot;	// Content Root (as specified in the incoming data) - pass in as formsfeeder: variable
	private String submitUrl;	// SubmitUrl (as specified in the incoming data) - pass in as formsfeeder: variable
	
    public Html5SubmitProxy() {
		super();
    	HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(aemConfig.username(), aemConfig.secret());
		httpClient = ClientBuilder.newClient().register(feature).register(MultiPartFeature.class);
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
			
			// TODO: Call the appropriate plugin
			DataSourceList pluginResult = DataSourceList.emptyList();
			// The response DataSourceList should have 0 or 1 entries in it.
			// We do not support compound responses because this response is going back to the user who submitted the form.
			
			ResponseBuilder responseBuilder = !pluginResult.isEmpty() ? DataSourceListJaxRsUtils.asResponseBuilder(pluginResult.get(0), logger) 
																	  : Response.ok("Form submission processed.", MediaType.TEXT_PLAIN_TYPE);
			return buildResponse(responseBuilder, correlationId);
		} else  { 
			logger.debug("AEM returned status code '" + result.getStatus() + "'. Returning response from AEM.");
			return Response.fromResponse(result).build();
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
