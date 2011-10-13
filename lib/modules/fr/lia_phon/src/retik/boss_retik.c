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
/*  Reetiketage  */

#include <stdio.h>
#include <anaretik.h>

extern void NomProp_main();

int main(argc,argv)
 int argc;
 char **argv;
{
FILE *file;
char FichRegleR[TailleLigne];
int n,i;

if ((argc>1)&&(!strcmp(argv[1],"-h")))
 {
 fprintf(stderr,"Syntaxe : retik [-h] [-r]\n\
 \t\t -h : ce message\n\
 \t\t -r : chargement d'un autre fichier de regle que 'regles_retik'\n\
 \t\t      dans le repertoire courant\n\
 \t\t      C'est egalement dans ce repertoire que devront se trouver\n\
 \t\t      les fichier model pour le module nom propre 'model-1,2,3'\n\n\
 \t Programme de reetiquetage concernant certaines heuristiques,\n\
 \t telles que chiffre romain et ambiguites semantiques (fils, etc.)\n\
 \t ainsi que la determination de l'origine linguistique des noms propres.\n");
 exit(0);
 }

if ((argc>1)&&(!strcmp(argv[1],"-r")))
 {
 strcpy(FichRegleR,argv[2]);
 }
else
 {
#ifdef COMPIL_MSDOS
 for (i=n=0;argv[0][n];n++) if (argv[0][n]=='\\') i=n+1;
#else
 for (i=n=0;argv[0][n];n++) if (argv[0][n]=='/') i=n+1;
#endif
 strcpy(FichRegleR,argv[0]);
 strcpy(FichRegleR+i,"regles_retik");
 }

if (!(file=fopen(FichRegleR,"r")))
 {
 printf("Je ne peux pas ouvrir le fichier : %s\n",FichRegleR);
 exit(0);
 }
if (!LisReglesRetik(file))
 {
 printf("Le fichier %s n'est pas au format des regles de reetiquetage\n",FichRegleR);
 exit(0);
 }
debug=((argc>1)&&(!strcmp(argv[1],"-d")))?1:0;
TestRegle=0;
if ((argc>1)&&(!strcmp(argv[1],"-r"))) sscanf(argv[2],"%d",&TestRegle);

/*  Lecture des modeles d'origine linguistique  */
NomProp_main(FichRegleR);

traite_fichier(stdin,stdout);

exit(0); 
}
  
