package com._4point.aem.formsfeeder.core.datasource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

public class DataSourceTestUtils {

	// Prevent this from being instantiated.
	private DataSourceTestUtils() {
	}

	/**
	 * Shouldn't allow getting an input stream while there is still an output stream open. 
	 * 
	 * @throws IOException
	 */
	public static void openInputStreamAndOutputStream(DataSource underTest) throws IOException {
		byte[] expectedBytes = "Expected Test Data".getBytes();
		try (OutputStream outputStream = underTest.outputStream()) {
			outputStream.write(expectedBytes);
			IllegalStateException ex = assertThrows(IllegalStateException.class, ()->underTest.inputStream());
			String msg = ex.getMessage();
			assertEquals("cannot open input stream while output stream is open.", msg.toLowerCase());
		}
	}

	/**
	 * Shouldn't be able to open OutputStream while one or more InputStreams are open. 
	 *
	 * We use recursion to test the counting of input streams.
	 * 
	 * @throws IOException
	 */
	public static void openOutputStreamWhileInputStreamOpen(byte[] expectedBytes, DataSource underTest) throws IOException {
		int maxNumOpenInputStreams = 3;
		openInputStream(0, maxNumOpenInputStreams, underTest);
		
		// If there are no open input streams, we should be able to write to it and read the data back.
		try (OutputStream outputStream = underTest.outputStream()) {
			outputStream.write(expectedBytes);
		}
		try (InputStream resultInputStream = underTest.inputStream()) {
			assertArrayEquals(expectedBytes, Jdk8Utils.readAllBytes(resultInputStream));
		}
	}

	/**
	 * Recursion routine that is used to open many input streams/
	 * 
	 * @param numOpenInputStreams	Number of Input Streams that are currently open
	 * @param maxNumOpenInputStreams	Maximum number we want to open (i.e. the termination of recursion condition)
	 * @param underTest	The ByteArrayOutputStream that is under test
	 * @throws IOException 
	 */
	private static void openInputStream(int numOpenInputStreams, int maxNumOpenInputStreams, DataSource underTest) throws IOException {
		// If we're less than the max number of InputStreams then open another and recurse.
		if (numOpenInputStreams <= maxNumOpenInputStreams) {
			try (InputStream inputStream = underTest.inputStream()) {
				openInputStream(numOpenInputStreams + 1, maxNumOpenInputStreams, underTest);

			}
		}
		// We'll get here as we're unwinding the stack.  If there are any open InputStreams we should generate an exception.
		if (numOpenInputStreams > 0) {
			IllegalStateException ex = assertThrows(IllegalStateException.class, ()->underTest.outputStream());
			String msg = ex.getMessage();
			assertEquals("cannot open output stream while input stream is open.", msg.toLowerCase());
		}
	}


}
