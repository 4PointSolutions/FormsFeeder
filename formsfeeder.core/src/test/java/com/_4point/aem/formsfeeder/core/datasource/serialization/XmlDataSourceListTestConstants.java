package com._4point.aem.formsfeeder.core.datasource.serialization;

import java.io.File;
import java.nio.file.Paths;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

/* package */ class XmlDataSourceListTestConstants {
	 enum TestScenario {
		EMPTY_LIST(DataSourceList.emptyList(), "<?xml version=\"1.0\" ?><DataSourceList/>"),
		EMPTY_LIST2(DataSourceList.builder().build(), "<?xml version=\"1.0\" ?><DataSourceList/>"),
		SIMPLE_ENTRY(DataSourceList.builder().add("DsName1", "DsValue1").build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName1\" ContentType=\"text/plain; charset=UTF-8\"><Content>RHNWYWx1ZTE=</Content></DataSource></DataSourceList>"),
		SIMPLE_ENTRY_FILENAME(DataSourceList.builder().add("DsName1", new byte[0], Paths.get("foo.bar")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName1\" ContentType=\"application/octet-stream\" Filename=\"foo.bar\"><Content></Content></DataSource></DataSourceList>"),
		SIMPLE_ENTRY_WITH_ATTRS(DataSourceList.builder().add("DsName2", 2, Jdk8Utils.mapOf("Ds2Attr1", "Ds2AttrValue1")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DsName2\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds2Attr1\" Value=\"Ds2AttrValue1\"/><Content>Mg==</Content></DataSource></DataSourceList>"),
		SIMPLE_ENTRY_ALL_ATTRS(DataSourceList.builder().add("DSName3", "DSValue3".getBytes(), StandardMimeTypes.APPLICATION_PDF_TYPE, Paths.get("dir", "filename"), Jdk8Utils.mapOf("Ds3Attr1", "Ds3AttrValue1")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DSName3\" ContentType=\"application/pdf\" Filename=\"dir" + File.separator + "filename\"><Attribute Name=\"Ds3Attr1\" Value=\"Ds3AttrValue1\"/><Content>RFNWYWx1ZTM=</Content></DataSource></DataSourceList>"),
		MULTI_ENTRY(DataSourceList.builder().addStrings("DSName4", Jdk8Utils.listOf("DSValue4_1", "DSValue4_2"), Jdk8Utils.mapOf("Ds4Attr1", "Ds4AttrValue1")).build(), 
				"<?xml version=\"1.0\" ?><DataSourceList><DataSource Name=\"DSName4\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds4Attr1\" Value=\"Ds4AttrValue1\"/><Content>RFNWYWx1ZTRfMQ==</Content></DataSource><DataSource Name=\"DSName4\" ContentType=\"text/plain; charset=UTF-8\"><Attribute Name=\"Ds4Attr1\" Value=\"Ds4AttrValue1\"/><Content>RFNWYWx1ZTRfMg==</Content></DataSource></DataSourceList>"),
		;
		
		final DataSourceList dsList;
		final String xml;

		private TestScenario(DataSourceList dsList, String xml) {
			this.dsList = dsList;
			this.xml = xml;
		}
		
	}
}
