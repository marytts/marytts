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
/****************************/
/*  Declaration Constantes  */
/****************************/

char *TablVoye[]=
	{
	"a", "à", "â", "ä", "e", "é", "è", "ê",
	"ë", "i", "î", "ï", "o", "ô", "ö", "u",
	"û", "ü", "y", "A", "E", "I", "O", "U",
	"Y", ""
	} ;

char *TablCons[]=
	{
	"z", "r", "t", "p", "q", "s", "d", "f",
	"g", "h", "j", "k", "l", "m", "w", "x",
	"c", "ç", "v", "b", "n", "Z", "R", "T",
	"P", "Q", "S", "D", "F", "G", "H", "J",
	"K", "L", "M", "W", "X", "C", "V", "B",
	"N", ""
	} ;

char *TablConsGene[]=
	{
	"d", "g", "n", "p", "s", "t", "x", "z",
	""
	} ;

char *TablOclu[]=
	{
	"p", "t", "c", "k", "b", "d", "g", ""
        } ;
        
char *TablLiqu[]=
	{
	"r", "l", "R", "L", ""
	} ;
        
char *TablNomb[]=
	{
	"0", "1", "2", "3", "4", "5", "6", "7",
	"8", "9", ""
	} ;

char *NonSec2Cons[]=
	{
	"ch", "kh", "ph", "qh", "rh", "th", "dj", "bl",
	"cl", "dl", "fl", "gl", "kl", "pl", "gn", "mn",
	"br", "cr", "dr", "fr", "gr", "pr", "tr", "vr",
	"cs", "ks", "ps", "ts", "cz", "tz", ""
	} ;

char *NonSec3Cons[]=
	{
	"chr", "phr", "thr", "pht", "sch", ""
	} ;

char *NonSec2Voye[]=
	{
	"ae", "ai", "âi", "aï", "aî", "au", "ay", "ea",
	"eâ", "ee", "ei", "eo", "eu", "eû", "ey", "ia",
	"ïa", "ie", "ïe", "io", "iu", "ié", "iè", "oe",
	"oi", "oï", "oî", "ou", "oû", "oy", "ui", "uï",
	"ue", "uy", "ya", "ye", "yi", "yo", "yu", "yé",
	"yè", "eô", ""
	} ;

int si_type_graph(tabl,ch)
 char *tabl[],*ch;
{
int n,i;

for (n=0;tabl[n][0];n++)
 {
 for (i=0;(tabl[n][i])&&(ch[i])&&(tabl[n][i]==ch[i]);i++);
 if (tabl[n][i]=='\0') return 1;
 }
return 0; 
}
  
