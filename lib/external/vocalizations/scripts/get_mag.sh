#!/bin/bash
#
# example how to run:
#   get_mag.sh arctic_a0100.mag 

# SPTK path:
SPTK='/home/sathish/Applications/SPTK/bin'

# TCL path:
TCL='/usr/bin'

#  fourier magnitude tcl script path
MAG='/home/sathish/Work/phd/voices/delme-f0desc-listener/vocalizations/scripts'

FRAMELEN=400
FRAMESHIFT=80

BC='/usr/bin/bc'
UPPERF0=300
LOWERF0=100
SAMPFREQ=16000
MAGORDER=10

file_name=`basename $1 .raw`


count=`echo "0.005 * 16000 " | /usr/bin/bc -l`
${SPTK}/step -l `printf "%.0f" ${count}` | ${SPTK}/x2x +fs > tmp.head

count=`echo "0.015 * 16000 " | /usr/bin/bc -l`
${SPTK}/step -l `printf "%.0f" ${count}` | ${SPTK}/x2x +fs > tmp.tail

cat tmp.head $1 tmp.tail > tmp.raw

${SPTK}/x2x +sf tmp.raw > tmp

${SPTK}/frame +f -p $FRAMESHIFT tmp | ${SPTK}/window -w 1 -n 1 | ${SPTK}/gcep -c 2 -m 24  -e 0.001 > tmp.gcep

${SPTK}/iglsadf -k -c 2 -m 24 -p $FRAMESHIFT tmp.gcep < tmp > tmp.res

${TCL}/tclsh ${MAG}/get_mag.tcl -l -H $UPPERF0 -L $LOWERF0 -m $MAGORDER -p $FRAMESHIFT -r $SAMPFREQ tmp.raw tmp.res | ${SPTK}/x2x +af > ${file_name}.mag


echo
echo "Created file ${file_name}.str"



