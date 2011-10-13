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
/*  qd plusieurs phonetiques sont possibles, les mots sont
    reecrit a la ligne suivante  -  Fred 01/97 - MODIF 0403  */

/*  Eclate sur plusieurs lignes les variantes de prononciation  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#define TailleLigne 2000

/*  Take an output of the phonetize function and expand it into several phonetical
    form (if possible) to propose as output a string with the pronounciations separate by \n  */

void ExpandResultII(ch,chtemp,i,pos,flag,result)
 char *ch,*chtemp;
 int i,pos,flag;
 char *result;
{
for(;(ch[i])&&(ch[i]!='|')&&(ch[i]!='#');i++) chtemp[pos++]=ch[i];

if (ch[i]=='\0') { chtemp[pos++]='\0'; sprintf(result,"%s%s",result,chtemp); return; }

if ((flag)&&(ch[i]=='|')) ExpandResultII(ch,chtemp,i+1,pos,0,result);
else
 if (ch[i]=='#')
  {
  while((ch[i])&&(ch[i]!='|')) i++;
  if (ch[i]) ExpandResultII(ch,chtemp,i+1,pos,0,result);
  }
 else
  {
  ExpandResultII(ch,chtemp,i+1,pos,1,result);
  for(i++;(ch[i])&&(ch[i]!='|');i++)
   if (ch[i]=='#') ExpandResultII(ch,chtemp,i+1,pos,1,result);
  }
}

void ExpandResult(chin,result)
 char *chin,*result;
{
char chtemp[400];
result[0]='\0';
ExpandResultII(chin,chtemp,0,0,0,result);
}

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[TailleLigne],resu[4000];
for(;fgets(ch,TailleLigne,stdin);)
 {
 ExpandResult(ch,resu);
 printf("%s",resu);
 }
exit(0); 
}
 
