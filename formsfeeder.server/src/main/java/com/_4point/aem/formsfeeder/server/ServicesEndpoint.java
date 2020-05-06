package com._4point.aem.formsfeeder.server;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerException;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.server.pf4j.FeedConsumers;
import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com._4point.aem.formsfeeder.server.support.FfLoggerFactory;

@Path(ServicesEndpoint.SERVICES_PATH)
public class ServicesEndpoint {
	private final static Logger baseLogger = LoggerFactory.getLogger(ServicesEndpoint.class);
	
	/* package */ static final String SERVICES_PATH = "/services";
	private static final String SERVICES_REMAINDER_PATH = "/{remainder : .+}";
	
	@Autowired
	private FeedConsumers feedConsumers;

	@Path(SERVICES_REMAINDER_PATH)
	@GET
    public Response invokeNoBody(@PathParam("remainder") String remainder, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @Context UriInfo uriInfo) {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		final Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.info("Recieved GET request to '" + SERVICES_PATH + "/" + remainder + "'.");
		final DataSourceList dataSourceList = convertQueryParamsToDataSourceList(uriInfo.getQueryParameters().entrySet());
		return invokePlugin(remainder, dataSourceList, logger);
	}

	@Path(SERVICES_REMAINDER_PATH)
	@POST
    public Response invokeWithBody(@PathParam("remainder") String remainder, @HeaderParam("Content-Type") String contentType, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, @Context UriInfo uriInfo, InputStream in) {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		final Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.info("Recieved POST request to '" + SERVICES_PATH + remainder + "'.");
		final DataSourceList dataSourceList = convertMultipartFormDataToDataSourceList(in);
		final DataSourceList dataSourceList2 = convertQueryParamsToDataSourceList(uriInfo.getQueryParameters().entrySet());
		// TODO:  Merge the two dataSourceLists before calling invokePlugin()
		return invokePlugin(remainder, dataSourceList, logger);
	}

	private Response invokePlugin(final String remainder, final DataSourceList dataSourceList, final Logger logger) {
		Optional<FeedConsumer> optConsumer = feedConsumers.consumer(determineConsumerName(remainder));
		if (optConsumer.isEmpty()) {
			// TODO: We're currently relying on the Jersey framework to return a reasonable error message.
			// Unfortunately, it is just barely adequate.  Would like to make it better at some point.
			String msg = "Resource '" + SERVICES_PATH + "/" + remainder + "' does not exist.";
			logger.error(msg);
			throw new NotFoundException(msg);
		} else {
			try {
				return convertToResponse(invokeConsumer(dataSourceList, optConsumer.get()), logger);
			} catch (FeedConsumerException e) {
				throw new InternalServerErrorException("Plugin processor error.", e);
			}
		}
	}
	
	private DataSourceList convertQueryParamsToDataSourceList(final Collection<Entry<String, List<String>>> queryParams) {
		Builder builder = DataSourceList.builder();
		for (Entry<String, List<String>> entry : queryParams) {
			String name = entry.getKey();
			for(String value : entry.getValue()) {
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
	
	private static final DataSourceList convertMultipartFormDataToDataSourceList(final InputStream inputStream) {
		// TODO: Write code to convert MultipartFormData to DataSourceList.
		return DataSourceList.emptyList();
	}
	
	private final DataSourceList invokeConsumer(final DataSourceList inputs, final FeedConsumer consumer) throws FeedConsumerException {
		return consumer.accept(inputs);
	}
	
	private static final Response convertToResponse(final DataSourceList outputs, Logger logger) {
		List<DataSource> dsList = Objects.requireNonNull(outputs, "Plugin returned null DataSourceList!").list();
		if (dsList.isEmpty()) {
			// Nothing in the response, so return no content.
	    	logger.debug("Returning no data sources.");
			return Response.noContent().build();
		} else if (dsList.size() == 1) {
			// One data source, so return the contents in the body of the response.
			// TODO:  Add name and filename to the response
			DataSource dataSource = outputs.list().get(0);
			MediaType mediaType = fromMimeType(dataSource.contentType());
	    	logger.debug("Returning one data source. mediatype='{}'.", mediaType.toString());
			return Response.ok(dataSource.inputStream(), mediaType).build();
		} else { // More than one return.
			// Convert DataSourceList to MultipartFormData.
	    	FormDataMultiPart responsesData = new FormDataMultiPart();
	    	for(var dataSource : outputs.list()) {
	    		responsesData.field(dataSource.name(), dataSource.inputStream(), fromMimeType(dataSource.contentType()));
	    	}
	    	logger.debug("Returning multiple data sources.");
			for (var bp : responsesData.getBodyParts()) {
				logger.debug("Added {} -> {}", bp.getMediaType().toString(), bp.getContentDisposition());
			}
			logger.debug("Responses mediatype='{}'.", responsesData.getMediaType().toString());
			return Response.ok(responsesData, responsesData.getMediaType()).build();
		}
	}
	
	private static final MediaType fromMimeType(MimeType mimeType) {
		Charset charset = mimeType.charset();
		if (charset != null) {
			return new MediaType(mimeType.type(), mimeType.subtype(), charset.name());
		} else {
			return new MediaType(mimeType.type(), mimeType.subtype());
		}
	}
}
