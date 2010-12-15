#!/bin/bash
#

if [ $# -ne 11 ]
then
echo "Error - Problem with command line arguments"
echo "Syntax : $0 <BCCommandPath> <SPTKPATH> <TCLCOMMAND> <TCLScriptsPath> <SAMPFREQ> <FRAMESHIFT> <LOWERF0> <UPPERF0> <STRORDER> <input RAW File> <output MAG File> "
echo "e.g. : $0 /usr/bin/bc /home/username/SPTK/bin /usr/bin/tclsh voicebuildingDir/vocalizations/scripts 16000 80 100 500 5 input.raw output.lf0 "
exit 1
fi

BC=$1
SPTK=$2
TCL=$3
STR=$4
SAMPFREQ=$5
FRAMESHIFT=$6
LOWERF0=$7
UPPERF0=$8
STRORDER=$9
shift
INPUTFILE=$9
shift
OUTPUTFILE=$9
#FRAMELEN=400

count=`echo "0.005 * 16000 " | /usr/bin/bc -l`
${SPTK}/step -l `printf "%.0f" ${count}` | ${SPTK}/x2x +fs > tmp.head
count=`echo "0.025 * 16000 " | /usr/bin/bc -l`
${SPTK}/step -l `printf "%.0f" ${count}` | ${SPTK}/x2x +fs > tmp.tail
cat tmp.head $INPUTFILE tmp.tail > tmp
${TCL} ${STR}/get_str.tcl -l -H $UPPERF0 -L $LOWERF0 -p $FRAMESHIFT -r $SAMPFREQ -f $STRORDER tmp | ${SPTK}/x2x +af > $OUTPUTFILE



