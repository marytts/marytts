******************************************************
* Documentation for the Speech Corpus selection tool *
******************************************************

Anna Hunecke, August 2007

The selection tools consist of three Java programs:

- DatabaseSelector : program for selecting the speech corpus

- FeatureMakerMaryServer :  program for building the text corpus
  from which to select

- SortTestResults : program for sorting the text results according to 
  the four coverage measures


Furthermore, there are two perl scripts:

- features2sentences.pl : program for converting a list of selected
  feature files to a list of sentence files and their content.

- sentences2features.pl : program for converting the file produced
  by features2sentences.pl into two files containing a list of feature
  files. One file is for those feature files that are wanted in the
  final script and one file for the bad feature files that are too be
  ignored in further selection steps.


There are also three files that can be used for selection:

- covDef.config : file containing the settings for the selection
  algorithm 

- featureDefinition.txt : feature definition file used by selection
  algorithm. Matches the file german-targetfeatures-selection.config.

- german-targetfeatures-selection.config : file for computing the
  features with the FeatureMaker classes. Matches the given feature
  definition file. 

In the following, the usage of the programs is documented in more
detail. 


********************
* DatabaseSelector *
********************

Selects a set of sentences for a speech corpus

*** Usage: ***

java -cp /path/to/mary/java/mary-common.jar 
de.dfki.lt.mary.dbselection.DatabaseSelector 
-basenames <file> 
-featDef <file> 
-stop <stopCriterion>

Optional arguments: 
-coverageConfig <file>
-initFile <file>
-selectedSentences <file>
-unwantedSentences <file>
-vectorsOnDisk
-overallLog <file>
-selectionDir <dir>
-logCoverageDevelopment
-verbose


*** Arguments: ***

-basenames <file> : The list of feature files to select from. The file
 either starts with the number of feature files followed by the actual
 list, or it contains just the list. The first version might be
 quicker when there are a great number of files. 

-featDef <file> : The feature definition for the feature files. It has
 to be consistent with the features. The given feature definition file
 can be used if the file german-targetfeatures-selection.config was
 used for computing the features.

-stop <stopCriterion> : which stop criterion to use. 
 There are five stop criteria: 
  - numSentences <n> : selection stops after n sentences
  - simpleDiphones : selection stops when simple diphone coverage has
    reached maximum 
  - clusteredDiphones : selection stops when clustered diphone
    coverage has reached maximum 
  - simpleProsody : selection stops when simple prosody coverage has
    reached maximum 
  - clusteredProsody : selection stops when clustered prosody coverage
    has reached maximum 
 
 The criteria can be used individually or can be combined. Examples:
  - stop criteria simpleDiphones and simpleProsody: selection stops
    when both criteria are fulfilled
  - stop criteria simpleDiphones and numSentences 300: selection stops
    when simpleDiphone coverages reaches maximum or number of
    sentences is 300. 


-coverageConfig <file> : The config file for the coverage
 definition. Contains the settings  for the current pass of the
 algorithm. Standard config file is selection/covDef.config. You can
 use the file covDef.config as a template. 

-vectorsOnDisk: if this option is given, the feature vectors are not
 loaded into memory during the run of the program. This notably slows down
 the run of the program! 

-initFile <file> : The file containing the coverage data needed to
 initialise  the algorithm. This file is automatically created by the
 program. Standard init file is selection/init.bin

-selectedSentences <file>: File containing a list of sentences
 selected in a previous pass of the algorithm. They are added to the
 cover before selection starts. The sentences can be part of the
 basename list.

-unwantedSentences <file>: File containing those sentences that are to
 be removed from the basename list prior to selection.

-overallLog <file> : Log file for all runs of the program: date,
 settings and coverage of the current pass are appended to the end of
 the file. This file is needed if you want to analyse your results
 with the ResultAnalyser later on. 

-selectionDir <dir> : the directory where all selection data is
 stored. Standard directory is ./selection

-logCoverageDevelopment : If this option is given, the coverage
 development over time is stored in text format. It can be converted
 into a table/diagram with OpenOffice or similar programs.

-verbose : If this option is given, there will be more output on the
 command line during the run of the program.



******************************
* FeatureMakerMaryServer * 
******************************

Takes a list of files containing text. For each file, the text is
divided into sentences and for each sentence, the features are
computed and features and sentences are written to disk. Sentences
with unreliable phonetic transcriptions are sorted out. The result is
a list of feature files that can be used by DatabaseSelector.

FeatureMakerMaryServer needs a running Mary server. The most important 
thing is that the target feature file 
german-targetfeatures-selection.config is used for the computation of
the features. This file has to be in the /conf directory of the Mary
installation.   


*** Usage: ***

Startup script for Windows. Save the following lines in <filename>.bat
and edit them according to your needs. Start the script from the command line:

@echo off
set MARY_BASE=drive:\path\to\mary

set CLASSPATH="%MARY_BASE%\java\mary-common.jar;
%MARY_BASE%\java\log4j-1.2.8.jar;%MARY_BASE%\java\mary-german.jar; 
%MARY_BASE%\java\freetts.jar;%MARY_BASE%\java\jsresources.jar"

java -Xmx512m -cp %CLASSPATH%
"-Djava.endorsed.dirs=%MARY_BASE%\lib\endorsed"  
"-Dmary.base=%MARY_BASE%" 
de.dfki.lt.mary.dbselection.FeatureMakerMaryServer <arguments>


Startup script for Linux:

export MARY_BASE="/path/to/mary"

export CLASSPATH="$MARY_BASE/java/mary-common.jar:
$MARY_BASE/java/log4j-1.2.8.jar:$MARY_BASE/java/mary-german.jar:
$MARY_BASE/java/freetts.jar:$MARY_BASE/java/jsresources.jar"

java -classpath $CLASSPATH 
-Djava.endorsed.dirs=$MARY_BASE/lib/endorsed 
-Dmary.base=$MARY_BASE 
de.dfki.lt.mary.dbselection.FeatureMakerMaryServer <arguments>



*** Arguments: ***

-textFiles <file>: File containing the list of text files to be
 processed. Default: textFiles.txt

-doneFile <file>: File containing the list of files that have already
 been processed. This file is created automatically during the run of
 the program. Default: done.txt

-featureDir <file>: Directory where the features are stored. Default:
 features1. Per default, appropriate sentence files are stored in
 sentences1. The index of feature/sentence directory is increased when
 the feature dir is full. 

-timeOut <time in ms>: The time in milliseconds the Mary server is
 allowed to split the text of a file into sentence. After the limit is
 exceeded, processing on this file is stopped, and the program
 continues to the next file. Default 30000ms. 

-unreliableLog <file>: Logfile for the unreliable sentence. Default:
 unreliableSents.log 

-credibility <setting>: Setting that determnines what kind of
 sentences are regarded  as credible. There are two settings: strict
 and lax. With setting strict, only those sentences that contain words
 in the lexicon or words that were transcribed by the preprocessor are
 regarded as credible; the other sentences as unreliable. With setting
 lax, also those words that are transcribed with the Denglish and the
 compound module are regarded credible. Default: strict 

-basenames <file>: File containing the list of feature files that can
 be used in the selection algorithm. Default: basenames.lst



*******************
* SortTestResults *
*******************

Sort the results of all tests. For this, a file containing all results
is needed. This file can be created by starting the DatabaseSelector
with the option -overallLog <file> at each run. 

The program produces six files. For each of the four coverage
measures, the results are sorted according to which result has the
highest coverage for the given measure and stored in a
file. Also, a file containing those results that are the same
but probablty were achieved with different settings is constructed.
And lastly, the program creates a file containing the results sorted
according to the number of sentences selected.

The files are written to the directory where the program is started from.  

*** Usage: ***

java -cp /path/to/mary/java/mary-common.jar 
de.dfki.lt.mary.dbselection.SortTestResults 
<resultFile> (justSettings or shortResults)


*** Arguments: ***

<resultFile> : file containing the results of DatabaseSelector. 

(justSettings/shortResults) : this argument is optional. When this
 option is not given, the program prints the full information for each
 pass in the output files. The argument justSettings has the effect
 that only the settings of the passes are printed. shortResults slightly
 shortens the full information.  

 For just settings, the following format is used:
 rank: coverage	    number of basenames; stop criterion; units are
 simple (SD) or clustered diphones (CD); frequency measure; wanted
 weights on phone/nextPhone/prosody level; number by which the wanted
 weight is divided; number of sentences selected 



**********************
* features2sentences *
**********************

Convert a list of selected  feature files to a list of sentence files
and the sentences they contain. This way, a synthesis script can be
created.

*** Usage: ***

perl features2sentences.pl <featurefile> <scriptfile>

*** Arguments: *** 

<featurefile> : file containing the list of feature files 
 
<scriptfile> : file to write the synthesis script to




**********************
* sentences2features *
**********************

Convert the file produced by features2sentences.pl into two files
each containing a list of feature files. 
One file is for those feature files that are wanted in the final
script.  
The other file is for the "bad" feature files that are too be ignored
in furthe selection steps. 

In the script, the corrector can mark bad feature files by a star (*)
in front of the file name. 
  
*** Usage: ***

perl sentences2features.pl <scriptfile> <goodfeaturefile> <badfeaturefile>

*** Arguments: ***

<scriptfile> : the synthesis script file 

<goodfeaturefile> : file to write the good feature files to

<badfeaturefile> : file to write the bad feature files to
