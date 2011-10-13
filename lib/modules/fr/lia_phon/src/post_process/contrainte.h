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
/* ooOOoo |   FRED 1996 - LIA               */
/*..........................................*/

/*  Definition des contraintes graphique et autres  */

char *TablContrainte[][2]=
    {
    {"",""}
    };

char *TablGraphik[][2]=
    {
    {"D",/* Doublet generateur de emuet */
        "bbll,bbrr,ddrr,ffrr,ggll,ggrr,kkll,kkss,kktt,llff,llkk,llmm,llpp,lltt,ppll,pprr,ppss,rrbb,rrch,rrdd,rrgg,rrgn,rrjj,rrkk,rrll,rrmm,rrnn,rrss,rrtt,rrvv,rrzz,sskk,ssmm,sstt,ttrr,vvrr"},
    {"C",/* phonemes des consonnes */
        "tt,kk,dd,gg,vv,zz,jj,ff,ss,ch,nn,gn,ll,rr,bb,pp,mm"},
    {"P",/* phonemes des consonnes occl sourde */
	"pp,tt,kk"},
    {"B",/* phonemes des consonnes occl sonore */
	"bb,dd,gg"},
    {"F",/* phonemes des consonnes fricative sourde */
	"ff,ss,ch"},
    {"Z",/* phonemes des consonnes fricative sonore */
	"vv,zz,jj"},
    {"M",/* phonemes des consonnes nasales */
	"mm,nn,gn"},
    {"L",/* phonemes des consonnes liquides */
	"ll"},
    {"R",/* phonemes des consonnes liquides */
	"rr"},
    {"W",/* phonemes des semi-voyelles */
        "ww,yy,uy"},
    {"V",/* phonemes des voyelles */
        "aa,oo,au,uu,ou,on,an,un,ai,ei,eu,oe,ii,in,EU"},
    {"Z",/* tous les phonemes ou '-' */
        "tt,kk,dd,gg,vv,zz,jj,ff,ss,ch,nn,gn,ll,rr,bb,pp,mm,aa,oo,au,uu,ou,on,an,un,ai,ei,eu,oe,ii,in,ww,yy,uy,--"},
    {"",""}
    };
 
