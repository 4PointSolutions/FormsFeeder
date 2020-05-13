package com._4point.aem.formsfeeder.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com._4point.aem.formsfeeder")
public class Application {

	public static void main(String[] args) {
		
		System.out.println("inside Main");
//		pluginManager = new DefaultPluginManager();
	    
		ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
		
		System.out.println("Contains springPlunginManager " + ctx.containsBeanDefinition("springPluginManager"));
		System.out.println("Contains feedConsumers " + ctx.containsBeanDefinition("feedConsumers"));
		
//		pluginManager.stopPlugins();
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {

//			System.out.println("Let's inspect the beans provided by Spring Boot:");
//
//			System.out.println("springPlugInManager is " + (springPlugInManager != null ? "not " : "") + "null");;
//
//			System.out.println("context " + (ctx.containsBean("springPluginManager") ? "contains" : "does not contain") + " springPluginManager");;
//			
//			List<NamedFeedConsumer> extensions = springPlugInManager.getExtensions(NamedFeedConsumer.class);
//			extensions.forEach((e)->System.out.println("Found " + e.name() + " extension."));
			
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
