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
	
	@Override
	public String host() {
		return Objects.requireNonNull(environment, "Environment has not been populated!").getRequiredProperty(EnvironmentConsumer.AEM_HOST_ENV_PARAM); 
	}

	@Override
	public int port() {
		return Integer.valueOf(Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_PORT_ENV_PARAM, "4502")); 
	}

	@Override
	public String username() {
		return Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_USERNAME_ENV_PARAM, "admin"); 
	}

	@Override
	public String secret() {
		return Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_SECRET_ENV_PARAM, "admin"); 
	}

	@Override
	public Protocol protocol() {
		String protocolStr = Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_USE_SSL_ENV_PARAM);
		return (protocolStr == null || protocolStr.isBlank()) ? Protocol.HTTP : Protocol.from(protocolStr).orElseThrow(()->new IllegalArgumentException("Invalid protocol string (" + protocolStr + ").")); 
	}

	@Override
	public AemServerType serverType() {
		String serverTypeStr = Objects.requireNonNull(environment, "Environment has not been populated!").getProperty(EnvironmentConsumer.AEM_SERVER_TYPE_PARAM);
		return (serverTypeStr == null || serverTypeStr.isBlank()) ? AemServerType.OSGI : AemServerType.from(serverTypeStr).orElseThrow(()->new IllegalArgumentException("Invalid server type string (" + serverTypeStr + ").")); 
	}

	@Override
	public void setEnvironment(Environment environment) {
		logger.debug("Initializing Environment Variable in AemConfig. Environment is " + (environment == null ? "" : "not ") + "null.");
		this.environment = environment;
	}
}
