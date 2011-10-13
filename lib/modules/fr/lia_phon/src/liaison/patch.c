/*  Patch to fix some errors due to the limited context size
 *  of the liaison rules (e.g. n'a plus) */
/*  FRED 0706  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

/*................................................................*/

#define TailleLigne     8000

#define True    1
#define False   0

void ERREUR(char *ch1,char *ch2)
{
fprintf(stderr,"ERREUR : %s %s\n",ch1,ch2);
exit(0);
}

/*................................................................*/

/* example:
il iill [PPER3MS]
n' nn [ADVNE]
a aa [VA3S]
plus pplluuss [ADV->EXCEPTION]
toute ttoutt [AINDFS]
*/

#define SIZE_CONTEXT	10
#define SIZE_LINE	200

char T_context[SIZE_CONTEXT][SIZE_LINE];

int compare_token(char *ch, int nb, char *token, int l)
{
int i,j;
for(i=j=0;i<nb-1;i++)
 {
 for(;(ch[j])&&(ch[j]!=' ')&&(ch[j]!='\t');j++);
 for(++j;(ch[j])&&(ch[j]==' ')||(ch[j]=='\t');j++);
 }
if (ch[j])
 {
 for(i=0;(i<l)&&(ch[j])&&(token[i])&&(ch[j]==token[i]);i++,j++);
 if ((i==l)&&(ch[j-1]==token[i-1])&&((ch[j]==' ')||(ch[j]=='\t')||(ch[j]=='\n'))) return True;
 }
return False;
}

int replace_token(char *ch, int nb, char *token_old, int l, char *token_new)
{
int i,j,k;
static char ch_bis[TailleLigne];
for(i=j=0;i<nb-1;i++)
 {
 for(;(ch[j])&&(ch[j]!=' ')&&(ch[j]!='\t');j++);
 for(++j;(ch[j])&&(ch[j]==' ')||(ch[j]=='\t');j++);
 }
if (ch[j])
 {
 for(i=0,k=j;(i<l)&&(ch[j])&&(token_old[i])&&(ch[j]==token_old[i]);i++,j++);
 if ((i==l)&&(ch[j-1]==token_old[i-1])&&((ch[j]==' ')||(ch[j]=='\t')||(ch[j]=='\n')))
  {
  ch[k]='\0';
  strcpy(ch_bis,ch);
  strcat(ch_bis,token_new);
  strcat(ch_bis,ch+j);
  if (strlen(ch_bis)>=SIZE_LINE) ERREUR("cste SIZE_LINE too small:",ch_bis);
  strcpy(ch,ch_bis);
  return True;
  }
 }
return False;
}

void process_patch(int i)
{
int j;

/* cas du n'a plus */
if ((i+2<SIZE_CONTEXT)&&
		(compare_token(T_context[i],1,"n'",2)) &&
			((compare_token(T_context[i+1],1,"a",1)) || 
			 (compare_token(T_context[i+1],1,"as",2)) || 
			 (compare_token(T_context[i+1],1,"ai",2)) || 
			 (compare_token(T_context[i+1],1,"ait",3)) || 
			 (compare_token(T_context[i+1],1,"ont",3)) ||
			 (compare_token(T_context[i+1],1,"avez",4)) ||
			 (compare_token(T_context[i+1],1,"aviez",5)) ||
			 (compare_token(T_context[i+1],1,"avait",5)) ||
			 (compare_token(T_context[i+1],1,"avons",5)) ||
			 (compare_token(T_context[i+1],1,"avions",6)) ||
			 (compare_token(T_context[i+1],1,"avaient",7))) &&
		(compare_token(T_context[i+2],1,"plus",4))&&(compare_token(T_context[i+2],2,"pplluuss",8)))
 {
 /* found one */
 if (!replace_token(T_context[i+2],2,"pplluuss",8,"pplluu")) ERREUR("strange:",T_context[i+2]);
 }
else
/* cas du n'a plus a */
if ((i+3<SIZE_CONTEXT)&&
		(compare_token(T_context[i],1,"n'",2)) &&
			((compare_token(T_context[i+1],1,"a",1)) || 
			 (compare_token(T_context[i+1],1,"as",2)) || 
			 (compare_token(T_context[i+1],1,"ai",2)) || 
			 (compare_token(T_context[i+1],1,"ait",3)) || 
			 (compare_token(T_context[i+1],1,"ont",3)) ||
			 (compare_token(T_context[i+1],1,"avez",4)) ||
			 (compare_token(T_context[i+1],1,"aviez",5)) ||
			 (compare_token(T_context[i+1],1,"avait",5)) ||
			 (compare_token(T_context[i+1],1,"avons",5)) ||
			 (compare_token(T_context[i+1],1,"avions",6)) ||
			 (compare_token(T_context[i+1],1,"avaient",7))) &&
		(compare_token(T_context[i+2],1,"plus",4))&&(compare_token(T_context[i+2],2,"pplluu",6)) &&
		(compare_token(T_context[i+3],1,"à",1))&&(compare_token(T_context[i+3],2,"zzaa",4)) )
 {
 /* found one */
 if (!replace_token(T_context[i+3],2,"zzaa",4,"aa")) ERREUR("strange:",T_context[i+2]);
 }

}

int main(int argc, char **argv)
{
char ch[TailleLigne];
int nb,i;
/*
if (argc>1)
 for(nb=1;nb<argc;nb++)
  if (!strcmp(argv[nb],"-XXXX"))
   {
   if (nb+1==argc) ERREUR("an option must follow option:",argv[nb]);
   XXXX
   }
  else
  if (!strcmp(argv[nb],"-h"))
   {
   fprintf(stderr,"Syntax: %s [-h]\n",argv[0]);
   exit(0);
   }
  else ERREUR("unknown option:",argv[nb]);
*/

for(nb=0;nb<SIZE_CONTEXT;nb++) T_context[nb][0]='\0';

for(nb=0;fgets(ch,TailleLigne,stdin);nb++)
 {
 if (strlen(ch)>=SIZE_LINE) ERREUR("cste SIZE_LINE too small:",ch);
 process_patch(0);
 if (T_context[0][0]) printf("%s",T_context[0]);
 for(i=0;i<SIZE_CONTEXT-1;i++) strcpy(T_context[i],T_context[i+1]);
 strcpy(T_context[i],ch);
 }
for(i=0;i<SIZE_CONTEXT;i++) { process_patch(i); if (T_context[i][0]) printf("%s",T_context[i]); }

exit(0);
}
  
