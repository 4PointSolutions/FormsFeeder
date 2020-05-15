# FormsFeeder Server Project
This project builds a Spring Boot web application that can be used standalone or with a formsfeeder.client library.  It works in concert with plug-ins that contain custom code specific to the organization that is using the server.  The custom code is built as PF4J (https://pf4j.org/) plug-ins.

The server is an application layer that sits between clients and the custom PF4J plug-ins.  It acts as a traffic cop, redirecting incoming REST transactions to the appropriate custom plug-in.

The custom plug-ins are the libraries that actually perform the real work.  They reside in a directory called `plugins` under the server's current working directory.
They house custom logic specific to a particular type of transaction.  This custom logic understands what internal services need to be called and what AEM operations need to be performed and then makes the appropriate REST calls to complete these operations.

 