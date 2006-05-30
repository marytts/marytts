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
export ESTDIR=/project/cl/mary/Festival-1.4.3-suse/speech_tools
export FESTIVALDIR=$ESTDIR/../festival
export FESTVOXDIR=$ESTDIR/../festvox

export HELPERDIR=~/mary/openmary/lib/festvox
#export HELPERDIR=~/anna/openmary/lib/festvox


export JAVAHEAP=-Xmx912m




# Compile the helping Java classes
#
(cd $HELPERDIR; mkdir -p classes; cd src; javac -d ../classes *.java)




# Get FV_VOICENAME and FV_FULLVOICENAME
#
. ./etc/voice.defs

echo Importing $FV_VOICENAME


mkdir -p FreeTTS


########################################################################
#                                                                      #
# Create voice-specific files that are part of the big database.       #
# These will ultimately be concatenated together to make the big txt   #
# file for the voice data.                                             #
#                                                                      #
########################################################################

#echo Creating lpc files
#mkdir -p lpc
#bin/make_lpc wav/*.wav

#echo Creating lpc/lpc.params
#for file in lpc/*.lpc; do
#    $ESTDIR/bin/ch_track -otype est_ascii $file
#done | sed '1,/EST_Header_End/d' |
#awk 'BEGIN {min=0; max=0;} {
#         for (i=4; i<=NF; i++) {
#             if ($i < min) min = $i;
#             if ($i > max) max = $i;
#         }
#     } END {
#         printf("LPC_MIN=%f\n",min);
#         printf("LPC_MAX=%f\n",max);
#         printf("LPC_RANGE=%f\n",max-min);
#     }' > lpc/lpc.params




#echo Creating mcep/mcep.params and converting mcep files to text
#for file in mcep/*.mcep; do
#    echo $file MCEP
#    $ESTDIR/bin/ch_track -otype est_ascii $file > $file.txt
#    cat $file.txt
#done | sed '1,/EST_Header_End/d' |
#awk 'BEGIN {min=0; max=0;} {
#         for (i=4; i<=NF; i++) {
#             if ($i < min) min = $i;
#             if ($i > max) max = $i;
#         }
#     } END {
#         printf("MCEP_MIN=%f\n",min);
#         printf("MCEP_MAX=%f\n",max);
#         printf("MCEP_RANGE=%f\n",max-min);
#     }' > mcep/mcep.params


echo "Creating short term signal (STS) files in sts/*.sts"
mkdir -p sts
java -cp $HELPERDIR/classes FindSTS .
    `find wav -type f | cut -f2 -d/ | cut -f1 -d.`


echo Creating FreeTTS/misc.txt
$FESTIVALDIR/bin/festival -b --heap 1500000 \
    festvox/$FV_FULLVOICENAME.scm \
    $HELPERDIR/scheme/dump_misc.scm \
    "(begin (voice_${FV_FULLVOICENAME}) (dump_misc))" > \
    FreeTTS/misc.txt



# UnitDatabase outputs its own info...
 echo "Building Database..."
 find wav -type f | cut -f2 -d/ | cut -f1 -d. > FreeTTS/filenames.txt
 
 java $JAVAHEAP -cp $HELPERDIR/classes UnitDatabase .


echo Creating FreeTTS/trees.txt
$FESTIVALDIR/bin/festival -b --heap 1500000 \
    festvox/$FV_FULLVOICENAME.scm \
    $HELPERDIR/scheme/dump_trees.scm \
    "(begin (voice_${FV_FULLVOICENAME}) (dump_trees))" > \
    FreeTTS/trees.txt


echo Creating FreeTTS/weights.txt
$FESTIVALDIR/bin/festival -b --heap 1500000 \
    festvox/$FV_FULLVOICENAME.scm \
   $HELPERDIR/scheme/dump_join_weights.scm \
    "(begin (voice_${FV_FULLVOICENAME}) (dump_join_weights))" > \
    FreeTTS/weights.txt



########################################################################
#                                                                      #
# Now create the big database file and also set up the *.java files    #
# for this voice.                                                      #
#                                                                      #
########################################################################

echo Creating FreeTTS/$FV_VOICENAME.txt
(cd FreeTTS; cat misc.txt unit_catalog.txt trees.txt unit_index.txt sts.txt mcep.txt weights.txt > $FV_VOICENAME.txt)





echo Done.
