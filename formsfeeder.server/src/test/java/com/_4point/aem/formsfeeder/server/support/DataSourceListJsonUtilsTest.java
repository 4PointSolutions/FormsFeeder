package com._4point.aem.formsfeeder.server.support;

import static com._4point.aem.formsfeeder.server.support.DataSourceListJsonUtils.asDataSourceList;
import static com._4point.aem.formsfeeder.server.support.DataSourceListJsonUtils.asJson;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;

class DataSourceListJsonUtilsTest {
	private final static Logger logger = LoggerFactory.getLogger(DataSourceListJsonUtilsTest.class);
	static final Decoder BYTE_DECODER = Base64.getDecoder();

	private static final BigDecimal BIG_DECIMAL_DATA = BigDecimal.TEN;
	private static final BigInteger BIG_INTEGER_DATA = BigInteger.TWO;
	private static final boolean BOOLEAN_DATA = true;
	private static final double DOUBLE_DATA = Double.MAX_VALUE;
	private static final int INTEGER_DATA = Integer.MAX_VALUE;
	private static final long LONG_DATA = Long.MAX_VALUE;
	private static final String STRING_DATA = "StringValue";
	private static final byte[] BYTE_ARRAY_DATA = STRING_DATA.getBytes(StandardCharsets.UTF_8);

	private static final String BIG_DECIMAL_DATA_STR = "BigDecimalData";
	private static final String BIG_INTEGER_DATA_STR = "BigIntegerData";
	private static final String BOOLEAN_DATA_STR = "BooleanData";
	private static final String DOUBLE_DATA_STR = "DoubleData";
	private static final String INTEGER_DATA_STR = "IntegerData";
	private static final String LONG_DATA_STR = "LongData";
	private static final String STRING_DATA_STR = "StringData";
	private static final String BYTE_ARRAY_DATA_STR = "ByteArrayData";
	private static final String NULL_DATA_STR = "NullData";
	private static final String JSON_ARRAY_DATA_STR = "JsonArrayData";
	private static final String JSON_OBJECT_DATA_STR = "JsonObjectData";
	private static final String NESTED_JSON_ARRAY_DATA_STR = "NestedJsonArrayData";
	private static final String NESTED_JSON_OBJECT_DATA_STR = "NestedJsonObjectData";
	
	@Test
	void testAsDataSourceListAllTypes() {
		JsonArrayBuilder arrayBldr = Json.createArrayBuilder()
				  .add(BIG_DECIMAL_DATA)
				  .add(BIG_INTEGER_DATA)
				  .add(BOOLEAN_DATA)
				  .add(DOUBLE_DATA)
				  .add(INTEGER_DATA)
				  .add(LONG_DATA)
				  .add(STRING_DATA)
				  .addNull();
		
		JsonObjectBuilder jsonObjBldr = Json.createObjectBuilder()
				  .add(BIG_DECIMAL_DATA_STR, BIG_DECIMAL_DATA)
				  .add(BIG_INTEGER_DATA_STR, BIG_INTEGER_DATA)
				  .add(BOOLEAN_DATA_STR, BOOLEAN_DATA)
				  .add(DOUBLE_DATA_STR, DOUBLE_DATA)
				  .add(INTEGER_DATA_STR, INTEGER_DATA)
				  .add(LONG_DATA_STR, LONG_DATA)
				  .add(STRING_DATA_STR, STRING_DATA)
				  .addNull(NULL_DATA_STR);
		
		JsonObject jsonData = Json.createObjectBuilder()
				  .add(BIG_DECIMAL_DATA_STR, BIG_DECIMAL_DATA)
				  .add(BIG_INTEGER_DATA_STR, BIG_INTEGER_DATA)
				  .add(BOOLEAN_DATA_STR, BOOLEAN_DATA)
				  .add(DOUBLE_DATA_STR, DOUBLE_DATA)
				  .add(INTEGER_DATA_STR, INTEGER_DATA)
				  .add(LONG_DATA_STR, LONG_DATA)
				  .add(STRING_DATA_STR, STRING_DATA)
				  .addNull(NULL_DATA_STR)
				  .add(JSON_ARRAY_DATA_STR, arrayBldr)
				  .add(JSON_OBJECT_DATA_STR, jsonObjBldr)
				  .build();
		
		DataSourceList resultDsl = asDataSourceList(jsonData, logger);

		Deconstructor deconstructor = resultDsl.deconstructor();
		List<DataSource> arrayDsl = deconstructor.getDataSourceListByName(JSON_ARRAY_DATA_STR).get().list();
		DataSourceList jsonObjDsl = deconstructor.getDataSourceListByName(JSON_OBJECT_DATA_STR).get();
		Deconstructor jsonObjDeconstructor = jsonObjDsl.deconstructor();
		// Main DataSourceList assertions.
		assertAll(
				()->assertEquals(BIG_DECIMAL_DATA.toString(), deconstructor.getStringByName(BIG_DECIMAL_DATA_STR).get()),
				()->assertEquals(BIG_INTEGER_DATA.toString(), deconstructor.getStringByName(BIG_INTEGER_DATA_STR).get()),
				()->assertEquals(BOOLEAN_DATA, deconstructor.getBooleanByName(BOOLEAN_DATA_STR).get()),
				()->assertEquals(DOUBLE_DATA, deconstructor.getDoubleByName(DOUBLE_DATA_STR).get()),
				()->assertEquals(INTEGER_DATA, deconstructor.getIntegerByName(INTEGER_DATA_STR).get()),
				()->assertEquals(LONG_DATA, deconstructor.getLongByName(LONG_DATA_STR).get()),
				()->assertEquals(STRING_DATA, deconstructor.getStringByName(STRING_DATA_STR).get()),
				()->assertTrue(deconstructor.getStringByName(NULL_DATA_STR).get().isEmpty()),
				()->assertEquals(10, resultDsl.size())
				);
		
		// JsonArray assertions.
		assertAll(
				()->assertEquals(8, arrayDsl.size()),
				()->assertEquals(BIG_DECIMAL_DATA.toString(), Deconstructor.dsToString(arrayDsl.get(0))),
				()->assertEquals(BIG_INTEGER_DATA.toString(), Deconstructor.dsToString(arrayDsl.get(1))),
				()->assertEquals(BOOLEAN_DATA, Deconstructor.dsToBoolean(arrayDsl.get(2))),
				()->assertEquals(DOUBLE_DATA, Deconstructor.dsToDouble(arrayDsl.get(3))),
				()->assertEquals(INTEGER_DATA, Deconstructor.dsToInteger(arrayDsl.get(4))),
				()->assertEquals(LONG_DATA, Deconstructor.dsToLong(arrayDsl.get(5))),
				()->assertEquals(STRING_DATA, Deconstructor.dsToString(arrayDsl.get(6))),
				()->assertTrue(Deconstructor.dsToString(arrayDsl.get(7)).isEmpty())
				);

		// Json Object assertions.
		assertAll(
				()->assertEquals(BIG_DECIMAL_DATA.toString(), jsonObjDeconstructor.getStringByName(BIG_DECIMAL_DATA_STR).get()),
				()->assertEquals(BIG_INTEGER_DATA.toString(), jsonObjDeconstructor.getStringByName(BIG_INTEGER_DATA_STR).get()),
				()->assertEquals(BOOLEAN_DATA, jsonObjDeconstructor.getBooleanByName(BOOLEAN_DATA_STR).get()),
				()->assertEquals(DOUBLE_DATA, jsonObjDeconstructor.getDoubleByName(DOUBLE_DATA_STR).get()),
				()->assertEquals(INTEGER_DATA, jsonObjDeconstructor.getIntegerByName(INTEGER_DATA_STR).get()),
				()->assertEquals(LONG_DATA, jsonObjDeconstructor.getLongByName(LONG_DATA_STR).get()),
				()->assertEquals(STRING_DATA, jsonObjDeconstructor.getStringByName(STRING_DATA_STR).get()),
				()->assertTrue(jsonObjDeconstructor.getStringByName(NULL_DATA_STR).get().isEmpty()),
				()->assertEquals(8, jsonObjDsl.size())
				);
		
	}

	@Test
	void testAsJson() {
		
		DataSourceList nestedJsonArrayDsl = DataSourceList.builder() 	// Order of entries matters for test verification
				  .add("", BOOLEAN_DATA)
				  .add("", BYTE_ARRAY_DATA)
				  .add("", DOUBLE_DATA)
//				  .add(name, f)		// Float
				  .add("", INTEGER_DATA)
				  .add("", LONG_DATA)
//				  .add("", p)		// Path
				  .add("", STRING_DATA)
				  .build();

		DataSourceList nestedJsonObjDsl = DataSourceList.builder()
											  .add(BOOLEAN_DATA_STR, BOOLEAN_DATA)
											  .add(BYTE_ARRAY_DATA_STR, BYTE_ARRAY_DATA)
											  .add(DOUBLE_DATA_STR, DOUBLE_DATA)
//											  .add(name, f)		// Float
											  .add(INTEGER_DATA_STR, INTEGER_DATA)
											  .add(LONG_DATA_STR, LONG_DATA)
//											  .add(name, p)		// Path
											  .add(STRING_DATA_STR, STRING_DATA)
											  .build();

		DataSourceList jsonArrayDsl = DataSourceList.builder()	// Order of entries matters for test verification
				  .add("", BOOLEAN_DATA)
				  .add("", BYTE_ARRAY_DATA)
				  .add("", DOUBLE_DATA)
//				  .add(name, f)		// Float
				  .add("", INTEGER_DATA)
				  .add("", LONG_DATA)
//				  .add("", p)		// Path
				  .add("", STRING_DATA)
				  .add("", nestedJsonObjDsl)	// DataSourceList of JSON Object
				  .add("", nestedJsonArrayDsl)	// DataSourceList of JSON Array
				  .build();

		DataSourceList jsonObjDsl = DataSourceList.builder()
											  .add(BOOLEAN_DATA_STR, BOOLEAN_DATA)
											  .add(BYTE_ARRAY_DATA_STR, BYTE_ARRAY_DATA)
											  .add(NESTED_JSON_OBJECT_DATA_STR, nestedJsonObjDsl)	// DataSourceList of JSON Object
											  .add(NESTED_JSON_ARRAY_DATA_STR, nestedJsonArrayDsl)	// DataSourceList of JSON Array
											  .add(DOUBLE_DATA_STR, DOUBLE_DATA)
//											  .add(name, f)		// Float
											  .add(INTEGER_DATA_STR, INTEGER_DATA)
											  .add(LONG_DATA_STR, LONG_DATA)
//											  .add(name, p)		// Path
											  .add(STRING_DATA_STR, STRING_DATA)
											  .build();

		DataSourceList srcDsl = DataSourceList.builder()
				  .add(BOOLEAN_DATA_STR, BOOLEAN_DATA)
				  .add(BYTE_ARRAY_DATA_STR, BYTE_ARRAY_DATA)
				  .add(JSON_OBJECT_DATA_STR, jsonObjDsl)	// DataSourceList of JSON Object
				  .add(JSON_ARRAY_DATA_STR, jsonArrayDsl)	// DataSourceList of JSON Array
				  .add(DOUBLE_DATA_STR, DOUBLE_DATA)
//				  .add(name, f)		// Float
				  .add(INTEGER_DATA_STR, INTEGER_DATA)
				  .add(LONG_DATA_STR, LONG_DATA)
//				  .add(name, p)		// Path
				  .add(STRING_DATA_STR, STRING_DATA)
				  .build();
		
		JsonObject result = asJson(srcDsl, logger);
		
		assertNotNull(result);
//		System.out.println(result.toString());
		

		validateJsonObjectScalarContents(result);
		
		JsonObject jsonObject = result.getJsonObject(JSON_OBJECT_DATA_STR);
		validateJsonObjectScalarContents(jsonObject);
		JsonObject objectNestedJsonObject = jsonObject.getJsonObject(NESTED_JSON_OBJECT_DATA_STR);
		validateJsonObjectScalarContents(objectNestedJsonObject);
		JsonArray objectNestedJsonArray = jsonObject.getJsonArray(NESTED_JSON_ARRAY_DATA_STR);
		validateJsonArrayScalarContents(objectNestedJsonArray);

		JsonArray jsonArray = result.getJsonArray(JSON_ARRAY_DATA_STR);
		int count = validateJsonArrayScalarContents(jsonArray);
		JsonObject arrayNestedJsonObject = jsonArray.getJsonObject(count++);
		validateJsonObjectScalarContents(arrayNestedJsonObject);
		JsonArray arrayNestedJsonArray = jsonArray.getJsonArray(count++);
		validateJsonArrayScalarContents(arrayNestedJsonArray);

		
	}

	void validateJsonObjectScalarContents(JsonObject jsonObject) {
		assertEquals(BOOLEAN_DATA, Boolean.valueOf(jsonObject.getString(BOOLEAN_DATA_STR)));
		assertEquals(DOUBLE_DATA, Double.valueOf(jsonObject.getString(DOUBLE_DATA_STR)));
		assertEquals(INTEGER_DATA, Integer.valueOf(jsonObject.getString(INTEGER_DATA_STR)));
		assertEquals(LONG_DATA, Long.valueOf(jsonObject.getString(LONG_DATA_STR)));
		assertEquals(STRING_DATA, jsonObject.getString(STRING_DATA_STR));
		assertArrayEquals(BYTE_ARRAY_DATA, BYTE_DECODER.decode(jsonObject.getString(BYTE_ARRAY_DATA_STR)));	

		// Ideally, this should be...
//		assertEquals(BOOLEAN_DATA, result.getBoolean(BOOLEAN_DATA_STR));
//		assertEquals(DOUBLE_DATA, result.getJsonNumber(DOUBLE_DATA_STR).doubleValue());
//		assertEquals(INTEGER_DATA, result.getInt(INTEGER_DATA_STR));
//		assertEquals(LONG_DATA, result.getJsonNumber(LONG_DATA_STR).longValue());
//		assertEquals(STRING_DATA, result.getString(STRING_DATA_STR));
		
	}

	int validateJsonArrayScalarContents(JsonArray jsonArray) {
		return validateJsonArrayScalarContents(jsonArray, 0);
	}
	
	int validateJsonArrayScalarContents(JsonArray jsonArray, int beginningCount) {
		int count = beginningCount;
		assertEquals(BOOLEAN_DATA, Boolean.valueOf(jsonArray.getString(count++)));
		assertArrayEquals(BYTE_ARRAY_DATA, BYTE_DECODER.decode(jsonArray.getString(count++)));
		assertEquals(DOUBLE_DATA, Double.valueOf(jsonArray.getString(count++)));
		assertEquals(INTEGER_DATA, Integer.valueOf(jsonArray.getString(count++)));
		assertEquals(LONG_DATA, Long.valueOf(jsonArray.getString(count++)));
		assertEquals(STRING_DATA, jsonArray.getString(count++));
		return count;
		
	}
}
