#!/usr/bin/perl
# ----------------------------------------------------------------- #
#           The HMM-Based Speech Synthesis System (HTS)             #
#           developed by HTS Working Group                          #
#           http://hts.sp.nitech.ac.jp/                             #
# ----------------------------------------------------------------- #
#                                                                   #
#  Copyright (c) 2008-2011  Nagoya Institute of Technology          #
#                           Department of Computer Science          #
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

#return filter coefficient by given sampling rate

$target = 6000;    # 0000Hz-6000Hz

@coefficient = (

   # 0000Hz-0500Hz
   [ -0.00302890, -0.00701117, -0.01130619, -0.01494082, -0.01672586, -0.01544189, -0.01006619, 0.00000000, 0.01474923, 0.03347158, 0.05477206, 0.07670890, 0.09703726, 0.11352143, 0.12426379, 0.12799355, 0.12426379, 0.11352143, 0.09703726, 0.07670890, 0.05477206, 0.03347158, 0.01474923, 0.00000000, -0.01006619, -0.01544189, -0.01672586, -0.01494082, -0.01130619, -0.00701117, -0.00302890 ],

   # 0500Hz-1000Hz
   [ -0.00249420, -0.00282091, 0.00257679, 0.01451271, 0.02868120, 0.03621179, 0.02784469, -0.00000000, -0.04079870, -0.07849207, -0.09392213, -0.07451087, -0.02211575, 0.04567473, 0.10232715, 0.12432599, 0.10232715, 0.04567473, -0.02211575, -0.07451087, -0.09392213, -0.07849207, -0.04079870, -0.00000000, 0.02784469, 0.03621179, 0.02868120, 0.01451271, 0.00257679, -0.00282091, -0.00249420 ],

   # 1000Hz-2000Hz
   [ -0.00231491, 0.00990113, 0.02086129, -0.00000000, -0.03086123, -0.02180695, 0.00769333, -0.00000000, -0.01127245, 0.04726837, 0.10106105, -0.00000000, -0.17904543, -0.16031428, 0.09497157, 0.25562154, 0.09497157, -0.16031428, -0.17904543, -0.00000000, 0.10106105, 0.04726837, -0.01127245, -0.00000000, 0.00769333, -0.02180695, -0.03086123, -0.00000000, 0.02086129, 0.00990113, -0.00231491 ],

   # 2000Hz-3000Hz
   [ 0.00231491, 0.00990113, -0.02086129, 0.00000000, 0.03086123, -0.02180695, -0.00769333, -0.00000000, 0.01127245, 0.04726837, -0.10106105, 0.00000000, 0.17904543, -0.16031428, -0.09497157, 0.25562154, -0.09497157, -0.16031428, 0.17904543, 0.00000000, -0.10106105, 0.04726837, 0.01127245, -0.00000000, -0.00769333, -0.02180695, 0.03086123, 0.00000000, -0.02086129, 0.00990113, 0.00231491 ],

   # 3000Hz-4000Hz
   [ 0.00554149, -0.00981750, 0.00856805, -0.00000000, -0.01267517, 0.02162277, -0.01841647, 0.00000000, 0.02698425, -0.04686914, 0.04150730, -0.00000000, -0.07353666, 0.15896026, -0.22734513, 0.25346255, -0.22734513, 0.15896026, -0.07353666, -0.00000000, 0.04150730, -0.04686914, 0.02698425, 0.00000000, -0.01841647, 0.02162277, -0.01267517, -0.00000000, 0.00856805, -0.00981750, 0.00554149 ]
);

if ( @ARGV < 2 ) {
   print "makefilter.pl sampling_rate flag\n";
   exit(0);
}

$samprate = $ARGV[0];
$flag     = $ARGV[1];    # 0: low-pass 1: high-pass

$rate = $target / $samprate;

if ( $rate <= 3 / 48 ) {
   $low_e = 1;
}
elsif ( $rate <= 6 / 48 ) {
   $low_e = 2;
}
elsif ( $rate <= 12 / 48 ) {
   $low_e = 3;
}
elsif ( $rate <= 18 / 48 ) {
   $low_e = 4;
}
else {
   $low_e = 5;
}

@low  = ();
@high = ();
for ( $i = 0 ; $i < 31 ; $i++ ) {
   push( @low,  0.0 );
   push( @high, 0.0 );
}

for ( $i = 0 ; $i < 5 ; $i++ ) {
   if ( $i < $low_e ) {
      for ( $j = 0 ; $j < 31 ; $j++ ) {
         $low[$j] += $coefficient[$i][$j];
      }
   }
   else {
      for ( $j = 0 ; $j < 31 ; $j++ ) {
         $high[$j] += $coefficient[$i][$j];
      }
   }
}

for ( $i = 0 ; $i < 31 ; $i++ ) {
   if ( $i > 0 ) {
      print " ";
   }
   if ( $flag == 0 ) {
      print "$low[$i]";
   }
   else {
      print "$high[$i]";
   }
}
