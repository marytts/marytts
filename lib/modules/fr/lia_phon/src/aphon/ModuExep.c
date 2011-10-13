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
/*  Interoge une liste d'exception compilee  */

#include <stdio.h>
#include <ctype.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#define MAX_NODES  22000    	/* maximal number of nodes after compaction */
#define MAXLENGTH 256                   /* longueur maxi des mots */
#define I(a) ((unsigned) a)
#define CTOI(a) ((a<0) ? a+256 : a)

char **ListPhon;
int NbList;

typedef struct NOEUD {
    char c;
    unsigned char existe[2];
    unsigned int fils,frere;
} noeud;

noeud T[MAX_NODES];
int racine;

void ExepMain(argv1,argv2)
char *argv1,*argv2;
{
char ch[200];

    FILE *f,*fopen(),*file;
    char c;
    unsigned char c1,c2,c3;
    int i=0;

/*  Modif adapt pour exep  */

/*  Charge les exep phonetises  */

if (!(file=fopen(argv2,"rt")))
 { printf("Impossible d'ouvrir %s\n",argv2); exit(0); }

for(NbList=0;fgets(ch,200,file);NbList++);
fseek(file,0,SEEK_SET);
ListPhon=(char**)malloc(sizeof(char*)*NbList);
for(NbList=0;fgets(ch,200,file);NbList++)
 {
 ch[strlen(ch)-1]='\0';
 ListPhon[NbList]=strdup(ch);
 }

    f = fopen(argv1,"rb");
    if (f==NULL) { fprintf(stderr,"can't open:  %s\n",argv1); exit(0); }
    c=getc(f);
    while (!feof(f))
     {
     T[++i].c = c;
	/*
	Reading of the categories with the corresponding codes
	*/
	T[i].existe[0]=(unsigned char) getc(f);
	T[i].existe[1]=(unsigned char) getc(f);
	
	c1 = getc(f); c2 = getc(f);
	/*T[i].fils = CTOI(c1) + (CTOI(c2) << 8);*/
	T[i].fils = c1 + (c2 << 8);
	c1 = getc(f); c2 = getc(f);
	/*T[i].frere = CTOI(c1) + (CTOI(c2) << 8);*/
	T[i].frere = c1 + (c2 << 8);
	c3 = getc(f);
	T[i].fils += ((I(c3) & 0x0F) << 16);
	T[i].frere += ((I(c3) & 0xF0) << 12);
    c=getc(f);
    }
    racine = i;
}

char *ExepValide(s)
char *s;
{
    int i,t,code;
    char c;

    i=0; t=racine;
    /*while ((c = tolower(s[i])) != 0) {*/
    while ((c = s[i]) != 0) {
	if (T[t].fils == 0) return(NULL); /* non valide */
	t = T[t].fils;
	while (T[t].c != c) {
	    if (T[t].frere == 0) return(NULL); /* non valide */
	    else t = T[t].frere;
	}
	i++;
    }
if ((!T[t].existe[0])&&(!T[t].existe[1])) return (NULL);
/*  We print the list of categories codes  */
code=(int)(T[t].existe[0]);
code=(int)(code<<8);
code+=(int)(T[t].existe[1]);
/*printf("%s %s\n",s,ListPhon[code-1]);*/
return ListPhon[code-1];

}
  
