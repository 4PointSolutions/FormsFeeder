package com._4point.aem.formsfeeder.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

class TestConstants {
	static final String ENV_FORMSFEEDER_AEM_PORT = "formsfeeder.aem.port";
	static final String ENV_FORMSFEEDER_AEM_HOST = "formsfeeder.aem.host";
	static final MediaType APPLICATION_PDF = new MediaType("application", "pdf");
	static final MediaType APPLICATION_XDP = new MediaType("application", "vnd.adobe.xdp+xml");

	static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	static final Path SAMPLE_FILES_DIR = RESOURCES_FOLDER.resolve("SampleFiles");
	static final Path ACTUAL_RESULTS_DIR = RESOURCES_FOLDER.resolve("ActualResults");

	private TestConstants() {
		// Prevent instantiation.
	}

	static URI getBaseUri(int port) throws URISyntaxException {
		return new URI("http://localhost:" + port);
	}
	
	static String getResponseBody(Response response) {
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
