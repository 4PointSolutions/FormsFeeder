package com._4point.aem.formsfeeder.server.db;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This class sets up the the ability to specify multiple database settings in
 * the application.properties file
 * 
 * 
 * {@code
 * # First data source
 * spring.datasource.datasource1.url=jdbc:mysql://localhost:3306/test1
 * spring.datasource.datasource1.username=root
 * spring.datasource.datasource1.password=root
 * spring.datasource.datasource1.driver-class-name=com.mysql.jdbc.Driver
 * # Second data source
 * spring.datasource.datasource2.url=jdbc:mysql://localhost:3306/test2
 * spring.datasource.datasource2.username=root
 * spring.datasource.datasource2.password=root
 * spring.datasource.datasource2.driver-class-name=com.mysql.jdbc.Driver
 * ...
 * }
 * 
 * Up to 5 datasources may be defined in this way (in addition to the default one that Spring Boot instantiates).
 *
 */
@Configuration
public class JdbcConfiguration {
	private final static Logger logger = LoggerFactory.getLogger(JdbcConfiguration.class);

	@Bean(name = "dataSourceProperties")
	@ConfigurationProperties(prefix = "spring.datasource")
	@Primary
	public DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name = "dataSourceProperties1")
	@ConfigurationProperties(prefix = "spring.datasource.datasource1")
	public DataSourceProperties dataSourceProperties1() {
		return new DataSourceProperties();
	}

	@Bean(name = "dataSourceProperties2")
	@ConfigurationProperties(prefix = "spring.datasource.datasource2")
	public DataSourceProperties dataSourceProperties2() {
		return new DataSourceProperties();
	}

	@Bean(name = "dataSourceProperties3")
	@ConfigurationProperties(prefix = "spring.datasource.datasource3")
	public DataSourceProperties dataSourceProperties3() {
		return new DataSourceProperties();
	}

	@Bean(name = "dataSourceProperties4")
	@ConfigurationProperties(prefix = "spring.datasource.datasource4")
	public DataSourceProperties dataSourceProperties4() {
		return new DataSourceProperties();
	}

	@Bean(name = "dataSourceProperties5")
	@ConfigurationProperties(prefix = "spring.datasource.datasource5")
	public DataSourceProperties dataSourceProperties5() {
		return new DataSourceProperties();
	}

	@Bean(name = "dataSource1")
	public DataSource dataSource1(@Qualifier("dataSourceProperties1") DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().build();
	}

	@Bean(name = "dataSource2")
	public DataSource dataSource2(@Qualifier("dataSourceProperties2") DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().build();
	}

	@Bean(name = "dataSource3")
	public DataSource dataSource3(@Qualifier("dataSourceProperties3") DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().build();
	}

	@Bean(name = "dataSource4")
	public DataSource dataSource4(@Qualifier("dataSourceProperties4") DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().build();
	}

	@Bean(name = "dataSource5")
	public DataSource dataSource5(@Qualifier("dataSourceProperties5") DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().build();
	}

	@Bean(name = "jdbcTemplate1")
	public JdbcTemplate jdbcTemplate1(@Qualifier("dataSource1") DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = "jdbcTemplate2")
	public JdbcTemplate jdbcTemplate2(@Qualifier("dataSource2") DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = "jdbcTemplate3")
	public JdbcTemplate jdbcTemplate3(@Qualifier("dataSource3") DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = "jdbcTemplate4")
	public JdbcTemplate jdbcTemplate4(@Qualifier("dataSource4") DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = "jdbcTemplate5")
	public JdbcTemplate jdbcTemplate5(@Qualifier("dataSource5") DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
}
