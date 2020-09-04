# FormsFeeder Plugins Project
This sub-project contains sample plug-ins used in the formsfeeder.server service.

The plug-ins demonstrate what a plug-in project looks like and are used by the formsfeeder.server project's unit tests to ensure that the server/plugin interfaces provide the functionality required.

There are three sample plug-ins:

`debug` - This plug-in just returns information about the arguments passed in back to the caller as String output arguments.

`example` - This plug-in is an example of how to call AEM in order to render a PDF.

`mock` - This plug-in is a mock plug-in used to test various different scenarios (throwing exceptions, accessing the environment, etc.)
  
## What are Plugins?

Plugins contain custom-built functionality specific to a particular client.  The FormsFeeder server acts as a front-end to an AEM server.  Plugins can be used to customize the behaviour of that front end in order to perform pre-processing and post-processing of AEM transactions.  For example, you could have a plugin that performs several REST service calls in order to gather data to prepopulate a form before calling AEM to generate a form.  Likewise the plugin could post a copy of a PDF document of record to an archive system after AEM generates a PDF.  Plugins are the easiest way to develop custom processing around AEM transactions.

Plugins are based on the PF4J Framework.

PF4J defines two things:
*   A Plugin – This is a .jar that extends the server functionality.
*   An Extension – This is a class that implements specific interface (ExtensionPoint) and will be called by the server.    A plug-in can contain multiple extension classess.

FormsFeeder looks for PF4J extensions that implement a specific interface (NamedFeedConsumer).  This interface provides two things:
*   A `name()` method that tells the FormsFeeder server the name of this extension.  This is used as part of the URL that will be used to invoke the extension (so for example, the Debug plugin returns the string “Debug” from the name() method and is invoked by calling `/api/v1/Debug` URL on the server).
*   An `accept()` method that receives a `DataSourceList` object as input and returns another `DataSourceList` object as output.  This is how the extension is called.  A transaction that comes into the server has the input arguments placed in the incoming `DataSourceList`.  The extension then takes those arguments out, processes them and builds a new `DataSourceList` that it returns to the server code.  The server code then translates the `DataSourceList` into a response.

The `DataSourceList` object is the lingua franca of the FormsFeeder code.  It is, as the name implies, a list of `DataSource` objects.  `DataSource` objects are modelled on the `javax.activation.DataSource` objects.  They are lower level constructs that make up a `DataSourceList` however in many cases, you never have to deal with them directly.  Instead, you deal with `DataSourceList.Builder` (for creating `DataSourceList` objects) and `DataSourceList.Deconstructor` (for extracting data from a `DataSourceList`).  

The typical plugin will grab the `Deconstructor` from an incoming `DataSourceList`, extract the parameters it is looking for, and then perform its processing.  After processing, it will use a `DataSourceList.Builder` object to create a `DataSourcelist` from the outputs and return that `DataSourceList` to the server.

