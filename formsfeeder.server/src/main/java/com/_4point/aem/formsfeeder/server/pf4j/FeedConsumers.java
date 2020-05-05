package com._4point.aem.formsfeeder.server.pf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;
import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;

@Component
public class FeedConsumers implements ApplicationContextAware {
	private static final FeedConsumers INSTANCE = new FeedConsumers();  

	@Autowired
	private SpringPluginManager springPluginManager;
	
    private ApplicationContext applicationContext;

	private FeedConsumers() {
		super();
	}

	private List<NamedFeedConsumer> consumers = null;
	
	public void printConsumers() {
		System.out.println(String.format("Found %d extensions for extension point '%s'", consumers().size(), NamedFeedConsumer.class.getName()));
	}
	
	private List<NamedFeedConsumer> consumers() {
		if (this.consumers != null) {
			return this.consumers;
		} else {
			if (springPluginManager == null) {
				springPluginManager = applicationContext.getBean("springPluginManager", SpringPluginManager.class);
			}
			return this.consumers = Objects.requireNonNull(springPluginManager, "SpringPluginManager has not been initialized!").getExtensions(NamedFeedConsumer.class);
		}
	}
	
	public Optional<FeedConsumer> consumer(String name) {
		for (NamedFeedConsumer consumer : consumers()) {
			if (Objects.requireNonNull(name, "name argument cannot be null.").equals(consumer.name())) {
				Optional.of(consumer);
			}
		}
		return Optional.empty();
	}
	
	public static final FeedConsumers instance() {
		return INSTANCE;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
