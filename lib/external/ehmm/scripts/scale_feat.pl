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

$_fDir = "feat/";

$nargs = $#ARGV + 1;

if ($nargs < 5) {
  print "Usage: file.pl <mywavelist> <feat-dir> <mod-dir> <extn. [ft]> <scaleFactor [4/10/20]>\n";
  exit;
}

my $inF = $ARGV[0];
$_fDir = $ARGV[1];
$_fDir = $_fDir."/";

my $modD = $ARGV[2];
my $ext = $ARGV[3];
my $scF = $ARGV[4];

my $mnvrF = $modD."/global_mn_vr.txt";

my @lln = &Get_ProcessedLines($inF);

&VarianceNormalize(\@lln, $scF, $ext,$_fDir, $mnvrF);


sub VarianceNormalize() {

  my $lr = shift(@_);
  my @srcL = @{$lr};

  my $_scF = shift(@_); #This would be the std. deviation.


  my $ext = shift(@_);
  my $fD  = shift(@_);

  my $mnvrF = shift(@_);

  print "Scaling Factor is $_scF $ext $fD\n";

  my $nPT = $#srcL + 1;
  my $tc = 0;

  my @msx;
  my @vsx;
 
  my $dim;

 for (my $pt = 1; $pt < $nPT; $pt++) {  #First line is a header.....

    my $lsrc = $srcL[$pt];
    &Make_SingleSpace(\$lsrc);

    my $file = $fD.$lsrc.".".$ext;
    print "Scale normalizing $file\n";

    my @obs = &Get_ProcessedLines($file);
    my $nt  = $#obs + 1;

    for (my $j = 0; $j < $nt; $j++) {

       my $sob = $obs[$j];
       my @dat = &Get_Words($sob);

       if (($#dat + 1) == 2 && $j == 0) {
         #if the dimension is 2, it is likely as header of r x c
         next;
       }

       for (my $k  = 0; $k <= $#dat; $k++) {
          $msx[$k] += $dat[$k];  
          $vsx[$k] += $dat[$k] * $dat[$k];
       }
       $dim = $#dat + 1;

       $tc++;
    }
 }
 
 my @_mean;
 my @_var;
 open(fp_mnv, ">$mnvrF");
 print fp_mnv "2 $dim\n";
 for (my $k = 0; $k < $dim; $k++) { 
    $_mean[$k] = $msx[$k] / $tc;  
    $_var[$k] = $vsx[$k] / $tc - ($_mean[$k] * $_mean[$k]);
    print "Mean: $k: $_mean[$k]   Var: $k: $_var[$k]\n";
    print fp_mnv "$_mean[$k] ";
 }
 print fp_mnv "\n";
 for (my $k = 0; $k < $dim; $k++) { 
   print fp_mnv "$_var[$k] ";
 }
 print fp_mnv "\n";
 close(fp_mnv);
 

 for (my $pt = 1; $pt < $nPT; $pt++) {  #First line is header....

    my $lsrc = $srcL[$pt];
    &Make_SingleSpace(\$lsrc);
    my $file = $fD.$lsrc.".".$ext;
    
    my $ni = $pt;

    my @obs = &Get_ProcessedLines($file);
    my $nt  = $#obs + 1;

    print "Writing $file $ni / $nPT NOF: $nt\n";

    open(fp1, ">$file");

    for (my $j = 0; $j < $nt; $j++) {
       my $sob = $obs[$j];
       my @dat = &Get_Words($sob);

       if (($#dat + 1) == 2 && $j == 0) {
         #if the dimension is 2, it is likely as header of r x c
	 #print it as it is 
         print fp1 "$sob\n";
         next;
       }
       
       for (my $k  = 0; $k <= $#dat; $k++) {
          ##print "$dat[$k] ... ";
          $dat[$k] = ($dat[$k] - $_mean[$k]) / sqrt($_var[$k]);
	  ##print "$dat[$k]...";
          $dat[$k] = $dat[$k] * $_scF;
	  ##print "$dat[$k]\n";
          printf fp1 "%-12.8f ", $dat[$k];
          ###printf "XX: %-12.8f \n", $dat[$k];
       }
       print fp1 "\n";
    }

    close(fp1);

 }


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


