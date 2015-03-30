/*************************************************************************/
/*                                                                       */
/*                     Carnegie Mellon University                        */
/*                         Copyright (c) 2005                            */
/*                        All Rights Reserved.                           */
/*                                                                       */
/*  Permission is hereby granted, free of charge, to use and distribute  */
/*  this software and its documentation without restriction, including   */
/*  without limitation the rights to use, copy, modify, merge, publish,  */
/*  distribute, sublicense, and/or sell copies of this work, and to      */
/*  permit persons to whom this work is furnished to do so, subject to   */
/*  the following conditions:                                            */
/*   1. The code must retain the above copyright notice, this list of    */
/*      conditions and the following disclaimer.                         */
/*   2. Any modifications must be clearly marked as such.                */
/*   3. Original authors' names are not deleted.                         */
/*   4. The authors' names are not used to endorse or promote products   */
/*      derived from this software without specific prior written        */
/*      permission.                                                      */
/*                                                                       */
/*  CARNEGIE MELLON UNIVERSITY AND THE CONTRIBUTORS TO THIS WORK         */
/*  DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING      */
/*  ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   */
/*  SHALL CARNEGIE MELLON UNIVERSITY NOR THE CONTRIBUTORS BE LIABLE      */
/*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES    */
/*  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN   */
/*  AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,          */
/*  ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF       */
/*  THIS SOFTWARE.                                                       */
/*                                                                       */
/*************************************************************************/
/*                                                                       */
/*            Authors: Kishore Prahallad                                 */
/*            Email:   skishore@cs.cmu.edu                               */
/*            Date Created: 05/25/2005                                   */
/*            Last Modified: 05/25/2005                                  */
/*            Purpose: A state class for HMM implementation              */
/*                                                                       */
/*************************************************************************/
#ifndef include
 #define include
 #include "header.h"
#endif

class wrdC {
  
  public: 	

     int bst;  //begining state number	  
     int est;  //ending state number
     int nst;  //no of states
     int wid;  //word id
     char nm[nmL];
     
     wrdC();
     void init(int, int, int, int, char*);  
     int getbst();
     int getest();
     int getnst();
};

int wrdC::getbst() { 
  return bst;
}

int wrdC::getest() { 
  return est;
}

int wrdC::getnst() { 
  return nst;
}

wrdC::wrdC() {

}
void wrdC::init(int t_bst, int t_est, int t_nst, int t_wid, char *t_nm) {
   
   bst = t_bst;	
   est = t_est;
   nst = t_nst;
   wid = t_wid;
   strcpy(nm, t_nm);
}
