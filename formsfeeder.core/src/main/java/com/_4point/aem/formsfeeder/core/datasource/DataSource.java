package com._4point.aem.formsfeeder.core.datasource;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public interface DataSource {
	MimeType contentType();
	InputStream inputStream();
	String name();	// Cannot be null, all data sources must have a name (empty string is valid though).
	OutputStream outputStream();
	Optional<Path> filename();
	Map<String, String> attributes();
}
