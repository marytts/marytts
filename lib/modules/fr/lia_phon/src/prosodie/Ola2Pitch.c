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
/*  Extrait la courbe de pitch d'un fichier .pho ola  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int ReadChain(ch,last)
 char *ch;
 int last;
{
int n,duree,fait=0,i,j,place;

for(n=0;(ch[n])&&(ch[n]!=' ');n++);
if (ch[n]!=' ') { fprintf(stderr,"Bad Format\n"); exit(0); }

sscanf(ch+n+1,"%d",&duree);

for(++n;(ch[n])&&(ch[n]!=' ');n++);
for(;(ch[n])&&(ch[n]==' ');n++);

/*printf("Duree : %d\n",duree);*/

while(ch[n])
 {
 sscanf(ch+n,"%d",&i);
 for(++n;(ch[n])&&(ch[n]!=' ');n++);
 if (ch[n]!=' ') { fprintf(stderr,"Bad Format\n"); exit(0); }
 sscanf(ch+n+1,"%d",&j);
 for(++n;(ch[n])&&(ch[n]!=' ');n++);
 for(;(ch[n])&&(ch[n]==' ');n++);
 place=(int)((i*duree)/100);

 /*printf("  avec a %d%% un pitch de %d (en place = %d)\n",i,j,place); */

 for(;fait<place;fait++) if ((fait%10)==0) printf("%d\n",last);
 last=j;
 }
for(;fait<duree;fait++) if ((fait%10)==0) printf("%d\n",last);
return last;
}

int main()
{
char ch[200];
int last;

/*printf("Pitch\n");*/
for(last=68;fgets(ch,200,stdin);)
 {
 ch[strlen(ch)-1]='\0';
 if ((ch[0]!=';')&&(ch[0]!='\n')) last=ReadChain(ch,last);
 }

exit(0);
}
 
