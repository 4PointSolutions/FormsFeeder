package com._4point.aem.formsfeeder.server;

import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerBadRequestException;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerException;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerInternalErrorException;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.server.pf4j.FeedConsumers;

@Component
public abstract class PluginInvoker {

	// Path that all plug-in services reside under.
	/* package */ static final String API_V1_PATH = "/api/v1";

	@Autowired
	private FeedConsumers feedConsumers;
	
	protected PluginInvoker() {
	}

	protected static interface ResponseData {
		public Object data();
		public MediaType mediaType();
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
	 * @return
	 * @throws PluginInvokerPluginNotFoundException 
	 * @throws PluginInvokerInternalErrorException 
	 * @throws PluginInvokerBadRequestException 
	 */
	protected final DataSourceList invokePlugin(final String consumerName, final DataSourceList dataSourceList, final Logger logger) throws PluginInvokerPluginNotFoundException, PluginInvokerInternalErrorException, PluginInvokerBadRequestException {
		Optional<FeedConsumer> optConsumer = feedConsumers.consumer(consumerName);
		if (optConsumer.isEmpty()) {
			String msg = "Resource '" + API_V1_PATH + "/" + consumerName + "' does not exist.";
			throw new PluginInvokerPluginNotFoundException(msg);
		} else {
			try {
				return invokeConsumer(dataSourceList, optConsumer.get(), logger);
			} catch (FeedConsumerInternalErrorException e) {
				String msg = String.format("Plugin processor experienced an Internal Server Error. (%s)", e.getMessage());
				throw new PluginInvokerInternalErrorException(msg, e);
			} catch (FeedConsumerBadRequestException e) {
				String msg = String.format("Plugin processor detected Bad Request. (%s)", e.getMessage());
				throw new PluginInvokerBadRequestException(msg, e);
			} catch (FeedConsumerException e) {
				String msg = String.format("Plugin processor error. (%s)", e.getMessage());
				throw new PluginInvokerInternalErrorException(msg, e);
			} catch (Exception e) {
				String msg = String.format("Error within Plugin processor. (%s)", e.getMessage());
				throw new PluginInvokerInternalErrorException(msg, e);
			}
		}
	}
	
	/**
	 * Invokes a FeedConsumer provided by a plug-in.
	 * 
	 * @param inputs  List of DataSources will be used as inputs to the FeedConsumer.
	 * @param consumer FeedConsumer (i.e. plugin) to invoke
	 * @param logger  Logger for method to log to.
	 * @return
	 * @throws FeedConsumerException
	 */
	private static final DataSourceList invokeConsumer(final DataSourceList inputs, final FeedConsumer consumer, final Logger logger) throws FeedConsumerException {
		logger.debug("Before calling Plugin");
		DataSourceList accept = consumer.accept(inputs);
		logger.debug("After calling Plugin");
		return accept;
	}
	
	@SuppressWarnings("serial")
	public static class PluginInvokerBadRequestException extends Exception {

		public PluginInvokerBadRequestException() {
		}

		public PluginInvokerBadRequestException(String message) {
			super(message);
		}

		public PluginInvokerBadRequestException(Throwable cause) {
			super(cause);
		}

		public PluginInvokerBadRequestException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	@SuppressWarnings("serial")
	public static class PluginInvokerInternalErrorException extends Exception {

		public PluginInvokerInternalErrorException() {
		}

		public PluginInvokerInternalErrorException(String message) {
			super(message);
		}

		public PluginInvokerInternalErrorException(Throwable cause) {
			super(cause);
		}

		public PluginInvokerInternalErrorException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	@SuppressWarnings("serial")
	public static class PluginInvokerPluginNotFoundException extends Exception {

		public PluginInvokerPluginNotFoundException() {
		}

		public PluginInvokerPluginNotFoundException(String message) {
			super(message);
		}

		public PluginInvokerPluginNotFoundException(Throwable cause) {
			super(cause);
		}

		public PluginInvokerPluginNotFoundException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	

}
