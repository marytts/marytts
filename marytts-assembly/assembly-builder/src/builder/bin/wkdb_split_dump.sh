#!/bin/sh

##################################
# wkdb_split_dump.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="Split wikipedia dump file"

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


BASENAME="$WIKILOCALE"wiki-latest-pages-articles
XMLFILENAME=$BASENAME.xml

BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"


# This program splits a big xml wikipedia dump file into small 
# chunks depending on the number of pages.
#
# Usage: java WikipediaDumpSplitter -xmlDump xmlDumpFile -dirOut outputFilesDir -maxPages maxNumberPages 
#      -xmlDump xml wikipedia dump file. 
#      -outDir directory where the small xml chunks will be saved.
#      -maxPages maximum number of pages of each small xml chunk (if no specified default 25000). 



cd $WIKIDATAPATH

# clean previus files  
rm -fr xml_splits
rm -f wikilist.txt
mkdir -p xml_splits/

java -showversion -cp "$MARY_BASE/lib/*" marytts.tools.dbselection.WikipediaDumpSplitter \
-xmlDump "$XMLFILENAME" \
-outDir "xml_splits/" \
-maxPages 25000

# Create a list of files:
find xml_splits/ | sed '1,1d' > wikilist.txt
