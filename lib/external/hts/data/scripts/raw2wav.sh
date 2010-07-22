#!/bin/bash

soxVer=`sox --version | awk '{print substr($3,2,2)}'`

options="-c 1 -s -w -r 16000 "  
if [ $soxVer -ge 13 ]; then
  options="-c 1 -s -2 -r 16000 "  
fi

if [ $# -ne 2 ] ; then
  echo ""
  echo "raw2wav.sh: convert .raw files into .wav files using sox."
  echo "     Usage: $0 raw-directory wav-directory"
  echo ""
else
  for file in $1/*.raw; do
    file_name=`basename $file .raw`
    echo $1/$file_name.raw" --> "$2/$file_name.wav
    sox $options $1/$file_name.raw $options $2/$file_name.wav
  done
fi