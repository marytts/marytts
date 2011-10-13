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
/*  Re etiquetage contextuel des mots  */
/*  + qq formules secretes et bien cachees  10/96  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <anaretik.h>

typedef struct type_regleR
	{
	char *g1,*c1,*g2,*c2,*g3,*c3,*g4,*c4,
	     *newgra,*newcat,*ex,*listcont;
	} *ty_regleR;

typedef struct type_mot
	{
	char g[TailleLigne],c[TailleLigne];
	} ty_mot;

ty_regleR *RegleR;

int debug,TestRegle;

extern int Zvalide();

/*  Stockage de la phrase entiere  */

#define Fenetre         8000
char Phrase[Fenetre][TailleLigne];

int T_PHRASE; /*  Variable globale stockant la taille max de la phrase  */

/*................................................................*/

/*  Choix de l'origine linguistique  */

extern char *DetermineOrigineDouble();

extern char *DetermineOrigineSimple();

/*................................................................*/

void *my_malloc(taille)
 int taille;
{
void *ptr;

ptr=(void *) malloc(taille);
if (ptr==NULL)
 {
 printf("Can't allocate memory\n");
 exit(0);
 }
return ptr;
}

/*  Creation regles  */

ty_regleR creer_regleR(g1,c1,g2,c2,g3,c3,g4,c4,newgra,newcat,ex,listcont)
 char *g1,*c1,*g2,*c2,*g3,*c3,*g4,*c4,*newgra,*newcat,*ex,*listcont;
{
ty_regleR pt;

pt=(ty_regleR) my_malloc(sizeof(struct type_regleR));
pt->g1=(char *)strdup(g1); pt->c1=(char *)strdup(c1);
pt->g2=(char *)strdup(g2); pt->c2=(char *)strdup(c2);
pt->g3=(char *)strdup(g3); pt->c3=(char *)strdup(c3);
pt->g4=(char *)strdup(g4); pt->c4=(char *)strdup(c4);
pt->newgra=(char *)strdup(newgra);
pt->newcat=(char *)strdup(newcat);
pt->ex=(char *)strdup(ex);
pt->listcont=(char *)strdup(listcont);
return pt;
}

void FreeReglesL(pt)
 ty_regleR pt;
{
free(pt->g1); free(pt->c1);
free(pt->g2); free(pt->c2);
free(pt->g3); free(pt->c3);
free(pt->g4); free(pt->c4);
free(pt->newgra);
free(pt->newcat);
free(pt->ex);
free(pt->listcont);
free(pt);
}

void AfficheRegle()
{
int n;

for(n=0;RegleR[n];n++) printf(
 "%d) m1=<%s,%s> m2=<%s,%s> m3=<%s,%s> m4=<%s,%s>\nModif=%s Liaison=%s\nEx=%s\nContrainte=%s\n",
 n,RegleR[n]->g1,RegleR[n]->c1,RegleR[n]->g2,RegleR[n]->c2,
 RegleR[n]->g3,RegleR[n]->c3,RegleR[n]->g4,RegleR[n]->c4,
 RegleR[n]->newgra,RegleR[n]->newcat,RegleR[n]->ex,RegleR[n]->listcont);
}

int rempli_champs(ch1,c,ch2)
 char *ch1,c,*ch2;
{
int n;
for (n=0;(ch1[n])&&(ch1[n]!=c);n++) ch2[n]=ch1[n];
if (ch1[n]) ch2[n]='\0';
else { printf ("Erreur de format (rempli champs) : %s\n",ch1); exit(0); }
return n;
}

int analyse_mot(ch,g,c,cara)
 char *ch,*g,*c,cara;
{
int i,n;

if (ch[0]=='m')
 {
 g[0]=c[0]='\0';
 return 2;
 }
if (ch[0]!='<')
 {
 printf("Erreur de format (analyse mot) : %s\n",ch);
 exit(0);
 }
i=0;
if (ch[3]!=',')
 {
 for(n=1;ch[n]!=',';n++)
  if (ch[n]=='"') for (++n;ch[n]!='"';n++) g[i++]=ch[n];
  else
   if (ch[n]=='a') g[i++]='A';
   else if (ch[n]=='g') g[i++]='G';
 }
else n=3;
g[i]='\0';
i=0;
if (ch[++n]=='"')
 for (++n;ch[n]!='"';n++) c[i++]=ch[n];
c[i]='\0';
for (;ch[n]!='>';n++);
for (++n;ch[n]!=cara;n++);
return n;
}

int LastChar(ch,c)
 char *ch,c;
{
int n;

for (n=strlen(ch)-1;(n)&&((ch[n]=='\n')||(ch[n]==' '));n--);
return (ch[n]==c?True:False);
}

int si_classe(ch,g1,g2,g3,g4)
 char *ch,*g1,*g2,*g3,*g4;
{
int n;
char c;

for (n=0;(ch[n]==' ')||(ch[n]=='\t');n++);
if (!strncmp(ch+n,"classe",6))
 {
 for (;ch[n]!=',';n++);
 if (ch[n+1]=='"')
  c=ch[n+2];
 else
  if (!strncmp(ch+n+1,"Consonne_Non_Generatrice",24)) c='N';
  else
   if (!strncmp(ch+n+1,"Non_X_ou_S",10)) c='X';
   else
    {
    printf("Classe inconnue : %s\n",ch+n+1);
    exit(0);
    }
 for (n=0;g1[n];n++) if (g1[n]=='A') g1[n]=c;
 for (n=0;g2[n];n++) if (g2[n]=='A') g2[n]=c;
 for (n=0;g3[n];n++) if (g3[n]=='A') g3[n]=c;
 for (n=0;g4[n];n++) if (g4[n]=='A') g4[n]=c;
 return True;
 }
else return False;
}

int LisReglesRetik(file)
 FILE *file;
{
char ch[TailleLigne],re[TailleLigne],newgra[TailleLigne],newcat[TailleLigne],
	ex[TailleLigne],listcont[TailleLigne],
	g1[TailleLigne],g2[TailleLigne],g3[TailleLigne],g4[TailleLigne],
	c1[TailleLigne],c2[TailleLigne],c3[TailleLigne],c4[TailleLigne];
int n,i,NbRegleR,j=0;

for(NbRegleR=0;(fgets(ch,TailleLigne,file))&&(ch[0]!=';');)
 if ((!strncmp(ch,"liaison(",8))||(!strncmp(ch,"context(",8))) NbRegleR++;
fseek(file,0,SEEK_SET);
RegleR=(ty_regleR *) my_malloc((NbRegleR+1)*sizeof(ty_regleR));

fgets(ch,TailleLigne,file);
while(ch[0]!=';')
 {
 if ((!strncmp(ch,"liaison(",8))||(!strncmp(ch,"context(",8)))
  {
  n=rempli_champs(ch+8,',',re)+8;
  n=analyse_mot(ch+n+2,g1,c1,',')+n+2;
  n=analyse_mot(ch+n+1,g2,c2,',')+n+1;
  n=analyse_mot(ch+n+1,g3,c3,',')+n+1;
  n=analyse_mot(ch+n+1,g4,c4,'>')+n+1;
  for (n+=3,i=0;ch[n]!='"';n++) newgra[i++]=ch[n]; newgra[i]='\0';
  if (ch[n+3]!=',')
   for (n+=3,i=0;ch[n]!='"';n++) { if (ch[n]!=' ') newcat[i++]=ch[n]; }
  else
   n+=2;
  newcat[i]='\0';
  for (n+=3,i=0;ch[n]!='"';n++) ex[i++]=ch[n]; ex[i]='\0';
  i=0;
  while (!LastChar(ch,';'))
   {
   if (!fgets(ch,TailleLigne,file)) return False;
   if (!si_classe(ch,g1,g2,g3,g4))
    for (n=0;ch[n];n++) if ((ch[n]!='\n')&&(ch[n]!=' ')&&(ch[n]!='\t')) listcont[i++]=ch[n];
   }
  listcont[i]='\0';
  RegleR[j++]=creer_regleR(g1,c1,g2,c2,g3,c3,g4,c4,newgra,newcat,ex,listcont);
  }
 if (!fgets(ch,TailleLigne,file)) ch[0]=';';
 }
RegleR[j]=NULL;
fclose(file);
return True;
}

int compatible_char(c1,c2)
 char *c1,*c2;
{
if (c1[0]==c2[0]) return True;
return False;
}

int compatible_simpleG(g,gr)
 char *g,*gr;
{
int n,i;

/*printf("Compatible simpleG : %s et %s\n",g,gr);*/

if (gr[0]=='\0') return True;
if (gr[0]=='G')
 {
 for (n=strlen(gr)-1,i=strlen(g)-1;(n>0)&&(gr[n]!='G');n--,i--)
  if (!compatible_char(g+i,gr+n)) return False;
 }
else
 {
 for (n=0;(gr[n])&&(gr[n]!='G');n++)
  if (!compatible_char(g+n,gr+n)) return False;
 if ((gr[n]=='\0')&&(g[n]!='\0')) return False;
 }
/*printf("Oui ....\n");*/
return True;
}

int compatible_simpleC(g,gr)
 char *g,*gr;
{
if (gr[0]=='\0') return True;
return ((strcmp(gr,g))?False:True);
}

char *determine_chaine(char *ch, ty_mot *m1, ty_mot *m2, ty_mot *m3, ty_mot *m4)
{
char *resu;
switch (ch[1])
 {
 case '1' : resu=((ch[0]=='g')?m1->g:m1->c); break;
 case '2' : resu=((ch[0]=='g')?m2->g:m2->c); break;
 case '3' : resu=((ch[0]=='g')?m3->g:m3->c); break;
 case '4' : resu=((ch[0]=='g')?m4->g:m4->c); break;
 otherwise: printf("Mauvais identificateur : %s\n",ch); exit(0);
 }
return resu;
}

/*  Fonctions contraintes  */

int SeulMaju(ch)
 char *ch;
{
int n;

for (n=0;ch[n];n++)
 if (((ch[n]<'A')||(ch[n]>'Z'))&&(ch[n]!=' ')&&(ch[n]!='.')) return False;
return True;
}

int potentiel_romain(ch)
 char *ch;
{
int n;
for(n=0;ch[n];n++)
 if ((ch[n]!='X')&&(ch[n]!='I')&&(ch[n]!='V')&&(ch[n]!='M')&&
     (ch[n]!='D')) return False;
return True;
}

int InSentenceBegin(ch)
 char *ch;
{
int n;
for (n=0;n<T_PHRASE;n++)
 if (!strncmp(ch,Phrase[n],strlen(ch))) return True;
return False;
}

int dans_la_phrase_debut(list)
 char *list;
{
int n,i;
char ch[TailleLigne];
for(n=1;(list[n])&&(list[n]!=']');)
 {
 for(i=0;(list[n])&&(list[n]!='"');n++) ch[i++]=list[n];
 ch[i]='\0';
 if (list[n]!='"') { printf("Mauvais format de contrainte : %s\n",list); exit(0); }
 if (InSentenceBegin(ch)) return True;
 for(++n;(list[n])&&(list[n]!='"')&&(list[n]!=']');n++);
 if (list[n]=='\0') { printf("Mauvais format de contrainte : %s\n",list); exit(0); }
 if (list[n]=='"') n++;
 }
return False;
}

int ou_bien_debut(test,list)
 char *test,*list;
{
char ch[TailleLigne];
int n,i;

for (n=1;(list[n])&&(list[n]!=']');)
 {
 for(i=0;(list[n])&&(list[n]!='"');i++,n++) ch[i]=list[n];
 if (list[n]!='"')
  { printf("Mauvais format de contrainte : %s\n",list); exit(0); }
 if (!strncmp(test,ch,i)) return True;
 for(;(list[n]=='"')||(list[n]==',');n++);
 }
if (list[n]!=']')
 { printf("Mauvais format de contrainte : %s\n",list); exit(0); }
return False;
}

int ou_bien(test,list)
 char *test,*list;
{
char ch[TailleLigne];
int n,i;

for (n=1;(list[n])&&(list[n]!=']');)
 {
 for(i=0;(list[n])&&(list[n]!='"');i++,n++) ch[i]=list[n];
 if (list[n]!='"')
  { printf("Mauvais format de contrainte : %s\n",list); exit(0); }
 ch[i]='\0';
 if (!strcmp(test,ch)) return True;
 for(;(list[n]=='"')||(list[n]==',');n++);
 }
if (list[n]!=']')
 { printf("Mauvais format de contrainte : %s\n",list); exit(0); }
return False;
}

int compatible_contrainte(ty_mot *m1, ty_mot *m2, ty_mot *m3, ty_mot *m4, int num, char *newcat)
{
char *ch,*chtest,*chtest2;
int n;

if (debug) printf("m1=(%s,%s) m2=(%s,%s) m3=(%s,%s) m4=(%s,%s)\n",
	m1->g,m1->c,m2->g,m2->c,m3->g,m3->c,m4->g,m4->c);
if ((compatible_simpleG(m1->g,RegleR[num]->g1))&&(compatible_simpleG(m2->g,RegleR[num]->g2))&&
    (compatible_simpleG(m3->g,RegleR[num]->g3))&&(compatible_simpleG(m4->g,RegleR[num]->g4))&&
    (compatible_simpleC(m1->c,RegleR[num]->c1))&&(compatible_simpleC(m2->c,RegleR[num]->c2))&&
    (compatible_simpleC(m3->c,RegleR[num]->c3))&&(compatible_simpleC(m4->c,RegleR[num]->c4)))
 {
 /*  Analyse contrainte  */
 for (n=0,ch=RegleR[num]->listcont;(ch[n])&&(ch[n]!=';');n++)
  {
  if (!strncmp(ch+n,"ou_bien_debut",13))
   {
   chtest=determine_chaine(ch+n+14,m1,m2,m3,m4);
   if (!ou_bien_debut(chtest,ch+n+18)) return False;
   for(;ch[n]!=')';n++);
   }
  else
  if (!strncmp(ch+n,"ou_bien",7))
   {
   chtest=determine_chaine(ch+n+8,m1,m2,m3,m4);
   if (!ou_bien(chtest,ch+n+12)) return False;
   for(;ch[n]!=')';n++);
   }
  else
   if (!strncmp(ch+n,"different_debut",15))
    {
    chtest=determine_chaine(ch+n+16,m1,m2,m3,m4);
    if (ou_bien_debut(chtest,ch+n+20)) return False;
    for(;ch[n]!=')';n++);
    }
   else
   if (!strncmp(ch+n,"different",9))
    {
    chtest=determine_chaine(ch+n+10,m1,m2,m3,m4);
    if (ou_bien(chtest,ch+n+14)) return False;
    for(;ch[n]!=')';n++);
    }
   else
    if (!strncmp(ch+n,"potentiel_romain",16))
     {
     chtest=determine_chaine(ch+n+17,m1,m2,m3,m4);
     if (!potentiel_romain(chtest)) return False;
     for(;ch[n]!=')';n++);
     }
    else
     if (!strncmp(ch+n,"dans_la_phrase_debut",20))
      {
      if (!dans_la_phrase_debut(ch+n+22)) return False;
      for(;ch[n]!=')';n++);
      }
     else
      if (!strncmp(ch+n,"just_maju",9))
       {
       chtest=determine_chaine(ch+n+10,m1,m2,m3,m4);
       if (!SeulMaju(chtest)) return False;
       for(;ch[n]!=')';n++);
       }
      else
      if (!strncmp(ch+n,"prem_lett_maju",14))
       {
       chtest=determine_chaine(ch+n+15,m1,m2,m3,m4);
       if ((chtest[0]<'A')||(chtest[0]>'Z')) return False;
       for(;ch[n]!=')';n++);
       }
      else
       if (!strncmp(ch+n,"origine_double",14))
	{
	chtest=determine_chaine(ch+n+15,m1,m2,m3,m4);
	chtest2=determine_chaine(ch+n+18,m1,m2,m3,m4);
	strcpy(newcat,"->");
        strcat(newcat,DetermineOrigineDouble(chtest,chtest2));
        for(;ch[n]!=')';n++);
	}
       else
	if (!strncmp(ch+n,"origine_simple",14))
	 {
	 chtest=determine_chaine(ch+n+15,m1,m2,m3,m4);
	 strcpy(newcat,"->");
	 strcat(newcat,DetermineOrigineSimple(chtest));
         for(;ch[n]!=')';n++);
	 }
        else
         {
         printf("Contrainte inconnue : %s\n",ch+n);
         exit(0);
         }
  }
 return True;
 }
return False;
}

void si_retik(ty_mot *m1, ty_mot *m2, ty_mot *m3, ty_mot *m4, char *newgra, char *newcat)
{
int n;
static char ch[TailleLigne];
newgra[0]=newcat[0]=ch[0]='\0';
for (n=0;RegleR[n];n++)
 if (compatible_contrainte(m1,m2,m3,m4,n,ch))
  {
  if (ch[0]=='\0') strcpy(newcat,RegleR[n]->newcat);
  else strcpy(newcat,ch);
  strcpy(newgra,RegleR[n]->newgra);
  if ((debug)||(TestRegle==n+1))
   printf("Regle appliquee : %d entre %s et %s\nEx : %s\nModif : %s\nLiaison : %s\n",
	n+1,m2->g,m3->g,RegleR[n]->ex,RegleR[n]->newgra,newcat);
  return;
  }
if (debug) printf("pas de regles compatible entre %s et %s\n",m2->g,m3->g);
}

void traite_phrase(nb,fileout)
 int nb;
 FILE *fileout;
{
static char ch[TailleLigne],newgra[TailleLigne],newcat[TailleLigne],newcatbak[TailleLigne],*pt;
ty_mot m1,m2,m3,m4;
int indice;

newcatbak[0]='\0';

strcpy(m1.g,"  "); strcpy(m2.g,"  "); strcpy(m3.g,"  "); strcpy(m4.g,"  ");
strcpy(m1.c,"  "); strcpy(m2.c,"  "); strcpy(m3.c,"  "); strcpy(m4.c,"  ");

for(indice=0;indice<=nb+1;indice++)
 {
 strcpy(m1.g,m2.g); strcpy(m2.g,m3.g); strcpy(m3.g,m4.g);
 strcpy(m1.c,m2.c); strcpy(m2.c,m3.c); strcpy(m3.c,m4.c);
 
 if (indice<=nb)
  {
  strcpy(ch,Phrase[indice]);
  pt=strtok(ch," \t\n");
  if (pt==NULL) { fprintf(stderr,"ZARBI: why no word on indice %d\n",indice); exit(0); }
  strcpy(m4.g,pt);
  pt=strtok(NULL," \t\n");
  if (pt==NULL) { fprintf(stderr,"ZARBI: why no POS on indice %d\n",indice); exit(0); }
  strcpy(m4.c,pt); 
  }
 else { strcpy(m4.g,"  "); strcpy(m4.c,"  "); }
 if ((m2.g)[0])
  {
  si_retik(&m1,&m2,&m3,&m4,newgra,newcat); 
  if (newgra[0]=='\0') strcpy(newgra,m2.g);
  if ((!TestRegle)&&(m2.g[0]!=' '))
   fprintf(fileout,"%s %s%s\n",newgra,m2.c,newcat);
  strcpy(newcatbak,newcat);
  }
 }
if (!TestRegle)
 fprintf(fileout,"%s %s\n",m3.g,m3.c);
}

void traite_fichier(filein,fileout)
 FILE *filein,*fileout;
{
int n,flag;
unsigned long nbphrase;

for(n=flag=0,nbphrase=0;fgets(Phrase[n],TailleLigne,filein);n++)
 {
 flag=0;
 /*if ((n>2)&&((strstr(Phrase[n],"ZTRM"))||(strstr(Phrase[n],"YPFOR"))))*/
 /* on ne s'arrete que sur les ZTRM */
 if ((n==Fenetre)||((n>2)&&(strstr(Phrase[n],"ZTRM"))))
  {
  if (n==Fenetre) { fprintf(stderr,"WARNING: in 'anaretik.c' Fenetre trop petite ....\n"); }
  nbphrase++;
  T_PHRASE=n;
  traite_phrase(n,fileout);
  n=-1;
  flag=1;
  }
 }
n--; /* on decremente n car le dernier correspond a la fin de l'entree standard */
if ((flag==0)&&(n>1)) { nbphrase++; traite_phrase(n,fileout); }
}
 
