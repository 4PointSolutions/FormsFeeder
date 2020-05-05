package com._4point.aem.formsfeeder.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
	void testInvokeNoParams() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH)
				 .request()
				 .get();
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
	}

	@Test
	void testInvokeNoParams_BadPath() {
		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(DEBUG_PLUGIN_PATH + "_BadPath")
				 .request()
				 .get();
		
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
