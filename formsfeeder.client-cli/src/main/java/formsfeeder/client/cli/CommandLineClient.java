package formsfeeder.client.cli;

import org.apache.commons.cli.ParseException;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList;

import formsfeeder.client.FormsFeederClient;
import formsfeeder.client.FormsFeederClient.FormsFeederClientException;
import formsfeeder.client.cli.parameters.AppParameters;
import formsfeeder.client.cli.parameters.CommandLineAppParameters;
import formsfeeder.client.cli.parameters.HostParameters;

public class CommandLineClient {

	public static void main(String[] args) {
		try {
			AppParameters cliParameters = CommandLineAppParameters.parseArgs(args);

			HostParameters hostParams = cliParameters.hostParameters();
			FormsFeederClient ffClient = FormsFeederClient.builder()
					  .machineName(hostParams.hostName())
					  .port(hostParams.hostPort())
					  .useSsl(hostParams.useSsl())
					  .plugin("Debug")
					  .build();	

			// TODO: Build DataSourceList
			
			DataSourceList result = ffClient.accept(null);
			
			// TODO:  Write out results.
			
		} catch (FormsFeederClientException | ParseException e) {
			System.err.println(e.getMessage());
		}
	}

}
