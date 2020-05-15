# FormsFeeder PF4J/Spring Project
This sub-project is a copy of the Plugin 4 Java Spring Framework project (pf4j:pf4j-spring).

This version has been modified for Spring Boot (which the original project does not directly support) and to contain the extra interfaces that are shared between the formsfeeder.server project and the formsfeeder.plug-ins project.

Rather than use the existing project (which uses an older version of pf4j), I decided to create a copy and modify it.  This
eliminates one dependency and lets us use the latest pf4j version.  I don't understand Spring and Spring Boot well enough to know the best way to make things work in both so this was the easiest approach.  At some point, I would like to go back and revisit this decision and see if we can contribute at least some of this code back into the pf4j-spring project and start using it again.  In order for that to happen though, it would have to be more active than it currently is. 
