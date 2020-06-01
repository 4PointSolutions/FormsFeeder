package formsfeeder.client.cli.parameters;

import org.apache.commons.cli.ParseException;

public class HostParameters {

	private final boolean useSsl;
	private final String hostname;
	private final int hostPort;

	private HostParameters(boolean useSsl, String hostname, int hostPort) {
		super();
		this.useSsl = useSsl;
		this.hostname = hostname;
		this.hostPort = hostPort;
	}
	
	public final boolean useSsl() {
		return useSsl;
	}

	public final String hostname() {
		return hostname;
	}

	public final int hostPort() {
		return hostPort;
	}

	/**
	 * Expect hostString to be in the form http://hostname:port or https://hostname:port.
	 * 
	 * @param hostString
	 * @return
	 * @throws ParseException 
	 */
	public static HostParameters from(String hostString) throws ParseException {
		boolean useSsl = hostString.startsWith("https");
		
		String[] hostParamStrings = splitHostnamePort(stripTrailingSlash(stripLeadingPrefix(hostString)));
		
		if (hostParamStrings.length > 2) {
			throw new ParseException("Error parsing host string '" + hostString + "'.");
		} else if (hostParamStrings.length < 2) {
			// No port was provided.
			return new HostParameters(useSsl, hostParamStrings[0], 80);
		} else {
			// Hostname and port provided.
			return new HostParameters(useSsl, hostParamStrings[0], Integer.parseInt(hostParamStrings[1]));
		}
	}

	private static String stripLeadingPrefix(String hostString) {
		return hostString.substring(hostString.indexOf("//") + 2);
	}
	
	private static String stripTrailingSlash(String s) {
		return s.endsWith("/") ? s.substring(0,  s.indexOf("/")) : s;
	}
	
	private static String[] splitHostnamePort(String s) {
		return s.split(":");
	}
}
