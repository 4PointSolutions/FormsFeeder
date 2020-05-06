package com._4point.aem.formsfeeder.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
class ServicesEndpointTest {

	private static final String SERVICE_PATH = "/services";
	private static final String DEBUG_PLUGIN_PATH = SERVICE_PATH + "/Debug";
	
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
	void testInvokePostNoQueryParamsNoBody() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .post(null);
		
		assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
	}


	@Test
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
		String responseBody = getResponseBody(response);
		assertTrue(responseBody.contains(expectedParamName), "Expected response body to contain '" + expectedParamName + "', but was '" + responseBody + "'.");
		assertTrue(responseBody.contains(expectedParamValue), "Expected response body to contain '" + expectedParamValue + "', but was '" + responseBody + "'.");
	}

	@Test
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
	void testInvokeGetNoParams_BadPath() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH + "_BadPath")
				 .request()
				 .get();
		
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
	}

	@Test
	void testInvokePostNoParams_BadPath() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH + "_BadPath")
				 .request()
				 .post(null);
		
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
	}

	@Disabled
	void testInvokeWithParams() {
		fail("Not yet implemented");
	}

	public static URI getBaseUri(int port) throws URISyntaxException {
		return new URI("http://localhost:" + port);
	}
	
	public static String getResponseBody(Response response) {
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
