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
/*  Eclate sur chaque ligne les phonemes issus d'un fichier GRIPHON  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

/*  Pour la duree  */

#define REFE_DUREE	90

int DUREE=REFE_DUREE;
char *PLUS60="anoninun";
char *MOINS40="iiuu";
char *PLUS40="ffssch";

/*  Pour le pitch  */

#define REFE_PITCH	120

int PITCH=REFE_PITCH;
int VARI=100;

int TrouveDuree(ch)
 char *ch;
{
char test[3];

test[0]=ch[0]; test[1]=ch[1]; test[2]='\0';
if (((ch[2]=='\0')||(ch[2]==' ')||(ch[2]=='\n'))&&(strcmp(test,"ee")))
 return DUREE+(int)((DUREE*60)/100);
if (strstr(PLUS60,test)) return DUREE+(int)((DUREE*60)/100);
if (strstr(PLUS40,test)) return DUREE+(int)((DUREE*40)/100);
if ((strstr(MOINS40,test))&&(ch[2])) return DUREE-(int)((DUREE*40)/100);
return DUREE;
}

int my_fgets(resu)
 char *resu;
{
char *pt,*fini,ch[202];
for (fini=fgets(ch,200,stdin);(fini)&&((strstr(ch,"????"))||(strstr(ch,"XXXX")));
	fini=fgets(ch,200,stdin)) if (strstr(ch,"XXXX")) PITCH=120;
if (fini==NULL) { resu[0]='\0'; return 0; }
pt=strtok(ch," "); pt=strtok(NULL," ");
strcpy(resu,pt);
return 1;
}

int main(argc,argv)
 int argc;
 char **argv;
{
char ch1[200],ch2[200];
int n,taille,pas,pitch,hasa,FINI;

if ((argc==2)&&(!strcmp(argv[1],"-h")))
 {
 fprintf(stderr,"Syntaxe : %s [-pitch pitch 1-10] [-duree duree 1-10]\n",
	argv[0]);
 exit(0);
 }

for(n=1;n<argc;n++)
 if (!strcmp(argv[n],"-pitch"))
  {
  sscanf(argv[n+1],"%d",&PITCH);
  PITCH=REFE_PITCH+(int)((double)((PITCH-5)*REFE_PITCH)/(double)(5));
  n++;
  }
 else
  if (!strcmp(argv[n],"-duree"))
   {
   sscanf(argv[n+1],"%d",&DUREE);
   DUREE=REFE_DUREE+(int)((double)((DUREE-5)*REFE_DUREE)/(double)(5));
   n++;
   }

printf("## 120\n");

if (!my_fgets(ch1)) FINI=1;
else
 if (!my_fgets(ch2)) FINI=2;
 else FINI=0;

for(;FINI!=1;PITCH--)
 {
 if (PITCH<60) PITCH=60;

 if (strstr(ch1,"YPFOR"))
  printf("## 120\n");
 else
  {
  taille=(int)strlen(ch1)/2;
  pas=(((float)taille)*(float)VARI)/(float)PITCH;
  for(n=0,pitch=PITCH;(ch1[n])&&(ch1[n]!=' ')&&(ch1[n]!='\n');n+=2)
   {
   hasa=(int)rand()%100;
   srand(n+pas);
   if ((ch2[0]=='\0')||(!strncmp(ch2,"##",2)))
    pitch-=pas;
   else
    if (n>taille)
     pitch-=pas;
    else pitch+=pas;
   /*printf("%c%c %d\n",ch1[n],ch1[n+1],TrouveDuree(ch1));*/
   if (pitch<0) pitch=2;
   printf("%c%c %d %d %d\n",ch1[n],ch1[n+1],TrouveDuree(ch1+n),hasa,pitch);
   if (!strncmp(ch1,"##",2)) { PITCH+=(PITCH/2); if (PITCH>120) PITCH=120; }
   }
  }
 strcpy(ch1,ch2);
 if ((FINI==2)||(!my_fgets(ch2))) ch2[0]='\0';
 if (ch1[0]=='\0') FINI=1;
 }

printf("## 120\n");

exit(0); 

}
  
