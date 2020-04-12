package com._4point.aem.formsfeeder.core.datasource;

import java.nio.file.Path;
import java.util.Optional;

public interface MimeTypeFileTypeMap {
	/**
	 * Returns the mime-type for a file.
	 * 
	 * @param filePath
	 * @return
	 */
	public Optional<MimeType> getMimeType(Path filePath);
	
	/**
	 * Returns the default file extension for a given mime-type
	 * 
	 * @param mimeType
	 * @return
	 */
	public Optional<String> getFileDefaultExtension(MimeType mimeType);
}
