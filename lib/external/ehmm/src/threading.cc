/*************************************************************************/
/*                                                                       */
/*                     Carnegie Mellon University                        */
/*                         Copyright (c) 2011                            */
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
/*               Authors: Alok Parlikar                                  */
/*               Email  : aup@cs.cmu.edu                                 */
/*               Date Created: 11/12/2011                                */
/*************************************************************************/
/* Classes and helper functions for enabling threading in EHMM training  */
/*                                                                       */
/*************************************************************************/

#include "threading.h"

// Implementation of a Mutex using pthreads
PthreadMutex::PthreadMutex() {
  pthread_mutex_init(&mutex_, NULL);
}
PthreadMutex::~PthreadMutex() {
  pthread_mutex_destroy(&mutex_);
}
void PthreadMutex::Lock() {
  pthread_mutex_lock(&mutex_);
}
void PthreadMutex::Unlock() {
  pthread_mutex_unlock(&mutex_);
}

Thread* StartJoinableThread(thread_function* func, void*userdata) {
  pthread_t* thread = new pthread_t;
  pthread_create(thread, NULL, func, userdata);

  return new PthreadThread(thread);
}
