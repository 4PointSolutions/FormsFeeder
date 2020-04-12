package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class MimeTypeTest {

	private enum ValidTestScenario {
		NORMAL("application/xml", "application", "xml", null),
		NORMAL_WITH_CHARSET("application/xml; charset=utf-8", "application", "xml", StandardCharsets.UTF_8);
		
		private final String testValue;
		private final String expectedType;
		private final String expectedSubType;
		private final Charset expectedCharset;
		
		private ValidTestScenario(String testValue, String expectedType, String expectedSubType,
				Charset expectedCharset) {
			this.testValue = testValue;
			this.expectedType = expectedType;
			this.expectedSubType = expectedSubType;
			this.expectedCharset = expectedCharset;
		}

		public final String getTestValue() {
			return testValue;
		}
		public final String getExpectedType() {
			return expectedType;
		}
		public final String getExpectedSubType() {
			return expectedSubType;
		}
		public final Charset getExpectedCharset() {
			return expectedCharset;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testOf_ValidScenarios(ValidTestScenario scenario) {
		MimeType result = MimeType.of(scenario.getTestValue());
		
		assertAll(
				()->assertEquals(scenario.getExpectedType(), result.getType(), "Unexpected result in Type."),
				()->assertEquals(scenario.getExpectedSubType(), result.getSubtype(), "Unexpected result in SubType."),
				()->assertEquals(scenario.getExpectedCharset(), result.getCharset(), "Unexpected result in Charset."),
				()->assertEquals(scenario.getTestValue(), result.asString().toLowerCase(), "Unexpected result.asString() to match input.")
				);
		
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"foo", 
			"application/xml; charset=utf-8; boundary=ffff",
			"application/xml; charset=utf-8 boundary=ffff",
			"application/xml; boundary=ffff"
			})
	void testOf_IllegalArgumentScenarios(String scenario) {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->MimeType.of(scenario));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(scenario), "Expected the exception message to contain '" + scenario + "', but didn't. (" + msg + ")");
	}
	
	@Test
	void testOf_BadCharset() {
		String unsupportedCharset = "UTF-9";
		String scenario = "application/xml; charset=" + unsupportedCharset;
		UnsupportedCharsetException ex = assertThrows(UnsupportedCharsetException.class, ()->MimeType.of(scenario));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(unsupportedCharset), "Expected the exception message to contain '" + unsupportedCharset + "', but didn't. (" + msg + ")");
	}
	
	@Test
	void testOfStringString() {
		assertEquals(MimeType.of("application/xml").asString(), MimeType.of("application","xml").asString());
		assertEquals(MimeType.of("application/xml; charset=ISO-8859-1").asString(), MimeType.of("application","xml", StandardCharsets.ISO_8859_1).asString());
		
		// Test equals() as well
		assertEquals(MimeType.of("application/xml"), MimeType.of("application","xml"));
		assertEquals(MimeType.of("application/xml; charset=ISO-8859-1"), MimeType.of("application","xml", StandardCharsets.ISO_8859_1));

	}
}
