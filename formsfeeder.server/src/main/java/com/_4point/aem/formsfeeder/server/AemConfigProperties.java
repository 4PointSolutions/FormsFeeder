package com._4point.aem.formsfeeder.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com._4point.aem.formsfeeder.core.api.AemConfig;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

@Configuration
@EnableEncryptableProperties
@ConfigurationProperties(prefix = "formsfeeder.aem")
public class AemConfigProperties implements AemConfig {

	private String host;
	private Integer port;
	private String username;
	private String secret;
	private String protocol;

	// TODO:  Get these working using the Spring Boot configuration capbilities.
	//        Currently, everything returns a default.
	
	@Override
	public String host() {
		return host == null ? "localhost" : host;
	}

	@Override
	public int port() {
		return port == null ? 4502 : port;
	}

	@Override
	public String username() {
		return username == null ? "admin" : username;
	}

	@Override
	public String secret() {
		return secret == null ? "admin" : secret;
	}

	@Override
	public Protocol protocol() {
		
		return protocol == null ? Protocol.HTTP : Protocol.from(protocol);
	}

}
