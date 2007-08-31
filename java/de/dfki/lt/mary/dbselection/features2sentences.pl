#!/usr/local/bin/perl

#Takes a list of basenames and return the appropriate sentence names
#should be called from /anna/wikipedia
#expects the sentences in sentences2-12 directories

$infile = $ARGV[0] or die("Program usage: perl -w listSentences.pl infile outfile\n");
$outfile = $ARGV[1] or die("Program usage: perl -w listSentences.pl infile outfile\n");

open(INFILE,"$infile") or die("Could not open infile");;
@featsfiles = <INFILE>;
close(INFILE);

open(OUTFILE,">$outfile") or die("Could not open outfile");
for($i=0;$i<@featsfiles;$i++){
    $textfile = $featsfiles[$i];
    $textfile = "sentences".substr($textfile,10,-6)."txt";

    if ($textfile =~ /(sentences\d*\\dewiki\d*\/\/dewiki\d*_)(\d*)(.txt)/){ 
	$num = $2; 
	$textfile = $1 . $num . $3;
	open(TEXTFILE,"$textfile") or die("Could not open $textfile");
	$text = <TEXTFILE>;
	chomp($text);
	close(TEXTFILE);    
	 print OUTFILE "$textfile $text\n"; 
	
    } else { 
	print "$textfile does not match\n"; 
    } 
}

close(OUTFILE);
