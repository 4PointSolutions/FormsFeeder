package com._4point.aem.formsfeeder.core.datasource;

import java.util.List;
import java.util.Map;

public class UnmodifiableFileExtensionsMap extends AbstractFileExtensionsMap implements MimeTypeFileTypeMap {
	
	public static final UnmodifiableFileExtensionsMap DEFAULT_MAP = 
			UnmodifiableFileExtensionsMap.from( new FileExtensionsEntry[] { 
				FileExtensionsEntry.of("application/pdf", new String[] { "pdf" }),
				FileExtensionsEntry.of("application/vnd.ms-word", new String[] {"doc"})
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
