#!/usr/bin/env perl
#
# MARY Text-to-Speech System
# Minimal Socket client (for demonstration)
##########################################################################
# Copyright (C) 2000-2006 DFKI GmbH.
# All rights reserved. Use is subject to license terms.
#
# Permission is hereby granted, free of charge, to use and distribute
# this software and its documentation without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of this work, and to
# permit persons to whom this work is furnished to do so, subject to
# the following conditions:
# 
#  1. The code must retain the above copyright notice, this list of
#     conditions and the following disclaimer.
#  2. Any modifications must be clearly marked as such.
#  3. Original authors' names are not deleted.
#  4. The authors' names are not used to endorse or promote products
#     derived from this software without specific prior written
#     permission.
# 
# DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH 
# REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF 
# MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE 
# CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL 
# DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR 
# PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS 
# ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF 
# THIS SOFTWARE.
##########################################################################
# Author: Marc Schroeder
# This is a minimal version of a socket client for the mary TtS system.
# It is intended to be used as a model for writing socket clients for
# particular applications. All input verification, command line options,
# and other luxury have been omitted.
#
# Usage:
# maryclient.pl infile.txt > outfile.wav
#
# Input/output formats and other options must be set in the perl code directly.
# See also Protocol.html for a description of the Protocol.
#

use strict;
use IO::Socket;

############################
# Package-global variables #
############################
# global settings:
my $maryInfoSocket; # handle to socket server
my $maryDataSocket; # handle to socket server
my $host; # string containing host address
my $port;   # socket port on which we listen
my ($in, $out, $audiotype); # requested input / output format
my $voice; # default voice
my $id; # request ID

######################################################################
################################ main ################################
######################################################################

STDOUT->autoflush(1);

$host = "cling.dfki.uni-sb.de";
$port = 59125;
$in  = "TEXT_DE";
$out = "AUDIO";
$audiotype = "MP3";
#$audiotype = "WAVE";
#$voice = "male";
$voice = "de3";

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
if ($voice) { print $maryInfoSocket " VOICE=$voice"; }
print $maryInfoSocket "\015\012";

# receive a request ID:
$id = <$maryInfoSocket>;
chomp $id; chomp $id;

# open second socket for the data:
$maryDataSocket = IO::Socket::INET->new(Proto     => "tcp",
				PeerAddr  => $host,
				PeerPort  => $port)
    or die "can't connect to port $port on $host: $!";
# identify with request number:
print $maryDataSocket $id, "\015\012";

# copy standard input and/or files given on the command line to the socket
while (defined (my $line = <>)) {
    print $maryDataSocket $line;
}
# mark end-of-request:
print $maryDataSocket "\015\012"; # that is a \n, actually
shutdown($maryDataSocket, 1); # we have stopped writing data

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
    while($nr = read($maryDataSocket, $buf, 100000)) {
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



