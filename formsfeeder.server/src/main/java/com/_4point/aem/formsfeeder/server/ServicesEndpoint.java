package com._4point.aem.formsfeeder.server;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerException;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.server.pf4j.FeedConsumers;
import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com._4point.aem.formsfeeder.server.support.FfLoggerFactory;

@Path(ServicesEndpoint.SERVICES_PATH)
public class ServicesEndpoint {
	private final static Logger baseLogger = LoggerFactory.getLogger(ServicesEndpoint.class);
	
	/* package */ static final String SERVICES_PATH = "/services";
	private static final String SERVICES_REMAINDER_PATH = "/{remainder : .+}";
	
	private static Function<String, Optional<FeedConsumer>> retrieveConsumer = (s)->FeedConsumers.instance().consumer(s);

	@Path(SERVICES_REMAINDER_PATH)
	@GET
    public Response invokeNoParams(@PathParam("remainder") String remainder, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr) {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.info("Recieved GET request to '" + SERVICES_PATH + "/" + remainder + "'.");
		DataSourceList dataSourceList = DataSourceList.emptyList();
		return invokePlugin(remainder, dataSourceList, logger);
	}

	@Path(SERVICES_REMAINDER_PATH)
	@POST
    public Response invokeWithParams(@PathParam("remainder") String remainder, @HeaderParam("Content-Type") String contentType, @HeaderParam(CorrelationId.CORRELATION_ID_HDR) final String correlationIdHdr, InputStream in) {
		final String correlationId = CorrelationId.generate(correlationIdHdr);
		Logger logger = FfLoggerFactory.wrap(correlationId, baseLogger);
		logger.info("Recieved POST request to '" + SERVICES_PATH + remainder + "'.");
		DataSourceList dataSourceList = convertMultipartFormData(in);
		return invokePlugin(remainder, dataSourceList, logger);
	}

	private Response invokePlugin(String remainder, DataSourceList dataSourceList, Logger logger) {
		Optional<FeedConsumer> optConsumer = retrieveConsumer.apply(determineConsumerName(remainder));
		if (optConsumer.isEmpty()) {
			// TODO: We're currently relying on the Jersey framework to return a reasonable error message.
			// Unfortunately, it is just barely adequate.  Would like to make it better at some point.
			throw new NotFoundException("Resource '" + SERVICES_PATH + remainder + "' does not exist.");
		} else {
			try {
				return convertToResponse(invokeConsumer(dataSourceList, optConsumer.get()));
			} catch (FeedConsumerException e) {
				throw new InternalServerErrorException("Plugin processor error.", e);
			}
		}
	}
	
	private static final String determineConsumerName(final String remainder) {
		System.out.println("Consumer Name = '" + remainder + "'.");
		return remainder;
	}
	
	private static final DataSourceList convertMultipartFormData(final InputStream inputStream) {
		// TODO: Write code to convert MultipartFormData to DataSourceList.
		return DataSourceList.emptyList();
	}
	
	private final DataSourceList invokeConsumer(final DataSourceList inputs, final FeedConsumer consumer) throws FeedConsumerException {
		return consumer.accept(inputs);
	}
	
	private static final Response convertToResponse(final DataSourceList outputs) {
		if (Objects.requireNonNull(outputs, "Plugin returned null DataSourceList!").list().isEmpty()) {
			return Response.noContent().build();
		} else {
			// TODO: Convert DataSourceList to MultipartFormData.
			return null;
		}
	}
}
