package com._4point.aem.formsfeeder.server.db;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import com._4point.aem.formsfeeder.server.Application;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
@TestPropertySource(properties = {
		 // First data source
		 "spring.datasource.datasource1.url=jdbc:h2:mem:testh2db1", 
		 "spring.datasource.datasource1.username=sa", 
		 "spring.datasource.datasource1.password=sa", 
		 "spring.datasource.datasource1.driver-class-name=org.h2.Driver",
		 // Second data source
		 "spring.datasource.datasource2.url=jdbc:h2:mem:testh2db2", 
		 "spring.datasource.datasource2.username=sa", 
		 "spring.datasource.datasource2.password=sa", 
		 "spring.datasource.datasource2.driver-class-name=org.h2.Driver",
		 // Third data source
		 "spring.datasource.datasource3.url=jdbc:h2:mem:testh2db3", 
		 "spring.datasource.datasource3.username=sa", 
		 "spring.datasource.datasource3.password=sa", 
		 "spring.datasource.datasource3.driver-class-name=org.h2.Driver"
		})
class JdbcConfigurationTest {
	private static final String API_V1_PATH = "/api/v1";
	private static final String JDBC_PLUGIN_PATH = API_V1_PATH + "/Jdbc";

	private static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	private static final Path ACTUAL_RESULTS_DIR = RESOURCES_FOLDER.resolve("ActualResults");

	private static final boolean SAVE_RESULTS = false;
	static {
		if (SAVE_RESULTS) {
			try {
				Files.createDirectories(ACTUAL_RESULTS_DIR);
			} catch (IOException e) {
				// eat it, we don't care.
			}
		}
	}


	@LocalServerPort
	private int port;

	private URI uri;

	@BeforeEach
	public void setUp() throws Exception {
		uri = getBaseUri(port);
	}


	@Test
	void testJdbcConfiguration() throws Exception {
		List<String> providedNames = List.of("John Woo", "Jeff Dean", "Josh Bloch", "Josh Long");

		Response response = ClientBuilder.newClient()
				 .target(uri)
				 .path(JDBC_PLUGIN_PATH)
				 .queryParam("name", providedNames.toArray())
				 .request()
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .get();

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), ()->"Unexpected response status returned from URL (" + JDBC_PLUGIN_PATH + ")." + getResponseBody(response));
		assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()), "Expected response media type (" + response.getMediaType().toString() + ") to be compatible with 'application/json'.");
		String responseStr = response.readEntity(String.class);
		if (SAVE_RESULTS) {
			Files.writeString(ACTUAL_RESULTS_DIR.resolve("testJdbcConfiguration_results.json"), responseStr);
		}
		String expectedJson = "{\"JdbcDataSource1\":{\"CustomerRecord\":[{\"id\":\"3\",\"firstName\":\"Josh\",\"lastName\":\"Bloch1\"},{\"id\":\"4\",\"firstName\":\"Josh\",\"lastName\":\"Long1\"}]},\"JdbcDataSource2\":{\"CustomerRecord\":[{\"id\":\"3\",\"firstName\":\"Josh\",\"lastName\":\"Bloch2\"},{\"id\":\"4\",\"firstName\":\"Josh\",\"lastName\":\"Long2\"}]},\"JdbcDataSource3\":{\"CustomerRecord\":[{\"id\":\"3\",\"firstName\":\"Josh\",\"lastName\":\"Bloch3\"},{\"id\":\"4\",\"firstName\":\"Josh\",\"lastName\":\"Long3\"}]}}";
		JSONAssert.assertEquals(expectedJson, responseStr, false);
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
