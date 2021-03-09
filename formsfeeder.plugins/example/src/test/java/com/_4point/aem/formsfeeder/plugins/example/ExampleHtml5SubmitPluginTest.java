package com._4point.aem.formsfeeder.plugins.example;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.fluentforms.testing.output.MockTraditionalOutputService;
import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.api.AemConfig.Protocol;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;

class ExampleHtml5SubmitPluginTest {
	private static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	private static final Path SAMPLE_FILES_DIR = RESOURCES_FOLDER.resolve("SampleFiles");
	private static final Path ACTUAL_RESULTS_DIR = RESOURCES_FOLDER.resolve("ActualResults");
	private static final Path SAMPLE_XDP = SAMPLE_FILES_DIR.resolve("SampleForm.xdp");
	private static final Path SAMPLE_DATA = SAMPLE_FILES_DIR.resolve("SampleForm_data.xml");

	private static final String PLUGIN_NAME = "Html5Submit";
	private static final String FORMSFEEDER_PREFIX = "formsfeeder:";
	private static final String SUBMITTED_DATA_DS_NAME = FORMSFEEDER_PREFIX + "SubmittedData";
	private static final String TEMPLATE_DS_NAME = FORMSFEEDER_PREFIX + "Template";
	private static final String CONTENT_ROOT_DS_NAME = FORMSFEEDER_PREFIX + "ContentRoot";

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
	private static final boolean USE_AEM = false;
	
	ExampleHtml5SubmitPlugin underTest = new ExampleHtml5SubmitPlugin();

	@Test
	void testAccept_Html5Submit() throws Exception {
		final byte[] expectedResponse = "Expected Response Data".getBytes();
		String expectedContentType = "application/pdf";
		final Document expectedResponseDoc = SimpleDocumentFactoryImpl.getFactory().create(expectedResponse);
		expectedResponseDoc.setContentType(expectedContentType);

		// Mock the REST Output Client
		if (!USE_AEM) {
			underTest.tradOutputServiceSupplier(()->MockTraditionalOutputService.createDocumentMock(expectedResponseDoc));
		}
		junitx.util.PrivateAccessor.setField(underTest,"aemConfig", mockConfig("localhost", 4502, Protocol.HTTP)); 
		
		DataSourceList result = underTest.accept(DataSourceList.build(ExampleHtml5SubmitPluginTest::buildInputs));

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(1, result.list().size());
		DataSource resultDs = result.list().get(0);
		byte[] resultBytes = resultDs.inputStream().readAllBytes();
		if (SAVE_RESULTS && USE_AEM) {
			try (var os = Files.newOutputStream(ACTUAL_RESULTS_DIR.resolve("testAccept_Html5Submit.pdf"))) {
				os.write(resultBytes);;
			}
		}
		assertEquals(expectedContentType,resultDs.contentType().asString());
		if (!USE_AEM) {
			// Since we're not using AEM, we can verify the result bytes.
			assertArrayEquals(expectedResponse, resultBytes, "Expected the result bytes to match the expected response we gave the mock object.");
		}		
	}

	@Test
	void testName() {
		assertEquals(PLUGIN_NAME, underTest.name());
	}

	private static DataSourceList.Builder buildInputs(DataSourceList.Builder builder) {
		return builder.add(SUBMITTED_DATA_DS_NAME, SAMPLE_DATA)
					  .add(TEMPLATE_DS_NAME, "SampleForm.xdp")
					  .add(CONTENT_ROOT_DS_NAME, "crx:///content/dam/formsanddocuments/sample-forms");
	}
	
	private static AemConfig mockConfig(String host, int port, Protocol protocol) {
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
				return protocol;
			}

			@Override
			public AemServerType serverType() {
				return null;
			}
		};
	}
}
