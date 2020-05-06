package com._4point.aem.formsfeeder.server.pf4j;


import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.server.ServicesEndpoint;

/**
 * @author Decebal Suiu
 */
@Component
public class SpringPluginManager extends DefaultPluginManager implements ApplicationContextAware {
	private final static Logger logger = LoggerFactory.getLogger(SpringPluginManager.class);

    private ApplicationContext applicationContext;

    public SpringPluginManager() {
    }

    public SpringPluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new SpringExtensionFactory(this);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * This method load, start plugins and inject extensions in Spring
     */
    @PostConstruct
    public void init() {
        loadPlugins();
        startPlugins();

        logger.info("Plugins initialized!");
    }

}
