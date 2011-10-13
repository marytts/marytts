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
/*  Permet de charger un fichier lexique compile avec 'CompileLexiTree'
    puis d'utiliser la fonction 'Present' afin d'obtenir la plus
    longue chaine compatible avec le lexique  */
/*  FRED 0199  */

/*  Chargement de l'arbre-tableau  */
void ChargeLexiqueCompile(char *);

/*  'Present' permet d'obtenir la plus longue chaine compatible avec
    le lexique. Les parametres sont les suivants :
    - ch	: chaine a analyser en entree
    - addr	: addresse dans l'arbre, a initialiser a 1
    - lastword	: renvoi l'adresse du dernier caractere de la chaine 'ch'
		  qui fait partie d'un mot du lexique
    - indice	: indice du caractere en cours, a initialiser a 0
    - refe	: tableau de caractere qui va contenir le mot reference
		  le plus long, dans le lexique, a partir de la chaine 'ch'
    - lastindice: indice de la fin du mot le plus long dans refe
    Si l'ensemble de la chaine 'ch' forme un mot du lexique, alors 'Present'
    retourne la valeur 1, et 0 sinon  */

int Present(char *,unsigned int,char**,int,char *,int*);
int JustPresent(char*); /* version light de present : renvoi 0 ou 1 si le mot y est ou pas !! */
  
