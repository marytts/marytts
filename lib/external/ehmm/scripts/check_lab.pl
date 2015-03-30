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
##          Author :  Kishore Prahallad (skishore@cs.cmu.edu)            ##
##          Date   :  July 2005                                          ##
##                                                                       ##
###########################################################################

$nargs = $#ARGV + 1;
if ($nargs < 2) {
  print "Usage: perl file.pl <labDir> <prompt-file [etc/txt.mod.data]> \n";
  exit;
}

$labD  = $ARGV[0];
$txtF  = $ARGV[1];
@tl = &Get_ProcessedLines($txtF);

@err = ();
$ec = 0;

for (my $i = 0; $i <= $#tl; $i++) {

   my @wd = &Get_Words($tl[$i]);

   my $prm = $wd[0];   #First field is the prompt name....

   my $labF = $labD."/".$prm.".lab";

   my @ll  = &Get_ProcessedLines($labF);

   my $nl  = $#ll;    #The first line is a hash in the lab file....

   print "Processing ... $labF with $nl and $#wd\n";

   if ($nl != $#wd) {  #The first field in the prompt is prompt name...

     $err[$ec] = $prm;
     $ec++;
     next;
   }

   for (my $j = 0; $j < $#wd; $j++) {

     my @fld = &Get_Words($ll[1 + $j]);   #Ignoring first line in lab....

     if ($wd[1 + $j] != $fld[2]) {        #Ignoring first field in @wd
                                          #fld[2] as the lab format is <int int char*>
       print "$labF do not match with the prompt \n";
       print "Aborting.....\n";
       exit(1);
     }
   }
}
print "********** NOTE:************\n";

print "Either delete the following prompts or manually label them..\n";
print "They could not be labelled automatically due to mismatch \n";
print "between models and the recorded speech \n";

print "**********************\n";

for (my $i = 0; $i < $ec; $i++) {
  my $j = $i + 1;
  print "Prompt $j: $err[$i]\n";
}
print "**********************\n";


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

