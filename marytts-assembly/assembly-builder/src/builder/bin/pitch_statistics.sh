#!/bin/sh

##################################
# pitch_statistics.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit
BINDIR="`dirname "$0"`"
DESCRIPTION="Compute some statistics on pitch"

NUMARG=1
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
`basename $0` [PRAATPMDIR] 
	PRAATPMDIR: pmDir

EXAMPLE:
	`basename $0` voice_buiding/pm" 
  exit 1
fi  

PRAATPMDIR=$1

cat $PRAATPMDIR/*.f0 | sort -n | sed -r -n -e '/[^0]/,$p' > $PRAATPMDIR/total.f0

octave $BINDIR/pitch_statistics.m $PRAATPMDIR/total.f0 2>&1 | tee $PRAATPMDIR/total.f0.pitch_statistics.txt  


