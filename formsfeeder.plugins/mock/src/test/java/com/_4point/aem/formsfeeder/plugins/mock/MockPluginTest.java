package com._4point.aem.formsfeeder.plugins.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerBadRequestException;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.plugins.mock.MockPlugin.MockExtension;

class MockPluginTest {

	MockExtension underTest = new MockPlugin.MockExtension();
	
	@Test
	void testScenario_BadScenario() {
		final String scenarioName = "FooBarScenario";
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(createBuilder(scenarioName).build()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains("Unexpected scenario"), "Expected msg to contain 'Unexpected scenario' but didn't (" + msg + ").");
		assertTrue(msg.contains("Unexpected scenario"), "Expected msg to contain '" + scenarioName + "' but didn't (" + msg + ").");
	}

	@Test
	void testScenario_Unknown() {
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(DataSourceList.emptyList()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertEquals("No scenario name was provided.", msg);
	}

	private static DataSourceList.Builder createBuilder(String scenario) {
		return DataSourceList.builder()
					  		 .add("scenario", scenario);
	}
}
