# Forms Feeder Client Project
This project builds a client library that can be used to invoke plug-ins running under a Forms Feeder Server (see the formsfeeder.server project for details).

This library can be used by a client to make the calls to the server without them having to learn details of the underlying protocols.  They only need to know information provided by the group running the server, such as the location of the server (machine name and port), authentication details (like username/password)  and the name of the plugin being invoked.

The client needs a basic understanding of what the plug-in's arguments are in order to build a `DataSourceList` object that is passed to the server (and then to the plug-in).  They also need to understand what the returns are going to be (which comes in the form of another DataSourceList object).  Any Java developer should be able to work with the `DataSourceList.Builder` to create the proper DataSourceList and to work with the `DataSourceList.Deconstructor` to extract the results.

Here is some sample source code that illustrates what client code would look like:

```java

    FormsFeederClient client = FormsFeederClient.builder()
                                                .machineName("localhost")
                                                .port(8080)
                                                .basicAuthentication("username", "password")
                                                .plugin("Example")
                                                .build();

    DataSourceList input = DataSourceList.builder()
                                         .add("Template", "SampleForm.xdp")
                                         .add("Data, "SampleForm_data.xml")
                                         .add("Interactive", true)
                                         .build();

    DataSourceList result = client.accept(input);		// Call the FormsFeeder server.

    byte[] pdf = result.deconstructor().getByteArrayByName(FORMSFEEDERCLIENT_DATA_SOURCE_NAME);
    
```

