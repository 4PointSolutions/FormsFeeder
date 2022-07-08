package com._4point.aem.formsfeeder.server.support;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CorrelationIdTest {
	

	private static final String TEST_CORRELATION_ID_STR = "TestCorrelationId";

	@Test
	void testGenerateString() {
		String result = CorrelationId.generate(TEST_CORRELATION_ID_STR);
		assertEquals(TEST_CORRELATION_ID_STR, result);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " "})
	@DisplayName("Empty, null or blank strings should return new non-empty String.")
	void testGenerateEmptyOrNullString(String testStr) {
		String result = CorrelationId.generate(testStr);
		assertAll(
				()->assertNotNull(result),
				()->assertFalse(result.isEmpty()),
				()->assertFalse(result.isBlank()),
				()->assertNotNull(UUID.fromString(result))
				);
		
	}

	@Test
	void testGenerate() {
		String result = CorrelationId.generate();
		assertAll(
				()->assertNotNull(result),
				()->assertFalse(result.isEmpty()),
				()->assertFalse(result.isBlank()),
				()->assertNotNull(UUID.fromString(result))
				);
	}

}
