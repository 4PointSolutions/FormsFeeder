package com._4point.aem.formsfeeder.plugins.encrypt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerBadRequestException;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;

class BcryptEncoderTest {
	private static final String FEED_CONSUMER_NAME = "BCryptEncoder";

	private static final String PASSWORD_PARAM_NAME = "password";
	private static final String RESULT_PARAM_NAME = "encodedValue";

	private static final PasswordEncoder ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	private BcryptEncoder underTest = new BcryptEncoder();
	
	@Test
	void testAccept() throws Exception {
		String passwordString = "passwordValue";
		
		DataSourceList resultDsl = underTest.accept(DataSourceList.build(b->b.add(PASSWORD_PARAM_NAME, passwordString)));
		
		assertNotNull(resultDsl);
		var resultD12r = resultDsl.deconstructor();
		String result = resultD12r.getStringByName(RESULT_PARAM_NAME).orElseThrow(()->new NoSuchElementException("Target plugin didn't return an '" + RESULT_PARAM_NAME + "' value."));
		System.out.println(result);
		assertTrue(ENCODER.matches(passwordString, result), "Returned hash does not match the password passed in.");
	}

	@Test
	void testNoPasswordParameter() throws Exception {
		String passwordString = "passwordValue";
		
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(DataSourceList.build(b->b.add("NotPassword", passwordString))));

		String msg = ex.getMessage();
		assertNotNull(msg);
		assertEquals("'" + PASSWORD_PARAM_NAME + "' parameter was not supplied.", msg);
	}

	@Test
	void testName() {
		assertEquals(FEED_CONSUMER_NAME, underTest.name(), "Expected the plugin name to match the expected name (" + FEED_CONSUMER_NAME + ").");
	}

}
