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
/*  Renvoi la proba du modele 3classe a partir des simples graphies
    Simple modif de QuickTagg  */
/*  FRED 0299 pour QuickTagg puis 11/99 pour ca  */
/*  MODIF FRED 1101 : suppression de DicoBase, remplacement par
    modele_pmc (de libgram) ENFIN !!
    + suppression de la cste MaxChoix (dynamique determinee par le lexique union)  */
/*  FRED 1101  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <math.h>
#include <libgram.h>
#include <module_morpho.h>
#include <quicktagg_lib.h>

int DevinMorpho=0;

#define viter_debug	0

#define TailleLigne	400

#define SiMajuscule(a)  ((((a)>='A')&&((a)<='Z'))?1:0)

#define PROBA_MIN	-100000.0 /* !!!! */

/* Stockage de la phrase  */
#define MaxMots		2000

/*  TAGGING  */

/* Nb maxi de choix de categorie pour un mot = nb classe ouverte */
int MaxChoix; /* fixe dans le modele PMC dans le champs 'max_nb_cate' */

/*  Stockage de la phrase avec les proba mot/classe  */

typedef struct
	{
	float pb_clas;
	short i_clas;
	} type_choix;

typedef struct type_phrase
	{
	int indice_best;
	type_choix *choix;
	} *ty_phrase;

ty_phrase new_phrase(int taille)
{
ty_phrase pt;
int i;
pt=(ty_phrase)malloc(sizeof(struct type_phrase)*taille);
for(i=0;i<taille;i++) pt[i].choix=(type_choix*)malloc(sizeof(type_choix)*(MaxChoix));
return pt;
}

void delete_phrase(ty_phrase pt,int taille)
{
if (pt)
 {
 int i;
 for(i=0;i<taille;i++) if (pt[i].choix) free(pt[i].choix);
 free(pt);
 }
}

/*  Stockage des chemins pour Viterbi  */

typedef struct
	{
	int ic1,ic2,pred;
	float pchemin;
	} type_viter;

typedef struct type_grafvite
	{
	type_viter *viter;
	} *ty_grafvite;

ty_grafvite new_grafvite(int taille)
{
ty_grafvite pt;
int i;
pt=(ty_grafvite)malloc(sizeof(struct type_grafvite)*taille);
for(i=0;i<taille;i++) pt[i].viter=(type_viter*)malloc(sizeof(type_viter)*(MaxChoix)*(MaxChoix));
return pt;
}

void delete_grafvite(ty_grafvite pt,int taille)
{
if (pt)
 {
 int i;
 for(i=0;i<taille;i++) if (pt[i].viter) free(pt[i].viter);
 free(pt);
 }
}

void AfficheSolution(indice,ph,file_out,phrase,num_modele)
 int indice;
 ty_phrase ph;
 FILE *file_out;
 char **phrase;
 int num_modele;
{
int n,i_clas;
char *chclass;

for(n=0;n<indice;n++)
 {
 fprintf(file_out,"%s ",phrase[n]);
 i_clas=ph[n].choix[ph[n].indice_best].i_clas;
 if (Code2Mot((wrd_index_t)i_clas,&chclass,LEXIQUE(num_modele)))
  fprintf(file_out,"%s\n",chclass);
 else
  { fprintf(stderr,"ATTENTION1 : le code %d du mot [%s] n'a aucune graphie !!\n",i_clas,phrase[n]); exit(0); }
 }
}

void AffichePhrase(indice,ph,phrase,file_out,num_modele)
 int indice;
 ty_phrase ph;
 char **phrase;
 FILE *file_out;
 int num_modele;
{
int n,i;
char *chclass;

for(n=0;n<indice;n++)
 {
 fprintf(file_out,"%s\n",phrase[n]);
 for(i=0;i<MaxChoix;i++)
  if (ph[n].choix[i].i_clas>=0)
   {
   if (Code2Mot((wrd_index_t)ph[n].choix[i].i_clas,&chclass,LEXIQUE(num_modele)))
    fprintf(file_out,"\t%s\t%f\n",chclass,ph[n].choix[i].pb_clas);
   else
    {
    fprintf(stderr,"ATTENTION2 : le code %d ne correspond a aucune graphie de classe !!\n",
        ph[n].choix[i].i_clas);
    exit(0);
    }
   }
 }
}

void AfficheGrafvite(indice,ph,phrase,graf,file_out,num_modele)
 int indice;
 ty_phrase ph;
 char **phrase;
 ty_grafvite graf;
 FILE *file_out;
 int num_modele;
{
int n,i;
char *chclass;
short cate1,cate2;

/* Ca c'est l'initialisation, bon */
fprintf(file_out,"%s\n",phrase[0]);
fprintf(file_out,"\tZTRM\tZTRM\t0.0\n");

for(n=1;n<indice;n++)
 {
 fprintf(file_out,"%s\n",phrase[n]);
 for(i=0;i<MaxChoix*MaxChoix;i++)
  if (graf[n].viter[i].ic1>=0)
   {
   cate1=ph[n-1].choix[graf[n].viter[i].ic1].i_clas;
   cate2=ph[n].choix[graf[n].viter[i].ic2].i_clas;
   if (Code2Mot((wrd_index_t)cate1,&chclass,LEXIQUE(num_modele)))
    fprintf(file_out,"\t%s\t",chclass);
   else
    {
    fprintf(stderr,"ATTENTION3 : le code %d n'a aucune graphie !!\n",cate1);
    exit(0);
    }
   if (Code2Mot((wrd_index_t)cate2,&chclass,LEXIQUE(num_modele)))
    fprintf(file_out,"%s\t%f\n",chclass,graf[n].viter[i].pchemin);
   else
    {
    fprintf(stderr,"ATTENTION4 : le code %d n'a aucune graphie !!\n",cate2);
    exit(0);
    }
   }
 }
}

void RempliChoix(
 char *mot,
 type_choix *choix,
 int num_modele,
 int *nb_in,
 int *nb_out,
 ty_lexique lexigraf,
 char **tablch,
 double *tablscore,
 wrd_index_t *t_code,
 logprob_t *t_proba
 )
{
int i,nb_code;
wrd_index_t code,i_mot;
short cate;
float score;

for (i=0;i<MaxChoix;i++)
 {
 tablch[i]=NULL;
 choix[i].i_clas=-1;
 }

/* Si c'est <s> ou </s> c'est ZTRM */
if ((!strcmp(mot,"<s>"))||(!strcmp(mot,"</s>")))
 if (Mot2Code("ZTRM",&code,LEXIQUE(num_modele)))
  {
  choix[0].i_clas=(short)code;
  choix[0].pb_clas=0.0;
  return;
  }
 else
  {
  fprintf(stderr,"ERREUR : je ne connais aucun code correspondant a ZTRM\n");
  exit(0);
  }

/* Si le mot appartient au vocabulaire ferme, on le laisse tel
   quel avec une proba de 1 */
if ((Mot2Code(mot,&i_mot,lexigraf)) &&
    (pmc_liste_mot_classe(t_code,t_proba,&nb_code,i_mot,MODELE_PMC(num_modele),0)==CORRECT))
 {
 (*nb_in)++;
 for(i=0;i<nb_code;i++)
  {
  choix[i].i_clas=(short)t_code[i];
  choix[i].pb_clas=(float)t_proba[i];
  }
 return;
 }
(*nb_out)++;

/* Sinon, s'il y a une majuscule alors MOTINC 
   (on verra plus tard pour la distinction X....) */
if ((DevinMorpho==0)||(SiMajuscule(mot[0]))) /* ON APPELLE PAS LE MODULE MORPHO */
 if ((Mot2Code("MOTINC",&code,LEXIQUE(num_modele)))||(Mot2Code("<UNK>",&code,LEXIQUE(num_modele))))
  {
  choix[0].i_clas=(short)code;
  choix[0].pb_clas=0.0;
  return;
  }
 else
  {
  fprintf(stderr,"ERREUR : je ne connais aucun code correspondant a MOTINC ou <UNK>\n");
  exit(0);
  }

/* Sinon on appelle le devin morphologique */
MorphoProbaClass(mot,tablch,tablscore,MaxChoix);

for (i=0,cate=0;i<MaxChoix;i++)
 if (tablch[i])
  {
  score=(float)log10(tablscore[i]);
  if (score>-10)
   if (Mot2Code(tablch[i],&code,LEXIQUE(num_modele)))
    {
    choix[i].i_clas=(short)code;
    choix[i].pb_clas=score;
    cate=1;
    }
   else
    {
    fprintf(stderr,"ERREUR : je ne connais aucun code correspondant a %s\n",tablch[i]);
    exit(0);
    }
  }

if (cate==0) /* Pas de cate determinee, c'est un mot inconnu !! */
if ((Mot2Code("MOTINC",&code,LEXIQUE(num_modele)))||(Mot2Code("<UNK>",&code,LEXIQUE(num_modele))))
 {
 choix[0].i_clas=(short)code;
 choix[0].pb_clas=0.0;
 }
else
 {
 fprintf(stderr,"ERREUR : je ne connais aucun code correspondant a MOTINC ou <UNK>\n");
 exit(0);
 }
}

void RempliGrafvite( int indice, ty_phrase ph, ty_grafvite gv)
{
int n,i,j,k;

/* Initialisation de la premiere colonne avec ZTRM */
gv[0].viter[0].ic1=gv[0].viter[0].ic2=0;
gv[0].viter[0].pchemin=0.; gv[0].viter[0].pred=0;
for(k=1;k<MaxChoix*MaxChoix;k++) gv[0].viter[k].ic1=gv[0].viter[k].ic2=-1;

for(n=1;n<indice;n++)
 {
 for(k=0;k<MaxChoix*MaxChoix;k++) gv[n].viter[k].ic1=gv[n].viter[k].ic2=-1;
 k=0;
 for (i=0;i<MaxChoix;i++)
  if (ph[n-1].choix[i].i_clas>=0)
   for (j=0;j<MaxChoix;j++)
    if (ph[n].choix[j].i_clas>=0)
     {
     if (k>=MaxChoix*MaxChoix)
      {
      fprintf(stderr,"ERREUR : depassement dans le remplissage de viterbi !!!!\n");
      exit(0);
      }
     gv[n].viter[k].ic1=i;
     gv[n].viter[k].ic2=j;
     gv[n].viter[k].pchemin=0.;
     gv[n].viter[k].pred=0;
     k++;
     }
 }
}

float Viterbi( int indice, ty_phrase ph, ty_grafvite gv, int num_modele)
{
int n,i,j,best_pred;
short cate1,cate2,cate3;
float best_pb,proba,pb_clas;
char *chclass;
err_t retour,trace=0;
logprob_t pdouble;

for (n=1;n<indice;n++)
 {

 if (viter_debug) printf("\nPour l'indice n=%d\n\n",n);

 for(i=0;i<MaxChoix*MaxChoix;i++)
  if (gv[n].viter[i].ic1>=0)
   {
   cate2=ph[n-1].choix[gv[n].viter[i].ic1].i_clas;
   cate3=ph[n].choix[gv[n].viter[i].ic2].i_clas;
   pb_clas=ph[n].choix[gv[n].viter[i].ic2].pb_clas;
   best_pb=PROBA_MIN;
   for(j=0;j<MaxChoix*MaxChoix;j++)
    if ((gv[n-1].viter[j].ic1>=0)&&(cate2==ph[n-1].choix[gv[n-1].viter[j].ic2].i_clas))
     { /* on a trouve 2 bigram telque : M1M2 M2M3 */
     if (n==1) cate1=ph[n-1].choix[gv[n-1].viter[j].ic1].i_clas;
     else      cate1=ph[n-2].choix[gv[n-1].viter[j].ic1].i_clas;

     retour=gram_proba_to_trigram(&pdouble,cate1,cate2,cate3,num_modele,trace);
     proba=(float)pdouble;
     proba+=(pb_clas+gv[n-1].viter[j].pchemin);

     if (retour!=CORRECT)
      {
      fprintf(stderr,"ERREUR : gram_proba_to_trigram retour=%d (c1=%d c2=%d c3=%d)\n",
	retour,cate1,cate2,cate3);
      exit(0);
      }

     if (best_pb<proba) { best_pb=proba; best_pred=j; }    

     if (viter_debug)
      {
      if (Code2Mot((wrd_index_t)cate1,&chclass,LEXIQUE(num_modele)))
       printf("%s",chclass);
      else { fprintf(stderr,"ATTENTION5 : le code %d n'a aucune graphie !!\n",cate1); exit(0); }
      if (Code2Mot((wrd_index_t)cate2,&chclass,LEXIQUE(num_modele)))
       printf("\t%s",chclass);
      else { fprintf(stderr,"ATTENTION6 : le code %d n'a aucune graphie !!\n",cate2); exit(0); }
      if (Code2Mot((wrd_index_t)cate3,&chclass,LEXIQUE(num_modele)))
       printf("\t%s = %f\n",chclass,proba);
      else { fprintf(stderr,"ATTENTION7 : le code %d n'a aucune graphie !!\n",cate3); exit(0); }
      }

     }
   if (viter_debug) printf("BestProba=%f\n",best_pb);

   gv[n].viter[i].pchemin=best_pb;
   gv[n].viter[i].pred=best_pred;
   }
 }

/* Sortie de la meilleure solution */
for (best_pb=PROBA_MIN,best_pred=0,i=0;i<MaxChoix*MaxChoix;i++)
 if ((gv[indice-1].viter[i].ic1>=0)&&(gv[indice-1].viter[i].pchemin>best_pb))
  { best_pb=gv[indice-1].viter[i].pchemin; best_pred=i; }

if (viter_debug) printf("Meilleure solution : cout=%f\n",best_pb);
for (n=indice-1;n>0;n--)
 {
 ph[n].indice_best=gv[n].viter[best_pred].ic2;
 if (viter_debug)
  {
  cate3=ph[n].choix[gv[n].viter[best_pred].ic2].i_clas;
  if (Code2Mot((wrd_index_t)cate3,&chclass,LEXIQUE(num_modele))) printf("\t%s",chclass);
  else { fprintf(stderr,"ATTENTION8 : le code %d n'a aucune graphie !!\n",cate3); exit(0); }
  }
 best_pred=gv[n].viter[best_pred].pred;
 }
ph[0].indice_best=0;
if (viter_debug) printf("\n");
return best_pb;
}

float tagg_phrase(
 int indice,
 char **phrase,
 int num_modele,
 FILE *si_affi,
 int *nb_in,
 int *nb_out,
 ty_lexique lexigraf)
{
int n;
ty_phrase ph;
ty_grafvite grafvite;
float pb;
static char **tablch=NULL;
static double *tablscore=NULL;
static wrd_index_t *t_code=NULL;
static logprob_t *t_proba=NULL;

/* allocation memoire */
ph=new_phrase(indice);
grafvite=new_grafvite(indice);
if (tablch==NULL)
 {
 tablch=(char **)malloc(sizeof(char *)*MaxChoix);
 tablscore=(double*)malloc(sizeof(double)*MaxChoix);
 t_code=(wrd_index_t *)malloc(sizeof(wrd_index_t)*MaxChoix);
 t_proba=(logprob_t *)malloc(sizeof(logprob_t)*MaxChoix);
 }

for(n=0;n<indice;n++) RempliChoix(phrase[n],ph[n].choix,num_modele,nb_in,nb_out,lexigraf,
			tablch,tablscore,t_code,t_proba);

RempliGrafvite(indice,ph,grafvite);

if (viter_debug)
 {
 AffichePhrase(indice,ph,phrase,stdout,num_modele);
 AfficheGrafvite(indice,ph,phrase,grafvite,stdout,num_modele);
 }

pb=Viterbi(indice,ph,grafvite,num_modele);

if (si_affi) AfficheSolution(indice,ph,si_affi,phrase,num_modele);

delete_phrase(ph,indice);
delete_grafvite(grafvite,indice);

return pb;
}

void init_quicktagg(
 char *dico_class,
 char *model_morpho,
 char *model3g,
 char *modelPMC,
 int *num_modele,
 char *nom_lexigraf,
 ty_lexique *lexigraf)
{
err_t retour;

retour=gram_module_init(model3g,num_modele,3,0);
if (retour!=CORRECT)
 { fprintf(stderr,"ERREUR chargement 3 gram retour=%d\n",retour); exit(0); }
LEXIQUE((*num_modele))=ChargeLexique(dico_class);
MODELE_PMC((*num_modele))=charge_pmc(modelPMC);
MaxChoix=MODELE_PMC((*num_modele))->max_nb_cate+1;
(*lexigraf)=ChargeLexique(nom_lexigraf);
if (!strcmp(model_morpho,"NULL")) DevinMorpho=0;
else { DevinMorpho=1; ChargeModelMorpho(model_morpho); }
}
  
