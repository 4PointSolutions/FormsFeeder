package com._4point.aem.formsfeeder.pf4j.spring;

import org.springframework.core.env.Environment;

@FunctionalInterface
public interface EnvironmentConsumer {
	public void accept(Environment environment);
}
