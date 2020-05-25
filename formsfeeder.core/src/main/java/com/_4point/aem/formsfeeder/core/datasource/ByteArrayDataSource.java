package com._4point.aem.formsfeeder.core.datasource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/* package */ class ByteArrayDataSource extends AbstractDataSource implements DataSource {

	private byte[] contents;
	private Path filename = null;
	
	/* package */ ByteArrayDataSource() {
		super();
		this.contents = new byte[0];
	}

	/* package */ ByteArrayDataSource(byte[] contents) {
		super();
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	/* package */ ByteArrayDataSource(byte[] contents, String name) {
		super(name);
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	/* package */ ByteArrayDataSource(byte[] contents, String name, Map<String, String> attributes) {
		super(name, attributes);
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	/* package */ ByteArrayDataSource(byte[] contents, String name, MimeType contentType) {
		super(name, contentType);
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	/* package */ ByteArrayDataSource(byte[] contents, String name, MimeType contentType, Map<String, String> attributes) {
		super(name, contentType, attributes);
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	/* package */ final byte[] getContents() {
		return contents;
	}

	@Override
	public InputStream inputStream() {
		return wrapInputStream(()->new ByteArrayInputStream(contents));
	}

	@Override
	public OutputStream outputStream() {
		return wrapOutputStream(()->new LocalByteArrayOutputStream(new ByteArrayOutputStream()));
	}

	@Override
	public Optional<Path> filename() {
		return Optional.ofNullable(filename);
	}

	final void filename(Path filename) {
		this.filename = filename;
	}

	private class LocalByteArrayOutputStream extends OutputStream {
		private final ByteArrayOutputStream bos;

		private LocalByteArrayOutputStream(ByteArrayOutputStream bos) {
			super();
			this.bos = bos;
		}

		@Override
		public void write(byte[] b) throws IOException {
			bos.write(b);
		}

		@Override
		public void write(int b) {
			bos.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) {
			bos.write(b, off, len);
		}

		@Override
		public void close() throws IOException {
			bos.close();
			ByteArrayDataSource.this.contents = bos.toByteArray();
		}

		@Override
		public void flush() throws IOException {
			bos.flush();
		}
	}
}
