#!/bin/sh

##################################
# wkdb_database_selector.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################


# EXIT ERROR settings 
set -o errexit

DESCRIPTION="The DatabaseSelector program selects a phonetically/prosodically balanced recording script."

NUMARG=1
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
	`basename $0` [config_file] 
	config_file: wkdb config file  

EXAMPLE:
	`basename $0` /home/mary/wikidb_data/wkdb.conf" 
  exit 1
fi  


# read variables from config file
CONFIG_FILE="`dirname "$1"`/`basename "$1"`"
. $CONFIG_FILE

BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"

#Usage: java DatabaseSelector -locale language -mysqlHost host -mysqlUser user -mysqlPasswd passwd -mysqlDB wikiDB 
#        -tableName selectedSentencesTableName 
#        [-stop stopCriterion]
#        [-featDef file -coverageConfig file]
#        [-initFile file -selectedSentences file -unwantedSentences file ]
#        [-tableDescription a brief description of the table ]
#        [-vectorsOnDisk -overallLog file -selectionDir dir -logCoverageDevelopment -verbose]
#
#Arguments:
#-tableName selectedSentencesTableName : The name of a new selection set, change this name when
#    generating several selection sets. FINAL name will be: "locale_name_selectedSenteces". 
#    where name is the name provided for the selected sentences table.
#-tableDescription : short description of the selected sentences table. 
#    Default: empty
#-featDef file : The feature definition for the features
#    Default: [locale]_featureDefinition.txt for example for US English: en_US_featureDefinition.txt
#            this file is automatically created in previous steps by the FeatureMaker.
#-stop stopCriterion : which stop criterion to use. There are five stop criteria. 
#    They can be used individually or can be combined:
#    - numSentences n : selection stops after n sentences
#    - simpleDiphones : selection stops when simple diphone coverage has reached maximum
#    - simpleProsody : selection stops when simple prosody coverage has reached maximum
#    Default: "numSentences 90 simpleDiphones simpleProsody"
#-coverageConfig file : The config file for the coverage definition. 
#    Default: there is a default coverage config file in jar archive 
#             this file will be copied to the current directory if no file is provided.
#-initFile file : The file containing the coverage data needed to initialise the algorithm.
#    Default: /current-dir/init.bin
#-overallLog file : Log file for all runs of the program: date, settings and results of the current
#    run are appended to the end of the file. This file is needed if you want to analyse your results 
#    with the ResultAnalyser later.
#-selectionDir dir : the directory where all selection data is stored.
#    Default: /current-dir/selection
#-vectorsOnDisk: if this option is given, the feature vectors are not loaded into memory during 
#    the run of the program. This notably slows down the run of the program!
#    Default: no vectorsOnDisk
#-logCoverageDevelopment : If this option is given, the coverage development over time is stored.
#    Default: no logCoverageDevelopment
#-verbose : If this option is given, there will be more output on the command line during the run of the program.
#    Default: no verbose




#export MARY_BASE="[PATH TO MARY BASE]"
#export CLASSPATH="$MARY_BASE/java/"

#java -classpath $CLASSPATH -Djava.endorsed.dirs=$MARY_BASE/lib/endorsed \
#-Dmary.base=$MARY_BASE marytts.tools.dbselection.DatabaseSelector \

cd $WIKIDATAPATH

java -showversion -cp "$MARY_BASE/lib/*" marytts.tools.dbselection.DatabaseSelector \
-locale "$LOCALE" \
-mysqlHost "$MYSQLHOST" \
-mysqlUser "$MYSQLUSER" \
-mysqlPasswd "$MYSQLPASSWD" \
-mysqlDB "$MYSQLDB" \
-tableName "$SELECTEDSENTENCESTABLENAME" \
-tableDescription "$SELECTEDSENTENCESTABLEDSCRIPTION" \
-stop "$DATABASESELECTORSTOPCRITERION" \
-logCoverageDevelopment \
-verbose \

# vectorsOnDisk not used because of an error (Exception in thread "main" java.lang.NullPointerException: Could not get features for sentence ID xxx)
