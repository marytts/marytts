/*  Remonte les liaions pour la phonetisation de lexique
    avec variantes

    Exemple:

    <s> ## [ZTRM->EXCEPTION]
    ceci sseessii [PDEMFP]
    </s> ## [ZTRM->EXCEPTION]
    <s> ## [ZTRM->EXCEPTION]
    maintenant mmintteennan [VPPRE]
    </s> |t##|| [ZTRM->EXCEPTION]


    devient:

    ceci sseessii [PDEMFP]
    maintenant mmintteennan|t [VPPRE]

*/

/*  FRED 0303  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

/*................................................................*/

#define TailleLigne	400

#define True	1
#define False	0

void ERREUR(char *ch1,char *ch2)
{
fprintf(stderr,"ERREUR : %s %s\n",ch1,ch2);
exit(0);
}

/*................................................................*/

int main(int argc, char **argv)
{
char ch1[TailleLigne],ch2[TailleLigne],*graf,*phon,*cate,*liaison;
int nb;

for(nb=0;fgets(ch1,TailleLigne,stdin);nb++)
 if ((strncmp(ch1,"<s>",3))&&(strncmp(ch1,"</s>",4))&&(strncmp(ch1,"<FIN>",5)))
  {
  if (!fgets(ch2,TailleLigne,stdin)) ERREUR("bad input file1: ",ch1);
  if (strncmp(ch2,"</s>",4)) ERREUR("bad input file2: ",ch2);
  graf=strtok(ch1," \t\n"); if (graf) phon=strtok(NULL," \t\n"); if (phon) cate=strtok(NULL," \t\n");
  if ((!graf)||(!phon)||(!cate)) ERREUR("bad input file3","");
  liaison=strtok(ch2," \t\n"); liaison=strtok(NULL," \t\n");
  printf("%s %s",graf,phon);
  if (liaison[0]=='|') printf("|%c",liaison[1]);
  printf(" %s\n",cate);
  }

}
  
