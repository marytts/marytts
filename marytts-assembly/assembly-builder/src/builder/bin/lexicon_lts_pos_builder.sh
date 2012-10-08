#!/bin/sh
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
#java -showversion -ea $* -cp "$MARY_BASE/lib/*" marytts.tools.transcription.LTSLexiconPOSBuilder 
java -showversion -ea -Xmx4096m  -cp "$MARY_BASE/lib/*" marytts.tools.transcription.LTSLexiconPOSBuilder $*

#lexicon_lts_pos_builder.sh ./marytts-lang-it/src/main/resources/marytts/language/it/lexicon/allophones.it.xml ~/tests/marytts5.0-new-lexicon/standard.lexicon