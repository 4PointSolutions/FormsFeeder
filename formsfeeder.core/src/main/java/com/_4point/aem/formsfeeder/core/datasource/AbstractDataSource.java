/**
 * 
 */
package com._4point.aem.formsfeeder.core.datasource;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract Data Source, provides a default implementation for handling attributes.
 *
 */
public abstract class AbstractDataSource implements DataSource {
	private String name;
	private MimeType contentType;
	private final Map<String, String> attributes = new HashMap<>();
	
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
}
