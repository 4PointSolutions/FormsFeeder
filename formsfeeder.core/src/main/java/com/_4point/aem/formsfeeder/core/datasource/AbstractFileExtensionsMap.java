package com._4point.aem.formsfeeder.core.datasource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractFileExtensionsMap implements MimeTypeFileTypeMap {

	protected final List<FileExtensionsEntry> entries;
	protected final Map<MimeType, FileExtensionsEntry> byMimeType;
	protected final Map<String, FileExtensionsEntry> byExtension;
	
	protected AbstractFileExtensionsMap(List<FileExtensionsEntry> entries,
			Map<MimeType, FileExtensionsEntry> byMimeType, Map<String, FileExtensionsEntry> byExtension) {
		super();
		this.entries = entries;
		this.byMimeType = byMimeType;
		this.byExtension = byExtension;
	}

	protected AbstractFileExtensionsMap(FileExtensionsEntry[] entries) {
		this.entries = List.of(entries);
		this.byMimeType = Arrays.stream(entries).collect(Collectors.toUnmodifiableMap(FileExtensionsEntry::getMimeType, Function.identity()));
		this.byExtension = Arrays.stream(entries)
				.flatMap(ExtensionRecord::from)
				.collect(Collectors.toUnmodifiableMap((er)->er.extension, (er)->er.localEntry));
	}

	protected AbstractFileExtensionsMap(FileExtensionsEntry[] entries, Supplier<List<FileExtensionsEntry>> listSupplier, Supplier<Map<MimeType, FileExtensionsEntry>> mimeMapSupplier, Supplier<Map<String, FileExtensionsEntry>> extensionMapSupplier) {
		this.entries = listSupplier.get();
		this.entries.addAll(List.of(entries));
		
		this.byMimeType = Arrays.stream(entries).collect(Collectors.toMap(FileExtensionsEntry::getMimeType, Function.identity(), (e1, e2)->e1, mimeMapSupplier));
		this.byExtension = Arrays.stream(entries)
				.flatMap(ExtensionRecord::from)
				.collect(Collectors.toMap((er)->er.extension, (er)->er.localEntry,  (e1, e2)->e1, extensionMapSupplier));
	}

	@Override
	public Optional<MimeType> mimeType(Path filePath) {
		String filename = filePath.getFileName().toString();
		int indexOfExtension = filename.lastIndexOf('.');
		if (indexOfExtension < 0) {
			throw new IllegalArgumentException("File Path provided does not have an extension. (" + filePath.toString() + ");");
		}
		FileExtensionsEntry fileExtensionsEntry = byExtension.get(filename.substring(indexOfExtension+1));
		return fileExtensionsEntry == null ? Optional.empty() : Optional.of(fileExtensionsEntry.getMimeType());
	}

	@Override
	public Optional<String> fileDefaultExtension(MimeType mimeType) {
		FileExtensionsEntry fileExtensionsEntry = byMimeType.get(mimeType);
		return fileExtensionsEntry == null ? Optional.empty() : Optional.of(fileExtensionsEntry.getDefaultExtension());
	}

	protected static class FileExtensionsEntry {
		private final MimeType mimeType;
		private final String defaultExtension;
		private final Set<String> extensions;

		protected FileExtensionsEntry(MimeType mimeType, String defaultExtension, Set<String> extensions) {
			super();
			this.mimeType = mimeType;
			this.defaultExtension = defaultExtension;
			this.extensions = Set.copyOf(extensions);
		}

		public MimeType getMimeType() {
			return mimeType;
		}

		public String getDefaultExtension() {
			return defaultExtension;
		}

		public Set<String> getExtensions() {
			return extensions;
		}
		
		protected static FileExtensionsEntry of(MimeType mimeType, String[] extensions) {
			return new FileExtensionsEntry(mimeType, extensions[0], Set.of(extensions));
		}
		protected static FileExtensionsEntry of(String mimeType, String[] extensions) {
			return new FileExtensionsEntry(MimeType.of(mimeType), extensions[0], Set.of(extensions));
		}
	}
	
	private static final class ExtensionRecord {
		public String extension;
		public FileExtensionsEntry localEntry;
		public ExtensionRecord(String extension, FileExtensionsEntry localentry) {
			super();
			this.extension = extension;
			this.localEntry = localentry;
		}
	    public static Stream<ExtensionRecord> from(FileExtensionsEntry entry) {
	    	return entry.getExtensions().stream().map(e->new ExtensionRecord(e, entry));
	    }
	};
	

}
