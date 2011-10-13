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
/*  Gestion d'un lexique :
      * Pour la compilation :
       - en entree : un fichier lexique compose de <code> <mot>
       - en sortie : 3 fichiers dico.des dico.tab et dico.zon
	 avec : dico.des=descripteur de taille du dico
		dico.tab=table des indices des mots du dico trie par alpha
		dico.tab_graf=table des indices des mots du dico trie par code
		dico.zon=stockage des graphies
      * Pour l'utilisation :
       - en entree : 3 fichiers dico.des dico.tab et dico.zon
       - une fonction Mot2Code faisant la correspondance entre mot et code  */
/*  FRED 0498  :  Modif Multi ML - 0399  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ailleur_siroco.h>
#include <gere_lexique.h>

#define TailleLigne	400

/* Structure de codage du lexique */

typedef struct type_item_lexique_compile
	{
	wrd_index_t code;
	char *mot;
	} *ty_item_lexique_compile;


/********************************

	Defini dans gere_lexique.h

typedef struct
	{
	wrd_index_t indice,code;
	} type_item_lexique;

typedef struct type_lexique
	{
	wrd_index_t GereLexique_NbMots,TailleZone;
	type_item_lexique *TablLexique,*TablLexiqueGraf;
	char *ZoneLexique;
	} *ty_lexique;

	********************************/

/*................................................................*/

/* Constructeur d'un lexique */

ty_lexique cons_lexique(wrd_index_t nb,wrd_index_t taille)
{
ty_lexique pt;

pt=(ty_lexique)malloc(sizeof(struct type_lexique));
pt->GereLexique_NbMots=nb;
pt->TailleZone=taille;
pt->TablLexique=(type_item_lexique *)malloc(nb*sizeof(type_item_lexique));
pt->TablLexiqueGraf=(type_item_lexique *)malloc(nb*sizeof(type_item_lexique));
pt->ZoneLexique=(char *)malloc((int)taille*sizeof(char));

if ((pt==NULL)||(pt->TablLexique==NULL)||(pt->TablLexiqueGraf==NULL)||(pt->ZoneLexique==NULL))
 { fprintf(stderr,"ERROR: not enough memory in 'cons_lexique'\n"); exit(0); }

return pt;
}

void delete_lexique(ty_lexique pt)
{
if (pt)
 {
 if (pt->TablLexique) free(pt->TablLexique);
 if (pt->TablLexiqueGraf) free(pt->TablLexiqueGraf);
 if (pt->ZoneLexique) free(pt->ZoneLexique);
 free(pt);
 }
}

/*................................................................*/

/*  Creation maillons  */

ty_item_lexique_compile NewItemCompile(wrd_index_t code ,char *mot )
{
ty_item_lexique_compile pt;
pt=(ty_item_lexique_compile)malloc(sizeof(struct type_item_lexique_compile));
pt->code=code;
if (mot) pt->mot=(char*)strdup(mot); else pt->mot=NULL;
return pt;
}

void FreeItemCompile(ty_item_lexique_compile pt )
{
if (pt)
 {
 if (pt->mot) free(pt->mot);
 free(pt);
 }
}

/*................................................................*/

/*  Fonction de comparaison pour la dichotomie  */

int compar_compile(const void *a ,const void *b )
{
ty_item_lexique_compile *c,*d;
c=(ty_item_lexique_compile*)a;
d=(ty_item_lexique_compile*)b;
return strcmp((*c)->mot,(*d)->mot);
}

int compar_selon_code(const void *a ,const void *b )
{
type_item_lexique *c,*d;
c=(type_item_lexique*)a;
d=(type_item_lexique*)b;
if ((*c).code<(*d).code) return -1;
if ((*c).code>(*d).code) return 1;
return 0;
}

/*................................................................*/

/*  Compilation du lexique  */

ty_lexique CompileLexique(char *nomfic )
{
FILE *file;
char ch[TailleLigne],*ch_code,*ch_mot;
ty_item_lexique_compile *tabl_temp;
wrd_index_t code,indice,nb,taille;
int i;
ty_lexique pt_lexique;

if (!(file=fopen(nomfic,"rt")))
 {
 fprintf(stderr,"Je ne peux ouvrir : %s\n",nomfic);
 exit(0);
 }

/* On compte le nombre de mots du lexique */
for(nb=0;fgets(ch,TailleLigne,file);nb++);
fseek(file,0,SEEK_SET);

/* Remplissage du tableau intermediaire */
tabl_temp=(ty_item_lexique_compile *)malloc(nb*sizeof(ty_item_lexique_compile));
for(nb=taille=0;fgets(ch,TailleLigne,file);nb++)
 {
 ch_code=strtok(ch," \t\n");
 ch_mot=strtok(NULL," \t\n");
 sscanf(ch_code,"%ld",&code);
 tabl_temp[nb]=NewItemCompile(code,ch_mot);
 taille+=(strlen(ch_mot)+1);
 }
fclose(file);

/* On trie le tableau intermediaire */
qsort(tabl_temp,nb,sizeof(ty_item_lexique_compile),compar_compile);

/* On alloue le lexique */

pt_lexique=cons_lexique(nb,taille);

fprintf(stderr,"Taille du lexique en memoire : %.2f Ko\n",
	(float)(nb*sizeof(type_item_lexique)+taille*sizeof(char))/(float)1024);

/* On remplit les tableaux a stocker */
for(nb=indice=0;nb<pt_lexique->GereLexique_NbMots;nb++)
 {
 pt_lexique->TablLexique[nb].indice=indice;
 pt_lexique->TablLexique[nb].code=tabl_temp[nb]->code;
 for(i=0;(tabl_temp[nb]->mot[i]!='\0')&&(tabl_temp[nb]->mot[i]!='\n');i++)
  {
  if (indice>=taille) { fprintf(stderr,"La variable TailleZone est trop petite ....\n"); exit(0); }
  pt_lexique->ZoneLexique[indice++]=tabl_temp[nb]->mot[i];
  }
 pt_lexique->ZoneLexique[indice++]='\0';
 if (indice>taille) { fprintf(stderr,"La variable TailleZone est trop petite ....\n"); exit(0); }
 
 FreeItemCompile(tabl_temp[nb]);
 /*if (nb%10000==0) fprintf(stderr,"en cours : %d\n",n);*/
 }

free(tabl_temp);

/* On sauvegarde les tableaux compiles */
sprintf(ch,"%s.tab",nomfic);
if (!(file=fopen(ch,"wb")))
 { fprintf(stderr,"Je ne peux ouvrir %s\n",ch); exit(0); }
fwrite(pt_lexique->TablLexique,1,sizeof(type_item_lexique)*pt_lexique->GereLexique_NbMots,file);
fclose(file);

sprintf(ch,"%s.zon",nomfic);
if (!(file=fopen(ch,"wb")))
 { fprintf(stderr,"Je ne peux ouvrir %s\n",ch); exit(0); }
if (fwrite((const void *)pt_lexique->ZoneLexique,sizeof(char),(int)pt_lexique->TailleZone,file)!=(int)pt_lexique->TailleZone)
 { fprintf(stderr,"ERROR in writing '%s'\n",ch); exit(0); }
if (ferror(file)!=0) { fprintf(stderr,"ERROR2 in writing '%s'\n",ch); exit(0); }
fclose(file);

/* Sauvegarde du fichier descripteur */
sprintf(ch,"%s.des",nomfic);
if (!(file=fopen(ch,"wt")))
 { fprintf(stderr,"Je ne peux ouvrir %s\n",ch); exit(0); }
fprintf(file,"%ld %ld\n",pt_lexique->GereLexique_NbMots,pt_lexique->TailleZone);
fclose(file);

/*  Modif pour avoir les graphies sachant le code  */
/* on tri selon le code */

memcpy((void*)pt_lexique->TablLexiqueGraf,(void*)pt_lexique->TablLexique,
	pt_lexique->GereLexique_NbMots*sizeof(type_item_lexique));
qsort((void*)pt_lexique->TablLexiqueGraf,pt_lexique->GereLexique_NbMots,
	sizeof(type_item_lexique),compar_selon_code);
sprintf(ch,"%s.tab_graf",nomfic);
if (!(file=fopen(ch,"wb"))) { fprintf(stderr,"Je ne peux ouvrir %s\n",ch); exit(0); }
fwrite(pt_lexique->TablLexiqueGraf,sizeof(type_item_lexique),pt_lexique->GereLexique_NbMots,file);
fclose(file);

return pt_lexique;
}

/*................................................................*/

/*   Chargement du lexique formate  */

ty_lexique ChargeLexique(char *nomfic )
{
FILE *file;
char ch[TailleLigne];
ty_lexique pt_lexique;
wrd_index_t nb,taille;

sprintf(ch,"%s.des",nomfic);
if (!(file=fopen(ch,"rt")))
 { fprintf(stderr,"Je ne peux ouvrir %s\n",ch); exit(0); }
if ((fscanf(file,"%ld %ld",&nb,&taille))<2)
 { fprintf(stderr,"Probleme lors du chargement de %s\n",ch); exit(0); }
fclose(file);

pt_lexique=cons_lexique(nb,taille);

sprintf(ch,"%s.tab",nomfic);
if (!(file=fopen(ch,"rb")))
 { fprintf(stderr,"Je ne peux ouvrir %s\n",ch); exit(0); }
if (!(fread(pt_lexique->TablLexique,sizeof(type_item_lexique),pt_lexique->GereLexique_NbMots,file)))
 { fprintf(stderr,"Probleme lors du chargement de %s\n",ch); exit(0); }
fclose(file);

sprintf(ch,"%s.zon",nomfic);
if (!(file=fopen(ch,"rb")))
 { fprintf(stderr,"Je ne peux ouvrir %s\n",ch); exit(0); }
if (!(fread(pt_lexique->ZoneLexique,sizeof(char),pt_lexique->TailleZone,file)))
 { fprintf(stderr,"Probleme lors du chargement de %s\n",ch); exit(0); }
fclose(file);

/*  Modif pour avoir les graphies sachant le code  */
sprintf(ch,"%s.tab_graf",nomfic);
if (!(file=fopen(ch,"rb")))
 {
 fprintf(stderr,"Il n'y a pas de fichier %s -> pas d'acces aux graphies sachant le code\n",ch);
 free(pt_lexique->TablLexiqueGraf);
 pt_lexique->TablLexiqueGraf=NULL;
 }
else
 {
 if (!(fread(pt_lexique->TablLexiqueGraf,sizeof(type_item_lexique),pt_lexique->GereLexique_NbMots,file)))
  { fprintf(stderr,"Probleme lors du chargement de %s\n",ch); exit(0); }
 fclose(file);
 }
return pt_lexique;
}

/*................................................................*/

/*  Gestion de la dichotomie, acces au lexique  */

int DichoIterLexique(char *ch ,long d ,long f ,wrd_index_t *resu ,ty_lexique pt_lexique)
{
long m;
int c;

while(d<=f)
 {
 m=(d+f)/2;
 /*fprintf(stderr,"d=%d m=%d f=%d\n",d,m,f);*/
 c=strcmp(ch,pt_lexique->ZoneLexique+pt_lexique->TablLexique[m].indice);
 if (c==0) { *resu=m; return 1; }
 if (c<0) f=m-1; else d=m+1;
 }
return 0;
}

int Mot2Code(char *ch ,wrd_index_t *code ,ty_lexique pt_lexique)
{
wrd_index_t resu;

if (pt_lexique==NULL)
 {
 fprintf(stderr,"ERREUR : pourquoi pt_lexique est-il a NULL dans gere_lexique ????\n");
 exit(0);
 }

if (DichoIterLexique(ch,0,pt_lexique->GereLexique_NbMots-1,&resu,pt_lexique))
 { *code=pt_lexique->TablLexique[resu].code; return 1; }
return 0;
}

/*  Modif pour avoir les graphies sachant le code  */

int DichoIterLexiqueGraf(wrd_index_t code ,long d ,long f ,wrd_index_t *resu ,ty_lexique pt_lexique)
{
long m;
while(d<=f)
 {
 m=(d+f)/2;
 /*fprintf(stderr,"d=%d m=%d f=%d  avec code=%ld et TablLexiqueGraf[%ld].code=%ld\n",
        d,m,f,code,m,pt_lexique->TablLexiqueGraf[m].code);*/

 if (code<pt_lexique->TablLexiqueGraf[m].code) f=m-1;
 else
  if (code>pt_lexique->TablLexiqueGraf[m].code) d=m+1;
  else { *resu=m; return 1; }
 }
return 0;
}

int Code2Mot(wrd_index_t code, char **ch,ty_lexique pt_lexique)
{
wrd_index_t resu;

if (pt_lexique==NULL)
 {
 fprintf(stderr,"ERREUR : pourquoi pt_lexique est-il a NULL dans gere_lexique ????\n");
 exit(0);
 }

if (pt_lexique->TablLexiqueGraf==NULL)
 {
 fprintf(stderr,"Code2Mot : Desole, l'acces par le code n'a pas ete compile ....\n");
 exit(0);
 }

if (DichoIterLexiqueGraf(code,0,pt_lexique->GereLexique_NbMots-1,&resu,pt_lexique))
 { *ch=pt_lexique->ZoneLexique+pt_lexique->TablLexiqueGraf[resu].indice; return 1; }

return 0;
}
  
