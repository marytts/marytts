/*  Extract proper-name from a POS tagged text corpus  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

/*................................................................*/

#define TailleLigne	40000

#define True	1
#define False	0

void ERREUR(char *ch1,char *ch2)
{
fprintf(stderr,"ERREUR : %s %s\n",ch1,ch2);
exit(0);
}

/*................................................................*/

#define IF_MAJU(a)	(((a)>='A')&&((a)<='Z'))
#define NON_ALPHA(a)	(((a)<'A')||(((a)>'Z')&&((a)<'a'))||((a)>'z'))

#define IF_PONCTU(a)	(((a)==',')||((a)=='.')||((a)==';')||((a)==':')||((a)=='!')||((a)=='?'))

#define IF_SEPAR(a)	(((a)=='-')||((a)=='_')||((a)=='&')||((a)=='*')||((a)=='@'))

#define TailleMot	200
#define WindowSize	10

char T_word[WindowSize][TailleMot];
char T_tagg[WindowSize][TailleMot];
char T_flag[WindowSize];

int sure_name(int i)
{
if ((T_tagg[i][0]=='X')||((!strcmp(T_tagg[i],"MOTINC"))&&(IF_MAJU(T_word[i][0])))) return True;
return False;
}

int find_name()
{
int i,pafini;
for(i=0,pafini=True;(i<WindowSize)&&(pafini);)
 {
 if (sure_name(i)) pafini=True;
 else
  if ((i>0)&&(IF_SEPAR(T_word[i][0]))&&(i<WindowSize-1)&&(sure_name(i+1))) pafini=True;
  else
   if ((i>0)&&((!strcmp(T_word[i],"de"))||(!strcmp(T_word[i],"y")))&&(i<WindowSize-1)&&(sure_name(i+1))) pafini=True;
   else pafini=False;
 if (pafini) i++;
 }
return i;
}

void traite_ligne(char *ch)
{
static char *lvide="  ";
char *word,*tagg;
int nb,i,j;

if (ch)
 {
 word=strtok(ch," \t\n");
 if (word) tagg=strtok(NULL," \t\n"); else tagg=NULL;
 if ((!word)||(!tagg)) { fprintf(stderr,"ERROR: bad format in input file line %d\n",nb+1); exit(0); }
 if (strlen(word)>=TailleMot) word[TailleMot-2]='\0';
 if (strlen(tagg)>=TailleMot) tagg[TailleMot-2]='\0';
 }
else tagg=word=lvide;

for(i=1;i<WindowSize;i++)
 {
 strcpy(T_word[i-1],T_word[i]);
 strcpy(T_tagg[i-1],T_tagg[i]);
 T_flag[i-1]=T_flag[i];
 }
strcpy(T_word[WindowSize-1],word);
strcpy(T_tagg[WindowSize-1],tagg);
T_flag[WindowSize-1]=0;

if (T_flag[0]==0)
 {
 if ((j=find_name())&&((j>1)||(strlen(T_word[0])>1)))
  {
  for(i=0;i<j;i++)
   {
   if (i>0) printf(" ");
   printf("%s",T_word[i]);
   T_flag[i]=1;
  }
  printf("\n");
  }
 }
}

int main(int argc, char **argv)
{
char ch[TailleLigne];
int nb;

for (nb=0;nb<WindowSize;nb++) T_flag[nb]=T_word[nb][0]=T_tagg[nb][0]='\0';

for(nb=0;fgets(ch,TailleLigne,stdin);nb++) traite_ligne(ch);
for(nb=0;nb<WindowSize;nb++) traite_ligne(NULL);
}
 
