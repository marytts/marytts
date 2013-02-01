#!/bin/bash

##################################
# wkdb_mysql_selectedSentences_clean.sh
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION=""

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

FILENAME=$2



mysql --user="$MYSQLUSER" --password="$MYSQLPASSWD" -e \
"use wiki; \
update ${LOCALE}_dbselection set selected=false; \
truncate ${LOCALE}_${SELECTEDSENTENCESTABLENAME}_selectedSentences;"
