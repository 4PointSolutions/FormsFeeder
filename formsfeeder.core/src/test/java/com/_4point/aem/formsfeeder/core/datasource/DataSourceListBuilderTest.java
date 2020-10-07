package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;

import com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListDecoderTest;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

class DataSourceListBuilderTest {

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
	private static final String BYTE_ARRAY_W_CT_DS_NAME = "ByteArrayDSWithContentType";
	private static final String DSL_DS_NAME = "DataSourceListDS";

	private static final String FIRST_DSL_ENTRY_NAME = "FirstName";
	private static final String SECOND_DSL_ENTRY_NAME = "SecondName";
	private static final String THIRD_DSL_ENTRY_NAME = "ThirdName";

	// Custom Data Source
	private static final DataSource dummyDS = new DataSource() {

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
			return null;
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
	private static final boolean booleanData = true;
	private static final byte[] byteArrayData = new byte[0];
	private static final double doubleData = Double.MAX_VALUE;
	private static final float floatData = Float.MAX_VALUE;
	private static final int intData = Integer.MAX_VALUE;
	private static final long longData = Long.MAX_VALUE;
	private static final Path pathData = Paths.get("FileDS.txt");
	private static final String stringData = "String Data";
	private static final MimeType mimeType = StandardMimeTypes.APPLICATION_PDF_TYPE;
 
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


	@Test
	void testBuildAllWithBuilder() throws Exception{
		// Construct a DataSourceList with one of each and every type
		DataSourceList result = addAllDsTypes(DataSourceList.builder()).build();
		
		List<DataSource> resultList = result.list();
		
		validateResultList(resultList);
	}

	@Test
	void testBuildAllWithBuild() throws Exception{
		// Construct a DataSourceList with one of each and every type
		DataSourceList result = DataSourceList.build(DataSourceListBuilderTest::addAllDsTypes);
		
		List<DataSource> resultList = result.list();
		
		validateResultList(resultList);
	}

	private static DataSourceList.Builder addAllDsTypes(DataSourceList.Builder builder) {
		return builder.add(dummyDS)
					  .add(BOOLEAN_DS_NAME, booleanData)
					  .add(BYTE_ARRAY_DS_NAME, byteArrayData)
					  .add(DOUBLE_DS_NAME, doubleData)
					  .add(FLOAT_DS_NAME, floatData)
					  .add(INTEGER_DS_NAME, intData)
					  .add(LONG_DS_NAME, longData)
					  .add(FILE_DS_NAME, pathData)
					  .add(STRING_DS_NAME, stringData)
					  .add(BYTE_ARRAY_W_CT_DS_NAME, byteArrayData, mimeType)
					  .add(DSL_DS_NAME, dslData);

	}
	private void validateResultList(List<DataSource> resultList) throws MultipleFailuresError {
		assertAll(
				()->assertEquals(11, resultList.size()),	// 10 DSes were added.
				()->assertSame(dummyDS, resultList.get(0)),
				()->assertEquals(BOOLEAN_DS_NAME, resultList.get(1).name()),
				()->assertEquals(Boolean.toString(booleanData), readIntoString(resultList.get(1).inputStream())),
				()->assertEquals(BYTE_ARRAY_DS_NAME, resultList.get(2).name()),
				()->assertArrayEquals(byteArrayData, Jdk8Utils.readAllBytes(resultList.get(2).inputStream())),
				()->assertEquals(DOUBLE_DS_NAME, resultList.get(3).name()),
				()->assertEquals(Double.toString(doubleData), readIntoString(resultList.get(3).inputStream())),
				()->assertEquals(FLOAT_DS_NAME, resultList.get(4).name()),
				()->assertEquals(Float.toString(floatData), readIntoString(resultList.get(4).inputStream())),
				()->assertEquals(INTEGER_DS_NAME, resultList.get(5).name()),
				()->assertEquals(Integer.toString(intData), readIntoString(resultList.get(5).inputStream())),
				()->assertEquals(LONG_DS_NAME, resultList.get(6).name()),
				()->assertEquals(Long.toString(longData), readIntoString(resultList.get(6).inputStream())),
				()->assertEquals(FILE_DS_NAME, resultList.get(7).name()),
				()->assertEquals(pathData, resultList.get(7).filename().get()),
				()->assertEquals(STRING_DS_NAME, resultList.get(8).name()),
				()->assertEquals(stringData, readIntoString(resultList.get(8).inputStream())),
				()->assertEquals(BYTE_ARRAY_W_CT_DS_NAME, resultList.get(9).name()),
				()->assertArrayEquals(byteArrayData, Jdk8Utils.readAllBytes(resultList.get(9).inputStream())),
				()->assertEquals(mimeType, resultList.get(9).contentType()),
				()->validateDslData(resultList.get(10))
				);
	}

	/**
	 * Validates that the resultDsl DataSourceList matches the dslData DataSourceList.  
	 * 
	 * @param resultDsl
	 * @throws Exception 
	 */
	void validateDslData(DataSource resultDs) throws Exception {
		assertTrue(resultDs instanceof ByteArrayDataSource, "Expected datasource to be instanceof BDataSource but it wasn't (" + resultDs.getClass().getName() + ").");
		Optional<DataSourceList> dsl = DataSourceList.Deconstructor.dsToDataSourceList(resultDs);
		assertTrue(dsl.isPresent(), "Expected DataSource to be present after decoding.");
		DataSourceList resultDsl = dsl.get();
		assertEquals(dslData.size(), resultDsl.size());
		List<DataSource> resultList = resultDsl.list();
		XmlDataSourceListDecoderTest.dsEquals(DS1, resultList.get(0), true);
		XmlDataSourceListDecoderTest.dsEquals(DS2, resultList.get(1), true);
		XmlDataSourceListDecoderTest.dsEquals(DS3, resultList.get(2), true);
		XmlDataSourceListDecoderTest.dsEquals(DS4, resultList.get(3), true);
	}
	
	@Test
	void testBuildEmpty() {
		DataSourceList result = DataSourceList.builder().build();
		assertTrue(result.list().isEmpty());
	}

	@Test
	void testBuildAllLists() throws Exception{
		// Construct a DataSourceList with one or more of each and every type
		List<DataSource> dsList = Jdk8Utils.listOf(dummyDS, dummyDS);
		List<Boolean> bList = Jdk8Utils.listOf(booleanData, booleanData, booleanData);
		List<byte[]> baList = Jdk8Utils.listOf(byteArrayData);
		List<Double> dList = Jdk8Utils.listOf(doubleData, doubleData);
		List<Float> fList = Jdk8Utils.listOf(floatData, floatData, floatData);
		List<Integer> iList = Jdk8Utils.listOf(intData);
		List<Long> lList = Jdk8Utils.listOf(longData, longData);
		List<Path> pList = Jdk8Utils.listOf(pathData, pathData, pathData);
		List<String> sList = Jdk8Utils.listOf(stringData);
		List<DataSourceList> dslList = Jdk8Utils.listOf(dslData, dslData);
		int expectedSize = dsList.size() + bList.size() + baList.size() + dList.size() + fList.size() + iList.size() + lList.size() + pList.size() + sList.size() + baList.size() + dslList.size();
		
		DataSourceList result = DataSourceList.builder()
				.addDataSources(dsList)
				.addBooleans(BOOLEAN_DS_NAME, bList)
				.addByteArrays(BYTE_ARRAY_DS_NAME, baList)
				.addDoubles(DOUBLE_DS_NAME, dList)
				.addFloats(FLOAT_DS_NAME, fList)
				.addIntegers(INTEGER_DS_NAME, iList)
				.addLongs(LONG_DS_NAME, lList)
				.addPaths(FILE_DS_NAME, pList)
				.addStrings(STRING_DS_NAME, sList)
				.addByteArrays(BYTE_ARRAY_W_CT_DS_NAME, baList, mimeType)
				.addDataSourceLists(DSL_DS_NAME, dslList)
				.build();
		
		List<DataSource> resultList = result.list();
		assertAll(
				()->assertEquals(expectedSize, 21),					// # of DSs matches what we've encoded below.
				()->assertEquals(expectedSize, resultList.size()),	// # of DSs that were added.
				()->assertSame(dummyDS, resultList.get(0)),
				()->assertSame(dummyDS, resultList.get(1)),
				()->assertEquals(BOOLEAN_DS_NAME, resultList.get(2).name()),
				()->assertEquals(Boolean.toString(booleanData), readIntoString(resultList.get(2).inputStream())),
				()->assertEquals(BOOLEAN_DS_NAME, resultList.get(3).name()),
				()->assertEquals(Boolean.toString(booleanData), readIntoString(resultList.get(3).inputStream())),
				()->assertEquals(BOOLEAN_DS_NAME, resultList.get(4).name()),
				()->assertEquals(Boolean.toString(booleanData), readIntoString(resultList.get(4).inputStream())),
				()->assertEquals(BYTE_ARRAY_DS_NAME, resultList.get(5).name()),
				()->assertArrayEquals(byteArrayData, Jdk8Utils.readAllBytes(resultList.get(5).inputStream())),
				()->assertEquals(DOUBLE_DS_NAME, resultList.get(6).name()),
				()->assertEquals(Double.toString(doubleData), readIntoString(resultList.get(6).inputStream())),
				()->assertEquals(DOUBLE_DS_NAME, resultList.get(7).name()),
				()->assertEquals(Double.toString(doubleData), readIntoString(resultList.get(7).inputStream())),
				()->assertEquals(FLOAT_DS_NAME, resultList.get(8).name()),
				()->assertEquals(Float.toString(floatData), readIntoString(resultList.get(8).inputStream())),
				()->assertEquals(FLOAT_DS_NAME, resultList.get(9).name()),
				()->assertEquals(Float.toString(floatData), readIntoString(resultList.get(9).inputStream())),
				()->assertEquals(FLOAT_DS_NAME, resultList.get(10).name()),
				()->assertEquals(Float.toString(floatData), readIntoString(resultList.get(10).inputStream())),
				()->assertEquals(INTEGER_DS_NAME, resultList.get(11).name()),
				()->assertEquals(Integer.toString(intData), readIntoString(resultList.get(11).inputStream())),
				()->assertEquals(LONG_DS_NAME, resultList.get(12).name()),
				()->assertEquals(Long.toString(longData), readIntoString(resultList.get(12).inputStream())),
				()->assertEquals(LONG_DS_NAME, resultList.get(13).name()),
				()->assertEquals(Long.toString(longData), readIntoString(resultList.get(13).inputStream())),
				()->assertEquals(FILE_DS_NAME, resultList.get(14).name()),
				()->assertEquals(pathData, resultList.get(14).filename().get()),
				()->assertEquals(FILE_DS_NAME, resultList.get(15).name()),
				()->assertEquals(pathData, resultList.get(15).filename().get()),
				()->assertEquals(FILE_DS_NAME, resultList.get(16).name()),
				()->assertEquals(pathData, resultList.get(16).filename().get()),
				()->assertEquals(STRING_DS_NAME, resultList.get(17).name()),
				()->assertEquals(stringData, readIntoString(resultList.get(17).inputStream())),
				()->assertEquals(BYTE_ARRAY_W_CT_DS_NAME, resultList.get(18).name()),
				()->assertArrayEquals(byteArrayData, Jdk8Utils.readAllBytes(resultList.get(18).inputStream())),
				()->assertEquals(mimeType, resultList.get(18).contentType()),
				()->validateDslData(resultList.get(19)),
				()->validateDslData(resultList.get(20))
				);
	}

	@Test
	void testBuildEmptyLists() throws Exception {
		DataSourceList result = DataSourceList.builder()
				.addDataSources(Collections.emptyList())
				.addBooleans(BOOLEAN_DS_NAME, Collections.emptyList())
				.addByteArrays(BYTE_ARRAY_DS_NAME, Collections.emptyList())
				.addDoubles(DOUBLE_DS_NAME, Collections.emptyList())
				.addFloats(FLOAT_DS_NAME, Collections.emptyList())
				.addIntegers(INTEGER_DS_NAME, Collections.emptyList())
				.addLongs(LONG_DS_NAME, Collections.emptyList())
				.addPaths(FILE_DS_NAME, Collections.emptyList())
				.addStrings(STRING_DS_NAME, Collections.emptyList())
				.addDataSourceLists(DSL_DS_NAME, Collections.emptyList())
				.build();
		
		assertTrue(result.list().isEmpty());
	}

	private static String readIntoString(InputStream is) throws IOException {
		return new String(Jdk8Utils.readAllBytes(is));
	}
}
