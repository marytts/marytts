#!/bin/bash

sox_cmd=$1/sox

if [ $# -ne 3 ] ; then
  echo ""
  echo "wav2raw.sh: convert .wav files into .raw files using sox."
  echo "     Usage: $0 sox-path-cmd wav-directory raw-directory"
  echo ""
else
  for file in $2/*.wav; do
    file_name=`basename $file .wav`
    echo $sox_cmd $2/$file_name.wav" --> "$3/$file_name.raw
    $sox_cmd $2/$file_name.wav $3/$file_name.raw
  done
fi