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
/*  Regles de postprocession des phonetiques  */

/*  Warning !! il faut faire des reecriture uniquement avec le meme  */
/*  nombre de caractere, cela est du a l'accolage des phonemes suiv et prec  */

regle(1,<l,"kkzz",l>,"ggzz","","eikkzzaakktt") 

/*  Les ii qui restent ii  */

regle(2,<"||rr","ii",V>,"ii","","rriion")

regle(2,<C.C,"ii",V>,"ii","","pprriion")

/*  Les ii qui se transforment en yy  */

regle(2,<V,"ii",V>,"yy","","aaiion")

regle(2,<C,"ii",V>,"yy","","lliieu")

regle(4,<C.C,"iiai",l>,"yyai","  ","lliiai")

regle(4,<C.C,"iiei",l>,"yyei","  ","bombardier")

regle(6,<C.C,"iiin",l>,"yyin","  ","lliiin")

regle(6,<"||".C,"iiin",l>,"yyin","  ","rriiin")

regle(6,<C.C,"iioe",l>,"yyoe","  ","ingénieur")

regle(6,<C.C,"iiEU",l>,"yyeu","  ","Montesquieu")

# regle(2,<l,"iiei",l>,"yyei","","ppiiei") 
#regle(3,<C.C.C,"iiai",l>,"iiai","","lliiai")
#regle(4,<l,"iiai",l>,"yyai","","lliiai")
#regle(6,<l,"iiin",l>,"yyin","","lliiin")
#regle(6,<l,"iioe",l>,"yyoe","","ingénieur")

/*  On transforme tous les II en ii  */

regle(8,<l,"II",l>,"ii","","LIA")

/*  On transforme tous les eu en ee  */

regle(8,<l,"eu",l>,"ee","","BRUTAL")

regle(6,<l,"EU",l>,"eu","","on remet les eu")

#regle(7,<V.C,"eu",C.V>,"ee","","mmouvveumman - un eu entre 2 voye")

#regle(8,<V.C,"eu",C.W.V>,"ee","","gibecière - un eu entre 2 voye")

#regle(9,<V.C.W,"eu",C.V>,"ee","","???? - un eu entre 2 voye")

#regle(10,<V."||".C,"eu",C.V>,"ee","","que||cela")

#regle(11,<V."||".C,"eu","||".C.V>,"ee","","cela||que||maintenant")

/*  Insertion de emuet entre les mots  */

regle(12,<D,"||",l>,"ee","","apres double generatrice")

#regle(12,<L,"||",P>,"ee","","entre liquide et occl sourde")

#regle(12,<L,"||",B>,"ee","","entre liquide et occl sonore")

#regle(12,<L,"||",F>,"ee","","entre liquide et fricative sourde")

#regle(12,<R,"||",B>,"ee","","entre RR et occl sonore")
 
/*  C'est tout pour le moment  */
