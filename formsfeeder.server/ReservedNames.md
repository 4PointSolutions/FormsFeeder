# Reserved Names used by the FormsFeeder Server

All reserved names are prefixed with either `formsfeeder:` or `formsfeeder.`, depending on which is appropriate to a particular usage.

## DataSource Names

`formsfeeder:x-correlation-id`: This is a datasource created by the formsfeeder server and passed down into any plug-ins that are called.  It contains a unique id associated with any particular transaction.  It can be passed in by a calling system so that they can track a particular transaction.  If it is not passed in, then the formsfeeder server generates one and uses that.  The correlation id appears on any log entries associated with a transaction.  It is also passed back to the calling system in the response so that it can be used to locate issues related to a particular transaction in the log.

`formsfeeder:BodyBytes`: This is the name of a datasource that contains the bytes from the body of an incoming transaction if that incoming transaction was a POST which was not encoded as multipart/form-data.

## Attribute Names

`formsfeeder:Content-Disposition`: This is an attribute that the formsfeeder server looks for and uses on a single output DataSource object that comes from a plug-in.  It is used to set the type of the Content-Disposition http header when that single DataSource is streamed back to the caller in the HTTP Response.  It can be any string however it is typically restricted to either `inline` or `attachment`.  If this attribute is not found on the DataSource, then a value if `inline` is assumec.

## Application Property Names

Some application properties are very commonly used and could potentially be duplicated for each and every plug-in.  In order to avoid this and to provide some uniformity, these common properties have been assigned names that, by convention, should always be available in all environments.

`formsfeeder.plugins.aemHost`: This is the machine name or IP address of the machine that hosts AEM.

`formsfeeder.plugins.aemPort`: This is the port that AEM is listening to on the aemHost machine.

These properties are defined as constants in the EnvironmentConsumer interface, and so are available to any plug-ins that implement that interface.
