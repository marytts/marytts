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
/* ooOOoo |     FRED  -   LIA 1996          */
/*..........................................*/

/*  Definition des contraintes graphique et autres  */

char *TablContrainte[][2]=
    {
    {"",""}
    };

char *TablGraphik[][2]=
    {
    {"P",/* Pause ou silence */
        "##"},
    {"S",/* Entre syllabes */
	"--,##"},
    {"E",/* phonemes des eu & oe */
        "eu,oe"},
    {"O",/* phonemes des oo & au */
        "oo,au"},
    {"T",/* phonemes des ei & ai */
        "ei,ai"},
    {"F",/* phonemes des voyelles fortes devant lesquels 'g' se prononce 'gg' */
        "aa,oo,au,uu,ou,on,an,un"},
    {"D",/* phonemes des voyelles devant lesquels 'g' se prononce 'jj' */
        "ai,ei,eu,oe,ii,in"},
    {"V",/* phonemes des voyelles */
        "aa,oo,au,uu,ou,on,an,un,ai,ei,eu,oe,ii,in"},
    {"M",/* phonemes devant lesquels 'en' s'ecrit 'em' */
        "bb,pp,mm"},
    {"N",/* phonemes devant lesquels 'en' ne s'ecrit pas 'em' */
        "tt,kk,dd,gg,vv,zz,jj,ff,ss,ch,nn,gn,ll,rr"},
    {"H",/* phonemes devant lesquels on peut ecrire 'ff' ph */
	"tt,rr,ll"},
    {"O",/* phonemes des consonnes orales */
	"pp,tt,kk,bb,dd,gg,vv,zz,jj,ff,ss,ch,rr,ll"},
    {"Q",/* phonemes des consones nasales */
	"mm,nn,gn"},
    {"L",/* phonemes des consonnes liquides */
	"ll,rr"},
    {"C",/* phonemes des consonnes */
        "tt,kk,dd,gg,vv,zz,jj,ff,ss,ch,nn,gn,ll,rr,bb,pp,mm"},
    {"W",/* phonemes des semi-voyelles */
        "ww,yy,uy"},
    {"Z",/* tous les phonemes ou '-' */
        "tt,kk,dd,gg,vv,zz,jj,ff,ss,ch,nn,gn,ll,rr,bb,pp,mm,aa,oo,au,uu,ou,on,an,un,ai,ei,eu,oe,ii,in,ww,yy,uy,--"},
    {"",""}
    };

