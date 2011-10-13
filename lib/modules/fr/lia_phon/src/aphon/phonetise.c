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
/*  Phonetise une chaine deja syllabisee  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <declphon.h>
#include <phonetise.h>

#define MaxWindow	16

int SeeReg,AfFrontiere;

/*................................................................*/

#define min(a,b)	((a)<(b)?(a):(b))
#define max(a,b)	((a)<(b)?(b):(a))

/*................................................................*/

/*  Trie les regles phonetiques  */

int score_chaine(ch)
 char *ch;
{
int n,i=0;
char c;

for(n=0,i=0;ch[n];n++)
 {
 c=ch[n];
 if ((c=='à')||(c=='â')||(c=='ä')) c='a';
 if ((c=='é')||(c=='è')||(c=='ê')||(c=='ë')) c='e';
 if ((c=='î')||(c=='ï')) c='i';
 if ((c=='ô')||(c=='ö')) c='o';
 if ((c=='û')||(c=='ü')) c='u';
 if (c=='ç') c='c';
 if ((c>='A')&&(c<='Z')) c='z';
 i+=(400-(int)c);
 }
return i;
}

void poids_regle(seat,pgauche,pdroite,cont,p1,p2,p3)
 char *seat,*pgauche,*pdroite,*cont;
 int *p1,*p2,*p3;
{
char ch[TailleLigne];

sprintf(ch,"%s%s",seat,pdroite);
*p1=(strlen(ch)*1000)+score_chaine(ch);
*p2=(strlen(pgauche)*1000)+score_chaine(pgauche);
*p3=(strlen(cont)*1000)+score_chaine(cont);

/*printf("#%s#%s#%s# -> p1=%d p2=%d p3=%d\n",ch,pgauche,cont,*p1,*p2,*p3);*/
}

/*  Fonctions d'allocation memoire  */

ty_regles creer_regles(pgauche,pdroite,seat,phon,cont,exemple)
 char *pgauche,*pdroite,*seat,*phon,*cont,*exemple;
{
ty_regles pt;

pt=(ty_regles) malloc(sizeof(struct type_regles));
pt->pgauche=strdup(pgauche);
pt->pdroite=strdup(pdroite);
pt->seat=strdup(seat);
pt->phon=strdup(phon);
pt->cont=strdup(cont);
pt->exemple=strdup(exemple);
poids_regle(seat,pgauche,pdroite,cont,&(pt->p1),&(pt->p2),&(pt->p3));
return pt;
}

void delete_regle(ty_regles pt)
{
if (pt)
 {
 if (pt->pgauche) free(pt->pgauche);
 if (pt->pdroite) free(pt->pdroite);
 if (pt->seat) free(pt->seat);
 if (pt->phon) free(pt->phon);
 if (pt->cont) free(pt->cont);
 if (pt->exemple) free(pt->exemple);
 free(pt);
 }
}

void delete_tabl_regles(ty_regles *tabl,int nb)
{
int i;
if (tabl)
 {
 for(i=0;i<nb;i++) delete_regle(tabl[i]);
 free(tabl);
 }
}

/*  Lecture d'une regle de phonetisation  */

int infe_poids(p1,p2,p3,q1,q2,q3)
 int p1,p2,p3,q1,q2,q3;
{
if (p1<q1) return True;
if (p1==q1)
 {
 if (p2<q2) return True;
 if (p2==q2)
  if (p3<q3) return True;
  else return False;
 else return False;
 }
else return False;
}

void insere_regle(pt,Nb_Regles,Regles)
 ty_regles pt, *Regles;
 int Nb_Regles;
{
int n,i;

for (i=0;i<Nb_Regles;i++)
 if (infe_poids(Regles[i]->p1,Regles[i]->p2,Regles[i]->p3,
		pt->p1,pt->p2,pt->p3))
  {
  for (n=Nb_Regles;n>i;n--)
   Regles[n]=Regles[n-1];
  Regles[i]=pt;
  return;
  }
Regles[Nb_Regles]=pt;
}

int decoupe_string(d,ch,resu,n_ligne)
 char d,*ch,*resu;
 int n_ligne;
{
int n;

for(n=0;(ch[n])&&(ch[n]!=d);n++) resu[n]=ch[n];
if (!ch[n])
 {
 printf("%s Bad Format, desole pour vous .... a la ligne %d\n",ch,n_ligne);
 exit(0);
 }
resu[n]='\0';
return n;
}

void nettoie_p(ch,resu)
 char *ch,*resu;
{
int n,i=0;

if ((ch[0]!='l')||((!si_type_graph(TablNomb,ch+1))&&(!ch[1]=='\0')))
 for(n=0,i=0;ch[n];n++)
  if ((ch[n]=='"')||(ch[n]=='.'))
   ;
  else
   if ((ch[n]=='C')||(ch[n]=='V')||(ch[n]=='S')
       ||(ch[n]=='O')||(ch[n]=='L')||(ch[n]=='E'))
    {
    resu[i++]=ch[n];
    if (si_type_graph(TablNomb,ch+n+1)) n++;
    }
   else
    resu[i++]=ch[n];
resu[i]='\0';
}

void nettoie_phon(ch,resu)
 char *ch,*resu;
{
int n,i;

for(n=0,i=0;ch[n];n++)
 if (ch[n]!=' ') resu[i++]=ch[n];
resu[i]='\0';
}

void modif_liaison(ch1,ch2,avphon,apphon)
 char *ch1,*ch2,*avphon,*apphon;
{
int n,i,t,tc;
char chad[TailleLigne];

chad[0]='\0';
for (i=0;avphon[i];i++) ch2[i]=avphon[i];
strcpy(ch2+i,ch1);
t=strlen(ch2);
for (n=0;apphon[n];)
 if (apphon[n]=='-')
  {
  for(++n;(apphon[n]==ch2[t-2])&&(apphon[n+1]==ch2[t-1]);n+=2)
   { t-=2; ch2[t]='\0'; }
  if ((apphon[n])&&(apphon[n]!='-')&&(apphon[n]!='+'))
   { printf("ERREUR - ERREUR : %s ne finit pas par %s\n",ch1,apphon); exit(0); }
  }
 else
  if(apphon[n]=='+')
   {
   for (++n;(apphon[n])&&(apphon[n+1])&&(apphon[n]!='-')&&(apphon[n]!='+');n+=2)
    {
    tc=strlen(chad);
    strncpy(chad+tc,apphon+n,2);
    chad[tc+2]='\0';
    }
   }
  else
   if ((apphon[n]=='|')&&(apphon[n+1]=='|'))
    {
    strcat(chad,apphon+n);
    n+=2;
    }
  else
   { printf("ERREUR - ERREUR : mauvais format pour apphon : %s\n",apphon); exit(0); }
strcat(ch2,chad);
}

ty_regles *lecture_regles(pathfic,nomfic,NbRegles)
 char *pathfic,*nomfic;
 int *NbRegles;
{
ty_regles *Regles;
FILE *file;
char ch[TailleLigne],temp[TailleLigne],
     pgauche[TailleLigne],pdroite[TailleLigne],
     seat[TailleLigne],phon[TailleLigne],cont[TailleLigne],exemple[TailleLigne];
int n,i,nb,n_ligne;

sprintf(ch,"%s%s",pathfic,nomfic);
/*printf("ouverture de %s\n", ch);*/
if (!(file=fopen(ch,"rt")))
 {
 printf("Impossible d'ouvrir le fichier regles : %s\n",ch);
 exit(0);
 }
 else /*printf("        ok\n")*/;
for(nb=0;fgets(ch,TailleLigne,file);) if (ch[0]=='r') nb++;
 
(*NbRegles)=nb;
fseek(file,0,SEEK_SET);
Regles=(ty_regles *) malloc(nb * sizeof(ty_regles));
/*printf("Lecture des regles en cours :\n");*/
for(i=n_ligne=0;fgets(ch,TailleLigne,file);n_ligne++)
 if (!strncmp(ch,"regle",5))
  {
  /*if (!(i%150)) printf("  %d%%\n",(int)((i*100)/(*NbRegles)));*/

  pgauche[0]=pdroite[0]=seat[0]=phon[0]=cont[0]=exemple[0]='\0';
  n=decoupe_string('<',ch,temp,n_ligne)+1;
  n+=decoupe_string(',',ch+n,temp,n_ligne)+2;
  nettoie_p(temp,pgauche);
  n+=decoupe_string('"',ch+n,seat,n_ligne)+2;
  n+=decoupe_string('>',ch+n,temp,n_ligne)+3;
  nettoie_p(temp,pdroite);
  n+=decoupe_string('"',ch+n,phon,n_ligne)+3;
  n+=decoupe_string('"',ch+n,cont,n_ligne)+3;
  n+=decoupe_string('"',ch+n,exemple,n_ligne);
  insere_regle(creer_regles(pgauche,pdroite,seat,phon,cont,exemple),i++,Regles);
  }
/*printf("  100%%\nNombre de regles lues : %d\n",i);*/
/*printf("\n");*/
fclose(file);
return(Regles);
}

/*  Phonetise  */

int compatible_char(c1,c2)
 char c1,*c2;  /*  La regle est dans c1  */
{
if (c1==(*c2)) return True;
switch (c1)
 {
 case 'V' :
	if (si_type_graph(TablVoye,c2)) return 1;
	break;
 case 'C' :
	if (si_type_graph(TablCons,c2)) return 1;
	break;
 case 'O' :
	if (si_type_graph(TablOclu,c2)) return 1;
	break;
 case 'L' :
	if (si_type_graph(TablLiqu,c2)) return 1;
	break;
 case 'S' :
	if (c2[0]==' ') return 1;
	if ((c2[0]=='s')&&(c2[1]==' ')) return 2;
	break;
 case 'E' :
	if (c2[0]==' ') return 1;
	if ((c2[0]=='e')&&(c2[1]==' ')) return 2;
	break;
 }
return 0;
}

int verifie_pgauche(pg1,pg2)
 char *pg1,*pg2;
{
int i,j,depl;

/*printf("compatible gauche : #%s# #%s#\n",pg1,pg2);*/

if ((*pg1=='\0')&&(*pg2=='\0')) return True;
for (i=strlen(pg1)-1,j=strlen(pg2)-1;(i>=0)&&(j>=0)&&
	(depl=compatible_char(pg1[i],pg2+j));i--,j-=depl);
if (i<0) return True;
return False;
}

int verifie_seatdroite(seat,pdroite,ch)
 char *seat,*pdroite,*ch;
{
int i,j,k,depl;

/*printf("compatible seatdroite : #%s#%s#%s#\n",seat,pdroite,ch);*/

for (i=0,k=0;(seat[i])&&(ch[k])&&(depl=compatible_char(seat[i],ch+k));i++,k+=depl);
if (seat[i]) return False;
for(j=0;(pdroite[j])&&(ch[k])&&(depl=compatible_char(pdroite[j],ch+k));j++,k+=depl);
if (pdroite[j]) return False;
return True;
}

int verifie_contrainte(cont_r,cont)
 char *cont_r,*cont;
{
if ((*cont_r=='\0')||(*cont=='\0')) return True;

/*  Verifie les contraintes : (V ou NV) et ADV */
if (!strcmp(cont_r,cont)) return True;
if ((!strcmp(cont_r,"V3P"))&&((!strcmp(cont,"VA3P"))||(!strcmp(cont,"VE3P")))) return True;
if ((!strcmp(cont_r,"V3S"))&&((!strcmp(cont,"VA3S"))||(!strcmp(cont,"VE3S")))) return True;
if ((cont_r[0]=='V')&&(cont_r[1]=='\0'))
 if (cont[0]=='V') return True; else return False;
if (!strcmp(cont_r,"NV"))
 if (cont[0]!='V') return True; else return False;
return False;
}

int verifie_contrainte_OLD(cont_r,cont)
 char *cont_r,*cont;
{
char reponse[TailleLigne];

if (*cont_r=='\0') return True;
if (*cont=='\0')
 {  return True;
 /*  Verification manuelle  */
 if (!strcmp(cont_r,"L"))
  { return False; printf("\nEst-ce qu'on fait la liaison ? (o/n) \n"); }
 else
  printf("\nEst-ce que le mot est un %s ? (o/n) \n",cont_r);
 fgets(reponse,TailleLigne,stdin);
 if ((reponse[0]=='o')||(reponse[0]=='O')) return True;
 return False;
 }
/*  Verifie les contraintes : L ou (V NV V3S V3P AD NA)  */
if (!strcmp(cont_r,cont)) return True;  /*  Pour V3S et V3P  */
if ((!strcmp(cont_r,"V3P"))&&((!strcmp(cont,"VA3P"))||(!strcmp(cont,"VE3P")))) return True;
if ((!strcmp(cont_r,"V3S"))&&((!strcmp(cont,"VA3S"))||(!strcmp(cont,"VE3S")))) return True;
if (!strcmp(cont_r,"V"))
 if (cont[0]=='V') return True; else return False;
if (!strcmp(cont_r,"NV"))
 if (cont[0]!='V') return True; else return False;
if (!strcmp(cont_r,"AD"))
 if (!strncmp(cont,"ADV",3)) return True; else return False;
if (!strcmp(cont_r,"NA"))
 if ((strncmp(cont,"VA",2))&&(strncmp(cont,"VE",2))) return True; else return False;
return False;
}

int compatible_pattern(posi,ch,pgauche,cont,Regles)
 int posi;
 char *ch,*pgauche,*cont;
 ty_regles *Regles;

{
/*  On verifie la partie gauche  */
if (!verifie_pgauche(Regles[posi]->pgauche,pgauche))
 return False;
if (!verifie_seatdroite(Regles[posi]->seat,Regles[posi]->pdroite,ch))
 return False;
if (!verifie_contrainte(Regles[posi]->cont,cont))
 return False;
return True;
}

int dichotomie_posi(p1,debr,finr,Regles)
 int p1,debr,finr;
 ty_regles *Regles;

{
int posi;

while (finr>=debr)
 {
 /*
 printf("recherche entre %d et %d (borne : %d -> %d)\n",debr,finr,
	Regles[debr]->p1,Regles[finr]->p1);
 */
 posi=(int)((finr-debr+1)/2)+debr;
 if (Regles[posi]->p1==p1)
  {
  /*  On revient au premier p1 de la classe  */
  while ((posi>0)&&(Regles[posi]->p1==p1)) posi--;
  if (Regles[posi]->p1!=p1) posi++;
  /*printf("On a trouve un p1 Ok en %d (avec p2=%d)\n",posi,Regles[posi]->p2);*/
  return posi;
  }
 else
  if (Regles[posi]->p1>p1)
   debr=posi+1;
  else
   finr=posi-1;
 }
return debr;
}

int calcule_limite(ch,twin)
 char *ch;
 int twin;
{
int n;

n=strlen(ch);
if ((ch[n-1]==' ')&&(ch[n-2]==' ')&&(ch[n-3]=='s')) twin--;
return ((twin*1000)+(twin*(400-(int)'z')));
}

int trouve_dicho_regle(ch,pgauche,cont,debr,finr,twin,tg,newlin,Regles,NbRegles)
 char *ch,*pgauche,*cont;
 int debr,finr,twin,tg,*newlin;
 ty_regles *Regles;
 int NbRegles;
{
int p1,posi,limite;

p1=(twin*1000)+score_chaine(ch);
limite=calcule_limite(ch,twin);

/*  Recherche dichotomique sur p1  */
posi=dichotomie_posi(p1,debr,finr,Regles);
for (;(posi<NbRegles)&&(Regles[posi]->p1>=limite);posi++)
 if (compatible_pattern(posi,ch,pgauche,cont,Regles))
  return posi+1;
*newlin=posi;
return False;
}

int phonetise(ch_entree,cont,ch_sortie,Regles,NbRegles)
 char *ch_entree,*cont,*ch_sortie;
 ty_regles *Regles;
 int NbRegles;
{
int n,posi,twin,debr;
char ch[TailleLigne],*pgauche,sauvchar;

if (SeeReg) printf("Règles utilisées : ");
ch_sortie[0]='\0';
for (n=0;(ch_entree[n])&&(ch_entree[n]!='\n');)
 {
 if (ch_entree[n]==' ') n++;
 else
  {
  twin=min(MaxWindow,strlen(ch_entree+n));
  strncpy(ch,ch_entree+n,twin+1);
  sauvchar=ch_entree[n];
  ch_entree[n]='\0';
  pgauche=ch_entree;
  debr=0;
  for(posi=0;(posi==0)&&(twin>0);twin--)
   {
   ch[twin]='\0';

   posi=trouve_dicho_regle(ch,pgauche,cont,debr,NbRegles-1,twin,n,&debr,Regles,NbRegles);

   }
  ch_entree[n]=sauvchar;
  if (posi==0) { /*printf("On a echoue, desole [%s]\n",ch_entree);*/return False; }
  if (SeeReg) printf("%d ",posi);
  if (AfFrontiere)
   {
   strcat(ch_sortie,Regles[posi-1]->seat);
   strcat(ch_sortie,"|");
   strcat(ch_sortie,Regles[posi-1]->phon);
   strcat(ch_sortie,"#");
   }
  else strcat(ch_sortie,Regles[posi-1]->phon);
  n+=strlen(Regles[posi-1]->seat);
  }
 }
if (SeeReg) printf("\n");
return True;
}

void ChangeIIPartie(pg,nbC,nbV,nbS,nbO,nbLi,nbE,nbL)
 char *pg;
 int *nbC,*nbV,*nbS,*nbO,*nbLi,*nbE,*nbL;
{
char c1[TailleLigne];
int n=0,i=0;

if (pg[n]=='\0')
 {
 if ((*nbL)==0) sprintf(c1,"l");
 else sprintf(c1,"l%d",(*nbL));
 (*nbL)++;
 }
else
 {
 for(n=0,i=0;pg[n];n++)
  switch (pg[n])
   {
   case 'C' :
	if ((i!=0)&&(c1[i-1]!='.'))
	 { c1[i++]='"'; c1[i++]='.'; }
	c1[i++]=pg[n];
	if ((*nbC)!=0) c1[i++]=(char) ('0'+(*nbC));
	c1[i++]='.';
	(*nbC)++;
	break;
   case 'V' :
	if ((i!=0)&&(c1[i-1]!='.'))
	 { c1[i++]='"'; c1[i++]='.'; }
	c1[i++]=pg[n];
	if ((*nbV)!=0) c1[i++]=(char) ('0'+(*nbV));
	c1[i++]='.';
	(*nbV)++;
	break;
   case 'S' :
	if ((i!=0)&&(c1[i-1]!='.'))
	 { c1[i++]='"'; c1[i++]='.'; }
	c1[i++]=pg[n];
	if ((*nbS)!=0) c1[i++]=(char) ('0'+(*nbS));
	c1[i++]='.';
	(*nbS)++;
	break;
   case 'O' :
	if ((i!=0)&&(c1[i-1]!='.'))
	 { c1[i++]='"'; c1[i++]='.'; }
	c1[i++]=pg[n];
	if ((*nbO)!=0) c1[i++]=(char) ('0'+(*nbO));
	c1[i++]='.';
	(*nbO)++;
	break;
   case 'L' :
	if ((i!=0)&&(c1[i-1]!='.'))
	 { c1[i++]='"'; c1[i++]='.'; }
	c1[i++]=pg[n];
	if ((*nbLi)!=0) c1[i++]=(char) ('0'+(*nbLi));
	c1[i++]='.';
	(*nbLi)++;
	break;
   case 'E' :
	if ((i!=0)&&(c1[i-1]!='.'))
	 { c1[i++]='"'; c1[i++]='.'; }
	c1[i++]=pg[n];
	if ((*nbE)!=0) c1[i++]=(char) ('0'+(*nbE));
	c1[i++]='.';
	(*nbE)++;
	break;
   default :
	if ((i==0)||(c1[i-1]=='.')) c1[i++]='"';
	c1[i++]=pg[n];
   }
 if (c1[i-1]=='.')
  c1[i-1]='\0';
 else
  { c1[i]='"'; c1[i+1]='\0'; }
 }
strcpy(pg,c1);
}

void ChangePartie(pg,pd)
 char *pg,*pd;
{
int nbC,nbV,nbS,nbO,nbLi,nbE,nbL;

nbC=nbV=nbS=nbL=0;
ChangeIIPartie(pg,&nbC,&nbV,&nbS,&nbO,&nbLi,&nbE,&nbL);
ChangeIIPartie(pd,&nbC,&nbV,&nbS,&nbO,&nbLi,&nbE,&nbL);
}

void affiche_regles_OLD(Regles,NbRegles)
ty_regles *Regles;
int NbRegles;
{
int i,j;
char pgauche[TailleLigne],pdroite[TailleLigne];

for (i=0;i<NbRegles;i++)
 {
 strcpy(pgauche,Regles[i]->pgauche);
 strcpy(pdroite,Regles[i]->pdroite);
 ChangePartie(pgauche,pdroite);
 /*
 printf("regle(00,<%s,\"%s\",%s>,\"%s\",\"%s\",\"%s\") ->",
	pgauche,Regles[i]->seat,pdroite,
	Regles[i]->phon,Regles[i]->cont,Regles[i]->exemple);
 */
 printf("regle(%d,<%s,\"%s\",%s>,\"%s\",\"%s\",\"%s\") ->",
	i+1,pgauche,Regles[i]->seat,pdroite,
	Regles[i]->phon,Regles[i]->cont,Regles[i]->exemple);

 for (j=0;pgauche[j];j++)
   if ((pgauche[j]=='C')||(pgauche[j]=='V')||(pgauche[j]=='S')
       ||(pgauche[j]=='O')||(pgauche[j]=='L')||(pgauche[j]=='E'))
    if (si_type_graph(TablNomb,pgauche+j+1))
     printf("\n        classe(%c%c,\"%c\")",pgauche[j],pgauche[j+1],pgauche[j]);
    else
     printf("\n        classe(%c,\"%c\")",pgauche[j],pgauche[j]);
 for (j=0;pdroite[j];j++)
   if ((pdroite[j]=='C')||(pdroite[j]=='V')||(pdroite[j]=='S')
       ||(pdroite[j]=='O')||(pdroite[j]=='L')||(pdroite[j]=='E'))
    if (si_type_graph(TablNomb,pdroite+j+1))
     printf("\n        classe(%c%c,\"%c\")",pdroite[j],pdroite[j+1],pdroite[j]);
    else
     printf("\n        classe(%c,\"%c\")",pdroite[j],pdroite[j]);
 printf(" ;\n");
 }
}

void affiche_regles(Regles,NbRegles)
ty_regles *Regles;
int NbRegles;
{
int i;
char pgauche[TailleLigne],pdroite[TailleLigne];

for (i=0;i<NbRegles;i++)
 {
 strcpy(pgauche,Regles[i]->pgauche);
 strcpy(pdroite,Regles[i]->pdroite);
 ChangePartie(pgauche,pdroite);
 /*
 printf("regle(00,<%s,\"%s\",%s>,\"%s\",\"%s\",\"%s\") ->",
        pgauche,Regles[i]->seat,pdroite,
        Regles[i]->phon,Regles[i]->cont,Regles[i]->exemple);
 */
 printf("regle(%d,<%s,\"%s\",%s>,\"%s\",\"%s\",\"%s\") ->",
        i+1,pgauche,Regles[i]->seat,pdroite,
        Regles[i]->phon,Regles[i]->cont,Regles[i]->exemple);
 /*
 for (j=0;pgauche[j];j++)
   if ((pgauche[j]=='C')||(pgauche[j]=='V')||(pgauche[j]=='S')
       ||(pgauche[j]=='O')||(pgauche[j]=='L')||(pgauche[j]=='E'))
    if (si_type_graph(TablNomb,pgauche+j+1))
     printf("\n        classe(%c%c,\"%c\")",pgauche[j],pgauche[j+1],pgauche[j]);
    else
     printf("\n        classe(%c,\"%c\")",pgauche[j],pgauche[j]);
 for (j=0;pdroite[j];j++)
   if ((pdroite[j]=='C')||(pdroite[j]=='V')||(pdroite[j]=='S')
       ||(pdroite[j]=='O')||(pdroite[j]=='L')||(pdroite[j]=='E'))
    if (si_type_graph(TablNomb,pdroite+j+1))
     printf("\n        classe(%c%c,\"%c\")",pdroite[j],pdroite[j+1],pdroite[j]);
    else
     printf("\n        classe(%c,\"%c\")",pdroite[j],pdroite[j]);
 */
 printf(" ;\n");
 }

}
  
