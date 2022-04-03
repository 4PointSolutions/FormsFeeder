# Data Sources

`DataSource` objects are the "lingua franca" of data within FormsFeeder.
They are the common format that all incoming data is converted to in order to communicate with a FormsFeeder plugin.
A set of `DataSource` objects are typically stored in a `DataSourceList`.
`DataSourceLists` have a large variety of convenience functions for retrieving `DataSources` from the
`DataSourceList` and converting them into Java object.

## DataSource objects
Each `DataSource` consists of a name, the data bytes, a mime type and an optional filename.
Most of the time, you don't deal with
`DataSource` objects directly (unless you need to construct a custom `DataSource`).
Most of the time, you extract a `DataSource`
from a `DataSourceList` and convert it to a Java object in one atomic operation.
Likewise, most of the time, you convert a
Java object to a `DataSource` and store it in a `DataSourceList` in one operation as well.

## DataSourceList objects
`DataSourceLists` are (as you might have already guessed) a list of `DataSources`.  It has most of the methods
that you would
expect on a Java List object but it also has a series of "convenience" methods for extracting and adding Java objects to
the list.

Each `DataSource` in the `DataSourceList` has a name.  The most common operations are to get a DataSource by name and
translate it to a Java object (often a `String` or an `InputStream`, but other types are also supported).

In order to construct a `DataSourceList`, you call `DataSourceList.builder()` to get a builder.
You invoke operations on the `Builder` to add `DataSources` to the list and when you are done, you call the
`build()` method on the `Builder`.
There is a convenience method called `build()` that takes a function that accepts and returns a `Builder` object.
This can be used to reduce the boilerplate code on when building a `DataSourceList`.  
It performs a `build()`, passes the resulting `Builder` to the function and then calls the `Builder`'s `build()` function afterwards.  The function can contain all the logic of adding objects to construct the `DataSourceList`.

Likewise, there is a `Deconstructor` object for extracting objects from a `DataSourceList`.  You call
`DataSourceList.deconstructor()` to get a `Deconstructor` for the list.
You can then use that `Deconstructor` object to 
pull `DataSources` out of the `DataSourceList` and convert them into Java objects.

There are several ways to get large blobs of data out of a `DataSourceList`.
One way is to retrieve the object as a `byte[]`.
This will return the whole object in memory.  The other is to retrieve the object as a `DataSource` and then call the
`inputStream()` method on the `DataSource`.
This latter approach may not require storing the entire set of data in memory
(it may be already be in memory anyway, depending on the underlying implementation which is subject to change).
The latter approach is preferred if saving memory is more important than speed.
The former approach will likely be faster but require more memory.

There are also some convenience object for storing/retrieving the bytes and the mime type at the same time (`Content`) 
and for storing/retrieving the bytes, mime type, and filename at the same time (`FileContent`).
These are useful for keeping everything together semantically without requiring the user to create their own type.

