# Frequently Asked Questions
This page contains answers to frequently asked questions about the formsfeeder server.
 
### How do I arrange for a plug-in to be executed on some schedule?
We recommend that an external scheduler be used which then invokes the plugin using either the formsfeeder.client API or REST calls directly.  Some examples of how this could be done are:
* Use Cron to invoke a plug-in using the formsfeeder.client-cli utility
* Use Cron to invoke a plug-in using cUrl
* Use Cron to invoke a plug-in using wget
* Write a Spring Boot app that contains a @Scheduled method to invoke a plug-in using the formsfeeder.client library

