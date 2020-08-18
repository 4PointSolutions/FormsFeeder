package com._4point.aem.formsfeeder.core.datasource.serialization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

class XmlDataSourceListDecoderTest {

	@ParameterizedTest
	@EnumSource
	void testDecode(XmlDataSourceListTestConstants.TestScenario scenario) throws Exception {
		try(XmlDataSourceListDecoder underTest = XmlDataSourceListDecoder.wrap(new StringReader(scenario.xml))) {
			Optional<DataSourceList> result = underTest.decode();

			assertNotNull(result);
			assertTrue(result.isPresent(), "Expected result to be present.");
			dslEquals(scenario.dsList, result.get(), true);
		}
	}

	/**
	 * Other scenarios that can't really happen when we use the XmlDataSourceListEndcoder but might happen if someone passes in XML
	 * that was created through some other means.  We should try and handle these kinds of situations gracefully.
	 * 
	 * We should ignore extraneous elements and attributes that we do not recognize and we should provide a reasonable default if
	 * contentType is omitted.
	 *
	 */
	enum OtherScenario {
		NO_CONTENT_TYPE_MULTI_ENTRIES(DataSourceList.builder().addByteArrays("DSName4", Jdk8Utils.listOf("DSValue4_1".getBytes(), "DSValue4_2".getBytes()), Jdk8Utils.mapOf("Ds4Attr1", "Ds4AttrValue1")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DSName4\"><Content>RFNWYWx1ZTRfMQ==</Content><Attribute Name=\"Ds4Attr1\" Value=\"Ds4AttrValue1\"/></DataSource><DataSource Name=\"DSName4\"><Attribute Name=\"Ds4Attr1\" Value=\"Ds4AttrValue1\"/><Content>RFNWYWx1ZTRfMg==</Content></DataSource></DataSourceList>"),
		NO_CONTENT_TYPE(DataSourceList.builder().add("DsName1", "DsValue1".getBytes(), Paths.get("bar.foo"), Jdk8Utils.mapOf("DsAttr1", "DsAttrValue1")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName1\" Filename=\"bar.foo\"><Content>RHNWYWx1ZTE=</Content><Attribute Name=\"DsAttr1\" Value=\"DsAttrValue1\"/></DataSource></DataSourceList>"),
		NO_CONTENT_TYPE_NO_FILENAME(DataSourceList.builder().add("DsName1", "DsValue1".getBytes()).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName1\"><Content>RHNWYWx1ZTE=</Content></DataSource></DataSourceList>"),
		NO_CONTENT_TYPE_NO_ATTRS(DataSourceList.builder().add("DsName1", new byte[0], Paths.get("foo.bar")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName1\" Filename=\"foo.bar\"><Content></Content></DataSource></DataSourceList>"),
		NO_CONTENT_TYPE_NO_FILENAME_NO_ATTRS(DataSourceList.builder().add("DsName1", "DsValue1".getBytes()).build(),
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName1\"><Content>RHNWYWx1ZTE=</Content></DataSource></DataSourceList>"),
		OTHER_ELEMENTS(DataSourceList.builder().add("DsName1", "DsValue1").build(),
				"<?xml version=\"1.0\" ?><DataSourceList><foo/><DataSource Name=\"DsName1\" ContentType=\"text/plain; charset=UTF-8\"><more>asdasda</more><Content>RHNWYWx1ZTE=</Content><other>ssss</other></DataSource><bar>xxx</bar></DataSourceList>"),
		OTHER_ATTRIBUTES(DataSourceList.builder().add("DsName1", "DsValue1").build(),
				"<?xml version=\"1.0\" ?><DataSourceList foo=\"bar\"><DataSource foo1=\"bar1\" Name=\"DsName1\" foo2=\"bar2\" ContentType=\"text/plain; charset=UTF-8\" foo3=\"bar3\"><Content foo=\"bar\">RHNWYWx1ZTE=</Content></DataSource></DataSourceList>")
		;
		
		final DataSourceList dsList;
		final String xml;

		private OtherScenario(DataSourceList dsList, String xml) {
			this.dsList = dsList;
			this.xml = xml;
		}
	}

	@ParameterizedTest
	@EnumSource
	void testDecode(OtherScenario scenario) throws Exception {
		try(XmlDataSourceListDecoder underTest = XmlDataSourceListDecoder.wrap(new StringReader(scenario.xml))) {
			Optional<DataSourceList> result = underTest.decode();

			assertNotNull(result); 
			assertTrue(result.isPresent(), "Expected result to be present.");
			dslEquals(scenario.dsList, result.get(), true);
		}
	}

	@Test
	void testInputStream() throws Exception {
		String inputXml = "<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName1\" ContentType=\"text/plain; charset=UTF-8\"><Content>RHNWYWx1ZTE=</Content></DataSource></DataSourceList>";
		try(XmlDataSourceListDecoder underTest = XmlDataSourceListDecoder.wrap(new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8)))) {
			Optional<DataSourceList> result = underTest.decode();

			assertNotNull(result); 
			assertTrue(result.isPresent(), "Expected result to be present.");
			dslEquals(DataSourceList.builder().add("DsName1", "DsValue1").build(), result.get(), true);
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

	/**
	 * Returns true of the other DataSourceList is equal to this one.  The comparison can be shallow or deep.  
	 * 
	 * A deep comparison compares the contents of the DataSources in addition to it's other attributes. 
	 * 
	 * @param firstDsl
	 * @param otherDsl
	 * @param deep
	 * @return
	 * @throws Exception 
	 */
	public static final void dslEquals(DataSourceList firstDsl, DataSourceList otherDsl, boolean deep) throws Exception {
		assertEquals(firstDsl.size(), otherDsl.size(), "DataSourceList sizes do not match.");
		for (int i = 0; i < firstDsl.size(); i++) {
			dsEquals(firstDsl.list().get(i), otherDsl.list().get(i), deep);
		}
	}
		
	public static final void dsEquals(DataSource firstDs, DataSource otherDs, boolean deep) throws Exception {
		assertEquals(firstDs.name(), otherDs.name(), "DataSource 'name' fields do not match.");
		assertEquals(firstDs.contentType(), otherDs.contentType(), "DataSource 'contentType' fields do not match.");
		assertEquals(firstDs.filename(), otherDs.filename(), "DataSource 'filename' fields do not match.");
		Map<String, String> firstAttributes = firstDs.attributes();
		Map<String, String> otherAttributes = otherDs.attributes();
		Set<Entry<String, String>> firstAttributeSet = firstAttributes.entrySet();
		Set<Entry<String, String>> otherAttributeSet = otherAttributes.entrySet();
		assertEquals(firstAttributeSet.size(), otherAttributeSet.size(),"Different number of attributes.");
		for (Entry<String, String> attr:firstAttributeSet) {
			assertEquals(attr.getValue(), otherAttributes.get(attr.getKey()));
		}
		if (deep) {
			assertArrayEquals(Jdk8Utils.readAllBytes(firstDs.inputStream()), Jdk8Utils.readAllBytes(otherDs.inputStream()), "DataSource 'content' fields do not match.");
		}
	}
}
