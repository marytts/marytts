#!/bin/bash
# 

if [ $# -ne 8 ]
then
echo "Error - Problem with command line arguments"
echo "Syntax : $0 <SPTKPATH> <FRAMELEN> <FRAMESHIFT> <FFTLEN> <FREQWARP> <MGCORDER> <input RAW File> <output LF0 File>"
echo "e.g. : $0 /home/username/SPTK/bin 400 80 512 0.42 24 input.raw output.lf0"
exit 1
fi

SPTK=$1
FRAMELEN=$2
FRAMESHIFT=$3
FFTLEN=$4
FREQWARP=$5
MGCORDER=$6 # MGCORDER is actually 24 plus 1 include MGC[0]
INPUTFILE=$7
OUTPUTFILE=$8

echo "${SPTK}/x2x +sf $INPUTFILE | ${SPTK}/frame +f -l $FRAMELEN -p $FRAMESHIFT | ${SPTK}/window -l $FRAMELEN -L $FFTLEN  -w 1  -n 1 | ${SPTK}/mcep -a $FREQWARP -m $MGCORDER -l $FFTLEN  -e 0.001 > $OUTPUTFILE"
${SPTK}/x2x +sf $INPUTFILE | ${SPTK}/frame +f -l $FRAMELEN -p $FRAMESHIFT | ${SPTK}/window -l $FRAMELEN -L $FFTLEN  -w 1  -n 1 | ${SPTK}/mcep -a $FREQWARP -m $MGCORDER -l $FFTLEN  -e 0.001 > $OUTPUTFILE


