package com._4point.aem.formsfeeder.core.datasource.serialization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

class XmlDataSourceListEncoderTest {
	
	
	private final StringWriter resultWriter = new StringWriter();

	private enum EncodingScenario {
		EMPTY_LIST(DataSourceList.emptyList(), "<?xml version=\"1.0\" ?><DataSourceList/>"),
		EMPTY_LIST2(DataSourceList.builder().build(), "<?xml version=\"1.0\" ?><DataSourceList/>"),
		SIMPLE_ENTRY(DataSourceList.builder().add("DsName1", "DsValue1").build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName1\" ContentType=\"text/plain; charset=UTF-8\"><Content>RHNWYWx1ZTE=</Content></DataSource></DataSourceList>"),
		SIMPLE_ENTRY_WITH_ATTRS(DataSourceList.builder().add("DsName2", 2, Jdk8Utils.mapOf("Ds2Attr1", "Ds2AttrValue1")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName2\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds2Attr1\" Value=\"Ds2AttrValue1\"/><Content>Mg==</Content></DataSource></DataSourceList>"),
		SIMPLE_ENTRY_ALL_ATTRS(DataSourceList.builder().add("DSName3", "DSValue3".getBytes(), StandardMimeTypes.APPLICATION_PDF_TYPE, Paths.get("dir", "filename"), Jdk8Utils.mapOf("Ds3Attr1", "Ds3AttrValue1")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DSName3\" ContentType=\"application/pdf\" Filename=\"dir\\filename\"><Attribute Name=\"Ds3Attr1\" Value=\"Ds3AttrValue1\"/><Content>RFNWYWx1ZTM=</Content></DataSource></DataSourceList>"),
		MULTI_ENTRY(DataSourceList.builder().addStrings("DSName4", Jdk8Utils.listOf("DSValue4_1", "DSValue4_2"), Jdk8Utils.mapOf("Ds4Attr1", "Ds4AttrValue1")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DSName4\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds4Attr1\" Value=\"Ds4AttrValue1\"/><Content>RFNWYWx1ZTRfMQ==</Content></DataSource><DataSource Name=\"DSName4\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds4Attr1\" Value=\"Ds4AttrValue1\"/><Content>RFNWYWx1ZTRfMg==</Content></DataSource></DataSourceList>"),
		;
		
		private final DataSourceList targetList;
		private final String expectedResult;

		private EncodingScenario(DataSourceList targetList, String expectedResult) {
			this.targetList = targetList;
			this.expectedResult = expectedResult;
		}
		
	}
	
	@ParameterizedTest
	@EnumSource
	void testEncode(EncodingScenario scenario) throws Exception {

		try(XmlDataSourceListEncoder underTest = XmlDataSourceListEncoder.wrap(resultWriter)) {
			underTest.encode(scenario.targetList);
		}

		
		resultWriter.close();
		assertEquals(scenario.expectedResult, resultWriter.toString());
	}

	@Test
	void test_OutputStream() throws Exception {
		String expectedResult = "<?xml version=\"1.0\" ?><DataSourceList>" + 
							    "<DataSource Name=\"DsName1\" ContentType=\"text/plain; charset=UTF-8\"><Content>RHNWYWx1ZTE=</Content></DataSource>" + 
							    "<DataSource Name=\"DsName2\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds2Attr1\" Value=\"Ds2AttrValue1\"/><Content>Mg==</Content></DataSource>" +
							    "<DataSource Name=\"DSName3\" ContentType=\"application/pdf\" Filename=\"dir\\filename\"><Attribute Name=\"Ds3Attr1\" Value=\"Ds3AttrValue1\"/><Content>RFNWYWx1ZTM=</Content></DataSource>" +
							    "<DataSource Name=\"DSName4\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds4Attr1\" Value=\"Ds4AttrValue1\"/><Content>RFNWYWx1ZTRfMQ==</Content></DataSource>" +
							    "<DataSource Name=\"DSName4\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds4Attr1\" Value=\"Ds4AttrValue1\"/><Content>RFNWYWx1ZTRfMg==</Content></DataSource>" +
							    "<DataSource Name=\"DSName5\" ContentType=\"text/html\"><Attribute Name=\"Ds5Attr3\" Value=\"Ds5AttrValue3\"/><Attribute Name=\"Ds5Attr2\" Value=\"Ds5AttrValue2\"/><Attribute Name=\"Ds5Attr1\" Value=\"Ds5AttrValue1\"/><Content>RFM1Q29udGVudDE=</Content></DataSource>" +
							    "<DataSource Name=\"DSName5\" ContentType=\"text/html\"><Attribute Name=\"Ds5Attr3\" Value=\"Ds5AttrValue3\"/><Attribute Name=\"Ds5Attr2\" Value=\"Ds5AttrValue2\"/><Attribute Name=\"Ds5Attr1\" Value=\"Ds5AttrValue1\"/><Content>RFM1Q29udGVudDI=</Content></DataSource>" +
							    "</DataSourceList>";
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataSourceList testDsl = DataSourceList.builder()
											   .add("DsName1", "DsValue1")
											   .add("DsName2", 2, Jdk8Utils.mapOf("Ds2Attr1", "Ds2AttrValue1"))
											   .add("DSName3", "DSValue3".getBytes(), StandardMimeTypes.APPLICATION_PDF_TYPE, Paths.get("dir", "filename"), Jdk8Utils.mapOf("Ds3Attr1", "Ds3AttrValue1"))
											   .addStrings("DSName4", Jdk8Utils.listOf("DSValue4_1", "DSValue4_2"), Jdk8Utils.mapOf("Ds4Attr1", "Ds4AttrValue1"))
											   .addByteArrays("DSName5", Jdk8Utils.listOf("DS5Content1".getBytes(), "DS5Content2".getBytes()), StandardMimeTypes.TEXT_HTML_TYPE, Jdk8Utils.mapOf("Ds5Attr1", "Ds5AttrValue1", "Ds5Attr2", "Ds5AttrValue2", "Ds5Attr3", "Ds5AttrValue3"))
											   .build();
		
		try(XmlDataSourceListEncoder underTest = XmlDataSourceListEncoder.wrap(bos)) {
			underTest.encode(testDsl);
		}
		
		assertEquals(expectedResult, new String(bos.toByteArray(), StandardCharsets.UTF_8));
	}
	
	@Test
	void test_NullWriter() {
		NullPointerException ex = assertThrows(NullPointerException.class, ()->XmlDataSourceListEncoder.wrap((Writer)null));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertThat(msg, containsString("Writer")),
				()->assertThat(msg, containsString("cannot be null"))
				);
	}
	
	@Test
	void test_NullOutputStream() {
		NullPointerException ex = assertThrows(NullPointerException.class, ()->XmlDataSourceListEncoder.wrap((OutputStream)null));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertThat(msg, containsString("Output Stream")),
				()->assertThat(msg, containsString("cannot be null"))
				);
	}


}
