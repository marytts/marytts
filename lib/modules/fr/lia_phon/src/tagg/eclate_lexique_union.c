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
/*  Transforme un fichier lexique_union en un fichier
    de compte+lemme compatible avec la bibliotheque pmc  */
/*  FRED 0699  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#define TailleLigne	10000

int main()
{
char ch[TailleLigne],*graf,*cate,*chnb,*lemm;
int nb;

for(nb=1;fgets(ch,TailleLigne,stdin);nb++)
 {
 if (nb%100000==0) fprintf(stderr,"\ten cours : %d mots\n",nb);

 graf=strtok(ch," \t\n");
 cate=strtok(NULL," \t\n");
 chnb=strtok(NULL," \t\n");
 lemm=strtok(NULL," \t\n");
 while (cate)
  {
  printf("%s\t%s\t%s\t%s\n",graf,cate,chnb,lemm);
  cate=strtok(NULL," \t\n");
  if (cate) chnb=strtok(NULL," \t\n");
  if (cate) lemm=strtok(NULL," \t\n");
  }
 }

exit(0); 
}
  
