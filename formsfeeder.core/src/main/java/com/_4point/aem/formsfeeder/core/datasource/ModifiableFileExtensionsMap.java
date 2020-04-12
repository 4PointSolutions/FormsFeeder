package com._4point.aem.formsfeeder.core.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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

	/**
	 * Puts an entry into the current Map.  If the MimeType already exists, then it replaces the existing one.
	 * 
	 * @param mimeType
	 * @param extensions
	 * @return
	 */
	public ModifiableFileExtensionsMap putMapping(MimeType mimeType, List<String> extensions) {
		if (extensions == null || extensions.isEmpty()) {
			throw new IllegalArgumentException("List of extensions cannot be null or empty.");
		}
		FileExtensionsEntry newEntry = new FileExtensionsEntry(mimeType, extensions.get(0), Set.copyOf(extensions));
		FileExtensionsEntry oldEntry = validate(mimeType, extensions);
		if (oldEntry != null) {
			removeEntry(oldEntry);
		}
		addEntry(newEntry);
		return this;
	}

	// Returns the old entry if the mimetype is already in the list.
	// Throws an IllegalArgumentException if any of the extensions are already used
	// by another entry
	private FileExtensionsEntry validate(MimeType mimeType, List<String> extensions) {
		FileExtensionsEntry oldEntry = this.byMimeType.get(mimeType);
		
		for (String extension : extensions) {
			FileExtensionsEntry extEntry = this.byExtension.get(extension);
			if (extEntry != null && extEntry != oldEntry) {
				throw new IllegalArgumentException("Cannot add a duplicate extension (" + extension + "), already connected to mime-type '" + extEntry.getMimeType().asString() + "'.");
			}
		}
		
		return oldEntry;
	}
	
	// Adds Entry into this Map
	private ModifiableFileExtensionsMap addEntry(FileExtensionsEntry newEntry) {
		this.entries.add(newEntry);
		this.byMimeType.put(newEntry.getMimeType(), newEntry);
		for (String key : newEntry.getExtensions()) {
			this.byExtension.put(key, newEntry);
		}
		return this;
	}

	// removes Entry from this Map
	private ModifiableFileExtensionsMap removeEntry(FileExtensionsEntry oldEntry) {
		this.entries.remove(oldEntry);
		this.byMimeType.remove(oldEntry.getMimeType());
		for (String key : oldEntry.getExtensions()) {
			this.byExtension.remove(key);
		}
		return this;
	}
}
