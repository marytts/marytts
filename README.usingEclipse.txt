HOW TO USE MARY IN ECLIPSE
==========================

This file describes how to import the MARY source code as a project into
Eclipse. These steps are written based on Eclipse 3.0, by Stephanie Becker and
Marc Schroeder. They should work under linux and, with minor adaptations, under windows.

1. Precondition: You must have installed the MARY TTS system including the
"source" package. We will refer to the installation directory 
(e.g., "/home/myself/MARY TTS" on linux or "c:\Program Files\MARY TTS" on windows)
as MARY_BASE.

2. Verify/adapt a number of general settings in Eclipse. From the Eclipse
   menu, select "Windows"->"Preferences...". In the dialogue window that opens,
   verify/adapt the following settings:
   - Source file encoding: "Workbench"->"Editors"->"Text file encoding"
     must be UTF-8 (Note: may be under "General"->"Editors"->"Text file encoding"
     on the Linux version of Eclipse)
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
   (for example "/home/myself/MARY TTS").
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
      export MARY_BASE=/home/myself/MARY TTS
      export SHPROT_BASE=$MARY_BASE/lib/modules/shprot
      export LD_LIBRARY_PATH=$MARY_BASE/lib/linux:$LD_LIBRARY_PATH
 b) Under Windows, right click "My computer"->"Properties"->"Advanced"->
    "Environment variables...".
    Under "user variables", add new variables using the "New..." button:
       MARY_BASE "c:\Program Files\MARY TTS"
       SHPROT_BASE "c:\Program Files\MARY TTS\lib\modules\shprot"
    Under "system variables", "Edit..." the variable "Path". To the existing
    content of the Path variable, append the following:
       ;c:\Program Files\MARY TTS\bin;c:\Program Files\MARY TTS\lib\windows

5. To define a run target: From the menu, select "Run"->"Run...".
   Click "New" in order to add a Mary process:
   Name=Mary, "Main"->"Main class"=de.dfki.lt.mary.Mary
   In the "Arguments" tab, add the following lines into the "VM Arguments"
   field:
   
   -Xmx256m -Dmary.base=/path/to/MARY TTS -ea
   
   If you don't use Java 1.5, you also have to add the following into the same
   field:

   -Djava.endorsed.dirs="/path/to/MARY TTS/lib/endorsed"

   For debugging informations displayed in Eclipse, you also have to add the 
   following:

   -Dlog.tofile=false -Dlog.level=debug
   
   If you don't plan to use the Tibetan voice, add:
   
    -Dignore.tibetan.config

   Click "Apply" to save these settings.

6. If you click on "Run" now, the process will start.
   
7. Start the MARY GUI client by double-clicking the MARY client icon on the desktop
   (or from the command line in MARY_BASE/bin/maryclient).
