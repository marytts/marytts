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
/*  Compile un fichier ARPA 2 ou 3 gram  */
/*  FRED 0498  :  Modif Multi ML - 0399  */

#include <libgram.h>

/*................................................................*/

/*  Diver  */

#define TailleLigne	400

void ERREUR(char *ch )
{
fprintf(stderr,"ERREUR : %s\n",ch);
exit(0);
}

void ERREUR2(char *ch1 ,char *ch2 )
{
fprintf(stderr,"ERREUR : %s %s\n",ch1,ch2);
exit(0);
}

/*................................................................*/

/* Recherche des nombres premiers */

/* renvoi le premier nombre premier superieur
   a un nombre donne en argument */

#define MaxPrem	100000

long PremPremier(long maxi )
{
long *TablPrem,nbprem,n,i,limite,last;

TablPrem=(long*)malloc(sizeof(long)*MaxPrem);
limite=(long)ceil(sqrt((double)maxi));
last=1;
nbprem=0;
for (n=2;last<maxi;n++)
 {
 for(i=0;(i<nbprem)&&(n%TablPrem[i]!=0);i++);
 if (i==nbprem)
  {
  if (nbprem==MaxPrem-1) { fprintf(stderr,"MaxPrem trop petit !!\n"); exit(0); }
  last=n;
  if (n<=limite) TablPrem[nbprem++]=n;
  }
 }
free(TablPrem);

if (last==maxi) last=PremPremier(maxi+1); /* pour le cas ou maxi est deja premier !! */
/*printf("Le premier nombre premier superieur a %d est %d\n",maxi,last);*/
return last;
}

/*................................................................*/

/*  Remplissage des NGrams  */

int Range1Gram(wrd_index_t i_mot ,flogprob_t lp ,flogprob_t fr ,ty_ml pt_ml)
{
wrd_index_t essai,indice,valh1,valh2;

indice=valh1=H1Value(i_mot,pt_ml);
valh2=Double1H(i_mot,pt_ml);
for (essai=1;(indice<=pt_ml->NB1GRAM)&&(essai<=pt_ml->NB1GRAM)&&
	(Case1Vide(indice,pt_ml)==0)&&(!SiEgal1(i_mot,indice,pt_ml));essai++)
 indice=EssaiSuivant(valh1,valh2,essai,pt_ml->NB1GRAM);

if (indice>pt_ml->NB1GRAM) { fprintf(stderr,"HOLA1 : indice=%ld\n",indice); exit(0); }

if (essai>pt_ml->NB1GRAM) ERREUR("Le tableau de 1-gram est plein ....\n");

if (Case1Vide(indice,pt_ml)==0) return 0; /* le 1-gram etait deja present */

pt_ml->TABL1GRAM[indice].cle[0]=BYTE1(i_mot);
pt_ml->TABL1GRAM[indice].cle[1]=BYTE2(i_mot);
pt_ml->TABL1GRAM[indice].cle[2]=BYTE3(i_mot);

pt_ml->TABL1GRAM[indice].lp=lp;
pt_ml->TABL1GRAM[indice].fr=fr;

return 1;
}

int Range2Gram(wrd_index_t i_mot1 ,wrd_index_t i_mot2 ,flogprob_t lp ,flogprob_t fr ,ty_ml pt_ml)
{
wrd_index_t essai,indice,valh1,valh2;

indice=valh1=H2Value(i_mot1,i_mot2,pt_ml);
valh2=Double2H(i_mot1,i_mot2,pt_ml);

/*printf("********************************\n");*/
for (essai=1;(indice<=pt_ml->NB2GRAM)&&(essai<=pt_ml->NB2GRAM)&&
	(Case2Vide(indice,pt_ml)==0)&&(!SiEgal2(i_mot1,i_mot2,indice,pt_ml));essai++)
 {
 /*printf("%d = indice ; essai=%d/%d XXXX\n",indice,essai,pt_ml->NB2GRAM);*/

 indice=EssaiSuivant(valh1,valh2,essai,pt_ml->NB2GRAM);
 }

if (indice>pt_ml->NB2GRAM) { fprintf(stderr,"HOLA2 : indice=%ld\n",indice); exit(0); }

if (essai>pt_ml->NB2GRAM) ERREUR("Le tableau de 2-gram est plein ....\n");

if (Case2Vide(indice,pt_ml)==0) return 0; /* le 2-gram etait deja present */

pt_ml->TABL2GRAM[indice].cle[0]=BYTE1(i_mot1);
pt_ml->TABL2GRAM[indice].cle[1]=BYTE2(i_mot1);
pt_ml->TABL2GRAM[indice].cle[2]=BYTE3(i_mot1);

pt_ml->TABL2GRAM[indice].cle[3]=BYTE1(i_mot2);
pt_ml->TABL2GRAM[indice].cle[4]=BYTE2(i_mot2);
pt_ml->TABL2GRAM[indice].cle[5]=BYTE3(i_mot2);

pt_ml->TABL2GRAM[indice].lp=lp;
pt_ml->TABL2GRAM[indice].fr=fr;

return 1;
}

int Range3Gram(wrd_index_t i_mot1 ,wrd_index_t i_mot2 ,wrd_index_t i_mot3 ,flogprob_t lp ,ty_ml pt_ml)
{
wrd_index_t essai,indice,valh1,valh2;

indice=valh1=H3Value(i_mot1,i_mot2,i_mot3,pt_ml);
valh2=Double3H(i_mot1,i_mot2,i_mot3,pt_ml);
for (essai=1;(indice<=pt_ml->NB3GRAM)&&(essai<=pt_ml->NB3GRAM)&&
	(Case3Vide(indice,pt_ml)==0)&&(!SiEgal3(i_mot1,i_mot2,i_mot3,indice,pt_ml));essai++)
 indice=EssaiSuivant(valh1,valh2,essai,pt_ml->NB3GRAM);

if (essai>pt_ml->NB3GRAM) ERREUR("Le tableau de 3-gram est plein ....\n");

if (indice>pt_ml->NB3GRAM) { fprintf(stderr,"HOLA3 : indice=%ld\n",indice); exit(0); }

if (Case3Vide(indice,pt_ml)==0) { fprintf(stderr,"3-gram deja present\n"); return 0; } /* le 3-gram etait deja present */

pt_ml->TABL3GRAM[indice].cle[0]=BYTE1(i_mot1);
pt_ml->TABL3GRAM[indice].cle[1]=BYTE2(i_mot1);
pt_ml->TABL3GRAM[indice].cle[2]=BYTE3(i_mot1);

pt_ml->TABL3GRAM[indice].cle[3]=BYTE1(i_mot2);
pt_ml->TABL3GRAM[indice].cle[4]=BYTE2(i_mot2);
pt_ml->TABL3GRAM[indice].cle[5]=BYTE3(i_mot2);

pt_ml->TABL3GRAM[indice].cle[6]=BYTE1(i_mot3);
pt_ml->TABL3GRAM[indice].cle[7]=BYTE2(i_mot3);
pt_ml->TABL3GRAM[indice].cle[8]=BYTE3(i_mot3);

pt_ml->TABL3GRAM[indice].lp=lp;

return 1;
}

/*................................................................*/

/*  Lecture et Sauvegarde des NGrams  */

void SauveGram(char *chfich , ty_ml pt_ml)
{
FILE *file;
char ch[TailleLigne];

sprintf(ch,"%s.1g",chfich);
if (!(file=fopen(ch,"wb"))) ERREUR2("je ne peux ouvrir",chfich);
fwrite(pt_ml->TABL1GRAM,sizeof(type_1gram),(pt_ml->NB1GRAM+MARGE),file);
fclose(file);
sprintf(ch,"%s.2g",chfich);
if (!(file=fopen(ch,"wb"))) ERREUR2("je ne peux ouvrir",chfich);
fwrite(pt_ml->TABL2GRAM,sizeof(type_2gram),(pt_ml->NB2GRAM+MARGE),file);
/*
fprintf(stderr,"pt_ml->NB2GRAM+MARGE=%d sizeof(type_2gram)=%d total=%d\n",pt_ml->NB2GRAM+MARGE,sizeof(type_2gram),
	(pt_ml->NB2GRAM+MARGE)*sizeof(type_2gram));
*/
fclose(file);

if (pt_ml->GRAM_SI_2G==0)
 {
 sprintf(ch,"%s.3g",chfich);
 if (!(file=fopen(ch,"wb"))) ERREUR2("je ne peux ouvrir",chfich);
 fwrite(pt_ml->TABL3GRAM,sizeof(type_3gram),(pt_ml->NB3GRAM+MARGE),file);
 fclose(file);
 }
}

/*................................................................*/

/*  Initialisation des NGrams  */

ty_ml InitGram(char *nom_darpa ,char *nom_model ,double coef1 ,double coef2 ,double coef3 ,
	int si_dicho ,int si_log_e,int si_2g)
{
FILE *file;
long n,i,nb1,nb2,nb3,t1,t2,t3;
char ch[TailleLigne];
ty_ml pt_ml;

if (!(file=fopen(nom_darpa,"r"))) ERREUR2("je ne peux ouvrir",nom_darpa);
for(fgets(ch,TailleLigne,file);strncmp(ch+1,"data",4);fgets(ch,TailleLigne,file));
/* on lit le nb d'1.2.3-gram */
fgets(ch,TailleLigne,file); sscanf(ch,"ngram 1=%d",&nb1);
fgets(ch,TailleLigne,file); sscanf(ch,"ngram 2=%d",&nb2);

if (si_2g==0)
 {
 fgets(ch,TailleLigne,file);
 if (sscanf(ch,"ngram 3=%d",&nb3)<1)
  {
  fprintf(stderr,"ATTENTION : le fichier arpa est un bigramme !!!!\n");
  exit(0);
  }
 }
fclose(file);

t1=PremPremier((long)((double)nb1/coef1));
t2=PremPremier((long)((double)nb2/coef2));
if (si_2g==0) t3=PremPremier((long)((double)nb3/coef2)); else t3=0;

fprintf(stderr,"Nb de 1-gram : %ld  -  coef : %.2lf  -  Taille du tableau : %ld\n",nb1,coef1,t1);
fprintf(stderr,"Nb de 2-gram : %ld  -  coef : %.2lf  -  Taille du tableau : %ld\n",nb2,coef2,t2);
if (si_2g==0)
 fprintf(stderr,"Nb de 3-gram : %ld  -  coef : %.2lf  -  Taille du tableau : %ld\n",nb3,coef3,t3);

sprintf(ch,"%s.desc",nom_model);
if (!(file=fopen(ch,"w"))) ERREUR2("je ne peux ecrire dans",nom_darpa);
fprintf(file,"%ld %ld %ld\n",t1,t2,t3);
if (si_dicho) fprintf(file,"DICHOTOMIE\n"); else fprintf(file,"HASH\n");
if (si_log_e) fprintf(file,"LOG_E\n"); else fprintf(file,"LOG_10\n");
if (si_2g) fprintf(file,"BIGRAMME\n"); else fprintf(file,"TRIGRAMME\n");
fclose(file);

/* on alloue le ML */
pt_ml=cons_ml(t1,t2,t3,si_dicho?0:1,si_2g,si_log_e?0:1);

for(n=0;n<(t1+MARGE);n++)
 {
 for(i=0;i<Nb1Byte-1;i++) pt_ml->TABL1GRAM[n].cle[i]=0;
 pt_ml->TABL1GRAM[n].cle[i]=(unsigned char)128;
 pt_ml->TABL1GRAM[n].lp=pt_ml->TABL1GRAM[n].fr=0;
 }
for(n=0;n<(t2+MARGE);n++)
 {
 for(i=0;i<Nb2Byte-1;i++) pt_ml->TABL2GRAM[n].cle[i]=0;
 pt_ml->TABL2GRAM[n].cle[i]=(unsigned char)128;
 pt_ml->TABL2GRAM[n].lp=pt_ml->TABL2GRAM[n].fr=0;
 }

if (si_2g==0)
 for(n=0;n<(t3+MARGE);n++)
  {
  for(i=0;i<Nb3Byte-1;i++) pt_ml->TABL3GRAM[n].cle[i]=0;
  pt_ml->TABL3GRAM[n].cle[i]=(unsigned char)128;
  pt_ml->TABL3GRAM[n].lp=0;
  }

fprintf(stderr,"Taille necessaire au modele : %.2lf Mo\n",(double)(sizeof(type_1gram)*(t1+MARGE)+
	sizeof(type_2gram)*(t2+MARGE)+sizeof(type_3gram)*(t3+MARGE))/(double)(1024*1024));

return pt_ml;
}

/*................................................................*/

wrd_index_t mot2indice(char *mot,int si_code, ty_lexique pt_lexique)
{
wrd_index_t code;

if (si_code)
 sscanf(mot,"%ld",&code);
else
 if (!Mot2Code(mot,&code,pt_lexique))
  {
  fprintf(stderr,"Warning : %s inconnu !!\n",mot);
  code=0;
  }
return code;
}

/*  Lecture d'un fichier DARPA et rangement des n-grams  */

void NextItem(char *ch ,char **pt ,char **ptnext )
{
int n;
for(n=0;(ch[n])&&((ch[n]==' ')||(ch[n]=='\t'));n++);
if (ch[n]=='\0') ERREUR2("Mauvaise chaine :",ch);
*pt=ch;
for(;(ch[n])&&(ch[n]!=' ')&&(ch[n]!='\t')&&(ch[n]!='\n');n++);
if (ch[n]=='\0') *ptnext=NULL; else *ptnext=ch+n+1;
ch[n]='\0';
}

void LisFichDARPA(FILE *file , int si_log_e , int si_2g , int si_code, ty_ml pt_ml)
{
char ch[TailleLigne],*pt,*ptnext,*mot1,*mot2,*mot3;
flogprob_t lp,fr;
long i_mot1,i_mot2,i_mot3,nb1,nb2,nb3;

nb1=nb2=nb3=0;
for(fgets(ch,TailleLigne,file);strncmp(ch+1,"data",4);fgets(ch,TailleLigne,file));
for(fgets(ch,TailleLigne,file);strncmp(ch+1,"1-grams:",8);fgets(ch,TailleLigne,file));
for(fgets(ch,TailleLigne,file);ch[0]!='\n';fgets(ch,TailleLigne,file))
 {
 NextItem(ch,&pt,&ptnext);
 sscanf(pt,"%f",&lp);
 if (si_log_e) lp=LOG10_2_LOGe(lp);
 if (ptnext==NULL) ERREUR2("Mauvaise Chaine :",ch);
 NextItem(ptnext,&mot1,&ptnext);
 if (ptnext==NULL) ERREUR2("Mauvaise Chaine :",ch);
 NextItem(ptnext,&pt,&ptnext);
 sscanf(pt,"%f",&fr);
 if (si_log_e) fr=LOG10_2_LOGe(fr);
 i_mot1=mot2indice(mot1,si_code,pt_ml->lexique);
 if (i_mot1<0) fprintf(stderr,"MOTINC : %s\n",mot1);
 else if (Range1Gram(i_mot1,lp,fr,pt_ml)) nb1++;
 if (nb1%10000==0) fprintf(stderr,"  en cours -> %ld\n",nb1);
 }
fprintf(stderr,"On a lu %ld 1-gram\n",nb1);
fprintf(stderr,"Taux de remplissage : %.2f\n",(float)nb1/(float)pt_ml->NB1GRAM);

for(fgets(ch,TailleLigne,file);strncmp(ch+1,"2-grams:",8);fgets(ch,TailleLigne,file));
for(fgets(ch,TailleLigne,file);ch[0]!='\n';fgets(ch,TailleLigne,file))
 {
 NextItem(ch,&pt,&ptnext);
 sscanf(pt,"%f",&lp);
 if (si_log_e) lp=LOG10_2_LOGe(lp);
 if (ptnext==NULL) ERREUR2("Mauvaise Chaine :",ch);
 NextItem(ptnext,&mot1,&ptnext);
 if (ptnext==NULL) ERREUR2("Mauvaise Chaine :",ch);
 NextItem(ptnext,&mot2,&ptnext);
 if (ptnext==NULL) ERREUR2("Mauvaise Chaine :",ch);

 /* Que si modele 3 gram */
 fr=0.;
 if (si_2g==0)
  {
  NextItem(ptnext,&pt,&ptnext);
  if (sscanf(pt,"%f",&fr)<1)
   {
   fprintf(stderr,"ATTENTION : le fichier arpa est un bigramme !!!!\n");
   exit(0);
   }
  if (si_log_e) fr=LOG10_2_LOGe(fr);
  }

 i_mot1=mot2indice(mot1,si_code,pt_ml->lexique);
 i_mot2=mot2indice(mot2,si_code,pt_ml->lexique);
 if (i_mot1<0) fprintf(stderr,"MOTINC : %s\n",mot1);
 else
  if (i_mot2<0) fprintf(stderr,"MOTINC : %s\n",mot2);
  else if (Range2Gram(i_mot1,i_mot2,lp,fr,pt_ml)) nb2++;
 if (nb2%50000==0) fprintf(stderr,"  en cours -> %ld\n",nb2);
 }
fprintf(stderr,"On a lu %ld 2-gram\n",nb2);
fprintf(stderr,"Taux de remplissage : %.2f\n",(float)nb2/(float)pt_ml->NB2GRAM);

if (si_2g==0)
 {
 for(fgets(ch,TailleLigne,file);strncmp(ch+1,"3-grams:",8);fgets(ch,TailleLigne,file));
 for(fgets(ch,TailleLigne,file);ch[0]!='\n';fgets(ch,TailleLigne,file))
  {
  NextItem(ch,&pt,&ptnext);
  sscanf(pt,"%f",&lp);
  if (si_log_e) lp=LOG10_2_LOGe(lp);
  if (ptnext==NULL) ERREUR2("Mauvaise Chaine :",ch);
  NextItem(ptnext,&mot1,&ptnext);
  if (ptnext==NULL) ERREUR2("Mauvaise Chaine :",ch);
  NextItem(ptnext,&mot2,&ptnext);
  if (ptnext==NULL) ERREUR2("Mauvaise Chaine :",ch);
  NextItem(ptnext,&mot3,&ptnext);

  i_mot1=mot2indice(mot1,si_code,pt_ml->lexique);
  i_mot2=mot2indice(mot2,si_code,pt_ml->lexique);
  i_mot3=mot2indice(mot3,si_code,pt_ml->lexique);
  if (i_mot1<0) fprintf(stderr,"MOTINC : %s\n",mot1);
  else
   if (i_mot2<0) fprintf(stderr,"MOTINC : %s\n",mot2);
   else
    if (i_mot3<0) fprintf(stderr,"MOTINC : %s\n",mot3);
    else if (Range3Gram(i_mot1,i_mot2,i_mot3,lp,pt_ml)) nb3++;
  if (nb3%50000==0) fprintf(stderr,"  en cours -> %ld\n",nb3);
  }
 fprintf(stderr,"On a lu %ld 3-gram\n",nb3);
 fprintf(stderr,"Taux de remplissage : %.2f\n",(float)nb3/(float)pt_ml->NB3GRAM);
 }
}

/*................................................................*/

/* Tri des tableaux de Hash pour la dichotomie */

void SortHash(ty_ml pt_ml)
{
qsort(pt_ml->TABL1GRAM,pt_ml->NB1GRAM+MARGE,sizeof(type_1gram),H1compar);
qsort(pt_ml->TABL2GRAM,pt_ml->NB2GRAM+MARGE,sizeof(type_2gram),H2compar);
if (pt_ml->GRAM_SI_2G==0)
 qsort(pt_ml->TABL3GRAM,pt_ml->NB3GRAM+MARGE,sizeof(type_3gram),H3compar);
}
 
/*................................................................*/
 
