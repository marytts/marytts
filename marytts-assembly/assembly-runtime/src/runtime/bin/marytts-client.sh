#!/bin/bash
##########################################################################
# MARY TTS client
##########################################################################

# Set the Mary base installation directory in an environment variable:
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"

java -showversion -ea -Dserver.host=localhost -Dserver.port=59125 -jar "$MARY_BASE/lib/marytts-client-${project.version}-jar-with-dependencies.jar"
