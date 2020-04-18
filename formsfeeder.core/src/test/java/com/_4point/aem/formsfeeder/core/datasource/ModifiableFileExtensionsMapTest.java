package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ModifiableFileExtensionsMapTest {

	private enum TestScenario {
		NOARGS_CONSTRUCTOR(ModifiableFileExtensionsMap::new, "No Arguments Constructor"),
		EXISTING_MAP_CONSTRUCTOR(()->new ModifiableFileExtensionsMap(UnmodifiableFileExtensionsMap.DEFAULT_MAP), "Existng Map Constructor")
		;
		
		private final Supplier<ModifiableFileExtensionsMap> mapSupplier;
		private final String testName;
		private TestScenario(Supplier<ModifiableFileExtensionsMap> mapSupplier, String testName) {
			this.mapSupplier = mapSupplier;
			this.testName = testName;
		}
		public final Supplier<ModifiableFileExtensionsMap> getMapSupplier() {
			return mapSupplier;
		}
		public final String getTestName() {
			return testName;
		}
	}

	@ParameterizedTest
	@EnumSource
	void testAddMapping(TestScenario scenario) {
		ModifiableFileExtensionsMap underTest = scenario.getMapSupplier().get();
		MimeType testMimeType = MimeType.of("application", "x-totally-fake-mime-type");
		String testExtension1 = "xxx";
		String testExtension2 = "xx1";
		underTest.putMapping(testMimeType, List.of(testExtension1, testExtension2));
		assertAll(
				()->assertEquals(testMimeType, underTest.mimeType(Path.of("TestFile." + testExtension1 )).get(), "Extension '" + testExtension1 + "didn't return '" + testMimeType + "'."),
				()->assertEquals(testMimeType, underTest.mimeType(Path.of("TestFile." + testExtension2 )).get(), "Extension '" + testExtension2 + "didn't return '" + testMimeType + "'."),
				()->assertEquals(testExtension1, underTest.fileDefaultExtension(testMimeType).get(), "TestMimeType '" + testMimeType + "didn't return extension of '" + testExtension1 + "'.")
				);
	}

	@Test
	void testReplaceMapping_newExtensions() {
		ModifiableFileExtensionsMap underTest = TestScenario.EXISTING_MAP_CONSTRUCTOR.getMapSupplier().get();
		MimeType testMimeType = MimeType.of("application", "pdf");
		String testExtension1 = "xxx";
		String testExtension2 = "xx1";
		underTest.putMapping(testMimeType, List.of(testExtension1, testExtension2));
		assertAll(
				()->assertEquals(testMimeType, underTest.mimeType(Path.of("TestFile." + testExtension1 )).get(), "Extension '" + testExtension1 + "didn't return '" + testMimeType + "'."),
				()->assertEquals(testMimeType, underTest.mimeType(Path.of("TestFile." + testExtension2 )).get(), "Extension '" + testExtension2 + "didn't return '" + testMimeType + "'."),
				()->assertEquals(testExtension1, underTest.fileDefaultExtension(testMimeType).get(), "TestMimeType '" + testMimeType + "didn't return extension of '" + testExtension1 + "'.")
				);
	}

	@Test
	void testReplaceMapping_existingExtensions() {
		ModifiableFileExtensionsMap underTest = TestScenario.EXISTING_MAP_CONSTRUCTOR.getMapSupplier().get();
		MimeType testMimeType = MimeType.of("application", "pdf");
		String testExtension1 = "pdf";
		String testExtension2 = "xx1";
		underTest.putMapping(testMimeType, List.of(testExtension1, testExtension2));
		assertAll(
				()->assertEquals(testMimeType, underTest.mimeType(Path.of("TestFile." + testExtension1 )).get(), "Extension '" + testExtension1 + "didn't return '" + testMimeType + "'."),
				()->assertEquals(testMimeType, underTest.mimeType(Path.of("TestFile." + testExtension2 )).get(), "Extension '" + testExtension2 + "didn't return '" + testMimeType + "'."),
				()->assertEquals(testExtension1, underTest.fileDefaultExtension(testMimeType).get(), "TestMimeType '" + testMimeType + "didn't return extension of '" + testExtension1 + "'.")
				);
	}

	@ParameterizedTest
	@EnumSource
	void testAddMappingInvalid_EmptyList(TestScenario scenario) {
		ModifiableFileExtensionsMap underTest = scenario.getMapSupplier().get();
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.putMapping(MimeType.of("text", "javascript"), Collections.emptyList()));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains("empty"), "Expected the exception message to contain 'empty'. (" + msg + ")." );
	}

	@ParameterizedTest
	@EnumSource
	void testAddMappingInvalid_NullList(TestScenario scenario) {
		ModifiableFileExtensionsMap underTest = scenario.getMapSupplier().get();
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.putMapping(MimeType.of("text", "javascript"), null));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains("empty"), "Expected the exception message to contain 'empty'. (" + msg + ")." );
	}

	@Test
	void testAddMappingInvalid_DuplicateExtensionEntry() {
		ModifiableFileExtensionsMap underTest = TestScenario.EXISTING_MAP_CONSTRUCTOR.getMapSupplier().get();
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.putMapping(MimeType.of("text", "javascript"), List.of("pdf")));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains("duplicate"), "Expected the exception message to contain 'duplicate'. (" + msg + ")." );
	}
}
