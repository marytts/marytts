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
/*  QUICKTAGG : un tagger simple et convivial !!
    Entree : stdin
    Sortie : stdout
    FRED 0299  - MODIF 1199  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <math.h>

#include <libgram.h>
#include <quicktagg_lib.h>

#define TailleLigne	2000

/* Stockage de la phrase  */
#define MaxMots         2000
char *Phrase[MaxMots];

void ERREUR(char *ch1, char *ch2)
{
fprintf(stderr,"ERREUR: %s %s\n",ch1,ch2);
exit(0);
}

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[TailleLigne],*pt,*lextag=NULL,*lexgraf=NULL,*ml=NULL,*morpho=NULL,*pmc=NULL;
int i,indice,nb_phrase,num_modele,nb_in,nb_out,si_af_proba=0;
float proba;
ty_lexique lexigraf;

if ((argc<2)||(!strcmp(argv[1],"-h")))
 {
 fprintf(stderr,"Syntaxe : QuickTagg [-h] -lextag <dico tags> -lexgraf <dicograf> -ml <ml 3gram> \
-pmc <modele pmc> -morpho <ml morpho> [-proba]\n");
 exit(0);
 }

for(i=1;i<argc;i++)
 {
 if (!strcmp(argv[i],"-lextag")) { if (i==argc-1) ERREUR("missing value after:",argv[i]); lextag=argv[++i]; } else
 if (!strcmp(argv[i],"-lexgraf")) { if (i==argc-1) ERREUR("missing value after:",argv[i]); lexgraf=argv[++i]; } else
 if (!strcmp(argv[i],"-ml")) { if (i==argc-1) ERREUR("missing value after:",argv[i]); ml=argv[++i]; } else
 if (!strcmp(argv[i],"-pmc")) { if (i==argc-1) ERREUR("missing value after:",argv[i]); pmc=argv[++i]; } else
 if (!strcmp(argv[i],"-morpho")) { if (i==argc-1) ERREUR("missing value after:",argv[i]); morpho=argv[++i]; } else
 if (!strcmp(argv[i],"-morpho")) si_af_proba=1;
 else { ERREUR("unknown parameter:",argv[i]); }
 }

init_quicktagg(lextag,morpho,ml,pmc,&num_modele,lexgraf,&lexigraf);

indice=0;
nb_in=nb_out=0;
for (nb_phrase=0;fgets(ch,MaxMots,stdin);)
 for(pt=strtok(ch," \t\n");pt;pt=strtok(NULL," \t\n"))
  {
  Phrase[indice++]=strdup(pt);
  if ((indice==MaxMots)||(!strcmp(pt,"</s>")))
   {
   proba=tagg_phrase(indice,Phrase,num_modele,stdout,&nb_in,&nb_out,lexigraf);
   if (si_af_proba) printf("SCORE_3CLASS=%.2lf\tPROP_MOTINC=%.2lf\n",
	proba/(float)indice,nb_out>0?log10((double)nb_out/(double)(nb_in+nb_out)):-99.99);
   nb_in=nb_out=0;

   for(i=0;i<indice;i++) free(Phrase[i]);
   indice=0;
   nb_phrase++;
   if (nb_phrase%100==0) fprintf(stderr,"\tEn cours : phrase %d\n",nb_phrase);
   }
  }

if (DevinMorpho) LibereModelMorpho();
delete_lexique(lexigraf);
gram_module_reset(num_modele,0);

exit(0); 
}
  
