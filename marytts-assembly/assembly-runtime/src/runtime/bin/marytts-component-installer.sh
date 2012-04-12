#!/bin/sh
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
java -showversion -ea -Dmary.base="$MARY_BASE" $* -cp "$MARY_BASE/lib/*" marytts.tools.install.InstallerGUI

