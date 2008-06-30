#!/usr/local/bin/perl

#Takes a list of sentence names and return the appropriate basenames
#should be called from /anna/wikipedia
#expects the sentences in sentences2-12 directories
$usage = "Program usage: perl -w sentences2features.pl sentencefile goodsentsfile badsentsfile\n";

$infile = $ARGV[0] 
    or die("$usage");
$goodoutfile = $ARGV[1] 
    or die("$usage");
$badoutfile = $ARGV[2] 
    or die("$usage");

open(INFILE,"$infile") or die("Could not open infile");;
@sentsfiles = <INFILE>;
close(INFILE);

open(GOODOUTFILE,">$goodoutfile") or die("Could not open $goodoutfile");
open(BADOUTFILE,">$badoutfile") or die("Could not open $badoutfile");
for($i=0;$i<@sentsfiles;$i++){
    $textfile = $sentsfiles[$i];
   
    if ($textfile 
	=~ /(\*\s*)(sentences)(\d*\\dewiki\d*\/\/dewiki\d*_)(\d*)(.txt) (.*)/){ 
	$num = $4;
	$textfile = "features" . $3 . $num . ".feats"; 
	print BADOUTFILE "$textfile\n"; 
    } else { 
	if ($textfile 
	    =~ /(sentences)(\d*\\dewiki\d*\/\/dewiki\d*_)(\d*)(.txt) (.*)/){ 
	    $num = $3;
	    $textfile = "features" . $2 . $num . ".feats"; 
	    print GOODOUTFILE "$textfile\n"; 
	} else { 
	    print "$textfile does not match\n"; 
	} 
    }
}

close(GOODOUTFILE);
close(BADOUTFILE);
