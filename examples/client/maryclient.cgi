#!/usr/bin/perl -T
# -*- Mode: Perl -*-

use strict;
use IO::Socket;
use CGI;

# variables getting their values from form:
my ($inputtext, $in, $out, $audiotype, $voice);

# little helpers:
my ($var, $tmp);

# contacting the mary server:
my ($host, $port, $maryInfoSocket, $maryDataSocket, $id);

# helping with audio output:
my ($save_to_disk, $audiosubtype, $filename);


my $cgi = new CGI;
my @param = $cgi->param();
$inputtext = $cgi->param('inputtext');
$in = $cgi->param('in');
$out = $cgi->param('out');
$audiotype = $cgi->param('audiotype');
$save_to_disk = $cgi->param('save_to_disk');
$voice = $cgi->param('voice');

my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)=localtime(time);
$year += 1900;
printf STDERR "[%04i-%02i-%02i %02i:%02i:%02i] ", $year, $mon, $mday, $hour, $min, $sec;
print STDERR "Request from ",$cgi->remote_user(),"@",$cgi->remote_host(),": \n";
print STDERR "  in=",$in;
print STDERR "  out=",$out;
print STDERR "  audiotype=",$audiotype;
print STDERR "  voice=",$voice;
print STDERR "  save_to_disk=",$save_to_disk,"\n";
print STDERR "  inputtext: ";
print STDERR $inputtext,"\n";


# Limit inputtext length to 5000 bytes:
if (length $inputtext > 5000) {
    $inputtext = substr $inputtext, 0, 5000;
}


# set audio subtype
if ($out eq "AUDIO") {
    if ($audiotype eq "AU") {
        $audiosubtype = "basic";
        $filename = "mary.au";
    } elsif ($audiotype eq "AIFF") {
        $audiosubtype = "x-aiff";
        $filename = "mary.aiff";
    } elsif ($audiotype eq "WAVE") {
        $audiosubtype = "x-wav";
        $filename = "mary.wav";
    } elsif ($audiotype eq "MP3") {
        $audiosubtype = "mp3";
        $filename = "mary.mp3";
    } else {
        $audiosubtype = "x-wav";
        $filename = "mary.wav";
    }
}

# announce data type on stdout
if ($save_to_disk) {
    print "Content-Type: application/octet-stream";
} else {
    print "Content-Type: audio/$audiosubtype";
}
print "\nContent-Disposition: filename=\"$filename\"\n\n";

# contact mary server
$host = "cling.dfki.uni-sb.de";
$port = 59125;

# create a tcp connection to the specified host and port
$maryInfoSocket = IO::Socket::INET->new(Proto     => "tcp",
				PeerAddr  => $host,
				PeerPort  => $port)
    or die "can't connect to port $port on $host: $!";

# avoid buffering when writing to server:
$maryInfoSocket->autoflush(1);              # so output gets there right away

########## Write input to server: ##########
# formulate the request:
print $maryInfoSocket "MARY IN=$in OUT=$out AUDIO=$audiotype";
if ($voice && $voice ne 'v') { print $maryInfoSocket " VOICE=$voice"; }
print $maryInfoSocket " LOG=\"REMOTE_HOST=$ENV{'REMOTE_HOST'}",
    ", REMOTE_ADDR=$ENV{'REMOTE_ADDR'}\"";
print $maryInfoSocket "\015\012";

# receive a request ID:
$id = <$maryInfoSocket>;

# open second socket for the data:
$maryDataSocket = IO::Socket::INET->new(Proto     => "tcp",
				PeerAddr  => $host,
				PeerPort  => $port)
    or die "can't connect to port $port on $host: $!";
# identify with request number:
print $maryDataSocket $id; # $id contains a newline character

# copy $inputtext to mary data socket
print $maryDataSocket $inputtext;

# mark end-of-request:
print $maryDataSocket "\015\012"; # that is a \n, actually
$maryDataSocket->shutdown(1); # we have stopped writing data

########## Read output from server: ##########
# copy the data socket to standard output
if ($out ne "AUDIO") { # text output
    my $line;
    while (defined ($line = <$maryDataSocket>)) {
        print STDOUT $line;
    }
} else { # audio data output
    my $nr; # number of bytes read
    my $buf; # buffer to read into
    my $outnr; # number of bytes written
    while($nr = read($maryDataSocket, $buf, 8192)) {
	# (read returns no. of bytes read, 0 at eof)
        print STDOUT $buf
            or die "Write error on stdout";
    } # while read something from socket
} # audio output

### Read complaints from server:
my $line;
while (defined ($line = <$maryInfoSocket>)) {
    print STDERR $line;
}







