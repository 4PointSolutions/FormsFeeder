package com._4point.aem.formsfeeder.plugins.example;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import com._4point.aem.fluentforms.impl.SimpleDocumentFactoryImpl;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerBadRequestException;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.plugins.example.ExamplePlugin.ExampleFeedConsumerExtension;
import com._4point.aem.formsfeeder.plugins.example.ExamplePlugin.ExampleFeedConsumerExtension.ExampleParameters;

class ExamplePluginTest {
	private static final Path SAMPLE_FILES = Paths.get("src", "test", "resources", "SampleForms");
	private static final Path SAMPLE_XDP = SAMPLE_FILES.resolve("SampleForm.xdp");
	private static final Path SAMPLE_DATA = SAMPLE_FILES.resolve("SampleForm_data.xml");

	private static final String AEM_HOST_NAME_ENV = "formsfeeder.plugins.example.aem-host"; 
	private static final String AEM_PORT_NAME_ENV = "formsfeeder.plugins.example.aem-port"; 
	private static final String AEM_PORT_DEFAULT_VALUE = "4502";

	private static final String TEMPLATE_PARAM_NAME = "template";
	private static final String DATA_PARAM_NAME = "data";
	private static final String INTERACTIVE_PARAM_NAME = "interactive";

	private ExamplePlugin.ExampleFeedConsumerExtension underTest = new ExamplePlugin.ExampleFeedConsumerExtension();;

	@Disabled
	void testAccept() throws Exception {
		final String expectedAemHostName = "aemHost";
		final String expectedAemPortNum = "55555";
		DataSourceList testData = DataSourceList.builder()
												.add(TEMPLATE_PARAM_NAME, SAMPLE_XDP)
												.add(DATA_PARAM_NAME, SAMPLE_DATA)
												.add(INTERACTIVE_PARAM_NAME, "true")
												.build();
		underTest.accept(getMockEnvironment(expectedAemHostName, expectedAemPortNum));	// Set up the environment
		DataSourceList result = underTest.accept(testData);
		assertNotNull(result);
		assertFalse(result.isEmpty());
	}

	@Test
	void testName() {
		assertEquals("Example", underTest .name());
	}

	@ParameterizedTest
	@ValueSource ( strings = {"true", "false"})
	void testExampleParametersFrom(boolean interactiveFlag) throws Exception {
		final byte[] expectedTemplateData = "Expected Tempate Data".getBytes();  
		final byte[] expectedDataData = "Expected Data Data".getBytes();
		DataSourceList dataSourceList = DataSourceList.builder()
													  .add(TEMPLATE_PARAM_NAME, expectedTemplateData)
													  .add(DATA_PARAM_NAME, expectedDataData)
													  .add(INTERACTIVE_PARAM_NAME, interactiveFlag)
													  .build();
		
		ExampleParameters result = ExampleFeedConsumerExtension.ExampleParameters.from(dataSourceList, SimpleDocumentFactoryImpl.getFactory());
		assertArrayEquals(expectedTemplateData, result.getTemplate().getInlineData());
		assertArrayEquals(expectedDataData, result.getData().getInlineData());
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
		
		private DataSourceList getDsl() {
			final byte[] expectedTemplateData = "Expected Tempate Data".getBytes();  
			final byte[] expectedDataData = "Expected Data Data".getBytes();
			Builder builder = DataSourceList.builder();
			if (!templateMissing) {
				builder.add(TEMPLATE_PARAM_NAME, expectedTemplateData);
			}
			if (!dataMissing) {
				builder.add(DATA_PARAM_NAME, expectedDataData);
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
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->ExampleFeedConsumerExtension.ExampleParameters.from(scenario.getDsl(), SimpleDocumentFactoryImpl.getFactory()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertAll(
				()->assertTrue(msg.contains(scenario.missingParamName()), "Expected message '" + msg + "' to contain missing parameter name '" + scenario.missingParamName() + "'."),
				()->assertTrue(msg.contains("Parameter must be supplied"), "Expected message '" + msg + "' to contain text 'Parameter must be supplied'.")
				);
	}
	
	private static Environment getMockEnvironment(String aemHostName, String aemHostPort) {
		return new Environment() {
			
			@Override
			public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String resolvePlaceholders(String text) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getRequiredProperty(String key) throws IllegalStateException {
				assertEquals(AEM_HOST_NAME_ENV, key);
				return aemHostName;
			}
			
			@Override
			public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> T getProperty(String key, Class<T> targetType) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getProperty(String key, String defaultValue) {
				assertEquals(AEM_PORT_NAME_ENV, key);
				assertEquals(AEM_PORT_DEFAULT_VALUE, defaultValue);
				return aemHostPort;
			}
			
			@Override
			public String getProperty(String key) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean containsProperty(String key) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getDefaultProfiles() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getActiveProfiles() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean acceptsProfiles(Profiles profiles) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean acceptsProfiles(String... profiles) {
				throw new UnsupportedOperationException("Not implmented.");
			}
		};
	}

}
