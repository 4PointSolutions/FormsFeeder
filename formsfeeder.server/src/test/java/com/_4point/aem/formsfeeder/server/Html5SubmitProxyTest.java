package com._4point.aem.formsfeeder.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static com._4point.aem.formsfeeder.server.TestConstants.getResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Base64.Decoder;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
@Tag("Integration")
class Html5SubmitProxyTest {
	private final static Logger baseLogger = LoggerFactory.getLogger(Html5SubmitProxyTest.class);
	
	private static final String SUBMIT_HTML5_FORM_SERVICE_PATH = "/content/xfaforms/profiles/default.submit.html";
	private static final String FORM_NAME = "SampleForm";
	private static final String XDP_TEMPLATE_NAME = "sample-forms/" + FORM_NAME +".xdp";

	private static final String EXPECTED_XML_DATA = "<?xml version='1.0' encoding='UTF-8'?>\r\n" + 
			"<form1><TextField1>Text Field 1 Data</TextField1><TextField2>Text Field 2 Data</TextField2><CommonAemData><AuthParameter/><EnvironmentName/></CommonAemData><EmailManifest><To>ToAddress1@Example.com</To><Cc>CcAddress@Example.com</Cc><Bcc>BccAddress@Example.com</Bcc><From>FromAddress@Example.com</From><Subject>Sample Subject Text</Subject><BodyContent>Body Content Text</BodyContent><BodyContentType>text/plain</BodyContentType><BypassEmailSend>1</BypassEmailSend><SubmittedData><Filename/></SubmittedData><DocumentOfRecord><Filename/></DocumentOfRecord><IgnoreAttachmentsWithoutFilenames>1</IgnoreAttachmentsWithoutFilenames></EmailManifest></form1>";

	/*
	 * Wiremock is used for unit testing.  It is not used for integration testing with a real AEM instance.
	 * Set USE_WIREMOCK to false to perform integration testing with a real Forms Feeder instance running on
	 * machine and port outlined in the application.properties formsfeeder.plugins.aemHost and 
	 * formsfeeder.plugins.aemHost settings. 
	 */
	private static final boolean USE_WIREMOCK = false;
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
			if (environment instanceof ConfigurableEnvironment) {
				ConfigurableEnvironment env1 = (ConfigurableEnvironment) environment;
				MutablePropertySources propertySources = env1.getPropertySources();
				Map<String, Object> myMap = new HashMap<>();
				myMap.put(TestConstants.ENV_FORMSFEEDER_AEM_HOST, "localhost");
				myMap.put(TestConstants.ENV_FORMSFEEDER_AEM_PORT, wiremockPort);
				propertySources.addFirst(new MapPropertySource("WIREMOCK_MAP", myMap));
			} else {
				System.out.println("Unable to write to environment.");
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
	
//	@BeforeEach
//	void setUp() throws Exception {
//		this.uri = TestUtils.getBaseUri(port);
//		
//		target = ClientBuilder.newClient()
//				 .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)	// Disable re-directs so that we can test for "thank you page" redirection.
//				 .register(TestUtils.getAuthenticationFeature())
//				 .register(MultiPartFeature.class)
//				 .target(this.uri);
//	}

//	@Test
//	void testProxySubmitPost_NoSubmitUrl() throws Exception {
//		final FormDataMultiPart getHtml5Form = mockDefaultFormDataJson();
//		
//		Response response = ClientBuilder.newClient()
//				 .target(uri)
//				 .path(SUBMIT_HTML5_FORM_SERVICE_PATH)
//				 .request()
//				 .accept(MediaType.APPLICATION_XML)
//				 .post(Entity.entity(getHtml5Form, getHtml5Form.getMediaType()));
//		
//		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + this.uri.resolve(SUBMIT_HTML5_FORM_SERVICE_PATH) + ")." + TestConstants.getResponseBody(response));
//		assertTrue(Return.SUBMITTED_DATA.getMediaType().isCompatible(response.getMediaType()));
//		if (SAVE_RESULTS && response.hasEntity()) {
//			((InputStream)response.getEntity())
//					.transferTo(Files.newOutputStream(ACTUAL_RESULTS_DIR.resolve("testHtml5Post_OneReturn_Data_results.xml")));
//		}
//
//	}

	@Disabled("Requires AEM instance to be running.")
	void testHtml5SubmitPost_NoReturn() throws Exception {
		final FormDataMultiPart getHtml5Form = mockDefaultFormData("whatever");

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
	
	/* package */ static FormDataMultiPart mockDefaultFormData(String submitUrl) {
		FormDataMultiPart mockFormData = mockDefaultFormDataJson();
		mockFormData.field("submitUrl", Objects.requireNonNull(submitUrl));
		return mockFormData;
	}

	/* package */ static FormDataMultiPart mockDefaultFormDataJson() {
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
