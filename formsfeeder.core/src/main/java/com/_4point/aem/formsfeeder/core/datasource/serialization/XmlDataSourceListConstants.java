package com._4point.aem.formsfeeder.core.datasource.serialization;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;

/* package */ class XmlDataSourceListConstants {
	static final MimeType DSL_MIME_TYPE = StandardMimeTypes.APPLICATION_VND_4POINT_DATASOURCELIST_TYPE;
	static final String DSL_ELEMENT_NAME = "DataSourceList";

	static class XmlDataSourceConstants {
		static final String DS_ELEMENT_NAME = "DataSource";
		static final String NAME_ATTR_NAME = "Name";
		static final String CONTENT_TYPE_ATTR_NAME = "ContentType";
		static final String FILENAME_ATTR_NAME = "Filename";
		static final String ATTR_ELEMENT_NAME = "Attribute";
		static final String ATTR_NAME_ATTR_NAME = "Name";
		static final String ATTR_VALUE_ATTR_NAME = "Value";
		static final String CONTENT_ELEMENT_NAME = "Content";
		static final Encoder ENCODER = Base64.getEncoder();
		static final Decoder DECODER = Base64.getDecoder();
	}
}
