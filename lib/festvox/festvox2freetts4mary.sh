#!/bin/bash
# Portions Copyright 2004 Sun Microsystems, Inc.
# Portions Copyright 1999-2003 Language Technologies Institute,
# Carnegie Mellon University.
# All Rights Reserved.  Use is subject to license terms.
#
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL
# WARRANTIES.

# Shell script to do a lot of the work to import a cluster
# unit selection voice into FreeTTS.  This is still a work
# in progress.

# You should override these environment variables to match
# your environment.
#
export JAVA_HOME=/lt
export ESTDIR=~/mary/Festival/speech_tools
export FESTIVALDIR=$ESTDIR/../festival
export FESTVOXDIR=$ESTDIR/../festvox

#export HELPERDIR=/d/mary/trunk/openmary/lib/festvox
export HELPERDIR=~/anna/openmary/lib/festvox
export HELPERCLASSES=$HELPERDIR/classes

# CYGWIN support:
cygwin=0;
case "`uname`" in
CYGWIN*) cygwin=1;;
esac

if [ $cygwin = 1 ] ; then
  export HELPERCLASSES=`cygpath --windows "$HELPERCLASSES"`
fi


export JAVAHEAP=-Xmx128m




# Compile the helping Java classes
#
(cd $HELPERDIR; mkdir -p classes; cd src; javac -d ../classes *.java)




# Get FV_VOICENAME and FV_FULLVOICENAME
#
. ./etc/voice.defs

echo Importing $FV_VOICENAME


mkdir -p FreeTTS

# Create LPC files

echo Creating lpc files
mkdir -p lpc
bin/make_lpc wav/*.wav

echo Creating lpc/lpc.params
for file in lpc/*.lpc; do
    $ESTDIR/bin/ch_track -otype est_ascii $file
done | sed '1,/EST_Header_End/d' |
awk 'BEGIN {min=0; max=0;} {
         for (i=4; i<=NF; i++) {
             if ($i < min) min = $i;
             if ($i > max) max = $i;
         }
     } END {
         printf("LPC_MIN=%f\n",min);
         printf("LPC_MAX=%f\n",max);
         printf("LPC_RANGE=%f\n",max-min);
     }' > lpc/lpc.params


# Create MCEP files

echo Creating mcep/mcep.params and converting mcep files to text
for file in mcep/*.mcep; do
    echo $file MCEP
    $ESTDIR/bin/ch_track -otype est_ascii $file > $file.txt
    cat $file.txt
done | sed '1,/EST_Header_End/d' |
awk 'BEGIN {min=0; max=0;} {
         for (i=4; i<=NF; i++) {
             if ($i < min) min = $i;
             if ($i > max) max = $i;
         }
     } END {
         printf("MCEP_MIN=%f\n",min);
         printf("MCEP_MAX=%f\n",max);
         printf("MCEP_RANGE=%f\n",max-min);
     }' > mcep/mcep.params

#Convert CARTS

echo Creating FreeTTS/trees.txt
$FESTIVALDIR/bin/festival -b --heap 1500000 \
    festvox/$FV_FULLVOICENAME.scm \
    $HELPERDIR/scheme/dump_trees.scm \
    "(begin (voice_${FV_FULLVOICENAME}) (dump_trees))" > \
    FreeTTS/trees.txt

echo Done.
