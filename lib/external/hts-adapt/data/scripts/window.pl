#!/usr/bin/perl
# ----------------------------------------------------------------- #
#           The HMM-Based Speech Synthesis System (HTS)             #
#           developed by HTS Working Group                          #
#           http://hts.sp.nitech.ac.jp/                             #
# ----------------------------------------------------------------- #
#                                                                   #
#  Copyright (c) 2001-2011  Nagoya Institute of Technology          #
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

if ( @ARGV < 3 ) {
   print "window.pl dimensionality infile winfile1 winfile2 ... \n";
   exit(0);
}

$ignorevalue = -1.0e+10;

# dimensionality of input vector
$dim = $ARGV[0];

# open infile as a sequence of static coefficients
open( INPUT, "$ARGV[1]" ) || die "cannot open file : $ARGV[1]";
@STAT = stat(INPUT);
read( INPUT, $data, $STAT[7] );
close(INPUT);

$nwin = @ARGV - 2;

$n = $STAT[7] / 4;    # number of data
$T = $n / $dim;       # number of frames of original data

# load original data
@original = unpack( "f$n", $data );    # original data must be stored in float, natural endian

# apply window
for ( $i = 1 ; $i <= $nwin ; $i++ ) {

   # load $i-th window coefficients
   open( INPUT, "$ARGV[$i+1]" ) || die "cannot open file : $ARGV[$i+1]";
   $data = <INPUT>;
   @win  = split( ' ', $data );
   $size = $win[0];                    # size of this window

   if ( $size % 2 != 1 ) {
      die "Size of window must be 2*n + 1 and float";
   }

   $nlr = ( $size - 1 ) / 2;

   # calcurate $i-th coefficients
   for ( $t = 0 ; $t < $T ; $t++ ) {
      for ( $j = 0 ; $j < $dim ; $j++ ) {

         # check space boundary (ex. voiced/unvoiced boundary)
         $boundary = 0;
         for ( $k = -$nlr ; $k <= $nlr ; $k++ ) {
            if ( $win[ $k + $nlr + 1 ] != 0.0 ) {
               if ( $t + $k < 0 ) {
                  $l = 0;
               }
               elsif ( $t + $k >= $T ) {
                  $l = $T - 1;
               }
               else {
                  $l = $t + $k;
               }
               if ( $original[ $l * $dim + $j ] == $ignorevalue ) {
                  $boundary = 1;
               }
            }
         }
         if ( $boundary == 0 ) {
            $transformed[ $t * $nwin * $dim + $dim * ( $i - 1 ) + $j ] = 0.0;
            for ( $k = -$nlr ; $k <= $nlr ; $k++ ) {
               if ( $t + $k < 0 ) {
                  $transformed[ $t * $nwin * $dim + $dim * ( $i - 1 ) + $j ] += $win[ $k + $nlr + 1 ] * $original[$j];
               }
               elsif ( $t + $k >= $T ) {
                  $transformed[ $t * $nwin * $dim + $dim * ( $i - 1 ) + $j ] += $win[ $k + $nlr + 1 ] * $original[ ( $T - 1 ) * $dim + $j ];
               }
               else {
                  $transformed[ $t * $nwin * $dim + $dim * ( $i - 1 ) + $j ] += $win[ $k + $nlr + 1 ] * $original[ ( $t + $k ) * $dim + $j ];
               }
            }
         }
         else {
            $transformed[ $t * $nwin * $dim + $dim * ( $i - 1 ) + $j ] = $ignorevalue;
         }
      }
   }
}

$n = $n * $nwin;

$data = pack( "f$n", @transformed );

print $data;
