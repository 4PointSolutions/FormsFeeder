package com._4point.aem.formsfeeder.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;

/**
 * Submit Proxy Utils class is a set of utilities common across the various SubmitProxy classes.
 *
 */
public class SubmitProxyUtils {

	/**
	 * Transforms a FormDataMultiPart object using a set of provided functions.
	 * 
	 * Accepts incoming form data, in the form of a FormDataMultiPart object and a Map collection of functions.  It walks through the
	 * parts and if it finds a function in the Map with the same name it executes that function on the the data from the corresponding part.
	 * It accumulates and returns the result in another FormDataMultiPart object.
	 * 
	 * @param inFormData	incoming form data
	 * @param fieldFunctions	set of functions that correspond to specific parts
	 * @param logger	logger for logging messages
	 * @return
	 * @throws IOException
	 */
	public static FormDataMultiPart transformFormData(final FormDataMultiPart inFormData, final Map<String, Function<byte[], byte[]>> fieldFunctions, Logger logger) throws IOException {
		FormDataMultiPart outFormData = new FormDataMultiPart();
    	var fields = inFormData.getFields();
		logger.debug("Found " + fields.size()  + " fields");
    	
		for (var fieldEntry : fields.entrySet()) {
			String fieldName = fieldEntry.getKey();
			for (FormDataBodyPart fieldData : fieldEntry.getValue()) {
				logger.debug("Copying '" + fieldName  + "' field");
				byte[] fieldBytes = ((BodyPartEntity)fieldData.getEntity()).getInputStream().readAllBytes();
				logger.trace("Fieldname '" + fieldName + "' is '" + new String(fieldBytes) + "'.");
				var fieldFn = fieldFunctions.getOrDefault(fieldName, Function.identity());	// Look for an entry in fieldFunctions table for this field.  Return the Identity function if we don't find one.
				byte[] modifiedFieldBytes = fieldFn.apply(fieldBytes);
				if (modifiedFieldBytes != null) {	// If the function returned bytes (if not, then remove that part)
					outFormData.field(fieldName, new String(modifiedFieldBytes, StandardCharsets.UTF_8));	// Apply the field function to bytes.
				}
			}
		}
		return outFormData;
	}

	/**
	 * Transfers a response from AEM and returns it in a byte array.  It handles chunked responses.
	 * 
	 * @param result	Response object from AEM
	 * @param logger	Logger for logging any errors/warnings/etc.
	 * @return
	 * @throws IOException
	 */
	public static byte[] transferFromAem(Response result, Logger logger) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("AEM Response Mediatype=" + (result.getMediaType() != null ? result.getMediaType().toString(): "null"));
			MultivaluedMap<String, Object> headers = result.getHeaders();
			for(Entry<String, List<Object>> entry : headers.entrySet()) {
				String msgLine = "For header '" + entry.getKey() + "', ";
				for (Object value : entry.getValue()) { 
					msgLine += "'" + value.toString() + "' ";
				}
				logger.debug(msgLine);
			}
		}
		
		String aemResponseEncoding = result.getHeaderString("Transfer-Encoding");
		if (aemResponseEncoding != null && aemResponseEncoding.equalsIgnoreCase("chunked")) {
			// They've sent back chunked response.
			logger.debug("Found a chunked encoding.");
			final ChunkedInput<byte[]> chunkedInput = result.readEntity(new GenericType<ChunkedInput<byte[]>>() {});
			byte[] chunk;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			try (buffer) {
				while ((chunk = chunkedInput.read()) != null) {
					buffer.writeBytes(chunk);
					logger.debug("Read chunk from AEM response.");
				}
			}
			
			return buffer.toByteArray();
		} else {
			return ((InputStream)result.getEntity()).readAllBytes();
		}
	}


}
