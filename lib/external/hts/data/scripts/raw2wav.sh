#!/bin/bash

sox_cmd=$1/sox

soxVer=`$sox_cmd --version | awk '{print substr($3,2,2)}'`

options="-c 1 -s -w -r 16000 "  
if [ $soxVer -ge 13 ]; then
  options="-c 1 -s -2 -r 16000 "  
fi

if [ $# -ne 3 ] ; then
  echo ""
  echo "raw2wav.sh: convert .raw files into .wav files using sox."
  echo "     Usage: $0 sox-path-cmd raw-directory wav-directory"
  echo ""
else
  for file in $2/*.raw; do
    file_name=`basename $file .raw`
    echo $sox_cmd  $options $2/$file_name.raw "-->"  $options $3/$file_name.wav
    $sox_cmd $options $2/$file_name.raw $options $3/$file_name.wav
  done
fi