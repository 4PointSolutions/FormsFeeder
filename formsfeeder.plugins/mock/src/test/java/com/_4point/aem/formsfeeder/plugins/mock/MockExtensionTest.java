package com._4point.aem.formsfeeder.plugins.mock;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;

import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerBadRequestException;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerException;
import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerInternalErrorException;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;

class MockExtensionTest {

	MockExtension underTest = new MockExtension();

	@Test
	void testScenario_BadRequestException() {
		final String scenarioName = "BadRequestException";
		FeedConsumerBadRequestException ex = assertThrows(FeedConsumerBadRequestException.class, ()->underTest.accept(createBuilder(scenarioName).build()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains("because scenario was '" + scenarioName + "'"), "Expected msg to contain \"because scenario was '" + scenarioName + "'\" but didn't (" + msg + ").");
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
		assertTrue(msg.contains("Throwing anonymous FeedConsumerException"), "Expected msg to contain 'Throwing anonymous FeedConsumerException' but didn't (" + msg + ").");
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
				()->assertEquals("PdfResult", pdfDataSource.name()),
				()->assertEquals(1, pdfDataSource.attributes().size()),	// We add a content disposition for this test.
				()->assertEquals("attachment", pdfDataSource.attributes().get("formsfeeder:Content-Disposition"))
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
	void testScenario_ReturnManyOutputs() throws Exception {
		final String scenarioName = "ReturnManyOutputs";
		DataSourceList result = underTest.accept(createBuilder(scenarioName).build());
		assertNotNull(result);
		assertEquals(3, result.list().size());
		DataSource pdfDataSource = result.list().get(0);
		DataSource xmlDataSource = result.list().get(1);
		DataSource baDataSource = result.list().get(2);
		assertAll(
				()->assertEquals(StandardMimeTypes.APPLICATION_PDF_TYPE, pdfDataSource.contentType()),
				()->assertEquals("SampleForm.pdf", pdfDataSource.filename().get().getFileName().toString()),
				()->assertEquals("PdfResult", pdfDataSource.name()),
				()->assertEquals(StandardMimeTypes.APPLICATION_XML_TYPE, xmlDataSource.contentType()),
				()->assertEquals("SampleForm_data.xml", xmlDataSource.filename().get().getFileName().toString()),
				()->assertEquals("XmlResult", xmlDataSource.name()),
				()->assertEquals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE, baDataSource.contentType()),
				()->assertTrue(baDataSource.filename().isEmpty()),
				()->assertEquals("ByteArrayResult", baDataSource.name()),
				()->assertArrayEquals("SampleData".getBytes(StandardCharsets.UTF_8), baDataSource.inputStream().readAllBytes())
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
		underTest.accept(getMockEnvironment(expectedConfigValue));
		DataSourceList result = underTest.accept(createBuilder(scenarioName).build());
		assertNotNull(result);
		assertEquals(1, result.list().size());
		DataSource returnedDataSource = result.list().get(0);
		assertAll(
				()->assertEquals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE, returnedDataSource.contentType()),
				()->assertEquals("ConfigValue", returnedDataSource.name()),
				()->assertEquals(expectedConfigValue, result.deconstructor().getStringByName("ConfigValue").get())
				);
	}

	@Test
	void testReturnConfigValueUsingApplicationContext() throws Exception {
		final String expectedConfigValue = "UnitTestValue";
		final String scenarioName = "ReturnApplicationContextConfigValue";
		underTest.accept(getMockApplicationContext(expectedConfigValue));
		DataSourceList result = underTest.accept(createBuilder(scenarioName).build());
		assertNotNull(result);
		assertEquals(1, result.list().size());
		DataSource returnedDataSource = result.list().get(0);
		final String expectedConfigName = "ConfigValue";
		assertAll(
				()->assertEquals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE, returnedDataSource.contentType()),
				()->assertEquals(expectedConfigName, returnedDataSource.name()),
				()->assertEquals(expectedConfigValue, result.deconstructor().getStringByName(expectedConfigName).get())
				);
	}

	@Test
	void testCallAnotherPlugin() throws Exception {
		final String expectedReturnedName = "UnitTestName";
		final String expectedReturnedValue = "UnitTestValue";
		final String scenarioName = "CallAnotherPlugin";
		underTest.accept(getMockConsumers());
		DataSourceList result = underTest.accept(createBuilder(scenarioName).add(expectedReturnedName, expectedReturnedValue).build());
		assertNotNull(result);
		assertEquals(1, result.list().size());
		DataSource returnedDataSource = result.list().get(0);
		assertAll(
				()->assertEquals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE, returnedDataSource.contentType()),
				()->assertEquals(expectedReturnedName, returnedDataSource.name()),
				()->assertEquals(expectedReturnedValue, result.deconstructor().getStringByName(expectedReturnedName).get())
				);
	}

	private List<NamedFeedConsumer> getMockConsumers() {
		NamedFeedConsumer mockDebugPlugin = new NamedFeedConsumer() {
			
			@Override
			public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
				return dataSources;
			}
			
			@Override
			public String name() {
				return "Debug";
			}
		};
		NamedFeedConsumer mockOtherPlugin = new NamedFeedConsumer() {
			
			@Override
			public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
				return dataSources;
			}
			
			@Override
			public String name() {
				return "Other";
			}
		};
		return List.of(mockOtherPlugin, mockDebugPlugin, mockOtherPlugin);
	}

	private ApplicationContext getMockApplicationContext(String expectedConfigValue) {
		return new ApplicationContext() {
			
			@Override
			public Resource getResource(String location) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public ClassLoader getClassLoader() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public Resource[] getResources(String locationPattern) throws IOException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public void publishEvent(Object event) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public BeanFactory getParentBeanFactory() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean containsLocalBean(String name) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public Object getBean(String name, Object... args) throws BeansException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getBean(Class<T> requiredType) throws BeansException {
				if (requiredType.equals(Environment.class)) {
					return (T) getMockEnvironment(expectedConfigValue);
				}
				return null;
			}
			
			@Override
			public Object getBean(String name) throws BeansException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getAliases(String name) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean containsBean(String name) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
					throws BeansException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
					throws BeansException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getBeanNamesForType(Class<?> type) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getBeanNamesForType(ResolvableType type) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getBeanDefinitionNames() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public int getBeanDefinitionCount() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
					throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean containsBeanDefinition(String beanName) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public Environment getEnvironment() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public long getStartupDate() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public ApplicationContext getParent() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getId() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getDisplayName() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getApplicationName() {
				throw new UnsupportedOperationException("Not implemented.");
			}

			@Override
			public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
				throw new UnsupportedOperationException("Not implemented.");
			}

			@Override
			public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
				throw new UnsupportedOperationException("Not implemented.");
			}

			@Override
			public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType,
					boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implemented.");
			}
		};
	}

	private static Environment getMockEnvironment(String expectedConfigValue) {
		return new Environment() {
			
			@Override
			public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String resolvePlaceholders(String text) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getRequiredProperty(String key) throws IllegalStateException {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public <T> T getProperty(String key, Class<T> targetType) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getProperty(String key, String defaultValue) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String getProperty(String key) {
				assertEquals("formsfeeder.plugins.mock.configValue", key, "Expected the configuration key to be \"formsfeeder.plugins.mock.configValue\"");
				return expectedConfigValue;
			}
			
			@Override
			public boolean containsProperty(String key) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getDefaultProfiles() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public String[] getActiveProfiles() {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean acceptsProfiles(Profiles profiles) {
				throw new UnsupportedOperationException("Not implemented.");
			}
			
			@Override
			public boolean acceptsProfiles(String... profiles) {
				throw new UnsupportedOperationException("Not implemented.");
			}
		};
	}
	
	private static DataSourceList.Builder createBuilder(String scenario) {
		return DataSourceList.builder()
					  		 .add("scenario", scenario);
	}
}
