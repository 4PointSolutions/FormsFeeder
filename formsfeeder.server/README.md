# FormsFeeder Server Project
This project builds a Spring Boot web application that can be used standalone or with a formsfeeder.client library.  It works in concert with plug-ins that contain custom code specific to the organization that is using the server.  The custom code is built as PF4J (https://pf4j.org/) plug-ins.

The server is an application layer that sits between clients and the custom PF4J plug-ins.  It acts as a traffic cop, redirecting incoming REST transactions to the appropriate custom plug-in.

The custom plug-ins are the libraries that actually perform the real work.  They reside in a directory called `plugins` under the server's current working directory.
They house custom logic specific to a particular type of transaction.  This custom logic understands what internal services need to be called and what AEM operations need to be performed and then makes the appropriate REST calls to complete these operations.

## FormsFeeder Server REST Interface
The FormsFeeder server acts as a traffic cop that allows a client to call custom logic contained in plugin jar files.  A plugin writer can write plugin code without regard to the REST mechanics required to invoke that logic.  They implement a known interface (`NamedFeedConsumer`) and build a plugin jar.  A client then has multiple ways to invoke that plugin (i.e. via REST GET or REST POST).

The server uses the value returned in the `NamedFeedConsumer.name()` method to direct transactions to a particular `NamedFeedConsumer`.  Transactions from the client should arrive at `/api/v1/{plugin_name}` where it calls the `NamedFeedConsumer` that returns a name that matches `{plugin_name}`.

The FormsFeeder server does not enforce the semantics of GET and POST (it does not currently support PUT).  It's up to the plugin writer and calling application to enforce these restrictions if they wish.
​
Basically a GET is treated the same as a POST from a FormsFeeder perspective.  The main difference is that a GET has no body, so you can only use query parameters, while a POST typically embeds input parameters into the body rather than using query parameters (although you can use query parameters on a POST if you wish).

Because we have no control over what the individual plugins do and don't do, we can't really enforce the restrictions that GETs are idempotent.  It's up to the plugin writer and the client to use whatever is appropriate.
 