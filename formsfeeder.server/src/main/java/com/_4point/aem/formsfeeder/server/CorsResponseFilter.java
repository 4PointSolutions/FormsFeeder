package com._4point.aem.formsfeeder.server;

import java.io.IOException;
import java.util.Objects;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@Provider
public class CorsResponseFilter implements ContainerResponseFilter {

	private static final String FF_ENABLE_CORS_PROPERTY = "formsfeeder.enable_cors";
	
	@Autowired
	Environment environment;
	
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		String corsProperty = Objects.requireNonNull(environment).getProperty(FF_ENABLE_CORS_PROPERTY);
		if (null != corsProperty && Boolean.valueOf(corsProperty.toLowerCase())) {
			responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
			responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST");
			// Other possible headers:
			// Not required because authentication is not implemented: responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
			// Not sure if the following will be required: responseContext.getHeaders().add("Access-Control-Allow-Headers","origin, content-type, accept, authorization");
		}
	}
}