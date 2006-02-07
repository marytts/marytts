HOW TO USE MARY IN ECLIPSE
==========================

This file describes how to import the MARY source code as a project into
Eclipse. These steps are written based on Eclipse 3.0, by Stephanie Becker and
Marc Schroeder. They should work under linux and, with minor adaptations, under windows.

1. Precondition: You must have downloaded the following files and unzipped
   them in the same directory:

   mary-source-2.X.X.zip

   and the binaries and voices packages required for your application.
   For a base system, you need:
      mary-common-X.Y.Z.zip
      mary-german-X.Y.Z.zip and/or mary-english-X.Y.Z.zip

   Normally you will want to add mbrola voices:
      mary-mbrola-common-X.Y.Z.zip
      mary-mbrola-german-X.Y.Z.zip
      mary-mbrola-english-X.Y.Z.zip

   For the mary client, you need:
      maryinterface-X.Y.Z.zip
   or, if you want to use the JSAPI version,
      maryclient-jsapi-X.Y.Z.zip

   The files will be unzipped into a mary-2.X.X directory. We will refer
   to this directory as MARY_BASE.

2. Verify/adapt a number of general settings in Eclipse. From the Eclipse
   menu, select "Windows"->"Preferences...". In the dialogue window that opens,
   verify/adapt the following settings:
   - Source file encoding: "Workbench"->"Editors"->"Text file encoding"
     must be UTF-8
   - Java version used: "Java"->"installed JREs" must be 1.4 or 1.5
   - Java source compatibility: "Java"->"Compiler"->"Compliance and Classfiles"
     "Use default compliance settings" must NOT be ticked; all of the following
     must be set to 1.4:
     "Compiler compliance level"
     "Generated .class files compatibility"
     "Source compatibility"

3. Import the MARY-project in Eclipse:
   In Eclipse, select from the "File" menu "Import", then click on
   "Existing Project into Workspace".
   Click "Next", then "Browse" and select the mary directory MARY_BASE 
   (for example "mary-2.1.1").
   Then click "Finish".
   If you get errors about jar files missing, verify the project settings
   under "Project"->"Properties"->"Java build path"->"Libraries". Remove or 
   "Add external JARs" as appropriate -- all required .jar files should be
   found under MARY_BASE/java.

At this stage, the code should compile without errors. Now, we need to
add some more settings in order to start the MARY server from within
Eclipse (e.g., for quick debugging).

4. Define a number of environment variables.
a) Under linux, add to the file ~/.bashrc:
      export MARY_BASE=/path/to/mary-2.x.x
      export SHPROT_BASE=$MARY_BASE/lib/modules/shprot
      export LD_LIBRARY_PATH=$MARY_BASE/lib/current_os:$LD_LIBRARY_PATH
 b) Under Windows, right click "My computer"->"Properties"->"Advanced"->
    "Environment variables...".
    Under "user variables", add new variables using the "New..." button:
       MARY_BASE c:\path\to\mary-2.x.x
       SHPROT_BASE c:\path\to\mary-2.x.x\lib\modules\shprot
    Under "system variables", "Edit..." the variable "Path". To the existing
    content of the Path variable, append the following:
       ;c:\path\to\mary-2.x.x\bin;c:\path\to\mary\lib\windows

5. To define a run target: From the menu, select "Run"->"Run...".
   Click "New" in order to add a Mary process:
   Name=Mary, "Main"->"Main class"=de.dfki.lt.mary.Mary
   In the "Arguments" tab, add the following lines into the "VM Arguments"
   field:
   
   -Dmary.base=/path/to/mary-2.1.1

   If you don't use Java 1.5, you also have to add the following into the same
   field:

   -Djava.endorsed.dirs=/path/to/mary-2.1.1/lib/endorsed 

   For debugging informations displayed in Eclipse, you also have to add the 
   following:

   -Dlog.tofile=false -Dlog.level=debug

   Click "Apply" to save these settings.

6. If you click on "Run" now, the process will start.
   
7. Open a shell, change to the MARY_BASE/bin directory and start the
   maryinterface with "./maryinterface".
