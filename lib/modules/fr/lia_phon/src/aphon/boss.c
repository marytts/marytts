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

/*
Remerciement : Merci a SYLVAIN DERDERIAN pour avoir participe aux
    premiers balbutiements de ce projet !!
*/

/*  Main du programme de syllabisation  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <syllabise.h>
#include <phonetise.h>
#include <gere_exception.h>

/*  Gestion de la ponctuation  */

#define Ponctu	1

char finch[TailleLigne];

void GenerePonctuation(orig,ch)
 char *orig,*ch;
{
switch (orig[0])
 {
 case ',' : strcpy(ch,"vviirrgguull"); break;
 case ';' : strcpy(ch,"ppwwinvviirrgguull"); break;
 case '?' : strcpy(ch,"ppwwinddintteirrauggaassyyon"); break;
 case '!' : strcpy(ch,"ppwwinddaikksseekkllaammaassyyon"); break;
 case '(' : strcpy(ch,"ouvvrriirreellaappaarranttaizz"); break;
 case ')' : strcpy(ch,"ffairrmmeillaappaarranttaizz"); break;
 case '[' : strcpy(ch,"ouvvrriirreelleikkrrauchei"); break;
 case ']' : strcpy(ch,"ffairrmmeilleikkrrauchei"); break;
 case '{' : strcpy(ch,"ouvvrriirreellaakkoollaadd"); break;
 case '}' : strcpy(ch,"ffairrmmeillaakkoollaadd"); break;
 case ':' : strcpy(ch,"ddeuppwwin"); break;
 case '.' : strcpy(ch,"ppwwin"); break;
 case '"' : strcpy(ch,"ggiiyyeemmei"); break;
 /*default : strcpy(ch,"    ");*/ default : strcpy(ch,"????");
 }
}

/*=========================================================*/
/* fonctions du phonetiseur                                */
/*=========================================================*/

void trouve_contrainte(ch,cont,phonav,phonap)
 char *ch,*cont,*phonav,*phonap;
{
int n,i,j;

cont[0]='\0';
for (n=0;(ch[n])&&(ch[n]!='\n')&&(ch[n]!=' ');n++);
if (ch[n]=='\n') ch[n]='\0';
if (ch[n]=='\0') return;
for (j=n;(ch[n])&&(ch[n]!='\n')&&(ch[n]==' ');n++);
if (ch[n]=='\n') ch[n]='\0';
if (ch[n]=='\0') return;
for (i=0;(ch[n])&&(ch[n]!='\n')&&(ch[n]!=' ');n++,i++) cont[i]=ch[n];
ch[j]='\0';
cont[i]='\0';
for (;(ch[n])&&(ch[n]!='\n')&&(ch[n]==' ');n++);

strcpy(finch,ch+n);

if (ch[n]=='[')
 {
 for(i=0,++n;(ch[n])&&(ch[n]!='\n')&&(ch[n]!=' ')&&(ch[n]!=']');n++,i++) phonav[i]=ch[n];
 phonav[i]='\0';
 for (;(ch[n])&&(ch[n]!='[');n++);
 for(i=0,++n;(ch[n])&&(ch[n]!='\n')&&(ch[n]!=' ')&&(ch[n]!=']');n++,i++) phonap[i]=ch[n];
 phonap[i]='\0';
 }
}

void enleve_majuscule(ch1,ch2)
 char *ch1,*ch2;
{
int n,i;

for(n=0,i=0;ch1[n];n++)
 /*if (ch1[n]!='\'')*/
  {
  switch (ch1[n])
   {
   case 'É' : ch2[i]='é'; break;
   case 'Ë' : ch2[i]='ë'; break;
   case 'Ê' : ch2[i]='ê'; break;
   case 'Ç' : ch2[i]='ç'; break;
   case 'Â' : ch2[i]='â'; break;
   case 'Ä' : ch2[i]='ä'; break;
   default : ch2[i]=ch1[n];
  	     if ((ch2[i]>='A')&&(ch2[i]<='Z')) ch2[i]+=('a'-'A');
   }
  i++;
  }
ch2[i]='\0';
}

int SeulMaju(ch)
 char *ch;
{
int n;

for (n=0;ch[n];n++)
 if (((ch[n]<'A')||(ch[n]>'Z'))&&(ch[n]!=' ')&&(ch[n]!='.')) return False;
return True;
}

/*  Determine la base de regle selon l'origine linguistique  */

/* si VARIANTE_NP= 1 alors on donne toutes les phonetisations possibles selon les 8 langues */
int VARIANTE_NP=0;

#define NbClassNP	8

int ChoisiLangue(ch,mot)
 char *ch,*mot;
{
if (strstr(ch,"propername_1"))	return 1;
if (strstr(ch,"propername_2"))	return 2;
if (strstr(ch,"propername_3"))	return 3;
if (strstr(ch,"propername_4"))	return 4;
if (strstr(ch,"propername_5"))	return 5;
if (strstr(ch,"propername_6"))	return 6;
if (strstr(ch,"propername_7"))	return 7;
if (strstr(ch,"propername_8")) 	return 8;
fprintf(stderr,"Warning!! %s origine inconnue de %s ....\n",ch,mot);
return 6;
}

int main(argc,argv)
 int argc;
 char **argv;
{
char    ch1[TailleLigne],  ch2[TailleLigne],tabl_NP[NbClassNP+1][TailleLigne],chsyll[TailleLigne],
	cont[10*TailleLigne], phonav[TailleLigne], phonap[TailleLigne],
	orig[TailleLigne], path_fich[TailleLigne], *exepphon, *pointeur,
 fichExep[200],				/* list d'exceptions aux regles */
 fichreglesP[200],			/* regles de phonétisation du propername_6 */

#ifdef COMPIL_MSDOS
 *fichreglesD  ="\\data\\desigle.pron",	/* regles de décision sur les sigles Epele/Lu */
 *fichreglesE  ="\\data\\epeler_sig.pron",/* regles de phonetisation des sigles epeles  */
 *fichreglesL  ="\\data\\lire_sig.pron",	/* regles de phonetisation des sigles lus */
 *fichreglesNP1="\\data\\propername_1.pron",	/* regles de phonetisation des Noms propername_1 */
 *fichreglesNP2="\\data\\propername_2.pron",	/* regles de phonetisation des Noms propername_2 */
 *fichreglesNP3="\\data\\propername_3.pron",	/* regles de phonetisation des Noms propername_3 */
 *fichreglesNP4="\\data\\propername_4.pron",	/* regles de phonetisation des Noms propername_4 */
 *fichreglesNP5="\\data\\propername_5.pron",	/* regles de phonetisation des Noms propername_5 */
 *fichreglesNP6="\\data\\propername_6.pron",	/* regles de phonetisation des Noms propername_6 */
 *fichreglesNP7="\\data\\propername_7.pron",	/* regles de phonetisation des Noms propername_7 */
 *fichreglesNP8="\\data\\propername_8.pron";	/* regles de phonetisation des Noms propername_8 */
#else
 *fichreglesD  ="/data/desigle.pron",	/* regles de décision sur les sigles Epele/Lu */
 *fichreglesE  ="/data/epeler_sig.pron",/* regles de phonetisation des sigles epeles  */
 *fichreglesL  ="/data/lire_sig.pron",	/* regles de phonetisation des sigles lus */
 *fichreglesNP1="/data/propername_1.pron",	/* regles de phonetisation des Noms propername_1 */
 *fichreglesNP2="/data/propername_2.pron",	/* regles de phonetisation des Noms propername_2 */
 *fichreglesNP3="/data/propername_3.pron",	/* regles de phonetisation des Noms propername_3 */
 *fichreglesNP4="/data/propername_4.pron",	/* regles de phonetisation des Noms propername_4 */
 *fichreglesNP5="/data/propername_5.pron",	/* regles de phonetisation des Noms propername_5 */
 *fichreglesNP6="/data/propername_6.pron",	/* regles de phonetisation des Noms propername_6 */
 *fichreglesNP7="/data/propername_7.pron",	/* regles de phonetisation des Noms propername_7 */
 *fichreglesNP8="/data/propername_8.pron";	/* regles de phonetisation des Noms propername_8 */
#endif

int n,j, siafregles=False, AfLiaison=False,sisigle,retour , si_romain,indice_tabl_NP, si_syllabe, af_syllabe;

ty_regles *ReglesP, *ReglesD, *ReglesL, *ReglesE, *ReglesNP1, *ReglesNP2,
	  *ReglesNP3, *ReglesNP4, *ReglesNP5, *ReglesNP6, *ReglesNP7, *ReglesNP8;

int NbReglesP, NbReglesD, NbReglesL, NbReglesE, NbReglesNP1, NbReglesNP2,
    NbReglesNP3, NbReglesNP4, NbReglesNP5, NbReglesNP6, NbReglesNP7, NbReglesNP8;

FILE *ftest;

extern int TransNombreRomain();
extern int SeeReg;
extern int AfFrontiere;

SeeReg=AfFrontiere=False;

fichreglesP[0]='\0';   /*  init du fich de regles courantes  */
fichExep[0]='\0';      /*  init du fich d'exceptions  */

path_fich[0]='\0';
si_syllabe=True;
af_syllabe=False;

for (n=1;n<argc;n++)
 switch (argv[n][1])
  {
  case 'a' :  /*  Affiche les regles triées  */
 	siafregles=True;
	break;
  case 'd' :  /*  Affichage des regles utilisées + syllabisation  */
  	SeeReg=True;
	break;
  case 'e' :  /*  Spécifie un fichier exception autre que data/list_exep  */
	strcpy(fichExep,argv[++n]);
	break;
  case 'f' :  /*  Affichage avec les frontieres de transcriptions  */
  	AfFrontiere=True;
	break;
  case 'l' :  /*  Affichage avec liaison : <mot> <phon> <categorie>  */
  	AfLiaison=True;
	break;
  case 'r' :  /*  Spécifie un fichier regles autre que regles_phon.pro3 ou un repertoire  */
	if (!strcmp(argv[n],"-rep"))
	 strcpy(path_fich,argv[++n]);
        else
	 strcpy(fichreglesP,argv[++n]);
	break;
  case 'v' : /* on donne toutes les variantes des noms propres selon les 8 langues */
	VARIANTE_NP=1;
	break;
  case 'n' : /* pas de syllabisation effectuee */
	si_syllabe=False;
	break;
  case 's' : /* affiche la syllabisation effectuee */
	af_syllabe=True;
	break;
  case 'h' :  /*  Affiche la liste des options possibles  */
	printf("Usage:  aphon [-a|-t|-d|-l|-r|-h]\n");
	printf("              a : affiche le fichier de regles triees\n");
	printf("              d : debug, affiche les regles utilisees avec la syllabisation\n");
	printf("              e : charge un fichier exception autre que data/list_exep(.zp)(.gra)\n");
	printf("              l : affiche suivant le format <mot> <phon> <categorie>\n");
	printf("              r : charge un fichier de regles different de 'french01.pron'\n");
	printf("              f : Affiche les frontieres de transcription\n");
	printf("              v : donne toutes les variantes des noms propres selon les 8 langues\n");
	printf("              n : pas de syllabisation effectuee\n");
	printf("              s : affiche la syllabisation\n");
	printf("              h : affiche ce message\n");
	exit(0);
  }

if (path_fich[0]=='\0') strcpy(path_fich,getenv("LIA_PHON_REP"));

#ifdef COMPIL_MSDOS
sprintf(ch1,"%s\\data",path_fich);
#else
sprintf(ch1,"%s/data",path_fich);
#endif

if (!(ftest=fopen(ch1,"r"))) /* Si le repertoire data n'existe pas, on mets un rep par defaut */
 strcpy(path_fich,".");
else
 fclose(ftest);

if (fichreglesP[0]=='\0')
 {
#ifdef COMPIL_MSDOS
 strcpy(fichreglesP,"\\data\\french01.pron");
#else
 strcpy(fichreglesP,"/data/french01.pron");
#endif

 ReglesP=lecture_regles(path_fich,fichreglesP,&NbReglesP);
 }
else /*  Dans le cas ou un autre fichregle est select on met pas le path  */
 ReglesP=lecture_regles("",fichreglesP,&NbReglesP);

ReglesD=lecture_regles(path_fich,fichreglesD,&NbReglesD);
ReglesL=lecture_regles(path_fich,fichreglesL,&NbReglesL);
ReglesE=lecture_regles(path_fich,fichreglesE,&NbReglesE);
ReglesNP1=lecture_regles(path_fich,fichreglesNP1,&NbReglesNP1);
ReglesNP2=lecture_regles(path_fich,fichreglesNP2,&NbReglesNP2);
ReglesNP3=lecture_regles(path_fich,fichreglesNP3,&NbReglesNP3);
ReglesNP4=lecture_regles(path_fich,fichreglesNP4,&NbReglesNP4);
ReglesNP5=lecture_regles(path_fich,fichreglesNP5,&NbReglesNP5);
ReglesNP6=lecture_regles(path_fich,fichreglesNP6,&NbReglesNP6);
ReglesNP7=lecture_regles(path_fich,fichreglesNP7,&NbReglesNP7);
ReglesNP8=lecture_regles(path_fich,fichreglesNP8,&NbReglesNP8);

if (siafregles)  { affiche_regles(ReglesP,NbReglesP); exit(0); }

/*  Charge la liste d'exceptions  */
if (fichExep[0]=='\0') sprintf(fichExep,"%s/data/list_exep",path_fich);
charge_exception(fichExep);

while(fgets(ch1,TailleLigne,stdin))
 {
 trouve_contrainte(ch1,cont,phonav,phonap);
 strcpy(orig,ch1);

 if (af_syllabe) syllabise(ch1,chsyll);

 if (strstr(cont,"SIGLE"))
  sisigle=True;
 else
  sisigle=False;

 enleve_majuscule(orig,ch1);

 /*  On test la presence du mot dans la liste des exceptions avec ou sans 's'  */
 retour=((exepphon=trouve_exception(ch1))?True:False);
 if ((retour==False)/*&&(SeulMaju(orig))*/) retour=((exepphon=trouve_exception(orig))?True:False);
 if ((retour==False)&&(ch1[strlen(ch1)-1]=='s'))
  {
  strcpy(ch2,ch1);
  ch2[strlen(ch2)-1]='\0';
  retour=((exepphon=trouve_exception(ch2))?True:False);
  }
 if (retour)
  {
  strcpy(ch1,exepphon);
  strcat(cont,"->EXCEPTION");
  }
 else
  {
  sprintf(ch2,"%s",ch1);  /*sprintf(ch2,"  %s  ",ch1);*/

  if (sisigle)
   /*================= phonetisation d'un sigle ================*/
   {
   if (si_syllabe) syllabise(ch2,ch1); else strcpy(ch1,ch2);
   if (SeeReg) printf("Syllabisation : %s\n",ch1);
   sprintf(ch2,"  %s  ",ch1);

   if ((!phonetise(ch2,cont,ch1,ReglesD,NbReglesD))||(ch1[0]=='E'))
    /*  On eppelle  */
    {
    if (SeeReg) printf("%s  eppelle\n",orig);
    retour=phonetise(ch2,cont,ch1,ReglesE,NbReglesE);
    strcat(cont,"->SIGLE_EPELE");
    }
   else
    /*  On lit  */
    {
    if (SeeReg) printf("%s  lu\n",orig);
    retour=phonetise(ch2,cont,ch1,ReglesL,NbReglesL);
    strcat(cont,"->SIGLE_LU");
    }
   }
  else
   if (pointeur=strstr(cont,"FILS_FIL"))  /* Le cas de l'ambiguite sur "fils" */
    {
    if (pointeur[8]=='S') strcpy(ch1,"ffiiss"); else strcpy(ch1,"ffiill");
    retour=True;
    }
   else
   {
   if (strstr(cont,"CHIF_ROMA"))  /* Le cas des chiffres romains  */
    {
    si_romain=1;
    if (!TransNombreRomain(orig,ch2))
     { printf("%s n'est pas un nombre romain ....\n",ch2); exit(0); }
    if (SeeReg) printf("Chiffre romain : %s\n",ch2);
    }
   else si_romain=0;

   if (si_syllabe) syllabise(ch2,ch1); else strcpy(ch1,ch2);
   if (SeeReg) printf("Syllabisation : %s\n",ch1);
   sprintf(ch2,"  %s  ",ch1);

   if ((si_romain==0) && (orig[0]>='A')&&(orig[0]<='Z') &&
       ((!strncmp(cont,"MOTINC",6))||(cont[0]=='X')))
    {          /*============= phonetisation d'un nom propre  =================*/
    switch (ChoisiLangue(cont,orig))
     {
     case 1 : retour=phonetise(ch2,cont,ch1,ReglesNP1,NbReglesNP1); break;
     case 2 : retour=phonetise(ch2,cont,ch1,ReglesNP2,NbReglesNP2); break;
     case 3 : retour=phonetise(ch2,cont,ch1,ReglesNP3,NbReglesNP3); break;
     case 4 : retour=phonetise(ch2,cont,ch1,ReglesNP4,NbReglesNP4); break;
     case 5 : retour=phonetise(ch2,cont,ch1,ReglesNP5,NbReglesNP5); break;
     case 6 : retour=phonetise(ch2,cont,ch1,ReglesNP6,NbReglesNP6); break;
     case 7 : retour=phonetise(ch2,cont,ch1,ReglesNP7,NbReglesNP7); break;
     case 8 : retour=phonetise(ch2,cont,ch1,ReglesNP8,NbReglesNP8); break;
     }
    if (VARIANTE_NP)
     {
     indice_tabl_NP=1;

     retour=phonetise(ch2,cont,tabl_NP[0],ReglesNP1,NbReglesNP1);
     for (j=1;(j<indice_tabl_NP)&&(strcmp(tabl_NP[0],tabl_NP[j]));j++);
     if (j==indice_tabl_NP) strcpy(tabl_NP[indice_tabl_NP++],tabl_NP[0]);

     retour=phonetise(ch2,cont,tabl_NP[0],ReglesNP2,NbReglesNP2);
     for (j=1;(j<indice_tabl_NP)&&(strcmp(tabl_NP[0],tabl_NP[j]));j++);
     if (j==indice_tabl_NP) strcpy(tabl_NP[indice_tabl_NP++],tabl_NP[0]);

     retour=phonetise(ch2,cont,tabl_NP[0],ReglesNP3,NbReglesNP3);
     for (j=1;(j<indice_tabl_NP)&&(strcmp(tabl_NP[0],tabl_NP[j]));j++);
     if (j==indice_tabl_NP) strcpy(tabl_NP[indice_tabl_NP++],tabl_NP[0]);

     retour=phonetise(ch2,cont,tabl_NP[0],ReglesNP4,NbReglesNP4);
     for (j=1;(j<indice_tabl_NP)&&(strcmp(tabl_NP[0],tabl_NP[j]));j++);
     if (j==indice_tabl_NP) strcpy(tabl_NP[indice_tabl_NP++],tabl_NP[0]);

     retour=phonetise(ch2,cont,tabl_NP[0],ReglesNP5,NbReglesNP5);
     for (j=1;(j<indice_tabl_NP)&&(strcmp(tabl_NP[0],tabl_NP[j]));j++);
     if (j==indice_tabl_NP) strcpy(tabl_NP[indice_tabl_NP++],tabl_NP[0]);

     retour=phonetise(ch2,cont,tabl_NP[0],ReglesNP6,NbReglesNP6);
     for (j=1;(j<indice_tabl_NP)&&(strcmp(tabl_NP[0],tabl_NP[j]));j++);
     if (j==indice_tabl_NP) strcpy(tabl_NP[indice_tabl_NP++],tabl_NP[0]);

     retour=phonetise(ch2,cont,tabl_NP[0],ReglesNP7,NbReglesNP7);
     for (j=1;(j<indice_tabl_NP)&&(strcmp(tabl_NP[0],tabl_NP[j]));j++);
     if (j==indice_tabl_NP) strcpy(tabl_NP[indice_tabl_NP++],tabl_NP[0]);

     retour=phonetise(ch2,cont,tabl_NP[0],ReglesNP8,NbReglesNP8);
     for (j=1;(j<indice_tabl_NP)&&(strcmp(tabl_NP[0],tabl_NP[j]));j++);
     if (j==indice_tabl_NP) strcpy(tabl_NP[indice_tabl_NP++],tabl_NP[0]);

     strcat(cont,"->{");
     for(j=1;j<indice_tabl_NP;j++)
      {
      strcat(cont,tabl_NP[j]);
      if (j==indice_tabl_NP-1) strcat(cont,"}"); else strcat(cont,",");
      }
     }
    }

   if (retour==False)
    { /*===================   phonetisation classique ==================*/
    retour=phonetise(ch2,cont,ch1,ReglesP,NbReglesP);
    }
   }
  }
 if (retour)
  {
  nettoie_phon(ch1,ch2);
  if (!sisigle)
   modif_liaison(ch2,ch1,phonav,phonap);
  else
   strcpy(ch1,ch2);

  if (!strcmp(ch1,"||")) strcpy(ch1,"##||");

  /*fprintf(stderr,"WAWA: ch=[%s] cont=[%s]\n",ch1,cont);*/

  
  if (AfLiaison)
   printf("%s %s [%s]",orig,ch1,cont);
   /*printf("%s %s %s %s",orig,ch1,cont,finch);*/
  else printf("%s",ch1);
  }
 else
  {
  if (Ponctu) GenerePonctuation(orig,ch1);
  else /*strcpy(ch1,"    "); else*/ strcpy(ch1,"????");
  if (AfLiaison)
   printf("%s %s [%s]",orig,ch1,cont);
   /*printf("%s %s %s %s",orig,ch1,cont,finch);*/
  else
   printf("%s",ch1);
  }

 if (af_syllabe)
  {
  if (chsyll[0]) printf(" :%s",chsyll); else printf(" :%s",orig);
  }
 printf("\n");
 }

/* nettoyage */
delete_charge_exception();
delete_tabl_regles(ReglesP,NbReglesP);
delete_tabl_regles(ReglesD,NbReglesD);
delete_tabl_regles(ReglesL,NbReglesL);
delete_tabl_regles(ReglesE,NbReglesE);
delete_tabl_regles(ReglesNP1,NbReglesNP1);
delete_tabl_regles(ReglesNP2,NbReglesNP2);
delete_tabl_regles(ReglesNP3,NbReglesNP3);
delete_tabl_regles(ReglesNP4,NbReglesNP4);
delete_tabl_regles(ReglesNP5,NbReglesNP5);
delete_tabl_regles(ReglesNP6,NbReglesNP6);
delete_tabl_regles(ReglesNP7,NbReglesNP7);
delete_tabl_regles(ReglesNP8,NbReglesNP8);

exit(0);
}
 
