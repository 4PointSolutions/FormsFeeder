package com._4point.aem.formsfeeder.server.support;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.formsfeeder.server.support.ProcessingMetadata.ProcessingMetadataBuilder;

class ProcessingMetadataTest {

	private static final String TEST_CORRELATION_ID = "TestCorrelationId";
	private static final int SLEEP_TIME = 50;
	private static final int TEST_TRANSACTION_SIZE = 23;

	@Test
	void testStart_WithArgs() throws InterruptedException {
		
		Instant testStart = Instant.now();
		Thread.sleep(SLEEP_TIME);
		ProcessingMetadataBuilder metadataBuilder = ProcessingMetadata.start(TEST_CORRELATION_ID);
		Thread.sleep(SLEEP_TIME);
		metadataBuilder.setTransactionSize(TEST_TRANSACTION_SIZE);
	
		ProcessingMetadata result = metadataBuilder.finish();
		Thread.sleep(SLEEP_TIME);
		Instant testEnd = Instant.now();

		System.out.println(result.getFormattedElapsedTime());
		
		assertAll(
				()->assertEquals(TEST_CORRELATION_ID, result.getCorrelationId()),
				()->assertTrue(result.getElapsedTimeMs() >= SLEEP_TIME),
				()->assertEquals(TEST_TRANSACTION_SIZE, result.getTransactionSize()),
				()->assertTrue(testStart.isBefore(result.getStartTime())),
				()->assertTrue(testEnd.isAfter(result.getStartTime())),
				()->assertTrue(testStart.isBefore(result.getEndTime())),
				()->assertTrue(testEnd.isAfter(result.getEndTime())),
				()->assertFalse(result.getFormattedElapsedTime().isEmpty() || result.getFormattedElapsedTime().isBlank())
				);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" "})
	void testStart_EmptyArgs(String correlationId) throws InterruptedException {
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->ProcessingMetadata.start(correlationId));
		assertTrue(ex.getMessage().contains("correlationId"));
	}
}
