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
/*  Chargement et appel des modeles de noms propres  */

#include <stdio.h>

#include <stdlib.h>
#include <GereTree.h>
#include <LitTri.h>

int SeeReg=0;

ty_class class,classbi,classun;

int nbclass;

double TablScore[100];

void ChargeModeleNomPro(path)
 char *path;
{
char ch[200];

/*  Initialise classe  */
#ifdef COMPIL_MSDOS
sprintf(ch,"%s\\model_tri",path);
class=NewTabClass(ch,&nbclass);
sprintf(ch,"%s\\model_bi",path);
classbi=NewTabClass(ch,&nbclass);
sprintf(ch,"%s\\model_un",path);
classun=NewTabClass(ch,&nbclass);
#else
sprintf(ch,"%s/model_tri",path);
class=NewTabClass(ch,&nbclass);
sprintf(ch,"%s/model_bi",path);
classbi=NewTabClass(ch,&nbclass);
sprintf(ch,"%s/model_un",path);
classun=NewTabClass(ch,&nbclass);
#endif
}

int ChoisiLangue(mots,label)
 char *mots,**label;
{
char ch[200];
int l,n;

for(n=0;mots[n]==' ';n++);
strcpy(ch,mots+n);
for(n=0;(ch[n])&&(ch[n]!=' ');n++); ch[n]='\0';
strcat(ch,"##");
if ((ch[0]>='a')&&(ch[0]<='z')) ch[0]-=('a'-'A');
/*printf("Mot=|%s|\n ",ch);*/
AffecteScoreTrueII(class,classbi,classun,ch,0);
l=BestScore(class);
if (SeeReg) printf("Langue : %s \n",class[l].label);
*label=class[l].label;
return l;
}

int ChoisiLanguePrenomNom(prenom,nom,label1,label2,label3)
 char *prenom,*nom,**label1,**label2,**label3;
{
char ch[200];
int l,n;

for(n=0;prenom[n]==' ';n++);
strcpy(ch,prenom+n);
for(n=0;(ch[n])&&(ch[n]!=' ');n++); ch[n]='\0';
strcat(ch,"##");
if ((ch[0]>='a')&&(ch[0]<='z')) ch[0]-=('a'-'A');
AffecteScoreTrueII(class,classbi,classun,ch,0);
*label1=class[BestScore(class)].label;
for(n=1;n<=NbClass;n++) TablScore[n]=class[n].score;
for(n=0;nom[n]==' ';n++);
strcpy(ch,nom+n);
for(n=0;(ch[n])&&(ch[n]!=' ');n++); ch[n]='\0';
strcat(ch,"##");
if ((ch[0]>='a')&&(ch[0]<='z')) ch[0]-=('a'-'A');
AffecteScoreTrueII(class,classbi,classun,ch,0);
*label2=class[BestScore(class)].label;

for(n=1;n<=NbClass;n++) class[n].score*=TablScore[n];

l=BestScore(class);
*label3=class[l].label;

if (*label1!=*label2) return 1; else return 0;
}

void LibereModele()
{
LibereClass(class);
LibereClass(classbi);
LibereClass(classun);
}

/*................................................................*/

char *DetermineOrigineDouble(ch1,ch2)
 char *ch1,*ch2;
{
char *label1,*label2,*label3;
extern int debug;
ChoisiLanguePrenomNom(ch1,ch2,&label1,&label2,&label3);
if (debug) printf("%s=%s %s=%s (%s %s)->%s\n",ch1,label1,ch2,label2,ch1,ch2,label3);
return label3;
}

char *DetermineOrigineSimple(ch)
 char *ch;
{
char *label;
ChoisiLangue(ch,&label);
return label;
}

void NomProp_main(chemin)
 char *chemin;
{
char ch[200];
int n;

#ifdef COMPIL_MSDOS
for(n=strlen(chemin);(n>=0)&&(chemin[n]!='\\');n--);
#else
for(n=strlen(chemin);(n>=0)&&(chemin[n]!='/');n--);
#endif
if (n<0) strcpy(ch,".");
else { strncpy(ch,chemin,n); ch[n]='\0'; }
ChargeModeleNomPro(ch);

}
  
