package com._4point.aem.formsfeeder.server;

import java.io.IOException;
import java.util.Objects;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com._4point.aem.formsfeeder.server.support.CorrelationId;

@Provider
public class CorsResponseFilter implements ContainerResponseFilter {

	private static final String FF_ENABLE_CORS_PROPERTY = "formsfeeder.enable_cors";
	private static final String FF_CORS_ALLOWED_HEADERS_PROPERTY = "formsfeeder.cors_add_headers";
	private static final String ALLOWED_METHODS = String.join(", ", "GET", "POST", "OPTIONS", "HEAD");
	private static final String ALLOWED_HEADERS = String.join(", ", "origin", "content-type", "accept", "authorization", CorrelationId.CORRELATION_ID_HDR);
	
	@Autowired
	Environment environment;
	
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		String corsProperty = Objects.requireNonNull(environment).getProperty(FF_ENABLE_CORS_PROPERTY);
		if (null != corsProperty) {
			responseContext.getHeaders().add("Access-Control-Allow-Origin", corsProperty);
			responseContext.getHeaders().add("Access-Control-Allow-Methods", ALLOWED_METHODS);
			responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
			String corsAddHeaders = Objects.requireNonNull(environment).getProperty(FF_CORS_ALLOWED_HEADERS_PROPERTY);
			responseContext.getHeaders().add("Access-Control-Allow-Headers", ALLOWED_HEADERS + (corsAddHeaders != null && !corsAddHeaders.isBlank() ? ", " + corsAddHeaders : ""));
		}
	}
}