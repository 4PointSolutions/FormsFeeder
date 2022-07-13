package com._4point.aem.formsfeeder.server;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
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
	private static final String AUTH__USERS_PROPERTY = "formsfeeder.auth.users";
	
	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

	private static final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
	private static final Function<String, String> encodingFn = p->encoder.encode(p);
	
	// For an explanation of how the Spring Expression Language is being used to populate users field, see the following linke: 
	//  https://stackoverflow.com/questions/28369458/how-to-fill-hashmap-from-java-property-file-with-spring-value
	//  https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions-inline-lists
	@Value("#{${" + AUTH__USERS_PROPERTY + "}}")
	List<List<String>> users;							// List of List of user properties defined in application.properties 

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
	
	private static class UserDefinition {
		String username;
		String password;
		String role;

		private UserDefinition(String username, String password, String role) {
			this.username = username;
			this.password = password;
			this.role = role;
		}
		
		public static UserDefinition of(String username, String password, String role) {
			return new UserDefinition(username, password, role);
		}

		public static UserDefinition of(List<String> params) {
			if (params.size() != 3) {
				throw new IllegalArgumentException("User Definitions require 3 parameters but " + params.size() + " were supplied.  " + params.toString());
			}
			return new UserDefinition(params.get(0), params.get(1), params.get(2));
		}

		private UserDetails toUserDetails() {
			return  User.withUsername(username)
						.password(password)
						.roles(role)
						.passwordEncoder(encodingFn)
						.build();
	    }
	}
	
    @Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="basic")
    public InMemoryUserDetailsManager basicAuthUsers() {
		return new InMemoryUserDetailsManager(toUserDetails(users));
    }

	private static Collection<UserDetails> toUserDetails(Collection<List<String>> userDefs) {
		return userDefs.stream()
					   .map(UserDefinition::of)
				 	   .map(UserDefinition::toUserDetails)
				 	   .collect(Collectors.toList());
	}

    @Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="none", matchIfMissing = true)
    public InMemoryUserDetailsManager noUsers() {
        return new InMemoryUserDetailsManager();	// Set up a "no users" manager to prevent Spring from automatically creating a user.
    }
}
