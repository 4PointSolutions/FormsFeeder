package com._4point.aem.formsfeeder.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

// TODO: Replace WebSecurityConfigurerAdapter
// https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/configuration/WebSecurityConfigurerAdapter.html

// Useful Links:
// https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
// https://spring.io/blog/2019/11/21/spring-security-lambda-dsl

// https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/builders/HttpSecurity.html

// Use org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer to upcate 
// org.springframework.security.config.annotation.web.builders.WebSecurity
// or org.springframework.security.web.SecurityFilterChain to configure  org.springframework.security.config.annotation.web.builders.HttpSecurity

// TODO: Use profiles to allow the security to be turned on and off
// https://stackoverflow.com/questions/54983171/spring-security-configuration-enable-disable-authentication

/**
 * This cless manages web security for the FormsFeeder server using Spring Security.
 * 
 * There are currently two settings:
 *   No Security (this is the default) - Anonymous access is allowed to all services.
 *   Basic Authentication - Callers must pass in credentials via basic authentication.
 *   
 *   The list of available users is controlled via application.properties. 
 *
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig  {

	private static final String AUTH_PROPERTY = "formsfeeder.auth";
	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);
	
	@Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="basic")
	public SecurityFilterChain basicAuthOn(HttpSecurity http) throws Exception {
		logger.warn("Basic Authentication is on.");
		return http
//			.csrf(csrf->csrf.disable())
			.authorizeRequests(authReq->authReq
					.antMatchers("/aem/**").permitAll()							// Allow anonymous access to all proxied AEM links
					.antMatchers("/content/xfaforms/profiles/**").permitAll()	// Allow anonymous access to AEM proxied links we could not transform
					.anyRequest().authenticated()								// All other requests must be authenticated.
					)
			.httpBasic(Customizer.withDefaults())
			.build();
	}

	@Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="none", matchIfMissing = true)
	public SecurityFilterChain noSecurity(HttpSecurity http) throws Exception {
		logger.warn("Basic Authentication is off.");
		return http
				.csrf(csrf->csrf.disable())									// This needs to be disabled for some reason (see https://stackoverflow.com/questions/61761499/how-to-allow-all-and-any-requests-with-spring-security)
				.authorizeRequests(authReq->authReq
						.anyRequest().permitAll()							// Allow all requests
						)
				.build();
	}
	
    @Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="basic")
    public InMemoryUserDetailsManager nobasicAuthUsers() {
    	// TODO:  Pull users from configuration.
    	UserDetails user = User.withUsername("admin")	// Temporary user for testing purposes.
    						   .password("admin")
    						   .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="none", matchIfMissing = true)
    public InMemoryUserDetailsManager noUsers() {
        return new InMemoryUserDetailsManager();	// Set up a "no users" manager to prevent Spring from automatically creating a user.
    }
}
