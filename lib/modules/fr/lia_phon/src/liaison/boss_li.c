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
/*  Genere Liaison  */

#include <stdio.h>
#include <liaison.h>
#include <libgram.h>

int main(argc,argv)
 int argc;
 char **argv;
{
FILE *file;
char FichRegleL[TailleLigne];
ty_lexique lexiqueH;

if (argc<=2)
 {
 fprintf(stderr,"Syntaxe : liphon <fich regle> <list h>\n");
 exit(0);
 }

if (!(file=fopen(argv[1],"rt")))
 {
 printf("Je ne peux pas ouvrir le fichier : %s\n",argv[1]);
 exit(0);
 }
if (!LisReglesLiaison(file))
 {
 printf("Le fichier %s n'est pas au format des regles de liaison\n",FichRegleL);
 exit(0);
 }

lexiqueH=ChargeLexique(argv[2]);
debug=((argc>3)&&(!strcmp(argv[3],"-d")))?1:0;
TestRegle=0;
traite_fichier(stdin,stdout,lexiqueH);
delete_lexique(lexiqueH);

exit(0); 
}
  
