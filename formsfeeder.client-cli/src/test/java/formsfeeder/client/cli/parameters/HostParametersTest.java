package formsfeeder.client.cli.parameters;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class HostParametersTest {

	private enum HappyPathScenarios {
		NO_HTTPS("http://foo:2323", false, "foo", 2323), 
		HTTPS("https://foobar:0876", true, "foobar", 876),
		NO_PORT("http://something", false, "something", 80),
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
				()->assertEquals(scenario.expectedHostName, underTest.hostname()),
				()->assertEquals(scenario.expectedHostPort, underTest.hostPort())
				);
	}

}
