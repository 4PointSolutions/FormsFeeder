package com._4point.aem.formsfeeder.core.datasource;

import java.util.List;
import java.util.Map;

import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

public class UnmodifiableFileExtensionsMap extends AbstractFileExtensionsMap implements MimeTypeFileTypeMap {
	
	public static final UnmodifiableFileExtensionsMap DEFAULT_MAP = 
			UnmodifiableFileExtensionsMap.from( new FileExtensionsEntry[] {
				// Common AEM File types we must deal with
				FileExtensionsEntry.of(StandardMimeTypes.APPLICATION_PDF_TYPE, new String[] { "pdf" }),
				FileExtensionsEntry.of(StandardMimeTypes.APPLICATION_XML_TYPE, new String[] { "xml" }),
				FileExtensionsEntry.of(StandardMimeTypes.APPLICATION_VND_ADOBE_XDP_TYPE, new String[] { "xdp" }),
				FileExtensionsEntry.of(StandardMimeTypes.APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE, new String[] { "dat" }),
				FileExtensionsEntry.of(StandardMimeTypes.TEXT_PLAIN_TYPE, new String[] { "txt" }),
				FileExtensionsEntry.of(StandardMimeTypes.TEXT_HTML_TYPE, new String[] { "html", "htm" }),
				// 
				// Less common file types
				FileExtensionsEntry.of(StandardMimeTypes.APPLICATION_MSWORD_TYPE, new String[] {"doc"}),
				FileExtensionsEntry.of(StandardMimeTypes.APPLICATION_VND_OPENXML_DOC_TYPE, new String[] {"docx"}),
				FileExtensionsEntry.of(StandardMimeTypes.APPLICATION_VND_MS_EXCEL_TYPE, new String[] {"xls", "xlsx"}),
				FileExtensionsEntry.of(StandardMimeTypes.APPLICATION_VND_ADOBE_XFDF_TYPE, new String[] {"xfdf"})
			} );
	
	private UnmodifiableFileExtensionsMap(AbstractFileExtensionsMap map) {
		super(Jdk8Utils.copyOfList(map.entries), Jdk8Utils.copyOfMap(map.byMimeType), Jdk8Utils.copyOfMap(map.byExtension));
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
