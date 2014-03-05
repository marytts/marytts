#!/usr/local/ActiveTcl/bin/tclsh
# ----------------------------------------------------------------- #
#           The HMM-Based Speech Synthesis System (HTS)             #
#           developed by HTS Working Group                          #
#           http://hts.sp.nitech.ac.jp/                             #
# ----------------------------------------------------------------- #
#                                                                   #
#  Copyright (c) 2001-2008  Nagoya Institute of Technology          #
#                           Department of Computer Science          #
#                                                                   #
#                2001-2008  Tokyo Institute of Technology           #
#                           Interdisciplinary Graduate School of    #
#                           Science and Engineering                 #
#                                                                   #
# All rights reserved.                                              #
#                                                                   #
# Redistribution and use in source and binary forms, with or        #
# without modification, are permitted provided that the following   #
# conditions are met:                                               #
#                                                                   #
# - Redistributions of source code must retain the above copyright  #
#   notice, this list of conditions and the following disclaimer.   #
# - Redistributions in binary form must reproduce the above         #
#   copyright notice, this list of conditions and the following     #
#   disclaimer in the documentation and/or other materials provided #
#   with the distribution.                                          #
# - Neither the name of the HTS working group nor the names of its  #
#   contributors may be used to endorse or promote products derived #
#   from this software without specific prior written permission.   #
#                                                                   #
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            #
# CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       #
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          #
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          #
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS #
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          #
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   #
# TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     #
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON #
# ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   #
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    #
# OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           #
# POSSIBILITY OF SUCH DAMAGE.                                       #
# ----------------------------------------------------------------- #
#
# This program is based on the program getf0.tcl
# Created  by Shinji SAKO  Mon Sep  1 18:54:35 JST 2003
# Modified by Heiga ZEN    Fri Nov  3 17:28:33 JST 2006
#
# get_mag.tcl : Fourier magnitudes extraction script using snack.
# It has as input the raw signal and its residual signal obtained by 
# inverse filtering. 
# The residual signal can be generated with SPTK, using inverse filtering, ex:
#   x2x +sf tmp.raw | frame +f -p 80 | window -w 1 -n 1 | gcep -g 2 -m 24 > tmp.gcep
#   x2x +sf tmp.raw | iglsadf -k -g 2 -m 24 -p 80 tmp.gcep  > tmp.residual
#
# Created by Marcela Charfuelan (DFKI) Wed Jun 25 11:00:22 CEST 2008
#
# use:
#  tclsh get_mag.tcl -l -H 280 -L 40 -p 80 -r 16000 tmp.raw tmp.residual


package require snack

set method ESPS
set maxpitch 400      
set minpitch 60       
set numharmonics 10
set framelength 0.005 
set frameperiod 80    
set samplerate 16000  
set encoding Lin16    
set endian bigEndian 
set outputmode 0     
set targetfile ""
set residualfile ""
set outputfile ""

set arg_index $argc
set i 0
set j 0

set help [ format "\nFourier magnitudes extraction script using snack library \nUsage %s \[-H max_f0\] \[-L min_f0\] \[-m number_of_harmonics (default 10=MAGORDER)\] \[-s frame_length (in second)\] \[-p frame_length (in point)\] \[-r samplerate\] \[-l (little endian)\] \[-b (big endian)\] \[-o output_file\] input_speechfile(raw/wav) input_residualfile\n" $argv0 ]

while { $i < $arg_index } {
    switch -exact -- [ lindex $argv $i ] {
    -H {
        incr i
        set maxpitch [ lindex $argv $i ]
    }
    -L {
        incr i
        set minpitch [ lindex $argv $i ]
    }
    -m {
        incr i
        set numharmonics [ lindex $argv $i ]       
    }
    -s {
        incr i
        set framelength [ lindex $argv $i ]       
    }
    -p {
        incr i
        set frameperiod [ lindex $argv $i ]
        set j 1
    }
    -o {
        incr i
        set outputfile [ lindex $argv $i ]       
    }
    -r {
        incr i
        set samplerate [ lindex $argv $i ]       
    }
    -l {
        set endian littleEndian
    }
    -b {
        set endian bigEndian
    }
    -h {
        puts stderr $help
        exit 1
    }
    default { 
        set targetfile [ lindex $argv $i ] 
        incr i
        set residualfile [ lindex $argv $i ]  
    }
    }
    incr i
}

# framelength
if { $j == 1 } {
   set framelength [expr {double($frameperiod) / $samplerate}]
}

# if input file does not exist, exit program
if { $targetfile == ""  || $residualfile == ""} {
    puts stderr $help
    exit 0
}

# the original speech signal
snack::sound s  
# the residual signal
snack::sound e   


# if input file is WAVE (RIFF) format, read it
if { [file isfile $targetfile ] && "[file extension $targetfile]" == ".wav"} {
    s read $targetfile
} else {
    s read $targetfile -fileformat RAW -rate $samplerate -encoding $encoding -byteorder $endian
}

# read residual, in HTS scripts this is created by SPTK so it contains float values
e read $residualfile -fileformat RAW -rate $samplerate -encoding "float" -byteorder $endian

# if output filename (-o option) is not specified, output result to stdout
set fd stdout

# if output filename is specified, save result to that file
if { $outputfile != "" } then {
    set fd [ open $outputfile w ]
}

# extract f0
set tmp [s pitch -method $method -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]

# n is the index number for a sample
set e_length [e length]
set fftlength 512 
set windowlength 400
set sample_start 0
set sample_end 0
set n 0;

# for each frame in f0
foreach line $tmp {
  set pitch [lindex $line 0]

  # the pitch value returned by snack is in Hz, so here is put in samples
  if { $pitch > 0.0 } then {
    set f0 [expr $samplerate/$pitch]
    set bin_width [expr $fftlength/$f0]
  } else {
    set f0 0
    set bin_width 0
  }    
  
  # take a window of the residual signal and get its FFT or power spectrum
  set sample_start [expr $n *$frameperiod]
  set sample_end [expr ($sample_start + $windowlength)-1]
 
  if { $bin_width > 0.0 && $sample_end <= $e_length } then {

    # this function computes the FFT power spectrum and returns a list of magnitude values (sqrt(r^2+i^2))
    set f [e powerSpectrum -start $sample_start -end $sample_end -windowlength $windowlength -fftlength $fftlength] 

    set bin [expr round($bin_width)]
    set half_bin [expr round($bin_width/2)]
    
    set sumavg 0
    set k 1
    while { $k <= $numharmonics } {
      set ival [expr ($k * $bin_width) - ($bin_width/2) + 1 ]
      # -1 because lindex starts in zero
      set i [expr round($ival) - 1]
      set j [expr $i + $bin ]

      # search for the max val in the interval e[i:j] and keep the index
      set max_harm [lindex $f $i]
      set imax_harm $i
      incr i
      while { $i <= $j } {
        if { [lindex $f $i] > $max_harm } then {
          set max_harm [lindex $f $i]	
	  set imax_harm $i
	}	
        incr i
      }
      
      set harm($k) $max_harm
      # keep a sum for averaging and normalisation
      set sumavg [expr $sumavg + ($max_harm * $max_harm)]
      incr k
    }
 
    # normalise the Fourier magnitudes by the average
    set alpha [expr sqrt($sumavg/$numharmonics)]
    set k 1
    while { $k <= $numharmonics } {
     set harm($k) [expr ($harm($k) / $alpha)]
     puts "$harm($k)"
     incr k
    }

  } else {
    # so it is a non-voiced frame, so the Fourier magnitudes here are set to 1???
    # or maybe I should take the first 10 Fourier magnitudes of the window frame ???
    set k 1
    while { $k <= $numharmonics } {
     puts 1
     incr k
    }   

  }
  incr n

}

exit
