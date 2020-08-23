# FormsFeeder
This project provides a microservice that can be customized for individual AEM Forms customers in order to build a complete customer solution that includes an AEM Forms server.  The Forms Feeder service acts as a front-end that wraps (and "feeds") the AEM Forms services with custom code to perform commonly required tasks such as gathering pre-population data, posting submitted data to internal systems, generating documents of record and archiving document data and artifacts.

The framework provides a “plugin” capability to allow each customer to extend the microservice in ways that are specific to that customer’s requirements.

The framework also provides client libraries and client command line interface for invoking the microservice, allowing client applications to access remote AEM Forms functionality through the framework without having to understand the REST protocols that underly that communication.

## Goals of this project
The goals of this project are:

* To provide a common framework for building AEM Forms based solutions.
* To provide a standard interface for invoking custom AEM Forms functionality.
* To remove the need to understand REST in order to invoke custom functionality made available through the framework.
* To provide a shared codebase that is common to many different AEM Forms solutions

