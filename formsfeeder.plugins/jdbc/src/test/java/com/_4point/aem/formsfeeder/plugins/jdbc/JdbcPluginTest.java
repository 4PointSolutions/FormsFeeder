package com._4point.aem.formsfeeder.plugins.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;

class JdbcPluginTest {
	private static final String FEED_CONSUMER_NAME = "Jdbc";

//	private static final String JDBC_H2 = "jdbc:h2:file:C:/temp/testh2db%s";	// Use file
	private static final String JDBC_H2 = "jdbc:h2:mem:testh2db%s;DB_CLOSE_DELAY=-1";	// Use memory

	private final JdbcPlugin underTest = new JdbcPlugin();
	
	@BeforeEach
	void setUp() throws Exception {
		injectJdbcTemplate("1");
		injectJdbcTemplate("2");
		injectJdbcTemplate("3");
	}

	@Test
	void testAccept() throws Exception {
		List<String> providedNames = List.of("John Woo", "Jeff Dean", "Josh Bloch", "Josh Long");
		
		var resultDsl = underTest.accept(DataSourceList.builder().addStrings("name", providedNames).build());
		
		assertEquals(3, resultDsl.size(), "Should be one DSL for each JdbcTemplate (i.e. 3)");
//		List<String> suffixes = resultDsl.list().stream()
//										 .map(DataSource::name)
//										 .map(n->n.substring(n.length() - 1, n.length()))
//										 .collect(Collectors.toList());
		Deconstructor resultD11r = resultDsl.deconstructor();
		for (var suffix : List.of("1", "2", "3")) {
			DataSourceList jdbcDateSourceDsl = resultD11r.getDataSourceListByName("JdbcDataSource" + suffix).get(); 
			assertEquals(2, jdbcDateSourceDsl.size(), "Should be two Josh's per jdbcTemplate.");
			jdbcDateSourceDsl.deconstructor().getDataSourceListsByName("CustomerRecord").forEach(dsl->validateCustomerRecord(dsl, suffix));
		}
		
	}

	void validateCustomerRecord(DataSourceList custDsl, String suffix) {
		assertEquals(3, custDsl.size(), "Should be 3 fields, id, firstName, lastName");
		var custD11r = custDsl.deconstructor();
		assertEquals("Josh", custD11r.getStringByName("firstName").get(), "firstName should be Josh");
		assertTrue(custD11r.getStringByName("lastName").get().endsWith(suffix), "lastName should end with the suffix number");
	}

	@Test
	void testName() {
		assertEquals(FEED_CONSUMER_NAME, underTest.name());
	}

	private void injectJdbcTemplate(String suffix) throws NoSuchFieldException {
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL(String.format(JDBC_H2, suffix));
		ds.setUser("sa");
		ds.setPassword("sa");
		 
		 JdbcTemplate t = new JdbcTemplate(ds, false);
		 junitx.util.PrivateAccessor.setField(underTest,"jdbcTemplate" + suffix, t);	// Simulate injection.
	}

}
