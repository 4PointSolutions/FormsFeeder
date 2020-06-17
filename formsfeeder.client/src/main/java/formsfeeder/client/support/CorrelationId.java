package formsfeeder.client.support;

import java.util.UUID;

import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

public class CorrelationId {
	public final static String CORRELATION_ID_HDR = "x-correlation-id";

	// Private constructor to prevent instantiation.
	private CorrelationId() {
		super();
	}

	/**
	 * Generate a CorrelationId if one may already exist.
	 * 
	 * @param existingId - Existing correlation id (maybe).  If this is null, empty or blank then we generate a new correlation id.
	 * @return new or existing correlation id.
	 */
	public static String generate(String existingId) {
		return existingId == null || existingId.isEmpty() || Jdk8Utils.isBlank(existingId) ? generate() : existingId;
	}
	
	/**
	 * Generate a CorrelationId.
	 * 
	 * @return new correlation id.
	 */
	public static String generate() {
		return UUID.randomUUID().toString();
	}
}
