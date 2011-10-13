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
/*  Gestion d'un lexique :
      * Pour la compilation :
       - en entree : un fichier lexique compose de <code> <mot>
       - en sortie : 3 fichiers dico.des dico.tab et dico.zon
	 avec : dico.des=descripteur de taille du dico
		dico.tab=table des indices des mots du dico
		dico.zon=stockage des graphies
      * Pour l'utilisation :
       - en entree : 3 fichiers dico.des dico.tab et dico.zon
       - une fonction gere_lexique_mot2code faisant la correspondance entre mot et code  */
/*  FRED 0498  :  Modif Multi ML - 0399  */

/* Structure de codage du lexique */

typedef struct
	{
	wrd_index_t indice,code;
	} type_item_lexique;

typedef struct type_lexique
	{
	wrd_index_t GereLexique_NbMots,TailleZone;
	type_item_lexique *TablLexique,*TablLexiqueGraf;
	char *ZoneLexique;
	} *ty_lexique;

void delete_lexique(ty_lexique);

ty_lexique CompileLexique(char *);
ty_lexique ChargeLexique(char *);

int Mot2Code(char * ,wrd_index_t * ,ty_lexique );
int Code2Mot(wrd_index_t , char **,ty_lexique );
 
