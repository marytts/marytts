#!/bin/sh
# Upgrade existing HMM-based and unit selection voices from MARY TTS 4.x to MARY TTS 5+.
#
# Usage:
# upgrade-marytts4-voices.sh mary-components.xml mary-voice-file-4.3.0.zip [more voice files...]

BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
java -showversion -Xmx1024m -Dmary.base="$MARY_BASE" -cp "$MARY_BASE/lib/marytts-builder-${project.version}-jar-with-dependencies.jar" marytts.tools.upgrade.Mary4To5VoiceConverter $*
