package com._4point.aem.formsfeeder.server.support;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListDecoder;

public class DataSourceListJsonUtils {
	static final Encoder BYTE_ENCODER = Base64.getEncoder();

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
	
	// Add JsonObject
	private static DataSourceList.Builder addAsDataSource(DataSourceList.Builder builder, JsonObject obj, Logger logger) {
		obj.forEach((k,v)->addAsDataSource(builder, k, v, logger));
		return builder;
	}
	
	// Add JsonArray
	private static DataSourceList.Builder addAsDataSource(DataSourceList.Builder builder, String key, JsonArray array, Logger logger) {
		array.forEach(v->addAsDataSource(builder, "", v, logger));
		return builder;
	}
	
	// Add JsonValue
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
	
	/**
	 * Convert a DataSourceList to a Json object.
	 * 
	 * @param dataSourceList
	 * @return FormDataMultipart
	 */
	public static JsonObject asJson(final DataSourceList dataSourceList, Logger logger) {
		return asJsonObjectBuilder(dataSourceList.list(), logger).build();
	}

	// Define an interface that we will use for both JsonObjects and JsonArrays for adding children
	private static interface JsonParent {
		void add(JsonObjectBuilder objBuilder);
		void add(JsonArrayBuilder arrayConsumer);
		void add(String str);
		// Ideally we would add other add() methods for other kinds of objects but we're not quite there yet.
	}
	
	private static JsonObjectBuilder asJsonObjectBuilder(final List<DataSource> dataSourceList, Logger logger) {
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		dataSourceList.forEach(ds->addAsJsonObject(toJsonParent(jsonBuilder, ds.name()), ds, logger));
		return jsonBuilder;
	}

	private static JsonArrayBuilder asJsonArrayBuilder(final List<DataSource> dataSourceList, Logger logger) {
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		dataSourceList.forEach(ds->addAsJsonObject(toJsonParent(jsonBuilder), ds, logger));
		return jsonBuilder;
	}

	private static void addAsJsonObject(JsonParent jsonParent, final DataSource dataSource, Logger logger) {
		try {
			switch(dataSource.contentType().asTypeString()) {
			case XmlDataSourceListDecoder.DSL_MIME_TYPE_STR:
				XmlDataSourceListDecoder.wrap(dataSource.inputStream())
										.decode()
										.ifPresent(dsl->asJsonObjectOrArray(jsonParent, dsl.list(), logger));
				break;
			case StandardMimeTypes.TEXT_PLAIN_STR:
				Charset charset = dataSource.contentType().charset();
				jsonParent.add(new String(dataSource.inputStream().readAllBytes(), charset != null ? charset : StandardCharsets.UTF_8));
				break;
			default:
				// Treat it as byte stream and base64 encode it into a String object.
				jsonParent.add(BYTE_ENCODER.encodeToString(dataSource.inputStream().readAllBytes()));
			}
		} catch (IOException | XMLStreamException | FactoryConfigurationError e) {
			String msg = e.getMessage();
			throw new IllegalStateException("Exception while converting datasources to JSON (" + (msg == null ? "null" : msg) + ").", e);
		}
	}
	
	private static void asJsonObjectOrArray(JsonParent jsonParent, final List<DataSource> dataSourceList, Logger logger) {
		// Partition the list into two lists, one containing entries with a blank name and one without
		Map<Boolean, List<DataSource>> partitionedLists = dataSourceList.stream().collect(Collectors.partitioningBy(ds->ds.name().isBlank()));
		
		// Add the blank-named entries into a Json array 
		List<DataSource> arrayList = partitionedLists.get(Boolean.TRUE);
		if (!arrayList.isEmpty()) {
			jsonParent.add(asJsonArrayBuilder(arrayList, logger));
		}
		
		// Add the non-blank-named entries into Json object
		List<DataSource> objectList = partitionedLists.get(Boolean.FALSE);
		if (!objectList.isEmpty()) {
			jsonParent.add(asJsonObjectBuilder(objectList, logger));
		}
	}
	
	private static JsonParent toJsonParent(JsonObjectBuilder builder, String name) {
		return new JsonParent() {
			@Override
			public void add(JsonObjectBuilder objBuilder) {
				builder.add(name, objBuilder);
			}

			@Override
			public void add(JsonArrayBuilder arrayBuilder) {
				builder.add(name, arrayBuilder);
			}

			@Override
			public void add(String str) {
				builder.add(name, str);
			}
		};
	}

	private static JsonParent toJsonParent(JsonArrayBuilder builder) {
		return new JsonParent() {
			@Override
			public void add(JsonObjectBuilder objBuilder) {
				builder.add(objBuilder);
			}
	
			@Override
			public void add(JsonArrayBuilder arrayBuilder) {
				builder.add(arrayBuilder);
			}
	
			@Override
			public void add(String str) {
				builder.add(str);
			}
		};
	}
}
