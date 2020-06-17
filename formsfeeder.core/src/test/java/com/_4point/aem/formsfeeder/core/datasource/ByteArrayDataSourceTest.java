package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

class ByteArrayDataSourceTest {

	@Test
	void testByteArrayDataSource() throws Exception {
		ByteArrayDataSource underTest = new ByteArrayDataSource();
		try (InputStream inputStream = underTest.inputStream()) {
			assertAll(
					()->assertEquals(0, Jdk8Utils.readAllBytes(inputStream).length),
					()->assertEquals("", underTest.name()),
					()->assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, underTest.contentType()),
					()->assertEquals(0, underTest.attributes().size())
					);
		}
	}

	@Test
	void testByteArrayDataSourceByteArray() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes);
		try (InputStream inputStream = underTest.inputStream()) {
			assertAll(
					()->assertArrayEquals(expectedBytes, Jdk8Utils.readAllBytes(inputStream)),
					()->assertEquals("", underTest.name()),
					()->assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, underTest.contentType()),
					()->assertEquals(0, underTest.attributes().size())
					);
		}
	}

	@Test
	void testByteArrayDataSourceByteArrayString() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		String expectedName = "DataSource Test Name";
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes, expectedName);
		try (InputStream inputStream = underTest.inputStream()) {
			assertAll(
					()->assertArrayEquals(expectedBytes, Jdk8Utils.readAllBytes(inputStream)),
					()->assertEquals(expectedName, underTest.name()),
					()->assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, underTest.contentType()),
					()->assertEquals(0, underTest.attributes().size())
					);
		}
	}

	@Test
	void testByteArrayDataSourceByteArrayStringMapOfStringString() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		String expectedName = "DataSource Test Name";
		Map<String, String> expectedAttributes = Jdk8Utils.mapOf("TestAttribute1", "TestAttributeValue1","TestAttribute2", "TestAttributeValue2");
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes, expectedName, expectedAttributes);
		try (InputStream inputStream = underTest.inputStream()) {
			Map<String, String> attributes = underTest.attributes();
			assertAll(
					()->assertArrayEquals(expectedBytes, Jdk8Utils.readAllBytes(inputStream)),
					()->assertEquals(expectedName, underTest.name()),
					()->assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, underTest.contentType()),
					()->assertTrue(attributes.keySet().containsAll(expectedAttributes.keySet()), "Expected all the keys to be returned."),
					()->assertTrue(attributes.values().containsAll(expectedAttributes.values()), "Expected all the values to be returned."),
					()->assertTrue(expectedAttributes.keySet().containsAll(attributes.keySet()), "Expected no extra keys to be returned."),
					()->assertTrue(expectedAttributes.values().containsAll(attributes.values()), "Expected no extra values to be returned.")
					);
		}
	}

	@Test
	void testByteArrayDataSourceByteArrayStringMimeType() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		String expectedName = "DataSource Test Name";
		MimeType expectedMimeType = StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE;
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes, expectedName, expectedMimeType);
		try (InputStream inputStream = underTest.inputStream()) {
			assertAll(
					()->assertArrayEquals(expectedBytes, Jdk8Utils.readAllBytes(inputStream)),
					()->assertEquals(expectedName, underTest.name()),
					()->assertEquals(expectedMimeType, underTest.contentType()),
					()->assertEquals(0, underTest.attributes().size())
					);
		}
	}

	@Test
	void testByteArrayDataSourceByteArrayStringMimeTypeMapOfStringString() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		String expectedName = "DataSource Test Name";
		MimeType expectedMimeType = StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE;
		Map<String, String> expectedAttributes = Jdk8Utils.mapOf("Test Attribute 1", "Test Attribute Value 1","Test Attribute 2", "Test Attribute Value 2");
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes, expectedName, expectedMimeType, expectedAttributes);
		try (InputStream inputStream = underTest.inputStream()) {
			Map<String, String> attributes = underTest.attributes();
			assertAll(
					()->assertArrayEquals(expectedBytes, Jdk8Utils.readAllBytes(inputStream)),
					()->assertEquals(expectedName, underTest.name()),
					()->assertEquals(expectedMimeType, underTest.contentType()),
					()->assertTrue(attributes.keySet().containsAll(expectedAttributes.keySet()), "Expected all the keys to be returned."),
					()->assertTrue(attributes.values().containsAll(expectedAttributes.values()), "Expected all the values to be returned."),
					()->assertTrue(expectedAttributes.keySet().containsAll(attributes.keySet()), "Expected no extra keys to be returned."),
					()->assertTrue(expectedAttributes.values().containsAll(attributes.values()), "Expected no extra values to be returned.")
					);
		}
	}

	/**
	 * Should be able to write to an output stream and then read the bytes back.
	 * 
	 * @throws Exception
	 */
	@Test
	void testOutputStream() throws Exception {
		byte[] expectedBytes = "Expected Test Data".getBytes();
		ByteArrayDataSource underTest = new ByteArrayDataSource();
		
		try (OutputStream outputStream = underTest.outputStream()) {
			outputStream.write(expectedBytes);
		}
		try (InputStream inputStream = underTest.inputStream()) {
			assertArrayEquals(expectedBytes, Jdk8Utils.readAllBytes(inputStream));
		}
	}
	
	/**
	 * Shouldn't be able to open OutputStream while one or more InputStreams are open. 
	 *
	 * We use recursion to test the counting of input streams.
	 * 
	 * @throws Exception
	 */
	@Test
	void testOutputStreamWhileInputStreamOpen() throws Exception {
		byte[] expectedBytes = "Expected Test Data".getBytes();
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes);
		
		DataSourceTestUtils.openOutputStreamWhileInputStreamOpen(expectedBytes, underTest);
	}

	/**
	 * Shouldn't allow getting an input stream while there is still an output stream open. 
	 * 
	 * @throws Exception
	 */
	@Test
	void testInputStreamWhileOutputStreamOpen() throws Exception {
		ByteArrayDataSource underTest = new ByteArrayDataSource();
		DataSourceTestUtils.openInputStreamAndOutputStream(underTest);
	}


	
	
}
