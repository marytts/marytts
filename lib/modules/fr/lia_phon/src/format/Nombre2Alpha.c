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
/*  Transforme les nombres en leur forme alphabetique
    Les cas traites par ce module sont :
	* les nombres seuls : 1234 = mille deux cent trente quatre
	* les nombres a virgule : 12,08 = douze virgule zero huit
	* les nombres avec un '.' : 12.08 = douze point zero huit
	* les nombres avec les milliers : 12 000 = douze mille
	* le 1 devant un mois de l'annee : 1 janvier = premier janvier
	* on remplace % par pour_cent et $ par dollars
	* le '-' devant un chiffre devient 'moins' et le '+' 'plus' !!!!
	et bientot ....
	* certains chiffres romains : le XV (de France/tricolore) ,
				      Louis/Henri/Philippe/Georges.... IV/X/....
    Entree : stdin
    Sortie : stdout
    FRED 0299  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <chif_to_alpha.h>
#include <GestionTablMots.h>

/* Traduction Chiffre -> Alpha */

#define TailleLigne	8000

#define SiChiffre(a)    ((((a)>='0')&&((a)<='9'))?1:0)
#define SiRomain(a)	((((a)=='I')||((a)=='V')||((a)=='X')||((a)=='L')||((a)=='C')||((a)=='M'))?1:0)

int SiSeparateur(char a)
{
return ((a==',')||(a=='.'))?1:0;
}

/* Gestion des mois de l'annee pour le traitement du : 1 janvier */

char *TablMois[12]=
        {
        "janvier", "février", "mars", "avril", "mai", "juin", "juillet",
        "août", "septembre", "octobre", "novembre", "décembre"
        } ;

int SiMois(ch)
 char *ch;
{
int n;
for(n=0;n<12;n++)
 if ((!strcmp(ch+1,TablMois[n]+1))&&
	((ch[0]==TablMois[n][0])||(ch[0]==TablMois[n][0]-('a'-'A'))))
  return 1;
return 0;
}

/* Gestion de la liste de mots necessitant la traduction de la ',' */

/* Stockage de la phrase  */

#define MaxMots		2000
#define MaxTailleMot	1000

char Phrase[MaxMots][MaxTailleMot];

/*  Traitement des phrases  */

void AffichePhrase(indice,file_out)
 int indice;
 FILE *file_out;
{
static int n,l;
fprintf(file_out,"%s\n",Phrase[0]);
for(n=1,l=0;n<indice;n++)
 if (Phrase[n][0])
  {
  fprintf(file_out,"%s",Phrase[n]);
  l+=strlen(Phrase[n]);
  if ((l>54)||(n==indice-1)) { fprintf(file_out,"\n"); l=0; } else fprintf(file_out," ");
  }
}

/* Test de type */

int JustDigit(ch)
 char *ch;
{
static int n;
for(n=0;ch[n];n++)
 if (!SiChiffre(ch[n])) return 0;
return 1;
}

int JustRomain(ch)
 char *ch;
{
static int n;
for(n=0;ch[n];n++)
 if (!SiRomain(ch[n])) return 0;
return 1;
}

void ProcessPhrase(indice)
 int indice;
{
static int n,lastindice,indice_modif,prem_indice;
static char refe[MaxTailleMot*10],*lastword;

for(n=0;n<indice;n++)
 /*
 if ((n>0)&&(JustRomain(Phrase[n])))
  {
  printf("%s %s\n",Phrase[n-1],Phrase[n]);
  }
 else
 */
 if (/*(0)&&*/(JustDigit(Phrase[n])))
  {
  /* Traitement de la forme : le 1 janvier */
  if ((!strcmp(Phrase[n],"1"))&&(n+1<indice)&&(SiMois(Phrase[n+1])))
   strcpy(Phrase[n],"premier");
  else
   {
   indice_modif=prem_indice=n;
   /* Traitement des formes : 28 000 = 28000 */
   if ((n+1<indice)&&(JustDigit(Phrase[n+1]))&&(strlen(Phrase[n+1])==3)&&(strlen(Phrase[n])<=3))
    {
    sprintf(refe,"%s%s",Phrase[n],Phrase[n+1]);
    strcpy(Phrase[n+1],refe);
    Phrase[n][0]='\0';
    n++; /* on avance n pour se positionner sur le nombre reconstitue */
    indice_modif=n;
    }
   /* Traitement des formes : <nomb> , <nomb> <mot> avec <mot> faisant
     partie de la liste des mots devant conserver la virgule */
   if ((n+3<indice)&&(SiSeparateur(Phrase[n+1][0]))&&(JustDigit(Phrase[n+2])))
    if (Present(Phrase[n+3],1,&lastword,0,refe,&lastindice))
     {
     sprintf(refe,"%s%s%s",Phrase[n],Phrase[n+1],Phrase[n+2]);
     strcpy(Phrase[n],refe);
     Phrase[n+1][0]=Phrase[n+2][0]='\0';
     indice_modif=n;
     n+=2;
     }
   if (TraiteChaine(Phrase[indice_modif],refe)) strcpy(Phrase[indice_modif],refe);
   if ((prem_indice>0)&&(!strcmp(Phrase[prem_indice-1],"-"))) strcpy(Phrase[prem_indice-1],"moins");
   else if ((prem_indice>0)&&(!strcmp(Phrase[prem_indice-1],"+"))) strcpy(Phrase[prem_indice-1],"plus");
   }
  }
 else
  {
  if (Phrase[n][0]=='%') strcpy(Phrase[n],"pour_cent");
  else
   if (Phrase[n][0]=='$') strcpy(Phrase[n],"dollars");
  }
AffichePhrase(indice,stdout);
}

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[MaxTailleMot],*pt;
int indice;

if (argc<2)
 {
 fprintf(stderr,"Syntaxe : %s [-h] <list mots virgule compil>\n",argv[0]);
 exit(0);
 }

if (!strcmp(argv[1],"-h"))
 {
 fprintf(stderr,"Syntaxe : %s [-h] <list mots virgule compil>\n\
 \t Transforme les nombres en leur forme alphabetique\n\
 \t Les cas traites par ce module sont :\n\
 \t * les nombres seuls : 1234 = mille deux cent trente quatre\n\
 \t * les nombres a virgule : 12,08 = douze vigule zero huit\n\
 \t * les nombres avec un '.' : 12.08 = douze point zero huit\n\
 \t * les nombres avec les milliers : 12 000 = douze mille\n\
 \t * le 1 devant un mois de l'annee : 1 janvier = premier janvier\n\
 \t * on remplace %% par pour_cent et $ par dollars\n\
 \t Le fichier <list mots virgule compil> contient un lexique\n\
 \t compile avec la commande 'compile_lexitree'. Ce lexique contient\n\
 \t tous les mots 'M' tel que, une expression de la forme :\n\
 \t '<nombre1> , <nombre2> M' sera re-ecrite :\n\
 \t '<nombre1> virgule <nombre2> M'. Par exemple : '12 , 8 millions'\n\
 \t sera re-ecrite : 'douze virgule huit millions'\n\n\
 \t ATTENTION :\n\
 \t	Le texte a traiter DOIT avoir ete segmente en phrases avec\n\
 \t	les marques suivantes : '<s>'=debut de phrase et \n\
 \t	'</s>'=fin de phrase. On peut utiliser la commande 'lia_sentence'\n",argv[0]);
 exit(0);
 }

/*fprintf(stderr,"Chargement de l'arbre dans le tableau -> ");*/
ChargeLexiqueCompile(argv[1]);
/*fprintf(stderr,"Termine\n");*/

indice=0;
while(fgets(ch,MaxTailleMot,stdin))
 for(pt=strtok(ch," \t\n");pt;pt=strtok(NULL," \t\n"))
  {
  strcpy(Phrase[indice++],pt);
  if ((indice==MaxMots)||(!strcmp(pt,"</s>")))
   {
   ProcessPhrase(indice);
   indice=0;
   }
  }

exit(0); 
}
 
