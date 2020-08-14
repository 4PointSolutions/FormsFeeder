package com._4point.aem.formsfeeder.core.datasource.serialization;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

public class XmlDataSourceListEncoder extends XmlEncoder {
	private static final String DSL_ELEMENT_NAME = "DataSourceList";
	
	private XmlDataSourceListEncoder(XMLStreamWriter xsw) throws XMLStreamException {
		super(xsw);
		xsw.writeStartDocument();
	}

	public XmlDataSourceListEncoder encode(DataSourceList dsl) throws XMLStreamException, IOException {
		if (dsl.isEmpty()) {
			xsw.writeEmptyElement(DSL_ELEMENT_NAME);
		} else {
			xsw.writeStartElement(DSL_ELEMENT_NAME);
			try (XmlDataSourceEncoder dsEncoder = new XmlDataSourceEncoder(xsw)) {
				for(DataSource ds : dsl.list()) {
					dsEncoder.encode(ds);
				}
			}
			xsw.writeEndElement();
		}
		return this;
	}
	
	@Override
	public void close() throws IOException {
		try {
			xsw.writeEndDocument();
			xsw.close();
		} catch (XMLStreamException e) {
			String msg = e.getMessage();
			throw new IOException("Error while writing EndDocument to XMLStream (" + (msg == null ? "null" : msg) + ").", e);
		}
		super.close();
	}

	public static XmlDataSourceListEncoder wrap(XMLStreamWriter xsw) throws XMLStreamException {
		return new XmlDataSourceListEncoder(xsw);
	}
	
	public static XmlDataSourceListEncoder wrap(OutputStream os) throws XMLStreamException, FactoryConfigurationError {
		return wrap(toXmlStreamWriter(os));
	}

	public static XmlDataSourceListEncoder wrap(Writer writer) throws XMLStreamException, FactoryConfigurationError {
		return wrap(toXmlStreamWriter(writer));
	}
	
	public static class XmlDataSourceEncoder extends XmlEncoder {
		private static final String DS_ELEMENT_NAME = "DataSource";
		private static final String NAME_ATTR_NAME = "Name";
		private static final String CONTENT_TYPE_ATTR_NAME = "ContentType";
		private static final String FILENAME_ATTR_NAME = "Filename";
		private static final String ATTR_ELEMENT_NAME = "Attribute";
		private static final String ATTR_NAME_ATTR_NAME = "Name";
		private static final String ATTR_VALUE_ATTR_NAME = "Value";
		private static final String CONTENT_ELEMENT_NAME = "Content";
		private static final Encoder ENCODER = Base64.getEncoder();


		private XmlDataSourceEncoder(XMLStreamWriter xsw) {
			super(xsw);
		}

		public XmlDataSourceEncoder encode(DataSource ds) throws XMLStreamException, IOException {
			xsw.writeStartElement(DS_ELEMENT_NAME);
			xsw.writeAttribute(NAME_ATTR_NAME, ds.name());
			xsw.writeAttribute(CONTENT_TYPE_ATTR_NAME, ds.contentType().asString());
			Optional<Path> filename = ds.filename();
			if (filename.isPresent()) {
				xsw.writeAttribute(FILENAME_ATTR_NAME, filename.get().toString());				
			}
			Set<Entry<String,String>> attributes = ds.attributes().entrySet();
			if (!attributes.isEmpty()) {
				for(Entry<String, String> attribute : attributes) {
					xsw.writeEmptyElement(ATTR_ELEMENT_NAME);
					xsw.writeAttribute(ATTR_NAME_ATTR_NAME, attribute.getKey());
					xsw.writeAttribute(ATTR_VALUE_ATTR_NAME, attribute.getValue());
				}
			}
			xsw.writeStartElement(CONTENT_ELEMENT_NAME);
			xsw.writeCharacters(ENCODER.encodeToString(Jdk8Utils.readAllBytes(ds.inputStream())));
			xsw.writeEndElement();
			xsw.writeEndElement();
			return this;
		}
	}

}
