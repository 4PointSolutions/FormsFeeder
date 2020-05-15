package com._4point.aem.formsfeeder.pf4j.spring;

import org.springframework.core.env.Environment;

/**
 * Implementing this interface in a plug-in allows that plug-in access to the formsfeeder.server Spring Boot configuration.
 *
 */
@FunctionalInterface
public interface EnvironmentConsumer {
	public void accept(Environment environment);
}
