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

CC=g++
CFLAGS=-Wall -w -O3 -g
ICUDIR=/usr/local/icu
ICULIBS=-Wl,-R,$(ICUDIR)/lib -L$(ICUDIR)/lib -licuuc -licui18n -lpthread -ldl


all: demo_mary_client

with_unicode: unicode.o
	make "CFLAGS=$(CFLAGS) LIBS=$(ICULIBS) -DWITH_UNICODE" all

demo_mary_client: mary_client.o demo_mary_client.o
	$(CC) $(CFLAGS) *.o -o demo_mary_client $(LIBS)


unicode.o: unicode.cc unicode.h
	$(CC) $(CFLAGS) -I$(ICUDIR)/include -o unicode.o -c unicode.c

%.o: %.cc %.h
	$(CC) $(CFLAGS) $(RFLAGS) -o $@ -c $<

clean:
	rm -rf ./*.o ./demo_mary_client

