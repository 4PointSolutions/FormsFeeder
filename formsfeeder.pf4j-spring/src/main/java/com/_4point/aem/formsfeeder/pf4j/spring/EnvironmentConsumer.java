package com._4point.aem.formsfeeder.pf4j.spring;

import org.springframework.core.env.Environment;

/**
 * Implementing this interface in a plug-in allows that plug-in access to the formsfeeder.server Spring Boot configuration.
 *
 */
@FunctionalInterface
public interface EnvironmentConsumer {
	public static final String FORMSFEEDER_ENV_PARAM_PREFIX = "formsfeeder.";
	public static final String FORMSFEEDER_PLUGINS_ENV_PARAM_PREFIX = FORMSFEEDER_ENV_PARAM_PREFIX + "plugins.";
	public static final String AEM_HOST_ENV_PARAM = FORMSFEEDER_ENV_PARAM_PREFIX + "aemHost";
	public static final String AEM_PORT_ENV_PARAM = FORMSFEEDER_ENV_PARAM_PREFIX + "aemPort";
	public static final String AEM_USERNAME_ENV_PARAM = FORMSFEEDER_ENV_PARAM_PREFIX + "aemUsername";
	public static final String AEM_SECRET_ENV_PARAM = FORMSFEEDER_ENV_PARAM_PREFIX + "aemSecret";
	public static final String AEM_USE_SSL_ENV_PARAM = FORMSFEEDER_ENV_PARAM_PREFIX + "aemUseSsl";
	// The following are for backwards compatibility
	public static final String AEM_USE_SSL_PARAM = AEM_USE_SSL_ENV_PARAM;
	public static final String AEM_USERNAME_PARAM = AEM_USERNAME_ENV_PARAM;
	public static final String AEM_SECRET_PARAM = AEM_SECRET_ENV_PARAM;
	
	public void accept(Environment environment);
}
