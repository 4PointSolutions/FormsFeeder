# Invoking FormsFeeder

There are a variety of ways to make calls to the FormsFeeder server.  Choosing the correct one for your particular application will 
depend on the circumstances and environment of the client.  The following approaches are presented roughly in order of preference.
Try and select the one that is highest on the list that still meets your requirements.

## Client API Library

FormsFeeder provides a client API library that can be used to pass input to FormsFeeder.  This is the preferred option because, if there
are changes in the interface, the client library will be updated to compensate for those changes.  Upgrading to new versions of
FormsFeeder should only require substituting a newer version of the client library.

This approach requires client applications to be written in Java and support a version of Java compatible with the client library jar.

## Client REST API

Calling FormFeeder directly using REST calls is another alternative. The REST interface can be utilized by client applications that cannot
use the Client API library (for instance, because they are not written in Java).  The details of the [REST API are detailed here](REST_INTERFACE.md).

## Client Command Line

Client Applications that do not support REST requests but that support invocation of other programs can utilize the FormsFeeder Client Command Line Interface (CLI).  This is a .jar file that can be invoked to send files to FormsFeeder.  Client applications can write their data to files on disk and then invoke the Client CLI to send the data for FormsFeeder.

## Future Functionality

Another popular way to interface with legacy applications is via a watched folder.  At some point a watched folder interface may also be developed for FormsFeeder. 