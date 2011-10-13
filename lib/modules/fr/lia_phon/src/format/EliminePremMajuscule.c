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
/*  Elimine la premiere majuscule des debuts de phrases avec la regle suivante :
      - si le mot avec la majuscule existe dans le lexique, on le garde
      - sinon, si le mot avec la prem lettre en minuscule existe dans le lexique
	on le remplace par le mot du lexique
      - sinon, on garde le mot inconnu
    Entree : stdin pour le texte et 1er parametre pour le dico
    Sortie : stdout
    ATTENTION : les phrases fournies en entree DOIVENT etre structures en paragraphe,
		c'est a dire avec la phrase (ou une partie de la phrase) sur une meme
		ligne et en AUCUN CAS avec un mot par ligne !!!! */
/*  FRED 0199  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

/*  Definition des separateurs de mots  */

char TablSeparateur[]=
	{
	' ','-','_','\'','"','*',	/* separateur intra/inter mots */
	'.',';',',','?','!',':',	/* separateur inter mots */
	'(',')','[',']','{','}',	/* separateur phrase */
	'\n','\0'} ;			/* fin de phrase */

int SiSeparateur(c)
 char c;
{
static int n;
if (c=='\0') return 1;
for(n=0;TablSeparateur[n];n++) if (c==TablSeparateur[n]) return 1;
return 0;
}

#define SiMajuscule(a)  ((((a)>='A')&&((a)<='Z'))?1:0)

#define TailleLigne	8000

/* Structure de codage de l'arbre dans un tableau */

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

int EgalChar(ch_test,c_arbre)
 char *ch_test,c_arbre;
{
static int n;

/*printf("EgalChar : %s == %c\n",ch_test,c_arbre);*/

if ((*ch_test==' ')&&(ch_test[1]==c_arbre)&&(SiSeparateur(c_arbre)))
 {
 /* on bouffe l'espace, peuchere , que si la caractere d'apres est un separateur
    ex : C ' est = c' est */
 for (n=0;ch_test[n];n++) ch_test[n]=ch_test[n+1];
 return 1;
 }

if (SiIgnoreBlanc)
 if ((*ch_test==' ')&&(c_arbre=='_')) return 1;
return 0;
}

int Present(ch,addr,lastword,indice,refe,lastindice)
 char *ch,**lastword,*refe;
 unsigned int addr;
 int indice,*lastindice;
{
if (addr==0) return 0;
if ((*ch==StockTree[addr].c)||(EgalChar(ch,StockTree[addr].c)))
 {
 refe[indice]=StockTree[addr].c;
 /*for(y=0;y<=indice;y++) printf("%c",refe[y]); printf("\n");*/
 if ((StockTree[addr].mot)&&((SiSeparateur(ch[1]))||(SiSeparateur(ch[0]))))
	{ *lastword=ch; *lastindice=indice+1; }
 if ((ch[1]=='\0')&&(StockTree[addr].mot)) return 1;
 return Present(ch+1,StockTree[addr].fg,lastword,indice+1,refe,lastindice);
 }
return Present(ch,StockTree[addr].fd,lastword,indice,refe,lastindice);
}

void ProcessLine(ch)
 char *ch;
{
/* on traite a part le cas : [A partir du ....]
   c'est a dire que tous les 'A' de debut de phrase sont
   systematiquement changes en 'a2' et hop !!!! */

if ((ch[0]=='A')&&(ch[1]==' ')) printf("à %s",ch+2);
else
if (SiMajuscule(ch[0]))
 {
 static char sauv_ch1[TailleLigne],sauv_ch2[TailleLigne],
	     refe1[TailleLigne],refe2[TailleLigne],*lastword1,*lastword2;
 static int lastindice1,lastindice2;

 lastword1=lastword2=NULL;

 strcpy(sauv_ch1,ch);
 Present(sauv_ch1,1,&lastword1,0,refe1,&lastindice1);
 if (lastword1) refe1[lastindice1]='\0';
 strcpy(sauv_ch2,ch);
 sauv_ch2[0]+=('a'-'A');
 Present(sauv_ch2,1,&lastword2,0,refe2,&lastindice2);
 if (lastword2) refe2[lastindice2]='\0';
 /*
 if (lastword1) printf("Avec maju : [%s] [%s]\n",refe1,lastword1+1); else printf("Avec maju : NULL\n");
 if (lastword2) printf("Sans maju : [%s] [%s]\n",refe2,lastword2+1); else printf("Sans maju : NULL\n");
 */
 if (lastword1)
  if ((lastword2)&&(lastindice2>lastindice1))
   if (lastword2[1]==' ') printf("%s%s",refe2,lastword2+1);
   else	printf("%s %s",refe2,lastword2+1);
  else
   if (lastword1[1]==' ') printf("%s%s",refe1,lastword1+1);
   else printf("%s %s",refe1,lastword1+1);
 else
  if (lastword2)
   if (lastword2[1]==' ') printf("%s%s",refe2,lastword2+1);
   else printf("%s %s",refe2,lastword2+1);
  else
   printf("%s",ch);
 }
else printf("%s",ch);
}

/*  Prog Principal  */

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[TailleLigne];
int n;

if (argc<2)
 {
 fprintf(stderr,"Syntaxe : %s [-h] <lexi compile>\n",argv[0]);
 exit(0);
 }

if (!strcmp(argv[1],"-h"))
 {
 fprintf(stderr,"Syntaxe : %s [-h] <lexi compile>\n\
 \t Elimine la premiere majuscule des debuts de phrases\n\
 \t avec la regle suivante :\n\
 \t - si le mot avec la majuscule existe dans le lexique,\n\
 \t   on le garde\n\
 \t - sinon, si le mot avec la prem lettre en minuscule existe\n\
 \t   dans le lexique on le remplace par le mot du lexique\n\
 \t - sinon, on garde le mot inconnu\n\
 \t Le dico passe en parametre est le meme que dans la\n\
 \t commande 'lia_token' et doit avoir ete compile avec\n\
 \t la commande 'lia_compile_lexitree'\n\
 \t Entree : stdin pour le texte et 1er parametre pour le dico\n\
 \t Sortie : stdout\n\n\
 \t ATTENTION 1 : le texte a traiter DOIT avoir au prealable ete\n\
 \t               decoupe en phrase grace aux marqueurs :\n\
 \t               '<s>'=debut de phrase , '</s>'=fin de phrase.\n\
 \t               On peut utiliser la commande : 'lia_sentence'\n\
 \t ATTENTION 2 : les phrases fournies en entree DOIVENT etre\n\
 \t               structures en paragraphe, c'est a dire avec la\n\
 \t               phrase (ou une partie de la phrase) sur une meme\n\
 \t               ligne et en AUCUN CAS avec un mot par ligne !!!!\n",argv[0]);
 exit(0);
 }

/*fprintf(stderr,"Chargement de l'arbre dans le tableau -> ");*/
ChargeLexiqueCompile(argv[1]);
/*fprintf(stderr,"Termine\n");*/
/*fprintf(stderr,"On a lu : %d noeuds\n",NbNode);*/

while(fgets(ch,TailleLigne,stdin))
 {
 if (!strncmp(ch,"<s>",3))
  if (fgets(ch,TailleLigne,stdin))
   {
   if (strncmp(ch,"</s>",4)) 
    {
    printf("<s>\n");
    /* on elimine les espaces en debut de ligne */
    for(n=0;(ch[n])&&(ch[n]==' ');n++);
    if (ch[n]) ProcessLine(ch+n);
    }
   }
  else
   {
   fprintf(stderr,"Bizarre ???? derniere phrase non fermee !!!!\n");
   exit(0);
   }
 else printf("%s",ch);
 }

exit(0); 
}
  
