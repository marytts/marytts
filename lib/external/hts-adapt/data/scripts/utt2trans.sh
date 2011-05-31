#!/bin/bash

if [ $# -ne 2 ] ; then
  echo ""
  echo "utt2trans.sh: extract transcription from festival .utt files, creates .txt files."
  echo "        Usage: $0 festival-utt-directory text-directory"
  echo "      Example: ./utt2trans.sh utts text"
  echo ""
else
  for file in $1/*; do
    file_name=`basename $file .utt`
    echo $1/$file_name.utt" --> "$2/$file_name.txt
    grep Features $file | awk '{split($0,a,"\""); print a[3] }' | awk '{split($0,a,"\\"); print a[1] }'
    grep Features $file | awk '{split($0,a,"\""); print a[3] }' | awk '{split($0,a,"\\"); print a[1] }'> $2/$file_name.txt 
  done
fi