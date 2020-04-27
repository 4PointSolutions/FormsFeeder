/**
 * 
 */
package com._4point.aem.formsfeeder.core.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Abstract Data Source, provides a default implementation for handling attributes.
 *
 */
public abstract class AbstractDataSource implements DataSource {
	private String name;
	private MimeType contentType;
	private final Map<String, String> attributes = new HashMap<>();
	private int isCounter = 0;	// Count of open InputStreams
	private int osCounter = 0;	// Count of open OutputStreams
	
	protected AbstractDataSource() {
		super();
		this.name = "";
		this.contentType = StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE;
	}

	protected AbstractDataSource(String name) {
		super();
		this.name = name;
		this.contentType = StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE;
	}

	protected AbstractDataSource(String name, Map<String, String> attributes) {
		super();
		this.name = name;
		this.contentType = StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE;
		this.attributes.putAll(attributes);
	}

	protected AbstractDataSource(String name, MimeType contentType) {
		super();
		this.name = name;
		this.contentType = contentType;
	}

	protected AbstractDataSource(String name, MimeType contentType, Map<String, String> attributes) {
		super();
		this.name = name;
		this.contentType = contentType;
		this.attributes.putAll(attributes);
	}

	@Override
	public MimeType contentType() {
		return contentType;
	}

	@Override
	public abstract InputStream inputStream();

	@Override
	public String name() {
		return name;
	}

	@Override
	public abstract OutputStream outputStream();

	@Override
	public Optional<Path> filename() {
		return Optional.empty();
	}

	@Override
	public Map<String, String> attributes() {
		// Return a defensive copy
		return Map.copyOf(attributes);
	}

	protected Map<String, String> attributeMap() {
		return attributes;
	}
	
	protected AbstractDataSource contentType(String contentType) {
		this.contentType = MimeType.of(contentType);
		return this;
	}
	
	protected AbstractDataSource contentType(MimeType contentType) {
		this.contentType = contentType;
		return this;
	}
	
	protected AbstractDataSource name(String name) {
		this.name = name;
		return this;
	}
	
	protected InputStream wrapInputStream(Supplier<InputStream> isSupplier) {
		if (osCounter > 0) {
			throw new IllegalStateException("Cannot open input stream while output stream is open.");
		}
		return new InputStreamCounter(isSupplier.get());
	}
	
	private class InputStreamCounter extends InputStream {
		private final InputStream is;

		private InputStreamCounter(InputStream is) {
			super();
			this.is = is;
			isCounter++;
		}

		@Override
		public int hashCode() {
			return is.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return is.equals(obj);
		}

		@Override
		public int read() throws IOException {
			return is.read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			return is.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return is.read(b, off, len);
		}

		@Override
		public String toString() {
			return is.toString();
		}

		@Override
		public byte[] readAllBytes() throws IOException {
			return is.readAllBytes();
		}

		@Override
		public byte[] readNBytes(int len) throws IOException {
			return is.readNBytes(len);
		}

		@Override
		public int readNBytes(byte[] b, int off, int len) throws IOException {
			return is.readNBytes(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException {
			return is.skip(n);
		}

		@Override
		public int available() throws IOException {
			return is.available();
		}

		@Override
		public void close() throws IOException {
			is.close();
			isCounter--;
		}

		@Override
		public void mark(int readlimit) {
			is.mark(readlimit);
		}

		@Override
		public void reset() throws IOException {
			is.reset();
		}

		@Override
		public boolean markSupported() {
			return is.markSupported();
		}

		@Override
		public long transferTo(OutputStream out) throws IOException {
			return is.transferTo(out);
		}

	}
	
	protected OutputStream wrapOutputStream(Supplier<OutputStream> osSupplier) {
		if (isCounter > 0) {
			throw new IllegalStateException("Cannot open output stream while input stream is open.");
		}
		return new OutputStreamCounter(osSupplier.get());
	}
	
	private class OutputStreamCounter extends OutputStream {
		private final OutputStream os;

		private OutputStreamCounter(OutputStream os) {
			super();
			this.os = os;
			osCounter++;
		}

		@Override
		public int hashCode() {
			return os.hashCode();
		}

		@Override
		public void write(int b) throws IOException {
			os.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			os.write(b);
		}

		@Override
		public boolean equals(Object obj) {
			return os.equals(obj);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			os.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			os.flush();
		}

		@Override
		public void close() throws IOException {
			os.close();
			osCounter--;
		}

		@Override
		public String toString() {
			return os.toString();
		}
	}
}
