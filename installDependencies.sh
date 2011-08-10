#!/bin/bash



all="jtok mwdumper"

############## individual installer functions #############
function announceAndHandleError {
  module=${1:-unknown}
  echo "Installing $module files..."
  trap 'echo "installation of $module failed" 1>&2 ; exit 1' ERR
}


function installJtok {
  announceAndHandleError "jtok"
  cd "$MARYBASE/tmp"
  svn checkout https://heartofgold.opendfki.de/repos/tags/jtok/release_1.9.1 jtok
  cd jtok
  mvn source:jar javadoc:jar install
}



function installMwdumper {
  announceAndHandleError "mwdumper"
  cd "$MARYBASE/tmp"
  svn checkout -r 87118 http://svn.wikimedia.org/svnroot/mediawiki/trunk/mwdumper
  cd mwdumper
  mvn source:jar javadoc:jar install
}




################# main program ###################
PROGNAME=$(basename "$0")
MARYBASE=$( cd $( dirname "$0" ) ; pwd )
trap 'echo "installation failed" 1>&2 ; exit 1' ERR


if [ $# -gt 0 ] ; then
  deps=$*
else 
  deps=$all
fi

echo "Will try to install the following dependencies: $deps"

for dep in $deps ; do
  case "$dep" in 
    jtok) installJtok ;;
    mwdumper) installMwdumper ;;
    *) echo "Ignoring unknown module '$dep'" ;;
  esac 
done


