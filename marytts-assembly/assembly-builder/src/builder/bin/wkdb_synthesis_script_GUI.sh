#!/bin/sh

##################################
# wkdb_synthesis_script_GUI.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################


# EXIT ERROR settings 
set -o errexit

DESCRIPTION="The SynthesisScriptGUI program allows you to check the sentences selected in the previous step, discard some (or all) and select and add more sentences."

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


cd $WIKIDATAPATH

java -showversion -ea -cp "$MARY_BASE/lib/*" marytts.tools.dbselection.SynthesisScriptGUI


