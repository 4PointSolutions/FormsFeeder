package com._4point.aem.formsfeeder.server;

import static com._4point.aem.formsfeeder.server.TestConstants.getResponseBody;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

/**
 * @author rob.mcdougall
 *
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
@Tag("Integration")
class Html5SubmitProxyTest {
	private final static Logger baseLogger = LoggerFactory.getLogger(Html5SubmitProxyTest.class);
	
	private static final String SUBMIT_HTML5_FORM_SERVICE_PATH = "/content/xfaforms/profiles/default.submit.html";
	private static final String FORM_NAME = "SampleForm";
	private static final String XDP_TEMPLATE_NAME = "sample-forms/" + FORM_NAME +".xdp";
	private static final String APPLICATION_PDF = "application/pdf";
	private static final MediaType APPLICATION_PDF_TYPE = MediaType.valueOf(APPLICATION_PDF);

	private static final String EXPECTED_XML_DATA = "<?xml version='1.0' encoding='UTF-8'?>\r\n" + 
			"<form1><TextField1>Text Field 1 Data</TextField1><TextField2>Text Field 2 Data</TextField2><CommonAemData><AuthParameter/><EnvironmentName/></CommonAemData><EmailManifest><To>ToAddress1@Example.com</To><Cc>CcAddress@Example.com</Cc><Bcc>BccAddress@Example.com</Bcc><From>FromAddress@Example.com</From><Subject>Sample Subject Text</Subject><BodyContent>Body Content Text</BodyContent><BodyContentType>text/plain</BodyContentType><BypassEmailSend>1</BypassEmailSend><SubmittedData><Filename/></SubmittedData><DocumentOfRecord><Filename/></DocumentOfRecord><IgnoreAttachmentsWithoutFilenames>1</IgnoreAttachmentsWithoutFilenames></EmailManifest></form1>";

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
			wireMockServer = new WireMockServer(wiremockPort == null ? new WireMockConfiguration().dynamicPort().extensions(new ResponseTemplateTransformer(true)) : new WireMockConfiguration().port(wiremockPort).extensions(new ResponseTemplateTransformer(true)));
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
	
	/**
	 * Validate that a HEAD request works for this URL.  HTML5 Forms perform a HEAD request before performing a submit POST
	 * to make sure that the server is there before attempting the POST.
	 * 
	 * @throws Exception
	 */
	@Test
	void testHtml5SubmitPost_Head() throws Exception {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .head();

//		response.getHeaders().entrySet().stream().forEach(e->System.out.println("Header '" + e.getKey() + "'='" + e.getValue().get(0) + "'"));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + this.uri.resolve(SUBMIT_HTML5_FORM_SERVICE_PATH) + ")." + TestConstants.getResponseBody(response));
	}
	
	/**
	 * Validates that a plugin can return a no returns (i.e. an empty DataSourceList) and a text/plain message will be returned 
	 * to the user.
	 * 
	 * @throws Exception
	 */
	@Test
	void testHtml5SubmitPost_NoReturn() throws Exception {
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", "NoReturns");

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + this.uri.resolve(SUBMIT_HTML5_FORM_SERVICE_PATH) + ")." + TestConstants.getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), ()->"Expected 'text/plain' but got '" + response.getMediaType() + "'." + TestConstants.getResponseBody(response));

		String responseText = TestConstants.getResponseBody(response);
		if (SAVE_RESULTS) {
			Files.writeString(TestConstants.ACTUAL_RESULTS_DIR.resolve("testHtml5SubmitPost_NoReturn_results.txt"), responseText);
		}
		assertEquals("Form submission processed.", responseText);
	}

	/**
	 * Validates that a plugin can return a single return (i.e. a DataSourceList with only one entry) and the content of that
	 * DataSource is returned to the user.
	 * 
	 * @throws Exception
	 */
	@Test
	void testHtml5SubmitPost_ReturnPdf() throws Exception {
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", "ReturnPdf");

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + this.uri.resolve(SUBMIT_HTML5_FORM_SERVICE_PATH) + ")." + TestConstants.getResponseBody(response));
		assertTrue(APPLICATION_PDF_TYPE.isCompatible(response.getMediaType()), ()->"Expected '" + APPLICATION_PDF + "' but got '" + response.getMediaType() + "'." + TestConstants.getResponseBody(response));

		byte[] resultBytes = response.readEntity(InputStream.class).readAllBytes();
		
		if (SAVE_RESULTS) 
		{
			Files.write(TestConstants.ACTUAL_RESULTS_DIR.resolve("testHtml5SubmitPost_ReturnPdf_results.pdf"), resultBytes);
		}
		PDDocument pdf = PDDocument.load(resultBytes);
		PDDocumentCatalog catalog = pdf.getDocumentCatalog();
		assertNotNull(pdf);
		assertNotNull(catalog);
	}
	
	/**
	 * Validates that the FormsFeeder server is passing in the additional HTML5 Form parameters to plugins that it calls.
	 * 
	 * @throws Exception
	 */
	@Test
	void testHtml5SubmitPost_ReturnHtml5Parameters() throws Exception {
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", "ReturnHtml5Parameters");

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + this.uri.resolve(SUBMIT_HTML5_FORM_SERVICE_PATH) + ")." + TestConstants.getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), ()->"Expected 'text/plain' but got '" + response.getMediaType() + "'." + TestConstants.getResponseBody(response));

		String responseText = TestConstants.getResponseBody(response);
		if (SAVE_RESULTS) {
			Files.writeString(TestConstants.ACTUAL_RESULTS_DIR.resolve("testHtml5SubmitPost_ReturnHtml5Parameters_results.txt"), responseText);
		}
		assertEquals("TemplateUrl='SampleForm.xdp', ContentRoot='crx:///content/dam/formsanddocuments/sample-forms', SubmitUrl='whatever'", responseText);
	}
	
	/**
	 * A submit plugin can return specifically named data source that will cause FormFeeder to return a redirect to the user.
	 * 
	 * @throws Exception
	 */
	@Test
	void testHtml5SubmitPost_ReturnRedirect() throws Exception {
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", "ReturnRedirect");

		Response response = ClientBuilder.newClient()
				 .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)	// Disable re-directs so that we can test for "thank you page" redirection.
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));

		assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + this.uri.resolve(SUBMIT_HTML5_FORM_SERVICE_PATH) + ")." + TestConstants.getResponseBody(response));
		String redirectUrlString = response.getLocation().toString();
		assertAll(
				()->assertTrue(redirectUrlString.contains("http"), "Expected redirect URL would contain 'http' but was '" + redirectUrlString + "'."),
				()->assertTrue(redirectUrlString.contains("localhost"), "Expected redirect URL would contain 'localhost' but was '" + redirectUrlString + "'."),
				()->assertTrue(redirectUrlString.contains("RedirectUrl"), "Expected redirect URL would contain 'RedirectUrl' but was '" + redirectUrlString + "'.")
				);
	}

	/**
	 * Validate that the FormsFeeder code handles a plugin that returns too many outputs (it should only return one).
	 * 
	 * @throws Exception
	 */
	@Test
	void testHtml5SubmitPost_TooManyOutputs() throws Exception {
		final String scenarioName = "ReturnTooMany";
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", scenarioName);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + SUBMIT_HTML5_FORM_SERVICE_PATH + ") for scenario '" + scenarioName + "'." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), ()->"Expected 'text/plain' but got '" + response.getMediaType() + "'." + TestConstants.getResponseBody(response));

		String responseText = TestConstants.getResponseBody(response);
		if (SAVE_RESULTS) {
			Files.writeString(TestConstants.ACTUAL_RESULTS_DIR.resolve("testHtml5SubmitPost_TooManyOutputs_results.txt"), responseText);
		}
		assertTrue(responseText.contains("returned multiple datasources"), "Expected response (" + responseText + ") to contain 'returned multiple datasources', but it didn't.");
	}
	
	/**
	 * Validate that the FormsFeeder code handles a bad plugin name (by returning a bad request status since we got the
	 * name from the incoming POST).
	 * 
	 * @throws Exception
	 */
	@Test
	void testHtml5SubmitPost_BadPluginName() throws Exception {
		String badPluginName = "BadPluginName";
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", badPluginName, null);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));

		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + this.uri.resolve(SUBMIT_HTML5_FORM_SERVICE_PATH) + ")." + TestConstants.getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), ()->"Expected 'text/plain' but got '" + response.getMediaType() + "'." + TestConstants.getResponseBody(response));

		String responseText = TestConstants.getResponseBody(response);
		if (SAVE_RESULTS) {
			Files.writeString(TestConstants.ACTUAL_RESULTS_DIR.resolve("testHtml5SubmitPost_BadPluginName_results.txt"), responseText);
		}
		assertTrue(responseText.contains(badPluginName), "Expected response (" + responseText + ") to contain '" + badPluginName + "', but it didn't (" + responseText + ").");
		assertTrue(responseText.contains("does not exist"), "Expected response (" + responseText + ") to contain 'does not exist', but it didn't.");
	}
	
	/**
	 * Validate that the FormsFeeder code handles no plugin name (by returning a bad request status) since we got the
	 * name from the incoming POST.
	 * 
	 * @throws Exception
	 */
	@Test
	void testHtml5SubmitPost_NoPluginName() throws Exception {
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "", null);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));

		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + this.uri.resolve(SUBMIT_HTML5_FORM_SERVICE_PATH) + ")." + TestConstants.getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), ()->"Expected 'text/plain' but got '" + response.getMediaType() + "'." + TestConstants.getResponseBody(response));

		String responseText = TestConstants.getResponseBody(response);
		if (SAVE_RESULTS) {
			Files.writeString(TestConstants.ACTUAL_RESULTS_DIR.resolve("testHtml5SubmitPost_BadPluginName_results.txt"), responseText);
		}
		assertTrue(responseText.contains("Plugin name could not be found."), "Expected response (" + responseText + ") to contain 'Plugin name could not be found.', but it didn't.");
	}

	// Use the Mock plugin to test some Exception scenarios
	/**
	 * Validate that the FormsFeeder passes on a BadRequestException coming from the plugin and turns it into a BadRequest status code.
	 * 
	 */
	@Test
	void testInvokePost_BadRequestExceptionFromPlugin() {
		String scenarioName = "BadRequestException";
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", scenarioName);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + SUBMIT_HTML5_FORM_SERVICE_PATH + ") for scenario '" + scenarioName + "'." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Bad Request")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	/**
	 * Validate that the FormsFeeder passes on a InternalServerException coming from the plugin and turns it into a Internal Server Error status code.
	 * 
	 */
	@Test
	void testInvokePost_InternalServerExceptionFromPlugin() {
		String scenarioName = "InternalErrorException";
		
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", scenarioName);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + SUBMIT_HTML5_FORM_SERVICE_PATH + ") for scenario '" + scenarioName + "'." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Internal Server Error")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	/**
	 * Validate that the FormsFeeder passes on any unexpected exceptions coming from the plugin and turns it into a Internal Server Error status code.
	 * 
	 */
	@Test
	void testInvokePost_UncheckedExceptionFromPlugin() {
		String scenarioName = "UncheckedException";
		
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", scenarioName);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + SUBMIT_HTML5_FORM_SERVICE_PATH + ") for scenario '" + scenarioName + "'." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Error within Plugin processor")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	/**
	 * Validate that the FormsFeeder passes on any unexpected exceptions coming from the plugin and turns it into a Internal Server Error status code.
	 * 
	 */
	@Test
	void testInvokePost_OtherFeedConsumerExceptionFromPlugin() {
		String scenarioName = "OtherFeedConsumerException";
		
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever", "MockSubmit", scenarioName);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
				 .request()
				 .accept(MediaType.TEXT_PLAIN_TYPE)
				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + SUBMIT_HTML5_FORM_SERVICE_PATH + ") for scenario '" + scenarioName + "'." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Plugin processor error")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}
	
	
	/* package */ static FormDataMultiPart mockDefaultFormData(String submitUrl, String pluginName, String scenario) {
		FormDataMultiPart mockFormData = mockDefaultFormDataJson_WithPlugin(pluginName, scenario);
		mockFormData.field("submitUrl", Objects.requireNonNull(submitUrl));
		return mockFormData;
	}

	/* package */ static FormDataMultiPart mockDefaultFormDataJson_WithPlugin(String pluginName, String scenario) {
		String scenarioJson = scenario == null ? "" : ",{\"_class\":\"field\",\"name\":\"Scenario\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"" + scenario + "\"}]}]}";
		String formDomJson = "{\"_class\":\"xfa\",\"name\":\"xfa\",\"versionNS\":\"http://ns.adobe.com/xdp/\",\"children\":[{\"_class\":\"form\",\"name\":\"form\",\"children\":[{\"_class\":\"subform\",\"name\":\"form1\",\"children\":[{\"_class\":\"subform\",\"children\":[{\"_class\":\"field\",\"name\":\"TextField1\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"Text Field 1 Data\"}]}]},{\"_class\":\"field\",\"name\":\"TextField2\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"Text Field 2 Data\"}]}]}" + scenarioJson + ",{\"_class\":\"subform\",\"name\":\"CommonAemData\",\"children\":[{\"_class\":\"field\",\"name\":\"AuthParameter\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":null}]}]},{\"_class\":\"field\",\"name\":\"EnvironmentName\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":null}]}]}]},{\"_class\":\"subform\",\"name\":\"EmailManifest\",\"children\":[{\"_class\":\"field\",\"name\":\"To\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"ToAddress1@Example.com\"}]}]},{\"_class\":\"field\",\"name\":\"Cc\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"CcAddress@Example.com\"}]}]},{\"_class\":\"field\",\"name\":\"Bcc\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"BccAddress@Example.com\"}]}]},{\"_class\":\"field\",\"name\":\"From\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"FromAddress@Example.com\"}]}]},{\"_class\":\"field\",\"name\":\"Subject\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"Sample Subject Text\"}]}]},{\"_class\":\"field\",\"name\":\"BodyContent\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"Body Content Text\"}]}]},{\"_class\":\"field\",\"name\":\"BodyContentType\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"text/plain\"}]}]},{\"_class\":\"field\",\"name\":\"DocumentOfRecordCheckbox\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"integer\",\"_value\":\"0\"}]}]},{\"_class\":\"field\",\"name\":\"SubmittedDataCheckbox\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"integer\",\"_value\":\"0\"}]}]},{\"_class\":\"field\",\"name\":\"BypassEmailSend\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"integer\",\"_value\":\"1\"}]}]},{\"_class\":\"field\",\"name\":\"AttachmentCalc\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"00\"}]}]}]},{\"_class\":\"subform\",\"name\":\"FormsFeeder\",\"children\":[{\"_class\":\"field\",\"name\":\"Plugin\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"" + pluginName + "\"}]}]}]}]}]}],\"versionNS\":\"http://www.xfa.org/schema/xfa-form/2.8/\"}],\"isComplete\":false}";
		return mockFormDataJson(formDomJson);
	}

	/* package */ static FormDataMultiPart mockFormDataJson_NoFormsFeederData() {
		String formDomJson = "{\"_class\":\"xfa\",\"name\":\"xfa\",\"versionNS\":\"http://ns.adobe.com/xdp/\",\"children\":[{\"_class\":\"form\",\"name\":\"form\",\"children\":[{\"_class\":\"subform\",\"name\":\"form1\",\"children\":[{\"_class\":\"subform\",\"children\":[{\"_class\":\"field\",\"name\":\"TextField1\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"Text Field 1 Data\"}]}]},{\"_class\":\"field\",\"name\":\"TextField2\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"Text Field 2 Data\"}]}]},{\"_class\":\"subform\",\"name\":\"CommonAemData\",\"children\":[{\"_class\":\"field\",\"name\":\"AuthParameter\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":null}]}]},{\"_class\":\"field\",\"name\":\"EnvironmentName\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":null}]}]}]},{\"_class\":\"subform\",\"name\":\"EmailManifest\",\"children\":[{\"_class\":\"field\",\"name\":\"To\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"ToAddress1@Example.com\"}]}]},{\"_class\":\"field\",\"name\":\"Cc\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"CcAddress@Example.com\"}]}]},{\"_class\":\"field\",\"name\":\"Bcc\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"BccAddress@Example.com\"}]}]},{\"_class\":\"field\",\"name\":\"From\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"FromAddress@Example.com\"}]}]},{\"_class\":\"field\",\"name\":\"Subject\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"Sample Subject Text\"}]}]},{\"_class\":\"field\",\"name\":\"BodyContent\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"Body Content Text\"}]}]},{\"_class\":\"field\",\"name\":\"BodyContentType\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"text/plain\"}]}]},{\"_class\":\"field\",\"name\":\"DocumentOfRecordCheckbox\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"integer\",\"_value\":\"0\"}]}]},{\"_class\":\"field\",\"name\":\"SubmittedDataCheckbox\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"integer\",\"_value\":\"0\"}]}]},{\"_class\":\"field\",\"name\":\"BypassEmailSend\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"integer\",\"_value\":\"1\"}]}]},{\"_class\":\"field\",\"name\":\"AttachmentCalc\",\"children\":[{\"_class\":\"value\",\"children\":[{\"_class\":\"text\",\"_value\":\"00\"}]}]}]}]}]}],\"versionNS\":\"http://www.xfa.org/schema/xfa-form/2.8/\"}],\"isComplete\":false}";
		return mockFormDataJson(formDomJson);
	}

	/* package */ static FormDataMultiPart mockFormData(String formDomJson, String submitUrl) {
		FormDataMultiPart mockFormData = mockFormDataJson(formDomJson);
		mockFormData.field("submitUrl", Objects.requireNonNull(submitUrl));
		return mockFormData;
	}

	/* package */ static FormDataMultiPart mockFormDataJson(String formDomJson) {
		final FormDataMultiPart getHtml5Form = new FormDataMultiPart();
		getHtml5Form.field("_charset_", "UTF-8")
				  .field("submitServiceProxy", "/content/xfaforms/profiles/default.submit.html")
				  .field("logServiceProxy", "/content/xfaforms/profiles/default.log.html")
				  .field("formDom", formDomJson)
				  .field("template", FORM_NAME +".xdp")
				  .field("contentRoot", "crx:///content/dam/formsanddocuments/sample-forms")
				  .field(":cq_csrf_token", "eyJleHAiOjE1NjgxMzg0OTUsImlhdCI6MTU2ODEzNzg5NX0.19peeJ8nHlcO-gzvPDM3en005pFiFFSYIHVo4XSrv9g");
		return getHtml5Form;
	}
}
