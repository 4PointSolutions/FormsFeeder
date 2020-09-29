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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import com._4point.aem.formsfeeder.server.support.CorrelationId;

class CorsResponseFilterTest {
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
	private static final String API_V1_PATH = "/api/v1";
	private static final String DEBUG_PLUGIN_PATH = API_V1_PATH + "/Debug";
	
	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
	@TestPropertySource(properties = {"formsfeeder.enable_cors=true"})
	class PositiveTests {
		@LocalServerPort
		private int port;

		private URI uri;

		@BeforeEach
		public void setUp() throws Exception {
			uri = getBaseUri(port);
		}

		@Test
		void testFilter_WithSetting() {
			Response response = ClientBuilder.newClient()
					 .target(uri)
					 .path(DEBUG_PLUGIN_PATH)
					 .request()
					 .get();
			
			assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
			assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
			assertNotNull(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER));
			assertNotNull(response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER));
		}
	}
	
	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
	@TestPropertySource(properties = {"formsfeeder.enable_cors=foobar"})
	class NegativeTests {
		@LocalServerPort
		private int port;

		private URI uri;

		@BeforeEach
		public void setUp() throws Exception {
			uri = getBaseUri(port);
		}

		@Test
		void testFilter_NoSetting() {
			Response response = ClientBuilder.newClient()
					 .target(uri)
					 .path(DEBUG_PLUGIN_PATH)
					 .request()
					 .get();
			
			assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
			assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
			assertNull(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER));
			assertNull(response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER));
		}
	}

	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
	class BadValueTests {
		@LocalServerPort
		private int port;

		private URI uri;

		@BeforeEach
		public void setUp() throws Exception {
			uri = getBaseUri(port);
		}

		@Test
		void testFilter_NoSetting() {
			Response response = ClientBuilder.newClient()
					 .target(uri)
					 .path(DEBUG_PLUGIN_PATH)
					 .request()
					 .get();
			
			assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + DEBUG_PLUGIN_PATH + ")." + getResponseBody(response));
			assertNotNull(response.getHeaderString(CorrelationId.CORRELATION_ID_HDR));
			assertNull(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER));
			assertNull(response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER));
		}
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
