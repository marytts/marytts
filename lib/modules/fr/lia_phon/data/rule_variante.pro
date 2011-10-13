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
#    Pour toute publication utilisant tout ou partie de LIA_PHON, la
#    reference suivante doit etre inseree dans la bibliographie :
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
/*  Regles de generation de variantes phonetiques  */

/* Rq: '*' est un separateur entre variantes */


regle(2,<l,"||",l>,"","","On supprime les marques de transitions impossibles")


/* E muet */

regle(2,<l,"ee",l>,"|ee*|","","soit ee soit rien")


/* Liaisons facultatives (ou normalement obligatoires) */

regle(2,<l,"|t",l>,"|tt*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|k",l>,"|kk*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|m",l>,"|mm*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|n",l>,"|nn*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|p",l>,"|pp*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|z",l>,"|zz*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|v",l>,"|vv*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|s",l>,"|ss*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|d",l>,"|dd*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|j",l>,"|jj*|","","soit la consonne de liaison soit rien")
regle(2,<l,"|l",l>,"|ll*|","","soit la consonne de liaison soit rien")
