/**
 * 
 */
package com._4point.aem.formsfeeder.core.datasource;

import java.io.InputStream;
import java.io.OutputStream;
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
	}

	protected AbstractDataSource(String name) {
		super();
		this.name = name;
	}

	protected AbstractDataSource(String name, Map<String, String> attributes) {
		super();
		this.name = name;
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
	public MimeType getContentType() {
		return contentType;
	}

	@Override
	public abstract InputStream getInputStream();

	@Override
	public String getName() {
		return name;
	}

	@Override
	public abstract OutputStream getOutputStream();

	@Override
	public Optional<Path> getFilename() {
		return Optional.empty();
	}

	@Override
	public Map<String, String> getAttributes() {
		return Map.copyOf(attributes);
	}

	protected Map<String, String> getAttributeMap() {
		return attributes;
	}
	
	protected AbstractDataSource setContentType(String contentType) {
		this.contentType = MimeType.of(contentType);
		return this;
	}
	
	protected AbstractDataSource setContentType(MimeType contentType) {
		this.contentType = contentType;
		return this;
	}
	
	protected AbstractDataSource setName(String name) {
		this.name = name;
		return this;
	}
}
