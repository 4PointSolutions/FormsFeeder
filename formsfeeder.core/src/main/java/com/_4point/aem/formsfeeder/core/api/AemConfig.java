package com._4point.aem.formsfeeder.core.api;

public interface AemConfig {
	public enum Protocol {
		HTTP("http"), HTTPS("https");
		
		private final String protocolString;

		private Protocol(String protocolString) {
			this.protocolString = protocolString;
		}

		public final String toProtocolString() {
			return protocolString;
		}
		
		public static final Protocol from(String string) {
			for (Protocol value : Protocol.values()) {
				if (value.protocolString.equalsIgnoreCase(string)) {
					return value;
				}
			}
			throw new IllegalArgumentException("Invalid protocol string (" + string + ").");
		}
	};
	
	public String   host();
	public int      port();
	public String   username();
	public String   secret();
	public Protocol protocol();
	
	default public String url() {
		return protocol().toProtocolString() + "://" + host() + (port() != 80 ? ":" + port() : "") + "/";
	}
}
