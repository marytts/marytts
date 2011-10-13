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
#  Makefile RETIK  #
####################


all : $(LIA_PHON_REP)/bin/lia_retik

$(LIA_PHON_REP)/bin/lia_retik: \
		$(LIA_PHON_REP)/object/boss_retik.o \
		$(LIA_PHON_REP)/object/anaretik.o \
		$(LIA_PHON_REP)/object/GereTree.o \
		$(LIA_PHON_REP)/object/LitTri.o \
		$(LIA_PHON_REP)/object/NomPro.o
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_retik \
		$(LIA_PHON_REP)/object/boss_retik.o \
		$(LIA_PHON_REP)/object/anaretik.o \
		$(LIA_PHON_REP)/object/GereTree.o \
		$(LIA_PHON_REP)/object/LitTri.o \
		$(LIA_PHON_REP)/object/NomPro.o

$(LIA_PHON_REP)/object/boss_retik.o: boss_retik.c
	$(CC) $(OPTIONS) -c boss_retik.c
	mv boss_retik.o $(LIA_PHON_REP)/object/boss_retik.o

$(LIA_PHON_REP)/object/anaretik.o: anaretik.c
	$(CC) $(OPTIONS) -c anaretik.c
	mv anaretik.o $(LIA_PHON_REP)/object/anaretik.o

$(LIA_PHON_REP)/object/GereTree.o: GereTree.c
	$(CC) $(OPTIONS) -c GereTree.c
	mv GereTree.o $(LIA_PHON_REP)/object/GereTree.o

$(LIA_PHON_REP)/object/LitTri.o: LitTri.c
	$(CC) $(OPTIONS) -c LitTri.c
	mv LitTri.o $(LIA_PHON_REP)/object/LitTri.o

$(LIA_PHON_REP)/object/NomPro.o: NomPro.c
	$(CC) $(OPTIONS) -c NomPro.c
	mv NomPro.o $(LIA_PHON_REP)/object/NomPro.o
 
