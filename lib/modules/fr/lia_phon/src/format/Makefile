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
# Makefile : formatage et nettoyage de textes
# FRED 0299

all : $(LIA_PHON_REP)/bin/lia_compile_lexitree \
      $(LIA_PHON_REP)/bin/lia_tokenize \
      $(LIA_PHON_REP)/bin/lia_sentence \
      $(LIA_PHON_REP)/bin/lia_nett_capital \
      $(LIA_PHON_REP)/bin/lia_rm_ponct \
      $(LIA_PHON_REP)/bin/lia_nomb2alpha \
      $(LIA_PHON_REP)/bin/lia_unmotparligne \
      $(LIA_PHON_REP)/bin/lia_split_sentence

$(LIA_PHON_REP)/bin/lia_compile_lexitree: CompileLexiTree.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_compile_lexitree CompileLexiTree.c

$(LIA_PHON_REP)/bin/lia_tokenize: DecoupeMots.c $(LIA_PHON_REP)/object/GestionTablMots.o
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_tokenize DecoupeMots.c \
		$(LIA_PHON_REP)/object/GestionTablMots.o

$(LIA_PHON_REP)/bin/lia_sentence: DecoupePhrases.c $(LIA_PHON_REP)/object/GestionTablMots.o
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_sentence DecoupePhrases.c \
		$(LIA_PHON_REP)/object/GestionTablMots.o

$(LIA_PHON_REP)/bin/lia_nett_capital: EliminePremMajuscule.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_nett_capital EliminePremMajuscule.c

$(LIA_PHON_REP)/bin/lia_rm_ponct: EnlevePonctuation.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_rm_ponct EnlevePonctuation.c

$(LIA_PHON_REP)/bin/lia_nomb2alpha: Nombre2Alpha.c \
		$(LIA_PHON_REP)/object/GestionTablMots.o \
		$(LIA_PHON_REP)/object/chif_to_alpha.o
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_nomb2alpha Nombre2Alpha.c \
		$(LIA_PHON_REP)/object/GestionTablMots.o \
		$(LIA_PHON_REP)/object/chif_to_alpha.o

$(LIA_PHON_REP)/bin/lia_unmotparligne: UnMotParLigne.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_unmotparligne UnMotParLigne.c

$(LIA_PHON_REP)/bin/lia_split_sentence: split_sentence.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_split_sentence split_sentence.c

$(LIA_PHON_REP)/object/GestionTablMots.o: GestionTablMots.c
	$(CC) $(OPTIONS) -c GestionTablMots.c
	mv GestionTablMots.o $(LIA_PHON_REP)/object/GestionTablMots.o

$(LIA_PHON_REP)/object/chif_to_alpha.o: chif_to_alpha.c
	$(CC) $(OPTIONS) -c chif_to_alpha.c 
	mv chif_to_alpha.o $(LIA_PHON_REP)/object/chif_to_alpha.o

