package formsfeeder.client.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

/**
 * This interface is is for a Builder object that assists in building a JAX-RS client.  It is shared between the following projects:
 * fluentforms/rest-services.client
 * formsfeeder.client
 * 
 * If changes are made to this file, those changes should be propagated into each of the other projects.
 * These files are copied because there is not enough code to justify creating another project and adding another dependency. 
 *
 */
public interface Builder {

	public Builder machineName(String machineName);

	public Builder port(int port);

	public Builder useSsl(boolean useSsl);

	public Builder contextRoot(String contextRoot);

	public Builder clientFactory(Supplier<Client> clientFactory);

	public Builder basicAuthentication(String username, String password);

	public Map<String, List<Supplier<String>>> getQueryParams();

	public Builder addQueryParam(String name, List<Supplier<String>> values);

	public default Builder addQueryParam(String name, Supplier<String> value) {
		this.addQueryParam(name, Collections.singletonList(value));	// List.of() would be better, but we're in Java 8 land here
		return this;
	}

	public default Builder addQueryParam(String name, String value) {
		this.addQueryParam(name, ()->value);
		return this;
	}

	public Builder correlationId(Supplier<String> correlationIdFn);

	public Builder addHeader(String header, Supplier<String> value);

	public Map<String, Supplier<String>> getHeaderMap();

	public Supplier<String> getCorrelationIdFn();

	public WebTarget createLocalTarget();


}
