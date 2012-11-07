#!/bin/sh

##################################
# lexicon_lts_pos_builder.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################


# EXIT ERROR settings 
set -o errexit


DESCRIPTION="Train Letter-to-sound(LTS) rules, create FST dictionary and POS tagger with command line using LTSLexiconPOSBuilder.\nLTSLexiconPOSBuilder has the same functionalities of TRANSCRIPTION TOOL but without GUI (better for a remote use for large lexicon and for scripting). This class is a light version of TranscriptionTable"

NUMARG=3
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
`basename $0` [allophones] [lexicon] [removeTrailingOneFromPhones] 
	config_file: wkdb config file  

EXAMPLES:
	`basename $0` ./marytts-lang-en/src/main/resources/marytts/language/en_GB/lexicon/allophones.en_GB.xml lexicon-en_GB.txt true
	`basename $0` ./marytts-lang-it/src/main/resources/marytts/language/it/lexicon/allophones.it.xml ./marytts-lang-it/lib/modules/it/lexicon/it.txt false
	" 
  exit 1
fi  



BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
java -showversion -ea -Xmx4096m  -cp "$MARY_BASE/lib/*" marytts.tools.transcription.LTSLexiconPOSBuilder $*