package com._4point.aem.formsfeeder.pf4j.spring;

import org.springframework.context.ApplicationContext;

/**
 * This interface is implemented on plug-ins that need to access the ApplicationContext.
 * 
 * Using this interface gives a plug-in access to the entire ApplicationContext.  It should be used with caution.
 * If there is another more limited interface that you can use (such as EnvironmentConsumer to access environment variables)
 * then the more limited interface should be used.  This should make it easier to locate misbehaving plug-ins.
 *
 */
@FunctionalInterface
public interface ApplicationContextConsumer {
	public void accept(ApplicationContext ctx);
}
