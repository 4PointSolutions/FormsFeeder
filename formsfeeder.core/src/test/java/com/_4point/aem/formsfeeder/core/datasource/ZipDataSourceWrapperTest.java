package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.formsfeeder.core.datasource.ZipDataSourceWrapper.ZipInputDataSourceStream;
import com._4point.aem.formsfeeder.core.datasource.ZipDataSourceWrapper.ZipMetadata;
import com._4point.aem.formsfeeder.core.datasource.ZipDataSourceWrapper.ZipMetadata.ZipMetadataException;
import com._4point.aem.formsfeeder.core.datasource.ZipDataSourceWrapper.ZipOutputDataSourceStream;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

class ZipDataSourceWrapperTest {

	private static final String FIRST_DSL_ENTRY_NAME = "FirstName";
	private static final String SECOND_DSL_ENTRY_NAME = "SecondName";
	private static final String THIRD_DSL_ENTRY_NAME = "ThirdName";

	private static final StringDataSource DS1 = new StringDataSource("FirstEntry", FIRST_DSL_ENTRY_NAME);
	private static final StringDataSource DS2 = new StringDataSource("SecondEntry", SECOND_DSL_ENTRY_NAME, Jdk8Utils.mapOf("attributeName1", "attributeValue1"));
	private static final StringDataSource DS3 = new StringDataSource("ThirdEntry", SECOND_DSL_ENTRY_NAME);	// Make sure we have a duplicate entry
	private static final StringDataSource DS4 = new StringDataSource("FourthEntry", THIRD_DSL_ENTRY_NAME);

	@Test
	void testZipDataSourceWrapper() throws Exception {
		ByteArrayDataSource resultDs = new ByteArrayDataSource();
		
		// Construct the ZipDataSource
		ZipDataSourceWrapper underTest = ZipDataSourceWrapper.wrap(resultDs);
		try (ZipOutputDataSourceStream zipOutputStream = underTest.zipOutputStream()) {
			zipOutputStream.putNextDataSource(DS1)
						   .putNextDataSource(DS2)
						   .putNextDataSource(DS3)
						   .putNextDataSource(DS4);
		}
		// Read the ZipDataSource
		List<DataSource> results = new ArrayList<>();
		try (ZipInputDataSourceStream zipInputStream = underTest.zipInputStream()) {
			Optional<DataSource> ds;
			while((ds = zipInputStream.getNextDataSource()).isPresent()) {
				results.add(ds.get());
			}
		}
			
		// Verify that it's correct
		assertEquals(4, results.size());
		List<StringDataSource> expectedResults = Jdk8Utils.listOf(DS1, DS2, DS3, DS4);
		for (int i = 0; i < 4; i++) {
			DataSource result = results.get(i);
			DataSource expectedResult = expectedResults.get(i);
//			assertEquals(expectedResult.name(), result.name());
			try( InputStream expectedIs = expectedResult.inputStream();
				 InputStream resultIs = result.inputStream()) {
				assertArrayEquals(Jdk8Utils.readAllBytes(expectedIs), Jdk8Utils.readAllBytes(resultIs)); 
			}
		}
	}


	/*
	 * Tests for internal ZipMetadata
	 */
	enum MetadataTestScenario {
		NO_FILENAME("datasourceName", StandardMimeTypes.APPLICATION_PDF_TYPE, null, "<?xml version=\"1.0\" ?><DataSourceMetadata DataSourceName=\"datasourceName\" MimeType=\"application/pdf\"></DataSourceMetadata>"),
		WITH_FILENAME("datasourceName2", StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE, "filename.dat", "<?xml version=\"1.0\" ?><DataSourceMetadata DataSourceName=\"datasourceName2\" MimeType=\"application/vnd.adobe.central.field-nominated\" Filename=\"filename.dat\"></DataSourceMetadata>");

		private final String datasourceName;
		private final MimeType mimeType;
		private final Optional<Path> filename;
		private final String xmlString;

		private MetadataTestScenario(String datasourceName, MimeType mimeType, String filename, String xmlString) {
			this.datasourceName = datasourceName;
			this.mimeType = mimeType;
			this.filename = Optional.ofNullable(filename).map(Paths::get);
			this.xmlString = xmlString;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testZipMetadata_EncodeMetadata(MetadataTestScenario scenario) throws Exception {
		assertEquals(scenario.xmlString, ZipMetadata.encodeMetadata(scenario.datasourceName, scenario.mimeType, scenario.filename));
	}
	
	@ParameterizedTest
	@EnumSource
	void testZipMetadata_DecodeMetadata(MetadataTestScenario scenario) throws ZipMetadataException {
		Optional<ZipMetadata> optResult = ZipMetadata.decodeMetadata(scenario.xmlString);
		assertNotNull(optResult);
		assertTrue(optResult.isPresent());
		ZipMetadata result = optResult.get();
		assertAll(
				()->assertEquals(scenario.datasourceName, result.datasourceName()),
				()->assertEquals(scenario.mimeType, result.mimeType()),
				()->assertEquals(scenario.filename, result.filename())
				);
	}

	@Test
	void testZipMetadata_ExtraKruftMetadata() throws ZipMetadataException {
		// Valid metadata but with extra kruft in it that we should ignore.
		String xmlString = "<?xml version=\"1.0\" ?><DataSourceMetadata ExtraAttribute1=\"Val1\"  DataSourceName=\"dsName\"  ExtraAttribute2=\"Val2\" MimeType=\"application/vnd.adobe.central.field-nominated\" ExtraAttribute3=\"Val3\" Filename=\"filename.dat\" ExtraAttribute4=\"Val4\" ><ExtraElement Attribute1=\"foo\" /></DataSourceMetadata>";
		Optional<ZipMetadata> optResult = ZipMetadata.decodeMetadata(xmlString);
		assertNotNull(optResult);
		assertTrue(optResult.isPresent());
		ZipMetadata result = optResult.get();
		assertAll(
				()->assertEquals("dsName", result.datasourceName()),
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
