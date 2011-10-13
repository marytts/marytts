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
/******************************************/
/*  Syllabisation orthographique de mots  */
/******************************************/

#include <stdio.h>
#include <declphon.h>

int autre_voyelle(ch,nb)
 char *ch;
 int *nb;
{
int n;

*nb=0;
for (n=0;(ch[n])&&(!si_type_graph(TablVoye,ch+n))&&
	 (si_type_graph(TablCons,ch+n));n++);
if ((ch[n])&&(si_type_graph(TablVoye,ch+n)))
 {
 *nb=n;
 return 1;
 }
return 0;
}

void syllabise(source,dest)
 char *source,*dest;
{
int n,i,nbcons,booque;

for (n=0,i=0;source[n];n++)
 {
 for(;(source[n])&&(!si_type_graph(TablVoye,source+n));)
  {
  if (source[n]==' ') dest[i++]=' ';
  if ((source[n]=='-')||(source[n]=='_'))
   { dest[i++]=' '; dest[i++]=' '; dest[i++]='-'; dest[i++]=' '; dest[i++]=' '; n++; }
  else dest[i++]=source[n++];
  }
 dest[i++]=source[n];
 if (source[n]=='\0') return;
 if (autre_voyelle(source+n+1,&nbcons))
  {
  n++;
  /*  Cas particulier : qu cu gu  */
  if ((n>=2)&&(source[n-1]=='u')&&(nbcons==0)&&
      ((source[n-2]=='q')||(source[n-2]=='c')||(source[n-2]=='g')))
   booque=1;
  else
   booque=0;
  if (nbcons>=4)
   {
   /*  trois consonnes non secables + une consonne  */
   if (si_type_graph(NonSec3Cons,source+n))
    {
    dest[i++]=source[n++]; dest[i++]=source[n++]; dest[i++]=source[n];
    dest[i++]=' ';
    }
   else
   /*  deux consonnes non secables + deux consonnes non secables  */
   if ((si_type_graph(NonSec2Cons,source+n)) &&
       (si_type_graph(NonSec2Cons,source+n+2)))
    {
    dest[i++]=source[n++]; dest[i++]=source[n];
    dest[i++]=' ';
    }
   else
   /*  une consonne + trois consonnes non secables  */
   if (si_type_graph(NonSec3Cons,source+n+1))
    {
    dest[i++]=source[n];
    dest[i++]=' ';
    }
   else
   /*  une consonne + deux consonnes non secables + une consonne  */
   /*  ou  */
   /*  deux consonnes non secables + deux consonnes  */
   if ((si_type_graph(NonSec2Cons,source+n+1)) ||
       (si_type_graph(NonSec2Cons,source+n)))
    {
    dest[i++]=source[n++]; dest[i++]=source[n++]; dest[i++]=source[n];
    dest[i++]=' ';
    }
   else
   /*  Sinon  */
    {
    dest[i++]=source[n++]; dest[i++]=source[n];
    dest[i++]=' ';
    }
   }
  else
  if (nbcons==3)
   {
   /*  trois consonnes non secables  */
   if (si_type_graph(NonSec3Cons,source+n))
    {
    dest[i++]=' ';
    dest[i++]=source[n];
    }
   else
   /*  une consonne + deux cons non secables  */
   if ((si_type_graph(NonSec2Cons,source+n+1)) &&
       (!si_type_graph(NonSec2Cons,source+n)))
    {
    dest[i++]=source[n];
    dest[i++]=' ';
    }
   else
   /*  Sinon  */
    {
    dest[i++]=source[n++]; dest[i++]=source[n];
    dest[i++]=' ';
    }
   }
  else
  if (nbcons==2)
   {
   /*  deux consonnes non secables  */
   if (si_type_graph(NonSec2Cons,source+n))
    {
    dest[i++]=' ';
    dest[i++]=source[n];
    }
   else
   /*  Sinon  */
    {
    dest[i++]=source[n];
    dest[i++]=' ';
    }
   }
  else
  if (nbcons==1)
   {
   dest[i++]=' ';
   dest[i++]=source[n];
   }
  else
  /*  Cas Particulier  */
  if ((n)&&(!strncmp("ouin ",source+n-1,5)))
   {
   dest[i]='\0';
   strcat(dest,"u in  ");
   i+=5; n+=3;
   }
  else
  if ((n)&&(!strncmp("ouins ",source+n-1,6)))
   {
   dest[i]='\0';
   strcat(dest,"u ins  ");
   i+=6; n+=4;
   }
  else
  if ((source[n]=='y')&&(si_type_graph(TablVoye,source+n+1)))
   {
   dest[i++]=' '; dest[i++]='y';
   }
  else
  /*  deux voyelles secables  */
   {
   if ((!booque)&&(si_type_graph(TablVoye,source+n))&&
       (!si_type_graph(NonSec2Voye,source+n-1)))
    dest[i++]=' ';
   n--;
   }
  }
 }
dest[i]='\0';
}

