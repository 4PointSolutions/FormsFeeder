package formsfeeder.client.cli.parameters;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class HostParametersTest {

	private enum HappyPathScenarios {
		NO_HTTPS("http://foo:2323", false, "foo", 2323), 
		HTTPS("https://foobar:0876", true, "foobar", 876),
		NO_PORT("http://something", false, "something", 80),
		NO_PORT2("http://something:/", false, "something", 80),
		NO_PORT3("http://something:", false, "something", 80),
		TRAILING_SLASH("https://somethingelse:7654/", true, "somethingelse", 7654)
		;
		
		private final String testStr;
		private final boolean expectedUseSsl;
		private final String expectedHostName;
		private final int expectedHostPort;
		/**
		 * @param testStr			- String to test
		 * @param expectedUseSsl	- Expected UseSsl settings
		 * @param expectedHostName	- Expected host name
		 * @param expectedHostPort	- Expected host port
		 */
		private HappyPathScenarios(String testStr, boolean expectedUseSsl, String expectedHostName,
				int expectedHostPort) {
			this.testStr = testStr;
			this.expectedUseSsl = expectedUseSsl;
			this.expectedHostName = expectedHostName;
			this.expectedHostPort = expectedHostPort;
		}
	}
	
	@ParameterizedTest
	@EnumSource
	void testFrom(HappyPathScenarios scenario) throws Exception {
		HostParameters underTest = HostParameters.from(scenario.testStr);
		
		assertAll(
				()->assertEquals(scenario.expectedUseSsl, underTest.useSsl()),
				()->assertEquals(scenario.expectedHostName, underTest.hostName()),
				()->assertEquals(scenario.expectedHostPort, underTest.hostPort())
				);
	}

	private enum ExceptionalScenario {
		BAD_PROTOCOL("ftp://hostname:23/", ParseException.class, "Bad Protocol", "ftp"),
		BAD_PORT("https://hostname:asd/", ParseException.class, "Unable to parse Port Number", "asd"),
		ZERO_PORT("https://hostname:0/", ParseException.class, "Port Number cannot be 0."),
		MISSING_HOST("https://:23/", ParseException.class, "Error parsing host string", "'https://:23/'"),
		TWO_HOSTS("https://hostname:hostname:23/", ParseException.class, "Error parsing host string", "'https://hostname:hostname:23/'")
		;
		
		private final String testStr;
		private final Class<? extends Exception> expectedException;
		private final String[] expectedMsgStrings;

		private ExceptionalScenario(final String testStr, final Class<? extends Exception> expectedException, final String... expectedMsgStrings) {
			this.testStr = testStr;
			this.expectedException = expectedException;
			this.expectedMsgStrings = expectedMsgStrings;
		}
	};
	
	@ParameterizedTest
	@EnumSource
	void testFrom_BadInput(ExceptionalScenario scenario) throws Exception {
		Exception ex = assertThrows(scenario.expectedException,()->HostParameters.from(scenario.testStr));
		final String msg = ex.getMessage();
		
		assertNotNull(msg);
		assertAll(Arrays.stream(scenario.expectedMsgStrings).map((s)->(()->assertTrue(msg.contains(s), "Expected '" + msg + "' to contain '" + s + "', but it didn't."))));
	}
}
