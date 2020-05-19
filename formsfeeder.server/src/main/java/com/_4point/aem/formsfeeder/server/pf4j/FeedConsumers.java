package com._4point.aem.formsfeeder.server.pf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.api.PluginsConsumer;
import com._4point.aem.formsfeeder.pf4j.SpringPluginManager;
import com._4point.aem.formsfeeder.pf4j.spring.ApplicationContextConsumer;
import com._4point.aem.formsfeeder.pf4j.spring.EnvironmentConsumer;

@Component
public class FeedConsumers {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private SpringPluginManager springPluginManager;

	@Autowired
	private Environment environment;

	@Autowired
	private ApplicationContext applicationContext;
	
	private List<NamedFeedConsumer> consumers = null;
	
	/* package */ FeedConsumers() {
		super();
	}
	
	private void consumersInfo() {
		logger.debug("Found {} extensions for extension point '{}'", consumers().size(), NamedFeedConsumer.class.getName());
		for (NamedFeedConsumer consumer : consumers()) {
				logger.info("Found FeedConsumer extension named '{}'.", consumer.name());
		}
	}
	
	private List<NamedFeedConsumer> consumers() {
		if (this.consumers != null) {
			return this.consumers;
		} else {
			return this.consumers = initializeExtensions();
		}
	}

	private List<NamedFeedConsumer> initializeExtensions() {
		
		List<NamedFeedConsumer> extensions = Objects.requireNonNull(springPluginManager, "SpringPluginManager has not been initialized!").getExtensions(NamedFeedConsumer.class);
		// Populate extensions with Spring beans
		for (NamedFeedConsumer extension:extensions) {
			if (extension instanceof EnvironmentConsumer) {
				EnvironmentConsumer envConsumer = (EnvironmentConsumer)extension;
				logger.info("Initializing EnvironmentConsumer extension '{}'.", extension.name());
				envConsumer.accept(environment);
			}
			if (extension instanceof ApplicationContextConsumer) {
				ApplicationContextConsumer ctxConsumer = (ApplicationContextConsumer)extension;
				logger.info("Initializing ApplicationContextConsumer extension '{}'.", extension.name());
				ctxConsumer.accept(applicationContext);
			}
			if (extension instanceof PluginsConsumer) {
				PluginsConsumer pluginsConsumer = (PluginsConsumer)extension;
				logger.info("Initializing PluginsConsumer extension '{}'.", extension.name());
				pluginsConsumer.accept(extensions);
			}
		}
		return extensions;
	}
	
	public Optional<FeedConsumer> consumer(String name) {
		for (NamedFeedConsumer consumer : consumers()) {
			if (Objects.requireNonNull(name, "name argument cannot be null.").equals(consumer.name())) {
				return Optional.of(consumer);
			}
		}
		return Optional.empty();
	}
	
    /**
     * This method load, start plugins and inject extensions in Spring
     */
    @PostConstruct
    public void init() {
    	logger.debug("PostConstruct of FeedConsumers, SpringPluginManager is " + (springPluginManager != null ? "not " : "") + "null.");
    	consumersInfo();
    }
}
