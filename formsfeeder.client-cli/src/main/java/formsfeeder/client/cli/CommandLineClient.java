package formsfeeder.client.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.cli.ParseException;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;

import formsfeeder.client.FormsFeederClient;
import formsfeeder.client.FormsFeederClient.FormsFeederClientException;
import formsfeeder.client.cli.parameters.AppParameters;
import formsfeeder.client.cli.parameters.CommandLineAppParameters;
import formsfeeder.client.cli.parameters.DataSourceInfo;
import formsfeeder.client.cli.parameters.HostParameters;

public class CommandLineClient {
	public static void main(String[] args) {
		mainline(args, System.in, System.out, System.err, FileSystems.getDefault());
	}
	
	public static void mainline(String[] args, InputStream in, PrintStream out, PrintStream err, FileSystem destFileSystem) {
		try {
			AppParameters cliParameters = CommandLineAppParameters.parseArgs(args);

			HostParameters hostParams = cliParameters.hostParameters();
			FormsFeederClient ffClient = FormsFeederClient.builder()
					  .machineName(hostParams.hostName())
					  .port(hostParams.hostPort())
					  .useSsl(hostParams.useSsl())
					  .plugin(cliParameters.plugin())
					  .build();	

			DataSourceList result = ffClient.accept(asDataSourceList(cliParameters.dataSourceInfos()));
			
			Optional<Path> output = cliParameters.output();
			int resultSize = result.list().size();
			if (resultSize > 0 ) {	// If no results were returned, do nothing.
				if (resultSize == 1) {
					DataSource dataSource = result.list().get(0);
					if ((output.isEmpty() && dataSource.filename().isEmpty()) ||						// No output was specified
					    (output.isPresent() && output.get().getFileName().toString().equals("---"))) {	// output --- was specified
							// send the lone output to stdout.
							dataSource.inputStream().transferTo(out);
					} else {
						// We'll be sending this to a filename
						Path outputPath = output.isPresent() 
								? destFileSystem.getPath(output.get().toString())	// Output parameter was found, so use that.
								: destFileSystem.getPath(dataSource.filename().get().getFileName().toString());		// No output parameter, so use filename for output.
						writeDataSourceToFile(dataSource, outputPath);
					}
				} else {
					// resultSize > 1, we'll be sending this to a .zip stream.
					if (output.isEmpty()) {
						writeDataSourceListToZip(result, out);
					} else {
						writeDataSourceListToFile(result, destFileSystem.getPath(output.get().toString()));
					}
				}
			}
			
		} catch (FormsFeederClientException | ParseException | IllegalStateException | IOException e) {
			err.println(e.getMessage());
		}
	}

	private static void writeDataSourceToFile(DataSource dataSource, Path outputPath) throws FileNotFoundException, IOException {
		if (Files.exists(outputPath)) {
			if (Files.isDirectory(outputPath) || !Files.isWritable(outputPath)) {
				throw new FileNotFoundException("Unable to write to file. (" + outputPath.toString() + ").");
			}
		}
		try (OutputStream os = Files.newOutputStream(outputPath)) {
			dataSource.inputStream().transferTo(os);
		}
	}

	private static void writeDataSourceListToFile(DataSourceList dataSourceList, Path outputPath) throws FileNotFoundException, IOException {
		if (Files.exists(outputPath)) {
			if (Files.isDirectory(outputPath) || !Files.isWritable(outputPath)) {
				throw new FileNotFoundException("Unable to write to file. (" + outputPath.toString() + ").");
			}
		}
		try (OutputStream os = Files.newOutputStream(outputPath)) {
			writeDataSourceListToZip(dataSourceList, os);
		}
	}

	private static DataSourceList asDataSourceList(List<DataSourceInfo> list) {
		Builder dslBuilder = DataSourceList.builder();
		for(var dsInfo : list) {
			switch(dsInfo.type()) {
			case PATH:
				dslBuilder.add(dsInfo.name(), dsInfo.path());
				break;
			case STRING:
				dslBuilder.add(dsInfo.name(), dsInfo.value());
				break;
			default:
				// This should never happen.
				throw new IllegalStateException("Internal error: Found DataSourceInfo object of unknown type (" + dsInfo.type().toString() + ").");
			}
		}
		return dslBuilder.build();
	}

	private static void writeDataSourceListToZip(DataSourceList dsl, OutputStream out) throws IOException {
		FilenameList filenameList = new FilenameList();
		ZipOutputStream zipOutputStream = new ZipOutputStream(out);
		zipOutputStream.setComment("DataSourceList output");
		try {
			for(var ds : dsl.list()) {
				String filename = determineFilename(ds, filenameList);
				ZipEntry zipEntry = new ZipEntry(filename);
				
				zipOutputStream.putNextEntry(zipEntry);
				ds.inputStream().transferTo(zipOutputStream);
			}
		} finally {
			zipOutputStream.finish();
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
	
}
