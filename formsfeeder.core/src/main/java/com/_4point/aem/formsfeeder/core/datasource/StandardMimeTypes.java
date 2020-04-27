package com._4point.aem.formsfeeder.core.datasource;

import java.nio.charset.StandardCharsets;

public class StandardMimeTypes {

	public static final String APPLICATION_PDF_STR = "application/pdf";
	public static final MimeType APPLICATION_PDF_TYPE = MimeType.of(APPLICATION_PDF_STR);
	public static final String APPLICATION_XML_STR = "application/xml";
	public static final MimeType APPLICATION_XML_TYPE = MimeType.of(APPLICATION_XML_STR);
	
	public static final String APPLICATION_VND_ADOBE_XDP_STR = "application/vnd.adobe.xdp+xml";
	public static final MimeType APPLICATION_VND_ADOBE_XDP_TYPE = MimeType.of(APPLICATION_VND_ADOBE_XDP_STR);
	public static final String APPLICATION_VND_ADOBE_CENTRAL_FNF_STR = "application/vnd.adobe.central.field-nominated";
	public static final MimeType APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE = MimeType.of(APPLICATION_VND_ADOBE_CENTRAL_FNF_STR);
	public static final String TEXT_PLAIN_STR = "text/plain";
	public static final MimeType TEXT_PLAIN_TYPE = MimeType.of(TEXT_PLAIN_STR);
	public static final MimeType TEXT_PLAIN_UTF8_TYPE = MimeType.of("text", "plain", StandardCharsets.UTF_8);
	public static final String TEXT_HTML_STR = "text/html";
	public static final MimeType TEXT_HTML_TYPE = MimeType.of(TEXT_HTML_STR);
	// 
	// Less common file types
	public static final String APPLICATION_MSWORD_STR = "application/msword";
	public static final MimeType APPLICATION_MSWORD_TYPE = MimeType.of(APPLICATION_MSWORD_STR);
	public static final String APPLICATION_VND_OPENXML_DOC_STR = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	public static final MimeType APPLICATION_VND_OPENXML_DOC_TYPE = MimeType.of(APPLICATION_VND_OPENXML_DOC_STR);
	public static final String APPLICATION_VND_MS_EXCEL_STR = "application/vnd.ms-excel";
	public static final MimeType APPLICATION_VND_MS_EXCEL_TYPE = MimeType.of(APPLICATION_VND_MS_EXCEL_STR);
	public static final String APPLICATION_VND_ADOBE_XFDF_STR = "application/vnd.adobe.xfdf";
	public static final MimeType APPLICATION_VND_ADOBE_XFDF_TYPE = MimeType.of(APPLICATION_VND_ADOBE_XFDF_STR);

	// Default
	public static final String APPLICATION_OCTET_STREAM_STR = "application/octet-stream";
	public static final MimeType APPLICATION_OCTET_STREAM_TYPE = MimeType.of(APPLICATION_OCTET_STREAM_STR);

	// Prevent instantiation.
	private StandardMimeTypes() {
	}

}
