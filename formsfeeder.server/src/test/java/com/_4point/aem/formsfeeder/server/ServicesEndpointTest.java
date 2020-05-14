package com._4point.aem.formsfeeder.server;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;

import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.server.support.CorrelationId;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
class ServicesEndpointTest {

	private static final MediaType APPLICATION_PDF = MediaType.valueOf("application/pdf");
	private static final String API_V1_PATH = "/api/v1";
	private static final String DEBUG_PLUGIN_PATH = API_V1_PATH + "/Debug";
	private static final String MOCK_PLUGIN_PATH = API_V1_PATH + "/Mock";
	
	private static final String BODY_DS_NAME = "formsfeeder:BodyBytes";
	private static final String MOCK_PLUGIN_SCENARIO_NAME = "scenario";
	
	@LocalServerPort
	private int port;

	private URI uri;

	@BeforeEach
	public void setUp() throws Exception {
		uri = getBaseUri(port);
	}
	
	@Test
	void testInvokeGetNoParams() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .get();
		
		assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	@Test
	void testInvokeGetOneParams() {
		String expectedParamName = "QueryParam1";
		String expectedParamValue = "QueryParam1 Value";
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName, expectedParamValue)
				 .request()
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + responseBody + "'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	@Test
	void testInvokeGetManyParams() {
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(body.getMediaType()), "Expected response media type (" + body.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(3, returnsCount);
	}

	// The "No Body" tests are currently disabled because they are producing an "status code 500 Internal Error" result.
	// Ideally, I'd like to find a way around this.  I'd prefer to allow for this however it looks like Jersey tries to
	// parse the input as multipart/form-data and fails.
	@Disabled
	void testInvokePostNoQueryParamsNoBody() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(null);
		
		assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}


	@Disabled
	void testInvokePostOneQueryParamNoBody() {
		String expectedParamName = "QueryParam1";
		String expectedParamValue = "QueryParam1 Value";
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName, expectedParamValue)
				 .request()
				 .post(null);
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + responseBody + "'.");
	}

	@Disabled
	void testInvokePostManyQueryParamsNoBody() {
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .post(null);
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(body.getMediaType()), "Expected response media type (" + body.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(3, returnsCount);
	}

	
	@Test
	void testInvokePostNoQueryParamsOneBodyParamOctetStream() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(InputStream.nullInputStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(BODY_DS_NAME), "Expected response body to contain '" + BODY_DS_NAME + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(MediaType.APPLICATION_OCTET_STREAM), "Expected response body to contain '" + MediaType.APPLICATION_OCTET_STREAM + "', but was '" + responseBody + "'.");
	}

	@Test
	void testInvokePostNoQueryParamsOneBodyParamText() {
		String expectedBodyText = "This is some text.";
		
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(new StringReader(expectedBodyText), MediaType.TEXT_PLAIN_TYPE));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedBodyText), "Expected response body to contain '" + expectedBodyText + "', but was '" + responseBody + "'.");
	}

	@Test
	void testInvokePostOneQueryParamOneBodyParam() {
		String expectedParamName = "QueryParam1";
		String expectedParamValue = "QueryParam1 Value";
		String expectedBodyText = "Body Text Value";
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName, expectedParamValue)
				 .request()
				 .post(Entity.text(expectedBodyText));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				MediaType mediaType = body.getMediaType();
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains(BODY_DS_NAME)) {
					// Contains the body text because the body of the post was text.
					assertTrue(value.contains(expectedBodyText), "Expected response body to contain '" + expectedBodyText + "', but was '" + value + "'.");
				} else {
					assertTrue(value.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + value + "'.");
					assertTrue(value.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + value + "'.");
				}
			}
		}
		assertEquals(2, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsOneBodyParam() {
		String expectedBodyText = "<root>Body Text Value</root>";
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .post(Entity.xml(expectedBodyText));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				MediaType mediaType = body.getMediaType();
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains(BODY_DS_NAME)) {
					// Contains the content type because the body of the post was not text.
					assertTrue(value.contains(MediaType.APPLICATION_XML), "Expected response body to contain '" + MediaType.APPLICATION_XML + "', but was '" + value + "'.");
				} else {
					assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
					assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
				}
			}
		}
		assertEquals(4, returnsCount);
	}

	@Test
	void testInvokePostNoQueryParamsOneFormParam() {
		String expectedParamName = "BodyParam1";
		String expectedParamValue = "BodyParam1 Value";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedParamName, expectedParamValue);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + responseBody + "'.");
	}

	@Test
	void testInvokePostNoQueryParamsManyFormParams() {
		String bodyParamString = "BodyParam";
		String bodyValueString = "Value";
		String expectedParamName1 = bodyParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + bodyValueString;
		String expectedParamName2 = bodyParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + bodyValueString;
		String expectedParamName3 = bodyParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + bodyValueString;

		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedParamName1, expectedParamValue1);
		bodyData.field(expectedParamName2, expectedParamValue2);
		bodyData.field(expectedParamName3, expectedParamValue3);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(body.getMediaType()), "Expected response media type (" + body.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				assertTrue(value.contains(bodyParamString), "Expected response body to contain '" + bodyParamString + "', but was '" + value + "'.");
				assertTrue(value.contains(bodyValueString), "Expected response body to contain '" + bodyValueString + "', but was '" + value + "'.");
			}
		}
		assertEquals(3, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsOneFormParam() {
		String expectedBodyParamName = "BodyParam1";
		String expectedBodyParamValue = "BodyParam1 Value";
		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedQueryParamName1 = queryParamString + "1";
		String expectedQueryParamValue1 = expectedQueryParamName1 + " " + queryValueString;
		String expectedQueryParamName2 = queryParamString + "2";
		String expectedQueryParamValue2 = expectedQueryParamName2 + " " + queryValueString;
		String expectedQueryParamName3 = queryParamString + "3";
		String expectedQueryParamValue3 = expectedQueryParamName3 + " " + queryValueString;
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedBodyParamName, expectedBodyParamValue);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedQueryParamName1, expectedQueryParamValue1)
				 .queryParam(expectedQueryParamName2, expectedQueryParamValue2)
				 .queryParam(expectedQueryParamName3, expectedQueryParamValue3)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				
				MediaType mediaType = body.getMediaType();
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains("Body")) {
					assertTrue(value.contains(expectedBodyParamName), "Expected response body to contain '" + expectedBodyParamName + "', but was '" + value + "'.");
					assertTrue(value.contains(expectedBodyParamValue), "Expected response body to contain '" + expectedBodyParamValue + "', but was '" + value + "'.");
				} else {
					assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
					assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
				}
			}
		}
		assertEquals(4, returnsCount);
	}

	@Test
	void testInvokePostManyQueryParamsManyFormParams() {
		String bodyParamString = "BodyParam";
		String bodyValueString = "Value";
		String expectedBodyParamName1 = bodyParamString + "1";
		String expectedBodyParamValue1 = expectedBodyParamName1 + " " + bodyValueString;
		String expectedBodyParamName2 = bodyParamString + "2";
		String expectedBodyParamValue2 = expectedBodyParamName2 + " " + bodyValueString;
		String expectedBodyParamName3 = bodyParamString + "3";
		String expectedBodyParamValue3 = expectedBodyParamName3 + " " + bodyValueString;
		String expectedBodyParamName4 = bodyParamString + "4";
		InputStream expectedBodyParamValue4 = (InputStream)(new ByteArrayInputStream("<root>Body Text Value</root>".getBytes(StandardCharsets.UTF_8)));

		String queryParamString = "QueryParam";
		String queryValueString = "Value";
		String expectedParamName1 = queryParamString + "1";
		String expectedParamValue1 = expectedParamName1 + " " + queryValueString;
		String expectedParamName2 = queryParamString + "2";
		String expectedParamValue2 = expectedParamName2 + " " + queryValueString;
		String expectedParamName3 = queryParamString + "3";
		String expectedParamValue3 = expectedParamName3 + " " + queryValueString;

		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(expectedBodyParamName1, expectedBodyParamValue1);
		bodyData.field(expectedBodyParamName2, expectedBodyParamValue2);
		bodyData.field(expectedBodyParamName3, expectedBodyParamValue3);
		bodyData.field(expectedBodyParamName4, expectedBodyParamValue4, MediaType.APPLICATION_XML_TYPE);

		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .queryParam(expectedParamName1, expectedParamValue1)
				 .queryParam(expectedParamName2, expectedParamValue2)
				 .queryParam(expectedParamName3, expectedParamValue3)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				
				MediaType mediaType = body.getMediaType();
				assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
				String value = body.getEntityAs(String.class);
				if (value.contains("Body")) {
					if (value.contains("BodyParam4")) {
						// Look for the XML parameter
						
					} else {
						assertTrue(value.contains(bodyParamString), "Expected response body to contain '" + bodyParamString + "', but was '" + value + "'.");
						assertTrue(value.contains(bodyValueString), "Expected response body to contain '" + bodyValueString + "', but was '" + value + "'.");
					}
				} else {
					assertTrue(value.contains(queryParamString), "Expected response body to contain '" + queryParamString + "', but was '" + value + "'.");
					assertTrue(value.contains(queryValueString), "Expected response body to contain '" + queryValueString + "', but was '" + value + "'.");
				}
			}
		}
		assertEquals(7, returnsCount);
	}

	// Use the Mock plugin to test some Exception scenarios
	
	// Cannot throw exceptions from the plugin at this time.  I am looking into the issue.
	@Test
	void testInvokePost_BadRequestExceptionFromPlugin() {
		String scenarioName = "BadRequestException";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Bad Request")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	@Test
	void testInvokePost_InternalServerExceptionFromPlugin() {
		String scenarioName = "InternalErrorException";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Internal Server Error")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	@Test
	void testInvokePost_UncheckedExceptionFromPlugin() {
		String scenarioName = "UncheckedException";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Error within Plugin processor")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	@Test
	void testInvokePost_OtherFeedConsumerExceptionFromPlugin() {
		String scenarioName = "OtherFeedConsumerException";
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, scenarioName);
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		String responseBody = getResponseBody(response);
		assertNotNull(responseBody);
		assertAll(
				()->assertTrue(responseBody.contains("Plugin processor error")),
				()->assertTrue(responseBody.contains(scenarioName))
				);
	}

	@Test
	void testInvokePost_ReturnPdfFromPlugin() throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "ReturnPdf");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(APPLICATION_PDF.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		assertTrue(response.hasEntity(), "Expected response to have entity");
		PDDocument pdf = PDDocument.load((InputStream)response.getEntity());
		PDDocumentCatalog catalog = pdf.getDocumentCatalog();
		assertNotNull(pdf);
		assertNotNull(catalog);
	}

	@Test
	void testInvokePost_ReturnXmlFromPlugin() throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "ReturnXml");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_XML_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/xml'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		assertTrue(response.hasEntity(), "Expected response to have entity");
		XML xml = new XMLDocument((InputStream)response.getEntity());
		assertEquals(2, Integer.valueOf(xml.xpath("count(//form1/*)").get(0)));
	}
	
	@Test
	void testInvokePost_ReturnPdfAndXmlFromPlugin() throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "ReturnPdfAndXml");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'text/plain'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		
		FormDataMultiPart readEntity = response.readEntity(FormDataMultiPart.class);
		Map<String, List<FormDataBodyPart>> fields = readEntity.getFields();
		int returnsCount = 0;
		for (Entry<String, List<FormDataBodyPart>> field : fields.entrySet()) {
			for (var body : field.getValue()) {
				returnsCount++;
				
				MediaType mediaType = body.getMediaType();
				if (APPLICATION_PDF.isCompatible(mediaType)) {
					PDDocument pdf = PDDocument.load(body.getEntityAs(InputStream.class));
					PDDocumentCatalog catalog = pdf.getDocumentCatalog();
					assertNotNull(pdf);
					assertNotNull(catalog);
				} else if (MediaType.APPLICATION_XML_TYPE.isCompatible(mediaType)) {
					XML xml = new XMLDocument(body.getEntityAs(InputStream.class));
					assertEquals(2, Integer.valueOf(xml.xpath("count(//form1/*)").get(0)));
					
				} else {
					fail("Found unexpected mediaType in response '" + mediaType.toString() + "'.");
				}
			}
		}
		assertEquals(2, returnsCount, "Expected 2 parts in the response.");
	}
	
	@Test
	void testInvokePost_ReturnConfigValueFromPlugin() throws Exception {
		
		FormDataMultiPart bodyData = new FormDataMultiPart();
		bodyData.field(MOCK_PLUGIN_SCENARIO_NAME, "ReturnConfigValue");
		
		Response response = ClientBuilder.newClient()
				 .register(MultiPartFeature.class)
				 .target(uri)
				 .path(MOCK_PLUGIN_PATH)
				 .request()
				 .post(Entity.entity(bodyData, bodyData.getMediaType()));
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + MOCK_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/xml'.");
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
		assertTrue(response.hasEntity(), "Expected response to have entity");
		String returnedConfigValue = new String(((InputStream)response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
		assertEquals("FromApplicationProperties", returnedConfigValue, "Expected the Config Value to match the value in application.properties (\"FromApplicationProperties\")." );
	}
		
	@Test
	void testInvokeGetNoParams_BadPath() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH + "_BadPath")
				 .request()
				 .get();
		
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}

	@Test
	void testInvokePostNoParams_BadPath() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH + "_BadPath")
				 .request()
				 .post(Entity.entity(InputStream.nullInputStream(), MediaType.TEXT_PLAIN_TYPE));
		
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
		assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
	}


	private static URI getBaseUri(int port) throws URISyntaxException {
		return new URI("http://localhost:" + port);
	}
	
	private static String getResponseBody(Response response) {
		final MediaType ANY_TEXT_MEDIATYPE = new MediaType("text", "*");

		if (!response.hasEntity()) {
			return "";
		} else if (!ANY_TEXT_MEDIATYPE.isCompatible(response.getMediaType()))  {
			return "Returned mediatype='" + response.getMediaType().toString() + "'.";
		} else {
			try {
				return new String(((InputStream)response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				return "Returned mediatype='" + response.getMediaType().toString() + "', but unable to display entity.";
			}
		}
	}



}
