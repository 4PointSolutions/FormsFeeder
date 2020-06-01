package formsfeeder.client.cli;

import org.apache.commons.cli.ParseException;

import formsfeeder.client.FormsFeederClient;
import formsfeeder.client.cli.parameters.AppParameters;
import formsfeeder.client.cli.parameters.CommandLineAppParameters;

public class CommandLineClient {

	public static void main(String[] args) throws ParseException {
		AppParameters cliParameters = CommandLineAppParameters.parseArgs(args);

		String formsfeederServerName = null;
		int formsfeederServerPort = 0;
		FormsFeederClient underTest = FormsFeederClient.builder()
				  .machineName(formsfeederServerName)
				  .port(formsfeederServerPort)
				  .plugin("Debug")
				  .build();	
	}

}
