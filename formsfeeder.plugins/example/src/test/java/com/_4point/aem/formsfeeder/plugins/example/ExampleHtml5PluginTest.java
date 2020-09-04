package com._4point.aem.formsfeeder.plugins.example;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.pf4j.spring.EnvironmentConsumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

class ExampleHtml5PluginTest {

	// These tests assume that the SampleForm.xdp included in the src/test/resources/SampleFiles has been uploaded
	// into a subdirectory called sample-forms under Forms and Documents.
	private static final String SAMPLE_FORM = "crx:/content/dam/formsanddocuments/sample-forms/SampleForm.xdp";
	private static final String TEST_MACHINE_NAME = "localhost";
	private static final String TEST_MACHINE_PORT = "4502";
	private static final String PLUGIN_NAME = "RenderHtml5";
	private static final String TEMPLATE_PARAM_NAME = "template";
	private static final String DATA_PARAM_NAME = "data";
	private static final String CONTENT_ROOT_PARAM_NAME = "contentRoot";

	private static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	private static final Path SAMPLE_FILES_DIR = RESOURCES_FOLDER.resolve("SampleFiles");
	private static final Path ACTUAL_RESULTS_DIR = RESOURCES_FOLDER.resolve("ActualResults");
	private static final Path SAMPLE_XDP = SAMPLE_FILES_DIR.resolve("SampleForm.xdp");
	private static final Path SAMPLE_DATA = SAMPLE_FILES_DIR.resolve("SampleForm_data.xml");
	
	/*
	 * Wiremock is used for unit testing.  It is not used for integration testing with a real AEM instance.
	 * Set USE_WIREMOCK to false to perform integration testing with a real Forms Feeder instance running on
	 * machine and port outlined in the application.properties formsfeeder.plugins.aemHost and 
	 * formsfeeder.plugins.aemHost settings. 
	 */
	private static final boolean USE_WIREMOCK = true;
	private static final boolean USE_AEM = !USE_WIREMOCK;
	/*
	 * Set WIREMOCK_RECORDING to true in order to record the interaction with a real FormsFeeder instance running on
	 * machine and port outlined in the application.properties formsfeeder.plugins.aemHost and
	 * formsfeeder.plugins.aemHost settings.  This is useful for recreating the Wiremock Mapping files. 
	 */
	private static final boolean WIREMOCK_RECORDING = false;

	private static final boolean SAVE_RESULTS = false;	// Set to true in order to save the results to resources/ActualResults
	static {
		if (SAVE_RESULTS) {
			try {
				Files.createDirectories(ACTUAL_RESULTS_DIR);
			} catch (IOException e) {
				// eat it, we don't care.
			}
		}
	}
	private WireMockServer wireMockServer;
	private static Integer wiremockPort = null;
	private Environment environment;

	private ExampleHtml5Plugin underTest = new ExampleHtml5Plugin();

	@BeforeEach
	public void setUp() throws Exception {
		if (USE_WIREMOCK) {
			// Let wiremock choose the port for the first test, but re-use the same port for all subsequent tests.
			wireMockServer = new WireMockServer(wiremockPort == null ? new WireMockConfiguration().dynamicPort() : new WireMockConfiguration().port(wiremockPort));
	        wireMockServer.start();
			System.out.println("Inside SetEnvironment wiremock block.");
			if (WIREMOCK_RECORDING) {
				String aemBaseUrl = "http://" + TEST_MACHINE_NAME + ":" + TEST_MACHINE_PORT;
				System.out.println("Wiremock recording of '" + aemBaseUrl + "'.");
				wireMockServer.startRecording(aemBaseUrl);
			}
			if (wiremockPort == null) {	// Save the port for subsequent invocations. 
				wiremockPort = wireMockServer.port();
			}
			environment = getMockEnvironment("localhost", Integer.toString(wiremockPort));
			System.out.println("Wiremock is up on port " + wiremockPort + " .");
		} else {
			environment = getMockEnvironment(TEST_MACHINE_NAME, TEST_MACHINE_PORT);
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
	
	private enum TestScenario {
		TEMPLATE_ONLY(
				b->b.add(TEMPLATE_PARAM_NAME, SAMPLE_FORM), 	// Add template to data
				not(anyOf(containsString("Text Field1 Data"), containsString("Text Field2 Data"))),	// Make sure data is not there
				b->b.withRequestBodyPart(WireMock.aMultipart("template").build())	// make sure template was passed in.
				),
		TEMPLATE_AND_DATA(
				b->TEMPLATE_ONLY.dataFunction.apply(b).add(DATA_PARAM_NAME, SAMPLE_DATA.toString()),	// Apply Template and add data. 
				allOf(containsString("Text Field1 Data"), containsString("Text Field2 Data")),	// Make sure data is there
				b->TEMPLATE_ONLY.verificationFunction.apply(b).withRequestBodyPart(WireMock.aMultipart("data").build())	// Run template verification and then make sure data was there too.
				);
		
		private final Function<DataSourceList.Builder, DataSourceList.Builder> dataFunction;
		private final Matcher<String> condition;
		private final Function<RequestPatternBuilder, RequestPatternBuilder> verificationFunction;
		
		private TestScenario(Function<Builder, Builder> dataFunction, Matcher<String> condition,
				Function<RequestPatternBuilder, RequestPatternBuilder> verificationFunction) {
			this.dataFunction = dataFunction;
			this.condition = condition;
			this.verificationFunction = verificationFunction;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testAcceptDataSourceList(TestScenario scenario) throws Exception {
		DataSourceList testData = scenario.dataFunction.apply(DataSourceList.builder()).build();
				
		underTest.accept(this.environment);	// Pass in the environment before performing the test
		
		DataSourceList result = underTest.accept(testData);
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(1, result.list().size());
		DataSource resultDs = result.list().get(0);
		byte[] resultBytes = resultDs.inputStream().readAllBytes();
		if (SAVE_RESULTS && USE_AEM) {
			try (OutputStream os = Files.newOutputStream(ACTUAL_RESULTS_DIR.resolve("test_ExampleHtml5Plugin_" + scenario.toString() + "_result.html"))) {
				os.write(resultBytes);;
			}
		}
		assertEquals("text/html; charset=UTF-8",resultDs.contentType().asString());
		if (!USE_AEM) {
			wireMockServer.verify(scenario.verificationFunction.apply(
					WireMock.postRequestedFor(WireMock.urlPathEqualTo("/services/Html5/RenderHtml5Form"))
					));
		} else {
			// Verify the result;
			HtmlFormDocument htmlDoc = HtmlFormDocument.create(resultBytes, new URI("http://" + TEST_MACHINE_NAME + ":" + TEST_MACHINE_PORT));
			assertEquals("LC Forms", htmlDoc.getTitle());
	
			// Make sure the data wasn't populated.
			String html = new String(resultBytes, StandardCharsets.UTF_8);
			// Does not contain field data.
			assertThat(html, scenario.condition);
		}
		String html = new String(resultBytes, StandardCharsets.UTF_8);
		// Check that the Forms Feeder filters worked
		assertThat(html, containsString("/aem/etc.clientlibs/fd/xfaforms/clientlibs/profile.css"));
	}

	@Test
	void testName() {
		assertEquals(PLUGIN_NAME, underTest.name());
	}

	private static Environment getMockEnvironment(String aemHostName, String aemHostPort) {
		return new Environment() {
			
			@Override
			public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String resolvePlaceholders(String text) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getRequiredProperty(String key) throws IllegalStateException {
				assertEquals(EnvironmentConsumer.AEM_HOST_ENV_PARAM, key);
				return aemHostName;
			}
			
			@Override
			public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> T getProperty(String key, Class<T> targetType) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getProperty(String key, String defaultValue) {
				assertEquals(EnvironmentConsumer.AEM_PORT_ENV_PARAM, key);
				assertEquals("4502", defaultValue);
				return aemHostPort;
			}
			
			@Override
			public String getProperty(String key) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean containsProperty(String key) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getDefaultProfiles() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getActiveProfiles() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean acceptsProfiles(Profiles profiles) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean acceptsProfiles(String... profiles) {
				throw new UnsupportedOperationException("Not implmented.");
			}

		};
	}
}
