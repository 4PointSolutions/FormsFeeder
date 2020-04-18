package com._4point.aem.formsfeeder.core.datasource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

public class ByteArrayDataSource extends AbstractDataSource implements DataSource {

	private final byte[] contents;
	
	public ByteArrayDataSource(byte[] contents) {
		super();
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	public ByteArrayDataSource(byte[] contents, String name) {
		super(name);
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	public ByteArrayDataSource(byte[] contents, String name, Map<String, String> attributes) {
		super(name, attributes);
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	public ByteArrayDataSource(byte[] contents, String name, MimeType contentType) {
		super(name, contentType);
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	public ByteArrayDataSource(byte[] contents, String name, MimeType contentType, Map<String, String> attributes) {
		super(name, contentType, attributes);
		this.contents = Arrays.copyOf(contents, contents.length);
	}

	@Override
	public InputStream inputStream() {
		return new ByteArrayInputStream(contents);
	}

	@Override
	public OutputStream outputStream() {
		throw new IllegalStateException("outputStream() is not implemented for " + this.getClass().getName() + ".");
	}

}
