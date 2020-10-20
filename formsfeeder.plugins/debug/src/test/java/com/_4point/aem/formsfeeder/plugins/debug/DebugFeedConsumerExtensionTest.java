package com._4point.aem.formsfeeder.plugins.debug;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;

class DebugFeedConsumerExtensionTest {
	private static final String RESULT_DS_NAME = "Message";	// Use the same name for all DSes in result.
	
	private DebugFeedConsumerExtension underTest = new DebugFeedConsumerExtension();
	
	@Test
	void testAccept() throws Exception {
		String STRING_PARAM_NAME = "String Parameter";
		String STRING_PARAM_VALUE = "StringValue";
		String USASCII_STRING_PARAM_NAME = "USASCII String Parameter";
		String USASCII_STRING_PARAM_VALUE = "USASCII StringValue";
		String BYTEARRAY_PARAM_NAME = "ByteArray Parameter";
		byte[] BYTEARRAY_PARAM_VALUE = new byte[0];

		String ATTRIBUTE_1_NAME = "attribute1Name";
		String ATTRIBUTE_1_VALUE = "attribute1Value";
		String DIRECTORY = "directory";
		String FILENAME = "filename.txt";

		final DataSourceList input = DataSourceList.builder()
				.add("formsfeeder:x-correlation-id", "correlationId")	// This should be ignored.
				.add(STRING_PARAM_NAME, STRING_PARAM_VALUE)
				.add(BYTEARRAY_PARAM_NAME, BYTEARRAY_PARAM_VALUE)
				.add(new DataSource() {
					@Override
					public OutputStream outputStream() {
						throw new IllegalStateException("outputStream() is not implemented.");
					}
					
					@Override
					public String name() {
						return USASCII_STRING_PARAM_NAME;
					}
					
					@Override
					public InputStream inputStream() {
						return new ByteArrayInputStream(USASCII_STRING_PARAM_VALUE.getBytes(StandardCharsets.US_ASCII));
					}
					
					@Override
					public Optional<Path> filename() {
						return Optional.of(Paths.get(DIRECTORY, FILENAME));
					}
					
					@Override
					public MimeType contentType() {
						return StandardMimeTypes.TEXT_PLAIN_TYPE;
					}
					
					@Override
					public Map<String, String> attributes() {
						return Map.of(ATTRIBUTE_1_NAME, ATTRIBUTE_1_VALUE);
					}
				})
				.build();
		
		DataSourceList result = underTest.accept(input);

		assertNotNull(result);
		assertEquals(3, result.list().size());
		
		var deconstructor = result.deconstructor();
		var strList = deconstructor.getStringsByName(RESULT_DS_NAME);
		assertEquals(3, strList.size());
		assertAll(
				()->assertTrue(strList.get(0).contains(STRING_PARAM_NAME)),
				()->assertTrue(strList.get(0).contains(STRING_PARAM_VALUE)),
				()->assertTrue(strList.get(1).contains(BYTEARRAY_PARAM_NAME)),
				()->assertTrue(strList.get(1).contains(StandardMimeTypes.APPLICATION_OCTET_STREAM_STR)),
				()->assertTrue(strList.get(2).contains(USASCII_STRING_PARAM_NAME)),
				()->assertTrue(strList.get(2).contains(USASCII_STRING_PARAM_VALUE)),
				()->assertTrue(strList.get(2).contains(ATTRIBUTE_1_NAME)),
				()->assertTrue(strList.get(2).contains(ATTRIBUTE_1_VALUE)),
				()->assertTrue(strList.get(2).contains(DIRECTORY)),
				()->assertTrue(strList.get(2).contains(FILENAME))
				);
	}

	@Test
	void testName() throws Exception {
		assertEquals("Debug", underTest.name());
	}

}
