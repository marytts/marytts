#!/bin/sh

# Start only the componet $1
# i.e: PraatPitchmarker, 

COMPONENT=$1
echo "Starting voice import component " $COMPONENT


BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"

echo MARY_BASE: $MARY_BASE

#java -showversion -ea -Xmx1024m -DMARYBASE="$MARY_BASE" $* -jar "$MARY_BASE"/java/voiceimport.jar
#java -showversion -ea -Xmx1024m -DMARYBASE="$MARY_BASE" $* -jar "$MARY_BASE"/lib/marytts-builder-5.0-SNAPSHOT.jar

#java -showversion -ea -Xmx1024m  -DMARYBASE="$MARY_BASE" -cp "$MARY_BASE/lib/*" -jar "$MARY_BASE"/lib/marytts-builder-5.0-SNAPSHOT-jar-with-dependencies.jar $COMPONENT

#java -showversion -ea -Xms40m -Xmx1g -cp "$MARY_BASE/lib/*" -DMARYBASE="$MARY_BASE" -Dmary.base="$MARY_BASE" -jar "$MARY_BASE"/lib/marytts-builder-5.0-SNAPSHOT-jar-with-dependencies.jar $COMPONENT

#java -showversion -ea -Xms40m -Xmx1g -cp "$MARY_BASE/lib/*" -DMARYBASE="$MARY_BASE" -jar "$MARY_BASE"/lib/marytts-builder-5.0-SNAPSHOT-jar-with-dependencies.jar


#java -showversion -ea -Xmx1024m -Dfile.encoding=UTF-8 -classpath $CLASSSSSSSSSES -DMARYBASE="$MARY_BASE" -jar "$MARY_BASE"/lib/marytts-builder-5.0-SNAPSHOT-jar-with-dependencies.jar

#marytts.tools.voiceimport.DatabaseImportMain

#-cp "$MARY_BASE/lib/*"
 #-Dmary.base="$MARY_BASE"
#$* marytts.server.Mary



#java -showversion -ea -Xms40m -Xmx1g -cp "$MARY_BASE/lib/*" -Dmary.base="$MARY_BASE"  marytts.tools.voiceimport.DatabaseImportMain

java -showversion -ea -Xms40m -Xmx1g -cp "$MARY_BASE/lib/*" -DMARYBASE="$MARY_BASE"  marytts.tools.voiceimport.DatabaseImportMain $COMPONENT







