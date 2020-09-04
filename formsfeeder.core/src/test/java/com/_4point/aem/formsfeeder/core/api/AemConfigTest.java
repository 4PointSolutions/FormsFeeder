package com._4point.aem.formsfeeder.core.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.formsfeeder.core.api.AemConfig.Protocol;

class AemConfigTest {

	@ParameterizedTest
	@ValueSource(strings = {"HTTP", "https"})
	void testUrl(final String protocol) {
		final String expectedHost = "TestHost";
		final int expectedPort = 23;
		final AemConfig underTest = new AemConfig() {

			@Override
			public String host() {
				return expectedHost;
			}

			@Override
			public int port() {
				return expectedPort;
			}

			@Override
			public String username() {
				return null;
			}

			@Override
			public String secret() {
				return null;
			}

			@Override
			public Protocol protocol() {
				return Protocol.from(protocol);
			}
			
		};
		
		assertEquals(protocol.toLowerCase() + "://" + expectedHost + ":" + Integer.toString(expectedPort) + "/", underTest.url());
	}

	@ParameterizedTest
	@ValueSource(strings = {"HTTP", "http", "HttP"})
	void testProtocol_Http(String testString) {
		assertEquals(Protocol.HTTP, Protocol.from(testString));
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"HTTPS", "https", "HttPs"})
	void testProtocol_Https(String testString) {
		assertEquals(Protocol.HTTPS, Protocol.from(testString));
	}

	@ParameterizedTest
	@ValueSource(strings = {"ftp", "foobar", "Httpf"})
	@NullAndEmptySource
	void testProtocol_Invalid(String testString) {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->Protocol.from(testString));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertTrue(msg.contains("Invalid protocol string"), "Expected '" + msg + "' to contain 'Invalid protocol string'"),
				()->assertTrue(msg.contains(testString != null ? testString : "null"), "Expected '" + msg + "' to contain '" + (testString != null ? testString : "null") + "'")
				);
	}
}
