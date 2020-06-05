package formsfeeder.client.cli.parameters;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineAppParameters implements AppParameters {
	private static final String HOST_LOCATION_SHORT_OPTION = "h";
	private static final String HOST_LOCATION_LONG_OPTION = "host";	
	private static final String USER_CREDENTIALS_SHORT_OPTION = "u";
	private static final String USER_CREDENTIALS_LONG_OPTION = "user";	
	private static final String DATA_SOURCE_SHORT_OPTION = "d";
	private static final String DATA_SOURCE_LONG_OPTION = "data";	
	private static final String OUTPUT_LOCATION_SHORT_OPTION = "o";
	private static final String OUTPUT_LOCATION_LONG_OPTION = "output";
	private static final String PLUGIN_SHORT_OPTION = "p";
	private static final String PLUGIN_LONG_OPTION = "plugin";
	private static final String VERBOSE_SHORT_OPTION = "v";	
	private static final String VERBOSE_LONG_OPTION = "verbose";

	private final HostParameters hostParameters;
	private final AuthParameters authParameters;
	private final List<DataSourceInfo> dataSourceInfos;
	private final Path outputPath;
	private final boolean verbose;
	private final String plugin;
	
	/**
	 * @param hostLocation
	 * @param authParameters
	 * @param dataSourceInfos
	 * @param verbose
	 * @param plugin 
	 */
	private CommandLineAppParameters(HostParameters hostLocation, AuthParameters authParameters, List<DataSourceInfo> dataSourceInfos, Path outputPath, String plugin, boolean verbose) {
		super();
		this.hostParameters = hostLocation;
		this.authParameters = authParameters;
		this.dataSourceInfos = dataSourceInfos;
		this.outputPath = outputPath;
		this.plugin = plugin;
		this.verbose = verbose;
	}

	@Override
	public HostParameters hostParameters() {
		return this.hostParameters;
	}

	@Override
	public Optional<AuthParameters> authParameters() {
		return Optional.ofNullable(this.authParameters);
	}

	@Override
	public List<DataSourceInfo> dataSourceInfos() {
		return this.dataSourceInfos != null ? this.dataSourceInfos : Collections.emptyList();
	}

	@Override
	public Optional<Path> output() {
		return Optional.ofNullable(this.outputPath);
	}

	@Override
	public String plugin() {
		return this.plugin;
	}

	@Override
	public boolean verbose() {
		return this.verbose;
	}

	public static final AppParameters parseArgs(String[] args) throws ParseException {
		final Options options = generateOptions();
		CommandLine cmd = generateCommandLine(options, args);
		String hostLocation = cmd.getOptionValue(HOST_LOCATION_SHORT_OPTION);
		AuthParameters authParameters = asAuthParameters(cmd.getOptionValue(USER_CREDENTIALS_SHORT_OPTION));
		List<DataSourceInfo> dataSourceInfos = asDataSourceInfoList(cmd.getOptionValues(DATA_SOURCE_SHORT_OPTION));
		Path outputPath = asPath(cmd.getOptionValue(OUTPUT_LOCATION_SHORT_OPTION));
		String plugin = cmd.getOptionValue(PLUGIN_SHORT_OPTION);
		boolean verbose = cmd.hasOption(VERBOSE_SHORT_OPTION);
		return new CommandLineAppParameters(HostParameters.from(hostLocation), authParameters, dataSourceInfos, outputPath, plugin, verbose);
	}
	
	private static final AuthParameters asAuthParameters(String authParam) throws ParseException {
		if (authParam == null)
			return null;
		String[] splitParam = authParam.strip().split(":");
		if (splitParam.length != 2) {
			throw new ParseException("Can't parse auth parameter(" + authParam + ").");
		}
		return new AuthParameters.BasicAuthParameters(splitParam[0], splitParam[1]);
	}
	
	private static final List<DataSourceInfo> asDataSourceInfoList(String[] dsParams) {
		if (dsParams == null || dsParams.length == 0)
			return null;
		return Arrays.stream(dsParams)							// String comes in as name=value
					 .map(CommandLineAppParameters::asEntry)	// Split it into an Entry object name as the key and value as the value 
					 .map((e)->DataSourceInfo.from(e.getKey(), e.getValue()))	// Create a DataSourceInfo object from it
					 .collect(Collectors.toList());				// Collect into a List of DataSourceInfo objects.
	}
	
	private static AbstractMap.SimpleEntry<String, String> asEntry(String dsParam) {
		var index = dsParam.indexOf("=");
		if (index < 0) {
			return new AbstractMap.SimpleEntry<>(dsParam.strip(), "");
		} else { 
			return new AbstractMap.SimpleEntry<>(dsParam.substring(0, index).strip(), dsParam.substring(index+1).strip());
		}
	}
	
	private static final Path asPath(String path) {
		if (path == null)
			return null;
		return Paths.get(path);
	}
	
	/** 
	 * "Definition" stage of command-line parsing with Apache Commons CLI. 
	 * @return Definition of command-line options. 
	 */  
	private static Options generateOptions()  
	{  
	   final Option hostLocation = Option.builder(HOST_LOCATION_SHORT_OPTION)  
			      .required(true)
			      .hasArg(true)
			      .longOpt(HOST_LOCATION_LONG_OPTION)  
			      .desc("AEM Server Name and Port.")  
			      .build();
	   final Option authParam = Option.builder(USER_CREDENTIALS_SHORT_OPTION)  
			      .required(false)
			      .hasArg(true)
			      .longOpt(USER_CREDENTIALS_LONG_OPTION)  
			      .desc("User credentials (user:password).")  
			      .build();
	   final Option dataSource = Option.builder(DATA_SOURCE_SHORT_OPTION)  
			      .required(false)
			      .hasArg(true)
			      .longOpt(DATA_SOURCE_LONG_OPTION)  
			      .desc("Data Source (string or @filename).")  
			      .build();
	   final Option outputLocation = Option.builder(OUTPUT_LOCATION_SHORT_OPTION)  
			      .required(false)  
			      .longOpt(OUTPUT_LOCATION_LONG_OPTION)  
			      .hasArg(true)  
			      .desc("Location of output PDF file.")  
			      .build();  	   
	   final Option plugin = Option.builder(PLUGIN_SHORT_OPTION)  
			      .required(true)  
			      .longOpt(PLUGIN_LONG_OPTION)  
			      .hasArg(true)  
			      .desc("Name of plug-in to be invoked.")  
			      .build();  	   
	   final Option verbose = Option.builder(VERBOSE_SHORT_OPTION)  
			      .required(false)
			      .longOpt(VERBOSE_LONG_OPTION)  			      
			      .hasArg(false)  
			      .desc("Run utility in verbose mode.")  
			      .build();

	   final Options options = new Options();  
	   options.addOption(hostLocation);
	   options.addOption(authParam);
	   options.addOption(dataSource);
	   options.addOption(outputLocation);
	   options.addOption(plugin);
	   options.addOption(verbose);
	   return options;
	}
	/** 
	 * "Parsing" stage of command-line processing demonstrated with 
	 * Apache Commons CLI. 
	 * 
	 * @param options Options from "definition" stage. 
	 * @param commandLineArguments Command-line arguments provided to application. 
	 * @return Instance of CommandLine as parsed from the provided Options and 
	 *    command line arguments; may be {@code null} if there is an exception 
	 *    encountered while attempting to parse the command line options. 
	 */  
	private static CommandLine generateCommandLine(final Options options, final String[] commandLineArguments)  
	{  
	   final CommandLineParser cmdLineParser = new DefaultParser();  
	   CommandLine commandLine = null;  
	   try  
	   {  
	      commandLine = cmdLineParser.parse(options, commandLineArguments);  
	   }  
	   catch (ParseException parseException)  
	   {
			throw new IllegalArgumentException("Unable to parse command-line arguments "  
	         + Arrays.toString(commandLineArguments) + " due to: "  
	         + parseException + "\n" + getUsage(), parseException);  
	   }  
	   return commandLine;  
	}
	
	/** 
	 * Generate usage information with Apache Commons CLI. 
	 * 
	 * @param options Instance of Options to be used to prepare 
	 *    usage formatter. 
	 * @return HelpFormatter instance that can be used to print 
	 *    usage information. 
	 */  
	public static void printUsage(final PrintStream out) {
		final Options options = generateOptions();
		final HelpFormatter formatter = new HelpFormatter();
		final String syntax = "FormsFeeder Client CLI Utilty";
		out.println("\n=====");
		out.println("USAGE");
		out.println("=====");
		final PrintWriter pw = new PrintWriter(out);
		formatter.printUsage(pw, 80, syntax, options);
		pw.flush();
	}

	/** 
	 * Generate help information with Apache Commons CLI. 
	 * 
	 * @param options Instance of Options to be used to prepare 
	 *    help formatter. 
	 * @return HelpFormatter instance that can be used to print 
	 *    help information. 
	 */  
	public static void printHelp(final PrintStream out) {
		final Options options = generateOptions();
		final HelpFormatter formatter = new HelpFormatter();
		final String syntax = "Render PDF Utility";
		final String usageHeader = "Renders an interactive or non-interactive PDF.  Accepts the following arguments:";
		final String usageFooter = "Written by 4Point Solutions (https://www.4point.com/).";
		out.println("\n====");
		out.println("HELP");
		out.println("====");
		formatter.printHelp(syntax, usageHeader, options, usageFooter);
	}

	public static String getUsage() {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		CommandLineAppParameters.printUsage(out);
		return baos.toString();
	}
}
