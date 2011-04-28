#!/bin/bash



all="opennlp jtok freetts"

############## individual installer functions #############


installOpennlp() {
  echo "Installing opennlp from source..."
  cd $MARYBASE/tmp
  svn checkout https://svn.apache.org/repos/asf/incubator/opennlp/tags/opennlp-1.5.1-incubating-rc7 opennlp
  cd opennlp/opennlp
  mvn install -Dmaven.test.skip=true
}











################# main program ###################
MARYBASE="`dirname "$0"`"

if [ $# -gt 0 ] ; then
  deps=$*
else 
  deps=$all
fi

echo "Will try to install the following dependencies: $deps"

for dep in $deps ; do
  case "$dep" in 
    opennlp) installOpennlp ;;
    *) echo "Ignoring unknown module '$dep'" ;;
  esac
done


