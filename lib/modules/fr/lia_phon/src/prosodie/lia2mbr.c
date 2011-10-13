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
/*  Transforme un fichier .ola ecrit avec
    l'alphabet phon du LIA dans l'alphabet MBROLA  */

#include <stdio.h>
#include <string.h>

#define TailleLigne	400

char *MBR2LIA[][2]=
	{
	{"i","ii"},
	{"e","ei"},
	{"E","ai"},
	{"a","aa"},
	{"O","oo"},
	{"o","au"},
	{"u","ou"},
	{"y","uu"},
	{"2","eu"},
	{"9","oe"},
	{"@","ee"},
	{"e~","in"},
	{"a~","an"},
	{"o~","on"},
	{"9~","un"},
	{"j","yy"},
	{"w","ww"},
	{"H","uy"},
	{"p","pp"},
	{"t","tt"},
	{"k","kk"},
	{"b","bb"},
	{"d","dd"},
	{"g","gg"},
	{"f","ff"},
	{"s","ss"},
	{"S","ch"},
	{"v","vv"},
	{"z","zz"},
	{"Z","jj"},
	{"l","ll"},
	{"R","rr"},
	{"m","mm"},
	{"n","nn"},
	{"N","ng"},
	{"_","##"},
	{"",""}
	} ;

char *Lia2Mbr(ch)
 char *ch;
{
int n;
for (n=0;MBR2LIA[n][0][0];n++)
 if (!strcmp(ch,MBR2LIA[n][1]))
  return MBR2LIA[n][0];
/* Si le symbole n'existe pas, on renvoi un silence */
return MBR2LIA[n-1][0];
}

int main()
{
char ch[TailleLigne],*pt;

/*  On ne transforme que le premier champs  */
while(fgets(ch,TailleLigne,stdin))
 {
 pt=strtok(ch," ");
 printf("%s %s",Lia2Mbr(pt),ch+strlen(pt)+1);
 }

exit(0); 
}
 
