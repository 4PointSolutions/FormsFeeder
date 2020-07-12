package formsfeeder.client;

import static formsfeeder.client.support.DataSourceListJaxRsUtils.asDataSourceList;
import static formsfeeder.client.support.DataSourceListJaxRsUtils.asFormDataMultipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

import formsfeeder.client.support.BuilderImpl;
import formsfeeder.client.support.CorrelationId;
import formsfeeder.client.support.FfLoggerFactory;


public class FormsFeederClient implements FeedConsumer {
	public static final String FORMSFEEDERCLIENT_DATA_SOURCE_NAME = "formsfeeder:server_response";

	private final Logger baseLogger = LoggerFactory.getLogger(this.getClass());
	
	private final WebTarget target;
	private final Supplier<String> correlationIdFn;
	private final String pluginName;
	private final Map<String,Supplier<String>> headerMap;
	private final Map<String,List<Supplier<String>>> queryParams;
	String returnedCorrelationId = null;
	
	private FormsFeederClient(WebTarget target, Map<String,List<Supplier<String>>> queryParams, Supplier<String> correlationIdFn, String pluginName,
							  Map<String, Supplier<String>> headerMap) {
		this.target = target;
		this.queryParams = queryParams;
		this.correlationIdFn = correlationIdFn;
		this.pluginName = pluginName;
		this.headerMap = headerMap;
	}

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FormsFeederClientException {
		try {
			String correlationIdSent = correlationIdFn != null ? CorrelationId.generate(correlationIdFn.get()) : CorrelationId.generate();
			final Logger logger = FfLoggerFactory.wrap(correlationIdSent, baseLogger);
			logger.info("Sending " + dataSources.list().size() + " DataSource to plugin '" + pluginName + "' at '" + target.getUri().toString() + "'.");
			if (logger.isDebugEnabled()) {
				for (DataSource ds : dataSources.list()) {
					logger.debug("  DataSource name='" + ds.name() + 
								 "', content-type='" + ds.contentType().asString() + "'" +
								 (ds.filename().isPresent() ? ", filename='" + ds.filename() + "'." : ".")
								 );
				}
			}

			WebTarget webTarget = target.path(pluginName);
			if(queryParams !=null && !queryParams.isEmpty()) {
				for(Map.Entry<String,List<Supplier<String>>> entry : queryParams.entrySet()) {
					for(Supplier<String> valueSupplier : entry.getValue()) {
						webTarget = webTarget.queryParam(entry.getKey(), valueSupplier.get());
					}
				}
			}
			javax.ws.rs.client.Invocation.Builder invocBuilder = webTarget.request().header(CorrelationId.CORRELATION_ID_HDR, correlationIdSent);
			if(headerMap!=null && !headerMap.isEmpty()) {
				headerMap.keySet().stream().forEach(header -> invocBuilder.header(header, headerMap.get(header).get()));
			}

			// If the list is empty, send a GET instead of a POST
			Response response = dataSources.list().isEmpty() ? invocBuilder.get() : invocBuilder.post(asEntity(asFormDataMultipart(dataSources)));
			
			
			StatusType resultStatus = response.getStatusInfo();
			if (!Family.SUCCESSFUL.equals(resultStatus.getFamily())) {
				String message = "Call to server failed, statusCode='" + resultStatus.getStatusCode() + "', reason='" + resultStatus.getReasonPhrase() + "'.";
				if (response.hasEntity()) {
					InputStream entityStream = (InputStream) response.getEntity();
					message += "\n" + new String(Jdk8Utils.readAllBytes(entityStream), StandardCharsets.UTF_8.name());
				}
				throw new FormsFeederClientException(message);
			}

			String correlationIdReceived = response.getHeaderString(CorrelationId.CORRELATION_ID_HDR);
			if (correlationIdReceived == null || !correlationIdReceived.equals(correlationIdSent)) {
				throw new FormsFeederClientException("Correlation ID sent (" + correlationIdSent + ") does not match Correlation ID recieved (" + correlationIdReceived + ").");
			}
			returnedCorrelationId = correlationIdReceived;	// Store it away in case the caller wants it.

			if (!response.hasEntity()) {
				if (!resultStatus.equals(Response.Status.NO_CONTENT)) {
					throw new FormsFeederClientException("Response was empty but status code was not 'No Content' (statuscode=" + resultStatus.getStatusCode() + ").");
				}
				logger.info("Formsfeeder server returned 0 DataSources.");
				return DataSourceList.emptyList();
			} else {
				DataSourceList returnedList = MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()) ?
													asDataSourceList(response.readEntity(FormDataMultiPart.class), logger) : // Multiple DataSource Response.
													asDataSourceList(response, FORMSFEEDERCLIENT_DATA_SOURCE_NAME, logger) ;// Single DataSource Response
				logger.info("Formsfeeder server returned " + returnedList.list().size() + " DataSources.");
				if (logger.isDebugEnabled()) {
					for (DataSource ds : returnedList.list()) {
						logger.debug("  DataSource name='" + ds.name() + 
								 "', content-type='" + ds.contentType().asString() + "'" +
								 (ds.filename().isPresent() ? ", filename='" + ds.filename() + "'." : ".")
								 );
					}
				}
				return returnedList;
			}
		} catch (IOException e) {
			throw new FormsFeederClientException("Error while reading response from the server.", e);
		} catch (ParseException e) {
			throw new FormsFeederClientException("Error while parsing ContentDisposition header in the response from the server.", e);
		}
	}

	public final String returnedCorrelationId() {
		return returnedCorrelationId;
	}

	private static Entity<FormDataMultiPart> asEntity(FormDataMultiPart requestData) {
		return Entity.entity(requestData, requestData.getMediaType());
	}

	public static Builder builder() {
		return new Builder();
	}
	
	/**
	 * Builds a FeedConsumer (whose implementation is a FormsFeederClient object).
	 *
	 */
	public static class Builder implements formsfeeder.client.support.Builder {
		private BuilderImpl builder = new BuilderImpl();
		private String pluginName = null;

		@Override
		public Builder machineName(String machineName) {
			builder.machineName(machineName);
			return this;
		}

		@Override
		public Builder port(int port) {
			builder.port(port);
			return this;
		}

		@Override
		public Builder useSsl(boolean useSsl) {
			builder.useSsl(useSsl);
			return this;
		}

		@Override
		public Builder contextRoot(String contextRoot) {
			builder.contextRoot(contextRoot);
			return this;
		}

		@Override
		public Builder clientFactory(Supplier<Client> clientFactory) {
			builder.clientFactory(clientFactory);
			return this;
		}

		@Override
		public Map<String, List<Supplier<String>>> getQueryParams() {
			return builder.getQueryParams();
		}

		@Override
		public Builder addQueryParam(String name, List<Supplier<String>> value) {
			builder.addQueryParam(name,value);
			return this;
		}

		@Override
		public Builder addQueryParam(String name, Supplier<String> value) {
			builder.addQueryParam(name,value);
			return this;
		}

		@Override
		public Builder addQueryParam(String name, String value) {
			builder.addQueryParam(name,value);
			return this;
		}

		@Override
		public Builder basicAuthentication(String username, String password) {
			builder.basicAuthentication(username, password);
			return this;
		}

		@Override
		public Builder correlationId(Supplier<String> correlationIdFn) {
			builder.correlationId(correlationIdFn);
			return this;
		}

		@Override
		public Builder addHeader(String header, Supplier<String> value) {
			builder.addHeader(header,value );
			return this;
		}

		@Override
		public Map<String, Supplier<String>> getHeaderMap() {
			return builder.getHeaderMap();
		}
		
		@Override
		public Supplier<String> getCorrelationIdFn() {
			return builder.getCorrelationIdFn();
		}

		@Override
		public WebTarget createLocalTarget() {
			return builder.createLocalTarget();
		}
		
		public Builder plugin(String pluginName) {
			this.pluginName = pluginName;
			return this;
		}
		
		public FormsFeederClient build() {
			return new FormsFeederClient(builder.createLocalTarget(),
										 builder.getQueryParams(),
										 builder.getCorrelationIdFn(), 
										 Objects.requireNonNull(this.pluginName, "Plug-in name must be supplied using plugin() method before build() is called."),
										 builder.getHeaderMap()
			);
		}
	}
	
	@SuppressWarnings("serial")
	public static class FormsFeederClientException extends FeedConsumerException {

		/**
		 * 
		 */
		private FormsFeederClientException() {
			super();
		}

		/**
		 * @param action
		 */
		private FormsFeederClientException(FailureAction action) {
			super(action);
		}

		/**
		 * @param message
		 * @param action
		 */
		private FormsFeederClientException(String message, FailureAction action) {
			super(message, action);
		}

		/**
		 * @param message
		 * @param cause
		 * @param action
		 */
		private FormsFeederClientException(String message, Throwable cause, FailureAction action) {
			super(message, cause, action);
		}

		/**
		 * @param message
		 * @param cause
		 */
		private FormsFeederClientException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * @param message
		 */
		private FormsFeederClientException(String message) {
			super(message);
		}

		/**
		 * @param cause
		 * @param action
		 */
		private FormsFeederClientException(Throwable cause, FailureAction action) {
			super(cause, action);
		}

		/**
		 * @param cause
		 */
		private FormsFeederClientException(Throwable cause) {
			super(cause);
		}
	}
}
