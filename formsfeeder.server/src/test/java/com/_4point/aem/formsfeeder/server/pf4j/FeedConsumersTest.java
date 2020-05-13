package com._4point.aem.formsfeeder.server.pf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.pf4j.SpringPluginManager;

class FeedConsumersTest {
	
	private MockSpringPluginManager mockSpringPluginManager = new MockSpringPluginManager();	// Must occur before FeedConsumer construction.
	private FeedConsumers underTest = constructFeedConsumers();

	@Test
	void testConsumerThatExists() {
		// Call it several times however, it should only call MockSpringPluginManager once
		assertTrue(underTest.consumer("Mock").isPresent());
		assertTrue(underTest.consumer("Mock").isPresent());
		assertTrue(underTest.consumer("Mock").isPresent());
		assertEquals(1, mockSpringPluginManager.timesCalled());
	}

	@Test
	void testConsumerThatDoesntExist() {
		// Call it several times however, it should only call MockSpringPluginManager once
		assertTrue(underTest.consumer("NotMock").isEmpty());
		assertTrue(underTest.consumer("NotMock").isEmpty());
		assertTrue(underTest.consumer("NotMock").isEmpty());
		assertEquals(1, mockSpringPluginManager.timesCalled());
	}


	private FeedConsumers constructFeedConsumers() {
		FeedConsumers feedConsumers = new FeedConsumers();
		// Emulate the injection that Spring Boot is doing
		try {
			junitx.util.PrivateAccessor.setField(feedConsumers, "springPluginManager", mockSpringPluginManager);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			return null;
		}
		return feedConsumers;
	}
	
	private static class MockSpringPluginManager extends SpringPluginManager {
		private int timesCalled = 0;

		@Override
		public <T> List<T> getExtensions(Class<T> type) {
			timesCalled++;
			return List.of((T)new MockNamedFeedConsumer());
		}

		public final int timesCalled() {
			return timesCalled;
		}
	}
	
	private static class MockNamedFeedConsumer implements NamedFeedConsumer {

		@Override
		public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String name() {
			return "Mock";
		}
		
	}
}
