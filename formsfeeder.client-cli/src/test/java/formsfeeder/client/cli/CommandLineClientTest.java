package formsfeeder.client.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

class CommandLineClientTest {

	// Need to test the following scenarios:
	//  - No input DSes - should perform a GET
	//  - One input DS - should POST with the DS in the body
	//  - Many input DS - should POST multipart/form-data
	//  = No output DSes - ?
	//  - One output DS - Should write out to file or stdout
	//  - Many output DSes - Should write out to .zip
	//  - Many output DSes with selected name - Should write out to a file or stdout.
	@Test
	void testMain_OneDS_NoOutput() throws Exception {
		String expectedParamValue = "Param1Value";
		String expectedParamName = "Param1";
		String[] args = { "-h", "http://localhost:8080/", "-d", expectedParamName + "=" + expectedParamValue, "-p", "Debug"};
		
		var stdin = new ByteArrayInputStream(new byte[0]);
		var stdout = new ByteArrayOutputStream();
		var stderr = new ByteArrayOutputStream();
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertTrue(stdoutStr.contains(expectedParamName), "Expected '" + stdoutStr + "' to contain '" + expectedParamName + "', but didn't."),
				()->assertTrue(stdoutStr.contains(expectedParamValue), "Expected '" + stdoutStr + "' to contain '" + expectedParamValue + "', but didn't."),
				()->assertTrue(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't."),
				()->assertEquals(1, StreamSupport.stream(fs.getRootDirectories().spliterator(), false).count()),		// Verify that there's only one root directory.
				()->assertEquals(1, StreamSupport.stream(Files.newDirectoryStream(fs.getRootDirectories().iterator().next()).spliterator(), false).count())		// Verify that there's no files in that root directory
				);
	}

}
