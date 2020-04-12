package com._4point.aem.formsfeeder.core.datasource;

import java.nio.charset.Charset;

public class MimeType {

	private static final String CHARSET_SEPARATOR = ";";
	private static final String TYPE_SEPARATOR = "/";
	private static final String EQUALS_CHAR = "=";
	
	private final String type;
	private final String subtype;
	private final Charset charset;

	private MimeType(String type, String subtype, Charset charset) {
		super();
		this.type = type;
		this.subtype = subtype;
		this.charset = charset;
	}

	private MimeType(String type, String subtype) {
		super();
		this.type = type;
		this.subtype = subtype;
		this.charset = null;
	}

	public String getType() {
		return type;
	}

	public String getSubtype() {
		return subtype;
	}

	public Charset getCharset() {
		return charset;
	}

	public String asString() {
		String result = type + TYPE_SEPARATOR + subtype;
		if (charset != null) {
			result = result + CHARSET_SEPARATOR + " charset=" + charset.name();
		}
		return result; 
	}
	
	public static MimeType of(String mimeType) {
		String[] parts = mimeType.split(TYPE_SEPARATOR);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid content type string - '" + mimeType + "'.  Expected exactly one separator character ('" + TYPE_SEPARATOR + "').");
		}
		String type = parts[0].trim().toLowerCase();
		final String[] latterParts = parts[1].split(CHARSET_SEPARATOR);
		if (latterParts.length > 2) {
			throw new IllegalArgumentException("Invalid content type string - '" + mimeType + "'.  Expected exactly one charset-separator character ('" + CHARSET_SEPARATOR + "').");
		}
		String subtype = latterParts[0].trim().toLowerCase();
		
		if (latterParts.length > 1) {
			String[] parameterParts = latterParts[1].trim().split(EQUALS_CHAR);
			if (parameterParts.length != 2) {
				throw new IllegalArgumentException("Invalid content type string - '" + mimeType + "'.  Expected exactly one equals character ('" + EQUALS_CHAR + "').");
			}
			String parameter = parameterParts[0].trim().toLowerCase();
			if (!parameter.equals("charset")) {
				throw new IllegalArgumentException("Invalid content type string - '" + mimeType + "'.  Expected parameter to be 'charset' ('" + parameter + "').");
			}
			Charset charset = Charset.forName(parameterParts[1].trim());
			return new MimeType(type, subtype, charset);
		} else {
			return new MimeType(type, subtype);
		}
	}

	public static MimeType of(String type, String subType) {
		return new MimeType(type, subType);
	}
	
	public static MimeType of(String type, String subType, Charset charset) {
		return new MimeType(type, subType, charset);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((charset == null) ? 0 : charset.hashCode());
		result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MimeType other = (MimeType) obj;
		if (charset == null) {
			if (other.charset != null)
				return false;
		} else if (!charset.equals(other.charset))
			return false;
		if (subtype == null) {
			if (other.subtype != null)
				return false;
		} else if (!subtype.equals(other.subtype))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
