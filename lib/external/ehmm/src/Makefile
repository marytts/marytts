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
##                                                                       ##
##  C++ part of the labeler                                              ##
##                                                                       ##
###########################################################################
TOP=../
DIRNAME=src/ehmm/src
BUILD_DIRS = 
ALL_DIRS = $(BUILD_DIRS)
SRCS = FeatureExtraction.cc edec.cc ehmm.cc ConvertFeatsFileToBinaryFormat.cc ScaleBinaryFeats.cc threading.cc
H = header.h hmmstate.h hmmword.h threading.h
FILES = Makefile $(SRCS) $(H)

EHMMBINDIR=../bin

ALL = $(EHMMBINDIR)/FeatureExtraction \
      $(EHMMBINDIR)/edec \
      $(EHMMBINDIR)/ehmm \
      $(EHMMBINDIR)/ConvertFeatsFileToBinaryFormat \
      $(EHMMBINDIR)/ScaleBinaryFeats \

include $(TOP)/common_make_rules

# Try and see if EST's config is where its said to be
#estconfig_dummy := $(shell test -f $(ESTDIR)/config/config || { echo '*** '; echo '*** Speech Tools config not found in '; echo '*** $(ESTDIR)/config/config.'; echo '*** Check EST installation and/or ESTDIR in festvox/config/config '; echo '*** '; }  >&2)

# GCC and systems settings got from EST's config
#EST=$(ESTDIR)
#include $(ESTDIR)/config/config
#include $(ESTDIR)/config/rules/defaults.mak
#include $(ESTDIR)/config/rules/compile_options.mak

#ESTINC=
#ESTLIBS=

CXXFLAGS += -O2 $(ESTINC)
CXX=g++
LINK_COMMAND = g++

LOCAL_CLEAN = $(ALL)

ifdef NOPTHREADFLAGS
CXXFLAGS += "-DFESTVOX_NO_THREADS"
endif

$(EHMMBINDIR)/FeatureExtraction: FeatureExtraction.o $(H)
	$(strip $(LINK_COMMAND)) -o $@ FeatureExtraction.o
$(EHMMBINDIR)/ConvertFeatsFileToBinaryFormat: ConvertFeatsFileToBinaryFormat.o
	$(strip $(LINK_COMMAND)) -o $@ ConvertFeatsFileToBinaryFormat.o

ifndef NOPTHREADFLAGS
$(EHMMBINDIR)/edec: edec.o threading.o $(H)
	$(strip $(LINK_COMMAND)) -o $@ edec.o threading.o -lpthread

$(EHMMBINDIR)/ehmm: ehmm.o threading.o $(H)
	$(strip $(LINK_COMMAND)) -o $@ ehmm.o threading.o -lpthread

$(EHMMBINDIR)/ScaleBinaryFeats: ScaleBinaryFeats.o
	$(strip $(LINK_COMMAND)) -o $@ ScaleBinaryFeats.o threading.o -lpthread
else
$(EHMMBINDIR)/edec: edec.o $(H)
	$(strip $(LINK_COMMAND)) -o $@ edec.o

$(EHMMBINDIR)/ehmm: ehmm.o $(H)
	@echo
	@echo "*****************************************************"
	@echo "NOTE: Compiling EHMM without multithreading support"
	@echo "*****************************************************"
	@echo
	$(strip $(LINK_COMMAND)) -o $@ ehmm.o
$(EHMMBINDIR)/ScaleBinaryFeats: ScaleBinaryFeats.o
	$(strip $(LINK_COMMAND)) -o $@ ScaleBinaryFeats.o
endif
