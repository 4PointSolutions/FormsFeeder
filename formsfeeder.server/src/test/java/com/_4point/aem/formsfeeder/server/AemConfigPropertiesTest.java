package com._4point.aem.formsfeeder.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
class AemConfigPropertiesTest /* implements EnvironmentAware */ {

//	private Environment environment;

	// TODO: This is not really complete.  The core we're testing is not currently wired into the Spring configuration mechanisms.
	//       So all we're checking is that we're getting back defaults.  At some point in the future we need better tests.
	@Autowired
	private AemConfigProperties aemConfig;

	@Test
	void testHost() {
		assertNotNull(aemConfig.host());
	}

	@Test
	void testPort() {
		assertNotNull(aemConfig.port());
	}

	@Test
	void testUsername() {
		assertNotNull(aemConfig.username());
	}

	@Test
	void testSecret() {
		assertNotNull(aemConfig.secret());
	}

	@Test
	void testProtocol() {
		assertNotNull(aemConfig.protocol());
	}

//	@Override
//	public void setEnvironment(Environment environment) {
//		this.environment = environment;
//	}

}
