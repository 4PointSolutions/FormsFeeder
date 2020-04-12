package com._4point.aem.formsfeeder.core.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ModifiableFileExtensionsMap extends AbstractFileExtensionsMap implements MimeTypeFileTypeMap {
	
	public ModifiableFileExtensionsMap() {
		super(new ArrayList<>(), new HashMap<>(), new HashMap<>());
	}
	
	public ModifiableFileExtensionsMap(AbstractFileExtensionsMap map) {
		super(new ArrayList<>(), new HashMap<>(), new HashMap<>());
		this.entries.addAll(map.entries);
		this.byMimeType.putAll(map.byMimeType);
		this.byExtension.putAll(map.byExtension);
	}

	public ModifiableFileExtensionsMap addMapping(MimeType mimeType, String defaultExtension, Set<String> extensions) {
		// TODO: Add code to validate all the provided extensions do not exist in any previous mappings.
		// TODO: Add mapping to the collection.
		return this;
	}

}
