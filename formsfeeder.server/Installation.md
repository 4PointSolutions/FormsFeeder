# Installation of FormsFeeder Server

## Prerequisites

Java 11 must be installed on the machine that will be running the formsfeeder.server jar.  Ensure this by logging on to 
that server using the user that will running the formsfeeder server.  Type java -version to ensure that java is in the
path and the version is at least Java 11.

## Files to be installed

The following files and directories should be on the server before launching the formsfeeder server:

* `formsfeeder.server-0.0.1-SNAPSHOT.jar` file - This is the formsfeeder server itself.  It can reside in any directory on the server however it usually resides in the directory where the process will be launched from.
* `application.properties` file - This is the properties file that contains configuration information used by the server and the server's plug-ins.  When the server is running, it looks for this file in the current working directory, so it usually resides in the same directory as the jar file.
* `plugins` directory - This is the directory where the plug-ins must reside.  The server looks for this directory within its current working directory, so this typically resides underneath the same directory as the jar file.

## Plug-in files

The following files *may* be installed into the `plugins` directory if their functionality is required.

* `debug-0.0.1-SNAPSHOT.jar` file - This is a plug-in that echoes back the parameters that are passed into it.  It is useful to validate that the connection to the server is working.
* `mock-0.0.1-SNAPSHOT.jar` file - This plug-in is emulates a working plugin.  It can be used to simulate various conditions without requiring an AEM server to be available. 
* `example-0.0.1-SNAPSHOT.jar` file - This is a plug-in that invokes AEM in order to render a PDF.  It therefore requires a working AEM instance to be available and it assumes that the application.properties file has been modified to point to that AEM server.  The AEM server needs to have the fluentforms.core and rest-service.server bundles installed on it.  The example plugin takes three parameters:
    * `template` - This is the path to an XDP that resides on the same server as the formsfeeder.server.
    * `data` - This is a path to an XML file that resides on the same server as the formsfeeder.server.
    * `interactive` - This is a boolean value (i.e. `true` or `false`) that indicates whether an interactive or non-interactive PDF is generated.

# Running the FormsFeeder Server

Assuming the server .jar is installed in a directory that contains the applications.properties file and has a plugins subdirectory, then the server can be started by running the .jar file from the command line using the following command:

`java -jar  formsfeeder.server-0.0.1-SNAPSHOT.jar`

You should see the Spring Boot app log start scolling by.  By default, the application runs on port 8080. The firewall on the machine that is running the server must allow traffic on the server's port. 

# Exercising the FormsFeeder Server

Once the server has been installed with the three plugins provided, the code can be exercised from a browser (for simple requests) and using cUrl (for more complex requests).  This section outlines examples of how to do that. 

## Browser URLs

The server is designed to understand query parameters and for plug-in operations that return only a single value to convert that single value into a response that a browser will be able to interpret.  This allows plugins that return only a single value to be called directly from a browser.  Here are some examples:  

`http://machine:port/api/v1/Debug?QueryParmeter=QueryParameterValue` - You should get a response `Found datasource: name='QueryParmeter' with value 'QueryParameterValue' with contentType 'text/plain; charset=UTF-8'.`

`http://machine:port/api/v1/Mock?scenario=ReturnXml` - You should see some XML appear in the browser.

`http://machine:port/api/v1/Mock?scenario=ReturnPdf` - You should see a PDF get downloaded.

## cUrl commands

Similarly, the server can be driven by multipart/form-data POSTs.  From the command line, these can be created using cUrl.  Here are some example commands that correspond to the browser URLs:

`curl -FQueryParameter=QueryParameterValue http://machine:port/api/v1/Debug -o response.txt` - This should create a file called response.txt that contains the text `Found datasource: name='QueryParmeter' with value 'QueryParameterValue' with contentType 'text/plain; charset=UTF-8'.`

`curl -Fscenario=ReturnXml http://machine:port/api/v1/Mock -o test.xml` - This should create a file called test.xml that contains some XML data.

`curl -Fscenario=ReturnPdf http://machine:port/api/v1/Mock -o test.pdf` - This should create a file called test.pdf that is a valid pdf.


## End-To-End Tests With an AEM Server

So far the tests have been using mock or debug plugins that do not require a working AEM instance.  To perform a complete end-to-end test using the example plug-in, some configuration needs to be done on the server where the formsfeeder server is running.  These steps are outlined next:

1. Modify the copy of `application.properties` in the formsfeeder server directory to set the `formsfeeder.plugins.aemHost` and `formsfeeder.plugins.aemPort` values to point to your AEM instance.
1. Restart the formsfeeder server after making changes to the `application.properties` file.  These values are only read on startup. 
1. As noted earlier, the AEM instance must have the `fluentforms.core` and `rest-service.server` bundles installed on it.
1. Copy the `SampleForm_data.xml` and `SampleForm.xdp` files from the formsfeeder.server `src/test/resources` directory onto the directory where formsfeeder server is running.

### Running the End-to-End Tests

1. Type the following url into your browser `http://machine:port/api/v1/Example?template=SampleForm.xdp&data=SampleForm_data.xml&interactive=false`.  The server should respond by sending a non-interactive PDF to your browser window.
1. Type the following url into your browser `http://machine:port/api/v1/Example?template=SampleForm.xdp&data=SampleForm_data.xml&interactive=true`.  The server should respond by sending an interactive PDF to your browser window.  NOTE: Interactive PDFs are dynamic by default and will not typically display properly using the built-in browser PDF viewers.  If you see a page that says <i>"The document you are trying to load requires Adobe Reader 8 or higher."</i> then you need to save the file locally and open it in Adobe Reader (or move on to the next command).
1. Type in the following cUrl command to generate the same PDF as the previous step: `curl -Ftemplate=SampleForm.xdp -Fdata=SampleForm_data.xml -Finteractive=true  http://machine:port/api/v1/Example -o test2.pdf`.  This should create a PDF file in the local directory called test2.pdf.  You should be able to open it with Adobe Reader.

Congratulations!  If all these steps succeed, your formfeeder server is up and running!
 
