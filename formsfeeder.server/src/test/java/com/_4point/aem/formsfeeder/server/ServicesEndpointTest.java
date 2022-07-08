package com._4point.aem.formsfeeder.server;

import static com._4point.aem.formsfeeder.server.TestConstants.getResponseBody;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
class ServicesEndpointTest implements EnvironmentAware {

	private static final String API_V1_PATH = "/api/v1";
	private static final String DEBUG_PLUGIN_PATH = API_V1_PATH + "/Debug";
	private static final String MOCK_PLUGIN_PATH = API_V1_PATH + "/Mock";
	private static final String PDF_PLUGIN_PATH = API_V1_PATH + "/RenderPdf";
	private static final String HTML5_PLUGIN_PATH = API_V1_PATH + "/RenderHtml5";

	private static final String BODY_DS_NAME = "formsfeeder:BodyBytes";
	private static final String MOCK_PLUGIN_SCENARIO_NAME = "scenario";
	
	private static final Path SAMPLE_XDP = TestConstants.SAMPLE_FILES_DIR.resolve("SampleForm.xdp");
	private static final Path SAMPLE_DATA = TestConstants.SAMPLE_FILES_DIR.resolve("SampleForm_data.xml");
	private static final Path SAMPLE_PDF = TestConstants.SAMPLE_FILES_DIR.resolve("SampleForm.pdf");

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
				Files.createDirectories(TestConstants.ACTUAL_RESULTS_DIR);
			} catch (IOException e) {
				// eat it, we don't care.
			}
		}
	}

	private static final Decoder DECODER = Base64.getDecoder();
	
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
		uri = TestConstants.getBaseUri(port);
		
		if (USE_WIREMOCK) {
			// Let wiremock choose the port for the first test, but re-use the same port for all subsequent tests.
			wireMockServer = new WireMockServer(wiremockPort == null ? new WireMockConfiguration().dynamicPort() : new WireMockConfiguration().port(wiremockPort));
	        wireMockServer.start();
			System.out.println("Inside SetEnvironment wiremock block.");
			if (WIREMOCK_RECORDING) {
				String aemBaseUrl = "http://" + environment.getRequiredProperty(TestConstants.ENV_FORMSFEEDER_AEM_HOST) + ":"
						+ environment.getRequiredProperty(TestConstants.ENV_FORMSFEEDER_AEM_PORT);
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
	void testInvokeGetNoParams() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .get();
		
		assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	@Test
	void testInvokeGetOneParams() {
		String expectedParamName = "QueryParam1";
		String expectedParamValue = "QueryParam1 Value";
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName, expectedParamValue)
				 .request()
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + responseBody + "'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	@Test
	void testInvokeGetManyParamsNoAccept() {
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String string = entry.toString();
			returnsCount++;
			assertTrue(string.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + string + "'.");
			assertTrue(string.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + string + "'.");
		}
		assertEquals(3, returnsCount);
	}

	@Test
	void testInvokeGetManyParamsNoAcceptWithHeader() {
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		String headerParamString = "FormsFeeder_HeaderParam";
		String headerValueString = "Value";
		String expectedHeaderParamName1 = headerParamString + "1";
		String expectedHeaderParamValue1 = expectedHeaderParamName1 + " " + headerValueString;
		String expectedHeaderParamName2 = headerParamString + "2";
		String expectedHeaderParamValue2 = expectedHeaderParamName2 + " " + headerValueString;
		String expectedHeaderParamName3 = headerParamString + "3";
		String expectedHeaderParamValue3 = expectedHeaderParamName3 + " " + headerValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .header(expectedHeaderParamName1, expectedHeaderParamValue1)
				 .header(expectedHeaderParamName2, expectedHeaderParamValue2)
				 .header("HELLO", "world")
				 .header(expectedHeaderParamName3, expectedHeaderParamValue3)
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 6 Strings with the messages in them.
			String string = entry.toString();
			returnsCount++;
			if (returnsCount < 4) {
				assertTrue(string.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + string + "'.");
				assertTrue(string.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + string + "'.");
			}
			else {
				assertTrue(string.contains(headerParamString.substring(12).toLowerCase()), "Expected response body to contain '" + headerParamString.substring(12).toLowerCase() + "', but was '" + string + "'.");
				assertTrue(string.contains(headerValueString), "Expected response body to contain '" + headerValueString + "', but was '" + string + "'.");
			}
		}
		assertEquals(6, returnsCount);
	}

	@Test
	void testInvokeGetManyParamsAcceptMultipartFormdata() {
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .accept(MediaType.MULTIPART_FORM_DATA_TYPE)
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'multipart/form-data'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(body.getMediaType()), "Expected response media type (" + body.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(3, returnsCount);
	}

	@Test
	void testInvokeGetManyParamsAcceptJson() {
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String string = entry.toString();
			returnsCount++;
			assertTrue(string.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + string + "'.");
			assertTrue(string.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + string + "'.");
		}
		assertEquals(3, returnsCount);
	}

	// The "No Body" tests are currently disabled because they are producing an "status code 500 Internal Error" result.
	// Ideally, I'd like to find a way around this.  I'd prefer to allow for this however it looks like Jersey tries to
	// parse the input as multipart/form-data and fails.
	@Disabled
	void testInvokePostNoQueryParamsNoBody() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(null);
		
		assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}


	@Disabled
	void testInvokePostOneQueryParamNoBody() {
		String expectedParamName = "QueryParam1";
		String expectedParamValue = "QueryParam1 Value";
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName, expectedParamValue)
				 .request()
				 .post(null);
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + responseBody + "'.");
	}

	@Disabled
	void testInvokePostManyQueryParamsNoBody() {
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .post(null);
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(body.getMediaType()), "Expected response media type (" + body.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(3, returnsCount);
	}

	
	@Test
	void testInvokePostNoQueryParamsEmptyBodyParamOctetStream() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(InputStream.nullInputStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(BODY_DS_NAME), "Expected response body to contain '" + BODY_DS_NAME + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(MediaType.APPLICATION_OCTET_STREAM), "Expected response body to contain '" + MediaType.APPLICATION_OCTET_STREAM + "', but was '" + responseBody + "'.");
	}

	
	@Test
	void testInvokePostNoQueryParamsOneBodyParamXdpParam() throws Exception {
		final Path testFile = SAMPLE_XDP;
		final ContentDisposition cd = ContentDisposition.type("inline").fileName(testFile.getFileName().toString()).build();
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
				 .post(Entity.entity(testFile.toFile(), TestConstants.APPLICATION_XDP));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(testFile.getFileName().toString()), "Expected response body to contain '" + testFile.getFileName().toString() + "', but was '" + responseBody + "'.");
	}

	@Test
	void testInvokePostNoQueryParamsOneBodyParamText() {
		String expectedBodyText = "This is some text.";
		
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(new StringReader(expectedBodyText), MediaType.TEXT_PLAIN_TYPE));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedBodyText), "Expected response body to contain '" + expectedBodyText + "', but was '" + responseBody + "'.");
	}

	@Test
	void testInvokePostOneQueryParamOneBodyParamAcceptMultpartFormData() {
		String expectedParamName = "QueryParam1";
		String expectedParamValue = "QueryParam1 Value";
		String expectedBodyText = "Body Text Value";
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName, expectedParamValue)
				 .request()
				 .accept(MediaType.MULTIPART_FORM_DATA_TYPE)
				 .post(Entity.text(expectedBodyText));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				MediaType mediaType = body.getMediaType();
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains(BODY_DS_NAME)) {
					// Contains the body text because the body of the post was text.
					assertTrue(value.contains(expectedBodyText), "Expected response body to contain '" + expectedBodyText + "', but was '" + value + "'.");
				} else {
					assertTrue(value.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + value + "'.");
					assertTrue(value.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + value + "'.");
				}
			}
		}
		assertEquals(2, returnsCount);
	}

	@Test
	void testInvokePostOneQueryParamOneBodyParamAcceptJson() {
		String expectedParamName = "QueryParam1";
		String expectedParamValue = "QueryParam1 Value";
		String expectedBodyText = "Body Text Value";
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName, expectedParamValue)
				 .request()
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .post(Entity.text(expectedBodyText));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String value = entry.toString();
			returnsCount++;
			if (value.contains(BODY_DS_NAME)) {
				// Contains the body text because the body of the post was text.
				assertTrue(value.contains(expectedBodyText), "Expected response body to contain '" + expectedBodyText + "', but was '" + value + "'.");
			} else {
				assertTrue(value.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + value + "'.");
				assertTrue(value.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + value + "'.");
			}
		}
		assertEquals(2, returnsCount);
	}

	@Test
	void testInvokePostOneQueryParamOneBodyParamNoAccept() {
		String expectedParamName = "QueryParam1";
		String expectedParamValue = "QueryParam1 Value";
		String expectedBodyText = "Body Text Value";
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName, expectedParamValue)
				 .request()
				 .post(Entity.text(expectedBodyText));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String value = entry.toString();
			returnsCount++;
			if (value.contains(BODY_DS_NAME)) {
				// Contains the body text because the body of the post was text.
				assertTrue(value.contains(expectedBodyText), "Expected response body to contain '" + expectedBodyText + "', but was '" + value + "'.");
			} else {
				assertTrue(value.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + value + "'.");
				assertTrue(value.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + value + "'.");
			}
		}
		assertEquals(2, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsOneBodyParamAcceptMultpartFormData() {
		String expectedBodyText = "<root>Body Text Value</root>";
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .accept(MediaType.MULTIPART_FORM_DATA_TYPE)
				 .post(Entity.xml(expectedBodyText));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				MediaType mediaType = body.getMediaType();
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains(BODY_DS_NAME)) {
					// Contains the content type because the body of the post was not text.
					assertTrue(value.contains(MediaType.APPLICATION_XML), "Expected response body to contain '" + MediaType.APPLICATION_XML + "', but was '" + value + "'.");
				} else {
					assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
					assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
				}
			}
		}
		assertEquals(4, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsOneBodyParamAcceptJson() {
		String expectedBodyText = "<root>Body Text Value</root>";
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .accept(MediaType.APPLICATION_JSON)
				 .post(Entity.xml(expectedBodyText));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String value = entry.toString();
			returnsCount++;
			if (value.contains(BODY_DS_NAME)) {
				// Contains the content type because the body of the post was not text.
				assertTrue(value.contains(MediaType.APPLICATION_XML), "Expected response body to contain '" + MediaType.APPLICATION_XML + "', but was '" + value + "'.");
			} else {
				assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(4, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsOneBodyParamNoAccept() {
		String expectedBodyText = "<root>Body Text Value</root>";
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .accept(MediaType.APPLICATION_JSON)
				 .post(Entity.xml(expectedBodyText));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String value = entry.toString();
			returnsCount++;
			if (value.contains(BODY_DS_NAME)) {
				// Contains the content type because the body of the post was not text.
				assertTrue(value.contains(MediaType.APPLICATION_XML), "Expected response body to contain '" + MediaType.APPLICATION_XML + "', but was '" + value + "'.");
			} else {
				assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(4, returnsCount);
	}

	@Test
	void testInvokePostNoQueryParamsOneFormParam() {
		String expectedParamName = "BodyParam1";
		String expectedParamValue = "BodyParam1 Value";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedParamName, expectedParamValue);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + responseBody + "'.");
	}

	@Test
	void testInvokePostNoQueryParamsComplexParams() {
		String expectedParamName1 = "BodyParam1";
		String expectedParamName2 = "BodyParam2";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.bodyPart(new FileDataBodyPart(expectedParamName1, SAMPLE_XDP.toFile(), TestConstants.APPLICATION_XDP));	// One with a filename
		bodyData.field(expectedParamName2, InputStream.nullInputStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE);	// One without a filename
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(body.getMediaType()), "Expected response media type (" + body.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains(expectedParamName1)) {
					assertTrue(value.contains(TestConstants.APPLICATION_XDP.toString()), "Expected response body to contain '" + TestConstants.APPLICATION_XDP.toString() + "', but was '" + value + "'.");
					assertTrue(value.contains(SAMPLE_XDP.getFileName().toString()), "Expected response body to contain '" + SAMPLE_XDP.getFileName().toString() + "', but was '" + value + "'.");
				} else if (value.contains(expectedParamName2)) {
					assertTrue(value.contains(MediaType.APPLICATION_OCTET_STREAM), "Expected response body to contain '" + MediaType.APPLICATION_OCTET_STREAM + "', but was '" + value + "'.");
				} else {
					fail("Unexpected response '" + value + "'.");
				}
			}
		}
		assertEquals(2, returnsCount);
	}

	@Test
	void testInvokePostNoQueryParamsOneBodyParamText_BadContentDisposition() {
		String expectedBodyText = "This is some text.";
		String badContentDisposition = "inline; foo bar whatever";

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .header(HttpHeaders.CONTENT_DISPOSITION, badContentDisposition)	// Bad content Disposition
				 .post(Entity.entity(new StringReader(expectedBodyText), MediaType.TEXT_PLAIN_TYPE));
		
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Error while parsing")),
				()->assertTrue(responseBody.contains("Content-Disposition header")),
				()->assertTrue(responseBody.contains(badContentDisposition))
				);
	}

	@Test
	void testInvokePostNoQueryParamsManyFormParams() {
		String bodyParamString = "BodyParam";
		String bodyValueString = "Value";
		String expectedParamName1 = bodyParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + bodyValueString;
		String expectedParamName2 = bodyParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + bodyValueString;
		String expectedParamName3 = bodyParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + bodyValueString;

		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedParamName1, expectedParamValue1);
		bodyData.field(expectedParamName2, expectedParamValue2);
		bodyData.field(expectedParamName3, expectedParamValue3);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(body.getMediaType()), "Expected response media type (" + body.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				assertTrue(value.contains(bodyParamString), "Expected response body to contain '" + bodyParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(bodyValueString), "Expected response body to contain '" + bodyValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(3, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsOneFormParam() {
		String expectedBodyParamName = "BodyParam1";
		String expectedBodyParamValue = "BodyParam1 Value";
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedQueryParamName1 = queryParamString + "1";
		String expectedQueryParamValue1 = expectedQueryParamName1 + " " + queryValueString;
		String expectedQueryParamName2 = queryParamString + "2";
		String expectedQueryParamValue2 = expectedQueryParamName2 + " " + queryValueString;
		String expectedQueryParamName3 = queryParamString + "3";
		String expectedQueryParamValue3 = expectedQueryParamName3 + " " + queryValueString;
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedBodyParamName, expectedBodyParamValue);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedQueryParamName1, expectedQueryParamValue1)
				 .queryParam(expectedQueryParamName2, expectedQueryParamValue2)
				 .queryParam(expectedQueryParamName3, expectedQueryParamValue3)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				
				MediaType mediaType = body.getMediaType();
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains("Body")) {
					assertTrue(value.contains(expectedBodyParamName), "Expected response body to contain '" + expectedBodyParamName + "', but was '" + value + "'.");
					assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
				} else {
					assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
					assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
				}
			}
		}
		assertEquals(4, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsManyFormParams() {
		String bodyParamString = "BodyParam";
		String bodyValueString = "Value";
		String expectedBodyParamName1 = bodyParamString + "1";
		String expectedBodyParamValue1 = expectedBodyParamName1 + " " + bodyValueString;
		String expectedBodyParamName2 = bodyParamString + "2";
		String expectedBodyParamValue2 = expectedBodyParamName2 + " " + bodyValueString;
		String expectedBodyParamName3 = bodyParamString + "3";
		String expectedBodyParamValue3 = expectedBodyParamName3 + " " + bodyValueString;
		String expectedBodyParamName4 = bodyParamString + "4";
		InputStream expectedBodyParamValue4 = (InputStream)(new ByteArrayInputStream("<root>Body Text Value</root>".getBytes(StandardCharsets.UTF_8)));

		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;

		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedBodyParamName1, expectedBodyParamValue1);
		bodyData.field(expectedBodyParamName2, expectedBodyParamValue2);
		bodyData.field(expectedBodyParamName3, expectedBodyParamValue3);
		bodyData.field(expectedBodyParamName4, expectedBodyParamValue4, MediaType.APPLICATION_XML_TYPE);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				
				MediaType mediaType = body.getMediaType();
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains("Body")) {
					if (value.contains("BodyParam4")) {
						// Look for the XML parameter
						
					} else {
						assertTrue(value.contains(bodyParamString), "Expected response body to contain '" + bodyParamString + "', but was '" + value + "'.");
						assertTrue(value.contains(bodyValueString), "Expected response body to contain '" + bodyValueString + "', but was '" + value + "'.");
					}
				} else {
					assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
					assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
				}
			}
		}
		assertEquals(7, returnsCount);
	}

	@Test
	void testInvokePostNoQueryParamsOneJsonParam() {
		String expectedParamName = "BodyParam1";
		String expectedParamValue = "BodyParam1 Value";
		
		JsonObject jsonData = Json.createObjectBuilder()
								  .add(expectedParamName, expectedParamValue)
								  .build();
		
		Response response = ClientBuilder.newClient()
				 .register(JsonProcessingFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.json(jsonData));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + responseBody + "'.");
	}

	@Test
	void testInvokePostNoQueryParamsComplexJsonParams() {
		String bodyParamString = "BodyParam";
		String expectedParamName1 = bodyParamString + "1";
		String expectedParamName2 = bodyParamString + "2";
		String expectedParamName3 = bodyParamString + "3";
		String expectedParamName4 = bodyParamString + "4";
		String expectedParamName5 = bodyParamString + "5";
		String expectedParamName6 = bodyParamString + "6";
		
		
		double expectedParamValue1 = Double.MIN_VALUE;
		int expectedParamValue2 = Integer.MIN_VALUE;
		long expectedParamValue3 = Long.MIN_VALUE;
		JsonObject jsonData = Json.createObjectBuilder()
				  .add(expectedParamName1, expectedParamValue1)
				  .add(expectedParamName2, expectedParamValue2)
				  .add(expectedParamName3, expectedParamValue3)
				  .addNull(expectedParamName4)
				  .add(expectedParamName5, Json.createArrayBuilder().add(1).build())
				  .add(expectedParamName6, Json.createObjectBuilder().add("foo", "bar").build())
				  .build();
		
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.json(jsonData));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		System.out.println(getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String value = entry.toString();
			returnsCount++;
			if (value.contains(expectedParamName1)) {
				CharSequence expectedBodyParamValue = Double.toString(Double.MIN_VALUE);
				assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
			} else if (value.contains(expectedParamName2)) {
				CharSequence expectedBodyParamValue = Integer.toString(Integer.MIN_VALUE);
				assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
			} else if (value.contains(expectedParamName3)) {
				CharSequence expectedBodyParamValue = Long.toString(Long.MIN_VALUE);
				assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
			} else if (value.contains(expectedParamName4)) {
				CharSequence expectedBodyParamValue = StandardMimeTypes.APPLICATION_OCTET_STREAM_STR;
				assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
			} else if (value.contains(expectedParamName5)) {
				CharSequence expectedBodyParamValue = StandardMimeTypes.TEXT_PLAIN_STR;
				assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
			} else if (value.contains(expectedParamName6)) {
				CharSequence expectedBodyParamValue = StandardMimeTypes.APPLICATION_VND_4POINT_DATASOURCELIST_STR;
				assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
			} else {
				fail("Unexpected response '" + value + "'.");
			}
		}
		assertEquals(6, returnsCount);
	}

	@Test
	void testInvokePostNoQueryParamsManyJsonParams() {
		String bodyParamString = "BodyParam";
		String bodyValueString = "Value";
		String expectedParamName1 = bodyParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + bodyValueString;
		String expectedParamName2 = bodyParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + bodyValueString;
		String expectedParamName3 = bodyParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + bodyValueString;

		JsonObject jsonData = Json.createObjectBuilder()
				  .add(expectedParamName1, expectedParamValue1)
				  .add(expectedParamName2, expectedParamValue2)
				  .add(expectedParamName3, expectedParamValue3)
				  .build();
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.json(jsonData));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String string = entry.toString();
			returnsCount++;
			assertTrue(string.contains(bodyParamString), "Expected response body to contain '" + bodyParamString + "', but was '" + string + "'.");
			assertTrue(string.contains(bodyValueString), "Expected response body to contain '" + bodyValueString + "', but was '" + string + "'.");
		}
		assertEquals(3, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsOneJsonParam() {
		String expectedBodyParamName = "BodyParam1";
		String expectedBodyParamValue = "BodyParam1 Value";
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedQueryParamName1 = queryParamString + "1";
		String expectedQueryParamValue1 = expectedQueryParamName1 + " " + queryValueString;
		String expectedQueryParamName2 = queryParamString + "2";
		String expectedQueryParamValue2 = expectedQueryParamName2 + " " + queryValueString;
		String expectedQueryParamName3 = queryParamString + "3";
		String expectedQueryParamValue3 = expectedQueryParamName3 + " " + queryValueString;
		
		JsonObject jsonData = Json.createObjectBuilder()
				  .add(expectedBodyParamName, expectedBodyParamValue)
				  .build();
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedQueryParamName1, expectedQueryParamValue1)
				 .queryParam(expectedQueryParamName2, expectedQueryParamValue2)
				 .queryParam(expectedQueryParamName3, expectedQueryParamValue3)
				 .request()
				 .post(Entity.json(jsonData));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String value = entry.toString();
			returnsCount++;
			if (value.contains("Body")) {
				assertTrue(value.contains(expectedBodyParamName), "Expected response body to contain '" + expectedBodyParamName + "', but was '" + value + "'.");
				assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
			} else {
				assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(4, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsManyJsonParams() {
		String bodyParamString = "BodyParam";
		String bodyValueString = "Value";
		String expectedBodyParamName1 = bodyParamString + "1";
		String expectedBodyParamValue1 = expectedBodyParamName1 + " " + bodyValueString;
		String expectedBodyParamName2 = bodyParamString + "2";
		String expectedBodyParamValue2 = expectedBodyParamName2 + " " + bodyValueString;
		String expectedBodyParamName3 = bodyParamString + "3";
		String expectedBodyParamValue3 = expectedBodyParamName3 + " " + bodyValueString;
		String expectedBodyParamName4 = bodyParamString + "4";
		Boolean expectedBodyParamValue4 = Boolean.TRUE;

		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;

		JsonObject jsonData = Json.createObjectBuilder()
				  .add(expectedBodyParamName1, expectedBodyParamValue1)
				  .add(expectedBodyParamName2, expectedBodyParamValue2)
				  .add(expectedBodyParamName3, expectedBodyParamValue3)
				  .add(expectedBodyParamName4, expectedBodyParamValue4)
				  .build();
		

		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .post(Entity.json(jsonData));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonArray jsonResultArray = jsonResultObj.getJsonArray("Message");
		int returnsCount = 0;
		for (var entry : jsonResultArray) {
			// Expecting 3 Strings with the messages in them.
			String value = entry.toString();
			returnsCount++;
			if (value.contains("Body")) {
				if (value.contains("BodyParam4")) {
					// Look for the XML parameter
					assertTrue(value.contains(bodyParamString), "Expected response body to contain '" + bodyParamString + "', but was '" + value + "'.");
					assertTrue(value.contains("true"), "Expected response body to contain '" + "true" + "', but was '" + value + "'.");
				} else {
					assertTrue(value.contains(bodyParamString), "Expected response body to contain '" + bodyParamString + "', but was '" + value + "'.");
					assertTrue(value.contains(bodyValueString), "Expected response body to contain '" + bodyValueString + "', but was '" + value + "'.");
				}
			} else {
				assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(7, returnsCount);
	}


	
	// Use the Mock plugin to test some Exception scenarios
	
	// Cannot throw exceptions from the plugin at this time.  I am looking into the issue.
	@Test
	void testInvokePost_BadRequestExceptionFromPlugin() {
		String scenarioName = "BadRequestException";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Bad Request")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	@Test
	void testInvokePost_InternalServerExceptionFromPlugin() {
		String scenarioName = "InternalErrorException";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Internal Server Error")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	@Test
	void testInvokePost_UncheckedExceptionFromPlugin() {
		String scenarioName = "UncheckedException";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Error within Plugin processor")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	@Test
	void testInvokePost_OtherFeedConsumerExceptionFromPlugin() {
		String scenarioName = "OtherFeedConsumerException";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Plugin processor error")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	@Test
	void testInvokePost_ReturnPdfFromPlugin() throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "ReturnPdf");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(TestConstants.APPLICATION_PDF.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String cdHeader = response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
		assertNotNull(cdHeader);
		ContentDisposition contentDisposition = new ContentDisposition(cdHeader);
		assertNotNull(contentDisposition.getFileName());
		assertEquals(SAMPLE_PDF.getFileName().toString(), contentDisposition.getFileName());
		assertEquals("attachment", contentDisposition.getType());
		assertTrue(response.hasEntity(), "Expected response to have entity");
		PDDocument pdf = PDDocument.load((InputStream)response.getEntity());
		PDDocumentCatalog catalog = pdf.getDocumentCatalog();
		assertNotNull(pdf);
		assertNotNull(catalog);
	}

	@Test
	void testInvokePost_ReturnXmlFromPlugin() throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "ReturnXml");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_XML_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/xml'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String cdHeader = response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
		assertNotNull(cdHeader);
		ContentDisposition contentDisposition = new ContentDisposition(cdHeader);
		assertNotNull(contentDisposition.getFileName());
		assertEquals(SAMPLE_DATA.getFileName().toString(), contentDisposition.getFileName());
		assertEquals("inline", contentDisposition.getType());
		assertTrue(response.hasEntity(), "Expected response to have entity");
		XML xml = new XMLDocument((InputStream)response.getEntity());
		assertEquals(2, Integer.valueOf(xml.xpath("count(//form1/*)").get(0)));
	}
	
	@Test
	void testInvokePost_ReturnDataSourceListPlugin() throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "ReturnDataSourceList");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'multipart/form-data'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		//TODO: Further verify the response
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		assertEquals(1, fields.size(), "Expected only one top level object");
		
		List<FormDataBodyPart> list = fields.get("DslResult");
		assertEquals(1, list.size());
		
		FormDataBodyPart body = list.get(0);
		MediaType mediaType = body.getMediaType();
		assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE, mediaType);
		
		// Jersey doesn't currently support decoding nested multipart/form-data, so we're not doing any further testing.
		// If we were the code below would try and validate the contents of the nested multipart/form-data.
		// For now, I'm not even sure we have a real use-case for this, so I am leaving it out.
		
//		FormDataMultiPart innerEntity = (FormDataMultiPart)body.getEntity();
//		Map<String, List<FormDataBodyPart>> innerFields = innerEntity.getFields();
//		assertEquals(2, innerFields.size(), "Expected only two embedded objects");
//		String expectedKey1 = "DslEntry1";
//		String expectedValue1 = "DslValue1";
//		String expectedKey2 = "DslEntry2";
//		String expectedValue2 = "DslValue2";
//		List<FormDataBodyPart> dslEntry1List = innerFields.get(expectedKey1);
//		assertEquals(1, dslEntry1List.size(), "Expected only one value for '" + expectedKey1 + "'.");
//		assertEquals(expectedValue1, dslEntry1List.get(0).getValue(), "Expected value for '" + expectedKey1 + "' to be '" + expectedValue1 + "'.");
//		List<FormDataBodyPart> dslEntry2List = innerFields.get(expectedKey2);
//		assertEquals(1, dslEntry2List.size(), "Expected only one value for '" + expectedKey2 + "'.");
//		assertEquals(expectedValue2, dslEntry2List.get(0).getValue(), "Expected value for '" + expectedKey2 + "' to be '" + expectedValue2 + "'.");
	}
				
	@Test
	void testInvokePost_ReturnDataSourceListPlugin_Json() throws Exception {
		
		JsonObject jsonData = Json.createObjectBuilder()
				  .add(MOCK_PLUGIN_SCENARIO_NAME, "ReturnDataSourceList")
				  .build();
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.json(jsonData));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));

		// Verify the response contents
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		String expectedKey1 = "DslEntry1";
		String expectedValue1 = "DslValue1";
		String expectedKey2 = "DslEntry2";
		String expectedValue2 = "DslValue2";
		
		assertEquals(1, jsonResultObj.entrySet().size());
		JsonObject jsonResultSubObj = jsonResultObj.getJsonObject("DslResult");
		assertEquals(2, jsonResultSubObj.entrySet().size());

		assertTrue(jsonResultSubObj.containsKey(expectedKey1), "Expected subObject 'DslResult' to contain key '" + expectedKey1 + "'.");
		assertEquals(expectedValue1, jsonResultSubObj.getString(expectedKey1), "Expected subObject key '" + expectedKey1 + "' to have associated value of '" + expectedValue1 + "'.");
		assertTrue(jsonResultSubObj.containsKey(expectedKey2), "Expected subObject 'DslResult' to contain key '" + expectedKey2 + "'.");
		assertEquals(expectedValue2, jsonResultSubObj.getString(expectedKey2), "Expected subObject key '" + expectedKey2 + "' to have associated value of '" + expectedValue2 + "'.");
	}
				
	@Test
	void testInvokePost_ReturnManyOutputsFromPlugin() throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "ReturnManyOutputs");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		// Decode the response and check that it is correct.
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				
				InputStream is = body.getEntityAs(InputStream.class);
				MediaType mediaType = body.getMediaType();
				if (TestConstants.APPLICATION_PDF.isCompatible(mediaType)) {
					ContentDisposition contentDisposition = body.getContentDisposition();
					assertNotNull(contentDisposition);
					assertEquals(SAMPLE_PDF.getFileName().toString(), contentDisposition.getFileName());
					validatePdf(is);
				} else if (MediaType.APPLICATION_XML_TYPE.isCompatible(mediaType)) {
					ContentDisposition contentDisposition = body.getContentDisposition();
					assertNotNull(contentDisposition);
					assertEquals(SAMPLE_DATA.getFileName().toString(), contentDisposition.getFileName());
					validateXml(is);
				} else if (MediaType.APPLICATION_OCTET_STREAM_TYPE.isCompatible(mediaType)) {
					ContentDisposition contentDisposition = body.getContentDisposition();
					assertNull(contentDisposition.getFileName());
					assertArrayEquals("SampleData".getBytes(StandardCharsets.UTF_8), is.readAllBytes());
				} else {
					fail("Found unexpected mediaType in response '" + mediaType.toString() + "'.");
				}
			}
		}
		assertEquals(3, returnsCount, "Expected 3 parts in the response.");
	}

	private void validateXml(InputStream is) throws IOException {
		XML xml = new XMLDocument(is);
		assertEquals(2, Integer.valueOf(xml.xpath("count(//form1/*)").get(0)));
	}

	private void validatePdf(InputStream is) throws IOException {
		try (PDDocument pdf = PDDocument.load(is)) {
			PDDocumentCatalog catalog = pdf.getDocumentCatalog();
			assertNotNull(pdf);
			assertNotNull(catalog);
		}
	}
	
	@Test
	void testInvokePost_ReturnManyOutputsFromPlugin_Json() throws Exception {
		JsonObject jsonData = Json.createObjectBuilder()
				  .add(MOCK_PLUGIN_SCENARIO_NAME, "ReturnManyOutputs")
				  .build();
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.json(jsonData));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		// Verify the response contents
		JsonObject jsonResultObj = response.readEntity(JsonObject.class);

		// Expecting a single entry called "Message"
		String expectedKey1 = "PdfResult";
		String expectedKey2 = "XmlResult";
		String expectedKey3 = "ByteArrayResult";
		
		assertEquals(3, jsonResultObj.entrySet().size());

		assertTrue(jsonResultObj.containsKey(expectedKey1), "Expected subObject 'DslResult' to contain key '" + expectedKey1 + "'.");
		validatePdf(asInputStream(jsonResultObj.getString(expectedKey1)));
		assertTrue(jsonResultObj.containsKey(expectedKey2), "Expected subObject 'DslResult' to contain key '" + expectedKey2 + "'.");
		validateXml(asInputStream(jsonResultObj.getString(expectedKey2)));
		assertTrue(jsonResultObj.containsKey(expectedKey3), "Expected subObject 'DslResult' to contain key '" + expectedKey3 + "'.");
		assertArrayEquals("SampleData".getBytes(StandardCharsets.UTF_8), asInputStream(jsonResultObj.getString(expectedKey3)).readAllBytes());
	}

	private InputStream asInputStream(String base64String) {
		return new ByteArrayInputStream(DECODER.decode(base64String));
	}
	
	// There are a couple of ways to get an environment variable (through Environment or through ApplicationContext to get Environment bean).
	// This test test each of the possible ways.
	@ParameterizedTest
	@ValueSource(strings= {"ReturnConfigValue", "ReturnApplicationContextConfigValue"})
	void testInvokePost_ReturnConfigValueFromPlugin(String scenarioName) throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/xml'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		assertTrue(response.hasEntity(), "Expected response to have entity");
		String returnedConfigValue = new String(((InputStream)response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
		assertEquals("FromApplicationProperties", returnedConfigValue, "Expected the Config Value to match the value in application.properties (\"FromApplicationProperties\")." );
	}
	
	@Test
	void testInvokePost_CallAnotherPlugin() throws Exception {
		
		String bodyParamString = "BodyParam";
		String bodyValueString = "Value";
		String expectedParamName1 = bodyParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + bodyValueString;
		String expectedParamName2 = bodyParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + bodyValueString;
		String expectedParamName3 = bodyParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + bodyValueString;

		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedParamName1, expectedParamValue1);
		bodyData.field(expectedParamName2, expectedParamValue2);
		bodyData.field(expectedParamName3, expectedParamValue3);
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "CallAnotherPlugin");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(body.getMediaType()), "Expected response media type (" + body.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				assertTrue(value.contains(bodyParamString), "Expected response body to contain '" + bodyParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(bodyValueString), "Expected response body to contain '" + bodyValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(3, returnsCount);
	}
	
	
		
	@Test
	void testInvokeGetNoParams_BadPath() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH + "_BadPath")
				 .request()
				 .get();
		
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	@Test
	void testInvokePostNoParams_BadPath() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH + "_BadPath")
				 .request()
				 .post(Entity.entity(InputStream.nullInputStream(), MediaType.TEXT_PLAIN_TYPE));
		
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	private enum PdfScenario {
		INTERACTIVE(true), NON_INTERACTIVE(false);
		
		private final boolean interactiveFlag;

		/**
		 * @param interactiveFlag
		 */
		private PdfScenario(boolean interactiveFlag) {
			this.interactiveFlag = interactiveFlag;
		}

		public final boolean isInteractiveFlag() {
			return interactiveFlag;
		}
	}
	
	@ParameterizedTest
	@EnumSource()
	void testInvokeExamplePdfPlugin(PdfScenario scenario) throws Exception {
		final String TEMPLATE_PARAM_NAME = "template";
		final String DATA_PARAM_NAME = "data";
		final String INTERACTIVE_PARAM_NAME = "interactive";

		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(TEMPLATE_PARAM_NAME, SAMPLE_XDP.toString());
		bodyData.field(DATA_PARAM_NAME, SAMPLE_DATA.toString());
		bodyData.field(INTERACTIVE_PARAM_NAME, Boolean.toString(scenario.isInteractiveFlag()));

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(PDF_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + PDF_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(TestConstants.APPLICATION_PDF.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/pdf'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		assertTrue(response.hasEntity(), "Expected response to have entity");
		byte[] resultBytes = ((InputStream)response.getEntity()).readAllBytes();
		if (SAVE_RESULTS /* && USE_AEM */) {
			try (var os = Files.newOutputStream(TestConstants.ACTUAL_RESULTS_DIR.resolve("testAccept_Pdf_" + scenario.toString() + ".pdf"))) {
				os.write(resultBytes);;
			}
		}
		PDDocument pdf = PDDocument.load(resultBytes);
		PDDocumentCatalog catalog = pdf.getDocumentCatalog();
		assertNotNull(pdf);
		assertNotNull(catalog);

	}

	@Test
	void testInvokeExampleHtml5Plugin() throws Exception {
		System.out.println("uri='" + uri.toString() + "' path='" + HTML5_PLUGIN_PATH + "'." );
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(HTML5_PLUGIN_PATH)
				 .queryParam("template", "crx:/content/dam/formsanddocuments/sample-forms/SampleForm.xdp")
				 .request()
				 .get();

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + HTML5_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_HTML_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/html'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		assertTrue(response.hasEntity(), "Expected response to have entity");
		byte[] resultBytes = ((InputStream)response.getEntity()).readAllBytes();
		if (SAVE_RESULTS /* && USE_AEM */) {
			try (var os = Files.newOutputStream(TestConstants.ACTUAL_RESULTS_DIR.resolve("testInvokeExampleHtml5Plugin_result.html"))) {
				os.write(resultBytes);;
			}
		}
		// Verify the result;
		HtmlFormDocument htmlDoc = HtmlFormDocument.create(resultBytes, new URI("http://" + environment.getProperty(TestConstants.ENV_FORMSFEEDER_AEM_HOST) + ":" + environment.getProperty(TestConstants.ENV_FORMSFEEDER_AEM_PORT, "4502")));
		assertEquals("LC Forms", htmlDoc.getTitle());

		// Make sure the data wasn't populated.
		String html = new String(resultBytes, StandardCharsets.UTF_8);
		// Does not contain field data.
		assertThat(html, not(anyOf(containsString("Text Field1 Data"), containsString("Text Field2 Data"))));
	}
	
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}


}
