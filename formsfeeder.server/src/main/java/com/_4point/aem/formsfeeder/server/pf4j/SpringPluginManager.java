package com._4point.aem.formsfeeder.server.pf4j;


import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;

/**
 * @author Decebal Suiu
 */
@Component
public class SpringPluginManager extends DefaultPluginManager implements ApplicationContextAware {

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

        System.out.println("Plugins initialized!");
        
        //
	    String pluginId = "com._4point.aem.formsfeeder.plugins.debug";
		List<Class<?>> extensionClasses = getExtensionClasses(pluginId);
		if (extensionClasses == null) {
			System.out.println("ExtensionClasses list is null!");
		} else {
			System.out.println("Found " + extensionClasses.size() + " extension classes of type " + pluginId + " .");
		}

		List<NamedFeedConsumer> extensions = getExtensions(NamedFeedConsumer.class);
		System.out.println("Found " + extensions.size() + " extensions of type NamedFeedConsumer.");
		
		List extensions2 = getExtensions(pluginId);
		System.out.println("Found " + extensions2.size() + " extensions.");
		
		
        System.out.println("Extensions logged!");
        
        
//        AbstractAutowireCapableBeanFactory beanFactory = (AbstractAutowireCapableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
//        ExtensionsInjector extensionsInjector = new ExtensionsInjector(this, beanFactory);
//        extensionsInjector.injectExtensions();
    }

}
