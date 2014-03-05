#!/bin/bash



if [ $# -ne 4 ] ; then
  echo ""
  echo "raw2wav.sh: convert .raw files into .wav files using sox."
  echo "     Usage: $0 sox-path-cmd raw-directory wav-directory sampling-freq"
  echo ""
else
  sox_cmd=$1/sox
  Fs=$4

  options="-c 1 -s -2 -r $Fs"

  for file in $2/*.raw; do
    file_name=`basename $file .raw`
    echo $sox_cmd  $options $2/$file_name.raw "-->"  $options $3/$file_name.wav
    $sox_cmd $options $2/$file_name.raw $options $3/$file_name.wav
  done
fi
