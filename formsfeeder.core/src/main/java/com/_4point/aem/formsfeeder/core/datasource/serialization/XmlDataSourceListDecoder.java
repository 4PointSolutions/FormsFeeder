package com._4point.aem.formsfeeder.core.datasource.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;

public class XmlDataSourceListDecoder extends XmlDecoder {

	private XmlDataSourceListDecoder(XMLStreamReader xsw) {
		super(xsw);
	}

	/**
	 * Read one DataSource
	 * 
	 * @return
	 */
	public Optional<DataSourceList> decode() {
		//TODO: Fill in the details
		return null;
	}

	/**
	 * Note: Calling close on the XmlSourceEncoder *does not* close the underlying output stream.  It mearly closes off the
	 * encoding of this data source.
	 *
	 */
	@Override
	public void close() throws IOException {
		try {
			xsr.close();
		} catch (XMLStreamException e) {
			String msg = e.getMessage();
			throw new IOException("Error while closing XMLStream (" + (msg == null ? "null" : msg) + ").", e);
		}
	}

	public static XmlDataSourceListDecoder wrap(XMLStreamReader xsw) {
		return new XmlDataSourceListDecoder(xsw);
	}
	
	public static XmlDataSourceListDecoder wrap(InputStream is) throws XMLStreamException, FactoryConfigurationError {
		return wrap(toXmlStreamReader(is));
	}

	public static XmlDataSourceListDecoder wrap(Reader reader) throws XMLStreamException, FactoryConfigurationError {
		return wrap(toXmlStreamReader(reader));
	}
	
	private class XmlDataSourceDecoder extends XmlDecoder {

		private XmlDataSourceDecoder(XMLStreamReader xsw) {
			super(xsw);
		}

		/**
		 * Produce a stream of datasources.
		 * 
		 * @return
		 */
		public Stream<DataSource> stream() {
			//TODO: Fill in the details
			return null;
		}
		
		/**
		 * Read one DataSource
		 * 
		 * @return
		 */
		public Optional<DataSource> decode() {
			//TODO: Fill in the details
			return null;
		}

	}

}
