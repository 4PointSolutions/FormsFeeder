package com._4point.aem.formsfeeder.core.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import com._4point.aem.formsfeeder.core.api.FeedConsumer.FeedConsumerInternalErrorException;

/**
 * This class is a helper class that provides access to resources stored in a plugin's resource directory.
 * 
 * <p>The resources stored in a plugins directory are zipped, so this class provides routines that locate and decode
 * those resources.</p?
 *
 */
public enum ResourceFactory {
	INSTANCE;
	
	private FileSystem zipfs = null;	// Used to hold ZipFs so that we can read our .jar resources using FileSystem

	/**
	 * Retrieve the contents of a resource as a byte array.
	 * 
	 * @param context An object to retrieve a classloader from.  Most classes will pass {@code this}. 
	 * @param resourceName Name of resource (i.e. the location of the resource under the resources directory).
	 * @return a byte[] containing the contents of the resource
	 * @throws FeedConsumerInternalErrorException
	 */
	public static final byte[] getResourceBytes(Object context, String resourceName) throws FeedConsumerInternalErrorException {
		try {
			return Files.readAllBytes(getResourcePath(context, resourceName));
		} catch (IOException e) {
			throw new FeedConsumerInternalErrorException("Problem reading bytes from resource (" + resourceName + ").", e);
		}
	}

	/**
	 * Retrieve the contents of a resource as an InputStream..
	 * 
	 * @param context An object to retrieve a classloader from.  Most classes will pass {@code this}. 
	 * @param resourceName Name of resource (i.e. the location of the resource under the resources directory).
	 * @return an InputStream that can be used to read the contents of the resource
	 * @throws FeedConsumerInternalErrorException
	 */
	public static final InputStream getResourceStream(Object context, String resourceName) throws FeedConsumerInternalErrorException {
		try {
			return Files.newInputStream(getResourcePath(context, resourceName));
		} catch (IOException e) {
			throw new FeedConsumerInternalErrorException("Problem opening input stream to resource (" + resourceName + ").", e);
		}
	}

	/**
	 * Retrieve the Path to a resource.  The Path can then be used to read the resource.
	 * 
	 * @param context An object to retrieve a classloader from.  Most classes will pass {@code this}. 
	 * @param resourceName Name of resource (i.e. the location of the resource under the resources directory).
	 * @return a Path object to the resource
	 * @throws FeedConsumerInternalErrorException
	 */
	public static final Path getResourcePath(Object context, String resourceName) throws FeedConsumerInternalErrorException {
		URL jarResource = context.getClass().getClassLoader().getResource(resourceName);
		if (jarResource == null) {
			throw new FeedConsumerInternalErrorException("Problem locating resource (" + resourceName + ").");
		} else {
			try {
				URI uri = jarResource.toURI();
				loadZipFsIfRequired(uri);
				return Paths.get(uri);
			} catch (URISyntaxException | IOException e) {
				throw new FeedConsumerInternalErrorException("Problem with converting jar resource to path. (" + e.getMessage() + ")", e);
			}
		}
	}

	// Since the zipfs member variable is mutable, we need to prevent race conditions in a multi-threaded scenario.
	// We do this by making sure that all accesses to that variable are from within a synchronized method.
	private static synchronized void loadZipFsIfRequired(URI uri) throws IOException {
		if (INSTANCE.zipfs == null && (uri.toString().startsWith("/") || uri.toString().startsWith("jar:"))) {
			try {
				INSTANCE.zipfs = FileSystems.getFileSystem(uri);
			} catch (FileSystemNotFoundException e) {
				// File system doesn't exist, so create it.
				INSTANCE.zipfs = FileSystems.newFileSystem(uri,  Collections.singletonMap("create", "true"));
			}
		}
	}
}
