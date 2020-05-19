package com._4point.aem.formsfeeder.pf4j.spring;

import org.springframework.core.env.Environment;

/**
 * Implementing this interface in a plug-in allows that plug-in access to the formsfeeder.server Spring Boot configuration.
 *
 */
@FunctionalInterface
public interface EnvironmentConsumer {
	public static final String FORMSFEEDER_PLUGINS_ENV_PARAM_PREFIX = "formsfeeder.plugins.";
	public static final String AEM_HOST_ENV_PARAM = FORMSFEEDER_PLUGINS_ENV_PARAM_PREFIX + "aemHost";
	public static final String AEM_PORT_ENV_PARAM = FORMSFEEDER_PLUGINS_ENV_PARAM_PREFIX + "aemPort";
	
	public void accept(Environment environment);
}
