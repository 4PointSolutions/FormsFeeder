package formsfeeder.client.cli;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
	void testMain_OneDS_OneOutput_Stdout() throws Exception {
		String expectedParamValue = "Param1Value";
		String expectedParamName = "Param1";
		String[] args = { "-h", "http://localhost:8080/", 
						  "-d", expectedParamName + "=" + expectedParamValue,
						  "-p", "Debug"};
		
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
				()->assertEquals(0, getFileCount(fs), "Expected no files to be created.")
				);
	}

	@Test
	void testMain_OneDS_OneOutput_File() throws Exception {
		String expectedParamValue = "Param1Value";
		String expectedParamName = "Param1";
		String expectedOutputLocation = "testMain_OneDS_OneOutput_File_result.txt";
		String[] args = { "-h", "http://localhost:8080/", 
						  "-d", expectedParamName + "=" + expectedParamValue,
						  "-p", "Debug", 
						  "-o", expectedOutputLocation};
		
		
		var stdin = new ByteArrayInputStream(new byte[0]);
		var stdout = new ByteArrayOutputStream();
		var stderr = new ByteArrayOutputStream();
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		String stdoutStr = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
		assertAll(
				()->assertFalse(stdoutStr.contains(expectedParamName), "Expected '" + stdoutStr + "' to not contain '" + expectedParamName + "', but did."),
				()->assertFalse(stdoutStr.contains(expectedParamValue), "Expected '" + stdoutStr + "' to not contain '" + expectedParamValue + "', but did."),
				()->assertFalse(stdoutStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + stdoutStr + "' to not contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but did."),
				()->assertEquals(1, getFileCount(fs), "Expected only one file to be created."),		// Verify that there's only one root directory.
				()->assertTrue(Files.exists(fs.getPath(expectedOutputLocation)))		// Verify that there's no files in that root directory
				);
		String outputContents = Files.readString(fs.getPath(expectedOutputLocation));
		assertAll(
				()->assertTrue(outputContents.contains(expectedParamName), "Expected '" + outputContents + "' to contain '" + expectedParamName + "', but didn't."),
				()->assertTrue(outputContents.contains(expectedParamValue), "Expected '" + outputContents + "' to contain '" + expectedParamValue + "', but didn't."),
				()->assertTrue(outputContents.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + outputContents + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't.")
				);
	}

	@Test
	void testMain_ManyDS_ManyOutput_Stdout() throws Exception {
		String[] expectedParamValues = { "Param1Value", "Param2Value", "Param3Value" }; 
		String[] expectedParamNames = { "Param1", "Param2", "Param3" }; 
		String[] args = { "-h", "http://localhost:8080/", 
						  "-d", expectedParamNames[0] + "=" + expectedParamValues[0], 
						  "-d", expectedParamNames[1] + "=" + expectedParamValues[1], 
						  "-d", expectedParamNames[2] + "=" + expectedParamValues[2], 
						  "-p", "Debug"};
		
		var stdin = new ByteArrayInputStream(new byte[0]);
		var stdout = new ByteArrayOutputStream();
		var stderr = new ByteArrayOutputStream();
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		CommandLineClient.mainline(args, stdin, new PrintStream(stdout), new PrintStream(stderr), fs);
		
		// create a buffer to improve copy performance later.
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(stdout.toByteArray()));
		ZipEntry ze;
		int count = 0;
		while((ze = zis.getNextEntry()) != null) {
			System.out.println("Found ZipEntry '" + ze.getName() + "' of size '" + ze.getSize() + "'.");
			
			String zipStr = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
			final int myCount = expectedParamValues.length - (count + 1);
			if (myCount >= 0) {	// Skip this section rather than generating out of bounds exceptions.  We'll catch the problem below.
				assertAll(
						()->assertTrue(zipStr.contains(expectedParamNames[myCount]), "Expected '" + zipStr + "' to contain '" + expectedParamNames[myCount] + "', but didn't."),
						()->assertTrue(zipStr.contains(expectedParamValues[myCount]), "Expected '" + zipStr + "' to contain '" + expectedParamValues[myCount] + "', but didn't."),
						()->assertTrue(zipStr.contains(StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString()), "Expected '" + zipStr + "' to contain '" + StandardMimeTypes.TEXT_PLAIN_UTF8_TYPE.asString() + "', but didn't.")
						);
			}
			count++;
		}
		assertEquals(expectedParamValues.length, count, "Expected to find the same number of ZipEntries as parameters.");
		assertEquals(0, getFileCount(fs), "Expected no files to be created.");
	}

	
	
	
	
	private static int getFileCount(FileSystem fs) throws IOException {
		int fileCount = 0;
		for(Path path : fs.getRootDirectories()) {
			fileCount += getFileCount(path); 
		}
		return fileCount;
		
	}
	
	private static int getFileCount(Path directory) throws IOException {
		int fileCount = 0;
		for (Path path : Files.newDirectoryStream(directory)) {
			if (Files.isDirectory(path)) {
//				System.out.println("Found dir '" + path.toString() + "'.");
				fileCount += getFileCount(path);
			} else {
//				System.out.println("Found file '" + path.toString() + "'.");
				fileCount += 1;
			}
		}
		return fileCount;
	}
}
