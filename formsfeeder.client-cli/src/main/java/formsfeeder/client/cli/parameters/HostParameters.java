package formsfeeder.client.cli.parameters;

import org.apache.commons.cli.ParseException;

public class HostParameters {

	private final boolean useSsl;
	private final String hostName;
	private final int hostPort;

	private HostParameters(boolean useSsl, String hostname, int hostPort) {
		super();
		this.useSsl = useSsl;
		this.hostName = hostname;
		this.hostPort = hostPort;
	}
	
	public final boolean useSsl() {
		return useSsl;
	}

	public final String hostName() {
		return hostName;
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
		
		if (!hostString.startsWith("http")) {
			throw new ParseException("Bad Protocol specified (" + hostString.substring(0, hostString.indexOf(":")) + ").  Only http/https is supported.");
		}
		boolean useSsl = hostString.startsWith("https");
		
		String[] hostParamStrings = splitHostnamePort(stripTrailingSlash(stripLeadingPrefix(hostString)));
		
		if (hostParamStrings.length > 2 || hostParamStrings[0].isEmpty() || (hostParamStrings.length > 1 && hostParamStrings[1].isEmpty())) {
			throw new ParseException("Error parsing host string '" + hostString + "'.");
		} else if (hostParamStrings.length < 2) {
			// No port was provided.
			return new HostParameters(useSsl, hostParamStrings[0], 80);
		} else {
			// Hostname and port provided.
			return new HostParameters(useSsl, hostParamStrings[0], parsePort(hostParamStrings[1]));
		}
	}

	private static int parsePort(String portString) throws ParseException {
		try {
			int portNo = Integer.parseInt(portString);
			if (portNo == 0) {
				throw new ParseException("Port Number cannot be 0.");
			}
			return portNo;
		} catch (NumberFormatException e) {
			throw new ParseException("Unable to parse Port Number. (" + e.getMessage() + ").");
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
