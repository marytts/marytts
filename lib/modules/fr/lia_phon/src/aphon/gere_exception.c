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
/*  Gere les exceptions a la phonetisation  */
/*  FRED  */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#include <libgram.h>

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

char *Exep_ZonePhon;
ty_lexique Exep_LexiGraf;

void charge_exception(char *nomfich)
{
char ch[TailleLigne];
int taille;
FILE *filephon;

sprintf(ch,"%s.sirlex",nomfich);
Exep_LexiGraf=ChargeLexique(ch);
sprintf(ch,"%s.zonpho",nomfich);
if (!(filephon=fopen(ch,"rb"))) ERREUR("can't open:",ch);
fseek(filephon,0,SEEK_END);
Exep_ZonePhon=(char*)malloc(sizeof(char)*(int)(taille=ftell(filephon))/sizeof(char));
fseek(filephon,0,SEEK_SET);
if (fread(Exep_ZonePhon,sizeof(char),taille,filephon)!=taille) ERREUR("pb dans la lecture de :",ch);
fclose(filephon);
}

char *trouve_exception(char *pt)
{
wrd_index_t indice;
if (Mot2Code(pt,&indice,Exep_LexiGraf)) return (Exep_ZonePhon+indice);
else return NULL;
}

void delete_charge_exception()
{
delete_lexique(Exep_LexiGraf);
}  
