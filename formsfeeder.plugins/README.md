# FormsFeeder Plugins Project
This sub-project contains sample plug-ins used in the formsfeeder.server service.

The plug-ins demonstrate what a plug-in project looks like and are used by the formsfeeder.server project's unit tests to ensure that the server/plugin interfaces provide the functionality required.

There are three sample plug-ins:

`debug` - This plug-in just returns information about the arguments passed in back to the caller as String output arguments.

`example` - This plug-in is an example of how to call AEM in order to render a PDF.

`mock` - This plug-in is a mock plug-in used to test various different scenarios (throwing exceptions, accessing the environment, etc.)
  
