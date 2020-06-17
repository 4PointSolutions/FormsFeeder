package com._4point.aem.formsfeeder.core.support;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Jdk8UtilsTest {

	@Test
	void testReadAllBytes() throws Exception {
		final byte[] expectedOuput = "Test Data".getBytes(StandardCharsets.UTF_8);
		
		assertArrayEquals(expectedOuput, Jdk8Utils.readAllBytes(new ByteArrayInputStream(expectedOuput)));
	}

	@Test
	void testTransfer() throws Exception {
		final byte[] expectedOuput = "Test Data".getBytes(StandardCharsets.UTF_8);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Jdk8Utils.transfer(new ByteArrayInputStream(expectedOuput), out);
		assertArrayEquals(expectedOuput, out.toByteArray());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", " 	   "})
	void testIsBlank_Blank(String input) {
		assertTrue(Jdk8Utils.isBlank(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"e", "  e  " })
	void testIsBlank_NotBlank(String input) {
		assertFalse(Jdk8Utils.isBlank(input));
	}

	@Test
	void testCopyOfMap() {
		final Map<String, String> expectedResult = new HashMap<>();
		expectedResult.put("Key1", "Value1");
		expectedResult.put("Key2", "Value2");
		
		Map<String, String> copyOfMap = Jdk8Utils.copyOfMap(expectedResult);
		
		// Make sure the sets are equal.  This isn't perfect but is good enough (i.e. doesn't detect transposition of keys and values)
		assertTrue(copyOfMap.keySet().containsAll(expectedResult.keySet()));
		assertTrue(copyOfMap.values().containsAll(expectedResult.values()));
		assertTrue(expectedResult.keySet().containsAll(copyOfMap.keySet()));
		assertTrue(expectedResult.values().containsAll(copyOfMap.values()));
		
		assertThrows(UnsupportedOperationException.class, ()->copyOfMap.put("Key3", "Value3"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testMapOfEntries() {
		List<Map.Entry<String, String>> expectedEntries = new ArrayList<>();
		String key1 = "Key1";
		String value1 = "Value1";
		expectedEntries.add(new AbstractMap.SimpleEntry<String, String>(key1, value1));
		String key2 = "Key2";
		String value2 = "Value2";
		expectedEntries.add(new AbstractMap.SimpleEntry<String, String>(key2, value2));

		String key3 = "Key3";
		String value3 = "Value3";
		expectedEntries.add(new AbstractMap.SimpleEntry<String, String>(key3, value3));

		final Map<String, String> expectedResult = new HashMap<>();
		expectedResult.put(key1, value1);
		expectedResult.put(key2, value2);
		expectedResult.put(key3, value3);

		Map<String, String> mapOfEntries = Jdk8Utils.mapOfEntries(expectedEntries.get(0), expectedEntries.get(1), expectedEntries.get(2));

		// Make sure the sets are equal.  This isn't perfect but is good enough (i.e. doesn't detect transposition of keys and values)
		assertTrue(mapOfEntries.keySet().containsAll(expectedResult.keySet()));
		assertTrue(mapOfEntries.values().containsAll(expectedResult.values()));
		assertTrue(expectedResult.keySet().containsAll(mapOfEntries.keySet()));
		assertTrue(expectedResult.values().containsAll(mapOfEntries.values()));
		
		assertThrows(UnsupportedOperationException.class, ()->mapOfEntries.put("Key4", "Value4"));
	}

	@Test
	void testMapOf() {
		String key1 = "Key1";
		String value1 = "Value1";
		String key2 = "Key2";
		String value2 = "Value2";
		String key3 = "Key3";
		String value3 = "Value3";

		final Map<String, String> expectedResult = new HashMap<>();
		expectedResult.put(key1, value1);
		expectedResult.put(key2, value2);
		expectedResult.put(key3, value3);

		Map<String, String> result = Jdk8Utils.mapOf(key1, value1, key2, value2, key3, value3);
		
		// Make sure the sets are equal.  This isn't perfect but is good enough (i.e. doesn't detect transposition of keys and values)
		assertTrue(result.keySet().containsAll(expectedResult.keySet()));
		assertTrue(result.values().containsAll(expectedResult.values()));
		assertTrue(expectedResult.keySet().containsAll(result.keySet()));
		assertTrue(expectedResult.values().containsAll(result.values()));
		
		assertThrows(UnsupportedOperationException.class, ()->result.put("Key4", "Value4"));
	}

	@Test
	void testCopyOfSet() {
		String value1 = "Value1";
		String value2 = "Value2";
		String value3 = "Value3";
		
		final Set<String> expectedResult = new HashSet<>();
		expectedResult.add(value1);
		expectedResult.add(value2);
		expectedResult.add(value3);
		
		Set<String> copyOfSet = Jdk8Utils.copyOfSet(expectedResult);
		assertTrue(copyOfSet.containsAll(expectedResult));
		assertTrue(expectedResult.containsAll(copyOfSet));
		
		assertThrows(UnsupportedOperationException.class, ()->copyOfSet.add("Value4"));
	}

	@Test
	void testSetOf() {
		String value1 = "Value1";
		String value2 = "Value2";
		String value3 = "Value3";
		
		final Set<String> expectedResult = new HashSet<>();
		expectedResult.add(value1);
		expectedResult.add(value2);
		expectedResult.add(value3);
		
		Set<String> resultSet = Jdk8Utils.setOf(value1, value2, value3);
		assertTrue(resultSet.containsAll(expectedResult));
		assertTrue(expectedResult.containsAll(resultSet));
		
		assertThrows(UnsupportedOperationException.class, ()->resultSet.add("Value4"));
	}

	@Test
	void testCopyOfList() {
		String value1 = "Value1";
		String value2 = "Value2";
		String value3 = "Value3";
		
		final List<String> expectedResult = new ArrayList<>();
		expectedResult.add(value1);
		expectedResult.add(value2);
		expectedResult.add(value3);
		
		List<String> copyOfList = Jdk8Utils.copyOfList(expectedResult);
		assertTrue(copyOfList.containsAll(expectedResult));
		assertTrue(expectedResult.containsAll(copyOfList));
		assertIterableEquals(expectedResult, copyOfList);
		
		assertThrows(UnsupportedOperationException.class, ()->copyOfList.add("Value4"));
	}

	@Test
	void testListOf() {
		String value1 = "Value1";
		String value2 = "Value2";
		String value3 = "Value3";
		
		final List<String> expectedResult = new ArrayList<>();
		expectedResult.add(value1);
		expectedResult.add(value2);
		expectedResult.add(value3);
		
		List<String> resultList = Jdk8Utils.listOf(value1, value2, value3);
		assertTrue(resultList.containsAll(expectedResult));
		assertTrue(expectedResult.containsAll(resultList));
		assertIterableEquals(expectedResult, resultList);
		
		assertThrows(UnsupportedOperationException.class, ()->resultList.add("Value4"));
	}

	@Test
	void testToUnmodifiableMap() {
		List<Map.Entry<String, String>> expectedEntries = new ArrayList<>();
		String key1 = "Key1";
		String value1 = "Value1";
		expectedEntries.add(new AbstractMap.SimpleEntry<String, String>(key1, value1));
		String key2 = "Key2";
		String value2 = "Value2";
		expectedEntries.add(new AbstractMap.SimpleEntry<String, String>(key2, value2));
		String key3 = "Key3";
		String value3 = "Value3";
		expectedEntries.add(new AbstractMap.SimpleEntry<String, String>(key3, value3));

		final Map<String, String> expectedResult = new HashMap<>();
		expectedResult.put(key1, value1);
		expectedResult.put(key2, value2);
		expectedResult.put(key3, value3);

		Map<String, String> mapOfEntries = expectedEntries.stream().collect(Jdk8Utils.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

		// Make sure the sets are equal.  This isn't perfect but is good enough (i.e. doesn't detect transposition of keys and values)
		assertTrue(mapOfEntries.keySet().containsAll(expectedResult.keySet()));
		assertTrue(mapOfEntries.values().containsAll(expectedResult.values()));
		assertTrue(expectedResult.keySet().containsAll(mapOfEntries.keySet()));
		assertTrue(expectedResult.values().containsAll(mapOfEntries.values()));
		
		assertThrows(UnsupportedOperationException.class, ()->mapOfEntries.put("Key4", "Value4"));

	}

}
