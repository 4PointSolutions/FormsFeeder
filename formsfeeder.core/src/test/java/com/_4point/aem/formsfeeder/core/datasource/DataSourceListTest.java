package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class DataSourceListTest {
	
	private static final String FIRST_NAME = "FirstName";
	private static final String SECOND_NAME = "SecondName";
	private static final String THIRD_NAME = "ThirdName";
	private static final StringDataSource DS1 = new StringDataSource("FirstEntry", FIRST_NAME);
	private static final StringDataSource DS2 = new StringDataSource("SecondEntry", SECOND_NAME, Map.of("attributeName1", "attributeValue1"));
	private static final StringDataSource DS3 = new StringDataSource("ThirdEntry", SECOND_NAME);	// Make sure we have a duplicate entry
	private static final StringDataSource DS4 = new StringDataSource("FourthEntry", THIRD_NAME);

	private static final Predicate<DataSource> emptyAttributes = (ds)->ds.attributes().isEmpty();
	private static final Predicate<DataSource> hasFilename = (ds)->ds.filename().isPresent();

	private final List<DataSource> srcList = List.of(
										DS1,
										DS2,
										DS3,
										DS4
									   );

	private final DataSourceList underTest = DataSourceList.from(srcList);

	@Test
	void testList() {
		List<DataSource> resultList = underTest.list();
		assertEquals(srcList.size(), resultList.size());
		for(int i = 0; i < srcList.size(); i++) {
			assertSame(srcList.get(i), resultList.get(i));
		}
	}

	@Test
	void testGetDataSourceByName() {
		assertAll(
			()->assertSame(DS1, underTest.getDataSourceByName(FIRST_NAME).get()),
			()->assertSame(DS2, underTest.getDataSourceByName(SECOND_NAME).get()),
			()->assertSame(DS4, underTest.getDataSourceByName(THIRD_NAME).get()),
			()->assertTrue(underTest.getDataSourceByName("SomethingNotThere").isEmpty())
		);
	}

	@Test
	void testGetDataSource() {
		assertAll(
				// DS1 has no attributes
				()->assertSame(DS1, underTest.getDataSource(emptyAttributes).get()),
				// DS2 has attributes
				()->assertSame(DS2, underTest.getDataSource(Predicate.not(emptyAttributes)).get()),
				// Should be no DataSources with a filename, so this query should return an Optional.empty() result. 
				()->assertTrue(underTest.getDataSource(hasFilename).isEmpty())
				);
	}

	@Test
	void testGetDataSourcesByName() {
		assertAll(
				()->assertIterableEquals(List.of(DS1), underTest.getDataSourcesByName(FIRST_NAME)),
				()->assertIterableEquals(List.of(DS2, DS3), underTest.getDataSourcesByName(SECOND_NAME)),
				()->assertIterableEquals(List.of(DS4), underTest.getDataSourcesByName(THIRD_NAME)),
				()->assertTrue(underTest.getDataSourcesByName("SomethingNotThere").isEmpty())
				);
	}

	@Test
	void testGetDataSources() {
		assertAll(
				// DS1, DS3, and DS4 hava no attributes
				()->assertIterableEquals(List.of(DS1, DS3, DS4), underTest.getDataSources(emptyAttributes)),
				// DS2 has attributes
				()->assertIterableEquals(List.of(DS2), underTest.getDataSources(Predicate.not(emptyAttributes))),
				// Should be no DataSources with a filename, so this query should return an Optional.empty() result. 
				()->assertTrue(underTest.getDataSources(hasFilename).isEmpty())
				);
	}

	@Test
	void testEmptyList() {
		DataSourceList emptyList = DataSourceList.emptyList();
		assertTrue(emptyList.isEmpty());
		assertTrue(emptyList.list().isEmpty());
		assertSame(emptyList, DataSourceList.emptyList());	// Test that we always get the same EmptyList back
		assertSame(emptyList, DataSourceList.from(Collections.emptyList()));	// Test that we always get the same EmptyList back
		
		assertFalse(underTest.isEmpty());	// Test that a non-empty list returns not empty.
	}
}
