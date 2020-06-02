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
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
	private static Supplier<FileSystem> fsSupplier = FileSystems::getDefault; 

	public static void main(String[] args) {
		mainline(args, System.in, System.out, System.err);
	}
	
	public static void mainline(String[] args, InputStream in, PrintStream out, PrintStream err) {
		try {
			AppParameters cliParameters = CommandLineAppParameters.parseArgs(args);

			HostParameters hostParams = cliParameters.hostParameters();
			FormsFeederClient ffClient = FormsFeederClient.builder()
					  .machineName(hostParams.hostName())
					  .port(hostParams.hostPort())
					  .useSsl(hostParams.useSsl())
					  .plugin("Debug")
					  .build();	

			DataSourceList result = ffClient.accept(asDataSourceList(cliParameters.dataSourceInfos()));
			
			// TODO:  Write out results.
//			result.list().forEach((ds)->out.println("Found DataSource '" + ds.name() + "'."));
			
			FileSystem destFileSystem = fsSupplier.get();
			Optional<Path> output = cliParameters.output();
			int resultSize = result.list().size();
			if (output.isEmpty()) {
				// No output was specified.
				if (resultSize == 1) {
					// Only one output so send to stdout.
					DataSource dataSource = result.list().get(0);
					dataSource.inputStream().transferTo(out);
				} else {
					// TODO: Figure out what to do if we have multiple. 
				}
			} else {
				// a -o parameter was supplied
				Path outputPath = destFileSystem.getPath(output.get().toString());
				if (resultSize == 1) {
					// Only one output so send we want to send it to a file.
					if (Files.exists(outputPath)) {
						if (Files.isDirectory(outputPath) || !Files.isWritable(outputPath)) {
							throw new FileNotFoundException("Unable to write to file. (" + outputPath.toString() + ").");
						}
					}
					DataSource dataSource = result.list().get(0);
					try (OutputStream os = Files.newOutputStream(outputPath)) {
						dataSource.inputStream().transferTo(os);
					}
				} else {
					// Multiple outputs so we want to send to a directory.
					// TODO: Fill this in.
				}
				
			}
			
		} catch (FormsFeederClientException | ParseException | IllegalStateException | IOException e) {
			System.err.println(e.getMessage());
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
				throw new IllegalStateException("Internal error: Found DataSourceInfo object of unknown type (" + dsInfo.type().toString() + ").");
			}
		}
		return dslBuilder.build();
	}

	
}
