# FormsFeeder Debug Plugin Project
This sub-project contains a sample plug-in used in the formsfeeder.server service.  It returns information about the input parameters as String output parameters.  It is useful for debugging client interactions.

On servers with the plugin installed, clients can switch the endpoint they are calling to the Debug endpoint (i.e. /api/v1/Debug) in order to invoke this plugin and ensure that the arguments they think that are passing in are as expected. 
