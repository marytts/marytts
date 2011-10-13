/*
    --------------------------------------------------------
    LIA_PHON : Un systeme complet de phonetisation de textes
    --------------------------------------------------------

    Copyright (C) 2001 FREDERIC BECHET

    ..................................................................

    This file is part of LIA_PHON

    LIA_PHON is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
    ..................................................................

    For any publication related to scientific work using LIA_PHON,
    the following reference paper must be mentioned in the bibliography: 
        
    Bechet F., 2001, "LIA_PHON - Un systeme complet de phonetisation
    de textes", revue Traitement Automatique des Langues (T.A.L.)
    volume 42, numero 1/2001, edition Hermes
    ..................................................................
                              
    Contact :
              FREDERIC BECHET - LIA - UNIVERSITE D'AVIGNON
              AGROPARC BP1228 84911  AVIGNON  CEDEX 09  FRANCE
              frederic.bechet@lia.univ-avignon.fr
    ..................................................................
*/
/*  ..............................................  */
/*  Test le modele en direct sur l'entree standard  */
/*  ..............................................  */

#include <stdio.h>
#include <stdlib.h>
#include <GereTreeMorpho.h>
#include <LitTriMorpho.h>

ty_class Morpho_class,Morpho_classbi,Morpho_classun;

void ChargeModelMorpho(chnom)
 char *chnom;
{
int nbclass;
char ch[400];
sprintf(ch,"%s.tri",chnom);
Morpho_class=NewTabClass(ch,&nbclass);
sprintf(ch,"%s.bi",chnom);
Morpho_classbi=NewTabClass(ch,&nbclass);
sprintf(ch,"%s.un",chnom);
Morpho_classun=NewTabClass(ch,&nbclass);
}

void MorphoProbaClass(ch,tablch,tablscore,tailletabl)
 char *ch,**tablch;
 double *tablscore;
 int tailletabl;
{
char ch2[400];
sprintf(ch2,"%s##",ch);
accent_mot(ch2);
AffecteScoreTrueII(Morpho_class,Morpho_classbi,Morpho_classun,ch2,0);
SortScore(Morpho_class,Morpho_classbi,Morpho_classun);
RangeScore(Morpho_class,tablch,tablscore,tailletabl);
}

void LibereModelMorpho()
{
LibereClass(Morpho_class);
LibereClass(Morpho_classbi);
LibereClass(Morpho_classun);
}
 
