package com._4point.aem.formsfeeder.server;

import static com._4point.aem.formsfeeder.server.TestConstants.ACTUAL_RESULTS_DIR;
import static com._4point.aem.formsfeeder.server.TestConstants.getResponseBody;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

/**
 * Anonymous access (the default) is tested by all the other tests in the test suite.
 * 
 * This test just tests the non-default authentication modes.  At this time, that is only basic authentication.
 *
 */
//@Disabled
@TestPropertySource(properties = {"formsfeeder.auth=basic", "formsfeeder.auth.users={{\"foo\", \"bar\", \"USER\"}, {\"admin\", \"admin\", \"ADMIN\"}}"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
@WireMockTest
class WebSecurityConfigTest implements EnvironmentAware {
	/*
	 * Wiremock is used for unit testing.  It is not used for integration testing with a real AEM instance.
	 * Set USE_WIREMOCK to false to perform integration testing with a real Forms Feeder instance running on
	 * machine and port outlined in the application.properties formsfeeder.plugins.aemHost and 
	 * formsfeeder.plugins.aemHost settings. 
	 */
	private static final boolean USE_WIREMOCK = true;
	/*
	 * Set WIREMOCK_RECORDING to true in order to record the interaction with a real FormsFeeder instance running on
	 * machine and port outlined in the application.properties formsfeeder.plugins.aemHost and
	 * formsfeeder.plugins.aemHost settings.  This is useful for recreating the Wiremock Mapping files. 
	 */
	private static final boolean WIREMOCK_RECORDING = false;
	private static final boolean SAVE_RESULTS = false;

	private static final String API_V1_PATH = "/api/v1";
	private static final String DEBUG_PLUGIN_PATH = API_V1_PATH + "/Debug";

	@LocalServerPort
	private int port;		// post of the Spring Boot instance we're testing
	private URI uri;		// URI that points to Spring Boot instance
	private static Integer wiremockPort = null;	// port that wiremock is running on.

	private Environment environment;	// Spring Boot Environment object

	@DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
		if (USE_WIREMOCK) {
			registry.add(TestConstants.ENV_FORMSFEEDER_AEM_HOST, ()->"localhost");
			registry.add(TestConstants.ENV_FORMSFEEDER_AEM_PORT, ()->wiremockPort);
		}		
	}

	@BeforeEach
	void setUp(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		uri = TestConstants.getBaseUri(port);
		wiremockPort = wmRuntimeInfo.getHttpPort();

		if (WIREMOCK_RECORDING) {
			String realServiceBaseUri = TestConstants.getBaseUri(4502).toString();
			WireMock.startRecording(realServiceBaseUri);
		}
	}

	@AfterEach
	void tearDown() throws Exception {
        if (WIREMOCK_RECORDING) {
        	SnapshotRecordResult recordings = WireMock.stopRecording();
        	List<StubMapping> mappings = recordings.getStubMappings();
        	System.out.println("Found " + mappings.size() + " recordings.");
        	for (StubMapping mapping : mappings) {
        		ResponseDefinition response = mapping.getResponse();
        		var jsonBody = response.getJsonBody();
        		System.out.println(jsonBody == null ? "JsonBody is null" : jsonBody.toPrettyString());
        	}
        }
	}


	/**
	 * Make sure that the AEM Proxy endpoints do not need authentication.  Only the plugin endpoints.
	 */
	@DisplayName("AEM Proxy Endpoints should not require authentication.")
	@Test
	void testAemEndpoint() throws Exception {
		byte[] resultBytes = AemProxyEndpointTest.testCsrf(uri);
		assertNotEquals(0, resultBytes.length, "Proxy request should return non-zero number of bytes.");
		if (SAVE_RESULTS) {
			Files.write(ACTUAL_RESULTS_DIR.resolve("testAemEndpoint_result.html"), resultBytes);
		}
	}

	/**
	 * Make sure that the plugin endpoints endpoints need authentication.
	 */
	@DisplayName("Plugin Endpoints calls without credentials should return 401 \"Unauthorized\".")
	@Test
	void testPluginEndpointNoCredentials() throws Exception {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .get();
		
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	/**
	 * Make sure that the plugin endpoints endpoints need authentication.
	 */
	@DisplayName("Plugin Endpoints should require authentication.")
	@Test
	void testPluginEndpoint() throws Exception {
		String username = "admin";
		String password = "admin";
		var authFeature = HttpAuthenticationFeature.basic(username, password);
		Response response = ClientBuilder.newClient()
				 .register(authFeature)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .get();
		
		assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
}
