package com._4point.aem.formsfeeder.core.datasource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StringDataSource extends AbstractDataSource implements DataSource {

	public static final Charset ENCODING = StandardCharsets.UTF_8;
	
	private static final MimeType MIME_TYPE = StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE;
	private String contents; 
	
	public StringDataSource() {
		super("", MIME_TYPE);
		this.contents = "";
	}

	public StringDataSource(String contents) {
		super("", MIME_TYPE);
		this.contents = contents;
	}

	public StringDataSource(String contents, String name) {
		super(name, MIME_TYPE);
		this.contents = contents;
	}

	public StringDataSource(String contents, String name, Map<String, String> attributes) {
		super(name, MIME_TYPE, attributes);
		this.contents = contents;
	}

	@Override
	public InputStream inputStream() {
		return wrapInputStream(()->new ByteArrayInputStream(contents.getBytes(ENCODING)));
	}

	@Override
	public OutputStream outputStream() {
		return wrapOutputStream(()->new LocalByteArrayOutputStream(new ByteArrayOutputStream()));
	}

	public final String contents() {
		return contents;
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
			StringDataSource.this.contents = new String(bos.toByteArray(), ENCODING);
		}

		@Override
		public void flush() throws IOException {
			bos.flush();
		}
	}
}
