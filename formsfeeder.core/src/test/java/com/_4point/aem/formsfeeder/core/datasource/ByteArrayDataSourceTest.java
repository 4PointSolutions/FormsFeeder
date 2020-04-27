package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ByteArrayDataSourceTest {

	@Test
	void testByteArrayDataSource() throws Exception {
		ByteArrayDataSource underTest = new ByteArrayDataSource();
		try (InputStream inputStream = underTest.inputStream()) {
			assertEquals(0, inputStream.readAllBytes().length);
		}
	}

	@Test
	void testByteArrayDataSourceByteArray() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes);
		try (InputStream inputStream = underTest.inputStream()) {
			assertArrayEquals(expectedBytes, inputStream.readAllBytes());
		}
	}

	@Test
	void testByteArrayDataSourceByteArrayString() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		String expectedName = "DataSource Test Name";
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes, expectedName);
		try (InputStream inputStream = underTest.inputStream()) {
			assertAll(
					()->assertArrayEquals(expectedBytes, inputStream.readAllBytes()),
					()->assertEquals(expectedName, underTest.name())
					);
		}
	}

	@Test
	void testByteArrayDataSourceByteArrayStringMapOfStringString() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		String expectedName = "DataSource Test Name";
		Map<String, String> expectedAttributes = Map.of("TestAttribute1", "TestAttributeValue1","TestAttribute2", "TestAttributeValue2");
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes, expectedName, expectedAttributes);
		try (InputStream inputStream = underTest.inputStream()) {
			Map<String, String> attributes = underTest.attributes();
			assertAll(
					()->assertArrayEquals(expectedBytes, inputStream.readAllBytes()),
					()->assertEquals(expectedName, underTest.name()),
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
					()->assertArrayEquals(expectedBytes, inputStream.readAllBytes()),
					()->assertEquals(expectedName, underTest.name()),
					()->assertEquals(expectedMimeType, underTest.contentType())
					);
		}
	}

	@Test
	void testByteArrayDataSourceByteArrayStringMimeTypeMapOfStringString() throws Exception {
		byte[] expectedBytes = "Test Data".getBytes();
		String expectedName = "DataSource Test Name";
		MimeType expectedMimeType = StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE;
		Map<String, String> expectedAttributes = Map.of("Test Attribute 1", "Test Attribute Value 1","Test Attribute 2", "Test Attribute Value 2");
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes, expectedName, expectedMimeType, expectedAttributes);
		try (InputStream inputStream = underTest.inputStream()) {
			Map<String, String> attributes = underTest.attributes();
			assertAll(
					()->assertArrayEquals(expectedBytes, inputStream.readAllBytes()),
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
			assertArrayEquals(expectedBytes, inputStream.readAllBytes());
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
		int maxNumOpenInputStreams = 3;
		byte[] expectedBytes = "Expected Test Data".getBytes();
		ByteArrayDataSource underTest = new ByteArrayDataSource(expectedBytes);
		
		openInputStream(0, maxNumOpenInputStreams, underTest);
		
		// If there are no open input streams, we should be able to write to it and read the data back.
		try (OutputStream outputStream = underTest.outputStream()) {
			outputStream.write(expectedBytes);
		}
		try (InputStream resultInputStream = underTest.inputStream()) {
			assertArrayEquals(expectedBytes, resultInputStream.readAllBytes());
		}
	}

	/**
	 * Recursion routine that is used to open many input streams/
	 * 
	 * @param numOpenInputStreams	Number of Input Streams that are currently open
	 * @param maxNumOpenInputStreams	Maximum number we want to open (i.e. the termination of recursion condition)
	 * @param underTest	The ByteArrayOutputStream that is under test
	 * @throws Exception
	 */
	private void openInputStream(int numOpenInputStreams, int maxNumOpenInputStreams, ByteArrayDataSource underTest) throws Exception {
		// If we're less than the max number of InputStreams then open another and recurse.
		if (numOpenInputStreams <= maxNumOpenInputStreams) {
			try (InputStream inputStream = underTest.inputStream()) {
				openInputStream(numOpenInputStreams + 1, maxNumOpenInputStreams, underTest);

			}
		}
		// We'll get here as we're unwinding the stack.  If there are any open InputStreams we should generate an exception.
		if (numOpenInputStreams > 0) {
			IllegalStateException ex = assertThrows(IllegalStateException.class, ()->underTest.outputStream());
			String msg = ex.getMessage();
			assertEquals("cannot open output stream while input stream is open.", msg.toLowerCase());
		}
	}

	/**
	 * Shouldn't allow getting an input stream while there is still an output stream open. 
	 * 
	 * @throws Exception
	 */
	@Test
	void testInputStreamWhileOutputStreamOpen() throws Exception {
		byte[] expectedBytes = "Expected Test Data".getBytes();
		ByteArrayDataSource underTest = new ByteArrayDataSource();
		
		try (OutputStream outputStream = underTest.outputStream()) {
			outputStream.write(expectedBytes);
			IllegalStateException ex = assertThrows(IllegalStateException.class, ()->underTest.inputStream());
			String msg = ex.getMessage();
			assertEquals("cannot open input stream while output stream is open.", msg.toLowerCase());
		}
	}

	
	
}
