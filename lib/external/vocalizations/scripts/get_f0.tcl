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
# get_f0.tcl
#
# f0 extraction script using snack 
#
# Created  by Shinji SAKO  Mon Sep  1 18:54:35 JST 2003
# Modified by Heiga ZEN    Fri Nov  3 17:28:33 JST 2006

package require snack

set method ESPS
set maxpitch 400      
set minpitch 60       
set framelength 0.005 
set frameperiod 80    
set samplerate 16000  
set encoding Lin16    
set endian bigEndian 
set outputmode 0     
set targetfile ""
set outputfile ""

set arg_index $argc
set i 0
set j 0

set help [ format "pitch extract tool using snack library (= ESPS get_f0)\nUsage %s \[-H max_f0\] \[-L min_f0\] \[-s frame_length (in second)\] \[-p frame_length (in point)\] \[-r samplerate\] \[-l (little endian)\] \[-b (big endian)\] \[-o output_file\] \[-f0 (output in f0)] \[-lf0 (output in log f0)\] inputfile" $argv0 ]

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
    -f0 {
        set outputmode 1
    }
    -lf0 {
        set outputmode 2
    }
    -h {
        puts stderr $help
        exit 1
    }
    default { set targetfile [ lindex $argv $i ] }
    }
    incr i
}

# framelength
if { $j == 1 } {
   set framelength [expr {double($frameperiod) / $samplerate}]
}

# if input file does not exist, exit program
if { $targetfile == "" } {
    puts stderr $help
    exit 0
}

snack::sound s 

# if input file is WAVE (RIFF) format, read it
if { [file isfile $targetfile ] && "[file extension $targetfile]" == ".wav"} {
    s read $targetfile
} else {
    s read $targetfile -fileformat RAW -rate $samplerate -encoding $encoding -byteorder $endian
}

# if output filename (-o option) is not specified, output result to stdout
set fd stdout

# if output filename is specified, save result to that file
if { $outputfile != "" } then {
    set fd [ open $outputfile w ]
}

# extract f0 and output results
switch $outputmode {
    0 {
        # output in ESPS format
        puts $fd [join [s pitch -method $method -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength] \n]
    }
    1 {
        # output f0
        set tmp [s pitch -method $method -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]
        foreach line $tmp {
            puts [lindex $line 0]
        }
    }
    2 {
        # output log f0
        set tmp [s pitch -method $method -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]
        foreach line $tmp {
            set x [lindex $line 0]
            if { $x == 0 } {
                puts $fd -1.0e+10
            } else {
                puts $fd [expr log($x)]
            }
        }
    }
}

exit
