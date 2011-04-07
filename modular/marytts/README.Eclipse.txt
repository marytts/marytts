Eclipse files can be automatically generated with maven as follows:

1. A single time, tell your Eclipse installation about your local Maven repository location as follows:

mvn -Declipse.workspace=<path-to-eclipse-workspace> eclipse:add-maven-repo


2. Automatically generate Eclipse project files for all marytts subprojects as follows: (in the top folder where this file is located):

mvn eclipse:eclipse

3. For every project that interests you (respecting dependencies, i.e. marytts-common is needed for marytts-signalproc etc.), do in Eclipse:
"File"->"Import..."->"Existing project into workspace..."->select the respective subproject folder, e.g. "marytts-common".
