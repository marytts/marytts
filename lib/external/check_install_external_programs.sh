# HMMVoice creation for MARY 4.0 beta

# Exit on all errors
set -e
# Trap exit
trap trap_exit_handler EXIT


# Handle exit trap
function trap_exit_handler() {
    # Backup exit status if you're interested...
    local exit_status=$?
    # Change value of $?
    true
    #echo $?
    #echo trap_handler $exit_status
} # trap_exit_handler()


if [ $# -lt 1 ] ; then
  echo ""
  echo "check_programs.sh: check the neccessary programs (and versions) to run the HTS-HTK demo for MARY 4.0 beta"
  echo "Usage: $0 [options:-check|-install][paths (if not in PATH): path-to-awk|path-to-perl|path-to-tclsh|path-to-bc|path-to-sox|path-to-htk|path-to-htsengine|path-to-sptk]"
  echo "     options:"
  echo "       -check  : just check programs in PATH or in the paths provided by the user"
  echo "       -install: check programs in PATH or in the paths provided by the user and try to install the missing ones"
  echo ""
  exit  
fi



# Add the paths provided to the general $PATH
for p in $@ 
do
  if [ $p =  "-check" ] || [ $p =  "-install" ] ; then
    option=$p
    echo "option=$p"
  else 
    PATH=$p:$PATH
  fi
done
echo "PATH=$PATH"
echo "OPTION = $option"


# The following programs will be checked:
# awk, perl,  tclsh and snack, bc, sox, htk, hts_engine, sptk
# If any of these programs exist then the programs terminates suggesting how it can be installed

#################awk
if which awk > /dev/null; then
   awkProg=`which awk`
   awkPath=`dirname $awkProg`
   echo "awk: $awkProg"
   echo "ok"
   echo
else
   echo "awk: does not exist"
   echo "It can be installed with: sudo apt-get install awk"
   echo
   exit
fi

################## perl
if which perl > /dev/null; then
   perlProg=`which perl`
   perlPath=`dirname $perlProg`
   echo "perl: $perlProg"
   echo "ok"
   echo
else
   echo "perl: does not exist"
   echo "It can be installed with: sudo apt-get install perl"
   echo
   exit
fi


################# tclsh and snack
if which tclsh > /dev/null; then
    tclshProg=`which tclsh`    
    echo "tclsh: $tclshProg"
    echo "package require snack" > tmp.tcl
    echo "snack::sound s" >> tmp.tcl
    if `$tclshProg tmp.tcl` > /dev/null; then
       rm tmp.tcl
       tclshPath=`dirname $tclshProg`
       echo "tclsh and snack exist"
       echo "ok"
       echo
    else
      echo "tclsh installed but it does not support snack." 
      echo "snack can be download from: http://www.speech.kth.se/snack/dist/snack2.2.10-linux.tar.gz"
      echo "unpack it and then copy the directory to your ../tclsh/lib directory"
      echo "suggested commands:"
      echo "  wget http://www.speech.kth.se/snack/dist/snack2.2.10-linux.tar.gz"
      echo "  tar -zxvf snack2.2.10-linux.tar.gz"
      dirTcl=`dirname $tclshProg`
      dirTcl=`dirname $dirTcl`
      echo "  cp -r snack2.2.10 $dirTcl"
      echo
      exit
    fi
else
    echo "tclsh does not exist"
    echo "It can be installed with the command:"
    echo "  sudo apt-get install tclsh"
    echo "Alternatively: it can be download from: http://www.activestate.com/Products/ActiveTcl/ "
    echo "and follow the instructions for installation"
    echo
    exit
fi



################## bc
if which bc > /dev/null; then
   bcProg=`which bc`
   echo "bc: $bcProg" 
   # also check the math lib
   count=`echo "0.005 * 1600" | bc -l`
   echo "bc: checking -l (mathlib)"
   if [ $count = 8.000 ]; then 
     bcPath=`dirname $bcProg`    
     echo "ok"
     echo
   else
     echo "Missing mathlib for running bc, install a newer version of bc"
     echo "It can be installed with the command:"
     echo "  sudo apt-get install bc"
     echo
     exit
   fi 
else
   echo "bc: does not exist"
   echo "It can be installed with the command:"
   echo "  sudo apt-get install bc"
   echo
   exit
fi


################ sox
if which sox > /dev/null; then
    soxProg=`which sox`
    # check sox version
    soxVer=`sox --version | awk '{print substr($3,2,2)}'`
    if [ $soxVer -ge 13 ]; then
      soxPath=`dirname $soxProg`
      echo "sox: $soxProg"
      echo "ok"
      echo
    else
      echo "sox: $soxProg"
      echo "sox installed but probably an older version, please install sox >= v13"
      echo "It can be installed with the command:"
      echo "  sudo apt-get install sox"
      echo "Alternatively: it can be download from: http://sox.sourceforge.net/ "
      echo "and follow the instructions for installation"
      echo
      exit
    fi
else
    echo "sox: does not exist"
    echo "It can be installed with the command:"
    echo "  sudo apt-get install sox"
    echo "Alternatively: it can be download from: http://sox.sourceforge.net/ "
    echo "and follow the instructions for installation"
    echo
    exit
fi


################# HTK
if which HHEd > /dev/null; then
    echo "HTK HHEd exists"
    hhedProg=`which HHEd`
    # Check HTK version and if it is patched with HTS
    htkVer=`$hhedProg -V | grep HHEd | awk '{print $2}'`
    echo "HTK version: $htkVer" 
    # Check if htk is patched, if so HHEd has to have a CM command which is:
    # CM directory         - Convert models to pdf for speech synthesizer
    htsCommands=`$hhedProg -Q | grep CM | awk '{print $1}'`
    # here it is used quotes for htsCommand because it can be empty ""
    if [ "$htsCommands" == "CM" ]; then
       htkPath=`dirname $hhedProg`      
       echo "HTK HHED contains HTS commands like CM "
       echo "HTK ok"
       echo
    else
       echo "htk is installed but it seems not patched with HTS, HHEd does not have command CM."
       echo "The HTS patch can be download from: http://hts.sp.nitech.ac.jp/archives/2.1/HTS-2.1_for_HTK-3.4.tar.bz2"
       echo "suggested commands:"
       echo "  mkdir HTS-patch"
       echo "  cd HTS-patch"
       echo "  wget http://hts.sp.nitech.ac.jp/archives/2.1/HTS-2.1_for_HTK-3.4.tar.bz2"
       echo "  tar -jxvf HTS-2.1_for_HTK-3.4.tar.bz2"
       echo "move HTS-2.1_for_HTK-3.4.tar.bz2 to your htk directory and there execute:"
       echo "  patch -p1 -d . < HTS-2.1_for_HTK-3.4.patch"
       echo "then run configure with something like (you might provide a --prefix directory as well):"
       echo "  ./configure MAXSTRLEN=2048"
       echo "and then follow the instructions for compiling and installing."
       echo
       exit
    fi
else
    
    echo "HTK 3.4 does not exist"
    echo "HTK 3.4 can be downloaded from: http://htk.eng.cam.ac.uk/download.shtml "
    echo "once HTK has been downloaded, apply the HTS patch, compile and install:"
    if [ $option = "-install" ] ; then
      echo "suggested commands:"
            root=`pwd`
      echo "  mkdir -p sw"
      echo "  cp HTK-3.4.tar.gz sw"
      echo "  cd sw"
      echo "  mkdir HTS-patch"
      echo "  cd HTS-patch"
      echo "  wget http://hts.sp.nitech.ac.jp/archives/2.1/HTS-2.1_for_HTK-3.4.tar.bz2"
      echo "  tar -jxvf HTS-2.1_for_HTK-3.4.tar.bz2"
      echo "  cd .."
      echo "  tar -zxvf HTK-3.4.tar.gz"
      echo "  cd htk"
      echo "  cp $root/sw/HTS-patch/HTS-2.1_for_HTK-3.4.patch ."
      echo "  patch -p1 -d . < HTS-2.1_for_HTK-3.4.patch"     
      echo "  ./configure --prefix=$root/sw MAXSTRLEN=2048"
      echo "  make"
      echo "  make install"
      echo "The previous commands will be executed, it assumes that the files HTK-3.4.tar.gz"
      echo "and HDecode-3.4.tar.gz are in the current directory. continue (y/n)?"
      read choice
      if [ $choice = "y" ] ; then
        if [ -f HTK-3.4.tar.gz ] && [ -f HDecode-3.4.tar.gz ] ; then 
          echo "Installing HTK..."
          echo "sources will be downloaded and compiled in: $root/sw"
          echo "binaries will be installed in $root/sw/bin"
          echo
          mkdir -p sw
          cp HTK-3.4.tar.gz sw
          cp HDecode-3.4.tar.gz sw
          cd sw
          mkdir -p HTS-patch
          cd HTS-patch
          wget http://hts.sp.nitech.ac.jp/archives/2.1/HTS-2.1_for_HTK-3.4.tar.bz2
          tar -jxvf HTS-2.1_for_HTK-3.4.tar.bz2
          cd ..
          tar -zxf HTK-3.4.tar.gz
          tar -zxf HDecode-3.4.tar.gz
          cd htk
          cp $root/sw/HTS-patch/HTS-2.1_for_HTK-3.4.patch .
          echo "applying HTS patch"
          patch -p1 -d . < HTS-2.1_for_HTK-3.4.patch
          ./configure --prefix=$root/sw MAXSTRLEN=2048
          make
          make install
          # And compile HDecode
          make hdecode
          make install-hdecode
          echo
          echo "HTK (htk and hdecode) installed in: $root/sw/bin"
          echo
       else
          echo "file: HTK-3.4.tar.gz not found"
          echo "download or copy HTK-3.4.tar.gz in the current directory"
          echo "HTK 3.4 can be downloaded from: http://htk.eng.cam.ac.uk/download.shtml "
          echo
          exit 
       fi 
    else
       echo "HTK is not installed"
       echo
       exit
    fi
   else
       echo "HTK is not installed"
       echo
    fi
fi


################# hts_engine
if which hts_engine > /dev/null; then
    echo "hts_engine exists"
    hts_engineProg=`which hts_engine`
    # CHECK: here it is missing to check the version!!!
    hts_enginePath=`dirname $hts_engineProg`
    echo "hts_engine: $hts_engineProg" 
    echo "ok"
    echo
else
    echo "hts_engine does not exist"
    echo "it can be download from: http://downloads.sourceforge.net/hts-engine/hts_engine_API-1.01.tar.gz "
    if [ $option = "-install" ] ; then
      echo "suggested commands:"
      root=`pwd`
      echo "Installing hts_engine..."
      echo "sources will be downloaded and compiled in: $root/sw"
      echo "binaries will be installed in $root/sw/bin"
      echo "The previous commands will be executed. continue (y/n)?"
      read choice
      if [ $choice = "y" ] ; then
        mkdir -p sw
        cd sw 
        wget http://downloads.sourceforge.net/hts-engine/hts_engine_API-1.01.tar.gz"
        tar -zxf hts_engine_API-1.01.tar.gz"
        cd hts_engine_API-1.01
        ./configure --prefix=$root/sw
        make
        make install
        echo "hts_engine installed in: $root/sw/bin"
        echo
      else
        echo "hts_engine not installed"
        echo
        exit
      fi
    else
      echo "hts_engine not installed"
      echo
    fi
fi


################# SPTK
if which mgcep > /dev/null; then
    echo "SPTK mgcep exists"
    mgcepProg=`which mgcep`
    echo "SPTK mgcep: $mgcepProg" 
    #check SPTK version
    # we need SPTK 3.2 which support gmm (check if gmm exist)
    if which gmm > /dev/null; then
      sptkPath=`dirname $mgcepProg`
      echo "SPTK gmm exist, SPTK version >= 3.2" 
      echo "ok"
      echo
    else
      echo "SPTK gmm does not exist, SPTK version < 3.2" 
      echo "SPTK installed but probably an older version, please install SPTK 3.2"
      echo "it can be download from: http://downloads.sourceforge.net/sp-tk/SPTK-3.2.tar.gz "
      if [ $option = "-install" ] ; then
        echo "suggested commands:"
        echo "  wget http://downloads.sourceforge.net/sp-tk/SPTK-3.2.tar.gz"
        echo "  tar -zxvf SPTK-3.2.tar.gz"
        echo "and follow the instructions for compiling and installing."
        echo
      else
       echo "SPTK not installed"      
      fi
    fi
else
    echo "SPTK does not exist" 
    echo "it can be download from: http://downloads.sourceforge.net/sp-tk/SPTK-3.2.tar.gz "
    if [ $option = "-install" ] ; then
      echo "suggested commands:"
      echo "  wget http://downloads.sourceforge.net/sp-tk/SPTK-3.2.tar.gz"
      echo "  tar -zxvf SPTK-3.2.tar.gz"
      echo "and follow the instructions for compiling and installing."
      echo
    else
     echo "SPTK not installed"      
    fi
fi


################# ehmm from Festival
if which ehmm > /dev/null; then
    echo "festvox ehmm exists"
    ehmmProg=`which ehmm`
    ehmmPath=`dirname $ehmmProg`
    echo "festvox ehmm: $ehmmProg" 
    echo "ehmm exist"
    echo "ok"
else
    echo "ehmm not found"
    echo "so probably festvox is not installed or provide a path for that,"
    echo "normally it can be found in your Festival directory:"
    echo "   ../Festival/festvox/src/ehmm/bin "
    echo "it can be download from: http://festvox.org/festvox-2.1/festvox-2.1-release.tar.gz "
    if [ $option = "-install" ] ; then
      echo "suggested commands:"
      echo "  wget http://festvox.org/festvox-2.1/festvox-2.1-release.tar.gz"
      echo "  tar -zxvf festvox-2.1-release.tar.gz"
      echo "and follow the instructions for compiling and installing."
      echo
    else
     echo "ehmm not installed" 
    fi
fi



############ paths
cur_dir=`pwd`
out_file=$cur_dir/mary/externalPaths.txt
echo ""
echo "________________________________________________________________"
echo "Programs status (detailed information above):"
echo "The following paths should be in the PATH variable"
echo "  awk: $awkPath"
echo "  perl: $perlPath"
echo "  bc: $bcPath"
echo
echo "The following paths are used when running HMMVoiceConfigure"
echo "  tclsh: $tclshPath"
echo "  sox: $soxPath"
echo "  htk: $htkPath"
echo "  hts_engine: $hts_enginePath"
echo "  sptk: $sptkPath"
echo
echo "This path is used when running the EHMMlabeler"  
echo "  ehmm: $ehmmPath"
echo


rm -f $out_file

if [ "$awkPath" != "" ] ; then 
  echo "awk $awkPath" >> $out_file
fi
if [ "$perlPath" != "" ] ; then 
  echo "perl $perlPath" >> $out_file
fi
if [ "$bcPath"  != "" ] ; then 
echo "bc $bcPath" >> $out_file
fi
if [ "$tclshPath" != "" ] ; then 
echo "tclsh $tclshPath" >> $out_file
fi
if [ "$soxPath" != "" ] ; then 
echo "sox $soxPath" >> $out_file
fi
if [ "$htkPath" != "" ] ; then 
echo "htk $htkPath" >> $out_file
fi
if [ "$hts_enginePath" != "" ] ; then 
echo "hts_engine $hts_enginePath" >> $out_file
fi
if [ "$sptkPath" != "" ] ; then 
echo "sptk $sptkPath" >> $out_file
fi
if [ "$ehmmPath" != "" ] ; then 
echo "ehmm $ehmmPath" >> $out_file
fi

echo "List of paths in: $out_file"
echo "" 

# Disable exit trap
trap - EXIT
exit 1
