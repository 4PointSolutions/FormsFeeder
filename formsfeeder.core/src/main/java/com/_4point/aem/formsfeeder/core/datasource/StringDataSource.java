package com._4point.aem.formsfeeder.core.datasource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StringDataSource extends AbstractDataSource implements DataSource {

	private static final Charset ENCODING = StandardCharsets.UTF_8;
	private static final MimeType MIME_TYPE = MimeType.of("text", "plain", ENCODING);
	private final String contents; 
	
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
		return new ByteArrayInputStream(contents.getBytes(ENCODING));
	}

	@Override
	public OutputStream outputStream() {
		throw new IllegalStateException("outputStream() is not implemented for " + this.getClass().getName() + ".");
	}

	public final String contents() {
		return contents;
	}
}
