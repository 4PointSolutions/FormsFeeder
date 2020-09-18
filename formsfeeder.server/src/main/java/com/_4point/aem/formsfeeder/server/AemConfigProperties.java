package com._4point.aem.formsfeeder.server;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.pf4j.spring.EnvironmentConsumer;

// TODO:  Get these working using the Spring Boot encrypted configuration capabilities.
//@Configuration
//@EnableEncryptableProperties
//@ConfigurationProperties(prefix = "formsfeeder.aem")
@Component
public class AemConfigProperties implements AemConfig, EnvironmentAware {
	private final static Logger logger = LoggerFactory.getLogger(AemConfigProperties.class);

	private Environment environment;
	
	private String host;
	private Integer port;
	private String username;
	private String secret;
	private Protocol protocol;

	@Override
	public String host() {
		if (host == null) {
			host = Objects.requireNonNull(environment, "Environment has not been populated!").getRequiredProperty(EnvironmentConsumer.AEM_HOST_ENV_PARAM); 
		}
		return host;
	}

	@Override
	public int port() {
		if (port == null) {
			port = Integer.valueOf(Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_PORT_ENV_PARAM, "4502")); 
		}
		return port;
	}

	@Override
	public String username() {
		if (username == null) {
			username = Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_USERNAME_ENV_PARAM, "admin"); 
		}
		return username;
	}

	@Override
	public String secret() {
		if (secret == null) {
			secret = Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_SECRET_ENV_PARAM, "admin"); 
		}
		return secret;
	}

	@Override
	public Protocol protocol() {
		if (protocol == null) {
			String protocolStr = Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_USE_SSL_ENV_PARAM);
			protocol = (protocolStr == null || protocolStr.isBlank()) ? Protocol.HTTP : Protocol.from(protocolStr); 
		}
		return protocol;
	}

	@Override
	public void setEnvironment(Environment environment) {
		logger.debug("Initializing Environment Variable in AemConfig. Environment is " + (environment == null ? "" : "not ") + "null.");
		this.environment = environment;
	}

}
