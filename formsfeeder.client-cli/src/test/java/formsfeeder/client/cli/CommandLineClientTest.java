package formsfeeder.client.cli;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

class CommandLineClientTest {
	private static final String FF_SERVER_MACHINE_NAME = "localhost";
	private static final int FF_SERVER_MACHINE_PORT = 8080;

	private static final String FORMSFEEDER_SERVER_USERNAME = "username";
	private static final String FORMSFEEDER_SERVER_PASSWORD = "password";

	private static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	private static final Path SAMPLE_FILES_DIR = RESOURCES_FOLDER.resolve("SampleFiles");
	private static final Path SAMPLE_PDF = SAMPLE_FILES_DIR.resolve("SampleForm.pdf");

	private static final boolean USE_WIREMOCK = true;
	private static final boolean WIREMOCK_RECORDING = false;

	private WireMockServer wireMockServer;
	private static Integer wiremockPort = null;
	
	private String formsfeederServerName;
	private int formsfeederServerPort;

	private final ByteArrayInputStream stdin = new ByteArrayInputStream(new byte[0]);
	private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
	private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

	@BeforeEach
	public void setUp() throws Exception {
		if (USE_WIREMOCK) {
			// Let wiremock choose the port for the first test, but re-use the same port for all subsequent tests.
			wireMockServer = new WireMockServer(wiremockPort == null ? new WireMockConfiguration().dynamicPort().extensions(new ResponseTemplateTransformer(true)) : new WireMockConfiguration().port(wiremockPort).extensions(new ResponseTemplateTransformer(true)));
	        wireMockServer.start();
			if (WIREMOCK_RECORDING) {
				String baseUrl = "http://" + FF_SERVER_MACHINE_NAME + ":"	+ FF_SERVER_MACHINE_PORT;
				System.out.println("Wiremock recording of '" + baseUrl + "'.");
				wireMockServer.startRecording(baseUrl);
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
	void testMain_QueryParamMultiValue() throws Exception {
		String qpName1 = "qpName1";
		String qpValue1 = "qpValue1";
		String qpName2 = "qpName2";
		String qpValue2 = "qpValue2";

		String[] args = { "-h", getFFServerLocation(),
				"-u", getFFServerCredentials(),
				"-p", "Debug",
				"-qp", qpName1+"="+qpValue1,
				"-qp", qpName1+"="+qpValue2,
				"-qp", qpName2+"="+qpValue2};

		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);

		assertAll(
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
		);

		if(USE_WIREMOCK) {
			wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1/Debug"))
					.withQueryParam(qpName1, matching(qpValue1))
					.withQueryParam(qpName1, matching(qpValue2) )
					.withQueryParam(qpName2,matching(qpValue2)));
		}
	}

	@Test
	void testMain_QueryParams() throws Exception {
		String qpName1 = "qpName1";
		String qpValue1 = "qpValue1";
		String qpName2 = "qpName2";
		String qpValue2 = "qpValue2";

		String[] args = { "-h", getFFServerLocation(),
				"-u", getFFServerCredentials(),
				"-p", "Debug",
				"-qp", qpName1+"="+qpValue1,
				"-qp", qpName2+"="+qpValue2};

		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);

		assertAll(
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
		);

		if(USE_WIREMOCK) {
			wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1/Debug"))
					.withQueryParam(qpName1, matching(qpValue1))
					.withQueryParam(qpName2,matching(qpValue2)));
		}
	}

	@Test
	void testMain_ContextRoot() throws Exception {
		String expectedParamValue = "ParamValue";
		String expectedParamName = "Param";
		String expectedContextRoot = "/context/root/";

		String[] args = { "-h", getFFServerLocation(),
				"-u", getFFServerCredentials(),
				"-d", expectedParamName + "=" + expectedParamValue,
				"-p", "Debug",
				"-cr", expectedContextRoot};

		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);

		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stdoutStr.contains(expectedParamName), "Expected '" + stdoutStr + "' to contain '" + expectedParamName + "', but didn't."),
				()->assertTrue(stdoutStr.contains(expectedParamValue), "Expected '" + stdoutStr + "' to contain '" + expectedParamValue + "', but didn't."),
				()->assertTrue(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't."),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
		);

		if(USE_WIREMOCK) {
			wireMockServer.verify(postRequestedFor(urlMatching(expectedContextRoot+"Debug")));
		}
	}

	@Test
	void testMain_RequestHeaders() throws Exception {
		String expectedParamValue = "ParamValue";
		String expectedParamName = "Param";
		String expectedHeader1 = "header1";
		String expectedHeader1Value = "header1Value";
		String expectedAuthHeader = "Authorization";
		String expectedAuthHeaderValue = "Basic " + Base64.encodeBase64String((FORMSFEEDER_SERVER_USERNAME + ":" + FORMSFEEDER_SERVER_PASSWORD).getBytes());

		String[] args = { "-h", getFFServerLocation(),
				"-u", getFFServerCredentials(),
				"-d", expectedParamName + "=" + expectedParamValue,
				"-p", "Debug",
				"-hdr", expectedHeader1+"="+expectedHeader1Value};

		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);

		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stdoutStr.contains(expectedParamName), "Expected '" + stdoutStr + "' to contain '" + expectedParamName + "', but didn't."),
				()->assertTrue(stdoutStr.contains(expectedParamValue), "Expected '" + stdoutStr + "' to contain '" + expectedParamValue + "', but didn't."),
				()->assertTrue(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't."),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
		);

		if(USE_WIREMOCK) {
			wireMockServer.verify(postRequestedFor(urlMatching("/api/v1/Debug"))
					.withHeader(expectedAuthHeader, matching(expectedAuthHeaderValue))
					.withHeader(expectedHeader1,matching(expectedHeader1Value)));
		}
	}

	@Test
	void testMain_OneDS_OneOutput_Stdout() throws Exception {
		
		String expectedParamValue = "ParamValue";
		String expectedParamName = "Param";
		String[] args = { "-h", getFFServerLocation(),
						  "-u", getFFServerCredentials(),
						  "-d", expectedParamName + "=" + expectedParamValue,
						  "-p", "Debug"};
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stdoutStr.contains(expectedParamName), "Expected '" + stdoutStr + "' to contain '" + expectedParamName + "', but didn't."),
				()->assertTrue(stdoutStr.contains(expectedParamValue), "Expected '" + stdoutStr + "' to contain '" + expectedParamValue + "', but didn't."),
				()->assertTrue(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't."),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
				);
	}

	@Test
	void testMain_OneDS_OneOutput_File() throws Exception {
		String expectedParamValue = "ParamValue";
		String expectedParamName = "Param";
		String expectedOutputLocation = "testMain_OneDS_OneOutput_File_result.txt";
		String[] args = { "-h", getFFServerLocation(), 
						  "-u", getFFServerCredentials(),
						  "-d", expectedParamName + "=" + expectedParamValue,
						  "-p", "Debug", 
						  "-o", expectedOutputLocation};
		
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		// Make sure stdout *does not* contain the expected output.
		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertFalse(stdoutStr.contains(expectedParamName), "Expected '" + stdoutStr + "' to not contain '" + expectedParamName + "', but did."),
				()->assertFalse(stdoutStr.contains(expectedParamValue), "Expected '" + stdoutStr + "' to not contain '" + expectedParamValue + "', but did."),
				()->assertFalse(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to not contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but did."),
				()->assertTrue(stdoutStr.contains(expectedOutputLocation), "Expected '" + stdoutStr + "' to contain '" + expectedOutputLocation + "', but did not."),
				()->assertEquals(1, getFileCount(fs), "Expected only one file to be created."),		// Verify that there's only one file created.
				()->assertTrue(Files.exists(fs.getPath(expectedOutputLocation)))		// Verify that the expected file exists
				);
		
		// Make sure the file *does* contain the expected output.
		String outputContents = new String(Jdk8Utils.readAllBytes(Files.newInputStream(fs.getPath(expectedOutputLocation))), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(outputContents.contains(expectedParamName), "Expected '" + outputContents + "' to contain '" + expectedParamName + "', but didn't."),
				()->assertTrue(outputContents.contains(expectedParamValue), "Expected '" + outputContents + "' to contain '" + expectedParamValue + "', but didn't."),
				()->assertTrue(outputContents.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + outputContents + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't.")
				);
	}

	@Test
	void testMain_ManyDS_ManyOutput_Stdout() throws Exception {
		String samplePdfFilename = SAMPLE_PDF.toString();
		String[] expectedParamValues = { "Param1Value", "@" + samplePdfFilename, "Param3Value" }; // Two strings and a document parameter
		String[] expectedParamNames = { "Param1", "Param2", "Param3" }; 
		String[] args = { "-h", getFFServerLocation(), 
						  "-u", getFFServerCredentials(),
						  "-d", expectedParamNames[0] + "=" + expectedParamValues[0], 
						  "-d", expectedParamNames[1] + "=" + expectedParamValues[1], 
						  "-d", expectedParamNames[2] + "=" + expectedParamValues[2], 
						  "-p", "Debug"};
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(stdout.toByteArray()));
		ZipEntry ze;
		int count = 0;
		while((ze = zis.getNextEntry()) != null) {
//			System.out.println("Found ZipEntry '" + ze.getName() + "' of size '" + ze.getSize() + "'.");
			
			String zipStr = new String(Jdk8Utils.readAllBytes(zis), StandardCharsets.UTF_8);
			final int myCount = expectedParamValues.length - (count + 1);
			if (myCount >= 0) {	// Skip this section rather than generating out of bounds exceptions.  We'll catch the problem below.
				if (myCount != 1) {	// String Parameters
					assertAll(
							()->assertTrue(zipStr.contains(expectedParamNames[myCount]), "Expected '" + zipStr + "' to contain '" + expectedParamNames[myCount] + "', but didn't."),
							()->assertTrue(zipStr.contains(expectedParamValues[myCount]), "Expected '" + zipStr + "' to contain '" + expectedParamValues[myCount] + "', but didn't."),
							()->assertTrue(zipStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + zipStr + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't.")
							);
				} else {		// File Parameter
					assertAll(
							()->assertTrue(zipStr.contains(expectedParamNames[myCount]), "Expected '" + zipStr + "' to contain '" + expectedParamNames[myCount] + "', but didn't."),
							()->assertTrue(zipStr.contains(SAMPLE_PDF.getFileName().toString()), "Expected '" + zipStr + "' to contain '" + SAMPLE_PDF.getFileName().toString() + "', but didn't."),
							()->assertTrue(zipStr.contains(StandardMimeTypes.APPLICATION_PDF_STR), "Expected '" + zipStr + "' to contain '" + StandardMimeTypes.APPLICATION_PDF_STR + "', but didn't.")
							);
				}
			}
			count++;
		}
		assertEquals(expectedParamValues.length, count, "Expected to find the same number of ZipEntries as parameters.");
		assertEquals(0, getFileCount(fs), "Expected no files to be created.");
	}

	
	@Test
	void testMain_ManyDS_ManyOutput_File() throws Exception {
		String samplePdfFilename = SAMPLE_PDF.toString();
		String[] expectedParamValues = { "Param1Value", "@" + samplePdfFilename, "Param3Value" }; // Two strings and a document parameter
		String[] expectedParamNames = { "Param1", "Param2", "Param3" }; 
		String expectedOutputLocation = "testMain_ManyDS_ManyOutput_File_result.zip";
		String[] args = { "-h", getFFServerLocation(), 
			  		  	  "-u", getFFServerCredentials(),
			  		  	  "-d", expectedParamNames[0] + "=" + expectedParamValues[0], 
						  "-d", expectedParamNames[1] + "=" + expectedParamValues[1], 
						  "-d", expectedParamNames[2] + "=" + expectedParamValues[2],
						  "-o", expectedOutputLocation,
						  "-p", "Debug"};
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);

		// Make sure stdout *does not* contain the expected output.
		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertFalse(stdoutStr.contains(expectedParamNames[0]), "Expected '" + stdoutStr + "' to not contain '" + expectedParamNames[0] + "', but did."),
				()->assertFalse(stdoutStr.contains(expectedParamValues[0]), "Expected '" + stdoutStr + "' to not contain '" + expectedParamValues[0] + "', but did."),
				()->assertFalse(stdoutStr.contains(expectedParamNames[1]), "Expected '" + stdoutStr + "' to not contain '" + expectedParamNames[1] + "', but did."),
				()->assertFalse(stdoutStr.contains(SAMPLE_PDF.getFileName().toString()), "Expected '" + stdoutStr + "' to not contain '" + SAMPLE_PDF.getFileName().toString() + "', but did."),
				()->assertFalse(stdoutStr.contains(expectedParamNames[2]), "Expected '" + stdoutStr + "' to not contain '" + expectedParamNames[2] + "', but did."),
				()->assertFalse(stdoutStr.contains(expectedParamValues[2]), "Expected '" + stdoutStr + "' to not contain '" + expectedParamValues[2] + "', but did."),
				()->assertFalse(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to not contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but did."),
				()->assertTrue(stdoutStr.contains(expectedOutputLocation), "Expected '" + stdoutStr + "' to contain '" + expectedOutputLocation + "', but did not."),
				()->assertEquals(1, getFileCount(fs), "Expected only one file to be created."),		// Verify that only one file was created.
				()->assertTrue(Files.exists(fs.getPath(expectedOutputLocation)))		// Verify that the expected file exists
				);
		
		
		// Make sure the file *does* contain the expected output.
		ZipInputStream zis = new ZipInputStream(Files.newInputStream(fs.getPath(expectedOutputLocation)));
		ZipEntry ze;
		int count = 0;
		while((ze = zis.getNextEntry()) != null) {
//			System.out.println("Found ZipEntry '" + ze.getName() + "' of size '" + ze.getSize() + "'.");
			
			String zipStr = new String(Jdk8Utils.readAllBytes(zis), StandardCharsets.UTF_8);
			final int myCount = expectedParamValues.length - (count + 1);
			if (myCount >= 0) {	// Skip this section rather than generating out of bounds exceptions.  We'll catch the problem below.
				if (myCount != 1) {	// String Parameters
					assertAll(
							()->assertTrue(zipStr.contains(expectedParamNames[myCount]), "Expected '" + zipStr + "' to contain '" + expectedParamNames[myCount] + "', but didn't."),
							()->assertTrue(zipStr.contains(expectedParamValues[myCount]), "Expected '" + zipStr + "' to contain '" + expectedParamValues[myCount] + "', but didn't."),
							()->assertTrue(zipStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + zipStr + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't.")
							);
				} else {		// File Parameter
					assertAll(
							()->assertTrue(zipStr.contains(expectedParamNames[myCount]), "Expected '" + zipStr + "' to contain '" + expectedParamNames[myCount] + "', but didn't."),
							()->assertTrue(zipStr.contains(SAMPLE_PDF.getFileName().toString()), "Expected '" + zipStr + "' to contain '" + SAMPLE_PDF.getFileName().toString() + "', but didn't."),
							()->assertTrue(zipStr.contains(StandardMimeTypes.APPLICATION_PDF_STR), "Expected '" + zipStr + "' to contain '" + StandardMimeTypes.APPLICATION_PDF_STR + "', but didn't.")
							);
				}
			}
			count++;
		}
		assertEquals(expectedParamValues.length, count, "Expected to find the same number of ZipEntries as parameters.");
	}

	@Test
	void testMain_OneDSNoValue_OneOutput_Stdout() throws Exception {
		String expectedParamName = "ParamNoValue";
		String[] args = { "-h", getFFServerLocation(), 
				  		  "-u", getFFServerCredentials(),
						  "-d", expectedParamName,			// No value supplied, should result in an empty string in the output.
						  "-p", "Debug"};
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stdoutStr.contains(expectedParamName), "Expected '" + stdoutStr + "' to contain '" + expectedParamName + "', but didn't."),
				()->assertTrue(stdoutStr.contains("''"), "Expected '" + stdoutStr + "' to contain '', but didn't."),
				()->assertTrue(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't."),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
				);
	}


	
	// Test two cases - one with an output filename and one without.
	// If an output filename is supplied, then that is used.
	// If an output filename is not supplied, then the filename provided in the response is used.
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"testMain_XML_File_result.xml"})
	void testMain_XML_File(String providedOutputLocation) throws Exception {
		String expectedParamName = "scenario";
		String expectedParamValue = "ReturnXml";
		String[] args = { "-h", getFFServerLocation(), 
				  		  "-u", getFFServerCredentials(),
						  "-d", expectedParamName + "=" + expectedParamValue,
						  "-p", "Mock"};

		if (providedOutputLocation != null) {	// If providedOutputLocation is not null, then add the -o parameter.
			int origLength = args.length;
			args = Arrays.copyOf(args, origLength + 2);
			args[origLength] = "-o";
			args[origLength+1] = providedOutputLocation;
		}
		
		final String expectedOutputLocation = providedOutputLocation != null ? providedOutputLocation : "SampleForm_data.xml"; 
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		// Make sure stdout *does not* contain the expected output.
		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertFalse(stdoutStr.contains(expectedParamName), "Expected '" + stdoutStr + "' to not contain '" + expectedParamName + "', but did."),
				()->assertFalse(stdoutStr.contains(expectedParamValue), "Expected '" + stdoutStr + "' to not contain '" + expectedParamValue + "', but did."),
				()->assertFalse(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to not contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but did."),
				()->assertTrue(stdoutStr.contains(expectedOutputLocation), "Expected '" + stdoutStr + "' to contain '" + expectedOutputLocation + "', but did not."),
				()->assertEquals(1, getFileCount(fs), "Expected only one file to be created."),		// Verify that there's only one file created.
				()->assertTrue(Files.exists(fs.getPath(expectedOutputLocation)))		// Verify that the expected file exists
				);
		
		// Make sure the file *does* contain the expected output.
		XML xml = new XMLDocument(Files.newInputStream(fs.getPath(expectedOutputLocation)));
		assertEquals(2, Integer.valueOf(xml.xpath("count(//form1/*)").get(0)));
	}	
	
	@Test
	void testMain_XML_Stdout() throws Exception {
		String expectedParamName = "scenario";
		String expectedParamValue = "ReturnXml";
		String[] args = { "-h", getFFServerLocation(), 
				  		  "-u", getFFServerCredentials(),
						  "-d", expectedParamName + "=" + expectedParamValue,
						  "-o", "---",	// Send to stdout
						  "-p", "Mock"};

		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		// Make sure stdout *does* contain the expected output.
		XML xml = new XMLDocument(new ByteArrayInputStream(stdout.toByteArray()));
		assertAll(
				()->assertEquals(2, Integer.valueOf(xml.xpath("count(//form1/*)").get(0))),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")		// Verify that no file is created.
				);
		
	}	

	@Test
	void testMain_OneOutput_CantWriteException() throws Exception {
		String expectedParamValue = "Param1Value";
		String expectedParamName = "Param1";
		String expectedOutputLocation = "testMain_OneOutput_CantWriteException_result.txt";
		String[] args = { "-h", getFFServerLocation(), 
				  		  "-u", getFFServerCredentials(),
						  "-d", expectedParamName + "=" + expectedParamValue,
						  "-p", "Debug", 
						  "-o", expectedOutputLocation};
		
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		Path expectedPath = fs.getPath(expectedOutputLocation);
		Files.createDirectory(expectedPath);
		
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		// Make sure stderr contains the expected output.
		String stderrStr = new String(stderr.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stderrStr.contains(expectedOutputLocation), "Expected '" + stderrStr + "' to contain '" + expectedOutputLocation + "', but did not."),
				()->assertTrue(stderrStr.contains("Unable to write to file"), "Expected '" + stderrStr + "' to contain 'Unable to write to file', but did not."),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
				);
	}

	@Test
	void testMain_ManyOutput_CantWriteException() throws Exception {
		String samplePdfFilename = SAMPLE_PDF.toString();
		String[] expectedParamValues = { "Param1Value", "@" + samplePdfFilename, "Param3Value" }; // Two strings and a document parameter
		String[] expectedParamNames = { "Param1", "Param2", "Param3" }; 
		String expectedOutputLocation = "testMain_ManyOutput_CantWriteException_result.zip";
		String[] args = { "-h", getFFServerLocation(), 
				  		  "-u", getFFServerCredentials(),
						  "-d", expectedParamNames[0] + "=" + expectedParamValues[0], 
						  "-d", expectedParamNames[1] + "=" + expectedParamValues[1], 
						  "-d", expectedParamNames[2] + "=" + expectedParamValues[2],
						  "-o", expectedOutputLocation,
						  "-p", "Debug"};
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		Path expectedPath = fs.getPath(expectedOutputLocation);
		Files.createDirectory(expectedPath);

		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);

		// Make sure stderr contains the expected output.
		String stderrStr = new String(stderr.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stderrStr.contains(expectedOutputLocation), "Expected '" + stderrStr + "' to contain '" + expectedOutputLocation + "', but did not."),
				()->assertTrue(stderrStr.contains("Unable to write to file"), "Expected '" + stderrStr + "' to contain 'Unable to write to file', but did not."),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
				);
	}

	@Test
	void testMain_PluginException() throws Exception {
		String expectedParamName = "scenario";
		String expectedParamValue = "BadRequestException";
		String expectedOutputLocation = "testMain_PluginException_result.txt";
		String[] args = { "-h", getFFServerLocation(), 
						  "-u", getFFServerCredentials(),
						  "-d", expectedParamName + "=" + expectedParamValue,
						  "-o", expectedOutputLocation,
						  "-p", "Mock"};

		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		// Make sure stderr contains the expected output.
		String stderrStr = new String(stderr.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stderrStr.contains("Call to server failed"), "Expected '" + stderrStr + "' to contain 'Call to server failed', but did not."),
				()->assertTrue(stderrStr.contains("Bad Request"), "Expected '" + stderrStr + "' to contain 'Bad Request', but did not."),
				()->assertTrue(stderrStr.contains("FeedConsumerBadRequestException"), "Expected '" + stderrStr + "' to contain 'FeedConsumerBadRequestException', but did not."),
				()->assertTrue(stderrStr.contains("scenario was 'BadRequestException'"), "Expected '" + stderrStr + "' to contain 'scenario was 'BadRequestException'', but did not."),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
				);
	}	

	@Test
	void testMain_BadAuthParam() throws Exception {
		String expectedParamName = "scenario";
		String expectedParamValue = "BadRequestException";
		String expectedOutputLocation = "testMain_BadAuthParam_result.txt";
		String badAuthParam = "foobar";
		String[] args = { "-h", getFFServerLocation(), 
				  "-d", expectedParamName + "=" + expectedParamValue,
				  "-o", expectedOutputLocation,
				  "-u", badAuthParam,
				  "-p", "Mock"};
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		// Make sure stderr contains the expected output.
		String stderrStr = new String(stderr.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stderrStr.contains("Can't parse auth parameter"), "Expected '" + stderrStr + "' to contain 'Can't parse auth parameter', but did not."),
				()->assertTrue(stderrStr.contains(badAuthParam), "Expected '" + stderrStr + "' to contain '" + badAuthParam + "', but did not."),
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
				);
	}

	private static int getFileCount(FileSystem fs) throws IOException {
		int fileCount = 0;
		for(Path path : fs.getRootDirectories()) {
			fileCount += getFileCount(path); 
		}
		return fileCount;
		
	}
	
	private static int getFileCount(Path directory) throws IOException {
		int fileCount = 0;
		for (Path path : Files.newDirectoryStream(directory)) {
			if (Files.isDirectory(path)) {
//				System.out.println("Found dir '" + path.toString() + "'.");
				fileCount += getFileCount(path);
			} else {
//				System.out.println("Found file '" + path.toString() + "'.");
				fileCount += 1;
			}
		}
		return fileCount;
	}
	
	private String getFFServerLocation() {
		return "http://" + formsfeederServerName + ":" + formsfeederServerPort;
	}
	
	private String getFFServerCredentials() {
		return FORMSFEEDER_SERVER_USERNAME + ":" + FORMSFEEDER_SERVER_PASSWORD;
	}
}
