package com._4point.aem.formsfeeder.plugins.mock;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerBadRequestException;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerException;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerInternalErrorException;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;

class MockSubmitExtensionTest {

	private static final String FEED_CONSUMER_NAME = "MockSubmit";
	private static final String SCENARIO_ELEMENT_NAME = "scenario";
	
	private static final String SCENARIO_NAME_UNKNOWN = "Unknown";

	
	private static final String FORMSFEEDER_PREFIX = "formsfeeder:";
	private static final String REDIRECT_LOCATION_DS_NAME = FORMSFEEDER_PREFIX + "RedirectLocation";

	private final MockSubmitExtension underTest = new MockSubmitExtension();
	
	@Test
	void testAccept_NoReturns() throws Exception {
		final String scenarioName = "NoReturns";
		DataSourceList result = underTest.accept(InputDslBuilder.build(scenarioName));
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testAccept_ReturnPdf() throws Exception {
		final String scenarioName = "ReturnPdf";
		DataSourceList result = underTest.accept(InputDslBuilder.build(scenarioName));
		assertNotNull(result);
		assertEquals(1, result.list().size());
		DataSource pdfDataSource = result.list().get(0);
		assertAll(
				()->assertEquals(StandardMimeTypes.APPLICATION_PDF_TYPE, pdfDataSource.contentType()),
				()->assertEquals("SampleForm.pdf", pdfDataSource.filename().get().getFileName().toString()),
				()->assertEquals("PdfResult", pdfDataSource.name()),
				()->assertEquals(1, pdfDataSource.attributes().size()),	// We add a content disposition for this test.
				()->assertEquals("attachment", pdfDataSource.attributes().get("formsfeeder:Content-Disposition"))
				);
	}

	@Test
	void testAccept_ReturnsOtherParameters() throws Exception {
		final String scenarioName = "ReturnHtml5Parameters";
		final String expectedTemplateUrl = "sampleTemplateUrl";
		final String expectedContentRoot = "sampleContentRoot";
		final String expectedSubmitUrl = "sampleSubmitUrl";
		DataSourceList result = underTest.accept(InputDslBuilder.createBuilder(scenarioName)
				.template(expectedTemplateUrl)
				.contentRoot(expectedContentRoot)
				.submitUrl(expectedSubmitUrl)
				.build()
				);
		assertNotNull(result);
		assertEquals(1, result.list().size());
		String output = result.deconstructor().getStringByName("Result").get();
		assertAll(
				()->assertTrue(output.contains(expectedTemplateUrl), "Expected '" + output + "' to contain template url '" + expectedTemplateUrl + "'."),
				()->assertTrue(output.contains(expectedContentRoot), "Expected '" + output + "' to contain content root '" + expectedContentRoot + "'."),
				()->assertTrue(output.contains(expectedSubmitUrl), "Expected '" + output + "' to contain submit url '" + expectedSubmitUrl + "'.")
				);
		
	}

	@Test
	void testAccept_ReturnRedirect() throws Exception {
		final String scenarioName = "ReturnRedirect";
		DataSourceList result = underTest.accept(InputDslBuilder.build(scenarioName));
		assertNotNull(result);
		assertEquals(1, result.list().size());
		assertEquals("RedirectUrl", result.deconstructor().getStringByName(REDIRECT_LOCATION_DS_NAME).get());
	}
	
	@Test
	void testAccept_ReturnTooMany() throws Exception {
		final String scenarioName = "ReturnTooMany";
		final String expectedTemplateUrl = "sampleTemplateUrl";
		final String expectedContentRoot = "sampleContentRoot";
		final String expectedSubmitUrl = "sampleSubmitUrl";
		DataSourceList result = underTest.accept(InputDslBuilder.createBuilder(scenarioName)
				.template(expectedTemplateUrl)
				.contentRoot(expectedContentRoot)
				.submitUrl(expectedSubmitUrl)
				.build()
				);
		assertNotNull(result);
		assertEquals(2, result.list().size());
		final Deconstructor deconstructor = result.deconstructor();
		String output = deconstructor.getStringByName("Result").get();
		assertAll(
				()->assertTrue(output.contains(expectedTemplateUrl), "Expected '" + output + "' to contain template url '" + expectedTemplateUrl + "'."),
				()->assertTrue(output.contains(expectedContentRoot), "Expected '" + output + "' to contain content root '" + expectedContentRoot + "'."),
				()->assertTrue(output.contains(expectedSubmitUrl), "Expected '" + output + "' to contain submit url '" + expectedSubmitUrl + "'.")
				);
		DataSource pdfDataSource = deconstructor.getDataSourceByName("PdfResult").get();
		assertAll(
				()->assertEquals(StandardMimeTypes.APPLICATION_PDF_TYPE, pdfDataSource.contentType()),
				()->assertEquals("SampleForm.pdf", pdfDataSource.filename().get().getFileName().toString()),
				()->assertEquals("PdfResult", pdfDataSource.name()),
				()->assertEquals(1, pdfDataSource.attributes().size()),	// We add a content disposition for this test.
				()->assertEquals("attachment", pdfDataSource.attributes().get("formsfeeder:Content-Disposition"))
				);
	}

	@Test
	void testScenario_BadRequestException() {
		final String scenarioName = "BadRequestException";
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(InputDslBuilder.build(scenarioName)));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains("because scenario was '" + scenarioName + "'"), "Expected msg to contain \"because scenario was '" + scenarioName + "'\" but didn't (" + msg + ").");
	}
	
	@Test
	void testScenario_InternalErrorException() {
		final String scenarioName = "InternalErrorException";
		FeedConsumerInternalErrorException ex = assertThrows(FeedConsumerInternalErrorException.class, ()->underTest.accept(InputDslBuilder.build(scenarioName)));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}
	
	@Test
	void testScenario_UncheckedException() {
		final String scenarioName = "UncheckedException";
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->underTest.accept(InputDslBuilder.build(scenarioName)));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}
	
	@Test
	void testScenario_OtherFeedConsumerException() {
		final String scenarioName = "OtherFeedConsumerException";
		FeedConsumerException ex = assertThrows(FeedConsumerException.class, ()->underTest.accept(InputDslBuilder.build(scenarioName)));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
		assertTrue(msg.contains("Throwing anonymous FeedConsumerException"), "Expected msg to contain 'Throwing anonymous FeedConsumerException' but didn't (" + msg + ").");
	}
	
	@Test
	void testScenario_UnknownScenarioName() {
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(InputDslBuilder.build("")));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertEquals("No scenario name was provided.", msg);
	}

	@Test
	void testScenario_BadScenario() {
		final String scenarioName = "FooBarScenario";
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(InputDslBuilder.build(scenarioName)));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains("Unexpected scenario"), "Expected msg to contain 'Unexpected scenario' but didn't (" + msg + ").");
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}

	@Test
	void testName() {
		assertEquals(FEED_CONSUMER_NAME, underTest.name());
	}

	private static class InputDslBuilder {
		private static final String SUBMITTED_DATA_DS_NAME = FORMSFEEDER_PREFIX + "SubmittedData";
		private static final String TEMPLATE_DS_NAME = FORMSFEEDER_PREFIX + "Template";
		private static final String CONTENT_ROOT_DS_NAME = FORMSFEEDER_PREFIX + "ContentRoot";
		private static final String SUBMIT_URL_DS_NAME = FORMSFEEDER_PREFIX + "SubmitUrl";

		private final DataSourceList.Builder builder = DataSourceList.builder();
		
		private InputDslBuilder(String scenario) {
			super();
			String xmlData = "<form1><TextField1>Text Field 1 Data</TextField1><TextField2>Text Field 2 Data</TextField2><FormsFeeder><Plugin>MockSubmit</Plugin></FormsFeeder><Scenario>" + scenario + "</Scenario></form1>";
			builder.add(SUBMITTED_DATA_DS_NAME, xmlData.getBytes(StandardCharsets.UTF_8), StandardMimeTypes.APPLICATION_XML_TYPE);
		}

		public static InputDslBuilder createBuilder(String scenario) {
			return new InputDslBuilder(scenario);
		}
		
		public DataSourceList build() {
			return builder.build();
		}
		
		/**
		 * Build a DataSourceList with just the minimum (i.e. just the data file).
		 * 
		 * @param scenario
		 * @return
		 */
		public static DataSourceList build(String scenario) {
			return InputDslBuilder.createBuilder(scenario).build();
		}
		
		/**
		 * Build a DataSourceList with form data and other stuff added via the addFn 
		 * 
		 * @param scenario
		 * @param addFn
		 * @return
		 */
		public static DataSourceList build(String scenario, Function<InputDslBuilder, InputDslBuilder> addFn) {
			return addFn.apply(InputDslBuilder.createBuilder(scenario)).build();
		}

		public InputDslBuilder template(String template) {
			builder.add(TEMPLATE_DS_NAME, template);
			return this;
		}

		public InputDslBuilder contentRoot(String contentRoot) {
			builder.add(CONTENT_ROOT_DS_NAME, contentRoot);
			return this;
		}

		public InputDslBuilder submitUrl(String submitUrl) {
			builder.add(SUBMIT_URL_DS_NAME, submitUrl);
			return this;
		}
	}
}
