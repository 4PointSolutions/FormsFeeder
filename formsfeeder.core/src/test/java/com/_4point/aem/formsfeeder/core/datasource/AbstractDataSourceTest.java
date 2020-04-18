package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AbstractDataSourceTest {

	@Test
	void testAbstractDataSource() {
		var underTest = new TestDataSource();
		
		basicChecks(underTest);
		
		// Specific to this test
		assertTrue(underTest.name().isEmpty());
		assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, underTest.contentType());
		assertTrue(underTest.attributes().isEmpty(), "Expected attributes to be empty.");
		assertTrue(underTest.filename().isEmpty(), "Expected filename to be empty.");
	}

	@Test
	void testAbstractDataSourceString() {
		String expectedName = "expectedName";
		var underTest = new TestDataSource(expectedName);
		
		basicChecks(underTest);
		
		// Specific to this test
		assertEquals(expectedName, underTest.name());
		assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, underTest.contentType());
		assertTrue(underTest.attributes().isEmpty(), "Expected attributes to be empty.");
		assertTrue(underTest.filename().isEmpty(), "Expected filename to be empty.");
	}

	@Test
	void testAbstractDataSourceStringMapOfStringString() {
		String expectedName = "expectedName";
		Map<String, String> expectedAttributes = Map.of("Key1", "Value1", "Key2", "Value2");
		var underTest = new TestDataSource(expectedName, expectedAttributes);
		
		basicChecks(underTest);
		
		// Specific to this test
		assertEquals(expectedName, underTest.name());
		assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, underTest.contentType());
		assertEquals(expectedAttributes, underTest.attributes(), "Expected attributes match the expected attributes.");
		assertTrue(underTest.filename().isEmpty(), "Expected filename to be empty.");
	}

	@Test
	void testAbstractDataSourceStringMimeType() {
		String expectedName = "expectedName";
		MimeType expectedMimeType = MimeType.of("application/x-fake-mime-type");
		var underTest = new TestDataSource(expectedName, expectedMimeType);
		
		basicChecks(underTest);
		
		// Specific to this test
		assertEquals(expectedName, underTest.name());
		assertEquals(expectedMimeType, underTest.contentType());
		assertTrue(underTest.attributes().isEmpty(), "Expected attributes to be empty.");
		assertTrue(underTest.filename().isEmpty(), "Expected filename to be empty.");
	}

	@Test
	void testAbstractDataSourceStringMimeTypeMapOfStringString() {
		String expectedName = "expectedName";
		MimeType expectedMimeType = MimeType.of("application/x-fake-mime-type");
		Map<String, String> expectedAttributes = Map.of("Key1", "Value1", "Key2", "Value2");
		var underTest = new TestDataSource(expectedName, expectedMimeType, expectedAttributes);
		
		basicChecks(underTest);
		
		// Specific to this test
		assertEquals(expectedName, underTest.name());
		assertEquals(expectedMimeType, underTest.contentType());
		assertEquals(expectedAttributes, underTest.attributes(), "Expected attributes match the expected attributes.");
		assertTrue(underTest.filename().isEmpty(), "Expected filename to be empty.");
	}

	@Test
	void testSetters() {
		String expectedName = "expectedName";
		MimeType expectedMimeType = MimeType.of("application/x-fake-mime-type");
		Map<String, String> expectedAttributes = Map.of("Key1", "Value1", "Key2", "Value2");
		var underTest = new TestDataSource(expectedName, expectedMimeType, expectedAttributes);

		underTest.callSetters();

		assertNotEquals(expectedName, underTest.name());
		assertNotEquals(expectedMimeType, underTest.contentType());
		assertEquals(TestDataSource.DIFFERENT_NAME, underTest.name());
		assertEquals(MimeType.of(TestDataSource.DIFFERENT_MIME_TYPE), underTest.contentType());
	}

	private void basicChecks(TestDataSource underTest) {
		assertNotNull(underTest.attributeMap());
		assertNotNull(underTest.attributes());
		assertEquals(underTest.attributeMap(), underTest.attributes());
		assertNotSame(underTest.attributeMap(), underTest.attributes(), "Should not return original map but should return a copy instead.");
		assertNotNull(underTest.name());
		assertNotNull(underTest.contentType());
		assertNotNull(underTest.filename());
	}

	private static class TestDataSource extends AbstractDataSource {

		public static final String DIFFERENT_MIME_TYPE = "application/x-something-different";
		public static final String DIFFERENT_NAME = "DifferentName";

		private TestDataSource() {
			super();
		}

		private TestDataSource(String name, Map<String, String> attributes) {
			super(name, attributes);
		}

		private TestDataSource(String name, MimeType contentType, Map<String, String> attributes) {
			super(name, contentType, attributes);
		}

		private TestDataSource(String name, MimeType contentType) {
			super(name, contentType);
		}

		private TestDataSource(String name) {
			super(name);
		}

		@Override
		public InputStream inputStream() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public OutputStream outputStream() {
			// TODO Auto-generated method stub
			return null;
		}
		
		public void callSetters() {
			this.name(DIFFERENT_NAME);
			this.contentType(DIFFERENT_MIME_TYPE);
		}
	}
	
}
