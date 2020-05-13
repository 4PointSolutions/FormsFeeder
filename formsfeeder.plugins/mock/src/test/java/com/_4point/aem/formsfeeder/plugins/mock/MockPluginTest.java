package com._4point.aem.formsfeeder.plugins.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerBadRequestException;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerException;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerInternalErrorException;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.plugins.mock.MockPlugin.MockExtension;

import junitx.util.PrivateAccessor;

class MockPluginTest {

	MockExtension underTest = new MockPlugin.MockExtension();

	@Test
	void testScenario_BadRequestException() {
		final String scenarioName = "BadRequestException";
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(createBuilder(scenarioName).build()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}
	
	@Test
	void testScenario_InternalErrorException() {
		final String scenarioName = "InternalErrorException";
		FeedConsumerInternalErrorException ex = assertThrows(FeedConsumerInternalErrorException.class, ()->underTest.accept(createBuilder(scenarioName).build()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}
	
	@Test
	void testScenario_UncheckedException() {
		final String scenarioName = "UncheckedException";
		IllegalStateException ex = assertThrows(IllegalStateException.class, ()->underTest.accept(createBuilder(scenarioName).build()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}
	
	@Test
	void testScenario_OtherFeedConsumerException() {
		final String scenarioName = "OtherFeedConsumerException";
		FeedConsumerException ex = assertThrows(FeedConsumerException.class, ()->underTest.accept(createBuilder(scenarioName).build()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}
	
	@Test
	void testScenario_ReturnPdf() throws Exception {
		final String scenarioName = "ReturnPdf";
		DataSourceList result = underTest.accept(createBuilder(scenarioName).build());
		assertNotNull(result);
		assertEquals(1, result.list().size());
		DataSource pdfDataSource = result.list().get(0);
		assertAll(
				()->assertEquals(StandardMimeTypes.APPLICATION_PDF_TYPE, pdfDataSource.contentType()),
				()->assertEquals("SampleForm.pdf", pdfDataSource.filename().get().getFileName().toString()),
				()->assertEquals("PdfResult", pdfDataSource.name())
				);
	}
	
	@Test
	void testScenario_ReturnXml() throws Exception {
		final String scenarioName = "ReturnXml";
		DataSourceList result = underTest.accept(createBuilder(scenarioName).build());
		assertNotNull(result);
		assertEquals(1, result.list().size());
		DataSource xmlDataSource = result.list().get(0);
		assertAll(
				()->assertEquals(StandardMimeTypes.APPLICATION_XML_TYPE, xmlDataSource.contentType()),
				()->assertEquals("SampleForm_data.xml", xmlDataSource.filename().get().getFileName().toString()),
				()->assertEquals("XmlResult", xmlDataSource.name())
				);
	}
	
	@Test
	void testScenario_ReturnPdfAndXml() throws Exception {
		final String scenarioName = "ReturnPdfAndXml";
		DataSourceList result = underTest.accept(createBuilder(scenarioName).build());
		assertNotNull(result);
		assertEquals(2, result.list().size());
		DataSource pdfDataSource = result.list().get(0);
		DataSource xmlDataSource = result.list().get(1);
		assertAll(
				()->assertEquals(StandardMimeTypes.APPLICATION_PDF_TYPE, pdfDataSource.contentType()),
				()->assertEquals("SampleForm.pdf", pdfDataSource.filename().get().getFileName().toString()),
				()->assertEquals("PdfResult", pdfDataSource.name()),
				()->assertEquals(StandardMimeTypes.APPLICATION_XML_TYPE, xmlDataSource.contentType()),
				()->assertEquals("SampleForm_data.xml", xmlDataSource.filename().get().getFileName().toString()),
				()->assertEquals("XmlResult", xmlDataSource.name())
				);
	}
	
	@Test
	void testScenario_BadScenario() {
		final String scenarioName = "FooBarScenario";
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(createBuilder(scenarioName).build()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains("Unexpected scenario"), "Expected msg to contain 'Unexpected scenario' but didn't (" + msg + ").");
		assertTrue(msg.contains(scenarioName), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}

	@Test
	void testScenario_UnknownScenarioName() {
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(DataSourceList.emptyList()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertEquals("No scenario name was provided.", msg);
	}

	@Test
	void testReturnConfigValue() throws Exception {
		final String expectedConfigValue = "UnitTestValue";
		final String scenarioName = "ReturnConfigValue";
		final MockPluginProperties properties = new MockPluginProperties();
		PrivateAccessor.setField(properties, "configValue", expectedConfigValue);
		MockExtension underTest2 = new MockPlugin.MockExtension();
		underTest2.setMockProperties(properties);
		DataSourceList result = underTest2.accept(createBuilder(scenarioName).build());
		assertNotNull(result);
		assertEquals(1, result.list().size());
		DataSource returnedDataSource = result.list().get(0);
		assertAll(
				()->assertEquals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE, returnedDataSource.contentType()),
				()->assertEquals("ConfigValue", returnedDataSource.name()),
				()->assertEquals(expectedConfigValue, result.deconstructor().getStringByName("ConfigValue").get())
				);
	}
	
	private static DataSourceList.Builder createBuilder(String scenario) {
		return DataSourceList.builder()
					  		 .add("scenario", scenario);
	}
}
