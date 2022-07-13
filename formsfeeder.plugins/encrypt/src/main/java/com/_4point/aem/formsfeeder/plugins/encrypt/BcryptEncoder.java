package com._4point.aem.formsfeeder.plugins.encrypt;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;

/*
 * This is a plugin the encodes a password using the BCryot encoder and returns that to the user.
 * 
 * This is useful if Basic Authentication is enabled.  Basic Authentication requires that user passwords
 * stored in application.properties be encrypted using BCrypt.
 *
 */
@Extension
public class BcryptEncoder implements NamedFeedConsumer, ExtensionPoint  {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final String FEED_CONSUMER_NAME = "BCryptEncoder";
	private static final String PASSWORD_PARAM_NAME = "password";
	private static final String RESULT_PARAM_NAME = "encodedValue";
	
	private static final PasswordEncoder ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		Deconstructor inputD12r = dataSources.deconstructor();
		String password = inputD12r.getStringByName(PASSWORD_PARAM_NAME).orElseThrow(()->new FeedConsumerBadRequestException("'" + PASSWORD_PARAM_NAME + "' parameter was not supplied."));
		
		Builder responseBuilder = DataSourceList.builder();
		responseBuilder.add(RESULT_PARAM_NAME, ENCODER.encode(password));
		
		return responseBuilder.build();
	}

	@Override
	public String name() {
		return FEED_CONSUMER_NAME;
	}

}
