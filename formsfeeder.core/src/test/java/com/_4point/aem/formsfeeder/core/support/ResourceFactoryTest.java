package com._4point.aem.formsfeeder.core.support;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class ResourceFactoryTest {
	static final String RESOURCE_NAME = "TestFiles/FileDS.txt";
	static final byte[] RESOURCE_BYTES;
	static {
		try {
			RESOURCE_BYTES = Files.readAllBytes(Paths.get("src", "test", "resources").resolve(RESOURCE_NAME));
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read RESOURCE_BYTES", e);
		}
	}

	@Test
	void testGetResourceBytes() throws Exception {
		final byte[] result = ResourceFactory.getResourceBytes(this, RESOURCE_NAME);
		assertNotNull(result);
		assertArrayEquals(RESOURCE_BYTES, result);
	}

	@Test
	void testGetResourceWithClassBytes() throws Exception {
		final byte[] result = ResourceFactory.getResourceBytes(ResourceFactoryTest.class, RESOURCE_NAME);
		assertNotNull(result);
		assertArrayEquals(RESOURCE_BYTES, result);
	}

	@Test
	void testGetResourceStream() throws Exception {
		final InputStream result = ResourceFactory.getResourceStream(this, RESOURCE_NAME);
		assertNotNull(result);
		assertArrayEquals(RESOURCE_BYTES, Jdk8Utils.readAllBytes(result));
	}

	@Test
	void testGetResourceWithClassStream() throws Exception {
		final InputStream result = ResourceFactory.getResourceStream(ResourceFactoryTest.class, RESOURCE_NAME);
		assertNotNull(result);
		assertArrayEquals(RESOURCE_BYTES, Jdk8Utils.readAllBytes(result));
	}

	@Test
	void testGetResourcePath() throws Exception {
		final Path result = ResourceFactory.getResourcePath(this, RESOURCE_NAME);
		assertNotNull(result);
		assertTrue(Files.exists(result));
		assertTrue(result.endsWith(RESOURCE_NAME));
		assertEquals(Paths.get("target", "test-classes").resolve(RESOURCE_NAME).toAbsolutePath(), result);
	}

	@Test
	void testGetResourceWithClassPath() throws Exception {
		final Path result = ResourceFactory.getResourcePath(ResourceFactoryTest.class, RESOURCE_NAME);
		assertNotNull(result);
		assertTrue(Files.exists(result));
		assertTrue(result.endsWith(RESOURCE_NAME));
		assertEquals(Paths.get("target", "test-classes").resolve(RESOURCE_NAME).toAbsolutePath(), result);
	}
}
