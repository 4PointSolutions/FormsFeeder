package com._4point.aem.formsfeeder.server;

import static com._4point.aem.formsfeeder.server.TestConstants.ACTUAL_RESULTS_DIR;
import static com._4point.aem.formsfeeder.server.TestConstants.getResponseBody;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
@TestPropertySource(properties = {"formsfeeder.auth=basic", 
								  "formsfeeder.auth.users={{\"foo\", \"bar\", \"USER\"}, {\"" + WebSecurityConfigTest.TEST_USERNAME + "\", \"" + WebSecurityConfigTest.TEST_PASSWORD_ENCODED + "\", \"ADMIN\"}}",
								  "formsfeeder.auth.overrides={{\"Mock\"}}"
								  })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
@WireMockTest
class WebSecurityConfigTest implements EnvironmentAware {
	private static final String MOCK_SCENARIO_RETURN_XML = "ReturnXml";
	protected static final String TEST_USERNAME = "admin";
	protected static final String TEST_PASSWORD = "passwordValue";
	protected static final String TEST_PASSWORD_ENCODED = "{bcrypt}$2a$10$X0R0vqKMYh5h0x/bBO0vy.5N4z68MvpS5CPgrNMfQbBA3t48JpSzy";

	/*
	 * Set WIREMOCK_RECORDING to true in order to record the interaction with a real FormsFeeder instance running on
	 * machine and port outlined in the application.properties formsfeeder.plugins.aemHost and
	 * formsfeeder.plugins.aemHost settings.  This is useful for recreating the Wiremock Mapping files. 
	 */
	private static final boolean WIREMOCK_RECORDING = false;
	private static final boolean SAVE_RESULTS = false;

	private static final String API_V1_PATH = "/api/v1";
	private static final String DEBUG_PLUGIN_PATH = API_V1_PATH + "/Debug";
	private static final String MOCK_PLUGIN_PATH = API_V1_PATH + "/Mock";
	private static final String MOCK_PLUGIN_SCENARIO_NAME = "scenario";
	
	@LocalServerPort
	private int port;		// post of the Spring Boot instance we're testing
	private URI uri;		// URI that points to Spring Boot instance
	private static Integer wiremockPort = null;	// port that wiremock is running on.

	@DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
		registry.add(TestConstants.ENV_FORMSFEEDER_AEM_HOST, ()->"localhost");
		registry.add(TestConstants.ENV_FORMSFEEDER_AEM_PORT, ()->wiremockPort);
	}

	@BeforeEach
	void setUp(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		uri = TestConstants.getBaseUri(port);
		wiremockPort = wmRuntimeInfo.getHttpPort();

		if (WIREMOCK_RECORDING) {
			String realServiceBaseUri = TestConstants.getBaseUri(4502).toString();
			wmRuntimeInfo.getWireMock().startStubRecording(realServiceBaseUri);
		}
	}

	@AfterEach
	void tearDown(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        if (WIREMOCK_RECORDING) {
        	SnapshotRecordResult recordings = wmRuntimeInfo.getWireMock().stopStubRecording();
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
	@DisplayName("Debug Plugin Endpoints calls without credentials should return 401 \"Unauthorized\".")
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
	 * Make sure that the plugin endpoints with correct authentication credendtials work.
	 */
	@DisplayName("Debug Plugin Endpoint requests that provide correct authentication should work.")
	@Test
	void testPluginEndpoint() throws Exception {
		var authFeature = HttpAuthenticationFeature.basic(TEST_USERNAME, TEST_PASSWORD);
		Response response = ClientBuilder.newClient()
				 .register(authFeature)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .get();
		
		assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	/**
	 * Make sure that the plugin endpoints endpoints need authentication.
	 */
	@DisplayName("Debug Plugin Endpoints calls with incorrect credentials should return 401 \"Unauthorized\".")
	@Test
	void testPluginEndpointWithBadCredentials() throws Exception {
		var authFeature = HttpAuthenticationFeature.basic(TEST_USERNAME, TEST_PASSWORD + "_bad");
		Response response = ClientBuilder.newClient()
				 .register(authFeature)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .get();
		
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	@Override
	public void setEnvironment(Environment environment) {
	}

	/**
	 * Make sure that the plugin endpoints endpoints need authentication.
	 */
	@Disabled("Disabled because it causes a maven build failure (unrelated to the actual test whic passes within Eclipse).")
	@DisplayName("Mock Plugin Endpoint GET should not require authentication because we've overridden it.")
	@Test
	void testUnauthenticatedPluginEndpointGET() throws Exception {
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .queryParam(MOCK_PLUGIN_SCENARIO_NAME, MOCK_SCENARIO_RETURN_XML)
				 .request()
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_XML_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/xml'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	/**
	 * Make sure that the plugin endpoints endpoints need authentication.
	 */
	@Disabled("Disabled because it causes a maven build failure (unrelated to the actual test whic passes within Eclipse).")
	@DisplayName("Mock Plugin Endpoint POST should not require authentication because we've overridden it.")
	@Test
	void testUnauthenticatedPluginEndpointPOST() throws Exception {
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, MOCK_SCENARIO_RETURN_XML);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_XML_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/xml'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}
}
