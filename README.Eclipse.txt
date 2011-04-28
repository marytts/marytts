Eclipse files can be automatically generated with maven as follows:

1. A single time, tell your Eclipse installation about your local Maven repository location as follows:

mvn -Declipse.workspace=<path-to-eclipse-workspace> eclipse:add-maven-repo


2. Automatically generate Eclipse project files for all marytts subprojects as follows: (in the top folder where this file is located):

mvn eclipse:eclipse

3. For every project that interests you (respecting dependencies, i.e. marytts-common is needed for marytts-signalproc etc.), do in Eclipse:
"File"->"Import..."->"Existing project into workspace..."->select the respective subproject folder, e.g. "marytts-common".

(or use the Eclipse plugin Multiple-Project Import/Export tool from http://eclipse-tools.sourceforge.net/updates/ -- install into Eclipse via Help->Install new software, giving the URI as download location; then import via File->Import...->Other->Multiple Projects. Downside of this approach: apparently this method does not import projects as SVN projects, so you don't see modified files in Eclipse. Your choice.)