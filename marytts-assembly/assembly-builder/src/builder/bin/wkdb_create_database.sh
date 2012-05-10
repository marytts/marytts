#!/bin/sh

##################################
# wkdb_create_database.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="mysql database creation"

NUMARG=0
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
	`basename $0` 

EXAMPLE:
	`basename $0`" 
  exit 1
fi  


# DB CREATION, this ask for mysql root password
echo "Please insert your mysql root password below."
mysql --user="root" -p -e \
"create database wiki; \
grant all privileges on wiki.* to mary@localhost identified by \"wiki123\"; \
flush privileges;"

#This is the default mary user password wiki123
echo "wiki database has been created, all privileges are granted to user mary in the localhost and the password wiki123"



