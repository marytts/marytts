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
# Created by Marcela Charfuelan (DFKI) Wed Jun 27 17:46:58 CEST 2007
#
# use:
#  tclsh8.4 get_str.tcl -H 280  -L 40 -f 5 -p 80 -o f0_tmp.tmp tmp.wav
#  or to output to stdout
#  tclsh8.4 get_str.tcl -H 280  -L 40 -f 5 -p 80 tmp.wav 

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
set targetfile ""
set outputfile ""

set arg_index $argc
set i 0
set j 0

set help [ format "\nStrengths extraction using snack library \nUsage %s \[-H max_f0\] \[-L min_f0\] \[-f number_of_filters (must be equal to STRORDER)\] \[-s frame_length (in second)\] \[-p frame_length (in point)\] \[-r samplerate\] \[-l (little endian)\] \[-b (big endian)\] \[-o output_file\] inputfile\n" $argv0 ]

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

#puts stderr "getf0.tcl: this program was modified to output Fs/pitch in samples instead of log pitch in Hz"

# framelength
if { $j == 1 } {
   set framelength [expr {double($frameperiod) / $samplerate}]
}

# if input file does not exist, exit program
if { $targetfile == "" } {
    puts stderr $help
    exit 0
}

# if STRORDER is different from 5, exit program
if { $numfilters != 5 } {
    puts "\nThis implementation supports only 5 filter bands and STRORDER is $numfilters\n";
    exit 0
}

snack::sound s 
snack::sound s1
snack::sound s2
snack::sound s3
snack::sound s4
snack::sound s5

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
set f [open filters/mix_excitation_filters.txt]
set k 0
set Array h
foreach line [split [read $f] \n] {
  if { [string is double $line] } then {
    set h($k) $line   
    incr k
  }
}
close $f

incr k -1
set size_h $k
set size_filter [expr ($k/$numfilters)]

set k 0
set i 0
set h1 {}
while { $i < $size_filter } {
  lappend h1 $h($k)
  #puts "h1($i)=[lindex $h1 $i]"
  incr k
  incr i
}
set i 0
set h2 {}
while { $i < $size_filter } {
  lappend h2 $h($k)
  #puts "h2($i)=[lindex $h2 $i]"
  incr k
  incr i
}
set i 0
set h3 {}
while { $i < $size_filter } {
  lappend h3 $h($k)
  #puts "h3($i)=[lindex $h3 $i]"
  incr k
  incr i
}
set i 0
set h4 {}
while { $i < $size_filter } {
  lappend h4 $h($k)
  #puts "h4($i)=[lindex $h4 $i]"
  incr k
  incr i
}
set i 0
set h5 {}
while { $i < $size_filter } {
  lappend h5 $h($k)
  #puts "h5($i)=[lindex $h5 $i]"
  incr k
  incr i
}




# create FIR filters
set h1 [snack::filter iir -impulse $h1]
set h2 [snack::filter iir -impulse $h2]
set h3 [snack::filter iir -impulse $h3]
set h4 [snack::filter iir -impulse $h4]
set h5 [snack::filter iir -impulse $h5]


# if input file is WAVE (RIFF) format, read it
if { [file isfile $targetfile ] && "[file extension $targetfile]" == ".wav"} {
    s read $targetfile
} else {
    s read $targetfile -fileformat RAW -rate $samplerate -encoding $encoding -byteorder $endian
}

# copy original sound in s1
s1 copy s
s2 copy s
s3 copy s
s4 copy s
s5 copy s

set ini 0
set final 500

#set s_size [lindex [ s info ] 0 ]
set s_size [ s length ]
#puts stderr $s_size

s1 filter $h1
s2 filter $h2
s3 filter $h3
s4 filter $h4
s5 filter $h5

# i need to do this because after filtering the 
# file is padded with zeros
s1 crop 0 $s_size
s2 crop 0 $s_size
s3 crop 0 $s_size
s4 crop 0 $s_size
s5 crop 0 $s_size

#s1 write s1.wav
#s2 write s2.wav
#s3 write s3.wav
#s4 write s4.wav
#s5 write s5.wav

# get the strengths for each band
# using method ESPS the result is a matrix of 4 columns
# col 0: pitch col 1: prob of voicing col 2: local root mean square measurement
# col 3: peak normalised cross correlation
# for example col 3 can be read with: [lindex $str1 0 3]
set str1 [s1 pitch -method ESPS -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]
set str2 [s2 pitch -method ESPS -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]
set str3 [s3 pitch -method ESPS -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]
set str4 [s4 pitch -method ESPS -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]
set str5 [s5 pitch -method ESPS -maxpitch $maxpitch -minpitch $minpitch -framelength $framelength]

#puts stderr [lindex $str1 0 3]
#puts stderr [lindex $str1 1 3]
#puts stderr [lindex $str1 2 3]
#puts stderr $str1


# if output filename (-o option) is not specified, output result to stdout
set fd stdout

# if output filename is specified, save result to that file
if { $outputfile != "" } then {
    set fd [ open $outputfile w ]
}

# output results
set ind 0
foreach line $str1 {
    puts [lindex $str1 $ind 3]
    puts [lindex $str2 $ind 3]
    puts [lindex $str3 $ind 3]
    puts [lindex $str4 $ind 3]
    puts [lindex $str5 $ind 3]
    incr ind
    
}

exit
