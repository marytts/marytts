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
/*  Rajoute les lemmes a un fichier .ecg  */
/*  FRED 0799 - Modif 1100   */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <libgram.h>

ty_lexique LexiqueGraf,LexiqueCate;
ty_pmc ModelePmc;

void renvoi_item(char *graf,char *cate,char *item,int si_remplace)
{
wrd_index_t c_graf,c_cate,c_lemm;
err_t retour;
double pb;
char *lemme;

if (Mot2Code(graf,&c_graf,LexiqueGraf))
 {
 if (!Mot2Code(cate,&c_cate,LexiqueCate))
  { fprintf(stderr,"C'est quoi cette classe ?? : %s\n",cate); exit(0); }
 retour=pmc_proba_mot_classe(&pb,&c_lemm,c_graf,c_cate,ModelePmc,0);
 if (retour!=CORRECT)
  { /* on mets le mot comme lemme */ c_lemm=c_graf; }
 if (!Code2Mot(c_lemm,&lemme,LexiqueGraf))
  { fprintf(stderr,"Je n'ai pas ce lemme dans les graphie !! : %ld\n",c_lemm); exit(0); }
 }
else lemme=graf;
if (lemme[0]=='?') lemme=graf;

/*sprintf(item,"%s#%s",graf,lemme);*/
/*sprintf(item,"%s",lemme);*/

if (si_remplace) sprintf(item,"%s %s",lemme,cate);
else             sprintf(item,"%s %s %s",graf,cate,lemme);
}

int main(argc,argv)
 int argc;
 char **argv;
{
char ch[200],*graf,*cate,item[200];
int si_remplace=0;

if (argc<4)
 {
 fprintf(stderr,"Syntaxe : %s <dico mot> <dico cate> <modele pmc/lemme> [-remplace]\n",argv[0]);
 exit(0);
 }

/*fprintf(stderr,"Chargement des modeles -> ");*/
LexiqueGraf=ChargeLexique(argv[1]);
LexiqueCate=ChargeLexique(argv[2]);
ModelePmc=charge_pmc(argv[3]);
/*fprintf(stderr,"Termine\n");*/

if ((argc>4)&&(!strcmp(argv[4],"-remplace"))) si_remplace=1;

while(fgets(ch,200,stdin))
 {
 graf=strtok(ch," \t\n");
 cate=strtok(NULL," \t\n");
 if (cate==NULL)
  { fprintf(stderr,"ERROR(rajoute_lemm): there should be a category after the word\n"); exit(0); }
 renvoi_item(graf,cate,item,si_remplace);
 printf("%s\n",item);
 }

exit(0); 

}
 
