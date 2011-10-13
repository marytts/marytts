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
/*  Produit un fichier lexique pour le reaccentueur :
    - prends en entree un lexique union eclate (mots\tcate\tcompte\tlemm)
    - remplace le lemme par le mot lui meme
    - enleve les accents des graphies ET laisse les graphies accentues
    - ressort le lexique au meme format
    entree : stdin
    sortie : stdout  */
/*  FRED 0201  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

/*................................................................*/

#define TailleLigne	400

#define True	1
#define False	0

void ERREUR(char *ch1,char *ch2)
{
fprintf(stderr,"ERREUR : %s %s\n",ch1,ch2);
exit(0);
}

/*................................................................*/

int poss_accent(char c)
{
if ((c>='1')&&(c<='4')) return True; else return False;
}

int main(int argc, char **argv)
{
char ch[TailleLigne],*graf,*cate,*compte,newgraf[TailleLigne];
int nb,i,j,accent_bdlex;

if ((argc>1)&&(!strcmp(argv[1],"-bdlex"))) accent_bdlex=1; else accent_bdlex=0;

for(nb=0;fgets(ch,TailleLigne,stdin);nb++)
 {
 if ((nb+1)%100000==0) fprintf(stderr,"En cours : %d\n",nb+1);
 graf=strtok(ch," \t\n");
 cate=strtok(NULL," \t\n");
 compte=strtok(NULL," \t\n");
 for(i=j=0;graf[i];)
  if (accent_bdlex)
   {
   newgraf[j++]=graf[i];
   if (
     (graf[i]=='a')&&(poss_accent(graf[i+1]))||
     (graf[i]=='e')&&(poss_accent(graf[i+1]))||
     (graf[i]=='i')&&(poss_accent(graf[i+1]))||
     (graf[i]=='o')&&(poss_accent(graf[i+1]))||
     (graf[i]=='u')&&(poss_accent(graf[i+1]))||
     (graf[i]=='c')&&(graf[i+1]=='5')
     )
     i+=2;
   else i++;
   }
  else
   {
   if ((graf[i]=='à')||(graf[i]=='â')) newgraf[j++]='a'; else
   if ((graf[i]=='ç')) newgraf[j++]='c'; else
   if ((graf[i]=='è')||(graf[i]=='é')||(graf[i]=='ê')||(graf[i]=='ë')) newgraf[j++]='e'; else
   if ((graf[i]=='î')||(graf[i]=='ï')) newgraf[j++]='i'; else
   if ((graf[i]=='ô')) newgraf[j++]='o'; else
   if ((graf[i]=='ù')||(graf[i]=='û')) newgraf[j++]='u'; else
   newgraf[j++]=graf[i];
   i++;
   }
 newgraf[j]='\0';

 printf("%s\t%s\t%s\t%s\n",graf,cate,compte,graf);
 if (strcmp(newgraf,graf)) printf("%s\t%s\t%s\t%s\n",newgraf,cate,compte,graf);
 }

exit(0); 
}
  
