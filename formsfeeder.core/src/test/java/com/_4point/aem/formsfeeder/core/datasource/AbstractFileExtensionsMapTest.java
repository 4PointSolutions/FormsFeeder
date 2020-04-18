package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com._4point.aem.formsfeeder.core.datasource.AbstractFileExtensionsMap.FileExtensionsEntry;

class AbstractFileExtensionsMapTest {

	private static final MimeType APPLICATION_VND_MS_WORD_MIMETYPE = MimeType.of("application", "vnd.ms-word");
	private static final MimeType APPLICATION_PDF_MIMETYPE = MimeType.of("application", "pdf");
	private static final String DOC_EXTENSION = "doc";
	private static final String PDF_EXTENSION = "pdf";

	public static final TestFileExtensionsMap UNDER_TEST_1 = 
			new TestFileExtensionsMap( new FileExtensionsEntry[] { 
				FileExtensionsEntry.of(APPLICATION_PDF_MIMETYPE.asString(), new String[] { PDF_EXTENSION, "foo" }),
				FileExtensionsEntry.of(APPLICATION_VND_MS_WORD_MIMETYPE.asString(), new String[] {DOC_EXTENSION, "bar"})
			}, "Created with Array Contructor." );

	public static final TestFileExtensionsMap UNDER_TEST_2 = 
			new TestFileExtensionsMap( new FileExtensionsEntry[] { 
				FileExtensionsEntry.of(APPLICATION_PDF_MIMETYPE.asString(), new String[] { PDF_EXTENSION, "foo" }),
				FileExtensionsEntry.of(APPLICATION_VND_MS_WORD_MIMETYPE.asString(), new String[] {DOC_EXTENSION, "bar"})
			}, ArrayList::new, HashMap::new, HashMap::new,
					"Created with Array Contructor and collection suppliers." );

	private static class TestFileExtensionsMap extends AbstractFileExtensionsMap {
		private final String testName;
		public TestFileExtensionsMap(FileExtensionsEntry[] entries, String testName) {
			super(entries);
			this.testName = testName;
		}

		private TestFileExtensionsMap(FileExtensionsEntry[] entries, Supplier<List<FileExtensionsEntry>> listSupplier,
				Supplier<Map<MimeType, FileExtensionsEntry>> mimeMapSupplier,
				Supplier<Map<String, FileExtensionsEntry>> extensionMapSupplier, String testName) {
			super(entries, listSupplier, mimeMapSupplier, extensionMapSupplier);
			this.testName = testName;
		}

		@Override
		public String toString() {
			return testName;
		}

	}

	static Stream<TestFileExtensionsMap> mapProvider() {
		return Stream.of(UNDER_TEST_1, UNDER_TEST_2);
	}
	
	@ParameterizedTest
	@MethodSource("mapProvider")
	void testGetMimeType(TestFileExtensionsMap underTest) {
		assertAll(
				()->assertEquals(APPLICATION_PDF_MIMETYPE, underTest.mimeType(Paths.get("TestDir", "TestFile." + PDF_EXTENSION)).get()),
				()->assertEquals(APPLICATION_VND_MS_WORD_MIMETYPE, underTest.mimeType(Paths.get("TestDir", "TestFile." + DOC_EXTENSION)).get())
				);
	}

	@ParameterizedTest
	@MethodSource("mapProvider")
	void testGetFileDefaultExtension(TestFileExtensionsMap underTest) {
		assertAll(
				()->assertEquals(PDF_EXTENSION, underTest.fileDefaultExtension(APPLICATION_PDF_MIMETYPE).get()),
				()->assertEquals(DOC_EXTENSION, underTest.fileDefaultExtension(APPLICATION_VND_MS_WORD_MIMETYPE).get())
				);
	}
	
	@ParameterizedTest
	@MethodSource("mapProvider")
	void testGetMimeType_BadFilename(TestFileExtensionsMap underTest) {
		Path testPath = Paths.get("TestDir", "TestFile");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.mimeType(testPath));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertTrue(msg.contains("does not have an extension.")),
				()->assertTrue(msg.contains(testPath.toString()), "Expected exception message (" + msg + ") to contain test path (" + testPath.toString() + ").")
				);
	}

	@ParameterizedTest
	@MethodSource("mapProvider")
	void testGetMimeType_NonexistentExtension(TestFileExtensionsMap underTest) {
		Path testPath = Paths.get("TestDir", "TestFile.xxx");
		assertTrue(underTest.mimeType(testPath).isEmpty());
	}

}
