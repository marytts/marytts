#!/bin/bash



if [ $# -ne 4 ] ; then
  echo ""
  echo "raw2wav.sh: convert .raw files into .wav files using sox."
  echo "     Usage: $0 sox-path-cmd raw-directory wav-directory sampling-freq"
  echo ""
else
  sox_cmd=$1/sox
  Fs=$4
  soxVer=`$sox_cmd --version | awk '{print substr($3,2,2)}'`

  options="-c 1 -s -w -r $Fs"  
  if [ $soxVer -ge 13 ]; then
    options="-c 1 -s -2 -r $Fs"  
  fi
  

  for file in $2/*.raw; do
    file_name=`basename $file .raw`
    echo $sox_cmd  $options $2/$file_name.raw "-->"  $options $3/$file_name.wav
    $sox_cmd $options $2/$file_name.raw $options $3/$file_name.wav
  done
fi