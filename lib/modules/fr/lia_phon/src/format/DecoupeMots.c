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
/*  Decoupe un corpus en accord avec un dico compile
    par CompileLexiTree et une table de separateur hard-codee
    Entree : stdin pour le texte et 1er parametre pour le dico
    Sortie : stdout */
/*  FRED 0199  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <GestionTablMots.h>

/*  Definition des separateurs de mots  */

char TablSeparateur[]=
	{
	' ','-','_','\'','"','*','%','+','@','>','/',	/* separateur intra/inter mots */
	'.',';',',','?','!',':',		/* separateur inter mots */
	'(',')','[',']','{','}',		/* separateur phrase */
	'\n','\0'} ;				/* fin de phrase */

int SiSeparateur(c)
 char c;
{
static int n;
if (c=='\0') return 1;
for(n=0;TablSeparateur[n];n++) if (c==TablSeparateur[n]) return 1;
return 0;
}

#define TailleLigne	8000

void ProcessLine(ch)
 char *ch;
{
char refe[TailleLigne],*lastword;
int lastindice,siblanc=0;

while ((*ch)&&(*ch!='\n'))
 {
 if (*ch!=' ')
  {
  lastword=NULL;
  Present(ch,1,&lastword,0,refe,&lastindice);
  if (lastword)
   {
   refe[lastindice]='\0';
   printf("%s ",refe);
   ch=lastword+1;
   siblanc=1;
   }
  else
   {
   /* Politique : on coupe les mots d'un qu'on trouve un separateur
      sauf si le separateur est inclu a un mot du dico */
   siblanc=0;
   while (!SiSeparateur(*ch)) printf("%c",*ch++);
   if (*ch!=' ') printf(" %c ",*ch++);
   }
  }
 else
  {
  while (*ch==' ') ch++;
  if (siblanc==0) printf(" ");
  }
 }
if (*ch=='\n') printf("\n");
}

/*  Prog Principal  */

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[TailleLigne];

if (argc<2)
 {
 fprintf(stderr,"Syntaxe : %s [-h] <lexi compile>\n",argv[0]);
 exit(0);
 }

if (!strcmp(argv[1],"-h"))
 {
 fprintf(stderr,"Syntaxe : %s [-h] <lexi compile>\n\
 \t Decoupe un corpus en accord avec un dico compile\n\
 \t par CompileLexiTree et une table de separateur hard-codee\n\
 \t dans le tableau 'TablSeparateur'. La politique de segmentation\n\
 \t est la suivante : on concatene les mots suivant la plus grande\n\
 \t expression composee trouvee dans le dico. Lorsqu'un\n\
 \t mot contient un caractere separateur, s'il fait partie du\n\
 \t dico, on le laisse tel-quel, sinon on l'eclate autour\n\
 \t du caractere separateur.\n\
 \t Entree : stdin pour le texte et 1er parametre pour le dico\n\
 \t Sortie : stdout\n",argv[0]);
 exit(0);
 }

/*fprintf(stderr,"Chargement de l'arbre dans le tableau -> ");*/
ChargeLexiqueCompile(argv[1]);
/*fprintf(stderr,"Termine\n");*/
/*fprintf(stderr,"On a lu : %d noeuds\n",NbNode);*/
while(fgets(ch,TailleLigne,stdin)) ProcessLine(ch);

exit(0); 
}
 
