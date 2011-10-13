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
/*  Gere l'arbre des tri-lettres  */

#include <stdio.h>
#include <stdlib.h>
#include <GereTreeMorpho.h>

int NbNoeuds;
int NbTri;

/*  Allocations memoire  */

void *my_malloc(size)
 unsigned long size;
{
void *pt;

pt=(void *)malloc(size);
if (pt==NULL)
 {
 printf("my_malloc : Allocation de memoire impossible ....\n");
 exit(0);
 }
return pt;
}

ty_tri NewTriNoeud(tri,nb,fg,fd)
 char *tri;
 int nb;
 ty_tri fg,fd;
{
ty_tri pt;

pt=(ty_tri) my_malloc(sizeof(struct type_tri)); 
pt->tri[0]=tri[0]; pt->tri[1]=tri[1]; pt->tri[2]=tri[2];
pt->nb=nb;
pt->fg=fg;
pt->fd=fd;
NbNoeuds++;
return pt;
}

void LibereArbre(pt)
 ty_tri pt;
{
ty_tri fg,fd;

if (pt==NULL) return;
fg=pt->fg;
fd=pt->fd;
free(pt);
LibereArbre(fg);
LibereArbre(fd);
}

ty_tri AjouteTrilettre(racine,tri,nb)
 ty_tri racine;
 char *tri;
 int nb;
{
ty_tri pt,ptbak;
int test;

NbTri+=nb;
if (racine==NULL)
 return NewTriNoeud(tri,nb,NULL,NULL);
for(pt=racine,ptbak=NULL;pt;)
 {
 ptbak=pt;
 test=strncmp(tri,pt->tri,3);
 if (test<0) pt=pt->fg;
 else
  if (test>0) pt=pt->fd;
   else
    { pt->nb+=nb; return racine; }
 }
if (strncmp(tri,ptbak->tri,3)<0)
 ptbak->fg=NewTriNoeud(tri,nb,NULL,NULL);
else
 ptbak->fd=NewTriNoeud(tri,nb,NULL,NULL);
return racine;
}

ty_tri AjouteBilettre(racine,tri,nb)
 ty_tri racine;
 char *tri;
 int nb;
{
ty_tri pt,ptbak;
int test;
char ch[4];

ch[0]=tri[0];ch[1]=tri[1];ch[2]='@';
NbTri+=nb;
if (racine==NULL)
 return NewTriNoeud(ch,nb,NULL,NULL);
for(pt=racine,ptbak=NULL;pt;)
 {
 ptbak=pt;
 test=strncmp(ch,pt->tri,3);
 if (test<0) pt=pt->fg;
 else
  if (test>0) pt=pt->fd;
   else
    { pt->nb+=nb; return racine; }
 }
if (strncmp(ch,ptbak->tri,3)<0)
 ptbak->fg=NewTriNoeud(ch,nb,NULL,NULL);
else
 ptbak->fd=NewTriNoeud(ch,nb,NULL,NULL);
return racine;
}

ty_tri AjouteUnlettre(racine,tri,nb)
 ty_tri racine;
 char *tri;
 int nb;
{
ty_tri pt,ptbak;
int test;
char ch[4];

ch[0]=tri[0];ch[1]=ch[2]='@';
NbTri+=nb;
if (racine==NULL)
 return NewTriNoeud(ch,nb,NULL,NULL);
for(pt=racine,ptbak=NULL;pt;)
 {
 ptbak=pt;
 test=strncmp(ch,pt->tri,3);
 if (test<0) pt=pt->fg;
 else
  if (test>0) pt=pt->fd;
   else
    { pt->nb+=nb; return racine; }
 }
if (strncmp(ch,ptbak->tri,3)<0)
 ptbak->fg=NewTriNoeud(ch,nb,NULL,NULL);
else
 ptbak->fd=NewTriNoeud(ch,nb,NULL,NULL);
return racine;
}

void AfficheArbre(pt,file)
 ty_tri pt;
 FILE *file;
{
if (pt==NULL) return;
fprintf(file,"%c%c%c %d\n",pt->tri[0],pt->tri[1],pt->tri[2],pt->nb);
AfficheArbre(pt->fg,file);
AfficheArbre(pt->fd,file);
}

void AfficheArbreSansUn(pt,file,nb)
 ty_tri pt;
 FILE *file;
 int nb;
{
if (pt==NULL) return;
if (pt->nb>nb) fprintf(file,"%c%c%c %d\n",pt->tri[0],pt->tri[1],pt->tri[2],pt->nb);
AfficheArbreSansUn(pt->fg,file,nb);
AfficheArbreSansUn(pt->fd,file,nb);
}

ty_tri TraiteChaine(racine,chaine)
 ty_tri racine;
 char *chaine;
{
char *ch;
int n,size;

ch=(char *) my_malloc(sizeof(char)*(strlen(chaine)+5));
sprintf(ch,"%s##",chaine);
size=strlen(ch);
if (size>MAGIC_N) n=size-MAGIC_N; else n=0;
for(;n<=(size-3);n++)
 {
 racine=AjouteTrilettre(racine,ch+n,1);
 /*
 printf("Ajout du tri-lettre : %c%c%c\nArbre Resultat :\n",ch[n],ch[n+1],ch[n+2]);
 AfficheArbre(racine,stdout);
 */
 }
free(ch);
return racine;
}
 
ty_tri TraiteChaineBi(racine,chaine)
 ty_tri racine;
 char *chaine;
{
char *ch;
int n,size;

ch=(char *) my_malloc(sizeof(char)*(strlen(chaine)+5));
sprintf(ch,"%s##",chaine);
size=strlen(ch);
if (size>MAGIC_N) n=size-MAGIC_N; else n=0;
for(;n<=(size-2);n++) racine=AjouteBilettre(racine,ch+n,1);
free(ch);
return racine;
}
 
ty_tri TraiteChaineUn(racine,chaine)
 ty_tri racine;
 char *chaine;
{
char *ch;
int n,size;

ch=(char *) my_malloc(sizeof(char)*(strlen(chaine)+5));
sprintf(ch,"%s##",chaine);
size=strlen(ch);
if (size>MAGIC_N) n=size-MAGIC_N; else n=0;
for(;n<=(size-2);n++) racine=AjouteUnlettre(racine,ch+n,1);
free(ch);
return racine;
}
 
double ScoreTri(ch,tree)
 char *ch;
 ty_tri tree;
{
int test;
while (tree)
 {
 test=strncmp(ch,tree->tri,3);
 if (test<0) tree=tree->fg;
 else
  if (test>0) tree=tree->fd;
  else
   return (double)tree->nb;
 }
/*return 0.001;*/

return 0.;
}

void TrouveInfo(tree,taille,nbtri,nb)
 ty_tri tree;
 int *taille,*nbtri,nb;
{
if (tree==NULL) return;
if (tree->nb>nb)
 {
 (*taille)++;
 (*nbtri)+=tree->nb;
 }
TrouveInfo(tree->fg,taille,nbtri,nb);
TrouveInfo(tree->fd,taille,nbtri,nb);
}
 

