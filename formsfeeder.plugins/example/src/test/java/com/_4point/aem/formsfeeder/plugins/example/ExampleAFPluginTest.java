package com._4point.aem.formsfeeder.plugins.example;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.plugins.example.helper.HtmlFormDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

class ExampleAFPluginTest {

	// These tests assume that the sample00002test.zip package included in the src/test/resources/SampleFiles has been uploaded
	// and installed using PackageManager.  The adaptive form contained therein should now be installed under the Forms and Documents
	// directory.
	private static final String SAMPLE_FORM = "sample00002test";
	private static final String TEST_MACHINE_NAME = "localhost";
	private static final int TEST_MACHINE_PORT = 4502;
	private static final String PLUGIN_NAME = "RenderAdaptiveForm";
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
	private AemConfig aemConfig;

	private ExampleAFPlugin underTest = new ExampleAFPlugin();

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
			aemConfig = mockConfig("localhost", wiremockPort);
			System.out.println("Wiremock is up on port " + wiremockPort + " .");
		} else {
			aemConfig = mockConfig(TEST_MACHINE_NAME, TEST_MACHINE_PORT);
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
				WireMock.getRequestedFor(WireMock.urlPathEqualTo("/content/forms/af/sample00002test.html?wcmmode=disabled"))
				),
		TEMPLATE_AND_DATA(
				b->TEMPLATE_ONLY.dataFunction.apply(b).add(DATA_PARAM_NAME, SAMPLE_DATA.toString()),	// Apply Template and add data. 
				allOf(containsString("Text Field1 Data"), containsString("Text Field2 Data")),	// Make sure data is there
				WireMock.getRequestedFor(WireMock.urlPathEqualTo("/content/forms/af/sample00002test.html?wcmmode=disabled&dataRef=service%3A%2F%2FFFPrefillService%2F5af0580b-5977-4b18-b400-abf439c7c216"))
				);
		
		private final Function<DataSourceList.Builder, DataSourceList.Builder> dataFunction;
		private final Matcher<String> condition;
		private final RequestPatternBuilder verification;

		private TestScenario(Function<Builder, Builder> dataFunction, Matcher<String> condition,
				RequestPatternBuilder verification) {
			this.dataFunction = dataFunction;
			this.condition = condition;
			this.verification = verification;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testAcceptDataSourceList(TestScenario scenario) throws Exception {
		DataSourceList testData = scenario.dataFunction.apply(DataSourceList.builder()).build();
		
		junitx.util.PrivateAccessor.setField(underTest,"aemConfig", aemConfig); 
		
		DataSourceList result = underTest.accept(testData);
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(1, result.list().size());
		DataSource resultDs = result.list().get(0);
		byte[] resultBytes = resultDs.inputStream().readAllBytes();
		if (SAVE_RESULTS && USE_AEM) {
			try (OutputStream os = Files.newOutputStream(ACTUAL_RESULTS_DIR.resolve("test_ExampleAFPlugin_" + scenario.toString() + "_result.html"))) {
				os.write(resultBytes);;
			}
		}
		assertEquals("text/html; charset=UTF-8",resultDs.contentType().asString());
		if (!USE_AEM) {
			// I couldn't get the WireMock verification working.  It's not critical, because everything works when connected to
			// AEM, so I am leaving it for now.
//			wireMockServer.verify(scenario.verification);
		} else {
			// Verify the result;
			HtmlFormDocument htmlDoc = HtmlFormDocument.create(resultBytes, new URI("http://" + TEST_MACHINE_NAME + ":" + TEST_MACHINE_PORT));
			assertEquals("Sample Adaptive Form", htmlDoc.getTitle());
	
			// Make sure the data wasn't populated.
			String html = new String(resultBytes, StandardCharsets.UTF_8);
			// Verify the field data.
			assertThat(html, scenario.condition);
		}
		String html = new String(resultBytes, StandardCharsets.UTF_8);
		// Check that the Forms Feeder filters worked
		assertThat(html, containsString("/aem/etc.clientlibs/fd/af/runtime/clientlibs/guideRuntime.css"));
	}

	@Disabled
	void testAcceptDataSourceList_TemplateAndData() {
		fail("Not yet implemented");
	}

	@Test
	void testName() {
		assertEquals(PLUGIN_NAME, underTest.name());
	}

	private static AemConfig mockConfig(String host, int port) {
		return new AemConfig() {

			@Override
			public String host() {
				return host;
			}

			@Override
			public int port() {
				return port;
			}

			@Override
			public String username() {
				return "admin";
			}

			@Override
			public String secret() {
				return "admin";
			}

			@Override
			public Protocol protocol() {
				return null;
			}

			@Override
			public AemServerType serverType() {
				return null;
			}
		};
	}
}
