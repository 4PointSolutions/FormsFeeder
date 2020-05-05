package com._4point.aem.formsfeeder.server;

import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com._4point.aem.formsfeeder.core.api.FeedConsumer;
import com._4point.aem.formsfeeder.server.pf4j.SpringPluginManager;

@SpringBootApplication
public class Application {

	private static PluginManager pluginManager = null;
	private static FeedConsumer x = null;

	@Autowired
	private SpringPluginManager springPlugInManager;
	
	public static void main(String[] args) {
		
		System.out.println("inside Main");
//		pluginManager = new DefaultPluginManager();
	    
		SpringApplication.run(Application.class, args);
		
//		pluginManager.stopPlugins();
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {

			System.out.println("Let's inspect the beans provided by Spring Boot:");

//			String[] beanNames = ctx.getBeanDefinitionNames();
//			Arrays.sort(beanNames);
//			for (String beanName : beanNames) {
//				System.out.println(beanName);
//			}

//			if (pluginManager == null) {
//				System.out.println("Plugin Manager is null!");
//			} else {
//			    String pluginId = "com._4point.aem.formsfeeder.plugins.debug";
//				List<Class<?>> extensionClasses = pluginManager.getExtensionClasses(pluginId);
//				if (extensionClasses == null) {
//					System.out.println("ExtensionClasses list is null!");
//				} else {
//					System.out.println("Found " + extensionClasses.size() + " extension classes of type " + pluginId + " .");
//				}
//			}
			
		    
		};
	}
}
