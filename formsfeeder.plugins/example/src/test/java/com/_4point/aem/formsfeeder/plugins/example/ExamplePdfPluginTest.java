package com._4point.aem.formsfeeder.plugins.example;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.fluentforms.api.Document;
import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.fluentforms.testing.forms.MockTraditionalFormsService;
import com._4point.aem.fluentforms.testing.output.MockTraditionalOutputService;
import com._4point.aem.formsfeeder.core.api.AemConfig;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerBadRequestException;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;

class ExamplePdfPluginTest {
	private static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	private static final Path SAMPLE_FILES_DIR = RESOURCES_FOLDER.resolve("SampleFiles");
	private static final Path ACTUAL_RESULTS_DIR = RESOURCES_FOLDER.resolve("ActualResults");
	private static final Path SAMPLE_XDP = SAMPLE_FILES_DIR.resolve("SampleForm.xdp");
	private static final Path SAMPLE_DATA = SAMPLE_FILES_DIR.resolve("SampleForm_data.xml");

	private static final String TEMPLATE_PARAM_NAME = "template";
	private static final String DATA_PARAM_NAME = "data";
	private static final String INTERACTIVE_PARAM_NAME = "interactive";

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
	
	private ExamplePdfPlugin underTest = new ExamplePdfPlugin();

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
	
	@SuppressWarnings("unused")
	@ParameterizedTest
	@EnumSource
	void testAccept_Pdf(PdfScenario scenario) throws Exception {
		AemConfig aemConfig = mockConfig("localhost", 4502);
		final byte[] expectedResponse = "Expected Response Data".getBytes();
		String expectedContentType = "application/pdf";
		final Document expectedResponseDoc = SimpleDocumentFactoryImpl.getFactory().create(expectedResponse);
		expectedResponseDoc.setContentType(expectedContentType);
	
		if (!USE_AEM) {
			if (scenario.isInteractiveFlag()) {
				// Mock the REST Forms Client
				underTest.tradFormsServiceSupplier(()->MockTraditionalFormsService.createRenderFormMock(expectedResponseDoc));
			} else {
				// Mock the REST Output Client
				underTest.tradOutputServiceSupplier(()->MockTraditionalOutputService.createDocumentMock(expectedResponseDoc));
			}
		}
		DataSourceList testData = DataSourceList.builder()
												.add(TEMPLATE_PARAM_NAME, SAMPLE_XDP.toString())	// Add as Strings so that the names are passed, not the contents.
												.add(DATA_PARAM_NAME, SAMPLE_DATA.toString())
												.add(INTERACTIVE_PARAM_NAME, scenario.isInteractiveFlag())
												.build();

		junitx.util.PrivateAccessor.setField(underTest,"aemConfig", aemConfig); 
		
		DataSourceList result = underTest.accept(testData);
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(1, result.list().size());
		DataSource resultDs = result.list().get(0);
		byte[] resultBytes = resultDs.inputStream().readAllBytes();
		if (SAVE_RESULTS && USE_AEM) {
			try (var os = Files.newOutputStream(ACTUAL_RESULTS_DIR.resolve("testAccept_Pdf_" + scenario.toString() + ".pdf"))) {
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
		assertEquals("RenderPdf", underTest .name());
	}

	@ParameterizedTest
	@ValueSource ( strings = {"true", "false"})
	void testExampleParametersFrom(boolean interactiveFlag) throws Exception {
		final Path expectedTemplateData = SAMPLE_XDP;  
		final Path expectedDataData = SAMPLE_DATA;
		DataSourceList dataSourceList = DataSourceList.builder()
													  .add(TEMPLATE_PARAM_NAME, expectedTemplateData.toString())
													  .add(DATA_PARAM_NAME, expectedDataData.toString())
													  .add(INTERACTIVE_PARAM_NAME, interactiveFlag)
													  .build();
		
		ExamplePdfPlugin.ExamplePluginInputParameters result = ExamplePdfPlugin.ExamplePluginInputParameters.from(dataSourceList, SimpleDocumentFactoryImpl.getFactory());
		assertArrayEquals(Files.newInputStream(expectedTemplateData).readAllBytes(), result.getTemplate().getInlineData());
		assertArrayEquals(Files.newInputStream(expectedDataData).readAllBytes(), result.getData().getInlineData());
		assertEquals(interactiveFlag, result.isInteractive());
	}
	
	private enum ExampleParametersMissingParametersScenario {
		TEMPLATE_MISSING(true, false, false), DATA_MISSING(false, true, false), INTERACTIVE_MISSING(false, false, true);
		
		private final boolean templateMissing;
		private final boolean dataMissing;
		private final boolean interactiveMissing;
		/**
		 * @param templateMissing
		 * @param dataMissing
		 * @param interactiveMissing
		 */
		private ExampleParametersMissingParametersScenario(boolean templateMissing, boolean dataMissing,
				boolean interactiveMissing) {
			this.templateMissing = templateMissing;
			this.dataMissing = dataMissing;
			this.interactiveMissing = interactiveMissing;
		}
		
		private DataSourceList getDataSourceList() {
			final Path expectedTemplateData = SAMPLE_XDP;  
			final Path expectedDataData = SAMPLE_DATA;
			Builder builder = DataSourceList.builder();
			if (!templateMissing) {
				builder.add(TEMPLATE_PARAM_NAME, expectedTemplateData.toString());
			}
			if (!dataMissing) {
				builder.add(DATA_PARAM_NAME, expectedDataData.toString());
			}
			if (!interactiveMissing) {
				builder.add(INTERACTIVE_PARAM_NAME, false);
			}
			return builder.build();
		}
		
		private String missingParamName() {
			if (templateMissing) {
				return TEMPLATE_PARAM_NAME;
			}
			if (dataMissing) {
				return DATA_PARAM_NAME;
			}
			if (interactiveMissing) {
				return INTERACTIVE_PARAM_NAME;
			}
			throw new IllegalStateException("Unexpected parameter missing '" + this.toString() + "'");
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testExampleParametersMissingParameters(ExampleParametersMissingParametersScenario scenario) {
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->ExamplePdfPlugin.ExamplePluginInputParameters.from(scenario.getDataSourceList(), SimpleDocumentFactoryImpl.getFactory()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertTrue(msg.contains(scenario.missingParamName()), "Expected message '" + msg + "' to contain missing parameter name '" + scenario.missingParamName() + "'."),
				()->assertTrue(msg.contains("Parameter must be supplied"), "Expected message '" + msg + "' to contain text 'Parameter must be supplied'.")
				);
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
		};
	}
}
