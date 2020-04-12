package com._4point.aem.formsfeeder.core.datasource;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public interface DataSource {
	MimeType getContentType();
	InputStream getInputStream();
	String getName();
	OutputStream getOutputStream();
	Optional<Path> getFilename();
	Map<String, String> getAttributes();
}
