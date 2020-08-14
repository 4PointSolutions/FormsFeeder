package com._4point.aem.formsfeeder.core.datasource;

import static com._4point.aem.formsfeeder.core.datasource.ZipDataSourceWrapper.ZipMetadata.encodeMetadata;

import java.io.Closeable;
import java.io.FilterInputStream;
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
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com._4point.aem.formsfeeder.core.datasource.ZipDataSourceWrapper.ZipMetadata.ZipMetadataException;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

public class ZipDataSourceWrapper extends DataSourceWrapper {

	private ZipDataSourceWrapper(DataSource dataSource) {
		super(dataSource);
	}

	public static ZipDataSourceWrapper wrap(DataSource dataSource) {
		return new ZipDataSourceWrapper(dataSource);
	}
	
	@Override
	public InputStream inputStream() {
		throw new UnsupportedOperationException("You cannot access a ZipDataSourceWrapper inputStream directly, use zipInputStream().");
	}

	@Override
	public OutputStream outputStream() {
		throw new UnsupportedOperationException("You cannot access a ZipDataSourceWrapper outputStream directly, use zipOutputStream().");
	}

	public ZipInputDataSourceStream zipInputStream() {
		return new ZipInputDataSourceStream(super.inputStream());
	}

	public ZipOutputDataSourceStream zipOutputStream() {
		return new ZipOutputDataSourceStream(super.outputStream());
	}

	public class ZipInputDataSourceStream implements Closeable {

		private final ZipInputStream zipStream;
		
		private ZipInputDataSourceStream(InputStream inputStream) {
			this.zipStream = new ZipInputStream(inputStream); 
		}
		
		public Optional<DataSource> getNextDataSource() throws IOException {
			ZipEntry nextEntry = zipStream.getNextEntry();
			
//			if (nextEntry == null) {
//				return Optional.empty();
//			} else {
//				ZipMetadata.decodeMetadata(nextEntry.getComment());
//			}
			return Optional.ofNullable(nextEntry)
						   .flatMap(this::from);
		}

		private Optional<DataSource> from(ZipEntry zipEntry) {
			Optional<ZipMetadata> optMetadata = ZipMetadata.decodeMetadata(zipEntry.getComment());
			return optMetadata.map(this::readEntryIntoDataSource);
//			return readEntryIntoDataSource();
		}

		private Optional<DataSource> readEntryIntoDataSource() {
			try {
				return Optional.of(new ByteArrayDataSource(Jdk8Utils.readAllBytes(zipStream), "rtmDS"));
			} catch (IOException e) {
				String msg = e.getMessage();
				throw new ZipMetadataException("I/O Error while reading zip. (" + (msg == null ? "null": msg) + ").", e);
			}
		}

		private DataSource readEntryIntoDataSource(ZipMetadata zipMetadata) {
			try {
				ByteArrayDataSource result = new ByteArrayDataSource(Jdk8Utils.readAllBytes(zipStream), zipMetadata.datasourceName(), zipMetadata.mimeType());
				zipMetadata.filename().ifPresent(result::filename);
				return result;
			} catch (IOException e) {
				String msg = e.getMessage();
				throw new ZipMetadataException("I/O Error while reading zip. (" + (msg == null ? "null": msg) + ").", e);
			}
		}

		@Override
		public void close() throws IOException {
			zipStream.close();
		}

	}
	
	public class ZipOutputDataSourceStream implements Closeable {

		private final ZipOutputStream zipStream;
		private int count = 0;
		
		private ZipOutputDataSourceStream(OutputStream outputStream) {
			this.zipStream = new ZipOutputStream(outputStream); 
		}
		
		public ZipOutputDataSourceStream putNextDataSource(DataSource dataSource) throws IOException, ZipMetadataException {
			ZipEntry zipEntry = new ZipEntry(String.format("Entry_%05d", ++count));
			String comment = encodeMetadata(dataSource.name(), dataSource.contentType(), dataSource.filename());
			zipEntry.setComment(comment);
			zipStream.putNextEntry(zipEntry);
			try (InputStream inputStream = dataSource.inputStream()) {
				Jdk8Utils.transfer(inputStream, zipStream);
			}
			zipStream.closeEntry();
			return this;
		}

		@Override
		public void close() throws IOException {
			zipStream.close();
		}
		
	}

	public static class ZipMetadata {
		
		private static final String DATA_SOURCE_METADATA_ELEMENT = "DataSourceMetadata";
		private static final String DATASOURCE_NAME_ATTR = "DataSourceName";
		private static final String MIMETYPE_ATTR = "MimeType";
		private static final String FILENAME_ATTR = "Filename";
		private final String datasourceName;
		private final MimeType mimeType;
		private final Optional<Path> filename;
		
		private ZipMetadata(String datasourceName, MimeType mimeType, Optional<Path> filename) {
			super();
			this.datasourceName = datasourceName;
			this.mimeType = mimeType;
			this.filename = filename;
		}

		public String datasourceName() {
			return datasourceName;
		}
		
		public MimeType mimeType() {
			return mimeType;
		}

		public Optional<Path> filename() {
			return filename;
		}

		public static String encodeMetadata(String datasourceName, MimeType mimeType, Optional<Path> filename) {
			try (StringWriter stringWriter = new StringWriter()) {
				XMLStreamWriter xmlWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(stringWriter);
				xmlWriter.writeStartDocument();
				xmlWriter.writeStartElement(DATA_SOURCE_METADATA_ELEMENT);
				xmlWriter.writeAttribute(DATASOURCE_NAME_ATTR, datasourceName);
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
			String datasourceName = null;
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
								if (attName.equals(DATASOURCE_NAME_ATTR)) {
									datasourceName = xmlReader.getAttributeValue(i);
								} else if (attName.equals(MIMETYPE_ATTR)) {
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
				// If datasourceName or mimetype weren't found, then return empty, otherwise use it to create a ZipMetadata object and return that.
				return (datasourceName == null || mimeType == null) ? 
						  	Optional.empty()
						  : Optional.of(new ZipMetadata(datasourceName, MimeType.of(mimeType), Optional.ofNullable(filename).map(Paths::get)));
			} catch (XMLStreamException e) {
				String msg = e.getMessage();
				throw new ZipMetadataException("Error while decoding zip metadata (" + (msg == null ? "null" : msg) + ").", e);
			}
		}

		@SuppressWarnings("serial")
		public static class ZipMetadataException extends RuntimeException {

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
