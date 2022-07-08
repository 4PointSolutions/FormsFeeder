package com._4point.aem.formsfeeder.server;

import static org.junit.jupiter.api.Assertions.*;
import static com._4point.aem.formsfeeder.server.support.MultiLineMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

/**
 * These tests test AemProxyEndpoint class (similar test to AemProxyEndpointTest) however they set a property
 * that turns on absolute path processing.
 * 
 * The property setting only seems to work on the main class and not with a nested class, so I have been forced
 * to move it into a separate test class.
 *
 */
@TestPropertySource(properties = {"formsfeeder.af-base-location="+AemProxyEndpointTest.ABSOLUTE_LOCATION_URL})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
class AemProxyEndpointAbsPathTest implements EnvironmentAware {

	static final String ABSOLUTE_LOCATION_URL = "http://example.com:9999";
	private static final String ENV_FORMSFEEDER_AEM_PORT = "formsfeeder.aem.port";
	private static final String ENV_FORMSFEEDER_AEM_HOST = "formsfeeder.aem.host";

	private static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	private static final Path ACTUAL_RESULTS_DIR = RESOURCES_FOLDER.resolve("actualResults");

	private static final MediaType APPLICATION_JAVASCRIPT = MediaType.valueOf("application/javascript");
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
	static {
		if (SAVE_RESULTS) {
			try {
				Files.createDirectories(ACTUAL_RESULTS_DIR);
			} catch (IOException e) {
				// eat it, we don't care.
			}
		}
	}

	@LocalServerPort
	private int port;

	private URI uri;
	private WireMockServer wireMockServer;
	private static Integer wiremockPort = null;
	private Environment environment;

	@DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
		if (USE_WIREMOCK) {
			registry.add(TestConstants.ENV_FORMSFEEDER_AEM_HOST, ()->"localhost");
			registry.add(TestConstants.ENV_FORMSFEEDER_AEM_PORT, ()->wiremockPort);
		}		
	}

	@BeforeEach
	public void setUp() throws Exception {
		uri = getBaseUri(port);
		
		if (USE_WIREMOCK) {
			// Let wiremock choose the port for the first test, but re-use the same port for all subsequent tests.
			wireMockServer = new WireMockServer(wiremockPort == null ? new WireMockConfiguration().dynamicPort() : new WireMockConfiguration().port(wiremockPort));
	        wireMockServer.start();
			System.out.println("Inside SetEnvironment wiremock block.");
			if (WIREMOCK_RECORDING) {
				String aemBaseUrl = "http://" + environment.getRequiredProperty(ENV_FORMSFEEDER_AEM_HOST) + ":"
						+ environment.getRequiredProperty(ENV_FORMSFEEDER_AEM_PORT);
				System.out.println("Wiremock recording of '" + aemBaseUrl + "'.");
				wireMockServer.startRecording(aemBaseUrl);
			}
			if (wiremockPort == null) {	// Save the port for subsequent invocations. 
				wiremockPort = wireMockServer.port();
			}
			System.out.println("Wiremock is up on port " + wiremockPort + " .");
		}
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (USE_WIREMOCK) {
	        if (WIREMOCK_RECORDING) {
	        	SnapshotRecordResult recordings = wireMockServer.stopRecording();
	        	List<StubMapping> mappings = recordings.getStubMappings();
	        	System.out.println("Found " + mappings.size() + " recordings.");
	        	for (StubMapping mapping : mappings) {
	        		ResponseDefinition response = mapping.getResponse();
	        		JsonNode jsonBody = response.getJsonBody();
	        		System.out.println(jsonBody == null ? "JsonBody is null" : jsonBody.toPrettyString());
	        	}
	        }
	        wireMockServer.stop();
			System.out.println("Wiremock is down.");
		}
	}

	@Test
	void testProxyGet_Utils() throws Exception {
		String utils_js_path = "/aem/etc.clientlibs/clientlibs/granite/utils.js";
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(utils_js_path)
				 .request()
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + utils_js_path + ")." + getResponseBody(response));
		assertTrue(APPLICATION_JAVASCRIPT.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/javascript'.");
		assertTrue(response.hasEntity(), "Expected response to have entity");
		byte[] resultBytes = ((InputStream)response.getEntity()).readAllBytes();
		if (SAVE_RESULTS /* && USE_AEM */) {
			try (var os = Files.newOutputStream(ACTUAL_RESULTS_DIR.resolve("AbsPath_testProxyGet_Utils_JS_result.html"))) {
				os.write(resultBytes);;
			}
		}
		
		List<String> string = new String(resultBytes, StandardCharsets.UTF_8).lines().collect(Collectors.toList());
		assertThat(string, anyLineMatches(containsString(ABSOLUTE_LOCATION_URL)));
	}

	private static URI getBaseUri(int port) throws URISyntaxException {
		return new URI("http://localhost:" + port);
	}
	
	private static String getResponseBody(Response response) {
		final MediaType ANY_TEXT_MEDIATYPE = new MediaType("text", "*");

		if (!response.hasEntity()) {
			return "";
		} else if (!ANY_TEXT_MEDIATYPE.isCompatible(response.getMediaType()))  {
			return "Returned mediatype='" + response.getMediaType().toString() + "'.";
		} else {
			try {
				return new String(((InputStream)response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				return "Returned mediatype='" + response.getMediaType().toString() + "', but unable to display entity.";
			}
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}
