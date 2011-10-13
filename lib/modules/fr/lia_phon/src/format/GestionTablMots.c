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
/*  Permet de charger un fichier lexique compile avec 'CompileLexiTree'
    puis d'utiliser la fonction 'Present' afin d'obtenir la plus
    longue chaine compatible avec le lexique  */
/*  FRED 0199  */

#include <stdio.h>
#include <stdlib.h>

#define TailleLigne	8000

#define debug_GestionTablMots	0

typedef struct
	{
	char c,mot;
	unsigned int fg,fd;
	} type_stoktree;

type_stoktree *StockTree;

int NbNode;

#define SiIgnoreBlanc		1

/*  Chargement de l'arbre-tableau  */

void ChargeLexiqueCompile(ch)
 char *ch;
{
FILE *file;

if (!(file=fopen(ch,"rb")))
 { fprintf(stderr,"Can't open : %s\n",ch); exit(0); }

fseek(file,0,SEEK_END);
NbNode=(int)(ftell(file)/sizeof(type_stoktree));
fseek(file,0,SEEK_SET);
StockTree=(type_stoktree *)malloc(sizeof(type_stoktree)*(NbNode));
if (fread(StockTree,sizeof(type_stoktree),NbNode+1,file)!=NbNode)
 {
 fprintf(stderr,"Erreur lors de la lecture du fichier : %s\n",ch);
 exit(0);
 }
fclose(file);
}

/*  Decoupage  */

int EgalChar(c_test,c_arbre)
 char c_test,c_arbre;
{
if (SiIgnoreBlanc)
 if ((c_test==' ')&&((c_arbre=='_')/*||(c_arbre=='-')*/)) return 1;
return 0;
}

/*  'Present' permet d'obtenir la plus longue chaine compatible avec
    le lexique. Les parametres sont les suivants :
    - ch	: chaine a analyser en entree
    - addr	: addresse dans l'arbre, a initialiser a 1
    - lastword	: renvoi l'adresse du dernier caractere de la chaine 'ch'
		  qui fait partie d'un mot du lexique
    - indice	: indice du caractere en cours, a initialiser a 0
    - refe	: tableau de caractere qui va contenir le mot reference
		  le plus long, dans le lexique, a partir de la chaine 'ch'
    - lastindice: indice de la fin du mot le plus long dans refe
    Si l'ensemble de la chaine 'ch' forme un mot du lexique, alors 'Present'
    retourne la valeur 1, et 0 sinon  */

extern int SiSeparateur(char);

int Present(ch,addr,lastword,indice,refe,lastindice)
 char *ch,**lastword,*refe;
 unsigned int addr;
 int indice,*lastindice;
{ static int y;
if (addr==0) return 0;
if ((*ch==StockTree[addr].c)||(EgalChar(*ch,StockTree[addr].c)))
 {
 refe[indice]=StockTree[addr].c;
 if (debug_GestionTablMots)
  {
  for(y=0;y<=indice;y++) printf("%c",refe[y]);
  printf("\navec StockTree[addr].mot=%d ch[1]=[%c]\n",StockTree[addr].mot,ch[1]);
  }
 if ((StockTree[addr].mot)&&((ch[1]=='\0')||(SiSeparateur(ch[1]))||(SiSeparateur(ch[0]))))
	{ *lastword=ch; *lastindice=indice+1; }
 if ((ch[1]=='\0')&&(StockTree[addr].mot)) return 1;
 return Present(ch+1,StockTree[addr].fg,lastword,indice+1,refe,lastindice);
 }
return Present(ch,StockTree[addr].fd,lastword,indice,refe,lastindice);
}

int JustPresent(ch) /* version light de present : renvoi 0 ou 1 si le mot y est ou pas !! */
 char *ch;
{
static char *lastword,refe[TailleLigne];
int lastindice;

return Present(ch,1,&lastword,0,refe,&lastindice);
}
  
