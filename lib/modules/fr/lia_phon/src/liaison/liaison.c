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
/*  Genere la liaison entre les mots  */

#include <stdio.h>
#include <declphon.h>
#include <syllabise.h>
#include <liaison.h>
#include <libgram.h>

typedef struct type_regleL
	{
	char *g1,*c1,*g2,*c2,*g3,*c3,*g4,*c4,
	     *modif,*chli,*ex,*listcont;
	} *ty_regleL;

typedef struct type_mot
	{
	char g[TailleLigne],c[TailleLigne];
	} ty_mot;

ty_regleL *RegleL;

int debug,TestRegle;

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

ty_regleL creer_regleL(g1,c1,g2,c2,g3,c3,g4,c4,modif,chli,ex,listcont)
 char *g1,*c1,*g2,*c2,*g3,*c3,*g4,*c4,*modif,*chli,*ex,*listcont;
{
ty_regleL pt;

pt=(ty_regleL) my_malloc(sizeof(struct type_regleL));
pt->g1=(char *)strdup(g1); pt->c1=(char *)strdup(c1);
pt->g2=(char *)strdup(g2); pt->c2=(char *)strdup(c2);
pt->g3=(char *)strdup(g3); pt->c3=(char *)strdup(c3);
pt->g4=(char *)strdup(g4); pt->c4=(char *)strdup(c4);
pt->modif=(char *)strdup(modif);
pt->chli=(char *)strdup(chli);
pt->ex=(char *)strdup(ex);
pt->listcont=(char *)strdup(listcont);
return pt;
}

void FreeReglesL(pt)
 ty_regleL pt;
{
free(pt->g1); free(pt->c1);
free(pt->g2); free(pt->c2);
free(pt->g3); free(pt->c3);
free(pt->g4); free(pt->c4);
free(pt->modif);
free(pt->chli);
free(pt->ex);
free(pt->listcont);
free(pt);
}

void AfficheRegle()
{
int n;

for(n=0;RegleL[n];n++) printf(
 "%d) m1=<%s,%s> m2=<%s,%s> m3=<%s,%s> m4=<%s,%s>\nModif=%s Liaison=%s\nEx=%s\nContrainte=%s\n",
 n,RegleL[n]->g1,RegleL[n]->c1,RegleL[n]->g2,RegleL[n]->c2,
 RegleL[n]->g3,RegleL[n]->c3,RegleL[n]->g4,RegleL[n]->c4,
 RegleL[n]->modif,RegleL[n]->chli,RegleL[n]->ex,RegleL[n]->listcont);
}

int rempli_champs(ch1,c,ch2)
 char *ch1,c,*ch2;
{
int n;

for (n=0;(ch1[n])&&(ch1[n]!=c);n++) ch2[n]=ch1[n];
if (ch1[n])
 ch2[n]='\0';
else
 { printf ("Erreur de format (rempli champs) : %s\n",ch1); exit(0); }
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

int LisReglesLiaison(file)
 FILE *file;
{
char ch[TailleLigne],re[TailleLigne],modif[TailleLigne],chli[TailleLigne],
	ex[TailleLigne],listcont[TailleLigne],
	g1[TailleLigne],g2[TailleLigne],g3[TailleLigne],g4[TailleLigne],
	c1[TailleLigne],c2[TailleLigne],c3[TailleLigne],c4[TailleLigne];
int n,i,NbRegleL,j=0;

for(NbRegleL=0;(fgets(ch,TailleLigne,file))&&(ch[0]!=';');)
 if ((!strncmp(ch,"liaison(",8))||(!strncmp(ch,"context(",8))) NbRegleL++;
fseek(file,0,SEEK_SET);
RegleL=(ty_regleL *) my_malloc((NbRegleL+1)*sizeof(ty_regleL));

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
  for (n+=3,i=0;ch[n]!='"';n++) modif[i++]=ch[n]; modif[i]='\0';
  if (ch[n+3]!=',')
   for (n+=3,i=0;ch[n]!='"';n++) { if (ch[n]!=' ') chli[i++]=ch[n]; }
  else
   n+=2;
  chli[i]='\0';
  for (n+=3,i=0;ch[n]!='"';n++) ex[i++]=ch[n]; ex[i]='\0';
  i=0;
  while (!LastChar(ch,';'))
   {
   if (!fgets(ch,TailleLigne,file)) return False;
   if (!si_classe(ch,g1,g2,g3,g4))
    for (n=0;ch[n];n++) if ((ch[n]!='\n')&&(ch[n]!=' ')&&(ch[n]!='\t')) listcont[i++]=ch[n];
   }
  listcont[i]='\0';
  RegleL[j++]=creer_regleL(g1,c1,g2,c2,g3,c3,g4,c4,modif,chli,ex,listcont);
  }
 if (!fgets(ch,TailleLigne,file)) ch[0]=';';
 }
RegleL[j]=NULL;
fclose(file);
return True;
}

int compatible_char(c1,c2)
 char *c1,*c2;
{
if (c1[0]==c2[0]) return True;
switch (c2[0])
 {
 case 'C' : return ((si_type_graph(TablCons,c1))&&(c1[0]!='h')?True:False);
 case 'V' : return ((si_type_graph(TablVoye,c1))||(c1[0]=='h')?True:False);
 case 'I' : return (si_type_graph(TablNomb,c1)?True:False);
 case 'N' : return (si_type_graph(TablConsGene,c1)?False:True);
 case 'X' : return (((c1[0]!='x')&&(c1[0]!='s'))?True:False);
 case '#' : return (((!si_type_graph(TablCons,c1))&&
		     (!si_type_graph(TablVoye,c1)))?True:False);
 }
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

char *determine_chaine(ch,m1,m2,m3,m4)
 char *ch;
 ty_mot *m1,*m2,*m3,*m4;
{
char *resu;
switch (ch[1])
 {
 case '1'  : resu=((ch[0]=='g')?m1->g:m1->c); break;
 case '2'  : resu=((ch[0]=='g')?m2->g:m2->c); break;
 case '3'  : resu=((ch[0]=='g')?m3->g:m3->c); break;
 case '4'  : resu=((ch[0]=='g')?m4->g:m4->c); break;
 otherwise : printf("Mauvais identificateur : %s\n",ch); exit(0);
 }
return resu;
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

int ou_bien_fin(char *test,char *list)
{
char ch[TailleLigne];
int n,i,l;

for (n=1;(list[n])&&(list[n]!=']');)
 {
 for(i=0;(list[n])&&(list[n]!='"');i++,n++) ch[i]=list[n];ch[i]='\0';
 if (list[n]!='"')
  { printf("Mauvais format de contrainte : %s\n",list); exit(0); }
 if ((i<=(l=strlen(test)))&&(!strncmp(test+l-i,ch,i))) return True;
 for(;(list[n]=='"')||(list[n]==',');n++);
 }
if (list[n]!=']')
 { printf("Mauvais format de contrainte : %s\n",list); exit(0); }
return False;
}

int ou_bien(char *test, char *list)
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

/*  MODIF 0305 FRED : MONOSYLABIQUE = NEED A VOWEL (example: PS)  */

int if_vowel(char c)
{
switch (c)
    {
    case 'a' :
	case 'à' :
	case 'â' :
	case 'ä' :
	case 'e' :
	case 'é' :
	case 'è' :
	case 'ê' :
    case 'ë' :
	case 'i' :
	case 'î' :
	case 'ï' :
	case 'o' :
	case 'ô' :
	case 'ö' :
	case 'u' :
    case 'û' :
	case 'ü' :
	case 'y' :
	case 'A' :
	case 'E' :
	case 'I' :
	case 'O' :
	case 'U' :
	case 'Y' : return True;
    default   : return False;
    }
}

int polysyllabique(ch,vowel)
 char *ch;
 int *vowel;
{
char syll[TailleLigne];
int n,nb;

syllabise(ch,syll);
for (n=0,*vowel=False,nb=1;syll[n];n++)
 if (syll[n]==' ') nb++; else
 if (if_vowel(syll[n])) *vowel=True;
return ((nb>1)?True:False);
}

void liaison_phon_facultative(chtest,chli)
 char *chtest,*chli;
{
switch (chtest[strlen(chtest)-1])
 {
 case 'd' : strcpy(chli,"TF"); return ;
 case 'g' : strcpy(chli,"KF"); return ;
 case 'n' : strcpy(chli,"NF"); return ;
 case 'p' : strcpy(chli,"PF"); return ;
 case 's' : strcpy(chli,"ZF"); return ;
 case 't' : strcpy(chli,"TF"); return ;
 case 'x' : strcpy(chli,"ZF"); return ;
 case 'z' : strcpy(chli,"ZF"); return ;
 }
chli[0]='\0';
}

void liaison_phon(chtest,chli)
 char *chtest,*chli;
{
switch (chtest[strlen(chtest)-1])
 {
 case 'd' : strcpy(chli,"T"); return ;
 case 'g' : strcpy(chli,"K"); return ;
 case 'n' : strcpy(chli,"N"); return ;
 case 'p' : strcpy(chli,"P"); return ;
 case 's' : strcpy(chli,"Z"); return ;
 case 't' : strcpy(chli,"T"); return ;
 case 'x' : strcpy(chli,"Z"); return ;
 case 'z' : strcpy(chli,"Z"); return ;
 }
chli[0]='\0';
}

int compatible_contrainte(ty_mot *m1, ty_mot *m2, ty_mot *m3, ty_mot *m4,
                          int num, char *chli, ty_lexique lexiqueH)
{
char *ch,*chtest;
int n;
wrd_index_t indiceH;

if ((compatible_simpleG(m1->g,RegleL[num]->g1))&&(compatible_simpleG(m2->g,RegleL[num]->g2))&&
    (compatible_simpleG(m3->g,RegleL[num]->g3))&&(compatible_simpleG(m4->g,RegleL[num]->g4))&&
    (compatible_simpleC(m1->c,RegleL[num]->c1))&&(compatible_simpleC(m2->c,RegleL[num]->c2))&&
    (compatible_simpleC(m3->c,RegleL[num]->c3))&&(compatible_simpleC(m4->c,RegleL[num]->c4)))
 {
 /*  Analyse contrainte  */
 for (n=0,ch=RegleL[num]->listcont;(ch[n])&&(ch[n]!=';');n++)
  {
  if (!strncmp(ch+n,"ou_bien_debut",13))
   {
   chtest=determine_chaine(ch+n+14,m1,m2,m3,m4);
   if (!ou_bien_debut(chtest,ch+n+18)) return False;
   for(;ch[n]!=')';n++);
   }
  else
  if (!strncmp(ch+n,"ou_bien_fin",11))
   {
   chtest=determine_chaine(ch+n+12,m1,m2,m3,m4);
   if (!ou_bien_fin(chtest,ch+n+16)) return False;
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
    if (!strncmp(ch+n,"polysyllabique",14))
     {
     int vowel;
     chtest=determine_chaine(ch+n+15,m1,m2,m3,m4);
     if (!polysyllabique(chtest,&vowel)) return False;
     for(;ch[n]!=')';n++);
     }
    else
     if (!strncmp(ch+n,"monosyllabique",14))
      {
      int vowel;
      chtest=determine_chaine(ch+n+15,m1,m2,m3,m4);
      if ((polysyllabique(chtest,&vowel))||(vowel==False)) return False;
      for(;ch[n]!=')';n++);
      }
     else
      if (!strncmp(ch+n,"liaison_phon",12))
       {
       chtest=determine_chaine(ch+n+13,m1,m2,m3,m4);
       liaison_phon(chtest,chli);
       for(;ch[n]!=')';n++);
       }
      else
      if (!strncmp(ch+n,"liaison_facultative",19))
       {
       chtest=determine_chaine(ch+n+20,m1,m2,m3,m4);
       liaison_phon_facultative(chtest,chli);
       for(;ch[n]!=')';n++);
       }
      else
       if (!strncmp(ch+n,"h_aspire",8))
        {
        chtest=determine_chaine(ch+n+9,m1,m2,m3,m4);
        if (!Mot2Code(chtest,&indiceH,lexiqueH)) return False;
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

void corres_liaison(c,li)
 char *c,*li;
{
switch (c[0])
 {
 case 'T' : strcpy(li,"tt") ; break ;
 case 'K' : strcpy(li,"kk") ; break ;
 case 'M' : strcpy(li,"mm") ; break ;
 case 'N' : strcpy(li,"nn") ; break ;
 case 'P' : strcpy(li,"pp") ; break ;
 case 'Z' : strcpy(li,"zz") ; break ;
 case 'V' : strcpy(li,"vv") ; break ;
 case 'S' : strcpy(li,"ss") ; break ;
 case 'D' : strcpy(li,"dd") ; break ;
 case 'J' : strcpy(li,"jj") ; break ;
 case 'L' : strcpy(li,"ll") ; break ;
 default  : li[0]='\0'; return;
 }
if (c[1]=='F') li[0]='|';
}

void si_liaison( ty_mot *m1, ty_mot *m2, ty_mot *m3, ty_mot *m4,
                 char *modif, char *chli, ty_lexique lexiqueH)
{
int n;
char ch[TailleLigne];

modif[0]=chli[0]=ch[0]='\0';
for (n=0;RegleL[n];n++)
 if (compatible_contrainte(m1,m2,m3,m4,n,ch,lexiqueH))
  {
  /*printf("Regle=|%s| Ch=|%s|\n",RegleL[n]->chli,ch);*/

  if (ch[0]=='\0') strcpy(ch,RegleL[n]->chli);
  corres_liaison(ch,chli);
  strcpy(modif,RegleL[n]->modif);
  if ((debug)||(TestRegle==n+1))
   printf("Regle appliquee : %d entre %s et %s\nEx : %s\nModif : %s\nLiaison : %s\n",
	n+1,m2->g,m3->g,RegleL[n]->ex,RegleL[n]->modif,chli);
  
  return;
  }
if (debug) printf("pas de regles compatible entre %s et %s\n",m2->g,m3->g);
}

void si_denasalisation(g2,chli,modif)
 char *g2,*chli,*modif;
{
int t;

if (strcmp(chli,"nn")) return; /* Si pas de liaison en NN, on s`casse  */
t=strlen(g2);
if ((t>2)&&(!strcmp(g2+t-2,"on"))&&(strcmp(g2,"mon"))&&(strcmp(g2,"ton"))&&(strcmp(g2,"son")))
 {
 strcat(modif,"-on+oo");
 return;
 }
if((t>2)&&(!strcmp(g2+t-2,"in"))&&(si_type_graph(TablCons,g2+t-3)))
 {
 strcat(modif,"-in+ii");
 return;
 }
if((t>2)&&((!strcmp(g2+t-3,"ain"))||(!strcmp(g2+t-3,"ein"))))
 strcat(modif,"-in+ai");
}

void traite_fichier(filein,fileout,lexiqueH)
 FILE *filein,*fileout;
 ty_lexique lexiqueH;
{
char ch[TailleLigne],modif[TailleLigne],chli[TailleLigne],chlibak[TailleLigne];
ty_mot m1,m2,m3,m4;
int n,i,fini;

chlibak[0]='\0';

strcpy(m1.g,"  "); strcpy(m2.g,"  "); strcpy(m3.g,"  "); strcpy(m4.g,"  ");
strcpy(m1.c,"  "); strcpy(m2.c,"  "); strcpy(m3.c,"  "); strcpy(m4.c,"  ");

for(fini=False;!fini;)
 {
 strcpy(m1.g,m2.g); strcpy(m2.g,m3.g); strcpy(m3.g,m4.g);
 strcpy(m1.c,m2.c); strcpy(m2.c,m3.c); strcpy(m3.c,m4.c);
 
 if ((!fini)&&(fgets(ch,TailleLigne,filein)))
  {
  ch[strlen(ch)-1]='\0';
  for (n=0;(ch[n])&&(ch[n]!=' ');n++);
  if (ch[n]=='\0')
   {
   /*printf("Cestla : 1\n%s\n",ch); exit(0);*/
   strcpy(m4.g,ch); strcpy(m4.c,"XXXX");
   }
  else
   {
   ch[n]='\0';
   for (i=n+1;(ch[i])&&(ch[i]!=' ');i++);
   ch[i]='\0';
   strcpy(m4.g,ch); strcpy(m4.c,ch+n+1);
   }
  }
 else
  {
  fini=True;
  strcpy(m4.g,"  "); strcpy(m4.c,"  ");
  }
 if ((m2.g)[0])
  {
  si_liaison(&m1,&m2,&m3,&m4,modif,chli,lexiqueH);
  si_denasalisation(m2.g,chli,modif);
  if ((!TestRegle)&&(m2.g[0]!=' ')) fprintf(fileout,"%s %s [%s] [%s]\n",m2.g,m2.c,chlibak,modif);
  strcpy(chlibak,chli);
  }
 }
if (!TestRegle) fprintf(fileout,"%s %s [%s] [%s]\n<FIN>\n",m3.g,m3.c,chlibak,modif);
}
  
