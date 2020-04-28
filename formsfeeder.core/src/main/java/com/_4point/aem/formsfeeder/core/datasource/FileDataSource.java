package com._4point.aem.formsfeeder.core.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class FileDataSource extends AbstractDataSource implements DataSource {
	private final Path filePath;
	
	public FileDataSource(Path filePath) {
		super("", determineMimeType(filePath, UnmodifiableFileExtensionsMap.DEFAULT_MAP));
		this.filePath = Objects.requireNonNull(filePath);
	}

	public FileDataSource(Path filePath, String name) {
		super(name, determineMimeType(filePath, UnmodifiableFileExtensionsMap.DEFAULT_MAP));
		this.filePath = Objects.requireNonNull(filePath);
	}

	public FileDataSource(Path filePath, String name, Map<String, String> attributes) {
		super(name, determineMimeType(filePath, UnmodifiableFileExtensionsMap.DEFAULT_MAP), attributes);
		this.filePath = Objects.requireNonNull(filePath);
	}

	public FileDataSource(Path filePath, MimeTypeFileTypeMap map) {
		super("", determineMimeType(filePath, map));
		this.filePath = Objects.requireNonNull(filePath);
	}

	public FileDataSource(Path filePath, String name, MimeTypeFileTypeMap map) {
		super(name, determineMimeType(filePath, map));
		this.filePath = Objects.requireNonNull(filePath);
	}

	public FileDataSource(Path filePath, String name, Map<String, String> attributes, MimeTypeFileTypeMap map) {
		super(name, determineMimeType(filePath, map), attributes);
		this.filePath = Objects.requireNonNull(filePath);
	}

	public FileDataSource(Path filePath, MimeType contentType) {
		super("", contentType);
		this.filePath = Objects.requireNonNull(filePath);
	}

	public FileDataSource(Path filePath, String name, MimeType contentType) {
		super(name, contentType);
		this.filePath = Objects.requireNonNull(filePath);
	}

	public FileDataSource(Path filePath, String name, MimeType contentType, Map<String, String> attributes) {
		super(name, contentType, attributes);
		this.filePath = Objects.requireNonNull(filePath);
	}

	private static MimeType determineMimeType(Path filePath, MimeTypeFileTypeMap map) {
		return map.mimeType(filePath).orElse(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE);
	}
	
	@Override
	public InputStream inputStream() {
		return wrapInputStream(this::internalInputStream);
	}

	// Need this to convert checked IOExceptions to unchecked IllegalArgumentExceptions
	private InputStream internalInputStream() {
		try {
			return Files.newInputStream(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to open input stream on '" + filePath + "'.", e);
		}
	}

	@Override
	public OutputStream outputStream() {
		return wrapOutputStream(this::internalOutputStream);
	}

	// Need this to convert checked IOExceptions to unchecked IllegalArgumentExceptions
	private OutputStream internalOutputStream() {
		try {
			return Files.newOutputStream(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to open output stream on '" + filePath + "'.", e);
		}
	}
	
	@Override
	public Optional<Path> filename() {
		return Optional.of(this.filePath);
	}

}
