package formsfeeder.client.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;

public class DataSourceListJaxRsUtils {

	/**
	 * Protected from instantiation.
	 */
	private DataSourceListJaxRsUtils() {
		
	}

	/**
	 * Converts the incoming multipart/form-data into a DataSourceList so that they can be processed by a plug-in
	 * 
	 * @param formData
	 * @return
	 * @throws IOException 
	 */
	public static final DataSourceList asDataSourceList(final FormDataMultiPart formData, final Logger logger) throws IOException {
		Builder builder = DataSourceList.builder();
		for (Entry<String, List<FormDataBodyPart>> entry : formData.getFields().entrySet()) {
			String name = entry.getKey();
			for(FormDataBodyPart part : entry.getValue()) {
				if (part.isSimple()) {
					logger.debug("Found simple Form Data Part '" + name + "' (" + part.getName() + ").");
					builder.add(name, part.getValue());
				} else {
					logger.debug("Found complex Form Data Part '" + name + "' (" + part.getName() + ").");
					ContentDisposition contentDisposition = part.getContentDisposition();
					String fileName = contentDisposition.getFileName();
					if (logger.isDebugEnabled()) {
						Date creationDate = contentDisposition.getCreationDate();
						Date modificationDate = contentDisposition.getModificationDate();
						Date readDate = contentDisposition.getReadDate();
						logger.debug("    Filename='" + fileName + "'.");
						logger.debug("    CreationDate='" + (creationDate != null ? creationDate.toString() : "null") + "'.");
						logger.debug("    ModificationDate='" + (modificationDate != null ? modificationDate : "null") + "'.");
						logger.debug("    ReadDate='" + (readDate != null ? readDate : "null") + "'.");
					}
					// TODO: This is a naive implementation that just reads the whole InputStream into memory.  Should fix this.
					if (fileName != null) {
						builder.add(name, part.getEntityAs(InputStream.class).readAllBytes(), asMimeType(part.getMediaType()), Paths.get(fileName));
					} else {
						builder.add(name, part.getEntityAs(InputStream.class).readAllBytes(), asMimeType(part.getMediaType()));
					}
				}
			}
		}
		return builder.build();
	}
	
	/**
	 * Converts a FormsFeeder core MimeType object into JAX-RS MediaType object.
	 * 
	 * @param mimeType
	 * @return
	 */
	public static final MediaType asMediaType(final MimeType mimeType) {
		Charset charset = mimeType.charset();
		if (charset != null) {
			return new MediaType(mimeType.type(), mimeType.subtype(), charset.name());
		} else {
			return new MediaType(mimeType.type(), mimeType.subtype());
		}
	}
	
	/**
	 * Converts a JAX-RS MediaType object into FormsFeeder core MimeType object.
	 * 
	 * @param mediaType
	 * @return
	 */
	public static final MimeType asMimeType(final MediaType mediaType) {
		return MimeType.of(mediaType.toString());
	}

	/**
	 * Convert a DataSourceList to a FormDataMultipart object.
	 * 
	 * @param dataSourceList
	 * @return FormDataMultipart
	 */
	public static FormDataMultiPart asFormDataMultipart(final DataSourceList dataSourceList) {
		FormDataMultiPart responsesData = new FormDataMultiPart();
		for(var dataSource : dataSourceList.list()) {
			addFormDataPart(responsesData, dataSource);
		}
		return responsesData;
	}

	/**
	 * Add FormDataPart to the FormDataMultiPart response based on the DataSource.
	 * 
	 * @param responsesData
	 * @param dataSource
	 * @return
	 */
	private static final FormDataMultiPart addFormDataPart(FormDataMultiPart responsesData, DataSource dataSource) {
		Optional<java.nio.file.Path> optFilename = dataSource.filename();
		if (optFilename.isPresent()) {
			java.nio.file.Path filename = optFilename.get();
			FormDataContentDisposition cd = FormDataContentDisposition.name(dataSource.name()).fileName(filename.getFileName().toString()).build();
			responsesData.bodyPart(new FormDataBodyPart(cd, dataSource.inputStream(), asMediaType(dataSource.contentType())));
		} else {
			responsesData.field(dataSource.name(), dataSource.inputStream(), asMediaType(dataSource.contentType()));
		}
		return responsesData;
	}

	/**
	 * This is a rather naive implementation.  It reads the incoming data into memory.
	 * 
	 * A better implementation would create a DataSource from the inputStream but that would require a little more
	 * work, so I am postponing that until later.
	 * 
	 * TODO: Create a better implementation for this.
	 * 
	 * @param in
	 * @param contentType
	 * @return
	 * @throws IOException
	 */
	public static final DataSourceList asDataSourceList(final InputStream in, final MediaType contentType, final ContentDisposition contentDisposition, final String dataSourceName, final Logger logger) throws IOException {
		logger.debug("Found Body Parameter of type '" + contentType.toString() + "'.");
		String filename = contentDisposition != null ? contentDisposition.getFileName() : null;
		if (filename != null) {
			return DataSourceList.builder().add(dataSourceName, in.readAllBytes(), asMimeType(contentType), Paths.get(filename)).build();
		} else {
			return DataSourceList.builder().add(dataSourceName, in.readAllBytes(), asMimeType(contentType)).build();
		}
	}

	/**
	 * Convert a Response into a DataSourceList object.
	 * 
	 * @param response
	 * @param dataSourceName
	 * @param logger
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public static final DataSourceList asDataSourceList(Response response, final String dataSourceName, final Logger logger) throws IOException, ParseException {
		String headerString = response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
		var contentDisposition = headerString != null ? new ContentDisposition(headerString) : null;
		return asDataSourceList((InputStream)response.getEntity(), response.getMediaType(), contentDisposition, dataSourceName, logger);
	}

}
