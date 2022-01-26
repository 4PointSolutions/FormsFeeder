package com._4point.aem.formsfeeder.server;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.server.PluginInvoker.PluginInvokerBadRequestException;
import com._4point.aem.formsfeeder.server.PluginInvoker.PluginInvokerInternalErrorException;
import com._4point.aem.formsfeeder.server.PluginInvoker.PluginInvokerPluginNotFoundException;
import com._4point.aem.formsfeeder.server.PluginInvoker.ResponseData;
import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com._4point.aem.formsfeeder.server.support.DataSourceListJaxRsUtils;
import com._4point.aem.formsfeeder.server.support.DataSourceListJsonUtils;
import com._4point.aem.formsfeeder.server.support.FfLoggerFactory;

/**
 * Class that contains the code for handling plug-in services.
 *
 */
@Path(PluginInvoker.API_V1_PATH)
public class ServicesEndpoint {
	private final static Logger baseLogger = LoggerFactory.getLogger(ServicesEndpoint.class);
	
	// The Remainder is used capture the name of the plug-in to be invoked (i.e. the stuff trailing the API_V1_PATH.
	private static final String PLUGIN_NAME_REMAINDER_PATH = "/{remainder : .+}";
	
	// Data Source name we use to pass in the correlation id.
	private static final String FORMSFEEDER_CORRELATION_ID_DS_NAME = PluginInvoker.FORMSFEEDER_PREFIX + CorrelationId.CORRELATION_ID_HDR;

	// Data Source Name we use to pass in the bytes from a POST body that does not include name. 
	private static final String FORMSFEEDER_BODY_BYTES_DS_NAME = PluginInvoker.FORMSFEEDER_PREFIX + "BodyBytes";

	private final PluginInvoker pluginInvoker;

	@Autowired
	public ServicesEndpoint(PluginInvoker pluginInvoker) {
		super();
		this.pluginInvoker = pluginInvoker;
	}

	/**
	 * Method that gets invoked for GET transactions that request multipart/form-data
	 *  
	 * This converts the query parameters into DataSources and then calls the appropriate plug-in.  It then returns
	 * the results of the plug-in as either a single response (if the plug-in returned just one DataSource) or as a
	 * multipart/form-data response (if the plug-in returned multiple DataSources).
	 * 
	 * @param remainder
	 * @param correlationIdHdr
	 * @param uriInfo
	 * @return
	 */
	@Path(PLUGIN_NAME_REMAINDER_PATH)
	@Produces({MediaType.MULTIPART_FORM_DATA})
	@GET
    public Response invokeNoBodyMultipartFormResponse(@PathParam("remainder") String remainder, @Context HttpHeaders httpHeaders, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @Context UriInfo uriInfo) {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		final Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.info("Recieved GET request to '" + PluginInvoker.API_V1_PATH + "/" + remainder + "' to produce multipart/form-data.");
		final DataSourceList dataSourceList1 = convertQueryParamsToDataSourceList(uriInfo.getQueryParameters().entrySet(), logger);
		final DataSourceList dataSourceList2 = generateFormsFeederDataSourceList(correlationId);
		final DataSourceList dataSourceList3 = convertRequestHeaderParamsToDataSourceList(httpHeaders, logger);
		return invokePluginConvertResponse(remainder, DataSourceList.from(dataSourceList1, dataSourceList2, dataSourceList3), logger, correlationId, ServicesEndpoint::toMultipartFormData);
	}

	/**
	 * Method that gets invoked for POST transactions that contain multipart/form-data.
	 * 
	 * This breaks apart the multipart/form-data into fields, converts the fields to DataSources.  Merges the DataSources
	 * from the query parameters with the field DataSources and then calls the appropriate plug-in.  It then returns
	 * the results of the plug-in as either a single response (if the plug-in returned just one DataSource) or as a
	 * multipart/form-data response (if the plug-in returned multiple DataSources).
	 * 
	 * @param remainder
	 * @param correlationIdHdr
	 * @param uriInfo
	 * @param formData
	 * @return
	 * @throws IOException 
	 */
	@Path(PLUGIN_NAME_REMAINDER_PATH)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({MediaType.MULTIPART_FORM_DATA, "*/*;qs=0.2"})
	@POST
    public Response invokeWithMultipartFormDataBody(@PathParam("remainder") String remainder, @Context HttpHeaders httpHeaders, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @Context UriInfo uriInfo, FormDataMultiPart formData) throws IOException {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		final Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.info("Received " + MediaType.MULTIPART_FORM_DATA + " POST request to '" + PluginInvoker.API_V1_PATH + "/" + remainder + "' to produce multipart/form-data.");
		final DataSourceList dataSourceList1 = DataSourceListJaxRsUtils.asDataSourceList(formData, logger);
		final DataSourceList dataSourceList2 = convertQueryParamsToDataSourceList(uriInfo.getQueryParameters().entrySet(), logger);
		final DataSourceList dataSourceList3 = generateFormsFeederDataSourceList(correlationId);
		final DataSourceList dataSourceList4 = convertRequestHeaderParamsToDataSourceList(httpHeaders, logger);
		return invokePluginConvertResponse(remainder, DataSourceList.from(dataSourceList1, dataSourceList2, dataSourceList3, dataSourceList4), logger, correlationId, ServicesEndpoint::toMultipartFormData);
	}

	/**
	 * Method that gets invoked for POST transactions that contain anything other than multipart/form-data
	 *  
	 * This converts the body of the incoming POST to single DataSources.  It then merges that DataSource with
	 * the DataSource from the query parameters and then calls the appropriate plug-in.  It then returns
	 * the results of the plug-in as either a single response (if the plug-in returned just one DataSource) or as a
	 * multipart/form-data response (if the plug-in returned multiple DataSources).
	 * 
	 * @param remainder
	 * @param httpHeaders
	 * @param correlationIdHdr
	 * @param uriInfo
	 * @param in
	 * @return
	 * @throws IOException
	 */
	@Path(PLUGIN_NAME_REMAINDER_PATH)
	@Consumes(MediaType.WILDCARD)
	@Produces({MediaType.MULTIPART_FORM_DATA})
	@POST
    public Response invokeWithAnyBodyMultipartFormResponse(@PathParam("remainder") String remainder, @Context HttpHeaders httpHeaders, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @Context UriInfo uriInfo, InputStream in) throws IOException {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		final Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		MediaType mediaType = httpHeaders.getMediaType();
		logger.info("Received '" + mediaType.toString() + "' POST request to '" + PluginInvoker.API_V1_PATH + "/" + remainder + "' to produce multipart/form-data.");
		try {
			final ContentDisposition contentDisposition = determineContentDisposition(httpHeaders);
			final DataSourceList dataSourceList1 = DataSourceListJaxRsUtils.asDataSourceList(in, mediaType, contentDisposition, FORMSFEEDER_BODY_BYTES_DS_NAME, logger);
			final DataSourceList dataSourceList2 = convertQueryParamsToDataSourceList(uriInfo.getQueryParameters().entrySet(), logger);
			final DataSourceList dataSourceList3 = generateFormsFeederDataSourceList(correlationId);
			final DataSourceList dataSourceList4 = convertRequestHeaderParamsToDataSourceList(httpHeaders, logger);
			return invokePluginConvertResponse(remainder, DataSourceList.from(dataSourceList1, dataSourceList2, dataSourceList3, dataSourceList4), logger, correlationId, ServicesEndpoint::toMultipartFormData);
		} catch (ContentDispositionHeaderException e) {
			// If we encounter Parse Errors while determining ContentDisposition, it must be a BadRequest.
			logger.error(e.getMessage() + ", Returning \"Bad Request\" status code.", e);
			return PluginInvoker.buildResponse(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN_TYPE), correlationId);
		}
	}

	/**
	 * Method that gets invoked for GET transactions that request application/json
	 *  
	 * This converts the query parameters into DataSources and then calls the appropriate plug-in.  It then returns
	 * the results of the plug-in as either a single response (if the plug-in returned just one DataSource) or as a
	 * multipart/form-data response (if the plug-in returned multiple DataSources).
	 * 
	 * @param remainder
	 * @param correlationIdHdr
	 * @param uriInfo
	 * @return
	 */
	@Path(PLUGIN_NAME_REMAINDER_PATH)
	@Produces({MediaType.APPLICATION_JSON, "*/*;qs=0.8"}) 
	@GET
    public Response invokeNoBodyJsonResponse(@PathParam("remainder") String remainder, @Context HttpHeaders httpHeaders, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @Context UriInfo uriInfo) {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		final Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.info("Recieved GET request to '" + PluginInvoker.API_V1_PATH + "/" + remainder + "' to produce JSON.");
		final DataSourceList dataSourceList1 = convertQueryParamsToDataSourceList(uriInfo.getQueryParameters().entrySet(), logger);
		final DataSourceList dataSourceList2 = generateFormsFeederDataSourceList(correlationId);
		final DataSourceList dataSourceList3 = convertRequestHeaderParamsToDataSourceList(httpHeaders, logger);
		return invokePluginConvertResponse(remainder, DataSourceList.from(dataSourceList1, dataSourceList2, dataSourceList3), logger, correlationId, ServicesEndpoint::toJson);
	}

	/**
	 * Method that gets invoked for POST transactions that contain application/json.
	 * 
	 * This breaks apart the application/json into individual strings, converts the strings to DataSources.  It merges the DataSources
	 * from the query parameters with the json DataSources and then calls the appropriate plug-in.  It then returns
	 * the results of the plug-in as either a single response (if the plug-in returned just one DataSource) or as an
	 * application/json response (if the plug-in returned multiple DataSources).
	 * 
	 * It stores that all scalar json fields are StringDataSource objects.  If the string is encoded in some way
	 * (such as base64 encoded), then it is the plugin's responsibility to decode  it.
	 * 
	 * Any json arrays or dictionaries are converted into DataSourceList objects that are added to the parent DataSourceList.  
	 * 
	 * @param remainder
	 * @param correlationIdHdr
	 * @param uriInfo
	 * @param formData
	 * @return
	 * @throws IOException 
	 */
	@Path(PLUGIN_NAME_REMAINDER_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "*/*;qs=0.3"})
	@POST
    public Response invokeWithJsonBody(@PathParam("remainder") String remainder, @Context HttpHeaders httpHeaders, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @Context UriInfo uriInfo, JsonObject json) throws IOException {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		final Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.info("Received " + MediaType.APPLICATION_JSON + " POST request to '" + PluginInvoker.API_V1_PATH + "/" + remainder + "' to produce JSON.");
		final DataSourceList dataSourceList1 = DataSourceListJsonUtils.asDataSourceList(json, logger);
		final DataSourceList dataSourceList2 = convertQueryParamsToDataSourceList(uriInfo.getQueryParameters().entrySet(), logger);
		final DataSourceList dataSourceList3 = generateFormsFeederDataSourceList(correlationId);
		final DataSourceList dataSourceList4 = convertRequestHeaderParamsToDataSourceList(httpHeaders, logger);
		return invokePluginConvertResponse(remainder, DataSourceList.from(dataSourceList1, dataSourceList2, dataSourceList3, dataSourceList4), logger, correlationId, ServicesEndpoint::toJson);
	}

	/**
	 * Method that gets invoked for POST transactions that contain anything other than multipart/form-data
	 *  
	 * This converts the body of the incoming POST to single DataSources.  It then merges that DataSource with
	 * the DataSource from the query parameters and then calls the appropriate plug-in.  It then returns
	 * the results of the plug-in as either a single response (if the plug-in returned just one DataSource) or as a
	 * multipart/form-data response (if the plug-in returned multiple DataSources).
	 * 
	 * @param remainder
	 * @param httpHeaders
	 * @param correlationIdHdr
	 * @param uriInfo
	 * @param in
	 * @return
	 * @throws IOException
	 */
	@Path(PLUGIN_NAME_REMAINDER_PATH)
	@Consumes(MediaType.WILDCARD)
	@Produces({MediaType.APPLICATION_JSON, "*/*;qs=0.3"})
	@POST
    public Response invokeWithAnyBodyJsonResponse(@PathParam("remainder") String remainder, @Context HttpHeaders httpHeaders, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @Context UriInfo uriInfo, InputStream in) throws IOException {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		final Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		MediaType mediaType = httpHeaders.getMediaType();
		logger.info("Received '" + mediaType.toString() + "' POST request to '" + PluginInvoker.API_V1_PATH + "/" + remainder + "' to produce JSON.");
		try {
			final ContentDisposition contentDisposition = determineContentDisposition(httpHeaders);
			final DataSourceList dataSourceList1 = DataSourceListJaxRsUtils.asDataSourceList(in, mediaType, contentDisposition, FORMSFEEDER_BODY_BYTES_DS_NAME, logger);
			final DataSourceList dataSourceList2 = convertQueryParamsToDataSourceList(uriInfo.getQueryParameters().entrySet(), logger);
			final DataSourceList dataSourceList3 = generateFormsFeederDataSourceList(correlationId);
			final DataSourceList dataSourceList4 = convertRequestHeaderParamsToDataSourceList(httpHeaders, logger);
			return invokePluginConvertResponse(remainder, DataSourceList.from(dataSourceList1, dataSourceList2, dataSourceList3, dataSourceList4), logger, correlationId, ServicesEndpoint::toJson);
		} catch (ContentDispositionHeaderException e) {
			// If we encounter Parse Errors while determining ContentDisposition, it must be a BadRequest.
			logger.error(e.getMessage() + ", Returning \"Bad Request\" status code.", e);
			return PluginInvoker.buildResponse(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN_TYPE), correlationId);
		}
	}

	/**
	 * Determines if there is a plug-in associated with an Url provided and, if so, then invokes that plug-in.  Also
	 * captures any exceptions that a plugin throws and converts it to a response.
	 * 
	 * @param remainder
	 * @param dataSourceList
	 * @param logger
	 * @param correlationId
	 * @param multiReturnConverter
	 * @return
	 */
	private final Response invokePluginConvertResponse(final String remainder, final DataSourceList dataSourceList, final Logger logger, final String correlationId, final BiFunction<DataSourceList, Logger, ResponseData> multiReturnConverter) {
		return PluginInvoker.buildResponse(invokePluginCreateResponse(determineConsumerName(remainder), dataSourceList, logger, multiReturnConverter), correlationId);
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
	protected final ResponseBuilder invokePluginCreateResponse(final String consumerName, final DataSourceList dataSourceList, final Logger logger, final BiFunction<DataSourceList, Logger, ResponseData> multiReturnConverter) {
		try {
			return PluginInvoker.convertToResponseBuilder(pluginInvoker.invokePlugin(consumerName, dataSourceList, logger), logger, (dsl, l)->PluginInvoker.multipleDsHandler(dsl, l, multiReturnConverter), ()->Response.noContent());
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
	 * Converts the incoming Query Parameters into a DataSourceList so that they can be processed by a plug-in
	 * 
	 * @param queryParams
	 * @return
	 */
	private static final DataSourceList convertQueryParamsToDataSourceList(final Collection<Entry<String, List<String>>> queryParams, final Logger logger) {
		Builder builder = DataSourceList.builder();
		for (Entry<String, List<String>> entry : queryParams) {
			String name = entry.getKey();
			for(String value : entry.getValue()) {
				logger.debug("Found Query Parameter '" + name + "'.");
				builder.add(name, value);
			}
		}
		return builder.build();
	}

	/**
	 * For now, this is a nop, however it's here in case we want to map the paths to FeedConsumer names in the future.
	 * 
	 * @param remainder
	 * @return
	 */
	private static final String determineConsumerName(final String remainder) {
		return remainder;
	}

	/**
	 * Determine the contentDisposition from the HTTP Headers of a single body request.
	 * 
	 * @param httpHeaders
	 * @return
	 * @throws ContentDispositionHeaderException 
	 */
	private static ContentDisposition determineContentDisposition(HttpHeaders httpHeaders) throws ContentDispositionHeaderException {
		try {
			List<String> contentDispositions = httpHeaders.getRequestHeader(HttpHeaders.CONTENT_DISPOSITION);
			if (contentDispositions != null && !contentDispositions.isEmpty()) {
				return new ContentDisposition(contentDispositions.get(0));
			} else {
				return null;
			}
		} catch (ParseException e) {
			String msg = "Error while parsing " + HttpHeaders.CONTENT_DISPOSITION + " header (" + e.getMessage() + "). '" + HttpHeaders.CONTENT_DISPOSITION + ": " + httpHeaders.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)+ "'";
			throw new ContentDispositionHeaderException(msg, e);
		}
	}

	/**
	 * Generates a DataSourceList containing variables that are generated by the FormsFeeder server.
	 * 
	 * @param correlationId
	 * @return
	 */
	private static final DataSourceList generateFormsFeederDataSourceList(final String correlationId) {
		return DataSourceList.builder()
				.add(FORMSFEEDER_CORRELATION_ID_DS_NAME, correlationId)
				.build();
	}

	/**
	 * Converts the incoming Request Header Parameters into a DataSourceList so that they can be processed by a plug-in
	 * 
	 * @param httpHeaders
	 * @return
	 */
	private static final DataSourceList convertRequestHeaderParamsToDataSourceList(final HttpHeaders httpHeaders, final Logger logger) {
		Builder builder = DataSourceList.builder();
		for( Entry<String, List<String>> headers : httpHeaders.getRequestHeaders().entrySet()) {
			String key = headers.getKey();
			for (String value : headers.getValue()) {
				logger.debug("HttpHeader->'{}'='{}'.", key, value);
				if (key.toLowerCase().startsWith("formsfeeder_")) {
					logger.debug("Found Header Parameter '{}'='{}'.", key.substring(12), value);
					builder.add(key.substring(12), value);
				}
			}
		}
		return builder.build();
	}
	
	/**
	 * Convert a DataSourceList to MultipartFormData and return that in a ResponseData object. 
	 * 
	 * @param outputs
	 * @param logger
	 * @return
	 */
	private static ResponseData toMultipartFormData(DataSourceList outputs, Logger logger) {
		// Convert DataSourceList to MultipartFormData.
    	FormDataMultiPart responsesData = DataSourceListJaxRsUtils.asFormDataMultipart(outputs);
    	logger.debug("Returning multiple data sources.");
		for (var bp : responsesData.getBodyParts()) {
			logger.debug("Added {} -> {}", bp.getMediaType().toString(), bp.getContentDisposition());
		}
		logger.debug("Responses mediatype='{}'.", responsesData.getMediaType().toString());
		return new ResponseData() {
			
			@Override
			public MediaType mediaType() {
				return responsesData.getMediaType();
			}
			
			@Override
			public Object data() {
				return responsesData;
			}
		};
	}

	/**
	 * Convert a DataSourceList to a Json object and return that in a ResponseData object. 
	 * 
	 * @param outputs
	 * @param logger
	 * @return
	 */
	private static ResponseData toJson(DataSourceList outputs, Logger logger) {
		JsonObject json = DataSourceListJsonUtils.asJson(outputs, logger);
		return new ResponseData() {
			
			@Override
			public MediaType mediaType() {
				return MediaType.APPLICATION_JSON_TYPE;
			}
			
			@Override
			public Object data() {
				return json;
			}
		};
	}
	/**
	 * Exceptions that occur while performing ContentDisposition processing. 
	 *
	 */
	@SuppressWarnings("serial")
	private static class ContentDispositionHeaderException extends Exception {
		private ContentDispositionHeaderException() {
			super();
		}

		private ContentDispositionHeaderException(String message, Throwable cause) {
			super(message, cause);
		}

		private ContentDispositionHeaderException(String message) {
			super(message);
		}

		private ContentDispositionHeaderException(Throwable cause) {
			super(cause);
		}
		
	}
}
