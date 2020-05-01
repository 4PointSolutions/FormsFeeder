package com._4point.aem.formsfeeder.plugins.debug;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com._4point.aem.formsfeeder.server.FeedConsumerExtensionPoint;

/**
 * This is a sample plugin that can be used to help debug interactions with the FormsFeeder Server.
 * It prints to the log all DataSources it receives and returns a text description of each DataSOurce it receives to the caller.
 *
 */
public class DebugPlugin implements FeedConsumerExtensionPoint {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public static final String RESULT_DS_NAME = "Message";	// Use the same name for all DSes in result.
	
	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		Builder responseBuilder = DataSourceList.builder();
		
		for(DataSource ds : dataSources.list()) {
			String msg = toMessageString(ds);
			responseBuilder.add(RESULT_DS_NAME, msg);
			logger.info(msg);
		}
		
		return responseBuilder.build();
	}

	private static String toMessageString(DataSource ds) {
		StringBuilder msgBuilder = new StringBuilder("Found datasource: name='").append(ds.name()).append("'");
		MimeType contentType = ds.contentType();
		if (contentType == null) {
			msgBuilder.append(" with null ContentType");
		} if (contentType.equals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE)) {
			msgBuilder.append(" with value '")
					  .append(Deconstructor.dsToString(ds))
					  .append("'");
		} if (contentType.equals(StandardMimeTypes.TEXT_PLAIN_TYPE)) {
			msgBuilder.append(" with value '")
					  .append(Deconstructor.dsToString(ds, StandardCharsets.US_ASCII))
					  .append("'");
		} else {
			msgBuilder.append(" with contentType '")
					  .append(contentType.asString())
					  .append("'");
		}
		if (ds.filename().isPresent()) {
			msgBuilder.append(" with filename '")
			          .append(ds.filename().get().toString())
			          .append("'");
		}
		if (!ds.attributes().isEmpty()) {
			ds.attributes().forEach((k,v)->msgBuilder.append(" with attribute '")
													 .append(k)
													 .append("'/'")
													 .append(v)
													 .append("'")
													 );
		}
		return msgBuilder.append(".").toString();
	}

}
