#!/bin/sh

##################################
# wkdb_setup.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="Create wkdb config file in wkdb data path"

NUMARG=3
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
	`basename $0` [wkdb data path] [wkdb locale] [mary locale]
wkdb data path: path for the wkdb data path a configuration file will be create
wkdb locale: the two letter locale for your language (e.g.: en, de, te, it, ...)
mary locale: the extended mary locale for your language (e.g.: en_US, en_GB, de, te, it, ...)
EXAMPLES:
	`basename $0` /home/mary/wikidb_data/ en en_US
	`basename $0` /home/mary/wikidb_data/ it it"
  exit 1
fi

WIKIDATAPATH=$1

mkdir -p $WIKIDATAPATH

if [ -f $WIKIDATAPATH/wkdb.conf ]
then
    echo "Warining file $WIKIDATAPATH/wkdb.conf already exists"
    echo "A backup file $WIKIDATAPATH/wkdb.conf.bak will be created"
    cp $WIKIDATAPATH/wkdb.conf $WIKIDATAPATH/wkdb.conf.bak
fi


FULLPATHWIKIDATAPATH="`(cd $WIKIDATAPATH; pwd)`"

# parameters from command line
echo "WIKIDATAPATH=$FULLPATHWIKIDATAPATH" > $WIKIDATAPATH/wkdb.conf
echo "WIKILOCALE=$2" >> $WIKIDATAPATH/wkdb.conf
echo "LOCALE=$3" >> $WIKIDATAPATH/wkdb.conf
# defaults
echo "MYSQLHOST=localhost" >> $WIKIDATAPATH/wkdb.conf
echo "MYSQLUSER=mary" >> $WIKIDATAPATH/wkdb.conf
echo "MYSQLPASSWD=wiki123" >> $WIKIDATAPATH/wkdb.conf
echo "MYSQLDB=wiki" >> $WIKIDATAPATH/wkdb.conf

echo "FEATUREMAKERRELIABILITY=strict" >> $WIKIDATAPATH/wkdb.conf
echo "FEATUREMAKERFEATURESFORSELECTION=\"phone,next_phone,selection_prosody\"" >> $WIKIDATAPATH/wkdb.conf

echo "SELECTEDSENTENCESTABLENAME=test" >> $WIKIDATAPATH/wkdb.conf
echo "SELECTEDSENTENCESTABLEDSCRIPTION=\"Testing table for selected sentences. Wikipedia locale: $LOCALE \"" >> $WIKIDATAPATH/wkdb.conf
echo "DATABASESELECTORSTOPCRITERION=\"numSentences 90 simpleDiphones simpleProsody\"" >> $WIKIDATAPATH/wkdb.conf


echo "Please look at the configuration file $WIKIDATAPATH/wkdb.conf and change it to according to your needs"



