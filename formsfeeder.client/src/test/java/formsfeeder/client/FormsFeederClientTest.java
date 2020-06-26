package formsfeeder.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

import formsfeeder.client.FormsFeederClient.FormsFeederClientException;

@ExtendWith(MockitoExtension.class)
class FormsFeederClientTest {
	private static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	private static final Path SAMPLE_FILES_DIR = RESOURCES_FOLDER.resolve("SampleFiles");
	private static final Path SAMPLE_XDP = SAMPLE_FILES_DIR.resolve("SampleForm.xdp");
	private static final Path SAMPLE_PDF = SAMPLE_FILES_DIR.resolve("SampleForm.pdf");
	private static final Path SAMPLE_DATA = SAMPLE_FILES_DIR.resolve("SampleForm_data.xml");

	private static final String FF_SERVER_MACHINE_NAME = "localhost";
	private static final int FF_SERVER_MACHINE_PORT = 8080;

	private static final String FORMSFEEDER_SERVER_USERNAME = "username";
	private static final String FORMSFEEDER_SERVER_PASSWORD = "password";

	// DataSource Names
	private static final String BOOLEAN_DS_NAME = "BooleanDS";
	private static final String BYTE_ARRAY_DS_NAME = "ByteArrayDS";
	private static final String DOUBLE_DS_NAME = "DoubleDS";
	private static final String FLOAT_DS_NAME = "FloatDS";
	private static final String INTEGER_DS_NAME = "IntegerDS";
	private static final String LONG_DS_NAME = "LongDS";
	private static final String FILE_DS_NAME = "FileDS";
	private static final String STRING_DS_NAME = "StringDS";
	private static final String DUMMY_DS_NAME = "DummyDS";
	private static final String BYTE_ARRAY_W_CT_DS_NAME = "ByteArrayDSWithContentType";
	
	// Custom Data Source
	private static final DataSource dummyDS = new DataSource() {
		byte[] data = "Some Data".getBytes();
		
		@Override
		public OutputStream outputStream() {
			return null;
		}
		
		@Override
		public String name() {
			return DUMMY_DS_NAME;
		}
		
		@Override
		public InputStream inputStream() {
			return new ByteArrayInputStream(data);
		}
		
		@Override
		public Optional<Path> filename() {
			return Optional.empty();
		}
		
		@Override
		public MimeType contentType() {
			return StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE;
		}
		
		@Override
		public Map<String, String> attributes() {
			return Collections.emptyMap();
		}
	};

	// Data for standard data sources.
	private static final boolean booleanData = true;
	private static final byte[] byteArrayData = new byte[0];
	private static final double doubleData = Double.MAX_VALUE;
	private static final float floatData = Float.MAX_VALUE;
	private static final int intData = Integer.MAX_VALUE;
	private static final long longData = Long.MAX_VALUE;
	private static final Path pathData = SAMPLE_XDP;
	private static final String stringData = "String Data";
	private static final MimeType mimeType = StandardMimeTypes.APPLICATION_PDF_TYPE;

	/*
	 * Wiremock is used for unit testing.  It is not used for integration testing with a real FormsFeeder instance.
	 * Set USE_WIREMOCK to false to perform integration testing with a real Forms Feeder instance running on
	 * FF_SERVER_MACHINE_NAME and FF_SERVER_MACHINE_PORT. 
	 */
	private static final boolean USE_WIREMOCK = true;
	
	/*
	 * Set WIREMOCK_RECORDING to true in order to record the interaction with a real FormsFeeder instance running on
	 * FF_SERVER_MACHINE_NAME and FF_SERVER_MACHINE_PORT.  This is useful for recreating the Wiremock Mapping files. 
	 */
	private static final boolean WIREMOCK_RECORDING = false;

	private WireMockServer wireMockServer;
	private static Integer wiremockPort = null;
	
	private static String formsfeederServerName;
	private static int formsfeederServerPort;

	@BeforeEach
	public void setUp() throws Exception {
		if (USE_WIREMOCK) {
			// Let wiremock choose the port for the first test, but re-use the same port for all subsequent tests.
			wireMockServer = new WireMockServer(wiremockPort == null ? new WireMockConfiguration().dynamicPort().extensions(new ResponseTemplateTransformer(true)) : new WireMockConfiguration().port(wiremockPort).extensions(new ResponseTemplateTransformer(true)));
	        wireMockServer.start();
			if (WIREMOCK_RECORDING) {
				String aemBaseUrl = "http://" + FF_SERVER_MACHINE_NAME + ":"	+ FF_SERVER_MACHINE_PORT;
				System.out.println("Wiremock recording of '" + aemBaseUrl + "'.");
				wireMockServer.startRecording(aemBaseUrl);
			}
			if (wiremockPort == null) {	// Save the port for subsequent invocations. 
				wiremockPort = wireMockServer.port();
			}
			formsfeederServerName = "localhost";
			formsfeederServerPort = wiremockPort;
		} else {
			formsfeederServerName = FF_SERVER_MACHINE_NAME;
			formsfeederServerPort = FF_SERVER_MACHINE_PORT;
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
		}
	}
	
	@Test
	void testAccept_OneParamReturned() throws Exception {
		String expectedParamName = "Param1";
		String expectedParamValue = "Param1Value";

		FormsFeederClient underTest = FormsFeederClient.builder()
												  .machineName(formsfeederServerName)
												  .port(formsfeederServerPort)
												  .plugin("Debug")
												  .build();	
		DataSourceList result = underTest.accept(DataSourceList.builder().add(expectedParamName,expectedParamValue).build());
		
		assertNotNull(underTest.returnedCorrelationId());
		assertFalse(Jdk8Utils.isBlank(underTest.returnedCorrelationId()));
		assertNotNull(result);
		assertEquals(1, result.list().size());
		Optional<String> optString = result.deconstructor().getStringByName(FormsFeederClient.FORMSFEEDERCLIENT_DATA_SOURCE_NAME);
		assertTrue(optString.isPresent(), "Expected to find string with name '" + FormsFeederClient.FORMSFEEDERCLIENT_DATA_SOURCE_NAME + "'.");
		String resultString = optString.get();
		assertAll(
				()->assertTrue(resultString.contains(expectedParamName)),
				()->assertTrue(resultString.contains(expectedParamValue))
				);
	}

	@Test
	void testAccept_ManyParamsReturned() throws Exception {
		String expectedCorrelationId = "fake correlation id";
		FormsFeederClient underTest = FormsFeederClient.builder()
												  .machineName(formsfeederServerName)
												  .port(formsfeederServerPort)
												  .basicAuthentication(FORMSFEEDER_SERVER_USERNAME, FORMSFEEDER_SERVER_PASSWORD)
												  .plugin("Debug")
												  .correlationId(()->expectedCorrelationId)
												  .build();	
		
		assertNull(underTest.returnedCorrelationId());
		DataSourceList result = underTest.accept(DataSourceList.builder()
				.add(dummyDS)
				.add(BOOLEAN_DS_NAME, booleanData)
				.add(BYTE_ARRAY_DS_NAME, byteArrayData)
				.add(DOUBLE_DS_NAME, doubleData)
				.add(FLOAT_DS_NAME, floatData)
				.add(INTEGER_DS_NAME, intData)
				.add(LONG_DS_NAME, longData)
				.add(FILE_DS_NAME, pathData)
				.add(STRING_DS_NAME, stringData)
				.add(BYTE_ARRAY_W_CT_DS_NAME, byteArrayData, mimeType)
				.build());
		
		String resultCorrelationId = underTest.returnedCorrelationId();
		assertNotNull(resultCorrelationId);
		assertEquals(expectedCorrelationId, resultCorrelationId, "Expected correlation ids to match.");
		assertNotNull(result);
		List<DataSource> resultList = result.list();
		assertEquals(10, resultList.size());
		List<String> resultStrList = resultList.stream().map(FormsFeederClientTest::toString).collect(Collectors.toList());
		Collections.sort(resultStrList);	// Sort them so that they are in a predictable order 
		assertAll(
				()->assertTrue(resultStrList.get(0).contains(BOOLEAN_DS_NAME), "Expected '" + resultStrList.get(0) + "' to contain '" + BOOLEAN_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(0).contains(Boolean.toString(booleanData)), "Expected '" + resultStrList.get(0) + "' to contain '" + Boolean.toString(booleanData) + "', but it didn't."),
				()->assertTrue(resultStrList.get(1).contains(BYTE_ARRAY_DS_NAME), "Expected '" + resultStrList.get(1) + "' to contain '" + BYTE_ARRAY_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(1).contains(MediaType.APPLICATION_OCTET_STREAM), "Expected '" + resultStrList.get(1) + "' to contain '" + MediaType.APPLICATION_OCTET_STREAM + "', but it didn't."),
				()->assertTrue(resultStrList.get(2).contains(BYTE_ARRAY_W_CT_DS_NAME), "Expected '" + resultStrList.get(2) + "' to contain '" + BYTE_ARRAY_W_CT_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(2).contains(mimeType.asString()), "Expected '" + resultStrList.get(2) + "' to contain '" + mimeType.asString() + "', but it didn't."),
				()->assertTrue(resultStrList.get(3).contains(DOUBLE_DS_NAME), "Expected '" + resultStrList.get(3) + "' to contain '" + DOUBLE_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(3).contains(Double.toString(doubleData)), "Expected '" + resultStrList.get(3) + "' to contain '" + Double.toString(doubleData) + "', but it didn't."),
				()->assertTrue(resultStrList.get(4).contains(dummyDS.name()), "Expected '" + resultStrList.get(4) + "' to contain '" + dummyDS.name() + "', but it didn't."),
				()->assertTrue(resultStrList.get(4).contains(dummyDS.contentType().asString()), "Expected '" + resultStrList.get(4) + "' to contain '" + dummyDS.contentType().asString() + "', but it didn't."),
				()->assertTrue(resultStrList.get(5).contains(FILE_DS_NAME), "Expected '" + resultStrList.get(5) + "' to contain '" + FILE_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(5).contains(StandardMimeTypes.APPLICATION_VND_ADOBE_XDP_STR), "Expected '" + resultStrList.get(5) + "' to contain '" + StandardMimeTypes.APPLICATION_VND_ADOBE_XDP_STR + "', but it didn't."),
				()->assertTrue(resultStrList.get(6).contains(FLOAT_DS_NAME), "Expected '" + resultStrList.get(6) + "' to contain '" + FLOAT_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(6).contains(Float.toString(floatData)), "Expected '" + resultStrList.get(6) + "' to contain '" + Float.toString(floatData) + "', but it didn't."),
				()->assertTrue(resultStrList.get(7).contains(INTEGER_DS_NAME), "Expected '" + resultStrList.get(7) + "' to contain '" + INTEGER_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(7).contains(Integer.toString(intData)), "Expected '" + resultStrList.get(7) + "' to contain '" + Integer.toString(intData) + "', but it didn't."),
				()->assertTrue(resultStrList.get(8).contains(LONG_DS_NAME), "Expected '" + resultStrList.get(8) + "' to contain '" + LONG_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(8).contains(Long.toString(longData)), "Expected '" + resultStrList.get(8) + "' to contain '" + Long.toString(longData) + "', but it didn't."),
				()->assertTrue(resultStrList.get(9).contains(STRING_DS_NAME), "Expected '" + resultStrList.get(9) + "' to contain '" + STRING_DS_NAME + "', but it didn't."),
				()->assertTrue(resultStrList.get(9).contains(stringData), "Expected '" + resultStrList.get(9) + "' to contain '" + stringData + "', but it didn't.")
				);
	}

	@Test
	void testAccept_NoParamsReturned() throws Exception {
		FormsFeederClient underTest = FormsFeederClient.builder()
												  .machineName(formsfeederServerName)
												  .port(formsfeederServerPort)
												  .plugin("Debug")
												  .build();	
		DataSourceList result = underTest.accept(DataSourceList.emptyList());
		
		assertNotNull(underTest.returnedCorrelationId());
		assertFalse(Jdk8Utils.isBlank(underTest.returnedCorrelationId()));
		assertNotNull(result);
		assertEquals(0, result.list().size());
	}

	@Test
	void testAccept_PdfReturned() throws Exception {
		FormsFeederClient underTest = FormsFeederClient.builder()
				  .machineName(formsfeederServerName)
				  .port(formsfeederServerPort)
				  .plugin("Mock")
				  .build();	

		DataSourceList input = DataSourceList.builder().add("scenario", "ReturnPdf").build();
		
		DataSourceList result = underTest.accept(input);
		
		assertEquals(1, result.list().size());
		DataSource resultDs = result.list().get(0);
		assertEquals(StandardMimeTypes.APPLICATION_PDF_TYPE, resultDs.contentType());
		assertTrue(resultDs.filename().isPresent());
		assertEquals(SAMPLE_PDF.getFileName(), resultDs.filename().get());
		PDDocument pdf = PDDocument.load(resultDs.inputStream());
		PDDocumentCatalog catalog = pdf.getDocumentCatalog();
		assertNotNull(pdf);
		assertNotNull(catalog);
	}
	
	@Test
	void testAccept_XmlReturned() throws Exception {
		FormsFeederClient underTest = FormsFeederClient.builder()
				  .machineName(formsfeederServerName)
				  .port(formsfeederServerPort)
				  .plugin("Mock")
				  .build();	

		DataSourceList input = DataSourceList.builder().add("scenario", "ReturnXml").build();
		
		DataSourceList result = underTest.accept(input);
		
		assertEquals(1, result.list().size());
		DataSource resultDs = result.list().get(0);
		assertEquals(StandardMimeTypes.APPLICATION_XML_TYPE, resultDs.contentType());
		assertTrue(resultDs.filename().isPresent());
		assertEquals(SAMPLE_DATA.getFileName(), resultDs.filename().get());
		XML xml = new XMLDocument(resultDs.inputStream());
		assertEquals(2, Integer.valueOf(xml.xpath("count(//form1/*)").get(0)));
	}
	
	@Test
	void testAccept_ManyOutputsReturned() throws Exception {
		FormsFeederClient underTest = FormsFeederClient.builder()
				  .machineName(formsfeederServerName)
				  .port(formsfeederServerPort)
				  .plugin("Mock")
				  .build();	

		DataSourceList input = DataSourceList.builder().add("scenario", "ReturnManyOutputs").build();
		
		DataSourceList result = underTest.accept(input);
		
		assertEquals(3, result.list().size());
		for (DataSource resultDs : result.list()) {
			MimeType contentType = resultDs.contentType();
			if (StandardMimeTypes.APPLICATION_PDF_TYPE.equals(contentType)) {
				assertTrue(resultDs.filename().isPresent());
				assertEquals(SAMPLE_PDF.getFileName(), resultDs.filename().get());
				PDDocument pdf = PDDocument.load(resultDs.inputStream());
				PDDocumentCatalog catalog = pdf.getDocumentCatalog();
				assertNotNull(pdf);
				assertNotNull(catalog);
				
			} else if (StandardMimeTypes.APPLICATION_XML_TYPE.equals(contentType)) {
				assertTrue(resultDs.filename().isPresent());
				assertEquals(SAMPLE_DATA.getFileName(), resultDs.filename().get());
				XML xml = new XMLDocument(resultDs.inputStream());
				assertEquals(2, Integer.valueOf(xml.xpath("count(//form1/*)").get(0)));

			} else if (StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE.equals(contentType)) {
				assertFalse(resultDs.filename().isPresent());
				assertArrayEquals("SampleData".getBytes(StandardCharsets.UTF_8),Jdk8Utils.readAllBytes(resultDs.inputStream()));
			} else {
				fail("Found unexpected contentType in response '" + contentType.asString() + "'.");
			}
		}
	}
	
	
	private enum PluginExceptionScenario {
		BAD_REQUEST("BadRequestException", "statusCode='400'", "Plugin processor detected Bad Request.", "Throwing FeedConsumerBadRequestException because scenario was 'BadRequestException'."), 
		INTERNAL_ERROR("InternalErrorException", "statusCode='500'", "Plugin processor experienced an Internal Server Error.", "Throwing FeedConsumerInternalErrorException because scenario was 'InternalErrorException'."), 
		UNCHECKED("UncheckedException", "statusCode='500'", "Error within Plugin processor.", "Throwing IllegalStateException because scenario was 'UncheckedException'."), 
		OTHER("OtherFeedConsumerException", "statusCode='500'", "Plugin processor error.", "Throwing anonymous FeedConsumerException because scenario was 'OtherFeedConsumerException'.");
		
		private final String scenarioName;
		private final String statusCode;
		private final String frameworkMessage;
		private final String pluginMessage;

		private PluginExceptionScenario(String scenarioName, String statusCode, String frameworkMessage,
				String pluginMessage) {
			this.scenarioName = scenarioName;
			this.statusCode = statusCode;
			this.frameworkMessage = frameworkMessage;
			this.pluginMessage = pluginMessage;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testAccept_FailureReturnedFromServer(PluginExceptionScenario scenario) throws Exception {
		FormsFeederClient underTest = FormsFeederClient.builder()
				  .machineName(formsfeederServerName)
				  .port(formsfeederServerPort)
				  .plugin("Mock")
				  .build();	

		DataSourceList input = DataSourceList.builder().add("scenario",scenario.scenarioName).build();
		
		FormsFeederClientException ex = assertThrows(FormsFeederClientException.class, ()->underTest.accept(input));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertTrue(msg.contains(scenario.statusCode), "Expected '" + msg + "' to contain '" + scenario.statusCode + "'."),
				()->assertTrue(msg.contains(scenario.frameworkMessage), "Expected '" + msg + "' to contain '" + scenario.frameworkMessage + "'."),
				()->assertTrue(msg.contains(scenario.pluginMessage), "Expected '" + msg + "' to contain '" + scenario.pluginMessage + "'.")
				);
	}
	
	@Test
	void testBuilder_NoPluginSupplied() {
		NullPointerException ex1 = assertThrows(NullPointerException.class, ()->FormsFeederClient.builder().build());
		String msg1 = ex1.getMessage();
		assertNotNull(msg1);
		assertAll(
				()->assertTrue(msg1.contains("plugin()"), "Expected '" + msg1 + "' to contain 'plugin()'."),
				()->assertTrue(msg1.contains("must be supplied"), "Expected '" + msg1 + "' to contain 'must be supplied'.")
				);
	}

	private static String toString(DataSource ds) {
		try {
			return new String(Jdk8Utils.readAllBytes(ds.inputStream()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Exception while converting DataSource content to String", e);
		}
	}
}
