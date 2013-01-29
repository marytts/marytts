#!/bin/bash

##################################
# wkdb_mysql_force_selection_by_id_file.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION=""

NUMARG=2
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
`basename $0` [config_file] [id_file]
	config_file: wkdb config file  
	id_file: file that contains the id of the sentence to set selected
EXAMPLE:
	`basename $0` /home/mary/wikidb_data/wkdb.conf id_selected.txt"
 
  exit 1
fi  


# read variables from config file
CONFIG_FILE="`dirname "$1"`/`basename "$1"`"
. $CONFIG_FILE

FILENAME=$2

# Load text file lines into a bash array.
ID_LIST=""
OLD_IFS=$IFS
IFS=$'\n'
for line in $(cat $FILENAME); do
    #printf "${line}\n"
    if [[ "$ID_LIST" -eq "" ]]; then
    	ID_LIST="${line}";
    else
        ID_LIST="$ID_LIST, ${line}";
    fi

done
IFS=$OLD_IFS

echo "update ${LOCALE}_dbselection set selected=true where id in ($ID_LIST);" 

mysql --user="$MYSQLUSER" --password="$MYSQLPASSWD" -e \
"use wiki; \
update ${LOCALE}_dbselection set selected=true where id in ($ID_LIST);"
