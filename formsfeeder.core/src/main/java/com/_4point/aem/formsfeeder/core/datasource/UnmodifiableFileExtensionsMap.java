package com._4point.aem.formsfeeder.core.datasource;

import java.util.List;
import java.util.Map;

public class UnmodifiableFileExtensionsMap extends AbstractFileExtensionsMap implements MimeTypeFileTypeMap {
	
	public static final UnmodifiableFileExtensionsMap DEFAULT_MAP = 
			UnmodifiableFileExtensionsMap.from( new FileExtensionsEntry[] {
				// Common AEM File types we must deal with
				FileExtensionsEntry.of("application/pdf", new String[] { "pdf" }),
				FileExtensionsEntry.of("application/xml", new String[] { "xml" }),
				FileExtensionsEntry.of("application/vnd.adobe.xdp+xml", new String[] { "xdp" }),
				FileExtensionsEntry.of("application/vnd.adobe.central.field-nominated", new String[] { "dat" }),
				FileExtensionsEntry.of("text/plain", new String[] { "txt" }),
				FileExtensionsEntry.of("text/html", new String[] { "html", "htm" }),
				// 
				// Less common file types
				FileExtensionsEntry.of("application/msword", new String[] {"doc"}),
				FileExtensionsEntry.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", new String[] {"docx"}),
				FileExtensionsEntry.of("application/vnd.ms-excel", new String[] {"xls", "xlsx"}),
				FileExtensionsEntry.of("application/vnd.adobe.xfdf", new String[] {"xfdf"})
			} );
	
	private UnmodifiableFileExtensionsMap(AbstractFileExtensionsMap map) {
		super(List.copyOf(map.entries), Map.copyOf(map.byMimeType), Map.copyOf(map.byExtension));
	}

	private UnmodifiableFileExtensionsMap(List<FileExtensionsEntry> entries,
			Map<MimeType, FileExtensionsEntry> byMimeType, Map<String, FileExtensionsEntry> byExtension) {
		super(entries, byMimeType, byExtension);
	}

	private UnmodifiableFileExtensionsMap(FileExtensionsEntry[] entries) {
		super(entries);
	}

	private static UnmodifiableFileExtensionsMap from(FileExtensionsEntry[] entries) {
		return new UnmodifiableFileExtensionsMap(entries);
	}

	public static UnmodifiableFileExtensionsMap from(AbstractFileExtensionsMap map) {
		return new UnmodifiableFileExtensionsMap(map);
	}
}
