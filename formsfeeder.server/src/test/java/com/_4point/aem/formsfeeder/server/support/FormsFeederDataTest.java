package com._4point.aem.formsfeeder.server.support;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FormsFeederDataTest {

	
	@ParameterizedTest
	@ValueSource(strings = {"PluginName", "   PluginName", "PluginName   ", "   PluginName  "})
	void testPluginName(String expectedPluginValue) {
		String xml = "<foo><bar><FormsFeeder><Plugin>" + expectedPluginValue + "</Plugin></FormsFeeder></bar></foo>";
		Optional<String> result = FormsFeederData.from(xml.getBytes(StandardCharsets.UTF_8)).get().pluginName();
		assertTrue(result.isPresent());
		assertEquals("PluginName", result.get());
	}

	@Test
	void testPluginName_NoPluginNameElement() {
		String xml = "<foo><bar><FormsFeeder></FormsFeeder></bar></foo>";
		Optional<String> result = FormsFeederData.from(xml.getBytes(StandardCharsets.UTF_8)).get().pluginName();
		assertTrue(result.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "  "})
	void testPluginName_EmptyPluginNameElement(String pluginValue) {
		String xml = "<foo><bar><FormsFeeder><Plugin>" + pluginValue + "</Plugin></FormsFeeder></bar></foo>";
		Optional<String> result = FormsFeederData.from(xml.getBytes(StandardCharsets.UTF_8)).get().pluginName();
		assertTrue(result.isEmpty());
	}

	@Test
	void testNoFormsFeederElement() {
		String xml = "<foo><bar/></foo>";
		Optional<FormsFeederData> result = FormsFeederData.from(xml.getBytes(StandardCharsets.UTF_8));
		assertTrue(result.isEmpty());
	}

	@Test
	void testPluginName_BadXml() {
		String xml = "<foo><bar><FormsFeeder></FormsFeeder><bar></foo>";
		assertThrows(FormsFeederData.FormsFeederDataException.class, ()->FormsFeederData.from(xml.getBytes(StandardCharsets.UTF_8)).get().pluginName());
	}


}
