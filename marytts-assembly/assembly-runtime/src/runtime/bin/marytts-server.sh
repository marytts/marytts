#!/bin/bash
##########################################################################
# MARY TTS server
##########################################################################

# Set the Mary base installation directory in an environment variable:
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"

CLASSPATH=$(echo $MARY_BASE/lib/voice*.jar $MARY_BASE/lib/marytts-lang-*.jar $MARY_BASE/lib/marytts-client*.jar $MARY_BASE/lib/marytts-server*.jar | tr ' ' ':')

java -showversion -ea -Xms40m -Xmx1g -cp $CLASSPATH -Dmary.base="$MARY_BASE" $* marytts.server.Mary
