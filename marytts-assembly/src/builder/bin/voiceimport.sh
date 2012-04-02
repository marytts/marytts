#!/bin/sh
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
java -showversion -ea -Xmx1024m -DMARYBASE="$MARY_BASE" $* -jar "$MARY_BASE/lib/marytts-builder-${project.version}-jar-with-dependencies.jar"
