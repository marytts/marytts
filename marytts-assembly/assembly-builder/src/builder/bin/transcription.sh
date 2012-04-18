#!/bin/sh
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
java -showversion -ea $* -cp "$MARY_BASE/lib/*" marytts.tools.transcription.TranscriptionGUI 

