package com._4point.aem.formsfeeder.plugins.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="formfeeder.plugins.mock")
public class MockPluginProperties {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	private String configValue;
	
	public MockPluginProperties() {
	}

	public final String getConfigValue() {
		return configValue;
	}
	
	public final void setConfigValue(String configValue) {
		logger.info("Setting configValue to '" + configValue + "'.");
		this.configValue = configValue;
	}
	
}
