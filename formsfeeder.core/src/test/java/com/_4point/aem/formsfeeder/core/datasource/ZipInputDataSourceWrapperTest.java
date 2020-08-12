package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.formsfeeder.core.datasource.ZipInputDataSourceWrapper.ZipMetadata;
import com._4point.aem.formsfeeder.core.datasource.ZipInputDataSourceWrapper.ZipMetadata.ZipMetadataException;

class ZipInputDataSourceWrapperTest {

	@Test
	void testGetNextDataSource() {
		fail("Not yet implemented");
	}

	
	/*
	 * Tests for internal ZipMetadata
	 */
	enum MetadataTestScenario {
		NO_FILENAME(StandardMimeTypes.APPLICATION_PDF_TYPE, null, "<?xml version=\"1.0\" ?><DataSourceMetadata MimeType=\"application/pdf\"></DataSourceMetadata>"),
		WITH_FILENAME(StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE, "filename.dat", "<?xml version=\"1.0\" ?><DataSourceMetadata MimeType=\"application/vnd.adobe.central.field-nominated\" Filename=\"filename.dat\"></DataSourceMetadata>");
		
		private final MimeType mimeType;
		private final Optional<Path> filename;
		private final String xmlString;

		private MetadataTestScenario(MimeType mimeType, String filename, String xmlString) {
			this.mimeType = mimeType;
			this.filename = Optional.ofNullable(filename).map(Paths::get);
			this.xmlString = xmlString;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testZipMetadata_EncodeMetadata(MetadataTestScenario scenario) throws Exception {
		assertEquals(scenario.xmlString, ZipMetadata.encodeMetadata(scenario.mimeType, scenario.filename));
	}
	
	@ParameterizedTest
	@EnumSource
	void testZipMetadata_DecodeMetadata(MetadataTestScenario scenario) throws ZipMetadataException {
		Optional<ZipMetadata> optResult = ZipMetadata.decodeMetadata(scenario.xmlString);
		assertNotNull(optResult);
		assertTrue(optResult.isPresent());
		ZipMetadata result = optResult.get();
		assertAll(
				()->assertEquals(scenario.mimeType, result.mimeType()),
				()->assertEquals(scenario.filename, result.filename())
				);
	}

	@Test
	void testZipMetadata_ExtraKruftMetadata() throws ZipMetadataException {
		// Valid metadata but with extra kruft in it that we should ignore.
		String xmlString = "<?xml version=\"1.0\" ?><DataSourceMetadata ExtraAttribute1=\"Val1\" MimeType=\"application/vnd.adobe.central.field-nominated\" ExtraAttribute2=\"Val2\" Filename=\"filename.dat\" ExtraAttribute3=\"Val3\" ><ExtraElement Attribute1=\"foo\" /></DataSourceMetadata>";
		Optional<ZipMetadata> optResult = ZipMetadata.decodeMetadata(xmlString);
		assertNotNull(optResult);
		assertTrue(optResult.isPresent());
		ZipMetadata result = optResult.get();
		assertAll(
				()->assertEquals(StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE, result.mimeType()),
				()->assertEquals(Paths.get("filename.dat"), result.filename().get())
				);
	}

	@ParameterizedTest
	@ValueSource( strings = {
			"<?xml version=\"1.0\" ?><DataSourceMetadata Attribute1=\"foo\"></DataSourceMetadata>",
			"<?xml version=\"1.0\" ?><ExtraElement Attribute1=\"foo\" />",
			"<?xml version=\"1.0\" ?><DataSourceMetadata ExtraAttribute1=\"Val1\" ExtraAttribute2=\"Val2\" Filename=\"filename.dat\" ExtraAttribute3=\"Val3\" ><ExtraElement Attribute1=\"foo\" /></DataSourceMetadata>",
			"  "
			})
	@NullAndEmptySource
	void testZipMetadata_BadMetadata(String xmlString) throws ZipMetadataException {
		// Invalid metadata that should return nothing.
		Optional<ZipMetadata> optResult = ZipMetadata.decodeMetadata(xmlString);
		assertNotNull(optResult);
		assertFalse(optResult.isPresent());
	}

}
