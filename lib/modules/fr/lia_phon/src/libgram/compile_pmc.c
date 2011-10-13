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
/*  Compile un modele Proba Mot sachant la Classe (PMC)
    Il faut en entree :
     - un lexique (compile sirocco) pour les mots
     - un lexique (compile sirocco) pour les classes
     - un fichier texte au format :
	mot\tclasse\tcompte\tlemme\n
    Cela produit 3 fichiers en sortie :
     - .pmc_des : descripteur
     - .pmc_tab : tableau des mots
     - .pmc_zon : les classes et les probas  */

/*  FRED 0399  */ /*  MODIF - FRED - Rajout des lemmes 0699  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <libgram.h>

int main(int argc,char **argv)
{
ty_lexique pt_lexique_mot,pt_lexique_classe;
int si_log_e,si_lemme;
ty_pmc pt_pmc;

if (argc<2)
 {
 fprintf(stderr,"Syntaxe : %s [-h] <lexi mot> <lexi classe> <fich compte> <lemme/no_lemme>\
<log_e/log_10> <nom modele> \n",argv[0]);
 exit(0);
 }

if ((argc<6)||(!strcmp(argv[1],"-h")))
 {
 fprintf(stderr,"Syntaxe : %s [-h] <lexi mot> <lexi classe> <fich compte> <lemme/no_lemme> \
 <log_e/log_10> <nom modele>\n\
 \t ce programme permet de compiler un modele Proba Mot sachant la Classe (PMC).\n\
 \t Les arguments d'entree sont les suivants :\n\
 \t  -h : affiche ce message\n\
 \t  lexi mot : lexique (compile sirocco) pour les mots\n\
 \t  lexi classe : lexique (compile sirocco) pour les classes\n\
 \t  fich compte : fichier texte au format (sur chaque ligne ) : mot classe compte \n\
 \t                obtenu, par exemple avec 'produit_compte_pmc'\n\
 \t  lemme ou no_lemme : permet de specifier si on veut obtenir les lemmes stockes dans le \n\
 \t                      fichier de compte lors de l'acces a une proba P(M/C) \n\
 \t  log_e ou log_10 : permet d'avoir les probas, soit en log_e soit en log_10\n\
 \t  nom modele : nom generique du fichier de stockage du modele\n\
 \t Ce programme produit en sortie 3 fichiers :\n\
 \t  <nom modele>.pmc_des : descripteur\n\
 \t  <nom modele>.pmc_tab : tableau des mots\n\
 \t  <nom modele>.pmc_zon : les classes et les probas\n\n",argv[0]);
 exit(0);
 }

fprintf(stderr,"Chargement des lexiques mot et classe -> ");
pt_lexique_mot=ChargeLexique(argv[1]);
pt_lexique_classe=ChargeLexique(argv[2]);
fprintf(stderr,"Termine\n");

if (!strcmp(argv[4],"lemme")) si_lemme=1; else si_lemme=0;

if (!strcmp(argv[5],"log_e")) si_log_e=1; else si_log_e=0;

fprintf(stderr,"Compilation du modele PMC\n");
pt_pmc=compile_modele_pmc(argv[3],pt_lexique_mot,pt_lexique_classe,si_log_e,si_lemme);
fprintf(stderr,"Termine\n");

fprintf(stderr,"Sauvegarde du modele PMC -> ");
sauve_pmc(argv[6],pt_pmc);
fprintf(stderr,"Termine\n");

delete_lexique(pt_lexique_mot);
delete_lexique(pt_lexique_classe);
delete_pmc(pt_pmc);

fprintf(stderr,"Termine\n");

exit(0); 
}
  
