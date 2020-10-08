package com._4point.aem.formsfeeder.plugins.jdbc;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;

@Extension
public class JdbcPlugin implements NamedFeedConsumer, ExtensionPoint {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String FEED_CONSUMER_NAME = "Jdbc";

	@Autowired
	@Qualifier("jdbcTemplate1")
	JdbcTemplate jdbcTemplate1;		// first datasource

	@Autowired
	@Qualifier("jdbcTemplate2")
	JdbcTemplate jdbcTemplate2;		// second datasource

	@Autowired
	@Qualifier("jdbcTemplate3")
	JdbcTemplate jdbcTemplate3;		// third datasource

	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		
		var d11r = dataSources.deconstructor();
		
		List<String> names = d11r.getStringsByName("name");
		
		populateDatasource(jdbcTemplate1, "1", names, logger);
		populateDatasource(jdbcTemplate2, "2", names, logger);
		populateDatasource(jdbcTemplate3, "3", names, logger);
		
		Builder builder = DataSourceList.builder();
		builder.add("JdbcDataSource1", validateDatasource(jdbcTemplate1, "1", logger));
		builder.add("JdbcDataSource2", validateDatasource(jdbcTemplate2, "2", logger));
		builder.add("JdbcDataSource3", validateDatasource(jdbcTemplate3, "3", logger));
		return builder.build();
	}

	@Override
	public String name() {
		return FEED_CONSUMER_NAME;
	}

	/**
	 * This code assumes H2 database is being used as it uses syntax that is specific to H2 
	 * 
	 */
	private static void populateDatasource(JdbcTemplate jdbcTemplate, String suffix, List<String> names, Logger log) {
	    log.debug("Creating tables");

	    Objects.requireNonNull(jdbcTemplate, "JdbcTemplate must not be null!").execute("DROP TABLE customers IF EXISTS");
	    jdbcTemplate.execute("CREATE TABLE customers(id SERIAL, first_name VARCHAR(255), last_name VARCHAR(255))");

	    // Split up the array of whole names into an array of first/last names
	    List<Object[]> splitUpNames = names.stream()
	    	.map(name -> name + suffix)			// Add suffix
	        .map(name -> name.split(" "))		// Break it up into array with first name and last name entries
	        .collect(Collectors.toList());		// Collect to List of arrays

	    // Use a Java 8 stream to print out each tuple of the list
	    splitUpNames.forEach(name -> log.debug(String.format("Inserting customer record for %s %s", name[0], name[1])));

	    // Uses JdbcTemplate's batchUpdate operation to bulk load data
	    jdbcTemplate.batchUpdate("INSERT INTO customers(first_name, last_name) VALUES (?,?)", splitUpNames);

	}

	private static DataSourceList validateDatasource(JdbcTemplate jdbcTemplate, String suffix, Logger log) {
	    log.debug("Querying for customer records where first_name = 'Josh' from jdbcTemplate" + suffix + ":");
	    
	    var builder = DataSourceList.builder();
	    jdbcTemplate.query(
	        "SELECT id, first_name, last_name FROM customers WHERE first_name = ?", new String[] { "Josh" },
	        (rs, rowNum) -> new Customer(rs.getLong("id"), rs.getString("first_name"), rs.getString("last_name"))
	    ).forEach(customer -> {log.debug(customer.toString()); builder.add("CustomerRecord", buildDsl(customer::addToBuilder));});
		
	    return builder.build();
	}
	
	private static DataSourceList buildDsl(Function<DataSourceList.Builder, DataSourceList.Builder> fieldBuilder) {
		return fieldBuilder.apply(DataSourceList.builder()).build();
	}
	
	public static class Customer {
		  private long id;
		  private String firstName, lastName;

		  public Customer(long id, String firstName, String lastName) {
		    this.id = id;
		    this.firstName = firstName;
		    this.lastName = lastName;
		  }

		  @Override
		  public String toString() {
		    return String.format(
		        "Customer[id=%d, firstName='%s', lastName='%s']",
		        id, firstName, lastName);
		  }

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
		
		public DataSourceList.Builder addToBuilder(DataSourceList.Builder builder) {
			builder.add("id", this.id);
			builder.add("firstName", this.firstName);
			builder.add("lastName", this.lastName);
			return builder;
		}
	}
}
