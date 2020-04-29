package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

class FileDataSourceTest {

	private static FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
	
	@Test
	void testFileDataSourcePath() throws Exception {
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePath.htm");
		FileDataSource underTest = new FileDataSource(expectedFilePath);
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals("", underTest.name()),
				()->assertEquals(StandardMimeTypes.TEXT_HTML_TYPE, underTest.contentType()),
				()->assertEquals(0, underTest.attributes().size())
				);
	}

	@Test
	void testFileDataSourcePathString() throws Exception {
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePathString.txt");
		String expectedName = "DataSource Name";
		FileDataSource underTest = new FileDataSource(expectedFilePath, expectedName);
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals(expectedName, underTest.name()),
				()->assertEquals(StandardMimeTypes.TEXT_PLAIN_TYPE, underTest.contentType()),
				()->assertEquals(0, underTest.attributes().size())
				);
	}

	@Test
	void testFileDataSourcePathStringMapOfStringString() throws Exception {
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePathStringMapOfStringString.dat");
		String expectedName = "DataSource Name";
		Map<String, String> expectedAttributes = Map.of("TestAttribute1", "TestAttributeValue1","TestAttribute2", "TestAttributeValue2");

		FileDataSource underTest = new FileDataSource(expectedFilePath, expectedName, expectedAttributes);

		Map<String, String> attributes = underTest.attributes();
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals(expectedName, underTest.name()),
				()->assertEquals(StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE, underTest.contentType()),
				()->assertTrue(attributes.keySet().containsAll(expectedAttributes.keySet()), "Expected all the keys to be returned."),
				()->assertTrue(attributes.values().containsAll(expectedAttributes.values()), "Expected all the values to be returned."),
				()->assertTrue(expectedAttributes.keySet().containsAll(attributes.keySet()), "Expected no extra keys to be returned."),
				()->assertTrue(expectedAttributes.values().containsAll(attributes.values()), "Expected no extra values to be returned.")
				);
	}

	@Test
	void testFileDataSourcePathMimeTypeFileTypeMap() throws Exception {
		final MimeType expectedMimeType = StandardMimeTypes.APPLICATION_PDF_TYPE;
		final String extension = "htm";
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePath." + extension);
		ModifiableFileExtensionsMap map = new ModifiableFileExtensionsMap();
		map.putMapping(expectedMimeType, List.of("html",extension));
		FileDataSource underTest = new FileDataSource(expectedFilePath, map);
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals("", underTest.name()),
				()->assertEquals(expectedMimeType, underTest.contentType()),
				()->assertEquals(0, underTest.attributes().size())
				);
	}

	@Test
	void testFileDataSourcePathStringMimeTypeFileTypeMap() throws Exception {
		final MimeType expectedMimeType = StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE;
		final String extension = "foo";
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePathString." + extension);
		ModifiableFileExtensionsMap map = new ModifiableFileExtensionsMap();
		map.putMapping(StandardMimeTypes.APPLICATION_MSWORD_TYPE, List.of("doc"));	// Dummy entry just so that we're not the first in the list.
		map.putMapping(expectedMimeType, List.of(extension));
		String expectedName = "DataSource Name";
		FileDataSource underTest = new FileDataSource(expectedFilePath, expectedName, map);
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals(expectedName, underTest.name()),
				()->assertEquals(expectedMimeType, underTest.contentType()),
				()->assertEquals(0, underTest.attributes().size())
				);
	}

	@Test
	void testFileDataSourcePathStringMapOfStringStringMimeTypeFileTypeMap() throws Exception {
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePathStringMapOfStringString.html");
		String expectedName = "DataSource Name";
		Map<String, String> expectedAttributes = Map.of("TestAttribute1", "TestAttributeValue1","TestAttribute2", "TestAttributeValue2");
		// In this test case we're passing in a map that doesn't contain the extension we are using (with the expectation we get octet-stream as the resulting contentType).
		ModifiableFileExtensionsMap map = new ModifiableFileExtensionsMap();
		map.putMapping(StandardMimeTypes.APPLICATION_MSWORD_TYPE, List.of("doc"));	// Dummy entry just so that we're not the first in the list.
		map.putMapping(StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE, List.of("foo"));

		FileDataSource underTest = new FileDataSource(expectedFilePath, expectedName, expectedAttributes, map);

		Map<String, String> attributes = underTest.attributes();
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals(expectedName, underTest.name()),
				()->assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, underTest.contentType()),
				()->assertTrue(attributes.keySet().containsAll(expectedAttributes.keySet()), "Expected all the keys to be returned."),
				()->assertTrue(attributes.values().containsAll(expectedAttributes.values()), "Expected all the values to be returned."),
				()->assertTrue(expectedAttributes.keySet().containsAll(attributes.keySet()), "Expected no extra keys to be returned."),
				()->assertTrue(expectedAttributes.values().containsAll(attributes.values()), "Expected no extra values to be returned.")
				);
	}

	@Test
	void testFileDataSourcePathMimeType() throws Exception {
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePathStringMimeType.pdf");
		MimeType expectedMimeType = StandardMimeTypes.TEXT_HTML_TYPE;	// different than the extensions default mime-type
		FileDataSource underTest = new FileDataSource(expectedFilePath, expectedMimeType);
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals("", underTest.name()),
				()->assertEquals(expectedMimeType, underTest.contentType()),
				()->assertEquals(0, underTest.attributes().size())
				);
	}

	@Test
	void testFileDataSourcePathStringMimeType() throws Exception {
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePathStringMimeType.pdf");
		String expectedName = "DataSource Name";
		MimeType expectedMimeType = StandardMimeTypes.TEXT_HTML_TYPE;	// different than the extensions default mime-type
		FileDataSource underTest = new FileDataSource(expectedFilePath, expectedName, expectedMimeType);
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals(expectedName, underTest.name()),
				()->assertEquals(expectedMimeType, underTest.contentType()),
				()->assertEquals(0, underTest.attributes().size())
				);
	}

	@Test
	void testFileDataSourcePathStringMimeTypeMapOfStringString() throws Exception {
		Path expectedFilePath = fs.getPath("ConstructorTests", "testFileDataSourcePathStringMimeTypeMapOfStringString.docx");
		String expectedName = "DataSource Name";
		MimeType expectedMimeType = StandardMimeTypes.TEXT_HTML_TYPE;	// different than the extensions default mime-type
		Map<String, String> expectedAttributes = Map.of("TestAttribute1", "TestAttributeValue1","TestAttribute2", "TestAttributeValue2");

		FileDataSource underTest = new FileDataSource(expectedFilePath, expectedName, expectedMimeType, expectedAttributes);

		Map<String, String> attributes = underTest.attributes();
		assertAll(
				()->assertEquals(expectedFilePath, underTest.filename().get()),
				()->assertEquals(expectedName, underTest.name()),
				()->assertEquals(expectedMimeType, underTest.contentType()),
				()->assertTrue(attributes.keySet().containsAll(expectedAttributes.keySet()), "Expected all the keys to be returned."),
				()->assertTrue(attributes.values().containsAll(expectedAttributes.values()), "Expected all the values to be returned."),
				()->assertTrue(expectedAttributes.keySet().containsAll(attributes.keySet()), "Expected no extra keys to be returned."),
				()->assertTrue(expectedAttributes.values().containsAll(attributes.values()), "Expected no extra values to be returned.")
				);
	}

	@Test
	void testInputStreamOnValidFile() throws Exception {
		byte[] expectedBytes = "Test Data Bytes".getBytes();
		Path expectedFilePath = fs.getPath("InputStreamTests", "testInputStreamOnValidFile.txt");
		Files.createDirectories(expectedFilePath.getParent());
		try (OutputStream os = Files.newOutputStream(expectedFilePath)) {
			os.write(expectedBytes);
		}
		
		FileDataSource underTest = new FileDataSource(expectedFilePath);
		
		try (InputStream is = underTest.inputStream()) {
			assertArrayEquals(expectedBytes, is.readAllBytes());
		}
	}

	@Test
	void testInputStreamOnNonexistentFile() throws Exception {
		Path expectedFilePath = fs.getPath("InputStreamTests", "testInputStreamOnNonexistentFile.txt");
		FileDataSource underTest = new FileDataSource(expectedFilePath);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.inputStream());
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertTrue(msg.toLowerCase().contains("unable to open input stream")),
				()->assertTrue(msg.contains(expectedFilePath.toString()))
				);
	}

	@Test
	void testOutputStream() throws Exception {
		byte[] expectedBytes = "Expected Test Data".getBytes();
		Path expectedFilePath = fs.getPath("OutputStreamTests", "testOutputStream.txt");
		Files.createDirectories(expectedFilePath.getParent());

		FileDataSource underTest = new FileDataSource(expectedFilePath);
		
		try (OutputStream outputStream = underTest.outputStream()) {
			outputStream.write(expectedBytes);
		}
		try (InputStream inputStream = underTest.inputStream()) {
			assertArrayEquals(expectedBytes, inputStream.readAllBytes());
		}
	}

	@Test
	void testOutputStreamOnBadFilename() throws Exception {
		Path expectedFilePath = fs.getPath("OutputStreamTests", "BadDirName", "testOutputStream.txt");
		FileDataSource underTest = new FileDataSource(expectedFilePath);
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.outputStream());
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertTrue(msg.toLowerCase().contains("unable to open output stream"), "Expected message to contain 'unable to open output stream' but was '" + msg + "'."),
				()->assertTrue(msg.contains(expectedFilePath.toString()), "Expected message to contain '" + expectedFilePath.toString() + "' but was '" + msg + "'.")
				);
	}
	
	@Test
	void testFileDataSourceNullPath() throws Exception {
		assertThrows(NullPointerException.class, ()->new FileDataSource(null));
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
		Path expectedFilePath = fs.getPath("OutputStreamTests", "testOutputStreamWhileInputStreamOpen.txt");
		byte[] expectedBytes = "Expected Test Data".getBytes();
		// Create the file used for input.
		Files.createDirectories(expectedFilePath.getParent());
		Files.write(expectedFilePath, expectedBytes);
		
		// Execute test
		FileDataSource underTest = new FileDataSource(expectedFilePath);
		DataSourceTestUtils.openOutputStreamWhileInputStreamOpen(expectedBytes, underTest);
	}

	/**
	 * Shouldn't allow getting an input stream while there is still an output stream open. 
	 * 
	 * @throws Exception
	 */
	@Test
	void testInputStreamWhileOutputStreamOpen() throws Exception {
		Path expectedFilePath = fs.getPath("OutputStreamTests", "testInputStreamWhileOutputStreamOpen.txt");
		Files.createDirectories(expectedFilePath.getParent());

		FileDataSource underTest = new FileDataSource(expectedFilePath);
		DataSourceTestUtils.openInputStreamAndOutputStream(underTest);
	}


}
