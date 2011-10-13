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
/*..........................................*/
/* ooOOoo | Rule Based Transcription System */
/*-RUMBAS-|---------------------------------*/
/* ooOOoo |     FRED  -  LIA 1996           */
/*..........................................*/

/*  Version specialisee postprocess : on colle les phonemes des mots
    precedent et suivant a la chaine phonetique examinee (sur option)  */

/*  Fred 12/1996  */

/*  Format d'entree : <graphie> <phonetique> <info ....>  */


/*  Phonetise une chaine deja syllabisee  */

#include <stdio.h>
#include <string.h>
#include <pprocess.h>
#include <contrainte.h>

#define True        1
#define False       0
#define TailleLigne 400
#define MaxWindow   16

int SeeReg=0;
int SiColl;

/*................................................................*/

/*  Verifie les contraintes graphiques  */

int min(a,b) int a,b; { return a<b?a:b; }
int max(a,b) int a,b; { return a>b?a:b; }

int PlusLongContGraphik(nu,poids)
 int nu,*poids;
{
char *ch;
int n,m,i;
for(n=m=0,ch=TablGraphik[nu][1],*poids=0;ch[n];)
 {
 for(i=0;(ch[n+i])&&(ch[n+i]!=',');i++)
  if ((ch[n+i]>='a')&&(ch[n+i]<='z')) *poids=max(*poids,(int)ch[n+i]);;
 m=max(m,i);
 if (ch[n+i]==',') n++;
 n+=i;
 }
return m;
}

int ContGraphik(c,nu)
 char c;
 int *nu;
{
for(*nu=0;TablGraphik[*nu][0][0];(*nu)++)
 if (c==TablGraphik[*nu][0][0]) return True;
return False;
}

int PresentGraphik(char *test,char *ch)
{
int n,i,l;
/*
printf("test : %s in %s\n",test,ch);
*/
for (n=0,l=strlen(ch);n<l;n++)
 {
 for (i=0;(test[i])&&((n+i)<l)&&(ch[n+i]!=',')&&(test[i]==ch[n+i]);i++);
 if (((n+i)>=l)||(ch[n+i]==',')) return i;
 while(((n+i)<l)&&(ch[n+i]!=',')) i++;
 n+=i;
 }
return 0;
}

int SiChiffre(c)
 char c;
{
if ((c>='0')&&(c<='9')) return True;
return False;
}

/*................................................................*/

/*  Trie les regles phonetiques  */

int score_chaine(ch)
 char *ch;
{
int n,i=0,coef,nu,poids;  /*  Le coef c'est la longueur de chaque 'caractere'  */
char c;

for(n=0,i=0;ch[n];n++)
 {
 c=ch[n]; coef=1;
 if ((c=='à')||(c=='â')||(c=='ä')) c='a';
 else
  if ((c=='é')||(c=='è')||(c=='ê')||(c=='ë')) c='e';
  else
   if ((c=='î')||(c=='ï')) c='i';
   else
    if ((c=='ô')||(c=='ö')) c='o';
    else
     if ((c=='ù')||(c=='û')||(c=='ü')) c='u';
     else
      if (c=='ç') c='c';
      else
       if ((c>='A')&&(c<='Z'))
        {
        /*  C'est une contrainte  */
        if (ContGraphik(c,&nu))
         {
         coef=PlusLongContGraphik(nu,&poids);
         /*c=(char)poids;*/
         }
        c='z';
        }
       else c='z';
 i+=(coef*(400-(int)c));
 }
return i;
}

int LongueurContrainte(ch)
 char *ch;
{
int n,nu,l,p;

for(n=l=0;ch[n];n++)
 if (ContGraphik(ch[n],&nu)) l+=PlusLongContGraphik(nu,&p);
 else l++;
return l;
}

void poids_regle(seat,pgauche,pdroite,cont,p1,p2,p3)
 char *seat,*pgauche,*pdroite,*cont;
 int *p1,*p2,*p3;
{
char ch[TailleLigne];
sprintf(ch,"%s%s",seat,pdroite);
*p1=(LongueurContrainte(ch)*1000)+score_chaine(ch);
*p2=(LongueurContrainte(pgauche)*1000)+score_chaine(pgauche);
*p3=(LongueurContrainte(cont)*1000)+score_chaine(cont);
}

/*  Fonctions d'allocation memoire  */

ty_regles creer_regles(pgauche,pdroite,seat,phon,cont,exemple)
 char *pgauche,*pdroite,*seat,*phon,*cont,*exemple;
{
ty_regles pt;

pt=(ty_regles) malloc(sizeof(struct type_regles));
pt->pgauche=(char *)strdup(pgauche);
pt->pdroite=(char *)strdup(pdroite);
pt->seat=(char *)strdup(seat);
pt->phon=(char *)strdup(phon);
pt->cont=(char *)strdup(cont);
pt->exemple=(char *)strdup(exemple);
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
int n,i=0,nu;

if ((ch[0]!='l')||((!SiChiffre(ch[1]))&&(!ch[1]=='\0')))
 for(n=0,i=0;ch[n];n++)
  if ((ch[n]=='"')||(ch[n]=='.'))
   ;
  else
   if (ContGraphik(ch[n],&nu))
    {
    resu[i++]=ch[n];
    if (SiChiffre(ch[n+1])) n++;  /*  On saute les indices 0->9  */
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
fclose(file);
return(Regles);
}

/*  Phonetise  */

int compatible_char(char c1,char *c2) /*  La regle est dans c1  */
{
int n;
/*fprintf(stderr,"CompaChar : %c et %s \n",c1,c2);*/
if (c1==(*c2)) return True;  /*  Egalite parfaite  */

/*  Verification avec la table graphik  */

for(n=0;TablGraphik[n][0][0];n++)
 if (c1==TablGraphik[n][0][0])
  return PresentGraphik(c2,TablGraphik[n][1]);
return 0;
}

int verifie_pgauche(pg1,pg2)
 char *pg1,*pg2;
{
int i,j,depl,nu,poids;

if ((*pg1=='\0')&&(*pg2=='\0')) return True;

/*
fprintf(stderr,"compatible gauche : [%s] [%s]\n",pg1,pg2);
for (i=strlen(pg1)-1,j=strlen(pg2)-1;(i>=0)&&(j>=0)&&
    (depl=compatible_char(pg1[i],pg2+j));i--,j-=depl);
*/
for (i=strlen(pg1)-1,j=strlen(pg2)-1;(i>=0)&&(j>=0);)
 {
 depl=compatible_char(pg1[i],pg2+j);
 if (depl) { i--; j--; }
 else
  {
  if (!ContGraphik(pg1[i],&nu)) return False;
  j-=(PlusLongContGraphik(nu,&poids)-1);
  if ((j<0)||(!compatible_char(pg1[i],pg2+j))) return False;
  i--; j--;
  }
 }
if (i<0) { /*printf("Vrai\n");*/ return True; }
/*printf("Faux\n");*/ return False;
}

int verifie_seatdroite(seat,pdroite,ch)
 char *seat,*pdroite,*ch;
{
int i,j,k,depl;
/*
printf("compatible seatdroite : #%s#%s#%s#\n",seat,pdroite,ch);
*/
for (i=0,k=0;(seat[i])&&(ch[k])&&(depl=compatible_char(seat[i],ch+k));i++,k+=depl);
if (seat[i]) return False;
for(j=0;(pdroite[j])&&(ch[k])&&(depl=compatible_char(pdroite[j],ch+k));j++,k+=depl);
if (pdroite[j]) return False;
/*printf("cestoubon \n");*/ return True;
}

/*................................................................*/

/*  Test des contraintes non graphiques  */

int TestSubChaine(test,ch)
 char *test,*ch;
{
int n,l;
l=strlen(test);
for (n=0;ch[n];)
 if (!strncmp(test,ch+n,l)) return True;
 else
  {
  while((ch[n])&&(ch[n]!=' ')&&(ch[n]!='\n')&&(ch[n]!=' ')) n++;
  while((ch[n])&&((ch[n]==' ')||(ch[n]=='\n')||(ch[n]=='\t'))) n++;
  }
return False;
}

int verifie_contrainte(cont_r,cont)
 char *cont_r,*cont;
{
int n;

/*  Si l'une des contraintes est absente, alors elle est verifiee  */
if ((*cont_r=='\0')||(*cont=='\0')) return True;

/*  S'il y a egalite parfaite, elle est verifiee  */
if (!strcmp(cont_r,cont)) return True;

/*  Sinon on test avec la table des contraintes  */

for (n=0;TablContrainte[n][0][0];n++)
 if (!strcmp(cont_r,TablContrainte[n][0]))
  return TestSubChaine(cont,TablContrainte[n][1]);

/*  ATTENTION SI LA CONTRAINTE EST INCONNUE, ELLE EST VERIFIEE  */
/*  Ca FORCE l'ORDRE DES REGLES  01/97  */
/*
printf("Warning : contrainte inconnue -> %s\n",cont_r);
return False;
*/
return True;
}

/*................................................................*/

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
 printf("recherche entre %d et %d (borne : %d -> %d) avec p1=%d\n",debr,finr,
    Regles[debr]->p1,Regles[finr]->p1,p1);
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
/*
if (SeeReg) printf("nbregle=%d limite=%d  posi=%d regle[posi]->p1=%d\n",
    NbRegles,limite,posi,Regles[posi]->p1); 
*/
for (;(posi<NbRegles)&&(Regles[posi]->p1>=limite);posi++)
 {
 /*if (SeeReg) printf("    * %d regle[posi]->p1=%d\n",posi,Regles[posi]->p1); */

 if (compatible_pattern(posi,ch,pgauche,cont,Regles))
  {
  /*if (SeeReg) printf("Trouve \n");*/
  return posi+1;
  }
 }
*newlin=posi;
/*if (SeeReg) printf("Pastrouve\n");*/
return False;
}

int trouve_dicho_regle_baktrak(ch,pgauche,cont,debr,finr,twin,tg,newlin,Regles,NbRegles,oldrules)
 char *ch,*pgauche,*cont;
 int debr,finr,twin,tg,*newlin;
 ty_regles *Regles;
 int NbRegles,oldrules;
{
int p1,posi,limite;

p1=(twin*1000)+score_chaine(ch);
limite=calcule_limite(ch,twin);

/*  Recherche dichotomique sur p1  */
posi=max(oldrules,dichotomie_posi(p1,debr,finr,Regles));
/*printf("  DICHOMAX : debr=%d finr=%d oldrules=%d posi=%d\n",debr,finr,oldrules,posi); */

for (;(posi<NbRegles)&&(Regles[posi]->p1>=limite);posi++)
 if (compatible_pattern(posi,ch,pgauche,cont,Regles))
  return posi+1;
*newlin=posi;
return False;
}

/*................................................................*/

/*  Modif pour le fonctionnement avec backtraking  */

#define MaxPile 400

struct { int n_in,n_out,n_regle; } Pile[MaxPile];
int pindice=0;

void Empile(ni,no,nr)
 int ni,no,nr;
{
if (pindice==MaxPile)
 { printf("Pile pleine, sorry so ....\n"); exit(0); }
Pile[pindice].n_in=ni;
Pile[pindice].n_out=no;
Pile[pindice].n_regle=nr;
pindice++;
}

int Depile(ni,no,nr)
 int *ni,*no,*nr;
{
if (pindice==0) return 0;
pindice--;
*ni=Pile[pindice].n_in;
*no=Pile[pindice].n_out;
*nr=Pile[pindice].n_regle;
return 1;
}

char *phonetise_baktrak(ch_entree,cont,Regles,NbRegles)
 char *ch_entree,*cont;
 ty_regles *Regles;
 int NbRegles;

{
int ni,no,posi,twin,debr,pafini=True,pas_impasse,ndepile=-1,l,foistaille=4;
char ch[TailleLigne],ch_sortie[TailleLigne],*pgauche,sauvchar,*chresu,*chtemp;

chresu=(char*)malloc(sizeof(char)*TailleLigne*foistaille);

pindice=0;

if (SeeReg) printf("R+gles utilis+es : ");

for(ch_sortie[0]=chresu[0]='\0',ni=no=debr=0;pafini;)
 {
 for (pas_impasse=True;(pas_impasse)&&(ch_entree[ni])&&(ch_entree[ni]!='\n');)
  {
  /*printf("Il reste a traiter : |%s|\n",ch_entree+ni); */

  twin=min(MaxWindow,strlen(ch_entree+ni));
  strncpy(ch,ch_entree+ni,twin+1);
  sauvchar=ch_entree[ni];
  ch_entree[ni]='\0';
  pgauche=ch_entree;
  
  if (ndepile>=0) debr=ndepile; else debr=0;
  for(posi=0;(posi==0)&&(twin>0);twin--)
   {
   ch[twin]='\0';
   posi=trouve_dicho_regle_baktrak(ch,pgauche,cont,debr,NbRegles-1,
                twin,ni,&debr,Regles,NbRegles,ndepile);
   }
  ch_entree[ni]=sauvchar;
  if (posi<=ndepile)
   {
   /* On evite l'emploi de la regle juste depilee */
   /*printf("  deja employee : %d (pour %s) - mise a 0\n",posi,ch_entree);*/

   posi=0;
   }
  if (posi==0)
   pas_impasse=False;
  else
   {
   ndepile=-1; /* On reinitialise la derniere pile */

   Empile(ni,no,posi);
   /*printf("On empile : %d\n",posi);*/

   if (SeeReg) printf("%d ",posi);
   if ((no+strlen(Regles[posi-1]->phon))>=TailleLigne)
    {
    printf("WARNING : taille de ch_sortie insuffisante !!!!\n");
    exit(0);
    }
   strcpy(ch_sortie+no,Regles[posi-1]->phon);
   no=strlen(ch_sortie);
   ni+=strlen(Regles[posi-1]->seat);
   debr=0;
   }
  }

 if (pas_impasse)
  {
  l=strlen(chresu);
  if ((l+strlen(ch_sortie)+2)>=foistaille*TailleLigne)
   {
   /* WARNING : taille de ch_sortie insuffisante !!!!  */
   foistaille++;
   chtemp=(char*)malloc(sizeof(char)*TailleLigne*foistaille);
   strcpy(chtemp,chresu);
   free(chresu);
   chresu=chtemp;
   }

  /*printf("Taille resu avant = %d - Ajoute : %s",l,ch_sortie);*/

  chresu[l]='\n'; chresu[l+1]='\0';
  strcat(chresu,ch_sortie);
  
  /*printf(" - Taille apres : %d\nGLOB : %s\n",strlen(chresu),chresu); */
 
  }
 else
  /*printf("IMPA : %s\n",ch_sortie)*/;
 pafini=Depile(&ni,&no,&ndepile);
 /*printf("On depile : %d\n",ndepile); */

 /*if (SeeReg) printf(" [BAKT] ");*/
 }
if (SeeReg) printf("\n");
l=strlen(chresu);
chresu[l]='\n'; chresu[l+1]='\0';

return chresu;
}

/*................................................................*/

char *phonetise(ch_entree,cont,Regles,NbRegles)
 char *ch_entree,*cont;
 ty_regles *Regles;
 int NbRegles;

{
int n,posi,twin,debr;
char ch[TailleLigne],*pgauche,sauvchar,*ch_sortie;

ch_sortie=(char*)malloc(sizeof(char)*TailleLigne);

if (SeeReg) printf("Regles utilisees : ");
ch_sortie[0]='\0';
for (n=0;(ch_entree[n])&&(ch_entree[n]!='\n');)
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
  /*if (SeeReg) printf("test : |%s|\n",ch);*/

  posi=trouve_dicho_regle(ch,pgauche,cont,debr,NbRegles-1,twin,n,&debr,Regles,NbRegles);
  }
 ch_entree[n]=sauvchar;
 if (posi==0)
  {
  /* On avance de 2 caracteres  */
  if (SeeReg) printf("Pas de règles qui s'applique\n");
  sprintf(ch,"%s%c%c",ch_sortie,ch_entree[n],ch_entree[n+1]);
  strcpy(ch_sortie,ch);
  n+=2;
  }
 else
  {
  if (SeeReg) printf("%d ",posi-1);
  strcat(ch_sortie,Regles[posi-1]->phon);
  n+=strlen(Regles[posi-1]->seat);
  }
 }
if (SeeReg) printf("\n");
return ch_sortie;
}

/*................................................................*/

/*  Re-affichage des regles triees  */

void ChangeIIPartie(pg,t_cont,nlibre)
 char *pg;
 int *t_cont,*nlibre;
{
char c1[TailleLigne];
int n=0,i=0,nu;

if (pg[n]=='\0')
 {
 if ((*nlibre)==0) sprintf(c1,"l");
 else sprintf(c1,"l%d",(*nlibre));
 (*nlibre)++;
 }
else
 {
 for(n=0,i=0;pg[n];n++)
  if (ContGraphik(pg[n],&nu))
   {
   if ((i!=0)&&(c1[i-1]!='.'))
    { c1[i++]='"'; c1[i++]='.'; }
   c1[i++]=pg[n];
   if (t_cont[nu]!=0) c1[i++]=(char) ('0'+t_cont[nu]);
   c1[i++]='.';
   t_cont[nu]++;
   }
  else
   {
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
int *t_cont,n,i,nlibre=0;

for (n=0;TablGraphik[n][0][0];n++);
t_cont=(int *)malloc(sizeof(int)*(++n));
for(i=0;i<n;i++) t_cont[i]=0;
ChangeIIPartie(pg,t_cont,&nlibre);
ChangeIIPartie(pd,t_cont,&nlibre);
}

void affiche_regles(Regles,NbRegles)
ty_regles *Regles;
int NbRegles;
{
int i,j,nu;
char pgauche[TailleLigne],pdroite[TailleLigne];

for (i=0;i<NbRegles;i++)
 {
 strcpy(pgauche,Regles[i]->pgauche);
 strcpy(pdroite,Regles[i]->pdroite);
 ChangePartie(pgauche,pdroite);
 printf("regle(%d,<%s,\"%s\",%s>,\"%s\",\"%s\",\"%s\") ->",
    i+1,pgauche,Regles[i]->seat,pdroite,
    Regles[i]->phon,Regles[i]->cont,Regles[i]->exemple);

 for (j=0;pgauche[j];j++)
   if (ContGraphik(pgauche[j],&nu))
    if (SiChiffre(pgauche[j+1]))
     printf("\n        classe(%c%c,\"%c\")",pgauche[j],pgauche[j+1],pgauche[j]);
    else
     printf("\n        classe(%c,\"%c\")",pgauche[j],pgauche[j]);
 for (j=0;pdroite[j];j++)
   if (ContGraphik(pdroite[j],&nu))
    if (SiChiffre(pdroite[j+1]))
     printf("\n        classe(%c%c,\"%c\")",pdroite[j],pdroite[j+1],pdroite[j]);
    else
     printf("\n        classe(%c,\"%c\")",pdroite[j],pdroite[j]);
 printf(" ;\n");
 }
}
  
/*................................................................*/

/*  Main Program  */

void FindConstraint(ch,cont,graphie)
 char *ch,*cont;
{
int n,i,j;

cont[0]='\0';

if (ch[0]=='"')
 {
 ch[0]=' ';
 for(n=1;ch[n]!='"';n++);
 ch[n]=' ';
 }
else
 for (n=0;(ch[n])&&(ch[n]!='\n')&&(ch[n]!=' ');n++);

if (ch[n]=='\n') ch[n]='\0'; if (ch[n]=='\0') return;

for (j=n;(ch[n])&&(ch[n]!='\n')&&(ch[n]==' ');n++);
if (ch[n]=='\n') ch[n]='\0'; if (ch[n]=='\0') return;
for (i=0;(ch[n])&&(ch[n]!='\n')&&(ch[n]!=' ');n++,i++) cont[i]=ch[n];
ch[j]='\0'; cont[i]='\0';
}

void DecoupeLigne(ligne,graphie,Ph,cont)
 char *ligne,**graphie,**Ph,**cont;
{
int n;
*graphie=ligne;
for(n=0;(ligne[n])&&(ligne[n]!=' ');n++);
if (ligne[n]=='\0') { printf("Mauvais format : %s\n",ligne); exit(0); }
ligne[n]='\0';
*Ph=ligne+n+1;
for(++n;(ligne[n])&&(ligne[n]!=' ');n++);

if (ligne[n]=='\0')
 {
 /*printf("Mauvais format : %s\n",ligne); exit(0);*/
 *cont=ligne+n;
 }
else
 {
 ligne[n]='\0';
 *cont=ligne+n+1;
 }
}

void EnrichiLigne(Phrich,Ph0,Ph1,Ph2)
 char *Phrich,*Ph0,*Ph1,*Ph2;
{
int l;
char t0[TailleLigne],t2[TailleLigne];

if (SiColl)
 {
 l=strlen(Ph0);
 if (l<6) {sprintf(t0,"      %s",Ph0); l+=6; }
 else strcpy(t0,Ph0);
 if (strlen(Ph2)<6) sprintf(t2,"%s      ",Ph2);
 else strcpy(t2,Ph2);
 }
else
 {
 l=8;
 strcpy(t0,"        ");
 strcpy(t2,"        ");
 }
sprintf(Phrich,"%c%c%c%c%c%c%s%c%c%c%c%c%c",t0[l-6],t0[l-5],t0[l-4],t0[l-3],t0[l-2],t0[l-1],
	Ph1,t2[0],t2[1],t2[2],t2[3],t2[4],t2[5]);

/*printf("TEST  <%s> <%s> <%s> -> <%s> avec t0=<%s> t2=<%s>\n",Ph0,Ph1,Ph2,Phrich,t0,t2);*/
}

/*................................................................*/

int main(argc,argv)
 int argc;
 char **argv;
{
int     n,i,j,NbRegles;
int     siafregles=False,af_syllabe;
char    fichregles[TailleLigne],path_fich[TailleLigne],*retour,
	*ligne[3],temp[TailleLigne],*Ph[3],*cont[3],*graphie[3],Phrich[TailleLigne],*tmpph,*chsyll[3];
static  char *ch_vide="    ";
ty_regles *Regles;

fichregles[0]='\0';   /*  init du fich de regles courantes  */
af_syllabe=False;
SiColl=1;

for (n=1;n<argc;n++)
 switch (argv[n][1])
    {
    case 'a' :  /*  Affiche les regles triées  */
        siafregles=True;
        break;
    case 'r' :  /*  Specifie un fichier regles autre que rule_phon.pro3  */
        strcpy(fichregles,argv[++n]);
        break;
    case 'i' :  /*  Traite les mots isoles : on ne colle pas les prec et succ  */
	SiColl=0;
	break;
    case 's' :  /*  La syllabisation est ajoutee dans le dernier champs, elle est ajoutee egalement en sortie  */
	af_syllabe=True;
	break;
    case 'h' :  /*  Affiche la liste des options possibles  */
        printf("Usage:  rumbas [-a|-r|-h|-s|-tnom-dico]\n");
        printf("              a : affiche le fichier de regles triees\n");
        printf("              r : charge un fichier de regles different'\n");
        printf("              i : traitement des mots isoles (pas de collage)'\n");
        printf("              s : ajout de la sylabisation dans le dernier champs\n");
        printf("              h : affiche ce message\n");
        exit(0);
    }

#ifdef COMPIL_MSDOS
for (i=n=0;argv[0][n];n++) if (argv[0][n]=='\\') i=n;
if (i==0) strcpy(path_fich,".\\");
else
 {
 strncpy(path_fich,argv[0],i+1);
 path_fich[i+1]='\0'; 
 }
#else
for (i=n=0;argv[0][n];n++) if (argv[0][n]=='/') i=n;
if (i==0) strcpy(path_fich,"./");
else
 {
 strncpy(path_fich,argv[0],i+1);
 path_fich[i+1]='\0'; 
 }
#endif

if (fichregles[0]=='\0')
 {
#ifdef COMPIL_MSDOS
 strcpy(fichregles,"data\\rule_phon.pro");
#else
 strcpy(fichregles,"data/rule_phon.pro");
#endif
 Regles=lecture_regles(path_fich,fichregles,&NbRegles);
 }
else /*  Dans le cas ou un autre fichregle est select on met pas le path  */
 Regles=lecture_regles("",fichregles,&NbRegles);

if (siafregles)
 { affiche_regles(Regles,NbRegles); exit(0); }

Ph[0]=tmpph=strdup("    ");
ligne[0]=NULL;

for(i=1;fgets(temp,TailleLigne,stdin);)
 {
 strtok(temp,"\n");
 ligne[i]=strdup(temp);
 if (af_syllabe)
  {
  for(j=strlen(ligne[i])-1;(j>0)&&(ligne[i][j]!=':');j--);
  if (j==0) { fprintf(stderr,"ERROR: bad format (1234) in: %s\n",temp); exit(1); }
  chsyll[i]=ligne[i]+j+1;
  for(j--;(j>0)&&((ligne[i][j]==' ')||(ligne[i][j]=='\t'));j--) ligne[i][j]='\0';
  if (j==0) { fprintf(stderr,"ERROR: bad format (123456) in: %s\n",temp); exit(1); }
  }
 DecoupeLigne(ligne[i],graphie+i,Ph+i,cont+i);
 if (i==2)
  {
  EnrichiLigne(Phrich,Ph[0],Ph[1],Ph[2]);
  retour=phonetise(Phrich,cont[1],Regles,NbRegles);
  if (retour)
   {
   retour[strlen(retour)-6]='\0';
   printf("%s %s %s",graphie[1],retour+6,cont[1]);
   if (af_syllabe) printf("\t%s",chsyll[1]);
   printf("\n");
   strcpy(Ph[1],retour+6);
   free(retour);
   }
  else
   {
   printf("%s %s %s",graphie[1],Ph[1],cont[1]);
   if (af_syllabe) printf("\t%s",chsyll[1]);
   printf("\n");
   }

  if (ligne[0]) free(ligne[0]);
  ligne[0]=ligne[1]; Ph[0]=Ph[1]; graphie[0]=graphie[1]; cont[0]=cont[1]; chsyll[0]=chsyll[1];
  ligne[1]=ligne[2]; Ph[1]=Ph[2]; graphie[1]=graphie[2]; cont[1]=cont[2]; chsyll[1]=chsyll[2];
  }
 else i++;
 }
Ph[2]=ch_vide;
EnrichiLigne(Phrich,Ph[0],Ph[1],Ph[2]);
retour=phonetise(Phrich,cont[1],Regles,NbRegles);
if (retour)
 {
 retour[strlen(retour)-6]='\0';
 printf("%s %s %s",graphie[1],retour+6,cont[1]);
 if (af_syllabe) printf("\t%s",chsyll[1]);
 printf("\n");
 free(retour);
 }
else
 {
 printf("%s %s %s",graphie[1],Ph[1],cont[1]);
 if (af_syllabe) printf("\t%s",chsyll[1]);
 printf("\n");
 }
free(ligne[0]); free(ligne[1]); free(tmpph);
delete_tabl_regles(Regles,NbRegles);

exit(0); 
}
  
