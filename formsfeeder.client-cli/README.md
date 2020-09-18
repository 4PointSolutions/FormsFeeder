# Forms Feeder Client CLI Utility Project
This project builds a command line client utility that can be used to invoke plug-ins running under a Forms Feeder Server (see the formsfeeder.server project for details).  It leverages the formsfeeder.client library to communicate with the Forms Feeder Server.

This utility can be used in a number of scenarios:

* To invoke a Forms Feeder plug-in from a shell script.
* To provide a working sample of how a particular plugin is called.
* To provide a command line sample that demonstrates a particular plug-in behaviour.
* To duplicate a client issue without requiring the client code.

The utility is a stand-alone executable .jar.  It can be invoked from the command line using the java command.  The typical usage is:

<code>java -jar formsfeeder.client-cli-<i>version</i> <i>[args]</i></code> where <i>version</i> is the version of the jar (currently `0.0.1-SNAPSHOT`) and <i>[args]</i> are the command line argument parameters outlined below. 

The available command line arguments are as follows:

<p><code>-h <i>hostlocation</i></code> where <i>hostlocation</i> is a string containing the location of the host (e.g. <code>http://localhost:8080/</code> or <code>https://formsfeeder.example.com</code>.  The <i>hostlocation</i> must be http or https protocol.  It may optionally end with a slash.  It must contain a machine name and may optionally contain a port.  This parameter is required.</p>

<p><code>-p <i>plugin</i></code> where <i>plugin</i> is the name of the plug-in to be invoked.  It is case-sensitive.  Examples of plugins that are part of the <code>formsfeeder.server</code> project are Debug, Mock, and Example.  This parameter is required.</p>

<p><code>-u <i>username</i>:<i>password</i></code> where <i>username</i> and <i>password</i> are the credentials that will be used to sign in to the Forms Feeder Server.  Currently, only Basic authentication is supported.  If this parameter is omitted, then no authentication will be provided (i.e. it will be an anonymous request).</p>

<p><code>-d <i>parameter name</i>=<i>parameter value</i></code> where <i>parameter name</i> is a plug-in input parameter name and  <i>parameter value</i> is the value to be passed in that parameter.  The <i>parameter value</i> can be a simple string or, if the parameter starts with @, it can contain the contents of a file from the local filesystem.  For example, the command args could contain <code>-d template=crx:/content/dam/formsanddocuments/SampleForm.xdp</code> which could be the location of file in the AEM CRX repository or it could contain <code>-d template=@SampleForm.xdp</code> which would pass the entire contents of the file SampleForm.xdp from the current working directory to the plugin.  This parameter is optional and repeating (i.e. you can send as many <code>-d</code> parameters as your command line allows).</p>

<p><code>-o <i>outputlocation</i></code> where <i>outputlocation</i> is a string containing the location where the output should be routed.  This parameter is optional.  If provided, the output from the plug-in is routed to a file with that filename on the local machine.  If it is not provided, but the output is a single output with a filename supplied, then then that filename will be used.  If the <code>-o</code> parameter is not provided and, either no filename was provided or the plug-in returned multiple outputs, then the result is sent to stdout.  Providing <code>-o ---</code> will force the output to go to stdout regardless of the contents of the response from the plug-in.</p><p> The format of the output will depend on the number of output parameters from the plug-in.  If the output from the plug-in is a single Data Source, then the output will be the contents of that Data Source.  If the output from the plug-in contains multiple Data Sources, then the output will be in .zip format with one entry per Data Source.</p>

<p><code>-v</code> this parameter is a switch that turns on verbose logging.  It is an optional parameter.</p>  

