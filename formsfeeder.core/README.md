# FormsFeeder Core Project
This sub-project contains the core classes that are shared between the servers and clients.

The classes fall into two packages:
* `api` - These are the interfaces that need to be implemented by clients and servers.
* `datasource` - These are the implementation classes around the primary objects that are passed in the API (`DataSourceList` and `DataSource`).

Clients (i.e. formsfeeder.client and formsfeeder.plugins) implement `NamedFeedConsumer` which passes them a `DatSourceList`.  As one would expect, a `DataSourceList` is a list of `DataSource` objects.  The `DataSourceList` has a associated `Builder` for creating a `DataSourceList` from standard Java objects and a `Deconstructor` for pulling specific `DataSource` objects from the list and/or transforming those `DataSource` objects into standard Java objects.

The rest of the classes are support classes that implement various facets necessary for the primary classes above.
