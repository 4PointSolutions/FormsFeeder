package com._4point.aem.formsfeeder.core.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com._4point.aem.formsfeeder.core.datasource.ZipInputDataSourceWrapper.ZipMetadata.ZipMetadataException;

public class ZipInputDataSourceWrapper extends DataSourceWrapper {

	private final ZipInputStream zipStream;
	
	private ZipInputDataSourceWrapper(DataSource dataSource) {
		super(dataSource);
		this.zipStream = new ZipInputStream(dataSource.inputStream()); 
	}
	
	public static ZipInputDataSourceWrapper wrap(DataSource dataSource) {
		return new ZipInputDataSourceWrapper(dataSource);
	}

	public DataSource getNextDataSource() throws IOException {
		return new ZipDataSource(zipStream.getNextEntry());
	}

	public class ZipDataSource extends AbstractDataSource {
		private final ZipEntry zipEntry;
		
		public ZipDataSource(ZipEntry zipEntry) {
			super();
			this.zipEntry = zipEntry;
		}

		@Override
		public InputStream inputStream() {
			return zipStream;
		}

		@Override
		public OutputStream outputStream() {
			throw new UnsupportedOperationException("Cannot get OutputStream on a ZipInputDataSource");
		}

		@Override
		public String name() {
			return zipEntry.getName();
		}
	}
	
	public static class ZipMetadata {
		
		private static final String DATA_SOURCE_METADATA_ELEMENT = "DataSourceMetadata";
		private static final String MIMETYPE_ATTR = "MimeType";
		private static final String FILENAME_ATTR = "Filename";
		private final MimeType mimeType;
		private final Optional<Path> filename;
		
		public ZipMetadata(MimeType mimeType, Optional<Path> filename) {
			super();
			this.mimeType = mimeType;
			this.filename = filename;
		}

		public MimeType mimeType() {
			return mimeType;
		}

		public Optional<Path> filename() {
			return filename;
		}

		public static String encodeMetadata(MimeType mimeType, Optional<Path> filename) throws ZipMetadataException {
			try (StringWriter stringWriter = new StringWriter()) {
				XMLStreamWriter xmlWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(stringWriter);
				xmlWriter.writeStartDocument();
				xmlWriter.writeStartElement(DATA_SOURCE_METADATA_ELEMENT);
				xmlWriter.writeAttribute(MIMETYPE_ATTR, mimeType.asString());
				if (filename.isPresent()) {
					xmlWriter.writeAttribute(FILENAME_ATTR, filename.get().toString());
				}
				xmlWriter.writeEndElement();
				xmlWriter.close();
				return stringWriter.toString();
			} catch (IOException | XMLStreamException e) {
				throw new ZipMetadataException("Error while encoding zip metadata", e);
				
			}
		}

		public static Optional<ZipMetadata> decodeMetadata(String comment) throws ZipMetadataException {
			if (comment == null || comment.trim().isEmpty()) {
				return Optional.empty();	// Shortcut if there's nothing in the comment.
			}
			String mimeType = null;
			String filename = null;
			try(StringReader stringReader = new StringReader(comment)) {
				XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(stringReader);
				while(xmlReader.hasNext()) {
					int state = xmlReader.next();
					switch(state) {
					case XMLStreamReader.END_DOCUMENT:
						xmlReader.close();
						break;
					case XMLStreamReader.START_DOCUMENT:
						xmlReader.nextTag();
					case XMLStreamReader.END_ELEMENT:
						break;
					case XMLStreamReader.START_ELEMENT:
						if (xmlReader.getLocalName().equals(DATA_SOURCE_METADATA_ELEMENT)) {
							int attributeCount = xmlReader.getAttributeCount();
							for (int i = 0; i < attributeCount; i++) {
								String attName = xmlReader.getAttributeLocalName(i);
								if (attName.equals(MIMETYPE_ATTR)) {
									mimeType = xmlReader.getAttributeValue(i);
								} else if (attName.equals(FILENAME_ATTR)) {
									filename = xmlReader.getAttributeValue(i);
								}
							}
						}
						break;
					default:	// If we encounter something we're not expecting, just ignore it.
					}
				}
				// If mimetype wasn't found, then return empty, otherwise use it to create a ZipMetadata object and return that.
				return mimeType == null ? 
						  	Optional.empty()
						  : Optional.of(new ZipMetadata(MimeType.of(mimeType), Optional.ofNullable(filename).map(Paths::get)));
			} catch (XMLStreamException e) {
				String msg = e.getMessage();
				throw new ZipMetadataException("Error while decoding zip metadata (" + (msg == null ? "null" : msg) + ").", e);
			}
		}

		@SuppressWarnings("serial")
		public static class ZipMetadataException extends Exception {

			public ZipMetadataException() {
				super();
			}

			public ZipMetadataException(String message, Throwable cause) {
				super(message, cause);
			}

			public ZipMetadataException(String message) {
				super(message);
			}

			public ZipMetadataException(Throwable cause) {
				super(cause);
			}
			
		}
	}
}
