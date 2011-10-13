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
/*  Compile un lexique ainsi qu'un modele n-gram  */
/*  FRED 0498  :  Modif Multi ML - 0399  */

#include <libgram.h>
#include <compile_gram.h>

#define TailleLigne	200

int main(int argc ,char **argv )
{
int si_log_e,si_2g,si_dicho,si_code;
FILE *file;
double coef1,coef2,coef3;
ty_lexique pt_lexique;
ty_ml pt_ml;

if ((argc==2)&&(!strcmp(argv[1],"-h")))
 {
 fprintf(stderr," <fich lexique> = nom du lexique au format sirocco\n\
 <fich arpa> = nom du fichier ML au format arpa\n\
 <log_10/log_e> = proba stockees en log base 10 ou log base e\n\
 <2g/3g> = compilation d'un modele bigramme ou trigramme\n\
 -dicho = flag signifiant que l'on veut utiliser la dichotomie comme recherche des elements\n\
 <coef 1g> <coef 2g> <coef 3g> : coeff de remplissage des tableaux de Hachage si l'on n'utilise\
 pas la dichotomie\n");
 exit(0);
 }

if (argc<6)
 {
 fprintf(stderr,"Syntaxe : compile_modele_langage [-h] <fich lexique> <fich arpa> <log_10/log_e>\
 <2g/3g> [-dicho | <coef 1g> <coef 2g> <coef 3g>]\n");
 exit(0);
 }

fprintf(stderr,"Compilation du lexique -> ");
pt_lexique=CompileLexique(argv[1]);
fprintf(stderr,"Terminee\n");

/*  Log base e ou base 10  */

if (!strcmp(argv[3],"log_e"))
 {
 si_log_e=1;
 fprintf(stderr,"On compile le ML en log neperien\n");
 }
else
 {
 si_log_e=0;
 fprintf(stderr,"On compile le ML en log base 10\n");
 }

/*  Bigramme ou trigramme  */

if (!strcmp(argv[4],"2g")) 
 {
 si_2g=1;
 fprintf(stderr,"On compile un modele bigramme\n");
 }
else
 {
 si_2g=0;
 fprintf(stderr,"On compile un modele trigramme\n");
 }

/*  Dichotomie ou hash code  */

if (!strcmp(argv[5],"-dicho")) si_dicho=1; else si_dicho=0;

/*  Creation des fichiers n-gram a partir d'un fichier ARPA  */

fprintf(stderr,"Initialisation des tableaux 1.2.3-grams -> \n");

if (si_dicho)
 {
 coef1=coef2=coef3=1.;
 pt_ml=InitGram(argv[2],argv[2],coef1,coef2,coef3,si_dicho,si_log_e,si_2g);
 }
else
 {
 sscanf(argv[5],"%lf",&coef1); sscanf(argv[6],"%lf",&coef2);
 if (si_2g) coef3=1.; else sscanf(argv[7],"%lf",&coef3);
 pt_ml=InitGram(argv[2],argv[2],coef1,coef2,coef3,si_dicho,si_log_e,si_2g);
 }

fprintf(stderr,"Terminee\n");

pt_ml->lexique=pt_lexique;

fprintf(stderr,"Lecture du fichier n-gram DARPA et creation des n-grams\n");
if (!(file=fopen(argv[2],"r"))) { fprintf(stderr,"Je ne peux lire: %s",argv[2]); exit(0); }

/*  on mets si_code a 0, c'est a dire que le fichier ARPA contient des graphies
    et non des codes de graphies  */ 
si_code=0;
LisFichDARPA(file,si_log_e,si_2g,si_code,pt_ml);
fclose(file);

if (si_dicho)
 {
 fprintf(stderr,"Tri des tableaux de Hash pour la dichotomie -> ");
 SortHash(pt_ml);
 fprintf(stderr,"Termine\n");
 }
SauveGram(argv[2],pt_ml);
delete_ml(pt_ml);

exit(0); 
}
 
