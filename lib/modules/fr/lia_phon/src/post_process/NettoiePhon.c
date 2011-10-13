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
/*  Nettoie les sorties phonetiques de Griphon
    au sens Aupelf :
    * on enleve les marques de liaisons et les categories et les signes autres que , . ! ?
    * on remplace la ponctuation phonetique par la ponctuation graphique

    et les ponctu par des silences  */

/*  Fred - 03/97  */

#include <stdio.h>

#define True	1
#define False	0

int main(int argc, char **argv)
{
char ch[200],*phon,*cate;
int n,keep_liaison;
unsigned long nb;

if ((argc==2)&&(!strcmp(argv[1],"-liaison"))) keep_liaison=True; else keep_liaison=False;

for(nb=0;fgets(ch,200,stdin);nb++)
 {
 for(n=0;(ch[n])&&(ch[n]!=' ');n++);
 if (ch[n]!=' ') { fprintf(stderr,"Mauvais format ligne %d\n",nb); exit(0); }
 ch[n]='\0';
 phon=ch+n+1;
 for(++n;(ch[n])&&(ch[n]!=' ');n++);
 if (ch[n]!=' ') { fprintf(stderr,"Mauvais format ligne %d\n",nb); exit(0); }
 ch[n]='\0';
 cate=ch+n+1;
 if (!strncmp(cate,"[YPF",4))
  {
  /*
  if ((ch[0]=='.')||(ch[0]==',')||(ch[0]=='!')||(ch[0]=='?'))
   printf("%s %s\n",ch,ch);
  */
  printf("pause ##\n");
  }
 else
  {
  printf("%s ",ch);
  for(n=0;phon[n];n++)
   if (phon[n]=='|')
    if ((!keep_liaison)||(phon[n+1]=='|'))n++;
    else printf("%c",phon[n+1]);
   else printf("%c",phon[n]);
  printf(" %s",cate);
  }
 }
exit(0);
}
  
