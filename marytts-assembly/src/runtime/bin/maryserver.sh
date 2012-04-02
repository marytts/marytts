#!/bin/bash
##########################################################################
# MARY TTS server
##########################################################################

# Set the Mary base installation directory in an environment variable:
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"


java -showversion -ea -Xms40m -Xmx1g -cp "$MARY_BASE/lib/*" -Dmary.base="$MARY_BASE" $* marytts.server.Mary
