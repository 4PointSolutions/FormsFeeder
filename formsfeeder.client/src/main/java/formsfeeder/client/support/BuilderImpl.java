package formsfeeder.client.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * This class provides an implementation of the Builder interface that assists in building a JAX-RS client.  It is shared between the following projects:
 * fluentforms/rest-services.client
 * formsfeeder.client
 * 
 * If changes are made to this file, those changes should be propagated into each of the other projects. 
 * These files are copied because there is not enough code to justify creating another project and adding another dependency. 
 *
 */
public class BuilderImpl implements Builder {
	private final static Supplier<Client> defaultClientFactory = ()->ClientBuilder.newClient();
	
	private String machineName = "localhost";
	private int port = 4502;
	private HttpAuthenticationFeature authFeature = null;
	private Map<String, Supplier<String>> headerMap = new HashMap<>();
	private boolean useSsl = false;
	private String contextRoot = "/api/v1/";
	private Supplier<Client> clientFactory = defaultClientFactory;
	private Map<String, List<Supplier<String>>> queryParams = new HashMap<>();
	private Supplier<String> correlationIdFn = null;

	public BuilderImpl() {
		super();
	}

	@Override
	public BuilderImpl machineName(String machineName) {
		this.machineName = machineName;
		return this;
	}

	@Override
	public BuilderImpl port(int port) {
		this.port = port;
		return this;
	}

	@Override
	public BuilderImpl useSsl(boolean useSsl) {
		this.useSsl = useSsl;
		return this;
	}

	@Override
	public BuilderImpl contextRoot(String contextRoot) {
		this.contextRoot = contextRoot;
		return this;
	}

	@Override
	public BuilderImpl clientFactory(Supplier<Client> clientFactory) {
		this.clientFactory = clientFactory;
		return this;
	}

	@Override
	public BuilderImpl basicAuthentication(String username, String password) {
		this.authFeature = HttpAuthenticationFeature.basic(username, password);
		return this;
	}

	@Override
	public BuilderImpl addQueryParam(String name, List<Supplier<String>> value) {
		this.queryParams.put(name,value);
		return this;
	}

	@Override
	public BuilderImpl addQueryParam(String name, Supplier<String> value) {
		if(this.queryParams.containsKey(name)) {
			this.queryParams.get(name).add(value);
		} else {
			List<Supplier<String>> list = new ArrayList<>();
			list.add(value);
			this.queryParams.put(name, list);
		}
		return this;
	}

	@Override
	public Map<String, List<Supplier<String>>> getQueryParams() { return this.queryParams; }

	@Override
	public BuilderImpl correlationId(Supplier<String> correlationIdFn) {
		this.correlationIdFn = correlationIdFn;
		return this;
	}

	@Override
	public Supplier<String> getCorrelationIdFn() {
		return this.correlationIdFn;
	}

	@Override
	public BuilderImpl addHeader(String header, Supplier<String> value) {
		this.headerMap.put(header, value);
		return this;
	}

	@Override
	public Map<String, Supplier<String>> getHeaderMap() {
		return this.headerMap;
	}

	@Override
	public WebTarget createLocalTarget() {
		Client client = clientFactory.get();
		client.register(MultiPartFeature.class);
		if (this.authFeature != null) {
			client.register(authFeature);
		}
		WebTarget localTarget = client.target("http" + (useSsl ? "s" : "") + "://" + machineName + ":" + Integer.toString(port) + contextRoot);
		return localTarget;
	}

}
