package com._4point.aem.formsfeeder.plugins.debug;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;

/*
 * This is a sample plugin that can be used to help debug interactions with the
 * FormsFeeder Server. It prints to the log all DataSources it receives and
 * returns a text description of each DataSource it receives to the caller.
 *
 */
@Extension
public class DebugFeedConsumerExtension implements NamedFeedConsumer, ExtensionPoint  {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final Set<String> blacklistedDataSources = Set.of("formsfeeder:x-correlation-id");

	public static final String FEED_CONSUMER_NAME = "Debug";
	public static final String RESULT_DS_NAME = "Message"; // Use the same name for all DSes in result.

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		Builder responseBuilder = DataSourceList.builder();

		for (DataSource ds : dataSources.list()) {
			if (!blacklistedDataSources.contains(ds.name())) {
				String msg = toMessageString(ds);
				responseBuilder.add(RESULT_DS_NAME, msg);
				logger.info(msg);
			}
		}

		return responseBuilder.build();
	}

	private static String toMessageString(DataSource ds) {
		StringBuilder msgBuilder = new StringBuilder("Found datasource: name='").append(ds.name()).append("'");
		MimeType contentType = ds.contentType();
		if (contentType == null) {
			msgBuilder.append(" with null ContentType");
		}
		if (contentType.equals(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE)) {
			msgBuilder.append(" with UTF-8 text value '").append(Deconstructor.dsToString(ds)).append("'");
		}
		if (contentType.equals(StandardMimeTypes.TEXT_PLAIN_TYPE)) {
			msgBuilder.append(" with plain text value '").append(Deconstructor.dsToString(ds, StandardCharsets.US_ASCII))
					.append("'");
		} else {
			msgBuilder.append(" with contentType '").append(contentType.asString()).append("'");
		}
		if (ds.filename().isPresent()) {
			msgBuilder.append(" with filename '").append(ds.filename().get().toString()).append("'");
		}
		if (!ds.attributes().isEmpty()) {
			ds.attributes().forEach(
					(k, v) -> msgBuilder.append(" with attribute '").append(k).append("'/'").append(v).append("'"));
		}
		return msgBuilder.append(".").toString();
	}

	@Override
	public String name() {
		return FEED_CONSUMER_NAME;
	}

}
