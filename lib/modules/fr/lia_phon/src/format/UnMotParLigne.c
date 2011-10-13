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
/* Permet d'eclater une phrase ecrite en paragraphe sous la
   forme : 1 mot par ligne, le separateur etant l'espace
   Entree : stdin
   Sortie : stdout */
/* FRED 0299 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#define TailleLigne 8000

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[TailleLigne],*pt;

if ((argc>1)&&(!strcmp(argv[1],"-h")))
 {
 fprintf(stderr,"Syntaxe : %s [-h] < stdin > stdout\n\
 \t Ce programme permet d'eclater une phrase ecrite en\n\
 \t paragraphe sous la forme : 1 mot par ligne,\n\
 \t le separateur etant l'espace.\n\
 \t Entree : stdin  -  Sortie : stdout\n",argv[0]);
 exit(0);
 }
while(fgets(ch,TailleLigne,stdin))
 for(pt=strtok(ch," \t\n");pt;pt=strtok(NULL," \t\n")) printf("%s\n",pt);

exit(0); 
}
  
