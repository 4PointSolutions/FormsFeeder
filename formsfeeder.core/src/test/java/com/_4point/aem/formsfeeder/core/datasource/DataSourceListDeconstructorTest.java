package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListDecoderTest;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

class DataSourceListDeconstructorTest {

	// DataSource Names
	private static final String BOOLEAN_DS_NAME = "BooleanDS";
	private static final String BYTE_ARRAY_DS_NAME = "ByteArrayDS";
	private static final String DOUBLE_DS_NAME = "DoubleDS";
	private static final String FLOAT_DS_NAME = "FloatDS";
	private static final String INTEGER_DS_NAME = "IntegerDS";
	private static final String LONG_DS_NAME = "LongDS";
	private static final String FILE_DS_NAME = "FileDS";
	private static final String STRING_DS_NAME = "StringDS";
	private static final String DUMMY_DS_NAME = "DummyDS";
	private static final String DSL_DS_NAME = "DataSourceListDS";

	private static final String FIRST_DSL_ENTRY_NAME = "FirstName";
	private static final String SECOND_DSL_ENTRY_NAME = "SecondName";
	private static final String THIRD_DSL_ENTRY_NAME = "ThirdName";

	// Custom Data Source
	private static final DataSource dummyDS = new DataSource() {

		public static final String DUMMY_IO_EXCEPTION_MSG = "Dummy IO Exception!";

		@Override
		public OutputStream outputStream() {
			return null;
		}
		
		@Override
		public String name() {
			return DUMMY_DS_NAME;
		}
		
		@Override
		public InputStream inputStream() {
			return new InputStream() {
				
				@Override
				public int read() throws IOException {
					throw new IOException(DUMMY_IO_EXCEPTION_MSG);
				}
			};
		}
		
		@Override
		public Optional<Path> filename() {
			return Optional.empty();
		}
		
		@Override
		public MimeType contentType() {
			return StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE;
		}
		
		@Override
		public Map<String, String> attributes() {
			return Collections.emptyMap();
		}
	};

	// Data for standard data sources.
	private static final String byteArrayDataStr = "Byte Array Data";
	private static final boolean booleanData = true;
	private static final byte[] byteArrayData = byteArrayDataStr.getBytes(StandardCharsets.UTF_8);
	private static final double doubleData = Double.MAX_VALUE;
	private static final float floatData = Float.MAX_VALUE;
	private static final int intData = Integer.MAX_VALUE;
	private static final long longData = Long.MAX_VALUE;
	private static final Path pathData = Paths.get("FileDS.txt");
	private static final String stringData = "String Data";

	private static final StringDataSource DS1 = new StringDataSource("FirstEntry", FIRST_DSL_ENTRY_NAME);
	private static final StringDataSource DS2 = new StringDataSource("SecondEntry", SECOND_DSL_ENTRY_NAME, Jdk8Utils.mapOf("attributeName1", "attributeValue1"));
	private static final StringDataSource DS3 = new StringDataSource("ThirdEntry", SECOND_DSL_ENTRY_NAME);	// Make sure we have a duplicate entry
	private static final StringDataSource DS4 = new StringDataSource("FourthEntry", THIRD_DSL_ENTRY_NAME);

	private static final List<DataSource> srcList = Jdk8Utils.listOf(
										DS1,
										DS2,
										DS3,
										DS4
									   );

	private static final DataSourceList dslData = DataSourceList.from(srcList);

	// Construct a DataSourceList with one or more of each and every type
	private static final DataSourceList sampleDataSource = DataSourceList.builder()
			.addDataSources(Jdk8Utils.listOf(dummyDS, dummyDS))
			.addBooleans(BOOLEAN_DS_NAME, Jdk8Utils.listOf(booleanData, booleanData, booleanData))
			.addByteArrays(BYTE_ARRAY_DS_NAME, Jdk8Utils.listOf(byteArrayData))
			.addDoubles(DOUBLE_DS_NAME, Jdk8Utils.listOf(doubleData, doubleData))
			.addFloats(FLOAT_DS_NAME, Jdk8Utils.listOf(floatData, floatData, floatData))
			.addIntegers(INTEGER_DS_NAME, Jdk8Utils.listOf(intData))
			.addLongs(LONG_DS_NAME, Jdk8Utils.listOf(longData, longData))
			.addPaths(FILE_DS_NAME, Jdk8Utils.listOf(pathData, pathData, pathData))
			.addStrings(STRING_DS_NAME, Jdk8Utils.listOf(stringData))
			.addDataSourceLists(DSL_DS_NAME, Jdk8Utils.listOf(dslData, dslData))
			.build();


	@Test
	void testGetByName() {
		Deconstructor underTest = sampleDataSource.deconstructor();
		assertAll(
				()->assertEquals(dummyDS, underTest.getDataSourceByName(DUMMY_DS_NAME).get()),
				()->assertEquals(booleanData, underTest.getBooleanByName(BOOLEAN_DS_NAME).get()),
				()->assertArrayEquals(byteArrayData, underTest.getByteArrayByName(BYTE_ARRAY_DS_NAME).get()),
				()->assertEquals(doubleData, underTest.getDoubleByName(DOUBLE_DS_NAME).get()),
				()->assertEquals(floatData, underTest.getFloatByName(FLOAT_DS_NAME).get()),
				()->assertEquals(intData, underTest.getIntegerByName(INTEGER_DS_NAME).get()),
				()->assertEquals(longData, underTest.getLongByName(LONG_DS_NAME).get()),
				()->assertEquals(stringData, underTest.getStringByName(STRING_DS_NAME).get()),
				()->XmlDataSourceListDecoderTest.dslEquals(dslData, underTest.getDataSourceListByName(DSL_DS_NAME).get(), true)
				);
		
		// Since the File does not exist, we expect an error here.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.getStringByName(FILE_DS_NAME).get());
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(pathData.toString()));
	}

	@Test
	void testGetByNameEmptyList() {
		Deconstructor underTest = DataSourceList.builder().build().deconstructor();
		assertAll(
				()->assertFalse(underTest.getDataSourceByName(DUMMY_DS_NAME).isPresent()),
				()->assertFalse(underTest.getBooleanByName(BOOLEAN_DS_NAME).isPresent()),
				()->assertFalse(underTest.getByteArrayByName(BYTE_ARRAY_DS_NAME).isPresent()),
				()->assertFalse(underTest.getDoubleByName(DOUBLE_DS_NAME).isPresent()),
				()->assertFalse(underTest.getFloatByName(FLOAT_DS_NAME).isPresent()),
				()->assertFalse(underTest.getIntegerByName(INTEGER_DS_NAME).isPresent()),
				()->assertFalse(underTest.getLongByName(LONG_DS_NAME).isPresent()),
				()->assertFalse(underTest.getStringByName(STRING_DS_NAME).isPresent()),
				()->assertFalse(underTest.getDataSourceListByName(DSL_DS_NAME).isPresent())
				);
	}

	@Test
	void testGetByPredicateByName() {
		Deconstructor underTest = sampleDataSource.deconstructor();
		assertAll(
				()->assertEquals(dummyDS, underTest.getDataSource(DataSourceList.byName(DUMMY_DS_NAME)).get()),
				()->assertEquals(booleanData, underTest.getBoolean(DataSourceList.byName(BOOLEAN_DS_NAME)).get()),
				()->assertArrayEquals(byteArrayData, underTest.getByteArray(DataSourceList.byName(BYTE_ARRAY_DS_NAME)).get()),
				()->assertEquals(doubleData, underTest.getDouble(DataSourceList.byName(DOUBLE_DS_NAME)).get()),
				()->assertEquals(floatData, underTest.getFloat(DataSourceList.byName(FLOAT_DS_NAME)).get()),
				()->assertEquals(intData, underTest.getInteger(DataSourceList.byName(INTEGER_DS_NAME)).get()),
				()->assertEquals(longData, underTest.getLong(DataSourceList.byName(LONG_DS_NAME)).get()),
				()->assertEquals(stringData, underTest.getString(DataSourceList.byName(STRING_DS_NAME)).get()),
				()->XmlDataSourceListDecoderTest.dslEquals(dslData, underTest.getDataSourceList(DataSourceList.byName(DSL_DS_NAME)).get(), true)
				);
		
		// Since the File does not exist, we expect an error here.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.getString(DataSourceList.byName(FILE_DS_NAME)).get());
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(pathData.toString()));
	}

	@Test
	void testGetWithNonNamePredicate() {
		Deconstructor underTest = sampleDataSource.deconstructor();
		// First DS with octet-stream type is the dummy DS
		assertEquals(dummyDS, underTest.getDataSource(ds->ds.contentType().equals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE)).get());
		// Test where nothing matches.
		assertFalse(underTest.getDataSource(ds->false).isPresent());
	}

	@Test
	void testGetMultipleByName() {
		Deconstructor underTest = sampleDataSource.deconstructor();
		assertAll(
				()->assertEquals(dummyDS, underTest.getDataSourcesByName(DUMMY_DS_NAME).get(0)),
				()->assertEquals(2, underTest.getDataSourcesByName(DUMMY_DS_NAME).size()),
				()->assertEquals(booleanData, underTest.getBooleansByName(BOOLEAN_DS_NAME).get(0)),
				()->assertEquals(3, underTest.getBooleansByName(BOOLEAN_DS_NAME).size()),
				()->assertArrayEquals(byteArrayData, underTest.getByteArraysByName(BYTE_ARRAY_DS_NAME).get(0)),
				()->assertEquals(1, underTest.getByteArraysByName(BYTE_ARRAY_DS_NAME).size()),
				()->assertEquals(doubleData, underTest.getDoublesByName(DOUBLE_DS_NAME).get(0)),
				()->assertEquals(2, underTest.getDoublesByName(DOUBLE_DS_NAME).size()),
				()->assertEquals(floatData, underTest.getFloatsByName(FLOAT_DS_NAME).get(0)),
				()->assertEquals(3, underTest.getFloatsByName(FLOAT_DS_NAME).size()),
				()->assertEquals(intData, underTest.getIntegersByName(INTEGER_DS_NAME).get(0)),
				()->assertEquals(1, underTest.getIntegersByName(INTEGER_DS_NAME).size()),
				()->assertEquals(longData, underTest.getLongsByName(LONG_DS_NAME).get(0)),
				()->assertEquals(2, underTest.getLongsByName(LONG_DS_NAME).size()),
				()->assertEquals(stringData, underTest.getStringsByName(STRING_DS_NAME).get(0)),
				()->assertEquals(1, underTest.getStringsByName(STRING_DS_NAME).size()),
				()->XmlDataSourceListDecoderTest.dslEquals(dslData, underTest.getDataSourceListsByName(DSL_DS_NAME).get(0), true),
				()->assertEquals(2, underTest.getDataSourceListsByName(DSL_DS_NAME).size())
				);
		
		// Since the File does not exist, we expect an error here.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.getStringsByName(FILE_DS_NAME).size());
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(pathData.toString()));
	}

	@Test
	void testGetMultipleByPredicateByName() {
		Deconstructor underTest = sampleDataSource.deconstructor();
		assertAll(
				()->assertEquals(dummyDS, underTest.getDataSources(DataSourceList.byName(DUMMY_DS_NAME)).get(0)),
				()->assertEquals(2, underTest.getDataSources(DataSourceList.byName(DUMMY_DS_NAME)).size()),
				()->assertEquals(booleanData, underTest.getBooleans(DataSourceList.byName(BOOLEAN_DS_NAME)).get(0)),
				()->assertEquals(3, underTest.getBooleans(DataSourceList.byName(BOOLEAN_DS_NAME)).size()),
				()->assertArrayEquals(byteArrayData, underTest.getByteArrays(DataSourceList.byName(BYTE_ARRAY_DS_NAME)).get(0)),
				()->assertEquals(1, underTest.getByteArrays(DataSourceList.byName(BYTE_ARRAY_DS_NAME)).size()),
				()->assertEquals(doubleData, underTest.getDoubles(DataSourceList.byName(DOUBLE_DS_NAME)).get(0)),
				()->assertEquals(2, underTest.getDoubles(DataSourceList.byName(DOUBLE_DS_NAME)).size()),
				()->assertEquals(floatData, underTest.getFloats(DataSourceList.byName(FLOAT_DS_NAME)).get(0)),
				()->assertEquals(3, underTest.getFloats(DataSourceList.byName(FLOAT_DS_NAME)).size()),
				()->assertEquals(intData, underTest.getIntegers(DataSourceList.byName(INTEGER_DS_NAME)).get(0)),
				()->assertEquals(1, underTest.getIntegers(DataSourceList.byName(INTEGER_DS_NAME)).size()),
				()->assertEquals(longData, underTest.getLongs(DataSourceList.byName(LONG_DS_NAME)).get(0)),
				()->assertEquals(2, underTest.getLongs(DataSourceList.byName(LONG_DS_NAME)).size()),
				()->assertEquals(stringData, underTest.getStrings(DataSourceList.byName(STRING_DS_NAME)).get(0)),
				()->assertEquals(1, underTest.getStrings(DataSourceList.byName(STRING_DS_NAME)).size()),
				()->XmlDataSourceListDecoderTest.dslEquals(dslData, underTest.getDataSourceLists(DataSourceList.byName(DSL_DS_NAME)).get(0), true),
				()->assertEquals(2, underTest.getDataSourceLists(DataSourceList.byName(DSL_DS_NAME)).size())
				);
		
		// Since the File does not exist, we expect an error here.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()->underTest.getStrings(DataSourceList.byName(FILE_DS_NAME)).size());
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertTrue(msg.contains(pathData.toString()));
	}

	@Test
	void testGetMultipleWithNonNamePredicate() {
		Deconstructor underTest = sampleDataSource.deconstructor();
		// First DS with octet-stream type is the dummy DS
		assertEquals(dummyDS, underTest.getDataSources(ds->ds.contentType().equals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE)).get(0));
		// Two dummyDS entries plus the one byte array should all have an mime-type of application/octet-stream.
		assertEquals(3, underTest.getDataSources(ds->ds.contentType().equals(StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE)).size());
		// Test where nothing matches.
		assertTrue(underTest.getDataSources(ds->false).isEmpty());
	}

	/**
	 * Test code where we don't shortcut and have to read in the bytes to convert to native type.
	 */
	@Test
	void testDataSourceConversions() {
		Deconstructor underTest = sampleDataSource.deconstructor();
	
		assertArrayEquals(stringData.getBytes(StandardCharsets.UTF_8), underTest.getByteArrayByName(STRING_DS_NAME).get());
		assertEquals(byteArrayDataStr, underTest.getStringByName(BYTE_ARRAY_DS_NAME).get());
	}
	
	/**
	 * Test code where we don't shortcut and have to read in the bytes to convert to native type reading the bytes throws an exception.
	 */
	@Test
	void testDataSourceConversionsWithExceptions() {
		Deconstructor underTest = sampleDataSource.deconstructor();
	
		IllegalStateException ex1 = assertThrows(IllegalStateException.class, ()->underTest.getByteArrayByName(DUMMY_DS_NAME).get());
		String msg1 = ex1.getMessage();
		assertNotNull(msg1);
		assertEquals("Error while converting DataSource to ByteArray", msg1);
		
		IllegalStateException ex2 = assertThrows(IllegalStateException.class, ()->underTest.getStringByName(DUMMY_DS_NAME).get());
		String msg2 = ex2.getMessage();
		assertNotNull(msg2);
		assertEquals("Error while converting DataSource to String", msg2);
	}
	
	@Test
	void testGetStringWithCharset() {
		Deconstructor underTest = sampleDataSource.deconstructor();
		assertEquals(stringData, Deconstructor.dsToString(underTest.getDataSourceByName(STRING_DS_NAME).get(), StandardCharsets.UTF_8));
	}
	
}
