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
/*  Codage du lexique_union sous forme d'arbre en 
    partie commune pour le tokenizer  */
/*  FRED 0199  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#define TailleLigne	8000

/* Structure de codage de l'arbre */

typedef struct type_tree
	{
	char c,mot;
	unsigned int addr;
	struct type_tree *fg,*fd;
	} *ty_tree;

typedef struct
	{
	char c,mot;
	unsigned int fg,fd;
	} type_stoktree;

ty_tree Arbre;

type_stoktree *StockTree;

int NbNode;

/* Gestion des noeuds */

ty_tree NewNode(c,mot)
 char c,mot;
{
ty_tree pt;
pt=(ty_tree)malloc(sizeof(struct type_tree));
pt->c=c;
pt->mot=mot;
pt->fg=pt->fd=NULL;
NbNode++; /* Le noeud 0 est reserve au test de case NULL */
pt->addr=(unsigned int)NbNode;
return pt;
}

/* Recopie de l'arbre dans le tableau */

void Arbre2Tableau(racine)
 ty_tree racine;
{
if (racine==NULL) return;
if (racine->addr>NbNode)
 { fprintf(stderr,"Depassement : racine->addr=%d\n",racine->addr); exit(0); }
if ((racine->fg)&&(racine->fg->addr>NbNode))
 { fprintf(stderr,"Depassement : racine->fg->addr=%d\n",racine->fg->addr); exit(0); }
if ((racine->fd)&&(racine->fd->addr>NbNode))
 { fprintf(stderr,"Depassement : racine->fd->addr=%d\n",racine->fd->addr); exit(0); }

StockTree[racine->addr].c=racine->c;
StockTree[racine->addr].mot=racine->mot;
if (racine->fg) StockTree[racine->addr].fg=racine->fg->addr;
else StockTree[racine->addr].fg=0;
if (racine->fd) StockTree[racine->addr].fd=racine->fd->addr;
else StockTree[racine->addr].fd=0;
Arbre2Tableau(racine->fg);
Arbre2Tableau(racine->fd);
}

/* Cree l'arbre lexical */

ty_tree Compile(ch,racine,pere)
 char *ch;
 ty_tree racine,pere;
{
if (*ch=='\0')
 {
 if (pere==NULL) { fprintf(stderr,"C'est quoi ce Oaie ....\n"); exit(0); }
 pere->mot=1;
 return racine;
 }
if (racine==NULL) racine=NewNode(*ch,0);
if (*ch==racine->c)
 {
 racine->fg=Compile(ch+1,racine->fg,racine);
 return racine;
 }
racine->fd=Compile(ch,racine->fd,racine);
return racine;
}

/* Test la presence d'un mot */

int Present(ch,addr)
 char *ch;
 unsigned int addr;
{
if (addr==0) return 0;
if (*ch==StockTree[addr].c)
 {
 if ((ch[1]=='\0')&&(StockTree[addr].mot)) return 1;
 return Present(ch+1,StockTree[addr].fg);
 }
return Present(ch,StockTree[addr].fd);
}

/* Test coherence du dico compile */

void TestCoherence(racine,ch,indice)
 ty_tree racine;
 char *ch;
 int indice;
{
int n;
if (racine==NULL) return;
ch[indice]=racine->c;
if (racine->mot)
 {
 for(n=0;n<=indice;n++) printf("%c",ch[n]);
 printf("\n");
 }
TestCoherence(racine->fg,ch,indice+1);
TestCoherence(racine->fd,ch,indice);
}

void ProfMax(racine,prof,profmax)
 ty_tree racine;
 int prof,*profmax;
{
if (racine==NULL)
 {
 if (prof>*profmax) *profmax=prof;
 return;
 }
ProfMax(racine->fg,prof+1,profmax);
ProfMax(racine->fd,prof+1,profmax);
}

/*  Prog Principal  */

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[TailleLigne],*pt;
int nb,prof;
FILE *file;

if (argc<2)
 {
 fprintf(stderr,"Syntaxe : %s [-h] <lexique_union> <lexi compile>\n",argv[0]);
 exit(0);
 }

if (!strcmp(argv[1],"-h"))
 {
 fprintf(stderr,"Syntaxe : %s [-h] <lexique_union> <lexi compile>\n\
 \t Ce programme permet de compiler une liste de mots sous forme d'arbre afin\n\
 \t d'etre utilise par les programmes 'lia_token' , 'lia_nett_capital'\n\
 \t et 'lia_nomb2alpha'. Le fichier d'entree doit contenir un mot par ligne, la\n\
 \t graphie devant se trouver dans le premier champs (les autres champs, s'ils\n\
 \t existent sont ignores).\n",argv[0]);
 exit(0);
 }

if (!(file=fopen(argv[1],"r")))
 {
 fprintf(stderr,"Can't open : %s\n",argv[1]);
 exit(0);
 }

Arbre=NULL; NbNode=0;

fprintf(stderr,"Lecture du lexique et construction de l'arbre\n");

for(nb=1;fgets(ch,TailleLigne,file);nb++)
 {
 pt=strtok(ch," \n");
 Arbre=Compile(pt,Arbre,NULL);
 if (nb%50000==0) fprintf(stderr,"En cours : %d\n",nb);
 }
fclose(file);

prof=0;
ProfMax(Arbre,0,&prof);
fprintf(stderr,"La profondeur maximum de l'arbre est : %d\n",prof);
fprintf(stderr,"Nombre de noeuds necessaires au stockage : %d (taille=%.2fKo)\n",
	(NbNode+1),(float)(sizeof(struct type_tree)*(NbNode+1))/(float)1024);
fprintf(stderr,"Taille de l'arbre stocke dans le tableau : %.2fKo\n",
	(float)(sizeof(type_stoktree)*(NbNode+1))/(float)1024); /* Node+1 car on commence a 1 */

fprintf(stderr,"Chargement de l'arbre dans le tableau -> ");
StockTree=(type_stoktree *)malloc(sizeof(type_stoktree)*(NbNode+1));
StockTree[0].c=StockTree[0].mot=(char)0;
StockTree[0].fg=StockTree[0].fd=0;
Arbre2Tableau(Arbre);
fprintf(stderr,"Termine\n");

fprintf(stderr,"Sauvegarde du tableau lexique -> ");
sprintf(ch,"%s",argv[2]);
if (!(file=fopen(ch,"wb")))
 {
 fprintf(stderr,"Can't write in %s\n",ch);
 exit(0);
 }
if (fwrite(StockTree,sizeof(type_stoktree),NbNode+1,file)!=NbNode+1)
 {
 fprintf(stderr,"Erreur lors de l'ecriture du fichier : %s\n",ch);
 exit(0);
 }

fclose(file);
fprintf(stderr,"Termine\n");

exit(0); 
}
  
