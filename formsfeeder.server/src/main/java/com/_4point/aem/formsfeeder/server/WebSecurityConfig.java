package com._4point.aem.formsfeeder.server;

import java.util.Collection;
import java.util.List;
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
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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
	private static final String AUTH_USERS_PROPERTY = "formsfeeder.auth.users";
	private static final String AUTH_OVERRIDES_PROPERTY = "formsfeeder.auth.overrides";
	
	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

	// For an explanation of how the Spring Expression Language is being used to populate users field, see the following linke: 
	//  https://stackoverflow.com/questions/28369458/how-to-fill-hashmap-from-java-property-file-with-spring-value
	//  https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions-inline-lists
	@Value("#{${" + AUTH_USERS_PROPERTY + ":{}}}")
	List<List<String>> users;							// List of List of user properties defined in application.properties 

	@Value("#{${" + AUTH_OVERRIDES_PROPERTY + ":{}}}")
	List<List<String>> pluginExceptions = List.of();	// List of List of exception properties defined in application.properties 

	@Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="basic")
	public SecurityFilterChain basicAuthOn(HttpSecurity http) throws Exception {
		logger.warn("Basic Authentication is on.");
		return http
			.csrf(csrf->csrf.disable())											// Don't think I need this turned off, so leave it on.
			.authorizeRequests(authReq->defaultRequestAuthorizations(customRequestAuthorizations(authReq)))
			.httpBasic(Customizer.withDefaults())
			.build();
	}

	private static class PluginAuthException {
		private static final String PLUGIN_PATH_PREFIX = "/api/v1/";
		String pluginName;
		String[] roles;

		private PluginAuthException(String pluginName, String... roles) {
			this.pluginName = pluginName;
			this.roles = roles;
		}

		public boolean hasRoles() {
			return roles.length > 0;
		}
		
		String getPluginPath() {
			return PLUGIN_PATH_PREFIX + pluginName + "/**";
		}

		public static PluginAuthException of(String pluginName, String... roles) {
			return new PluginAuthException(pluginName, roles);
		}

		public static PluginAuthException of(String pluginName, List<String> roles) {
			return PluginAuthException.of(pluginName, roles.toArray(new String[0]));
		}
		
		public static PluginAuthException of(List<String> strings) {
			if (strings.size() < 1) {
				throw new IllegalArgumentException("Invalid Plugin Authentication Exception - no arguments were supplied.");
			}
			return PluginAuthException.of(strings.get(0), strings.subList(1, strings.size()));
		}
		
		public static List<PluginAuthException> toPluginException(List<List<String>> pluginExceptions) {
			return pluginExceptions.stream()
								   .map(PluginAuthException::of)
								   .collect(Collectors.toList());
		}
	}

	// Configure custom request authentication/authorizations.
	private ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry customRequestAuthorizations(
			ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry authReq) {
		if (logger.isDebugEnabled()) {
			logPluginExceptions(pluginExceptions);
		}

		// Loop through the authentication exceptions specified in the application.properties.
		// If roles were specified, then users must have one of the roles required,
		// otherwise turn off authentication (i.e. allow anonymous access)
		List<PluginAuthException> pluginException = PluginAuthException.toPluginException(pluginExceptions);
		for(PluginAuthException pe : pluginException) {
			authReq = pe.hasRoles() ? authReq.antMatchers(pe.getPluginPath()).hasAnyRole(pe.roles) 
									: authReq.antMatchers(pe.getPluginPath()).permitAll();
		}
		
		return authReq;
	}

	private static void logPluginExceptions(List<List<String>> pluginExceptions) {
		logger.debug("Found {} authorization overrides.", pluginExceptions.size());
		for(List<String> peList : pluginExceptions) {
			if (peList.size() > 1) {
				logger.debug("  Plugin '{}' authorized for roles " + peList.subList(1, peList.size()).stream().collect(Collectors.joining("','", "'", "'")) + ".", peList.get(0));
			} else {
				logger.debug("  Plugin '{}' authentication disabled.", peList.get(0));
			}
		}
	}

	private static ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry defaultRequestAuthorizations(
			ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry authReq) {
		return authReq
				.antMatchers("/aem/**").permitAll()							// Allow anonymous access to all proxied AEM links
				.antMatchers("/content/xfaforms/profiles/**").permitAll()	// Allow anonymous access to AEM proxied links we could not transform
				.anyRequest().authenticated()								// All other requests must be authenticated.
				;
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
			return UserDefinition.of(params.get(0), params.get(1), params.get(2));
		}

		private UserDetails toUserDetails() {
			// This code assumes that the password is already encoded in a format that Spring Security understands
			// e.g. {bcrypt}$2a$10$X0R0vqKMYh5h0x/bBO0vy.5N4z68MvpS5CPgrNMfQbBA3t48JpSzy
			// If the password is unencoded, it needs to be encoded using either the encrypt plugin (under formsfeeder.plugins) or
			// a passwordEncoder needs to be supplied as part of this call. 
			return  User.withUsername(username)
						.password(password)
						.roles(role)
						.build();
	    }
	}
	
    @Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="basic")
    public InMemoryUserDetailsManager basicAuthUsers() {
		return new InMemoryUserDetailsManager(toUserDetails(users));
    }

	private static Collection<UserDetails> toUserDetails(Collection<List<String>> userDefs) {
		if (logger.isDebugEnabled()) {
			logUserDefinitions(userDefs);
		}
		return userDefs.stream()
					   .map(UserDefinition::of)
				 	   .map(UserDefinition::toUserDetails)
				 	   .collect(Collectors.toList());
	}

	private static void logUserDefinitions(Collection<List<String>> userDefs) {
		logger.debug("Found {} user definitions.", userDefs.size());
		for(List<String> userList : userDefs) {
			logger.debug("  User '{}' with password '{}' and roles " + userList.subList(2, userList.size()).stream().collect(Collectors.joining("','", "'", "'")) + ".", userList.get(0), userList.get(1));
		}
	}

    @Bean
	@ConditionalOnProperty(name=AUTH_PROPERTY, havingValue="none", matchIfMissing = true)
    public InMemoryUserDetailsManager noUsers() {
        return new InMemoryUserDetailsManager();	// Set up a "no users" manager to prevent Spring from automatically creating a user.
    }
}
