#!/bin/sh

##################################
# wkdb_cleaning_up.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="Extract clean text and words from the wikipedia split files"

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


# Before using this program is recomended to split the big xml dump into 
# small files using the wikipediaDumpSplitter. 
#
# WikipediaProcessor: this program processes wikipedia xml files using 
# mwdumper (http://www.mediawiki.org/wiki/Mwdumper).
# mwdumper extract pages from the xml file and load them as tables into a database.
#
# Once the tables are loaded the WikipediMarkupCleaner is used to extract
# clean text and a wordList. As a result two tables will be created in the
# database: local_cleanText and local_wordList (the wordList is also
# saved in a file).
#
#
# Usage: java WikipediaProcessor -locale language -mysqlHost host -mysqlUser user -mysqlPasswd passwd 
#                                   -mysqlDB wikiDB -listFile wikiFileList.
#                                   [-minPage 10000 -minText 1000 -maxText 15000] 
#
#      -listFile is a a text file that contains the xml wikipedia file names (plus path) to be processed. 
#
#      default/optional: [-minPage 10000 -minText 1000 -maxText 15000] 
#      -minPage is the minimum size of a wikipedia page that will be considered for cleaning.
#      -minText is the minimum size of a text to be kept in the DB.
#      -maxText is used to split big articles in small chunks, this is the maximum chunk size. 

cd $WIKIDATAPATH

java -showversion -cp "$MARY_BASE/lib/*" marytts.tools.dbselection.WikipediaProcessor \
-locale "$LOCALE" \
-mysqlHost "$MYSQLHOST" \
-mysqlUser "$MYSQLUSER" \
-mysqlPasswd "$MYSQLPASSWD" \
-mysqlDB "$MYSQLDB" \
-listFile "wikilist.txt"

