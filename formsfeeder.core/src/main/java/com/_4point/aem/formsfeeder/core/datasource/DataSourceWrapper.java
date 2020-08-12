package com._4point.aem.formsfeeder.core.datasource;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public abstract class DataSourceWrapper implements DataSource {

	private final DataSource dataSource;

	protected DataSourceWrapper(DataSource dataSource) {
		super();
		this.dataSource = dataSource;
	}
	
	@Override
	public MimeType contentType() {
		return dataSource.contentType();
	}

	@Override
	public InputStream inputStream() {
		return dataSource.inputStream();
	}

	@Override
	public String name() {
		return dataSource.name();
	}

	@Override
	public OutputStream outputStream() {
		return dataSource.outputStream();
	}

	@Override
	public Optional<Path> filename() {
		return dataSource.filename();
	}

	@Override
	public Map<String, String> attributes() {
		return dataSource.attributes();
	}

}
