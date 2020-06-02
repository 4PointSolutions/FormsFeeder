package formsfeeder.client.cli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommandLineClientTest {

	// Need to test the following scenarios:
	//  - No input DSes - should perform a GET
	//  - One input DS - should POST with the DS in the body
	//  - Many input DS - should POST multipart/form-data
	//  = No output DSes - ?
	//  - One output DS - Should write out to file or stdout
	//  - Many output DSes - Should write out to directory
	//  - Many output DSes with selected name - Should write out to a file or stdout.
	@Test
	void testMain_OneDS() {
		String[] args = { "-h", "http://localhost:8080/", "-d", "Param1=Param1Value" };
	}

}
