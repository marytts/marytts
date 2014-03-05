#!/usr/local/ActiveTcl/bin/tclsh
#  ---------------------------------------------------------------  #
#           The HMM-Based Speech Synthesis System (HTS)             #
#                       HTS Working Group                           #
#                                                                   #
#                  Department of Computer Science                   #
#                  Nagoya Institute of Technology                   #
#                               and                                 #
#   Interdisciplinary Graduate School of Science and Engineering    #
#                  Tokyo Institute of Technology                    #
#                     Copyright (c) 2001-2007                       #
#                       All Rights Reserved.                        #
#                                                                   #
#  Permission is hereby granted, free of charge, to use and         #
#  distribute this software and its documentation without           #
#  restriction, including without limitation the rights to use,     #
#  copy, modify, merge, publish, distribute, sublicense, and/or     #
#  sell copies of this work, and to permit persons to whom this     #
#  work is furnished to do so, subject to the following conditions: #
#                                                                   #
#    1. The code must retain the above copyright notice, this list  #
#       of conditions and the following disclaimer.                 #
#                                                                   #
#    2. Any modifications must be clearly marked as such.           #
#                                                                   #
#  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSITITUTE OF TECHNOLOGY,  #
#  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    #
#  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       #
#  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   #
#  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSITITUTE OF        #
#  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    #
#  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        #
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  #
#  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   #
#  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          #
#  PERFORMANCE OF THIS SOFTWARE.                                    #
#                                                                   #
#  ---------------------------------------------------------------  #
#
# This program is based on the program getf0.tcl
# Created  by Shinji SAKO   Mon Sep  1 18:54:35 JST 2003
# Modified by Heiga ZEN     Fri Nov  3 17:28:33 JST 2006
#
# get_str.tcl : strengths extraction script using snack.
# Extracting strengths from 5 filtered bands of raw audio
# it requires 5 filter taps in ../data/filters/mix_excitation_filters.txt 
#
# Created  by Marcela Charfuelan (DFKI) Wed Jun 27 17:46:58 CEST 2007
# Modified by Marcela Charfuelan (DFKI) Wed Dec 14 13:58:35 CET  2011
#
# use in dir hts/data/:
#  tclsh8.4 scripts/get_str.tcl -H 280  -L 40 -f filters/mix_excitation_5filters_199taps_48Kz.txt -n 5 -p 80 -o f0_tmp.tmp tmp.wav
#  or to output to stdout
#  tclsh8.4 scripts/get_str.tcl -H 280  -L 40 -f filters/mix_excitation_5filters_199taps_48Kz.txt -n 5 -p 80 tmp.wav 

package require snack

set method ESPS
set maxpitch 400      
set minpitch 60       
set framelength 0.005 
set windowlength 0.025
set frameperiod 80    
set samplerate 16000  
set encoding Lin16    
set endian bigEndian 
set numfilters 5    
set filtersfile "filters/mix_excitation_5filters_199taps_48Kz.txt"
set targetfile ""
set outputfile ""

set arg_index $argc
set i 0
set j 0

set help [ format "\nStrengths extraction using snack library \nUsage %s \[-H max_f0\] \[-L min_f0\] \[-f filters_filename \] \[-n number_of_filters (must be equal to STRORDER)\] \[-s frame_length (in second)\] \[-p frame_length (in point)\] \[-r samplerate\] \[-l (little endian)\] \[-b (big endian)\] \[-o output_file\] inputfile\n" $argv0 ]

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
    -f {
        incr i
        set filtersfile [ lindex $argv $i ]       
    }
    -n {
        incr i
        set numfilters [ lindex $argv $i ]       
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

# The number of filters is specified
snack::sound s 
set j 1
while { $j <= $numfilters } {
  snack::sound s($j)
  incr j
}

# Load filter's coefficients from the file: mix_excitation_filters.txt. This file have a 
# text header of 5 lines:
#   #Created by Octave 2.1.73, Fri Jun 20 14:37:48 2008 CEST <marcela@ideas02>
#   #name: H1
#   #type: matrix
#   #rows: 49
#   #columns: 1
# then the taps are in a single column:
#   tap[1][1] 
#   ... 
#   tap[1][49] 
#   tap[2][1] 
#   ... 
#   tap[2][49] 
#   ... 
#   tap[5][1] 
#   ... 
#   tap[5][49] 
#
# Filter's coefficients must be located in ../data/filters/mix_excitation_filters.txt
set f [open $filtersfile]
set k 0
set Array all_taps
foreach line [split [read $f] \n] {  
    if { [string is double -strict $line] } then {
    set all_taps($k) $line  
    #puts "line($k) = $line" 
    incr k
  }
}
close $f

# numfilter is provided, so the number of taps is divided by that number
set size_all_taps $k
set size_filter [expr ($k/$numfilters)]
#puts "filters_file=$filtersfile  numfilters=$numfilters  size_all_taps=$size_all_taps  size_filter=$size_filter"

# k is total number of filter taps read from file
# j is number of filters
# i is number of taps per filter
set k 0  
set j 1  
while { $j <= $numfilters } {
  set i 0
  set filter_taps($j) {}
  while { $i < $size_filter } {
    lappend filter_taps($j) $all_taps($k)
    #puts "filter_taps($j)($i)=[lindex $filter_taps($j) $i]"
    incr k
    incr i
  }
  incr j
}


# if input file is WAVE (RIFF) format, read it
if { [file isfile $targetfile ] && "[file extension $targetfile]" == ".wav"} {
    s read $targetfile
} else {
    s read $targetfile -fileformat RAW -rate $samplerate -encoding $encoding -byteorder $endian
}


set s_size [ s length ]
set j 1  
while { $j <= $numfilters } {
  # create FIR filter
  set h($j) [snack::filter iir -impulse $filter_taps($j)]

  # copy original sound in s1
  s($j) copy s

  # filter signal
  s($j) filter $h($j)

  # i need to do this because after filtering the 
  # file is padded with zeros
  s($j) crop 0 $s_size

  # get the strengths for each band
  # using method ESPS the result is a matrix of 4 columns
  # col 0: pitch col 1: prob of voicing col 2: local root mean square measurement
  # col 3: peak normalised cross correlation
  # for example col 3 can be read with: [lindex $str1 0 3]
  set str($j) [s($j) pitch -method ESPS -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]

  incr j
}


# if output filename (-o option) is not specified, output result to stdout
set fd stdout

# if output filename is specified, save result to that file
if { $outputfile != "" } then {
    set fd [ open $outputfile w ]
}

# output results
set ind 0
foreach line $str(1) {
  set j 1
  while { $j <= $numfilters } {
    puts [lindex $str($j) $ind 3]
    incr j
  }
  incr ind 
}

exit
