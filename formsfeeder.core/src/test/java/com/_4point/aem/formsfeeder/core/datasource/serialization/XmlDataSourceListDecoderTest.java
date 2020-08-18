package com._4point.aem.formsfeeder.core.datasource.serialization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Optional;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList;

class XmlDataSourceListDecoderTest {

	@ParameterizedTest
	@EnumSource
	void testDecode(XmlDataSourceListTestConstants.TestScenario scenario) throws Exception {
		try(XmlDataSourceListDecoder underTest = XmlDataSourceListDecoder.wrap(new StringReader(scenario.xml))) {
			Optional<DataSourceList> result = underTest.decode();

			assertNotNull(result);
			assertTrue(result.isPresent(), "Expected result to be present.");
			// TODO: Check that the DataSourceLists are equal.
			
		}

		

	}

	@Test
	void test_NullReader() {
		NullPointerException ex = assertThrows(NullPointerException.class, ()->XmlDataSourceListDecoder.wrap((Reader)null));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertThat(msg, containsString("Reader")),
				()->assertThat(msg, containsString("cannot be null"))
				);
	}
	
	@Test
	void test_NullInputStream() {
		NullPointerException ex = assertThrows(NullPointerException.class, ()->XmlDataSourceListDecoder.wrap((InputStream)null));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertThat(msg, containsString("Input Stream")),
				()->assertThat(msg, containsString("cannot be null"))
				);
	}
	
	@Test
	void test_NullXmlStreamReader() {
		NullPointerException ex = assertThrows(NullPointerException.class, ()->XmlDataSourceListDecoder.wrap((XMLStreamReader)null));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertThat(msg, containsString("XMLStreamReader")),
				()->assertThat(msg, containsString("cannot be null"))
				);
	}


}
