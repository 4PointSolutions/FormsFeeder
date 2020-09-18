package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

class DataSourceListTest {
	
	private static final String FIRST_NAME = "FirstName";
	private static final String SECOND_NAME = "SecondName";
	private static final String THIRD_NAME = "ThirdName";
	private static final StringDataSource DS1 = new StringDataSource("FirstEntry", FIRST_NAME);
	private static final StringDataSource DS2 = new StringDataSource("SecondEntry", SECOND_NAME, Jdk8Utils.mapOf("attributeName1", "attributeValue1"));
	private static final StringDataSource DS3 = new StringDataSource("ThirdEntry", SECOND_NAME);	// Make sure we have a duplicate entry
	private static final StringDataSource DS4 = new StringDataSource("FourthEntry", THIRD_NAME);

	private static final Predicate<DataSource> emptyAttributes = (ds)->ds.attributes().isEmpty();
	private static final Predicate<DataSource> hasFilename = (ds)->ds.filename().isPresent();

	private final List<DataSource> srcList = Jdk8Utils.listOf(
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
			()->assertFalse(underTest.getDataSourceByName("SomethingNotThere").isPresent())
		);
	}

	@Test
	void testGetDataSource() {
		assertAll(
				// DS1 has no attributes
				()->assertSame(DS1, underTest.getDataSource(emptyAttributes).get()),
				// DS2 has attributes
				()->assertSame(DS2, underTest.getDataSource(emptyAttributes.negate()).get()),
				// Should be no DataSources with a filename, so this query should return an Optional.empty() result. 
				()->assertFalse(underTest.getDataSource(hasFilename).isPresent())
				);
	}

	@Test
	void testGetDataSourcesByName() {
		assertAll(
				()->assertIterableEquals(Jdk8Utils.listOf(DS1), underTest.getDataSourcesByName(FIRST_NAME)),
				()->assertIterableEquals(Jdk8Utils.listOf(DS2, DS3), underTest.getDataSourcesByName(SECOND_NAME)),
				()->assertIterableEquals(Jdk8Utils.listOf(DS4), underTest.getDataSourcesByName(THIRD_NAME)),
				()->assertTrue(underTest.getDataSourcesByName("SomethingNotThere").isEmpty())
				);
	}

	@Test
	void testGetDataSources() {
		assertAll(
				// DS1, DS3, and DS4 hava no attributes
				()->assertIterableEquals(Jdk8Utils.listOf(DS1, DS3, DS4), underTest.getDataSources(emptyAttributes)),
				// DS2 has attributes
				()->assertIterableEquals(Jdk8Utils.listOf(DS2), underTest.getDataSources(emptyAttributes.negate())),
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
	
	@Test
	void testFrom_MultipleDataSourceLists_OneList() {
		DataSourceList underTest2 = DataSourceList.from(underTest);
		assertAll(
				()->assertIterableEquals(Jdk8Utils.listOf(DS1), underTest2.getDataSourcesByName(FIRST_NAME)),
				()->assertIterableEquals(Jdk8Utils.listOf(DS2, DS3), underTest2.getDataSourcesByName(SECOND_NAME)),
				()->assertIterableEquals(Jdk8Utils.listOf(DS4), underTest2.getDataSourcesByName(THIRD_NAME)),
				()->assertTrue(underTest.getDataSourcesByName("SomethingNotThere").isEmpty())
				);
		
	}

	@Test
	void testFrom_MultipleDataSourceLists_TwoLists() {
		DataSourceList underTest2 = DataSourceList.from(underTest, underTest);
		assertAll(
				()->assertIterableEquals(Jdk8Utils.listOf(DS1, DS1), underTest2.getDataSourcesByName(FIRST_NAME)),
				()->assertIterableEquals(Jdk8Utils.listOf(DS2, DS3, DS2, DS3), underTest2.getDataSourcesByName(SECOND_NAME)),
				()->assertIterableEquals(Jdk8Utils.listOf(DS4, DS4), underTest2.getDataSourcesByName(THIRD_NAME)),
				()->assertTrue(underTest.getDataSourcesByName("SomethingNotThere").isEmpty())
				);
		
	}

	@Test
	void testFrom_MultipleDataSourceLists_ThreeLists() {
		DataSourceList underTest2 = DataSourceList.from(underTest, underTest, underTest);
		assertAll(
				()->assertIterableEquals(Jdk8Utils.listOf(DS1, DS1, DS1), underTest2.getDataSourcesByName(FIRST_NAME)),
				()->assertIterableEquals(Jdk8Utils.listOf(DS2, DS3, DS2, DS3, DS2, DS3), underTest2.getDataSourcesByName(SECOND_NAME)),
				()->assertIterableEquals(Jdk8Utils.listOf(DS4, DS4, DS4), underTest2.getDataSourcesByName(THIRD_NAME)),
				()->assertTrue(underTest.getDataSourcesByName("SomethingNotThere").isEmpty())
				);
		
	}

	@Test
	void testIterable() {
		List<DataSource> list = new ArrayList<>();
		for (DataSource ds : underTest) {
			list.add(ds);
		}
		assertIterableEquals(underTest.list(), list);
	}


	@Test
	void testForEach() {
		List<DataSource> list = new ArrayList<>();
		underTest.forEach(list::add);
		assertIterableEquals(underTest.list(), list);
	}

	@Test
	void testStream() {
		List<DataSource> list = underTest.stream().collect(Collectors.toList());
		assertIterableEquals(underTest.list(), list);
	}

	@Test
	void testParallelStream() {
		List<DataSource> list = underTest.parallelStream().collect(Collectors.toList());
		assertIterableEquals(underTest.list(), list);
	}

	@Test
	void testSize() {
		assertEquals(4, underTest.size());
	}

	@Test
	void testGet() {
		assertAll(
				()->assertEquals(DS1, underTest.get(0)),
				()->assertEquals(DS2, underTest.get(1)),
				()->assertEquals(DS3, underTest.get(2)),
				()->assertEquals(DS4, underTest.get(3))
				);
	}

	@Test
	void testSpliterator() {
		List<DataSource> list = new ArrayList<>();
		underTest.spliterator().forEachRemaining(list::add);
		assertIterableEquals(underTest.list(), list);
	}

}
