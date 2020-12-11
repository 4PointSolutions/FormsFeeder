package com._4point.aem.formsfeeder.core.datasource.serialization;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Objects;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public abstract class XmlEncoder implements Closeable {
	protected final XMLStreamWriter xsw;
	private static XMLOutputFactory factory = XMLOutputFactory.newFactory();

	protected XmlEncoder(XMLStreamWriter xsw) {
		super();
		this.xsw = xsw;
	}
	
	@Override
	public void close() throws IOException {
	}

	public static XMLStreamWriter toXmlStreamWriter(OutputStream os) throws XMLStreamException, FactoryConfigurationError {
		return factory.createXMLStreamWriter(Objects.requireNonNull(os, "Output Stream cannot be null."));
	}

	public static XMLStreamWriter toXmlStreamWriter(Writer writer) throws XMLStreamException, FactoryConfigurationError {
		return factory.createXMLStreamWriter(Objects.requireNonNull(writer, "Writer cannot be null."));
	}
}
