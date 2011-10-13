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
/*  Compile une liste d'exceptions au format suivant:
    - 1 mot par ligne
    - pour chaque mot: graphie phonetisation
    exemple:
    # ddyyaizz
    $ ddoollaarr
    Produit un fichier .sirlex compilable avec 'compile_lexique'
    et un fichier .zonpho contenant les phonetisations
*/
/*  FRED  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

/*................................................................*/

#define TailleLigne	10000

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
char ch[TailleLigne],*zone,*graf,*phon;
int nb,indice,taille;
FILE *file,*filegraf,*filephon;

if (argc<2)
 {
 fprintf(stderr,"Syntaxe : %s <list exceptions phonetisees>\n",argv[0]);
 exit(0);
 }

sprintf(ch,"%s.sirlex",argv[1]);
if (!(filegraf=fopen(ch,"wt"))) ERREUR("can't write in:",ch);
sprintf(ch,"%s.zonpho",argv[1]);
if (!(filephon=fopen(ch,"wb"))) ERREUR("can't write in:",ch);

/* calcul de la taille max des phonetisations */
if (!(file=fopen(argv[1],"rt"))) ERREUR("can't open :",argv[1]);
fseek(file,0,SEEK_END);
zone=(char*)malloc(sizeof(char)*(int)(taille=ftell(file)/sizeof(char)));
fseek(file,0,SEEK_SET);

for(zone[0]='\0',indice=0,nb=0;fgets(ch,TailleLigne,file);nb++)
 {
 if ((nb+1)%100000==0) fprintf(stderr,"En cours : %d\n",nb+1);

 graf=strtok(ch," \t\n"); phon=strtok(NULL," \t\n");
 fprintf(filegraf,"%d\t%s\n",indice,graf);
 strcpy(zone+indice,phon);
 zone[indice+strlen(phon)]='\0';
 indice+=strlen(phon)+1;

 if (indice>=taille-10) ERREUR("pourquoi 'taille' est-t-il trop petit ??",phon);
 }

fwrite(zone,sizeof(char),indice,filephon);

fclose(file);
fclose(filegraf); fclose(filephon);

exit(0); 
}
 
