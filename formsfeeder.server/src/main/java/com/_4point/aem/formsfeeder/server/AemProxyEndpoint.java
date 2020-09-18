package com._4point.aem.formsfeeder.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javax.naming.ConfigurationException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/aem")
public class AemProxyEndpoint {
	private final static Logger logger = LoggerFactory.getLogger(AemProxyEndpoint.class);

	AemConfigProperties aemConfig = Objects.requireNonNull(Objects.requireNonNull(Application.getApplicationContext(), "Application Context cannot be null.")
																  .getBean(AemConfigProperties.class), "AemConfigurationProperties cannot be null");
	
	private static final String AEM_APP_PREFIX = "/";
	private Client httpClient;

	private HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(aemConfig.username(), aemConfig.secret());

    public AemProxyEndpoint() {
		super();
    	httpClient = ClientBuilder.newClient().register(feature).register(MultiPartFeature.class);
	}

    @Path("libs/granite/csrf/token.json")
    @GET
    public ChunkedOutput<byte[]> proxyCsrfToken() throws IOException {
    	logger.debug("Proxying GET request. CSFR token");
		WebTarget webTarget = httpClient.target(aemConfig.url())
								.path(AEM_APP_PREFIX + "libs/granite/csrf/token.json");
		logger.debug("Proxying GET request for CSRF token '" + webTarget.getUri().toString() + "'.");
		Response result = webTarget.request()
		   .get();

		final ChunkedInput<byte[]> chunkedInput = result.readEntity(new GenericType<ChunkedInput<byte[]>>() {});
		final ChunkedOutput<byte[]> output = new ChunkedOutput<byte[]>(byte[].class);
		
		new Thread() {
            public void run() {
            	try {
					try (chunkedInput; output) {
					    byte[] chunk;
 
					    while ((chunk = chunkedInput.read()) != null) {
					        output.write(chunk);
					        logger.debug("Returning GET chunk for CSRF token.");
					    }
					}
				} catch (IllegalStateException | IOException e) {
					e.printStackTrace();
				}
            }
        }.start();
		
        logger.debug("Returning GET response for CSRF token.");
		return output;
    }



	/**
     * This function acts as a reverse proxy for anything under clientlibs.  It just forwards
     * anything it receives on AEM and then returns the response.  
     * 
     * @param remainder
     * @return
     * @throws ConfigurationException
     */
    @Path("{remainder : .+}")
    @GET
    public Response proxyGet(@PathParam("remainder") String remainder) {
    	logger.debug("Proxying GET request. remainder=" + remainder);
		WebTarget webTarget = httpClient.target(aemConfig.url())
								.path(AEM_APP_PREFIX + remainder);
		logger.debug("Proxying GET request for target '" + webTarget.getUri().toString() + "'.");
		Response result = webTarget.request()
		   .get();
		
//		System.out.println("Received GET response from target '" + webTarget.getUri().toString() + "'. contentType='" + result.getMediaType().toString() + "'.  transfer-encoding='" + result.getHeaderString("Transfer-Encoding") + "'.");
		logger.debug("Returning GET response from target '" + webTarget.getUri().toString() + "'.");
		
		return Response.fromResponse(result).build();
    }


    @Path("{remainder : .+}")
    @POST
    public Response proxyPost(@PathParam("remainder") String remainder, @HeaderParam("Content-Type") String contentType, InputStream in) {
    	logger.debug("Proxying POST request. remainder=" + remainder);
		WebTarget webTarget = httpClient.target(aemConfig.url())
								.path(AEM_APP_PREFIX + remainder);
		logger.debug("Proxying POST request for target '" + webTarget.getUri().toString() + "'.");
		
		Response result = webTarget.request()
		   .post(Entity.entity(in, contentType));

		if (remainder.contains("af.submit.jsp")) {
			logger.debug("result == null is " + Boolean.valueOf(result == null).toString() + ".");
			MediaType mediaType = result.getMediaType();
			logger.debug("Received POST response from target '" + webTarget.getUri().toString() + "'. contentType='" + (mediaType != null ? mediaType.toString() : "") + "'.  transfer-encoding='" + result.getHeaderString("Transfer-Encoding") + "'.");
		} else {
			logger.debug("Returning POST response from target '" + webTarget.getUri().toString() + "'.");
		}
		
		return Response.fromResponse(result).build();
    }

}
