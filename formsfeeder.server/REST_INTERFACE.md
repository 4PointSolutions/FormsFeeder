# FormsFeeder REST Interface
The FormsFeeder REST interface is designed to be flexible in order to:
  * handle a variety of scenarios
  * to allow for as simple an interface as is it can be for the data to be passed

## URL to Invoke
Whether a plugin is to be invoked via a GET or a POST, the URL to invoke will always be 
http(s)://<machinename[:port]>/api/v1/<ConsumerName> where <ConsumerName> is the name returned from the name() method 
of the class that implements the NamedFeedConsumer interface.

## GET Interface
This is the simplest way to invoke FormsFeeder and is suitable for any call that includes only simple String parameters.
Each string parameter is passed as a query parameter on GET request to the NamedFeedConsumer's URL (as outlined above).
Each query parameter is converted into a String DataSource.  Parameters may occur multiple times if multiple values need to
be passed in.

## POST Interface
This is the most flexible way to invoke FormsFeeder.  It is suitable for passing large blobs of data that are not suitable
for passing via query parameters.  JSON and XML data files are typical examples.  It is also suitable for passing in simple
data in large quantities.

The caller may still pass simple `String` parameters via the command line, but blobs must be passed in the body of the POST
request.  

### Body Format

The body of the POST is interpreted as one of two things.
It is either interpreted as a single `DataSource`, or as wrapper that contains multiple `DataSources`.
The interpretation of the format is based on the content-type.
If the content-type is `mutipart/form-data` or `application/json`, the incoming data is broken up into multiple `DataSources`.
If the content-type is anything else, it treats the entire body of the POST as a single `DataSource`.

If the body of the POST is determined to be a single `DataSource`, then it is passed to the `NamedFeedConsumer` as a `DataSource` with a name of "formsfeeder:BodyBytes".
The media type of the `DataSource` will reflect the value of the content-type of the POST body. 

If the body of the POST is determined to be a wrapper type, then the wrapper is broken apart and each entry within the
wrapper becomes a `DataSource`.  If an entry contains multiple sub-entries, then the entry becomes a `DataSourceList`
containing one `DataSource` per entry instead.

For a `multipart/form-data` body, each part becomes a `DataSource` (with a mimetype based on that part's content-type).

For an `application/json` file, each JSON field becomes a `DataSource`.
Each array entry becomes a `DataSource` with the same name as the array's name.
Each JSON object within the incoming data becomes a `DataSourceList` containing all the fields within that object in
`DataSource`s.

For more information on `DataSource`s and `DataSourceList`s, see [the DataSources page](DATASOURCES.md).