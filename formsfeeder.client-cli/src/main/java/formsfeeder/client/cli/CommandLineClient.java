package formsfeeder.client.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.ParseException;


import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;

import formsfeeder.client.FormsFeederClient;
import formsfeeder.client.FormsFeederClient.FormsFeederClientException;
import formsfeeder.client.cli.parameters.AppParameters;
import formsfeeder.client.cli.parameters.CommandLineAppParameters;
import formsfeeder.client.cli.parameters.DataSourceInfo;
import formsfeeder.client.cli.parameters.HostParameters;
import formsfeeder.client.cli.writer.DataSourceListWriter;
import formsfeeder.client.cli.writer.OutputType;

public class CommandLineClient {
	public static void main(String[] args) {
		mainline(args, System.in, System.out, System.err, FileSystems.getDefault());
	}
	
	public static void mainline(String[] args, InputStream in, PrintStream out, PrintStream err, FileSystem destFileSystem) {
		try {
			AppParameters cliParameters = CommandLineAppParameters.parseArgs(args);
			
			if (!cliParameters.verbose()) {	// If not verbose, then disable the INFO messages from FormsFeederClient object.
				Logger clientLogger = Logger.getLogger("formsfeeder.client.FormsFeederClient");	// Turn off the INFO logging in FormsFeederClient id .
				clientLogger.setLevel(Level.WARNING);
			}
			
			
			HostParameters hostParams = cliParameters.hostParameters();
			FormsFeederClient ffClient = FormsFeederClient.builder()
					  .machineName(hostParams.hostName())
					  .port(hostParams.hostPort())
					  .useSsl(hostParams.useSsl())
					  .plugin(cliParameters.plugin())
					  .build();	

			DataSourceList result = ffClient.accept(asDataSourceList(cliParameters.dataSourceInfos()));

			// Determine where we will be writing output to.
			OutputType outputType = cliParameters.output().map((p)->OutputType.from(result, p))	// If an Output parameter was supplied, then use it. 
														  .orElse(OutputType.from(result));		// If not, then infer it from the response.
			
			// Actually write out the result from the server.
			switch (outputType.type()) {
			case USE_FILENAME:
				DataSourceListWriter.write(result, destFileSystem.getPath(outputType.filename()));
				out.println("Result written to '" + outputType.filename() + "'.");	// If we're writing to a file, let the user know we've done it.
				break;
			case USE_STDOUT:
				DataSourceListWriter.write(result, out);
				if (System.console() != null) {
					out.println();	// If we're writing to a console, then append a newline.
				}
				break;
			default:
				throw new IllegalStateException("Unexpected OutputType encountered (" + outputType.type().toString() + ").");
			}
		} catch (FormsFeederClientException | ParseException | IllegalStateException | IOException e) {
			err.println(e.getMessage());
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

}
