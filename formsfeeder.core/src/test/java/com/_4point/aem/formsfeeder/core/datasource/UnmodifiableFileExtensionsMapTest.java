package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 
 *
 */
class UnmodifiableFileExtensionsMapTest {

	@Test
	void testFrom() {
		var underTest = UnmodifiableFileExtensionsMap.from(UnmodifiableFileExtensionsMap.DEFAULT_MAP);
		assertAll(
				()->assertEquals(MimeType.of("application", "xml"), underTest.getMimeType(Path.of("test.xml"))),
				()->assertEquals("xdp", underTest.getFileDefaultExtension(MimeType.of("application", "vnd.adobe.xdp+xml")))
				);
	}

}
