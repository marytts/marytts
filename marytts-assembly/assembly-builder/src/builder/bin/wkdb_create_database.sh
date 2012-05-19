#!/bin/sh

##################################
# wkdb_create_database.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="mysql database creation"

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



# DB CREATION, this ask for mysql root password
echo "Please insert your mysql root password below."
mysql --user="root" -p -e \
"create database $MYSQLDB; \
grant all privileges on $MYSQLDB.* to $MYSQLUSER@$MYSQLHOST identified by \"$MYSQLPASSWD\"; \
flush privileges;"

echo "$MYSQLDB database has been created, all privileges are granted to user $MYSQLUSER in the $MYSQLHOST and the password $MYSQLPASSWD"



