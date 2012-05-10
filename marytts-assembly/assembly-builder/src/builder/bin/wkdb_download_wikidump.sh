#!/bin/sh

##################################
# wkdb_download_wikidump.sh
# Created on: May 9, 2012
# Author: Fabio Tesser
# Institution: CNR-ISTC, Padova - Italy
# Email: fabio.tesser@gmail.com
##################################

DESCRIPTION="Download the wikipedia dump file"

MINARG=1
if [ $# -lt $MINARG ]
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
CONFIG_FILE=$1
. $CONFIG_FILE

getDATE=`date '+%Y%m%d%H%M'`

DOWNLOADURL=http://download.wikimedia.org/"$WIKILOCALE"wiki/latest/
BASENAME="$WIKILOCALE"wiki-latest-pages-articles
FILENAME=$BASENAME.xml.bz2

cd $WIKIDATAPATH

# clean previus files
#rm $FILENAME

#if [ -f $WIKIDATAPATH/$FILENAME]
#then
	
#fi


echo "Downloading ..."
#wget $DOWNLOADURL/$FILENAME

echo "This wikipidia dump ($FILENAME) has been downloaded on $getDATE"
echo "$getDATE"> wkdb_download_timestamp.txt
echo "This wikipidia dump ($FILENAME) has been downloaded on $getDATE" >> wkdb_download_timestamp.txt


# make a backup copy 
cp  $FILENAME $BASENAME-backup-$getDATE.xml.bz2

echo "Extracting archive ..."
bunzip2 $FILENAME
echo "Done"


