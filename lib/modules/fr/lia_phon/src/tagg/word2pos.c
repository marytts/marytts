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
/*  Input: words
    Output: words+POS  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <math.h>

#include <libgram.h>
#include <quicktagg_lib.h>

#define TailleLigne	2000

#define True	1
#define False	0

void ERREUR(char *ch1, char *ch2)
{
fprintf(stderr,"ERREUR: %s %s\n",ch1,ch2);
exit(0);
}

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[TailleLigne],*lextag=NULL,*lexgraf=NULL,*pmc=NULL,*clas;
int i,j,nb_code,if_proper_name=False,if_ztrm=False;
ty_lexique lexigraf,lexiclass;
ty_pmc model_pmc;
err_t retour;
wrd_index_t *t_code,i_mot;
logprob_t *t_proba;

if ((argc<2)||(!strcmp(argv[1],"-h")))
 {
 fprintf(stderr,"Syntaxe : %s [-h] -lextag <dico tags> -lexgraf <dicograf> \
-pmc <modele pmc> [-proper_name] [-ztrm]\n",argv[0]);
 exit(0);
 }

for(i=1;i<argc;i++)
 {
 if (!strcmp(argv[i],"-lextag")) { if (i==argc-1) ERREUR("missing value after:",argv[i]); lextag=argv[++i]; } else
 if (!strcmp(argv[i],"-lexgraf")) { if (i==argc-1) ERREUR("missing value after:",argv[i]); lexgraf=argv[++i]; } else
 if (!strcmp(argv[i],"-pmc")) { if (i==argc-1) ERREUR("missing value after:",argv[i]); pmc=argv[++i]; } else
 if (!strcmp(argv[i],"-proper_name")) if_proper_name=True; else
 if (!strcmp(argv[i],"-ztrm")) if_ztrm=True;
 else { ERREUR("unknown parameter:",argv[i]); }
 }

lexigraf=ChargeLexique(lexgraf);
lexiclass=ChargeLexique(lextag);
model_pmc=charge_pmc(pmc);

t_code=(wrd_index_t *)malloc(sizeof(wrd_index_t)*model_pmc->max_nb_cate);
t_proba=(logprob_t *)malloc(sizeof(logprob_t)*model_pmc->max_nb_cate);

if (if_ztrm) printf("<s> ZTRM\n</s> ZTRM\n");
while(fgets(ch,TailleLigne,stdin))
 {
 strtok(ch," \t\n");
 if ((Mot2Code(ch,&i_mot,lexigraf)) &&
     (pmc_liste_mot_classe(t_code,t_proba,&nb_code,i_mot,model_pmc,0)==CORRECT))
  {
  for(i=0;i<nb_code;i++)
   {
   if (!Code2Mot(t_code[i],&clas,lexiclass)) ERREUR("code unknown in lexiclas","");

	   /* it's a proper name, we split it according to the possible pronunciations */
   if ((if_proper_name)&&(clas[0]=='X'))
    {
    for(j=1;j<=8;j++)
 	 {
	 if (if_ztrm) printf("<s> ZTRM\n");
	 printf("%s %s->propername_%d\n",ch,clas,j);
	 if (if_ztrm) printf("</s> ZTRM\n");
	 }
	/* we add one with a "standard" pronunciation */
	if (if_ztrm) printf("<s> ZTRM\n"); printf("%s NMS\n",ch); if (if_ztrm) printf("</s> ZTRM\n");
	}
   else { if (if_ztrm) printf("<s> ZTRM\n"); printf("%s %s\n",ch,clas); if (if_ztrm) printf("</s> ZTRM\n"); }
   }
  }
 else
  {
  if (if_ztrm) printf("<s> ZTRM\n"); printf("%s MOTINC\n",ch); if (if_ztrm) printf("</s> ZTRM\n");
  /* if it can be a proper-name, we process it */
  if ((if_proper_name)&&(ch[0]>='A')&&(ch[0]<='Z'))
   {
   for(j=1;j<=8;j++)
    {
    if (if_ztrm) printf("<s> ZTRM\n");
    printf("%s XSOC->propername_%d\n",ch,j);
    if (if_ztrm) printf("</s> ZTRM\n");
	/* we add one with a "standard" pronunciation */
	if (if_ztrm) printf("<s> ZTRM\n"); printf("%s NMS\n",ch); if (if_ztrm) printf("</s> ZTRM\n");
    }
   }
  }
 }
if (if_ztrm) printf("<s> ZTRM\n</s> ZTRM\n");
free(t_code); free(t_proba);
delete_lexique(lexigraf);
delete_lexique(lexiclass);
delete_pmc(model_pmc);
exit(0); 
}
 
