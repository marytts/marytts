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

$nargs = $#ARGV + 1;
if ($nargs < 3) {
  print "This program extracts unique units/phones/words from the festival prompt file and prints them to a file... \n";
  print "Expected format: <text-id> unit sequence \n"; 
  print "Usage: perl file.pl <promp-file> <out-file> <no-of-state/unit>\n";
  exit;
}

$inF = $ARGV[0];
$ouF = $ARGV[1];
$ns  = $ARGV[2];

%unit = ();
@inL = &Get_Lines($inF);
for (my $i = 0; $i <= $#inL; $i++) {
  my @wrd = &Get_Words($inL[$i]);
  for (my $j = 1; $j <= $#wrd; $j++) {  
     #0th element is wavefile name...
     $unit{$wrd[$j]}++;
  }
}

open(fp_o, ">$ouF");
foreach my $k (sort {$a cmp $b} keys(%unit)) {
   #print fp_o "$k $unit{$k}\n";
   print fp_o "$k $ns\n";
}
close(fp_o);


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
