package com._4point.aem.formsfeeder.server.pf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;

public class FeedConsumers {
	
	@Autowired
	private List<FeedConsumer> consumers;
	
	public void printConsumers() {
		System.out.println(String.format("Found %d extensions for extension point '%s'", consumers.size(), FeedConsumer.class.getName()));
	}
}
