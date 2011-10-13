#
#    --------------------------------------------------------
#    LIA_PHON : Un systeme complet de phonetisation de textes
#    --------------------------------------------------------
#
#    Copyright (C) 2001 FREDERIC BECHET
#
#    ..................................................................
#
#    This file is part of LIA_PHON
#
#    LIA_PHON is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program; if not, write to the Free Software
#    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
#    ..................................................................
#
#    For any publication related to scientific work using LIA_PHON,
#    the following reference paper must be mentioned in the bibliography: 
#        
#    Bechet F., 2001, "LIA_PHON - Un systeme complet de phonetisation
#    de textes", revue Traitement Automatique des Langues (T.A.L.)
#    volume 42, numero 1/2001, edition Hermes
#    ..................................................................
#                              
#    Contact :
#              FREDERIC BECHET - LIA - UNIVERSITE D'AVIGNON
#              AGROPARC BP1228 84911  AVIGNON  CEDEX 09  FRANCE
#              frederic.bechet@lia.univ-avignon.fr
#    ..................................................................
#
#

#  Makefile LIA_PHON  #

# C Compiler definition
#
CC = gcc

# Math library
#
LIBS = -lm



# Include files
#
INCLUDES = -I. -I$(LIA_PHON_REP)/src/aphon -I$(LIA_PHON_REP)/src/libgram -I$(LIA_PHON_REP)/src/format

# C compiler options (default=NONE)
#
OPTIONS = $(INCLUDES)

# Source compilation

MAKE = make LIBS="$(LIBS)" INCLUDES="$(INCLUDES)" OPTIONS="$(OPTIONS)" CC="$(CC)"

all :
	@set fnord $(MAKEFLAGS); \
        subdir='$(LIA_PHON_REP)/src/format' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all
	@set fnord $(MAKEFLAGS); \
	subdir='$(LIA_PHON_REP)/src/libgram' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all
	@set fnord $(MAKEFLAGS); \
	subdir='$(LIA_PHON_REP)/src/aphon' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all
	@set fnord $(MAKEFLAGS); \
	subdir='$(LIA_PHON_REP)/src/liaison' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all
	@set fnord $(MAKEFLAGS); \
	subdir='$(LIA_PHON_REP)/src/post_process' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all
	@set fnord $(MAKEFLAGS); \
	subdir='$(LIA_PHON_REP)/src/retik' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all
	@set fnord $(MAKEFLAGS); \
	subdir='$(LIA_PHON_REP)/src/prosodie' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all
	@set fnord $(MAKEFLAGS); \
	subdir='$(LIA_PHON_REP)/src/variante' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all
	@set fnord $(MAKEFLAGS); \
	subdir='$(LIA_PHON_REP)/src/tagg' ; \
	echo "Making all in $$subdir" ; \
	cd $$subdir && $(MAKE) all

# Cleaning binaries
clean :
	rm $(LIA_PHON_REP)/bin/*
	rm $(LIA_PHON_REP)/object/*

# Compilation of data files
ressource:
	chmod ug+x $(LIA_PHON_REP)/script/*
	$(LIA_PHON_REP)/bin/lia_compile_exep $(LIA_PHON_REP)/data/list_exep
	$(LIA_PHON_REP)/bin/lia_compile_lexique $(LIA_PHON_REP)/data/list_exep.sirlex
	$(LIA_PHON_REP)/bin/lia_compile_lexique $(LIA_PHON_REP)/data/h_aspi.sirlex
	csh $(LIA_PHON_REP)/script/lia_make_lexique_reacc $(LIA_PHON_REP)/data/lex10k $(LIA_PHON_REP)/data/lm3classe.arpa
	$(LIA_PHON_REP)/bin/lia_compile_lexitree $(LIA_PHON_REP)/data/list_chif_virgule $(LIA_PHON_REP)/data/list_chif_virgule.tab
	$(LIA_PHON_REP)/bin/lia_compile_lexitree $(LIA_PHON_REP)/data/lex10k $(LIA_PHON_REP)/data/lex10k.tab

lex80k:
	gunzip $(LIA_PHON_REP)/data/lex80k
	csh $(LIA_PHON_REP)/script/lia_make_lexique_reacc $(LIA_PHON_REP)/data/lex80k $(LIA_PHON_REP)/data/lm3classe.arpa
	$(LIA_PHON_REP)/bin/lia_compile_lexitree $(LIA_PHON_REP)/data/lex80k  $(LIA_PHON_REP)/data/lex80k.tab
	gzip $(LIA_PHON_REP)/data/lex80k

# Cleaning binary data files
clean_ressource:
	csh $(LIA_PHON_REP)/script/lia_delete_lexicon lex10k
	csh $(LIA_PHON_REP)/script/lia_delete_lexicon lex80k
	rm $(LIA_PHON_REP)/data/list_exep.*
	rm $(LIA_PHON_REP)/data/h_aspi.sirlex.*
	rm $(LIA_PHON_REP)/data/lm3classe.arpa.*
	rm $(LIA_PHON_REP)/data/list_chif_virgule.tab

# Checking if everything is OK
check:
	$(LIA_PHON_REP)/script/lia_check
 
