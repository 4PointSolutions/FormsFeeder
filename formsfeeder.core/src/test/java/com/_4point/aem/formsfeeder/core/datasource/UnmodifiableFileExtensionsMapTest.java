package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * 
 *
 */
class UnmodifiableFileExtensionsMapTest {

	@Test
	void testFrom() {
		UnmodifiableFileExtensionsMap underTest = UnmodifiableFileExtensionsMap.from(UnmodifiableFileExtensionsMap.DEFAULT_MAP);
		assertAll(
				()->assertEquals(MimeType.of("application", "xml"), underTest.mimeType(Paths.get("test.xml")).get()),
				()->assertEquals("xdp", underTest.fileDefaultExtension(MimeType.of("application", "vnd.adobe.xdp+xml")).get())
				);
	}

}
