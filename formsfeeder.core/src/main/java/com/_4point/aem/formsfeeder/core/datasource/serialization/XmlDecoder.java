package com._4point.aem.formsfeeder.core.datasource.serialization;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public abstract class XmlDecoder implements Closeable {
	protected final XMLStreamReader xsr;
	private	static final XMLInputFactory factory = XMLInputFactory.newFactory();

	protected XmlDecoder(XMLStreamReader xsw) {
		super();
		this.xsr = xsw;
	}
	
	@Override
	public void close() throws IOException {
	}

	public static XMLStreamReader toXmlStreamReader(InputStream is) throws XMLStreamException, FactoryConfigurationError {
		return factory.createXMLStreamReader(Objects.requireNonNull(is, "Input Stream cannot be null."));
	}
	
	public static XMLStreamReader toXmlStreamReader(Reader reader) throws XMLStreamException, FactoryConfigurationError {
		return factory.createXMLStreamReader(Objects.requireNonNull(reader, "Input Reader cannot be null."));
	}

}
