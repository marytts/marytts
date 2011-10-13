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
/*  Gestion des probas des mots sachant les classes :
      * Pour la compilation :
       - en entree : 2 fichiers lexiques (format sirocco)
			- un pour les mots
			- un pour les categories
                     1 fichier texte au format suivant :
			mot\tcate\tnb\n
       - en sortie : 3 fichiers .pmc_des .pmc_tab .pmc_zon
	 avec : .pmc_des=descripteur de taille du modele
		.pmc_tab=table des indices des mots du dico trie par alpha
		.pmc_zon=stockage des probas des mots sachant la classe
      * Pour l'utilisation :
       - en entree : 3 fichiers .pmc_des .pmc_tab .pmc_zon
       - une fonction p_mot_classe donnant la proba du mot sachant la classe  */
/*  FRED 0399  */  /*  Modif ajout des lemmes - FRED 0699  */

#include <stdio.h>
#include <stdlib.h>
#include <libgram.h>

/*................................................................*/

#define TailleLigne	400

#define SiMajuscule(n)	((((n)>='A')&&((n)<='Z'))?1:0)

/*................................................................*/

/* Structure de codage du modele */

/* Arbre des categories avec leur compte :
   * en general pour avoir le nombre total
   * en particulier pour chaque mot */

typedef signed long	compte_t;

typedef struct type_tree_cate_pmc
	{
	wrd_index_t c_cate,c_lemm;
	compte_t nb;
	struct type_tree_cate_pmc *fg,*fd;
	} *ty_tree_cate_pmc;

/* Arbre des mots avec le nb de categorie et l'arbre
   des categories attachees au mot */

typedef struct type_tree_mot_pmc
	{
	signed char dq;
	wrd_index_t c_mot;
	compte_t nb_cate;
	ty_tree_cate_pmc t_cate;
	struct type_tree_mot_pmc *fg,*fd;
	} *ty_tree_mot_pmc;

/* Zone de stockage des categorie avec leur proba en log */

/********** Definie dans le .h

typedef struct type_zon_pmc
	{
	wrd_index_t c_cate;
	float pmc;
	} type_zon_pmc;

typedef struct type_zon_lemm_pmc
	{
	wrd_index_t c_cate,c_lemm;
	float pmc;
	} type_zon_lemm_pmc;

 Tableau des mots avec pointeur sur leur liste de categorie+proba 

typedef struct type_tab_pmc
	{
	wrd_index_t c_mot;
	unsigned int nb_cate;
	long i_cate;
	} type_tab_pmc;

 Type general pmc permettant de stocker plusieurs modeles 

typedef struct type_pmc
	{
	wrd_index_t nb_tab,nb_zon;
	int si_log_e,si_lemme,max_nb_cate;
	type_zon_lemm_pmc *p_zon_lemm;
	type_zon_pmc *p_zon;
	type_tab_pmc *p_tab;
	} *ty_pmc;

**********/

/*................................................................*/

/*  Constructeur, Destructeur, et autres babioles des types ty_tree....  */

/* Tout d'abord quelques rotations .... */

void tree_mot_echange_info(ty_tree_mot_pmc n1,ty_tree_mot_pmc n2)
{
char dq;
wrd_index_t c_mot;
compte_t nb_cate;
ty_tree_cate_pmc t_cate;

dq=n1->dq;
c_mot=n1->c_mot;
nb_cate=n1->nb_cate;
t_cate=n1->t_cate;

n1->dq=n2->dq;
n1->c_mot=n2->c_mot;
n1->nb_cate=n2->nb_cate;
n1->t_cate=n2->t_cate;

n2->dq=dq;
n2->c_mot=c_mot;
n2->nb_cate=nb_cate;
n2->t_cate=t_cate;
}

ty_tree_mot_pmc tree_mot_rotation_droite(ty_tree_mot_pmc pt)
{
ty_tree_mot_pmc tmpfgfd,tmpfd;

if (pt->fg==NULL)
 {
 fprintf(stderr,"ERROR : le fg ne peut etre NULL !!!!\n");
 exit(0);
 }

/*  On echange *this et fg  */
tree_mot_echange_info(pt,pt->fg);

tmpfgfd=pt->fg->fd;
tmpfd=pt->fd;

pt->fd=pt->fg;
pt->fg=pt->fg->fg;
pt->fd->fg=tmpfgfd;
pt->fd->fd=tmpfd;

return pt;
}

ty_tree_mot_pmc tree_mot_rotation_gauche(ty_tree_mot_pmc pt)
{
ty_tree_mot_pmc tmpfdfg,tmpfg;

if (pt->fd==NULL)
 {
 fprintf(stderr,"ERROR : le fd ne peut etre NULL !!!!\n");
 exit(0);
 }

/*  On echange *this et fd  */
tree_mot_echange_info(pt,pt->fd);

tmpfdfg=pt->fd->fg;
tmpfg=pt->fg;

pt->fg=pt->fd;
pt->fd=pt->fd->fd;
pt->fg->fd=tmpfdfg;
pt->fg->fg=tmpfg;

return pt;
}

ty_tree_mot_pmc tree_mot_rotation_gauche_droite(ty_tree_mot_pmc pt)
{
pt->fg=tree_mot_rotation_gauche(pt->fg);
pt=tree_mot_rotation_droite(pt);
return pt;
}

ty_tree_mot_pmc tree_mot_rotation_droite_gauche(ty_tree_mot_pmc pt)
{
pt->fd=tree_mot_rotation_droite(pt->fd);
pt=tree_mot_rotation_gauche(pt);
return pt;
}

/* et maintenant les allocations memoires */

ty_tree_cate_pmc new_tree_cate_pmc(wrd_index_t c_cate,wrd_index_t c_lemm,compte_t nb)
{
ty_tree_cate_pmc pt;
pt=(ty_tree_cate_pmc)malloc(sizeof(struct type_tree_cate_pmc));
pt->c_cate=c_cate;
pt->c_lemm=c_lemm;
pt->nb=nb;
pt->fg=pt->fd=NULL;
return pt;
}

ty_tree_cate_pmc ajoute_tree_cate_pmc(wrd_index_t c_cate,wrd_index_t c_lemm,
	compte_t nb,ty_tree_cate_pmc racine,int *si_nouveau)
{
if (racine==NULL) { *si_nouveau=1; return new_tree_cate_pmc(c_cate,c_lemm,nb); }
if (racine->c_cate==c_cate) { *si_nouveau=0; racine->nb+=nb; }
else
 if (racine->c_cate>c_cate) racine->fg=ajoute_tree_cate_pmc(c_cate,c_lemm,nb,racine->fg,si_nouveau);
 else                       racine->fd=ajoute_tree_cate_pmc(c_cate,c_lemm,nb,racine->fd,si_nouveau);
return racine;
}

void delete_tree_cate_pmc(ty_tree_cate_pmc racine)
{
if (racine)
 {
 delete_tree_cate_pmc(racine->fg);
 delete_tree_cate_pmc(racine->fd);
 free(racine);
 }
}

int recherche_tree_cate_pmc(wrd_index_t c_cate,compte_t *nb,ty_tree_cate_pmc racine)
{
if (racine==NULL) return 0;
if (racine->c_cate==c_cate) { *nb=racine->nb; return 1; }
if (racine->c_cate>c_cate) return recherche_tree_cate_pmc(c_cate,nb,racine->fg);
else                       return recherche_tree_cate_pmc(c_cate,nb,racine->fd);
}

ty_tree_mot_pmc new_tree_mot_pmc(wrd_index_t c_mot,wrd_index_t c_cate,wrd_index_t c_lemm,compte_t nb)
{
int si_nouveau;
ty_tree_mot_pmc pt;
pt=(ty_tree_mot_pmc)malloc(sizeof(struct type_tree_mot_pmc));
pt->dq=0;
pt->c_mot=c_mot;
pt->nb_cate=1;
pt->t_cate=ajoute_tree_cate_pmc(c_cate,c_lemm,nb,NULL,&si_nouveau);
pt->fg=pt->fd=NULL;
return pt;
}

ty_tree_mot_pmc ajoute_tree_mot_pmc(wrd_index_t c_mot,wrd_index_t c_cate,wrd_index_t c_lemm,
	compte_t nb, ty_tree_mot_pmc racine, char *si_augm)
{
int si_nouveau;
static char r;
/*
if (c_mot==25) printf("TEST: c_mot=%d racine==%s \n",c_mot,racine==NULL?"NULL":"PASNULL");
*/
if (racine==NULL) { *si_augm=1; return new_tree_mot_pmc(c_mot,c_cate,c_lemm,nb); }

if (racine->c_mot==c_mot)
 {
 /*if (c_mot==25) printf("TEST: c_mot=%d racine->c_mot=%d : nb_cate=%d \n",c_mot,racine->c_mot,racine->nb_cate);*/

 *si_augm=0;
 racine->t_cate=ajoute_tree_cate_pmc(c_cate,c_lemm,nb,racine->t_cate,&si_nouveau);
 if (si_nouveau) racine->nb_cate++;

 /*if (c_mot==25) printf("TEST:                apres ajout: nb_cate=%d \n",racine->nb_cate);*/
 }
else
 if (racine->c_mot>c_mot)
  { /* sur le fils gauche */
  racine->fg=ajoute_tree_mot_pmc(c_mot,c_cate,c_lemm,nb,racine->fg,si_augm);
  if ((*si_augm==0)||(racine->dq<=-1)) r=0; else r=1;
  racine->dq+=(*si_augm);
  *si_augm=r;

  /* eventuelle rotation */
  if (racine->dq==2)
   {
   if (racine->fg->dq==1)
    {
    racine=tree_mot_rotation_droite(racine);
    racine->dq=racine->fd->dq=0;
    }
   else
    {
    racine=tree_mot_rotation_gauche_droite(racine);
    switch (racine->dq)
     {
     case  1 : racine->fg->dq=0; racine->fd->dq=-1; break;
     case -1 : racine->fg->dq=1; racine->fd->dq= 0; break;
     case  0 : racine->fg->dq=racine->fd->dq=0; break;
     }
    racine->dq=0;
    }
   *si_augm=0;
   }
  }
 else
  { /* sur le fils droit */
  racine->fd=ajoute_tree_mot_pmc(c_mot,c_cate,c_lemm,nb,racine->fd,si_augm);
  if ((*si_augm==0)||(racine->dq>=1)) r=0; else r=1;
  racine->dq-=(*si_augm);
  *si_augm=r;

  /* eventuelle rotation */
  if (racine->dq==-2)
   {
   if (racine->fd->dq==-1)
    {
    racine=tree_mot_rotation_gauche(racine);
    racine->dq=racine->fg->dq=0;
    }
   else
    {
    racine=tree_mot_rotation_droite_gauche(racine);
    switch (racine->dq)
     {
     case  1 : racine->fd->dq=-1; racine->fg->dq= 0; break;
     case -1 : racine->fd->dq= 0; racine->fg->dq= 1; break;
     case  0 : racine->fg->dq=racine->fd->dq=0; break;
     }
    racine->dq=0;
    }
   *si_augm=0;
   }
  }
return racine;
}

void delete_tree_mot_pmc(ty_tree_mot_pmc racine)
{
if (racine)
 {
 delete_tree_mot_pmc(racine->fg);
 delete_tree_mot_pmc(racine->fd);
 delete_tree_cate_pmc(racine->t_cate);
 free(racine);
 }
}

int recherche_tree_mot_pmc(wrd_index_t c_mot,compte_t *nb_cate,ty_tree_cate_pmc *t_cate,ty_tree_mot_pmc racine)
{
if (racine==NULL) return 0;
if (racine->c_mot==c_mot) { *nb_cate=racine->nb_cate; *t_cate=racine->t_cate; return 1; }
if (racine->c_mot>c_mot) return recherche_tree_mot_pmc(c_mot,nb_cate,t_cate,racine->fg);
else                     return recherche_tree_mot_pmc(c_mot,nb_cate,t_cate,racine->fd);
}

/*................................................................*/

/* Constructeur d'un modele pmc */

ty_pmc cons_pmc(wrd_index_t nb_tab,wrd_index_t nb_zon,int si_log_e,int si_lemme,int max_nb_cate)
{
ty_pmc pt;

pt=(ty_pmc)malloc(sizeof(struct type_pmc));
if (pt==NULL) return NULL;

pt->nb_tab=nb_tab;
pt->nb_zon=nb_zon;
pt->max_nb_cate=max_nb_cate;
pt->si_log_e=si_log_e;
pt->si_lemme=si_lemme;

pt->p_tab=(type_tab_pmc *)malloc(nb_tab*sizeof(type_tab_pmc));
if (si_lemme)
 {
 pt->p_zon_lemm=(type_zon_lemm_pmc *)malloc(nb_zon*sizeof(type_zon_lemm_pmc));
 pt->p_zon=NULL;
 }
else
 {
 pt->p_zon=(type_zon_pmc *)malloc(nb_zon*sizeof(type_zon_pmc));
 pt->p_zon_lemm=NULL;
 }
if ((pt->p_tab==NULL)||((si_lemme)&&(pt->p_zon_lemm==NULL))||((!si_lemme)&&(pt->p_zon==NULL)))
 return NULL;

return pt;
}

void delete_pmc(pt)
 ty_pmc pt;
{
if (pt)
 {
 if (pt->p_tab) free(pt->p_tab);
 if (pt->p_zon) free(pt->p_zon);
 if (pt->p_zon_lemm) free(pt->p_zon_lemm);
 free(pt);
 }
}

/*................................................................*/

/* Acces dichotomique sur le tableau p_tab */

int acces_dicho(wrd_index_t code ,wrd_index_t d ,wrd_index_t f ,wrd_index_t *resu , type_tab_pmc *p_tab)
{
static wrd_index_t m;
while(d<=f)
 {
 m=(d+f)/2;
 if (code<p_tab[m].c_mot) f=m-1;
 else
  if (code>p_tab[m].c_mot) d=m+1;
  else { *resu=m; return 1; }
 }
return 0;
}

void range_zone_pmc(wrd_index_t c_mot,ty_tree_cate_pmc t_cate,ty_tree_cate_pmc p_cate,
	type_zon_pmc *p_zon,type_zon_lemm_pmc *p_zon_lemm,
	long *indice_zone,compte_t nb_cate,int si_log_e,int si_lemme)
{
static compte_t nb_total;

/*if (c_mot==25) printf("TEST: c_mot=%d nb_cate=%d t_cate==%s\n",c_mot,nb_cate,t_cate==NULL?"NULL":"PASNULL");*/

if (t_cate)
 {
 if (nb_cate<=0) /* XXXXXX : <= ou < ?????? */
  {
  fprintf(stderr,"STRANGE : pourquoi l'arbre t_cate a plus de noeuds que nb_cate ???? c_mot=%d nb_cate=%d\n",
	c_mot,nb_cate);
  exit(0);
  }
 if (t_cate->fg) range_zone_pmc(c_mot,t_cate->fg,p_cate,p_zon,p_zon_lemm,indice_zone,--nb_cate,si_log_e,si_lemme);
 if (t_cate->fd) range_zone_pmc(c_mot,t_cate->fd,p_cate,p_zon,p_zon_lemm,indice_zone,--nb_cate,si_log_e,si_lemme);

 if (si_lemme)
  {
  p_zon_lemm[*indice_zone].c_cate=t_cate->c_cate;
  p_zon_lemm[*indice_zone].c_lemm=t_cate->c_lemm;
  }
 else
  p_zon[*indice_zone].c_cate=t_cate->c_cate;

 if (!recherche_tree_cate_pmc(t_cate->c_cate,&nb_total,p_cate))
  {
  fprintf(stderr,"STRANGE : pourquoi la categorie %ld est-elle inconue de l'arbre ????\n",t_cate->c_cate);
  exit(0);
  }
/* 
 if (c_mot==25)
  {
  printf("\nTEST : c_mot=%ld - cate=%ld - lemme=%ld - nb=%ld - nbtotal=%ld  **  indice_zone=%ld\n",
	c_mot,t_cate->c_cate,t_cate->c_lemm,t_cate->nb,nb_total,*indice_zone);
  }
*/ 
 if (si_log_e)
  if (si_lemme)
   p_zon_lemm[*indice_zone].pmc=(float)log((double)t_cate->nb/(double)nb_total);
  else
   p_zon[*indice_zone].pmc=(float)log((double)t_cate->nb/(double)nb_total);
 else
  if (si_lemme)
   p_zon_lemm[*indice_zone].pmc=(float)log10((double)t_cate->nb/(double)nb_total);
  else
   p_zon[*indice_zone].pmc=(float)log10((double)t_cate->nb/(double)nb_total);
 /*
 if (c_mot==262721) printf("TEST\tproba=%f\n",p_zon_lemm[*indice_zone].pmc);
 */
 (*indice_zone)++;
 }
}

void range_tableau_pmc(ty_tree_mot_pmc p_mot,ty_tree_cate_pmc p_cate,
	ty_pmc pt_pmc,long *indice_zone,int *max_nb_cate)
{
static wrd_index_t indice;
if (p_mot)
 {
 if (p_mot->nb_cate>(*max_nb_cate)) *max_nb_cate=(int)p_mot->nb_cate;

 range_tableau_pmc(p_mot->fg,p_cate,pt_pmc,indice_zone,max_nb_cate);
 range_tableau_pmc(p_mot->fd,p_cate,pt_pmc,indice_zone,max_nb_cate);

 /* maintenant on travaille la */
 /* acces au code mot par dichotomie sur le tableau p_tab pour trouver sa place */
 if (!acces_dicho(p_mot->c_mot,0,pt_pmc->nb_tab,&indice,pt_pmc->p_tab))
  {
  fprintf(stderr,"ERREUR : et pourquoi ce code est inconnu du tabelau p_tab ???? code=%ld\n",p_mot->c_mot);
  exit(0);
  }
 pt_pmc->p_tab[indice].nb_cate=(unsigned int)p_mot->nb_cate;
 pt_pmc->p_tab[indice].i_cate=*indice_zone;

 /*if (p_mot->c_mot==25) printf("TEST : juste avant 'range_zone_pmc', nb cate=%d\n",p_mot->nb_cate);*/
 range_zone_pmc(p_mot->c_mot,p_mot->t_cate,p_cate,pt_pmc->p_zon,pt_pmc->p_zon_lemm,indice_zone,p_mot->nb_cate,
	pt_pmc->si_log_e,pt_pmc->si_lemme);
 if (*indice_zone>pt_pmc->nb_zon)
  {
  fprintf(stderr,"ERROR : indice_zone=%ld depasse la taille de nb_zon=%ld !!!!\n",*indice_zone,pt_pmc->nb_zon);
  exit(0);
  }
 }
}

/*................................................................*/

/* Construit l'arbre du modele a partir du fichier texte et compile
   le modele pmc en le stockant dans les tableaux */

ty_pmc compile_modele_pmc(char *nomfic,ty_lexique pt_lexi_mot,ty_lexique pt_lexi_cate,int si_log_e,int si_lemme)
{
FILE *file;
ty_tree_cate_pmc pt_tree_cate;
ty_tree_mot_pmc pt_tree_mot;
compte_t nb_couple;
char ch[TailleLigne],*mot,*cate,*ch_compte,*lemm,si_augm;
compte_t compte;
int si_nouveau,in_lexi,max_nb_cate;
long indice_zone;
wrd_index_t c_mot,c_cate,c_lemm,i;
ty_pmc pt_pmc;

if (!(file=fopen(nomfic,"rt")))
 {
 fprintf(stderr,"Can't open : %s\n",nomfic);
 exit(0);
 }

/* Construction de l'arbre */

pt_tree_cate=NULL;
pt_tree_mot=NULL;

fprintf(stderr,"Compilation des arbres classe et mot\n");

for(nb_couple=0;fgets(ch,TailleLigne,file);)
 {
 /* format : mot cate compte */
 mot=(char*)strtok(ch," \t\n");
 cate=(char*)strtok(NULL," \t\n");
 ch_compte=(char*)strtok(NULL," \t\n");
 sscanf(ch_compte,"%ld",&compte);
 if (si_lemme) lemm=(char*)strtok(NULL," \t\n");
 /*
 if (!strcmp(mot,"y"))
  printf("TEST : lecture -> %s %s %s %s\n",mot,cate,lemm,ch_compte);
 */
 if (!(Mot2Code(cate,&c_cate,pt_lexi_cate)))
  {
  fprintf(stderr,"ATTENTION : c'est quoi cette categorie [%s] !!!!\n",cate);
  exit(0);
  }

 /* on ne prends que les mots qui font partie du lexique de mots */
 in_lexi=Mot2Code(mot,&c_mot,pt_lexi_mot);
 if ((!in_lexi)&&(SiMajuscule(mot[0]))&&(!SiMajuscule(mot[1])))
  { /* on laisse une chance aux mots qui sont dans le lexique sans la majuscule de debut */
  mot[0]+=('a'-'A');
  in_lexi=Mot2Code(mot,&c_mot,pt_lexi_mot);
  }
 if (in_lexi)
  {
  if (si_lemme)
   {
   /* on cherche le code du lemme , s'il n'y est pas, on prend le mot lui meme */
   if (!Mot2Code(lemm,&c_lemm,pt_lexi_mot)) c_lemm=c_mot;
   }
  else c_lemm=0;

  nb_couple++;
/*  
  if (c_mot==25)
   {
   printf("TEST : %s=%ld %s=%ld %s=%ld nb=%ld\n",mot,c_mot,cate,c_cate,lemm,c_lemm,compte);
   }
*/  
  pt_tree_mot=ajoute_tree_mot_pmc(c_mot,c_cate,c_lemm,compte,pt_tree_mot,&si_augm);

  if ((nb_couple%50000)==0) fprintf(stderr,"\ten cours : %d\n",nb_couple);
  }
 /* on ajoute ds le compte total de chaque categorie meme les comptes des mots inconnus */
 pt_tree_cate=ajoute_tree_cate_pmc(c_cate,0,compte,pt_tree_cate,&si_nouveau);
 }
fclose(file);

fprintf(stderr,"Termine\n -> on a lu %d couples\n",nb_couple);

fprintf(stderr,"Allocation du modele PMC -> ");

/* Allocation du modele pmc */
pt_pmc=cons_pmc(pt_lexi_mot->GereLexique_NbMots,nb_couple,si_log_e,si_lemme,0);
if (pt_pmc==NULL) { fprintf(stderr,"desole, pas assez de memoire\n"); exit(0); }

fprintf(stderr,"Termine\n");

fprintf(stderr,"Taille necessaire au modele : %.2f Ko\n",
	(float)((sizeof(type_tab_pmc)*pt_lexi_mot->GereLexique_NbMots)+
		(sizeof(type_zon_pmc)*nb_couple))/(float)1024);

/* Initialisation :
	on initialise le tableau pt_pmc->p_tab avec les codes des mots
	ranges dans le tableau TablLexiqueGraf du lexique mot. En effet
	ce dernier est tri dans l'ordre des codes, pour la dichotomie,
	et cela permet d'initialiser toutes les cases du tableau p_tab
	avec 0 dans le champs nb_cate pour permettre de traiter sans
	probleme les mots du lexique qui n'ont pas ete vu dans le fichier
	texte d'apprentissage du modele pmc. Il suffira lors de l'utilisation
	du modele de tester cette variable, et s'il est egal a 0 de renvoyer
	une proba bien basse, mes amis */

fprintf(stderr,"Initialisation du modele -> ");

for(i=0;i<pt_pmc->nb_tab;i++)
 {
 pt_pmc->p_tab[i].c_mot=pt_lexi_mot->TablLexiqueGraf[i].code;
 pt_pmc->p_tab[i].i_cate=0;
 pt_pmc->p_tab[i].nb_cate=0;
 }

fprintf(stderr,"Termine\n");

/* Maintenant on parcours l'arbre des mots pour les ranger dans les 2 tableaux */
indice_zone=0;

fprintf(stderr,"Calcul et stockage des probas dans le modele -> ");

max_nb_cate=0;
range_tableau_pmc(pt_tree_mot,pt_tree_cate,pt_pmc,&indice_zone,&max_nb_cate);
pt_pmc->max_nb_cate=max_nb_cate;

fprintf(stderr,"Termine\n");

fprintf(stderr,"On a range %ld couples mot/cate (%ld dans le fichier)\n",indice_zone,pt_pmc->nb_zon);
/* on reactualise 'pt_pmc->nb_zon' pour enlever les doublons */
pt_pmc->nb_zon=indice_zone;

delete_tree_mot_pmc(pt_tree_mot);
delete_tree_cate_pmc(pt_tree_cate);

return pt_pmc;
}

/*................................................................*/

/* Sauvegarde du modele PMC */

void sauve_pmc(char *nomfic,ty_pmc pt_pmc)
{
char ch[TailleLigne];
FILE *file;
wrd_index_t nb;

sprintf(ch,"%s.pmc_des",nomfic);
if (!(file=fopen(ch,"wt")))
 { fprintf(stderr,"Can't open : %s\n",ch); exit(0); }
fprintf(file,"%ld %ld %d\n",pt_pmc->nb_tab,pt_pmc->nb_zon,pt_pmc->max_nb_cate);
if (pt_pmc->si_log_e) fprintf(file,"LOG_E\n");
else                  fprintf(file,"LOG_10\n");
if (pt_pmc->si_lemme) fprintf(file,"LEMME\n");
fclose(file);

sprintf(ch,"%s.pmc_tab",nomfic);
if (!(file=fopen(ch,"wb")))
 { fprintf(stderr,"Can't open : %s\n",ch); exit(0); }
if ((nb=fwrite((void*)pt_pmc->p_tab,sizeof(type_tab_pmc),pt_pmc->nb_tab,file))!=pt_pmc->nb_tab)
 { fprintf(stderr,"Erreur d'ecriture : %s (%ld items au lieu de %ld)\n",ch,nb,pt_pmc->nb_tab); exit(0); }
fclose(file);

sprintf(ch,"%s.pmc_zon",nomfic);
if (!(file=fopen(ch,"wb")))
 { fprintf(stderr,"Can't open : %s\n",ch); exit(0); }
if (pt_pmc->p_zon_lemm!=NULL)
 {
 if ((nb=fwrite((void*)pt_pmc->p_zon_lemm,sizeof(type_zon_lemm_pmc),pt_pmc->nb_zon,file))!=pt_pmc->nb_zon)
  { fprintf(stderr,"Erreur d'ecriture : %s (%ld items au lieu de %ld)\n",ch,nb,pt_pmc->nb_zon); exit(0); }
 }
else
 {
 if ((nb=fwrite((void*)pt_pmc->p_zon,sizeof(type_zon_pmc),pt_pmc->nb_zon,file))!=pt_pmc->nb_zon)
  { fprintf(stderr,"Erreur d'ecriture : %s (%ld items au lieu de %ld)\n",ch,nb,pt_pmc->nb_zon); exit(0); }
 }
fclose(file);
}

/*................................................................*/

/* Chargement du modele PMC */

ty_pmc charge_pmc(char *nomfic )
{
FILE *file;
char ch[TailleLigne];
ty_pmc pt_pmc;
wrd_index_t nb_tab,nb_zon,nb;
int si_log_e,si_lemme,max_nb_cate;

sprintf(ch,"%s.pmc_des",nomfic);
if (!(file=fopen(ch,"rt")))
 { fprintf(stderr,"Can't open : %s\n",ch); exit(0); }
fgets(ch,TailleLigne,file);
sscanf(ch,"%ld %ld %d",&nb_tab,&nb_zon,&max_nb_cate);
fgets(ch,TailleLigne,file);
if (!strncmp(ch,"LOG_10",6)) si_log_e=0; else si_log_e=1;
if ((fgets(ch,TailleLigne,file))&&(!strncmp(ch,"LEMME",5))) si_lemme=1; else si_lemme=0;
fclose(file);

pt_pmc=cons_pmc(nb_tab,nb_zon,si_log_e,si_lemme,max_nb_cate);
if (pt_pmc==NULL) { fprintf(stderr,"desole, pas assez de memoire\n"); exit(0); }

sprintf(ch,"%s.pmc_tab",nomfic);
if (!(file=fopen(ch,"rb")))
 { fprintf(stderr,"Can't open : %s\n",ch); exit(0); }
if ((nb=fread((void*)pt_pmc->p_tab,sizeof(type_tab_pmc),pt_pmc->nb_tab,file))!=pt_pmc->nb_tab)
 { fprintf(stderr,"Erreur de lecture : %s (%ld items au lieu de %ld)\n",ch,nb,pt_pmc->nb_tab); exit(0); }
fclose(file);

sprintf(ch,"%s.pmc_zon",nomfic);
if (!(file=fopen(ch,"rb")))
 { fprintf(stderr,"Can't open : %s\n",ch); exit(0); }
if (si_lemme)
 {
 if ((nb=fread((void*)pt_pmc->p_zon_lemm,sizeof(type_zon_lemm_pmc),pt_pmc->nb_zon,file))!=pt_pmc->nb_zon)
  { fprintf(stderr,"Erreur de lecture : %s (%ld items au lieu de %ld)\n",ch,nb,pt_pmc->nb_zon); exit(0); }
 }
else
 {
 if ((nb=fread((void*)pt_pmc->p_zon,sizeof(type_zon_pmc),pt_pmc->nb_zon,file))!=pt_pmc->nb_zon)
  { fprintf(stderr,"Erreur de lecture : %s (%ld items au lieu de %ld)\n",ch,nb,pt_pmc->nb_zon); exit(0); }
 }
fclose(file);

return pt_pmc;
}

/*................................................................*/

/* fonction qui renvoi la proba du mot sachant la classe ainsi que le lemme
   associer au couple mot/cate */

err_t pmc_proba_mot_classe(logprob_t *proba,wrd_index_t *c_lemm,
	const wrd_index_t mot, const wrd_index_t classe,
	ty_pmc pt_pmc, const err_t trace)
{
static wrd_index_t indice,i;

if (!acces_dicho(mot,0,pt_pmc->nb_tab,&indice,pt_pmc->p_tab))
 {
 if (trace & SIR_GRAM_TRACE)
  fprintf(stderr,"PMC : Je ne connais pas le mot : %ld\n",mot);
 *proba=-99.00;
 return GRAM_CODE_UNKNOWN;
 }

if (pt_pmc->p_tab[indice].nb_cate==0)
 {
 if (trace & SIR_GRAM_TRACE)
  fprintf(stderr,"PMC : Le mot : %ld n'a pas ete vu lors de l'apprentissage\n",mot);
 *proba=-99.00;
 return GRAM_CODE_UNKNOWN;
 }

if (pt_pmc->si_lemme) /* il y a des lemmes */
 {
 for(i=0;(i<pt_pmc->p_tab[indice].nb_cate)&&(pt_pmc->p_zon_lemm[pt_pmc->p_tab[indice].i_cate+i].c_cate!=classe);i++);
 /*
  printf("i=%ld - nb_cate=%d - c_cate=%ld classe=%ld\n",
	i,pt_pmc->p_tab[indice].nb_cate,pt_pmc->p_zon_lemm[pt_pmc->p_tab[indice].i_cate+i].c_cate,classe);
 */
 }
else
 {
 for(i=0;(i<pt_pmc->p_tab[indice].nb_cate)&&(pt_pmc->p_zon[pt_pmc->p_tab[indice].i_cate+i].c_cate!=classe);i++);
 }

if (i==pt_pmc->p_tab[indice].nb_cate)
 {
 if (trace & SIR_GRAM_TRACE)
  fprintf(stderr,"PMC : Le mot : %ld n'a pas ete vu avec la classe %ld lors de l'apprentissage\n",mot,classe);
 *proba=-99.00;
 return GRAM_CODE_UNKNOWN;
 }

if (pt_pmc->si_lemme) /* il y a des lemmes */
 {
 *proba=(logprob_t)(pt_pmc->p_zon_lemm[pt_pmc->p_tab[indice].i_cate+i].pmc);
 *c_lemm=pt_pmc->p_zon_lemm[pt_pmc->p_tab[indice].i_cate+i].c_lemm;
 }
else
 {
 *proba=(logprob_t)(pt_pmc->p_zon[pt_pmc->p_tab[indice].i_cate+i].pmc);
 *c_lemm=0;
 }
if (trace & SIR_GRAM_TRACE)
 fprintf(stderr,"PMC : P(%ld|%ld)=%lf\n",mot,classe,*proba);

return CORRECT;
}

/*................................................................*/

/* fonction qui renvoi la liste des classes possibles pour un mot
   avec les proba (MODIF 1101) */

err_t pmc_liste_mot_classe(wrd_index_t *tabl,logprob_t *tpro,int *nb,
	const wrd_index_t mot, ty_pmc pt_pmc, const err_t trace)
{
static wrd_index_t indice,i;

if (!acces_dicho(mot,0,pt_pmc->nb_tab,&indice,pt_pmc->p_tab))
 {
 if (trace & SIR_GRAM_TRACE)
  fprintf(stderr,"PMC : Je ne connais pas le mot : %ld\n",mot);
 *nb=0;
 return GRAM_CODE_UNKNOWN;
 }

if (pt_pmc->p_tab[indice].nb_cate==0)
 {
 if (trace & SIR_GRAM_TRACE)
  fprintf(stderr,"PMC : Le mot : %ld n'a pas ete vu lors de l'apprentissage\n",mot);
 *nb=0;
 return GRAM_CODE_UNKNOWN;
 }
*nb=0;

if (pt_pmc->si_lemme) /* il y a des lemmes */
 {
 for(i=0;i<pt_pmc->p_tab[indice].nb_cate;i++)
  {
  tabl[*nb]=pt_pmc->p_zon_lemm[pt_pmc->p_tab[indice].i_cate+i].c_cate;
  tpro[*nb]=(logprob_t)pt_pmc->p_zon_lemm[pt_pmc->p_tab[indice].i_cate+i].pmc;
  (*nb)++;
  }
 }
else
 {
 for(i=0;i<pt_pmc->p_tab[indice].nb_cate;i++)
  {
  tabl[*nb]=pt_pmc->p_zon[pt_pmc->p_tab[indice].i_cate+i].c_cate;
  tpro[*nb]=(logprob_t)pt_pmc->p_zon[pt_pmc->p_tab[indice].i_cate+i].pmc;
  (*nb)++;
  }
 }
return CORRECT;
}
  
