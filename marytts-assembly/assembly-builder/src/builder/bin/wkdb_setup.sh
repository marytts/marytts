#!/bin/sh

##################################
# wkdb_setup.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="Create wkdb config file in wkdb data path"

MINARG=2
if [ $# -lt $MINARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
	`basename $0` [wkdb data path] [wkdb locale]
wkdb data path: full path for the wkdb data path a configuration file will be create
wkdb locale: the two letter locale for your language (e.g.: en, de, te, it, ...)
EXAMPLE:
	`basename $0` /home/mary/wikidb_data/ en"
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


echo "WIKIDATAPATH=$FULLPATHWIKIDATAPATH" > $WIKIDATAPATH/wkdb.conf
echo "WIKILOCALE=$2" >> $WIKIDATAPATH/wkdb.conf





