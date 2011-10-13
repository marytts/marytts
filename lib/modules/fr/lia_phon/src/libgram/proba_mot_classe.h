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
/*  Gestion des probas des mots sachant les classes :
      * Pour la compilation :
       - en entree : 2 fichiers lexiques (format sirocco)
			- un pour les mots
			- un pour les categories
                     1 fichier texte au format suivant :
			mot\tcate\tnb\n
       - en sortie : 3 fichiers .pmc_des .pmc_tab .pmc_zon
	 avec : .pmc_des=descripteur de taille du modele
		.pmc_tab=table des indices des mots du dico trie par alpha
		.pmc_zon=stockage des probas des mots sachant la classe
      * Pour l'utilisation :
       - en entree : 3 fichiers .pmc_des .pmc_tab .pmc_zon
       - une fonction p_mot_classe donnant la proba du mot sachant la classe  */

/*  FRED 0399  */

/* Structure de codage du modele */

/* Zone de stockage des categorie avec leur proba en log */

typedef struct type_zon_lemm_pmc
	{
	wrd_index_t c_cate,c_lemm;
	float pmc;
	} type_zon_lemm_pmc;

typedef struct type_zon_pmc
	{
	wrd_index_t c_cate;
	float pmc;
	} type_zon_pmc;

/* Tableau des mots avec pointeur sur leur liste de categorie+proba */

typedef struct type_tab_pmc
	{
	wrd_index_t c_mot;
	unsigned int nb_cate;
	long i_cate;
	} type_tab_pmc;

/* Type general pmc permettant de stocker plusieurs modeles */

typedef struct type_pmc
	{
	wrd_index_t nb_tab,nb_zon;
	int si_log_e,si_lemme,max_nb_cate;
	type_zon_lemm_pmc *p_zon_lemm;
	type_zon_pmc *p_zon;
	type_tab_pmc *p_tab;
	} *ty_pmc;

void delete_pmc(ty_pmc);

ty_pmc compile_modele_pmc(char *nomfic,ty_lexique ,ty_lexique ,int ,int);

void sauve_pmc(char *,ty_pmc);

ty_pmc charge_pmc(char *);

err_t pmc_proba_mot_classe(logprob_t *,wrd_index_t *,
	const wrd_index_t , const wrd_index_t , ty_pmc , const err_t );

err_t pmc_liste_mot_classe(wrd_index_t *, logprob_t *, int *, const wrd_index_t, ty_pmc , const err_t) ;
 
