#!/usr/bin/perl 
###########################################################################
##                                                                       ##
##                                                                       ##
##              Carnegie Mellon University, Pittsburgh, PA               ##
##                      Copyright (c) 2004-2005                          ##
##                        All Rights Reserved.                           ##
##                                                                       ##
##  Permission is hereby granted, free of charge, to use and distribute  ##
##  this software and its documentation without restriction, including   ##
##  without limitation the rights to use, copy, modify, merge, publish,  ##
##  distribute, sublicense, and/or sell copies of this work, and to      ##
##  permit persons to whom this work is furnished to do so, subject to   ##
##  the following conditions:                                            ##
##   1. The code must retain the above copyright notice, this list of    ##
##      conditions and the following disclaimer.                         ##
##   2. Any modifications must be clearly marked as such.                ##
##   3. Original authors' names are not deleted.                         ##
##   4. The authors' names are not used to endorse or promote products   ##
##      derived from this software without specific prior written        ##
##      permission.                                                      ##
##                                                                       ##
##  CARNEGIE MELLON UNIVERSITY AND THE CONTRIBUTORS TO THIS WORK         ##
##  DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING      ##
##  ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   ##
##  SHALL CARNEGIE MELLON UNIVERSITY NOR THE CONTRIBUTORS BE LIABLE      ##
##  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES    ##
##  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN   ##
##  AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,          ##
##  ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF       ##
##  THIS SOFTWARE.                                                       ##
##                                                                       ##
###########################################################################
##                                                                       ##
##          Author :  S P Kishore (skishore@cs.cmu.edu)                  ##
##          Date   :  June 2005                                          ##
##                                                                       ##
###########################################################################

$_win = 2;

$nargs = $#ARGV + 1;

if ($nargs < 6) {
  print "Usage: file.pl [mywavelist] <feat-dir> <feat-type [lpcc/mfcc]> <mod-ext [ft]> <delta-flag [1/0]> <delta-delta-flag [1/0]>\n";
  exit;
}

my $inF = $ARGV[0];
$_fDir  = $ARGV[1];
$_fDir  = $_fDir."/";
$_ftyp  = $ARGV[2];
$_mext  = $ARGV[3];
$_df    = $ARGV[4];
$_ddf   = $ARGV[5];

my @ln = &Get_ProcessedLines($inF);

my $dim;

my @head = &Get_Words($ln[0]);

print "No of files are: $head[1]\n";

for (my $i = 1; $i <= $#ln; $i++) {   #first line is header

  my @feat = ();
  my @del  = ();
  my @ddel = ();

  print "Processing $ln[$i]\n";


  my $fnm = $_fDir.$ln[$i].".".$_ftyp;
  my $nfn  = $_fDir.$ln[$i].".".$_mext;

  my @ft = &Get_ProcessedLines($fnm);  #first line is feat header;
  for (my $j  = 1; $j <= $#ft; $j++) {
    my @wrd = &Get_Words($ft[$j]);
    for (my $k = 0; $k <= $#wrd; $k++) {
       $feat[$j-1][$k] = $wrd[$k];
    }
  }
  @del = &Get_Deltas(\@feat);
  @ddel = &Get_Deltas(\@del);
  &Store_Feats($nfn, \@feat, \@del, \@ddel, $_df, $_ddf);
}

sub Get_Deltas() {

  my $lr = shift(@_);
  my @ft = @{$lr};
  my @dcep = ();

  my $r = $#ft + 1;
  my $c = $#{$ft[0]} + 1;

  my $den = 0;
  for (my $k = 1; $k <= $_win; $k++) {
     $den += ($k * $k);
  }
  $den = $den * 2;

  for (my $i = $_win; $i < $r - $_win; $i++) {
    for (my $j = 0; $j < $c; $j++) {
      $dcep[$i][$j] = 0;
      for (my $k = 1; $k <= $_win; $k++) {
        $dcep[$i][$j] += $k * ($ft[$i + $k][$j] - $ft[$i - $k][$j]);
      } 	
      $dcep[$i][$j] /= $den;
    }
  }

  ##Fill the window borders....
  for (my $j = 0; $j < $c; $j++) {

    for (my $i = 0; $i < $_win; $i++) {
       $dcep[$i][$j] = $dcep[$_win][$j];
    }

    for (my $k = $r - $_win; $k < $r; $k++) {
       $dcep[$k][$j] = $dcep[$r - $_win - 1][$j];
    }
  }
  return @dcep;
}

sub Store_Feats() {

  my $fnm = shift(@_);

  my $lf = shift(@_);
  my @ft = @{$lf};

  my $ld = shift(@_);
  my @del = @{$ld};

  my $ldd = shift(@_);
  my @ddel = @{$ldd};

  my $df = shift(@_);
  my $ddf = shift(@_);

  my $r1 = $#ft + 1;
  my $r2 = $#del + 1;
  my $r3 = $#ddel + 1;

  my $c = $#{$ft[0]} + 1;

  if ($r1 != $r2 || $r1 != $r3) {
     print "Error in number of rows (Unusal...) \n";
     exit(1);
  }

  my $dim = $c;
  my $r = $r1;

  if ($df == 1) {
     $dim += $c;
  }
  if ($ddf == 1) {
     $dim += $c;
  }

  open(fp_o, ">$fnm");
  print fp_o "$r $dim\n";
  print "$r $dim\n";

  for (my $i = 0; $i < $r; $i++) {
    
    #Store static....
    for (my $j = 0; $j < $c; $j++) {
       printf fp_o "%-12.8f ", $ft[$i][$j];
    }

    #Store deltas....
    if ($df == 1) {
      for (my $j = 0; $j < $c; $j++) {
       printf fp_o "%-12.8f ", $del[$i][$j];
      }
    }

    #Store delta-deltas....
    if ($ddf == 1) {
      for (my $j = 0; $j < $c; $j++) {
       printf fp_o "%-12.8f ", $ddel[$i][$j];
      }
    }
    print fp_o "\n";
  }
  close(fp_o);
}

sub Make_SingleSpace() {
   chomp(${$_[0]});
   ${$_[0]} =~ s/[\s]+$//;
   ${$_[0]} =~ s/^[\s]+//;
   ${$_[0]} =~ s/[\s]+/ /g;
   ${$_[0]} =~ s/[\t]+/ /g;
}

sub Check_FileExistence() {
  my $inF = shift(@_);
  if (!(-e $inF)) {
    print "Cannot open $inF \n";
    exit;
  }
  return 1;
}

sub Get_ProcessedLines() {
  my $inF = shift(@_);
  &Check_FileExistence($inF);
  open(fp_llr, "<$inF");
  my @dat = <fp_llr>;
  close(fp_llr);
 
  my @nd;
  for (my $i = 0; $i <= $#dat; $i++) {
     my $tl = $dat[$i];
     &Make_SingleSpace(\$tl);
     $nd[$i]  = $tl; 
  }
  return @nd;
}

sub Get_Words() {
  my $ln = shift(@_);
  &Make_SingleSpace(\$ln);
  my @wrd = split(/ /, $ln);
  return @wrd;
}


