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

#ifndef SRC_EHMM_SRC_THREADING_H_
#define SRC_EHMM_SRC_THREADING_H_

#include <pthread.h>
#include <sys/time.h>

// An interface for a runnable class.
// A macro to disallow the copy constructor and operator= functions
// This should be used in the private: declarations for a class,
// preferably at the very end of the class.
#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&);               \
  void operator=(const TypeName&)

class Thread {
 public:
  virtual ~Thread() { }

  // Block until this thread has finished.  Deletes this object upon return.
  virtual void Join() = 0;
};

class PthreadThread : public Thread {
 public:
  explicit PthreadThread(pthread_t *thread)
    : thread_(thread) { }

  virtual void Join() {
    pthread_join(*thread_, NULL);
    delete thread_;
  }
 private:
  pthread_t* thread_;
};


// A simple mutex.  Only one thread can hold the lock at a time.
class Mutex {
 public:
  virtual ~Mutex() { }

  virtual void Lock() = 0;
  virtual void Unlock() = 0;
};

class PthreadMutex : public Mutex {
 public:
  PthreadMutex();
  virtual ~PthreadMutex();
  void Lock();
  void Unlock();
 private:
  pthread_mutex_t mutex_;
};


// Scoped lock: locks a Mutex in the constructor, unlocks the same
// Mutex in the destructor.  Useful to make sure that a lock is
// unlocked on all exits (normal and error-related) from a lexical
// scope.

class ScopedLock {
 public:
  explicit ScopedLock(Mutex *mutex) : mutex_(mutex) {
    mutex_->Lock();
  }

  ~ScopedLock() {
    mutex_->Unlock();
  }

 private:
  Mutex *mutex_;  // Not owned.

  DISALLOW_COPY_AND_ASSIGN(ScopedLock);
};

typedef void* (thread_function)(void* data);

Thread* StartJoinableThread(thread_function* func, void* userdata = NULL);

#endif  // SRC_EHMM_SRC_THREADING_H_
