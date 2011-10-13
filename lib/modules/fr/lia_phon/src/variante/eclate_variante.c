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
/*  Formate les sortie de Griphon au format de steph :
      * 1 fichier ne contenant que les mots avec un numero pour chacun
      * 1 fichier ou les mots sont associes a leur phonetique
	qd plusieurs phonetiques sont possibles, les mots sont
	reecrit a la ligne suivante  -  Fred 01/97  */

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
char ch[TailleLigne],resu[4000],*graf,*phon,*cate,ch2[TailleLigne];
FILE *filemots,*filephon;
int nb,n,i;

if (argc<2)
 {
 printf("Syntaxe : pprocess <nom fich sortie>\n");
 exit(0);
 }

sprintf(ch,"%s.mots",argv[1]);
if (!(filemots=fopen(ch,"wt")))
 {
 printf("ERREUR : Je ne peux ecrire dans le fichier %s ....\n",ch);
 exit(0);
 }
sprintf(ch,"%s.phon",argv[1]);
if (!(filephon=fopen(ch,"wt")))
 {
 printf("ERREUR : Je ne peux ecrire dans le fichier %s ....\n",ch);
 exit(0);
 }

for(nb=0;fgets(ch,TailleLigne,stdin);nb++)
 {
 if (!strncmp(ch,"<s>",3)) nb=0;

 graf=strtok(ch," \t\n");
 phon=strtok(NULL," \t\n");
 cate=strtok(NULL," \t\n");
 fprintf(filemots,"%s_%d\n",graf,nb);
 
 for(;(*cate)&&(*cate!='{');cate++);
 if (*cate)
  for(cate=strtok(cate+1,",}");(cate)&&(cate[0]!=']');cate=strtok(NULL,",}"))
   {
   sprintf(ch2,"%s_%d ",graf,nb);
   for(i=strlen(ch2),n=0;cate[n];n+=2)
    if (!strncmp(cate+n,"ee",2))
     { sprintf(ch2+i,"|eu#|"); i+=5; }
    else
     if (!strncmp(cate+n,"EU",2))
      { sprintf(ch2+i,"eu"); i+=2; }
     else
      { sprintf(ch2+i,"%c%c",cate[n],cate[n+1]); i+=2; }
   sprintf(ch2+i,"\n");
   ExpandResult(ch2,resu);
   fprintf(filephon,"%s",resu);
   }
 else
  {
  sprintf(ch2,"%s_%d %s\n",graf,nb,phon);
  ExpandResult(ch2,resu);
  fprintf(filephon,"%s",resu);
  }
 }
fclose(filephon);
fclose(filemots);

exit(0); 
}
 
