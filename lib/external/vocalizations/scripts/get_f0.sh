#!/bin/bash
#

if [ $# -ne 10 ]
then
echo "Error - Problem with command line arguments"
echo "Syntax : $0 <BCCommandPath> <SPTKPATH> <TCLCOMMAND> <TCLScriptsPath> <input RAW File> <output LF0 File> <SAMPFREQ> <FRAMESHIFT> <LOWERF0> <UPPERF0>"
echo "e.g. : $0 /usr/bin/bc /home/username/SPTK/bin /usr/bin/tclsh voicebuildingDir/vocalizations/scripts input.raw output.lf0 16000 80 100 500"
exit 1
fi

BC=$1
SPTK=$2
TCL=$3
SCRIPTSPATH=$4
INFILE=$5
OUTFILE=$6

SAMPFREQ=$7
FRAMESHIFT=$8
LOWERF0=$9
shift
UPPERF0=$9
#STRORDER=5
#echo
#echo $SAMPFREQ $FRAMESHIFT $LOWERF0 $UPPERF0

count=`echo "0.005 * 16000 " | $BC -l`
${SPTK}/step -l `printf "%.0f" ${count}` -v 0.0 | ${SPTK}/x2x +fs > tmp.head
count=`echo "0.025 * 16000 " | $BC -l`
${SPTK}/step -l `printf "%.0f" ${count}` -v 0.0 | ${SPTK}/x2x +fs > tmp.tail
cat tmp.head ${INFILE} tmp.tail | ${SPTK}/x2x +sf > tmp
leng=`${SPTK}/x2x +fa tmp | /usr/bin/wc -l`
${SPTK}/nrand -l $leng | ${SPTK}/sopr -m 50 | ${SPTK}/vopr -a tmp | ${SPTK}/x2x +fs > tmp.raw
${TCL} ${SCRIPTSPATH}/get_f0.tcl -l -lf0 -H $UPPERF0 -L $LOWERF0 -p $FRAMESHIFT -r $SAMPFREQ tmp.raw | ${SPTK}/x2x +af > ${OUTFILE}


