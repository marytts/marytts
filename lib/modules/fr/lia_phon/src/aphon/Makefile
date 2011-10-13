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
####################
####################
#  Makefile APHON  #
####################
####################


all : $(LIA_PHON_REP)/bin/lia_phonetise $(LIA_PHON_REP)/bin/lia_compile_exep

$(LIA_PHON_REP)/bin/lia_phonetise: $(LIA_PHON_REP)/object/boss.o \
			 $(LIA_PHON_REP)/object/declphon.o \
			 $(LIA_PHON_REP)/object/syllabise.o \
			 $(LIA_PHON_REP)/object/phonetise.o \
			 $(LIA_PHON_REP)/object/ModuExep.o \
			 $(LIA_PHON_REP)/object/chif_to_alpha.o \
			 $(LIA_PHON_REP)/object/gere_lexique.o \
			 $(LIA_PHON_REP)/object/gere_exception.o
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_phonetise \
			 $(LIA_PHON_REP)/object/boss.o \
			 $(LIA_PHON_REP)/object/declphon.o \
			 $(LIA_PHON_REP)/object/syllabise.o \
			 $(LIA_PHON_REP)/object/phonetise.o \
			 $(LIA_PHON_REP)/object/ModuExep.o \
			 $(LIA_PHON_REP)/object/chif_to_alpha.o \
			 $(LIA_PHON_REP)/object/gere_lexique.o \
			 $(LIA_PHON_REP)/object/gere_exception.o

$(LIA_PHON_REP)/bin/lia_compile_exep: compile_exep.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_compile_exep compile_exep.c

$(LIA_PHON_REP)/object/boss.o: boss.c
	$(CC) $(OPTIONS) -c boss.c
	mv boss.o $(LIA_PHON_REP)/object/boss.o

$(LIA_PHON_REP)/object/declphon.o: declphon.c
	$(CC) $(OPTIONS) -c declphon.c
	mv declphon.o $(LIA_PHON_REP)/object/declphon.o

$(LIA_PHON_REP)/object/syllabise.o: syllabise.c
	$(CC) $(OPTIONS) -c syllabise.c
	mv syllabise.o $(LIA_PHON_REP)/object/syllabise.o

$(LIA_PHON_REP)/object/phonetise.o: phonetise.c
	$(CC) $(OPTIONS) -c phonetise.c
	mv phonetise.o $(LIA_PHON_REP)/object/phonetise.o

$(LIA_PHON_REP)/object/ModuExep.o: ModuExep.c
	$(CC) $(OPTIONS) -c ModuExep.c
	mv ModuExep.o $(LIA_PHON_REP)/object/ModuExep.o

$(LIA_PHON_REP)/object/gere_exception.o: gere_exception.c
	$(CC) $(OPTIONS) -c gere_exception.c 
	mv gere_exception.o $(LIA_PHON_REP)/object/gere_exception.o
  
