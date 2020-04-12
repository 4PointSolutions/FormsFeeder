package com._4point.aem.formsfeeder.core.datasource;

import java.nio.file.Path;

public interface MimeTypeFileTypeMap {
	/**
	 * Returns the mime-type for a file.
	 * 
	 * @param filePath
	 * @return
	 */
	public MimeType getMimeType(Path filePath);
	
	/**
	 * Returns the default file extension for a given mime-type
	 * 
	 * @param mimeType
	 * @return
	 */
	public String getFileDefaultExtension(MimeType mimeType);
}
