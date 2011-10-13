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
	/*  Transcription : chiffre ou romain ->orthographique  */

#include <stdio.h>

#define TailleLigne	2000

#define True		1
#define False		0


char *Tabl[][2]={
		{"0","zéro"},
		{"1","un"},
		{"2","deux"},
		{"3","trois"},
		{"4","quatre"},
		{"5","cinq"},
		{"6","six"},
		{"7","sept"},
		{"8","huit"},
		{"9","neuf"},
		{"10","dix"},
		{"11","onze"},
		{"12","douze"},
		{"13","treize"},
		{"14","quatorze"},
		{"15","quinze"},
		{"16","seize"},
		{"20","vingt"},
		{"21","vingt-et-un"},
		{"30","trente"},
		{"31","trente-et-un"},
		{"40","quarante"},
		{"41","quarante-et-un"},
		{"50","cinquante"},
		{"51","cinquante-et-un"},
		{"60","soixante"},
		{"70","soixante-dix"},
		{"71","soixante-et-onze"},
		{"72","soixante-douze"},
		{"73","soixante-treize"},
		{"74","soixante-quatorze"},
		{"75","soixante-quinze"},
		{"76","soixante-seize"},
		{"77","soixante-dix-sept"},
		{"78","soixante-dix-huit"},
		{"79","soixante-dix-neuf"},
		{"80","quatre-vingt"},
		{"90","quatre-vingt-dix"},
		{"91","quatre-vingt-onze"},
		{"92","quatre-vingt-douze"},
		{"93","quatre-vingt-treize"},
		{"94","quatre-vingt-quatorze"},
		{"95","quatre-vingt-quinze"},
		{"96","quatre-vingt-seize"},
		{"97","quatre-vingt-dix-sept"},
		{"98","quatre-vingt-dix-huit"},
		{"99","quatre-vingt-dix-neuf"},
		{"",""}
		};

int GetTabl(ch1,ch2)
 char *ch1,*ch2;
{
int n;

for (n=0;Tabl[n][0][0];n++)
 if (!strcmp(ch1,Tabl[n][0]))
  {
  strcpy(ch2,Tabl[n][1]);
  return True;
  }
return False;
}

int TransDizaine(ch1,ch2)
 char *ch1,*ch2;
{
char ch[4];
int n;

ch2[0]='\0';
if (GetTabl(ch1,ch2)) return True;
if (ch1[0]=='0')
 {
 if (ch1[1]=='0') return True; 
 return GetTabl(ch1+1,ch2);
 }
ch[0]=ch1[0]; ch[1]='0'; ch[2]='\0';
if (!GetTabl(ch,ch2)) return False;
ch[0]=ch1[1]; ch[1]='\0';
n=strlen(ch2);
ch2[n]='-';
if (!GetTabl(ch,ch2+n+1)) return False;
return True;
}

int TransCentaine(ch1,ch2)
 char *ch1,*ch2;
{
char ch[4];

ch[0]=ch1[0]; ch[1]='\0';
ch2[0]='\0';
if (ch[0]!='0')
 {
 if (ch[0]!='1') TransDizaine(ch,ch2);
 strcpy(ch2+strlen(ch2),"-cent-");
 }
return TransDizaine(ch1+1,ch2+strlen(ch2));
}

int TransMillier(ch1,ch2)
 char *ch1,*ch2;
{
int n;
char ch[TailleLigne];

ch2[0]='\0';
n=strlen(ch1)-3;
strcpy(ch,"000");
strncpy(ch+3-n,ch1,n);
ch[3]='\0';
if (!TransCentaine(ch,ch2)) return False;
if (ch2[0])
 {
 if (!strcmp(ch2,"un")) ch2[0]='\0';
 strcat(ch2,"-mille-");
 }
return TransCentaine(ch1+n,ch2+strlen(ch2));
}

int TransMillion(ch1,ch2)
 char *ch1,*ch2;
{
int n;
char ch[TailleLigne];

ch2[0]='\0';
n=strlen(ch1)-6;
strcpy(ch,"000000");
strncpy(ch+6-n,ch1,n);
ch[6]='\0';
if (!TransMillier(ch,ch2)) return False;
if (ch2[0]) strcat(ch2,"-million-");
return TransMillier(ch1+n,ch2+strlen(ch2));
}

int TransMilliard(ch1,ch2)
 char *ch1,*ch2;
{
int n;
char ch[TailleLigne];

ch2[0]='\0';
n=strlen(ch1)-9;
strcpy(ch,"000000000");
strncpy(ch+9-n,ch1,n);
ch[9]='\0';
if (!TransMillion(ch,ch2)) return False;
if (ch2[0]) strcat(ch2,"-milliard-");
return TransMillion(ch1+n,ch2+strlen(ch2));
}

void nettoie(ch1,ch2)
 char *ch1,*ch2;
{
int n,i;

for (n=0,i=0;ch1[n];n++)
 if ((ch1[n]!='-')||((n!=0)&&(ch1[n+1]!='-')&&(ch1[n+1]!='\0')))
  if (ch1[n]=='-') ch2[i++]=' '; else ch2[i++]=ch1[n];
ch2[i]=' '; ch2[i+1]='\0';
}

int TransNombre0(ch1,ch2)
 char *ch1,*ch2;
{
int n;

n=strlen(ch1);
if (n==0) {  /* <- Yannick was here */
	        ch2[0]='\0';
			        return True;
}

if (n<=2) return TransDizaine(ch1,ch2);
if (n<=3) return TransCentaine(ch1,ch2);
if (n<=6) return TransMillier(ch1,ch2);
if (n<=9) return TransMillion(ch1,ch2);
return TransMilliard(ch1,ch2);
}

int TransNombre(ch1,ch2)
 char *ch1,*ch2;
{
char ch[TailleLigne];

if (TransNombre0(ch1,ch))
 {
 nettoie(ch,ch2);
 return True;
 }
return False;
}

int TransNombreDebutEn0(ch1,ch2)
 char *ch1,*ch2;
{
int r=0;
char ch[TailleLigne];
char chbis[TailleLigne];
strcpy(chbis,"");
while (ch1[r++]=='0') strcat(chbis,"zéro ");
if (TransNombre0(ch1+r-1,ch)) /* <- Yannick was here aussi*/
 {
 nettoie(ch,ch2);
 strcat(chbis,ch2);
 strcpy(ch2,chbis);
 return True;
 }
return False;
}


int val( c )
char c;
{
switch (c)
  {
   case 'M' : return(1000);
              break;  
   case 'D' : return(500);
              break;  
   case 'C' : return(100);
              break;  
   case 'L' : return(50);
              break;  
   case 'X' : return(10);
              break;  
   case 'V' : return(5);
              break;  
   case 'I' : return(1);
              break;  
   default  : return (0); 
  }
}


int TransNombreRomain(ch,ch2)
char *ch,*ch2;
{
 int  n=0;
 int  taille=0, total=0, max=1000;
 char tmp[TailleLigne];  
 taille=strlen(ch)-1;
 /* printf("chaine : -%s - taille : %i\n",ch,taille);  */
 
 for (n=0;n<taille;n++)
   {
    if (!val(ch[n])) return(0);  /* le caractere de la chaine n'a pas de sens */
    if ((n+1<strlen(ch)) && (val(ch[n]) < val(ch[n+1]))) 
      {
       /*  printf(" n < suiv %c %i\n",ch[n],val(ch[n]));  */
       total=total - val(ch[n]);
      } 
     else
       {
        /* printf(" n > suiv %c %i\n",ch[n],val(ch[n]));  */
        if ( val(ch[n]) > max) 
          return (0); 
          else max=val(ch[n]);
        total= total + val(ch[n]);  
       } 
   }
 /*   printf(" n fin %c %i\n",ch[n],val(ch[n]));   */
 if (!val(ch[n])) return (0);
    else total+=val(ch[n]);  

sprintf(tmp,"%i\0",total);
if (total==0) return(0);
if (!TransNombre(tmp,ch2)) return (0);
return(1);    
}

/*  Test les formats possible :
        - <nombre>
        - <nombre>.<nombre>
        - <nombre>,<nombre>  */

#define TailleTamp      16

int TraiteChaine(ch,ch2)
 char *ch,*ch2;
{
char chnb1[TailleTamp*100],chnb2[TailleTamp*100];
int n,numero,i,j,ponctu;

chnb1[0]=chnb2[0]='\0';
for(n=i=j=0,numero=1;(n<TailleTamp)&&(ch[n])&&(ch[n]!='\n');n++)
 if ((ch[n]>='0')&&(ch[n]<='9'))
  if (numero==1) chnb1[i++]=ch[n];
  else           chnb2[j++]=ch[n];
 else
  if ((ch[n]==',')||(ch[n]=='.'))
   {
   if (numero>1) return False;
   ponctu=n;
   numero++;
   }
  else return False;
chnb1[i]=chnb2[j]='\0';

if (!TransNombreDebutEn0(chnb1,ch2)) return False;
if (numero>1)
 {
 if (ch[ponctu]==',') strcat(ch2,"virgule ");
 else strcat(ch2,"point ");
 if (!TransNombreDebutEn0(chnb2,ch2+strlen(ch2))) return False;
 }
ch2[strlen(ch2)-1]='\0';
return True;
}
 
