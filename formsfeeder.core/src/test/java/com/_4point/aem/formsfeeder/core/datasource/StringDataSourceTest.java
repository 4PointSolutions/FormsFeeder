package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

class StringDataSourceTest {

	@Test
	void testStringDataSource() throws Exception {
		StringDataSource underTest = new StringDataSource();
		try (InputStream inputStream = underTest.inputStream()) {
			assertAll(
					()->assertEquals(0, Jdk8Utils.readAllBytes(inputStream).length),
					()->assertEquals("", underTest.name()),
					()->assertEquals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE, underTest.contentType()),
					()->assertEquals(0, underTest.attributes().size())
					);
		}
	}

	@Test
	void testStringDataSourceString() throws Exception {
		String expectedString = "Test Data";
		StringDataSource underTest = new StringDataSource(expectedString);
		try (InputStream inputStream = underTest.inputStream()) {
			assertAll(
					()->assertArrayEquals(expectedString.getBytes(StringDataSource.ENCODING), Jdk8Utils.readAllBytes(inputStream)),
					()->assertEquals("", underTest.name()),
					()->assertEquals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE, underTest.contentType()),
					()->assertEquals(0, underTest.attributes().size())
					);
		}
	}

	@Test
	void testStringDataSourceStringString() throws Exception {
		String expectedString = "Test Data";
		String expectedName = "TestName";
		StringDataSource underTest = new StringDataSource(expectedString, expectedName);
		try (InputStream inputStream = underTest.inputStream()) {
			assertAll(
					()->assertArrayEquals(expectedString.getBytes(StringDataSource.ENCODING), Jdk8Utils.readAllBytes(inputStream)),
					()->assertEquals(expectedName, underTest.name()),
					()->assertEquals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE, underTest.contentType()),
					()->assertEquals(0, underTest.attributes().size())
					);
		}
	}

	@Test
	void testStringDataSourceStringStringMapOfStringString() throws Exception {
		String expectedString = "Test Data";
		String expectedName = "TestName";
		Map<String, String> expectedAttributes = Jdk8Utils.mapOf("TestAttribute1", "TestAttributeValue1","TestAttribute2", "TestAttributeValue2");
		StringDataSource underTest = new StringDataSource(expectedString, expectedName, expectedAttributes);
		try (InputStream inputStream = underTest.inputStream()) {
			Map<String, String> attributes = underTest.attributes();
			assertAll(
					()->assertArrayEquals(expectedString.getBytes(StringDataSource.ENCODING), Jdk8Utils.readAllBytes(inputStream)),
					()->assertEquals(expectedName, underTest.name()),
					()->assertEquals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE, underTest.contentType()),
					()->assertTrue(attributes.keySet().containsAll(expectedAttributes.keySet()), "Expected all the keys to be returned."),
					()->assertTrue(attributes.values().containsAll(expectedAttributes.values()), "Expected all the values to be returned."),
					()->assertTrue(expectedAttributes.keySet().containsAll(attributes.keySet()), "Expected no extra keys to be returned."),
					()->assertTrue(expectedAttributes.values().containsAll(attributes.values()), "Expected no extra values to be returned.")
					);
		}
	}

	@Test
	void testContents() throws Exception {
		String expectedString = "Test Data";
		StringDataSource underTest = new StringDataSource(expectedString);
		assertEquals(expectedString, underTest.contents());
	}

	@Test
	void testOutputStream() throws Exception {
		String expectedString = "Expected Test Data";
		StringDataSource underTest = new StringDataSource();
		
		try (OutputStream outputStream = underTest.outputStream()) {
			outputStream.write(expectedString.getBytes(StringDataSource.ENCODING));
		}
		assertEquals(expectedString, underTest.contents());
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
		String expectedString = "Expected Test Data";
		StringDataSource underTest = new StringDataSource(expectedString);

		DataSourceTestUtils.openOutputStreamWhileInputStreamOpen(expectedString.getBytes(StringDataSource.ENCODING), underTest);
	}

	/**
	 * Shouldn't allow getting an input stream while there is still an output stream open. 
	 * 
	 * @throws Exception
	 */
	@Test
	void testInputStreamWhileOutputStreamOpen() throws Exception {
		StringDataSource underTest = new StringDataSource();

		DataSourceTestUtils.openInputStreamAndOutputStream(underTest);
	}

	
}
