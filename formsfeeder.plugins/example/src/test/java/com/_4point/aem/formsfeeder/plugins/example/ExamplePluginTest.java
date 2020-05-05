package com._4point.aem.formsfeeder.plugins.example;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ExamplePluginTest {

	private ExamplePlugin.ExampleFeedConsumerExtension underTest = new ExamplePlugin.ExampleFeedConsumerExtension();;

	@Disabled
	void testAccept() {
		fail("Not yet implemented");
	}

	@Test
	void testName() {
		assertEquals("Example", underTest .name());
	}

}
