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
# Makefile Variante #


all :	$(LIA_PHON_REP)/bin/lia_genevari \
	$(LIA_PHON_REP)/bin/lia_eclate_variante \
	$(LIA_PHON_REP)/bin/lia_remonte_liaison_variante \
	$(LIA_PHON_REP)/bin/lia_expand_phon

$(LIA_PHON_REP)/bin/lia_genevari: genevari.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_genevari genevari.c

$(LIA_PHON_REP)/bin/lia_eclate_variante: eclate_variante.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_eclate_variante eclate_variante.c

$(LIA_PHON_REP)/bin/lia_remonte_liaison_variante: remonte_liaison_variante.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_remonte_liaison_variante remonte_liaison_variante.c

$(LIA_PHON_REP)/bin/lia_expand_phon: expand_phon.c
	$(CC) $(OPTIONS) -o $(LIA_PHON_REP)/bin/lia_expand_phon expand_phon.c

