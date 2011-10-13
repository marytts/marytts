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

# Makefile ModuleGram #

all : $(LIA_PHON_REP)/bin/lia_compile_ml \
      $(LIA_PHON_REP)/bin/lia_compile_lexique \
      $(LIA_PHON_REP)/bin/lia_compile_arpa \
      $(LIA_PHON_REP)/bin/lia_compile_pmc \
      $(LIA_PHON_REP)/bin/lia_rajoute_lemme_ecg \
      $(LIA_PHON_REP)/bin/lia_rajoute_code \
      $(LIA_PHON_REP)/object/proba_mot_classe.o

$(LIA_PHON_REP)/bin/lia_compile_ml: compile_modele_langage.c \
		$(LIA_PHON_REP)/object/libgram.o \
		$(LIA_PHON_REP)/object/compile_gram.o \
		$(LIA_PHON_REP)/object/proba_mot_classe.o \
		$(LIA_PHON_REP)/object/gere_lexique.o
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_compile_ml compile_modele_langage.c \
		$(LIA_PHON_REP)/object/libgram.o \
		$(LIA_PHON_REP)/object/compile_gram.o \
		$(LIA_PHON_REP)/object/proba_mot_classe.o \
		$(LIA_PHON_REP)/object/gere_lexique.o $(LIBS)

$(LIA_PHON_REP)/bin/lia_compile_arpa: compile_arpa.c \
		$(LIA_PHON_REP)/object/libgram.o \
		$(LIA_PHON_REP)/object/compile_gram.o \
		$(LIA_PHON_REP)/object/proba_mot_classe.o \
		$(LIA_PHON_REP)/object/gere_lexique.o
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_compile_arpa compile_arpa.c \
		$(LIA_PHON_REP)/object/libgram.o \
		$(LIA_PHON_REP)/object/proba_mot_classe.o \
		$(LIA_PHON_REP)/object/compile_gram.o \
		$(LIA_PHON_REP)/object/gere_lexique.o $(LIBS)

$(LIA_PHON_REP)/bin/lia_compile_lexique: compile_lexique.c $(LIA_PHON_REP)/object/gere_lexique.o
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_compile_lexique compile_lexique.c \
		$(LIA_PHON_REP)/object/gere_lexique.o

$(LIA_PHON_REP)/bin/lia_compile_pmc: compile_pmc.c \
		$(LIA_PHON_REP)/object/libgram.o \
		$(LIA_PHON_REP)/object/gere_lexique.o \
		$(LIA_PHON_REP)/object/proba_mot_classe.o
	 $(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_compile_pmc compile_pmc.c \
		$(LIA_PHON_REP)/object/libgram.o \
		$(LIA_PHON_REP)/object/gere_lexique.o \
		$(LIA_PHON_REP)/object/proba_mot_classe.o $(LIBS)

$(LIA_PHON_REP)/bin/lia_rajoute_lemme_ecg: rajoute_lemme_ecg.c \
		$(LIA_PHON_REP)/object/libgram.o \
		$(LIA_PHON_REP)/object/gere_lexique.o \
		$(LIA_PHON_REP)/object/proba_mot_classe.o
	 $(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_rajoute_lemme_ecg rajoute_lemme_ecg.c \
		$(LIA_PHON_REP)/object/libgram.o \
		$(LIA_PHON_REP)/object/gere_lexique.o \
		$(LIA_PHON_REP)/object/proba_mot_classe.o $(LIBS)

$(LIA_PHON_REP)/bin/lia_rajoute_code: rajoute_code.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_rajoute_code rajoute_code.c

$(LIA_PHON_REP)/object/libgram.o: libgram.c
	$(CC) $(OPTIONS) -c libgram.c
	mv libgram.o $(LIA_PHON_REP)/object/libgram.o

$(LIA_PHON_REP)/object/compile_gram.o: compile_gram.c
	$(CC) $(OPTIONS) -c compile_gram.c
	mv compile_gram.o $(LIA_PHON_REP)/object/compile_gram.o

$(LIA_PHON_REP)/object/gere_lexique.o: gere_lexique.c
	$(CC) $(OPTIONS) -c gere_lexique.c
	mv gere_lexique.o $(LIA_PHON_REP)/object/gere_lexique.o

$(LIA_PHON_REP)/object/proba_mot_classe.o: proba_mot_classe.c
	$(CC) $(OPTIONS) -c proba_mot_classe.c
	mv proba_mot_classe.o $(LIA_PHON_REP)/object/proba_mot_classe.o
 
