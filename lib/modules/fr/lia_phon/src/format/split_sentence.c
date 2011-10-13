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
/*  Slit a tagged corpus into sentence  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

/*................................................................*/

#define TailleLigne	1000

#define True	1
#define False	0

#define max(a,b)        ((a)>(b)?(a):(b))
#define min(a,b)        ((a)<(b)?(a):(b))

void ERREUR(char *ch1,char *ch2)
{
fprintf(stderr,"ERREUR : %s %s\n",ch1,ch2);
exit(0);
}

/*................................................................*/

int main(int argc, char **argv)
{
char ch[TailleLigne];
int nb=0;
FILE *filein,*fileout=NULL;

if (argc<=2)
 {
 fprintf(stderr,"Syntaxe : %s <tagged file to split> <output file>\n",argv[0]);
 exit(0);
 }

if (!(filein=fopen(argv[1],"rt"))) ERREUR("can't open:",argv[1]);

while(fgets(ch,TailleLigne,filein))
 {
 if (!strncmp(ch,"<s>",3))
  {
  if (fileout) fclose(fileout);
  sprintf(ch,"%s.%d",argv[2],++nb);
  if (!(fileout=fopen(ch,"wt"))) ERREUR("can't write in:",ch);
  fprintf(fileout,"<s> ZTRM\n");
  }
 else if (fileout) fprintf(fileout,"%s",ch);
 }
fclose(filein);
if (fileout) fclose(fileout);

exit(0); 
}
 
