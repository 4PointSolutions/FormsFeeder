package com._4point.aem.formsfeeder.server.support;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.slf4j.Logger;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;

public class DataSourceListJsonUtils {

	/**
	 * Convert a Json Object into the DataSourceList
	 * 
	 * @param document
	 * @param logger
	 * @return
	 */
	public static final DataSourceList asDataSourceList(JsonObject document, Logger logger) {
		return addAsDataSource(DataSourceList.builder(), document, logger).build();
	}
	
	/**
	 * Convert a DataSourceList to a Json object.
	 * 
	 * @param dataSourceList
	 * @return FormDataMultipart
	 */
	public static JsonObject asJson(final DataSourceList dataSourceList) {
		//TODO: Fill in the details
		return null;
	}

	private static DataSourceList.Builder addAsDataSource(DataSourceList.Builder builder, JsonObject obj, Logger logger) {
		obj.forEach((k,v)->addAsDataSource(builder, k, v, logger));
		return builder;
	}
	
	private static DataSourceList.Builder addAsDataSource(DataSourceList.Builder builder, String key, JsonArray array, Logger logger) {
		array.forEach(v->addAsDataSource(builder, "", v, logger));
		return builder;
	}
	
	private static DataSourceList.Builder addAsDataSource(DataSourceList.Builder builder, String key, JsonValue value, Logger logger) {
		switch (value.getValueType()) {
		case ARRAY:
			builder.add(key, addAsDataSource(DataSourceList.builder(), key, value.asJsonArray(), logger).build());
			break;
		case OBJECT:
			builder.add(key, addAsDataSource(DataSourceList.builder(), value.asJsonObject(), logger).build());
			break;
		case FALSE:
			builder.add(key, false);
			break;
		case NULL:
			builder.add(key, new byte[0]);	// Empty input stream.
			break;
		case NUMBER:
			JsonNumber num = (JsonNumber)value;
			if (num.isIntegral()) {
				builder.add(key, ((JsonNumber)value).bigIntegerValueExact().toString());
			} else { 
				builder.add(key, ((JsonNumber)value).bigDecimalValue().toString());
			}
			break;
		case STRING:
			builder.add(key, ((JsonString)value).getString());
			break;
		case TRUE:
			builder.add(key, true);
			break;
		default:
			throw new IllegalArgumentException("Unexpected Value Type '" + value.getValueType().toString() + "'.");
		}
		return builder;
	}
}
