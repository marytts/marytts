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
/*  Regles de generation de variantes phonetiques hors contexte (lexique) */

/* Rq: '*' est un separateur entre variantes */

regle(2,<"  ttou","ss||","  ">,"|ss*|","","cas particulier de tous")

regle(2,<l,"||",l>,"","","On supprime les marques de transitions impossibles")


/* E muet */

regle(2,<l,"ee",l>,"|ee*eu*oe*|","","soit ee soit eu soit oe soit rien")


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


/* Aperture de certaines voyelles (ouvert - fermé) */

regle(2,<l,"ai",l>,"|ai*ei|","","ouvert devient fermé")		/* dans les deux sens */
regle(2,<l,"ei",l>,"|ei*ai|","","fermé devient ouvert")

regle(2,<l,"oo",l>,"|oo*au|","","ouvert devient fermé")		/* dans les deux sens */
regle(2,<l,"au",l>,"|au*oo|","","fermé devient ouvert")

/* Cas pour lesquels on garde la distinction */

regle(3,<l,"EI",l>,"ei","","on remet les ei")
regle(3,<l,"AI",l>,"ai","","on remet les ai")




/* Ici la suppression de liaison fonctionne en sortie de remonte_liaison_variante	*/
/* donc adaptée uniquement au lexique							*/

/* Rq : cette règle est désormais remplacée par une règle de pprocess			*/

# regle(2,<l,"ss|z",l>,"ss","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"tt|t",l>,"tt","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"kk|k",l>,"kk","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"mm|m",l>,"mm","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"nn|n",l>,"nn","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"pp|p",l>,"pp","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"zz|z",l>,"zz","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"vv|v",l>,"vv","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"ss|s",l>,"ss","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"dd|d",l>,"dd","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"jj|j",l>,"jj","","on supprime les liaisons sur consonnes !!")
# regle(2,<l,"ll|l",l>,"ll","","on supprime les liaisons sur consonnes !!")



