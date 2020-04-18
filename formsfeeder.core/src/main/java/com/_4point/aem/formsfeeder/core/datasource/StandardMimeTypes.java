package com._4point.aem.formsfeeder.core.datasource;

public class StandardMimeTypes {

	public static String APPLICATION_PDF_STR = "application/pdf";
	public static MimeType APPLICATION_PDF_TYPE = MimeType.of(APPLICATION_PDF_STR);
	public static String APPLICATION_XML_STR = "application/xml";
	public static MimeType APPLICATION_XML_TYPE = MimeType.of(APPLICATION_XML_STR);
	
	public static String APPLICATION_VND_ADOBE_XDP_STR = "application/vnd.adobe.xdp+xml";
	public static MimeType APPLICATION_VND_ADOBE_XDP_TYPE = MimeType.of(APPLICATION_VND_ADOBE_XDP_STR);
	public static String APPLICATION_VND_ADOBE_CENTRAL_FNF_STR = "application/vnd.adobe.central.field-nominated";
	public static MimeType APPLICATION_VND_ADOBE_CENTRAL_FNF_TYPE = MimeType.of(APPLICATION_VND_ADOBE_CENTRAL_FNF_STR);
	public static String TEXT_PLAIN_STR = "text/plain";
	public static MimeType TEXT_PLAIN_TYPE = MimeType.of(TEXT_PLAIN_STR);
	public static String TEXT_HTML_STR = "text/html";
	public static MimeType TEXT_HTML_TYPE = MimeType.of(TEXT_HTML_STR);
	// 
	// Less common file types
	public static String APPLICATION_MSWORD_STR = "application/msword";
	public static MimeType APPLICATION_MSWORD_TYPE = MimeType.of(APPLICATION_MSWORD_STR);
	public static String APPLICATION_VND_OPENXML_DOC_STR = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	public static MimeType APPLICATION_VND_OPENXML_DOC_TYPE = MimeType.of(APPLICATION_VND_OPENXML_DOC_STR);
	public static String APPLICATION_VND_MS_EXCEL_STR = "application/vnd.ms-excel";
	public static MimeType APPLICATION_VND_MS_EXCEL_TYPE = MimeType.of(APPLICATION_VND_MS_EXCEL_STR);
	public static String APPLICATION_VND_ADOBE_XFDF_STR = "application/vnd.adobe.xfdf";
	public static MimeType APPLICATION_VND_ADOBE_XFDF_TYPE = MimeType.of(APPLICATION_VND_ADOBE_XFDF_STR);

	// Default
	public static String APPLICATION_OCTET_STREAM_STR = "application/octet-stream";
	public static MimeType APPLICATION_OCTET_STREAM_TYPE = MimeType.of(APPLICATION_OCTET_STREAM_STR);

	private StandardMimeTypes() {
	}

}
