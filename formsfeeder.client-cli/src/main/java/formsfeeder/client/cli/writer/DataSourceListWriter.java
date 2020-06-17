package formsfeeder.client.cli.writer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

/**
 * Utility class for writing DataSourceLists out.
 * 
 * If the DataSourceList contains just one entry then the contents of that one entry are written out.  If the
 * DataSourceList contains multiple entries, then a .zip is written with each entry represented by a file in the .zip.
 *
 */
public class DataSourceListWriter {

	// Can't be instantiated.
	private DataSourceListWriter() {
	}

	/**
	 * Write to a DataSourceList to an output stream
	 * 
	 * @param dsl
	 * @param out
	 * @throws IOException
	 */
	public static void write(DataSourceList dsl, OutputStream out) throws IOException {
		int size = dsl.list().size();
		if (size == 1) {
			Jdk8Utils.transfer(dsl.list().get(0).inputStream(), out);
		} else if (size > 1) {
			writeDataSourceListToZip(dsl, out);
		}
	}

	/**
	 * Write a DataSourceList to a file.
	 * 
	 * @param dsl
	 * @param outputPath
	 * @throws IOException
	 */
	public static void write(DataSourceList dsl, Path outputPath) throws IOException {
		int size = dsl.list().size();
		if (size > 0) {		// Skip the writing if there's nothing in the DSL.
			if (Files.exists(outputPath)) {
				if (Files.isDirectory(outputPath) || !Files.isWritable(outputPath)) {
					throw new FileNotFoundException("Unable to write to file. (" + outputPath.toString() + ").");
				}
			}
			try (OutputStream os = Files.newOutputStream(outputPath)) {
				write(dsl, os);
			}
		}
	}

	private static void writeDataSourceListToZip(DataSourceList dsl, OutputStream out) throws IOException {
		FilenameList filenameList = new FilenameList();
		ZipOutputStream zipOutputStream = new ZipOutputStream(out);
		zipOutputStream.setComment("DataSourceList output");
		try {
			for(DataSource ds : dsl.list()) {
				String filename = determineFilename(ds, filenameList);
				ZipEntry zipEntry = new ZipEntry(filename);
				
				zipOutputStream.putNextEntry(zipEntry);
				Jdk8Utils.transfer(ds.inputStream(), zipOutputStream);
			}
		} finally {
			zipOutputStream.finish();
		}
	}

	/**
	 * Used to ensure that each filename is unique.
	 *
	 */
	private static final class FilenameList {
		private final Set<String> usedFilenames = new HashSet<>();

		private String uniqueFilename(String candidate) {
			if (!usedFilenames.contains(candidate)) {
				usedFilenames.add(candidate);
				return candidate;		// first one is good.
			}
			int extensionLocation = candidate.lastIndexOf('.');
			if (extensionLocation < 0) {
				return tryAgain(candidate, 1, "");	// No extension
			} else {
				// break out the extension and the main filename.
				return tryAgain(candidate.substring(0, extensionLocation - 1), 1, candidate.substring(extensionLocation + 1));
			}
		}
		
		private String tryAgain(String filename, int number, String extension) {
			String candidate = filename + number + "." + extension; 
			if (!usedFilenames.contains(candidate)) {
				usedFilenames.add(candidate);
				return candidate;
			} else {
				return tryAgain(filename, number + 1, extension);
			}
			
		}
	}
	
	private static String determineFilename(DataSource ds, FilenameList fl) {
		return fl.uniqueFilename(ds.filename()							// Use the filename if it's there
								.map(Path::getFileName)
								.map(Path::toString)
								.orElse(removeNamespace(ds.name()))	// If there's no filename, use the dsName with any namespace removed.
								);
	}
	
	private static String removeNamespace(String dsName) {
		int extensionLocation = dsName.lastIndexOf(':');
		return extensionLocation < 0 ? dsName : dsName.substring(extensionLocation + 1);
	}


}
