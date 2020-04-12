package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.datasource.AbstractFileExtensionsMap;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.AbstractFileExtensionsMap.FileExtensionsEntry;

class AbstractFileExtensionsMapTest {

	private static final MimeType APPLICATION_VND_MS_WORD_MIMETYPE = MimeType.of("application", "vnd.ms-word");
	private static final MimeType APPLICATION_PDF_MIMETYPE = MimeType.of("application", "pdf");
	private static final String DOC_EXTENSION = "doc";
	private static final String PDF_EXTENSION = "pdf";
	public static final TestFileExtensionsMap UNDER_TEST = 
			new TestFileExtensionsMap( new FileExtensionsEntry[] { 
				FileExtensionsEntry.of(APPLICATION_PDF_MIMETYPE.asString(), new String[] { PDF_EXTENSION, "foo" }),
				FileExtensionsEntry.of(APPLICATION_VND_MS_WORD_MIMETYPE.asString(), new String[] {DOC_EXTENSION, "bar"})
			} );

	@Test
	void testGetMimeType() {
		assertAll(
				()->assertEquals(APPLICATION_PDF_MIMETYPE, UNDER_TEST.getMimeType(Paths.get("TestDir", "TestFile." + PDF_EXTENSION))),
				()->assertEquals(APPLICATION_VND_MS_WORD_MIMETYPE, UNDER_TEST.getMimeType(Paths.get("TestDir", "TestFile." + DOC_EXTENSION)))
				);
	}

	@Test
	void testGetFileDefaultExtension() {
		assertAll(
				()->assertEquals(PDF_EXTENSION, UNDER_TEST.getFileDefaultExtension(APPLICATION_PDF_MIMETYPE)),
				()->assertEquals(DOC_EXTENSION, UNDER_TEST.getFileDefaultExtension(APPLICATION_VND_MS_WORD_MIMETYPE))
				);
	}
	
	private static class TestFileExtensionsMap extends AbstractFileExtensionsMap {
		public TestFileExtensionsMap(FileExtensionsEntry[] entries) {
			super(entries);
		}
	}
}
