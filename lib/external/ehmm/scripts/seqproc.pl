#!/usr/local/bin/perl
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

##NOTE: Pau Model and Short pause model configuration is fixed.

$_pauS = 5;
$_pauC = 3;
$_pauG  = 2;
$_spauS = 3;
$_spauC = 2;
$_spauG = 2;

$nargs = $#ARGV + 1;
if ($nargs < 5) {
  print "Input: <prompt-file> <unit nstates pair-file>\n";
  print "  - first line of prompt file is wave-file-name indicator\n";
  print "Further Input: <no-of-gaussians> <no-of-connections> <feat-dim>\n";
  print "Output: <moified prompt file> <target word list>\n";
  print "Usage: perl file.pl <promp-file> <unit nstates pair-file> \n";
  exit;
}

$prmF  = $ARGV[0];
$stF = $ARGV[1];

$ngau = $ARGV[2];
$noc  = $ARGV[3];
$fdim = $ARGV[4];

%uid = ();
@sta = ();
@stn = ();

#Read unit nstates pair-file 
@stL = &Get_Lines($stF);

my $ou2F = $stF.".int";
my $tF  = "tmp.txt";

$id = 0;
$tst = 0;

open(fp_o1, ">$tF");
for (my $i = 0; $i <= $#stL; $i++) {
   my @wrd = &Get_Words($stL[$i]);
   $uid{$wrd[0]} = $id;
   $sta[$id] = $wrd[1];
   $stn[$id] = $wrd[0];

   ##Make use of silence name and ssil name;;
   my $tstr = $wrd[0];
   if ($tstr eq "SIL" || $tstr eq "pau" || $tstr eq "PAU" || $tstr eq "sil") {
     $wrd[1] = $_pauS; #Fixing the silence states to be three...
   }elsif ($tstr eq "ssil" || $tstr eq "SSIL")  {
     $wrd[1] = $_spauS; #Fixing the short pause model to have only one state.....
   }

   print fp_o1 "$id $wrd[0] $wrd[1]\n";

   $id++;
   $tst += $wrd[1];
}
close(fp_o1);

##Generate .int file...

my @fL = &Get_Lines($tF);
open(fp_o2, ">$ou2F");
$nu = $#fL + 1;
#NOW: 3 TOS: 9 NOG: 2 NOC: 2 DIM: 13 0   3

print fp_o2 "NoWords: $nu\n";
print fp_o2 "TotalSt: $tst\n";
print fp_o2 "FDim: $fdim\n";

##print fp_o2 "No.Gau: $ngau\n";
##print fp_o2 "No.Con: $noc\n";

for (my $i = 0; $i <= $#fL; $i++) {

  my @nw = &Get_Words($fL[$i]);
  my $noc1 = $noc;
  my $ng1  = $ngau;

  ##Make use of silence name and ssil name;;
  if ($nw[1] eq "SIL" || $nw[1] eq "pau" || $nw[1] eq "PAU" || $nw[1] eq "sil") {
     $nw[2] = $_pauS; #Fixing the silence states to be three...
     $noc1  = $_pauC; #Allowing a skip state
     $ng1   = $_pauG;
  }elsif ($nw[1] eq "ssil" || $nw[1] eq "SSIL")  {
     $nw[2] = $_spauS; #Fixing the short pause model to have only one state.....
     $noc1  =  $_spauC;  #A self transition......
     $ng1   =  $_spauG;
     ###print "$noc1 --  $ngl\n";
  }

  for (my $z = 0; $z <= $#nw; $z++) {
    print fp_o2 "$nw[$z] ";
  }
  print fp_o2 "$noc1 $ng1\n";

}
close(fp_o2);

#change the promptfile; 

$cpF = $prmF.".int";
@prL = &Get_Lines($prmF);
open(fp_o3, ">$cpF");
$nnp = $#prL + 1;
print fp_o3 "$nnp\n";

for (my $i = 0; $i <= $#prL; $i++) {
  my @wrd = &Get_Words($prL[$i]);
  my $cn = $#wrd + 1;

  if ($cn == 1) {
    ##May be useful for ergodic models.....
    print fp_o3 "$wrd[0] $nu ";
    for (my $k = 0; $k < $nu; $k++) {
       print fp_o3 "$k ";
    }

  }else {

    print fp_o3 "$wrd[0] $#wrd ";
    for (my $j = 1; $j <= $#wrd; $j++) {
        print fp_o3 "$uid{$wrd[$j]} "; 
    } 
  }  
  print fp_o3 "\n";
}
close(fp_o3);

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

sub Get_Lines() {
  my $inF = shift(@_); 
  &Check_FileExistence($inF);
  open(fp_llr, "<$inF");
  my @dat = <fp_llr>;
  close(fp_llr);
  return @dat;
}

sub Get_Words() {
  my $ln = shift(@_);
  &Make_SingleSpace(\$ln);
  my @wrd = split(/ /, $ln);
  return @wrd;
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
