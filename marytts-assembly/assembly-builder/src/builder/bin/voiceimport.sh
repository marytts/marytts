#!/bin/sh
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
java -showversion -Xmx1024m -Dmary.base="$MARY_BASE" -cp "$MARY_BASE/lib/*" marytts.tools.voiceimport.DatabaseImportMain $*
