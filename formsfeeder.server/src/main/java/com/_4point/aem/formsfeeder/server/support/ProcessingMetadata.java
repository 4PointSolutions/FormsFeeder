package com._4point.aem.formsfeeder.server.support;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains processing metatdata (i.e. data about the processing of this transaction).
 * 
 * Usage:
 *   Call <code>ProcessingMetadata.start()</code> at the start of the transaction, passing in the correlationId.  This returns a Builder object.
 *   Set properties on the Builder as they become known (things like transaction size and adding other entries.
 *   Call <code>ProcessingMetadataBuilder.finish()</code> to signify the conclusion of processing and to get the ProcessingMetadata object. 
 *
 */
public class ProcessingMetadata {
	private final String correlationId;
	private final int transactionSize;
	private final Instant startTime;
	private final Instant endTime;

	private ProcessingMetadata(String correlationId, int transactionSize, Instant startTime) {
		super();
		this.correlationId = correlationId;
		this.transactionSize = transactionSize;
		this.startTime = startTime;
		this.endTime = Instant.now();
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public int getTransactionSize() {
		return transactionSize;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public long getElapsedTimeMs()
	{
		return Duration.between(startTime, endTime).toMillis();
	}
	
	public String getFormattedElapsedTime() {
		long elapsedTimeMs = getElapsedTimeMs();
		long second = (elapsedTimeMs / 1000) % 60;
		long minute = (elapsedTimeMs / (1000 * 60)) % 60;
		long hour   = (elapsedTimeMs / (1000 * 60 * 60)) % 24;
		return String.format("%02d:%02d:%02d.%03d", hour, minute, second, (elapsedTimeMs % 1000));
	}

	public static ProcessingMetadataBuilder start(String correlationId) {
		if (correlationId == null || correlationId.isBlank()) {
			throw new IllegalArgumentException("No correlationId was supplied.");
		}
		return new ProcessingMetadataBuilder(correlationId);
	}
	
	@Override
	public String toString() {
		return "ProcessingMetadata [correlationId=" + correlationId
				+ ", transactionSize=" + transactionSize + ", startTime=" + startTime + ", endTime=" + endTime
				+ "]";
	}

	public static class ProcessingMetadataBuilder {
		private final String correlationId;
		private final Instant startTime;
		private int transactionSize;
		
		private ProcessingMetadataBuilder(String correlationId) {
			super();
			this.correlationId = correlationId;
			this.startTime = Instant.now();
		}

		public ProcessingMetadataBuilder setTransactionSize(int transactionSize) {
			this.transactionSize = transactionSize;
			return this;
		}

		public ProcessingMetadata finish() {
			return new ProcessingMetadata(this.correlationId, this.transactionSize, this.startTime);
		}

	}
}
