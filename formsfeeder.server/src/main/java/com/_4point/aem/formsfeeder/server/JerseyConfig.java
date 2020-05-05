package com._4point.aem.formsfeeder.server;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		registerJAXRS();
	}

    private void registerJAXRS() {
    	// Additional JAX-RS Features 
    	register(MultiPartFeature.class);
    	register(LoggingFeature.class);
    	
    	// Internal classes that contain JAX-RS Annotations
    	register(ServicesEndpoint.class);
    }
}
