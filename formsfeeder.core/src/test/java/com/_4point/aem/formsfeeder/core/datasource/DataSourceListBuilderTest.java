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

class DataSourceListBuilderTest {

	private static final String BOOLEAN_DS_NAME = "BooleanDS";
	private static final String BYTE_ARRAY_DS_NAME = "ByteArrayDS";
	private static final String DOUBLE_DS_NAME = "DoubleDS";
	private static final String FLOAT_DS_NAME = "FloatDS";
	private static final String INTEGER_DS_NAME = "IntegerDS";
	private static final String LONG_DS_NAME = "LongDS";
	private static final String FILE_DS_NAME = "FileDS";
	private static final String STRING_DS_NAME = "StringDS";
	private static final String DUMMY_DS_NAME = "DummyDS";
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
	
	@Test
	void testBuildAll() throws Exception{
		// Construct a DataSourceList with one of each and every type
		boolean booleanData = true;
		byte[] byteArrayData = new byte[0];
		double doubleData = Double.MAX_VALUE;
		float floatData = Float.MAX_VALUE;
		int intData = Integer.MAX_VALUE;
		long longData = Long.MAX_VALUE;
		Path pathData = Paths.get("FileDS.txt");
		String stringData = "String Data";
		DataSourceList result = DataSourceList.builder()
				.add(dummyDS)
				.add(BOOLEAN_DS_NAME, booleanData)
				.add(BYTE_ARRAY_DS_NAME, byteArrayData)
				.add(DOUBLE_DS_NAME, doubleData)
				.add(FLOAT_DS_NAME, floatData)
				.add(INTEGER_DS_NAME, intData)
				.add(LONG_DS_NAME, longData)
				.add(FILE_DS_NAME, pathData)
				.add(STRING_DS_NAME, stringData)
				.build();
		
		List<DataSource> resultList = result.list();
		
		assertAll(
				()->assertEquals(9, resultList.size()),	// 9 DSes were added.
				()->assertSame(dummyDS, resultList.get(0)),
				()->assertEquals(BOOLEAN_DS_NAME, resultList.get(1).name()),
				()->assertEquals(Boolean.toString(booleanData), readIntoString(resultList.get(1).inputStream())),
				()->assertEquals(BYTE_ARRAY_DS_NAME, resultList.get(2).name()),
				()->assertArrayEquals(byteArrayData, resultList.get(2).inputStream().readAllBytes()),
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
				()->assertEquals(stringData, readIntoString(resultList.get(8).inputStream()))
				);
	}

	@Test
	void testBuildEmpty() {
		DataSourceList result = DataSourceList.builder().build();
		assertTrue(result.list().isEmpty());
	}


	private static String readIntoString(InputStream is) throws IOException {
		return new String(is.readAllBytes());
	}
}
