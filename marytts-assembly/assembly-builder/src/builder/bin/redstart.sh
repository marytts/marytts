#!/bin/sh
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
java -showversion -ea $* -jar "$MARY_BASE/lib/marytts-redstart-${project.version}.jar"

