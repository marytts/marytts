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
/*  .................................................  */
/*  Lits le fichier tri lettre d'une serie de classes  */
/*  .................................................  */

#include <stdio.h>
#include <stdlib.h>
#include <GereTreeMorpho.h>
#include <LitTriMorpho.h>

#define Lambda1	0.7
#define Lambda2	0.2
#define Lambda3	0.1

int NbClass;


/*  Allocation memoire  */

char *mystrdup(ch)
 char *ch;
{
char *resu;
resu=(char *)malloc(sizeof(char)*(strlen(ch)+1));
strcpy(resu,ch);
return resu;
}

ty_class NewTabClass(chfile,nnbb)
 char *chfile;
 int *nnbb;
{
FILE *file;
int nb,n,taille;
char ch[200],tri[3];
ty_class resu;

if (!(file=fopen(chfile,"rt")))
 { printf("Impossible d'ouvrir : %s\n",chfile); exit(0); }

fgets(ch,200,file);
sscanf(ch,"NombreClasse: %d",&nb);

resu=(ty_class)my_malloc(sizeof(struct type_class)*(nb+1));
resu[0].tree=NULL;
resu[0].label=(char *)mystrdup("tout");
resu[0].taille=resu[0].nbtri=resu[0].nbmots=0;
for (nb=1;fgets(ch,200,file);nb++)
 {
 if (strncmp(ch,"Classe",6))
  { printf("Mauvais format de fichier ....\n"); exit(0); }

 for(n=8;ch[n]!=' ';n++);
 ch[n]='\0';
 resu[nb].label=(char *)mystrdup(ch+8);
 resu[nb].code=nb-1;
 sscanf(ch+n+9,"%d %d %d",&resu[nb].taille,&resu[nb].nbtri,&resu[nb].nbmots);
 resu[nb].tree=NULL;
 for (n=0;n<resu[nb].taille;n++)
  {
  fgets(ch,200,file);
  sscanf(ch,"%c%c%c %d",tri,tri+1,tri+2,&taille);
  resu[nb].tree=AjouteTrilettre(resu[nb].tree,tri,taille);
  resu[0].tree=AjouteTrilettre(resu[0].tree,tri,taille);
  }
 resu[nb].score=1.;

 resu[0].taille+=resu[nb].taille;
 resu[0].nbtri+=resu[nb].nbtri;
 resu[0].nbmots+=resu[nb].nbmots;
 }
NbClass=(*nnbb)=nb-1;
fclose(file);
return resu;
}

void LibereClass(class)
 ty_class class;
{
int n;

for(n=0;n<=NbClass;n++)
 {
 free(class[n].label);
 LibereArbre(class[n].tree);
 }
free(class);
}

void AfficheClass(class,file)
 ty_class class;
 FILE *file;
{
int n;

fprintf(file,"NombreClasse: %d\n",NbClass);
for(n=0;n<=NbClass;n++)
 {
 fprintf(file,"Classe: %s Taille: %d %d\n",
	class[n].label,class[n].taille,class[n].nbtri);
 AfficheArbre(class[n].tree,file);
 }
}

/*  Determination du score lie a chaque classe  */

void AffecteScoreNaif(class,ch)
 ty_class class;
 char *ch;
{
int n,nb,le,sc_tri;

le=strlen(ch);
for (nb=1;nb<=NbClass;nb++)
 {
 class[nb].score=1.0;
 if (le>MAGIC_N) n=le-MAGIC_N; else n=0;
 for (;n<le-2;n++)
  {
  sc_tri=(int)ScoreTri(ch+n,class[nb].tree);
  class[nb].score+=(double)((double)sc_tri/(double)class[nb].nbmots);
  }
 class[nb].score/=(float)(le-2);

 }
}

void AffecteScore(class,ch,comm)
 ty_class class;
 char *ch;
 int comm;
{
int n,nb,le;
double tmp,somm;

le=strlen(ch);
if (le>MAGIC_N) n=le-MAGIC_N; else n=0;

for (nb=1;nb<=NbClass;nb++) class[nb].score=1.;

for (;n<le-2;n++)
 if ((int)ScoreTri(ch+n,class[0].tree))
  {
  if (comm)
   printf("%c%c%c  =  %d\n",ch[n],ch[n+1],ch[n+2],(int)ScoreTri(ch+n,class[0].tree));


  for (somm=0.,nb=1;nb<=NbClass;nb++)
   somm+=ScoreTri(ch+n,class[nb].tree);

  for (nb=1;nb<=NbClass;nb++)
   {

   tmp=ScoreTri(ch+n,class[nb].tree)/somm;
   class[nb].score*=tmp;

   if (comm)
    printf("  %s=%d -> %lf total : %lf  somm : %lf\n",class[nb].label,
	(int)ScoreTri(ch+n,class[nb].tree),tmp,class[nb].score,somm);
   }
  } 
}
 
void AffecteScoreTrue(class,classbi,ch,comm)
 ty_class class,classbi;
 char *ch;
 int comm;
{
int n,nb,le,nbbi;
double C1,C2;
char bich[4];

le=strlen(ch);
if (le>MAGIC_N) n=le-MAGIC_N; else n=0;

for (nb=1;nb<=NbClass;nb++)
 {
 class[nb].score=(double)((double)class[nb].nbmots/(double)class[0].nbmots);
 if (comm)
  printf("Classe : %s = %d  Total = %d  Score=%lf\n",
	class[nb].label,class[nb].nbmots,class[0].nbmots,class[nb].score);
 }

for (;n<le-2;n++)
 {
 bich[0]=ch[n+1];bich[1]=ch[n+2];bich[2]='@';bich[3]='\0';
 if (comm)
  printf("%c%c%c  (%s) =  %d\n",ch[n],ch[n+1],ch[n+2],bich,(int)ScoreTri(ch+n,class[0].tree));
 for (nb=1;nb<=NbClass;nb++)
  {
  C1=ScoreTri(ch+n,class[nb].tree);
  /*
  for(nbbi=1;(nbbi<=NbClass)&&(strcmp(class[nb].label,classbi[nbbi].label));nbbi++);
  if (nbbi>NbClass) { printf("Probleme : %s\n",class[nb].label); exit(0); }
  */

  nbbi=nb;
  C2=ScoreTri(bich,classbi[nbbi].tree);
  if (((int)C1==0)&&(C1==C2)) C1=0.0000001;

  class[nb].score*=(double)(C1/C2);
  if (comm)
   printf("%s  Ctri=%d  Cbi=%d  Prob=%.4lf  Score=%.4lf\n",class[nb].label,
	(int)C1,(int)C2,C1/C2,class[nb].score);
  } 
 }
}
 
void AffecteScoreTrueII(class,classbi,classun,ch,comm)
 ty_class class,classbi,classun;
 char *ch;
 int comm;
{
int n,nb,le;
double C1,C2,P1,P2,P3;
char bich1[4],bich2[4],unch1[4],unch2[4];

le=strlen(ch);
if (le>MAGIC_N) n=le-MAGIC_N; else n=0;

for (nb=1;nb<=NbClass;nb++)
 {
 class[nb].score=(double)((double)class[nb].nbmots/(double)class[0].nbmots);
 if (comm)
  printf("Classe : %s = %d  Total = %d  Score=%lf\n",
	class[nb].label,class[nb].nbmots,class[0].nbmots,class[nb].score);
 }

for (;n<le-2;n++)
 {
 bich1[0]=ch[n+1];bich1[1]=ch[n+2];bich1[2]='@';bich1[3]='\0';
 bich2[0]=ch[n];bich2[1]=ch[n+1];bich2[2]='@';bich2[3]='\0';
 unch1[0]=ch[n+1];unch1[1]=unch1[2]='@';unch1[3]='\0';
 unch2[0]=ch[n];unch2[1]=unch2[2]='@';unch2[3]='\0';

 if (comm)
  printf("%c%c%c : c2c3=%s c1c2=%s c2=%s c1=%s\n",ch[n],ch[n+1],ch[n+2],bich1,bich2,unch1,unch2);

 for (nb=1;nb<=NbClass;nb++)
  {
  if (comm) printf ("%s ",class[nb].label);

  C1=ScoreTri(ch+n,class[nb].tree);
  C2=ScoreTri(bich1,classbi[nb].tree);

  if (comm) printf("C1=ScoreTri(%c%c%c,trimodel)=%lf\n",ch[n],ch[n+1],ch[n+2],C1);
  if (comm) printf("C2=ScoreTri(%s,bimodel)=%lf\n",bich1,C2); 

  if (C2==0) P1=0; else P1=(double)(Lambda1*C1)/C2;

  /*if (comm) printf("P(%c|%c%c)=%d/%d=%lf  ",ch[n],ch[n+1],ch[n+2],(int)C1,(int)C2,P1);*/
  if (comm) printf("P(%c|%c%c)=%lf ",ch[n],ch[n+1],ch[n+2],P1);

  C1=ScoreTri(bich2,classbi[nb].tree);
  C2=ScoreTri(unch1,classun[nb].tree);
  if (C2==0) P2=0; else P2=(double)(Lambda2*C1)/C2;

  /*if (comm) printf("P(%c|%c)=%d/%d=%lf  ",ch[n],ch[n+1],(int)C1,(int)C2,P2);*/
  if (comm) printf("P(%c|%c)=%lf ",ch[n],ch[n+1],P2);

  C1=ScoreTri(unch2,classun[nb].tree);
  C2=(double)classun[nb].nbtri;
  if (C2==0) P3=0; else P3=(double)(Lambda3*C1)/C2;

  /*if (comm) printf("P(%c)=%d/%d=%lf ",ch[n],(int)C1,(int)C2,P3);*/
  if (comm) printf("P(%c)=%lf  ",ch[n],P3);

  class[nb].score*=(double)(P1+P2+P3);

  if (comm) printf("P=%lf S=%lf\n",(double)P1+P2+P3,class[nb].score);
  } 
 }
}
 
void AffecteScoreII(class,ch,comm)
 ty_class class;
 char *ch;
 int comm;
{
int n,nb,le;
double tmp;

le=strlen(ch);
if (le>MAGIC_N) n=le-MAGIC_N; else n=0;
for (nb=1;nb<=NbClass;nb++) class[nb].score=1.;
for (;n<le-2;n++)
 if ((int)ScoreTri(ch+n,class[0].tree))
  {
  if (comm)
   printf("%c%c%c  =  %d\n",ch[n],ch[n+1],ch[n+2],(int)ScoreTri(ch+n,class[0].tree));
  for (nb=1;nb<=NbClass;nb++)
   {
   tmp=(double)(ScoreTri(ch+n,class[nb].tree)/**1000.*/)/(double)class[nb].nbtri;
   class[nb].score*=tmp;
   if (comm)
    printf("  %s=%d -> %lf total : %lf\n",class[nb].label,
	(int)ScoreTri(ch+n,class[nb].tree),tmp,class[nb].score);
   }
  }
}

void SortScore(class,classbi,classun)
 ty_class class,classbi,classun;
{
int fini=0,n;
struct type_class cl;

while(!fini)
 for(n=1,fini=1;n<=NbClass-1;n++)
  if (class[n].score<class[n+1].score)
   {
   fini=0;
   cl=class[n];
   class[n]=class[n+1];
   class[n+1]=cl;

   cl=classbi[n];
   classbi[n]=classbi[n+1];
   classbi[n+1]=cl;

   cl=classun[n];
   classun[n]=classun[n+1];
   classun[n+1]=cl;
   }
}

void AfficheScore(class)
 ty_class class;
{
int nb;

for (nb=1;nb<=NbClass;nb++)
 printf("%2d] %8s  (%E)\n",nb,class[nb].label,class[nb].score);
}

void RangeScore(class,tablch,tablscore,tailletabl)
 ty_class class;
 char **tablch;
 double *tablscore;
 int tailletabl;
{
int nb;
/* on ne copie que jusqu'a 'tailletabl' qui est le nb max de categorie
   qu'a recu un mot dans le lexique utilise par le tagger !! */
for (nb=1;(nb<=tailletabl)&&(nb<=NbClass);nb++)
 if (class[nb].score>0.0)
  {
  tablch[nb-1]=class[nb].label;
  tablscore[nb-1]=class[nb].score;
  }
}

/*  Outils  */

void TrouvePlace(ch,mot,cate)
 char *ch,**mot,**cate;
{
int n;

*mot=ch;
for(n=0;(ch[n])&&(ch[n]!=' ');n++);
ch[n]='\0';
for(++n;(ch[n])&&(ch[n]==' ');n++);
*cate=ch+n;
/*
for(;(ch[n])&&(ch[n]!=' ');n++);
ch[n]='\0';
*/
}

/*  Retablissement des accents  */

void accent_mot(ch)
 char *ch;
{
char temp[200];
int i,n;

for(n=0,i=0;ch[n];n++)
 {
 switch (ch[n+1])
  {
  case '1' :
	switch (ch[n])
	 {
	 case 'e' : temp[i++]='é'; n++; break;
	 default  : temp[i++]=ch[n];
	 }
	break;
  case '2' :
	switch (ch[n])
	 {
	 case 'a' : temp[i++]='à'; n++; break;
	 case 'e' : temp[i++]='è'; n++; break;
	 case 'u' : temp[i++]='ù'; n++; break;
	 default  : temp[i++]=ch[n];
	 }
	break;
  case '3' :
	switch (ch[n])
	 {
	 case 'a' : temp[i++]='â'; n++; break;
	 case 'e' : temp[i++]='ê'; n++; break;
	 case 'i' : temp[i++]='î'; n++; break;
	 case 'o' : temp[i++]='ô'; n++; break;
	 case 'u' : temp[i++]='û'; n++; break;
	 default  : temp[i++]=ch[n];
	 }
	break;
  case '4' :
	switch (ch[n])
	 {
	 case 'a' : temp[i++]='Ì'; n++; break;
	 case 'e' : temp[i++]='ë'; n++; break;
	 case 'i' : temp[i++]='ï'; n++; break;
	 case 'o' : temp[i++]='Î'; n++; break;
	 case 'u' : temp[i++]='ü'; n++; break;
	 default  : temp[i++]=ch[n];
	 }
	break;
  case '5' :
	switch (ch[n])
	 {
	 case 'c' : temp[i++]='ç'; n++; break;
	 default  : temp[i++]=ch[n];
	 }
	break;
  default  : temp[i++]=ch[n];
  }
 }

temp[i]='\0';
strcpy(ch,temp);
}
  
