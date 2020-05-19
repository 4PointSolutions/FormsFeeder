package com._4point.aem.formsfeeder.plugins.mock;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
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
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.plugins.mock.MockPlugin.MockExtension;

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
		MockExtension underTest2 = new MockPlugin.MockExtension();
		underTest2.accept(getMockEnvironment(expectedConfigValue));
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

	@Test
	void testReturnConfigValueUsingApplicationContext() throws Exception {
		final String expectedConfigValue = "UnitTestValue";
		final String scenarioName = "ReturnApplicationContextConfigValue";
		MockExtension underTest2 = new MockPlugin.MockExtension();
		underTest2.accept(getApplicationContext(expectedConfigValue));
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

	private ApplicationContext getApplicationContext(String expectedConfigValue) {
		return new ApplicationContext() {
			
			@Override
			public Resource getResource(String location) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public ClassLoader getClassLoader() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public Resource[] getResources(String locationPattern) throws IOException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public void publishEvent(Object event) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public BeanFactory getParentBeanFactory() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean containsLocalBean(String name) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public Object getBean(String name, Object... args) throws BeansException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
				throw new UnsupportedOperationException("Not implmented.");
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
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getAliases(String name) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean containsBean(String name) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
					throws BeansException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
					throws BeansException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getBeanNamesForType(Class<?> type) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getBeanNamesForType(ResolvableType type) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String[] getBeanDefinitionNames() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public int getBeanDefinitionCount() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
					throws NoSuchBeanDefinitionException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public boolean containsBeanDefinition(String beanName) {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public Environment getEnvironment() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public long getStartupDate() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public ApplicationContext getParent() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getId() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getDisplayName() {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getApplicationName() {
				throw new UnsupportedOperationException("Not implmented.");
			}
		};
	}

	private static Environment getMockEnvironment(String expectedConfigValue) {
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
				throw new UnsupportedOperationException("Not implmented.");
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
				throw new UnsupportedOperationException("Not implmented.");
			}
			
			@Override
			public String getProperty(String key) {
				assertEquals("formsfeeder.plugins.mock.configValue", key, "Expected the configuration key to be \"formsfeeder.plugins.mock.configValue\"");
				return expectedConfigValue;
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
	
	private static DataSourceList.Builder createBuilder(String scenario) {
		return DataSourceList.builder()
					  		 .add("scenario", scenario);
	}
}
