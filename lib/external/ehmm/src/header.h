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

#ifndef SRC_EHMM_SRC_HEADER_H_
#define SRC_EHMM_SRC_HEADER_H_

#include<math.h>
#include<stdio.h>
#include<stdlib.h>
#include<time.h>

#include<iostream>
#include<fstream>

#define kNmLimit 250
#define _pi 3.141592653

// Avoid unused variable warnings with this
#define UNUSED_PARAMETER(p)  static_cast<void>(p)


int FileExist(char* filename);
void Alloc2f(double**& arr, int r, int c);
void Delete2f(double**& arr, int r);
void Alloc1f(double*& arr, int r);
void Delete1f(double*& arr);
int IsNAN(double val);

int FileExist(char *fnm) {
  using std::ifstream;
  using std::ios;
  using std::cout;
  using std::endl;
  int rv = 1;
  ifstream fp_in;
  fp_in.open(fnm, ios::in);
  if (fp_in == 0) {
    cout << "Local: Cannot open file..."<< fnm <<endl;
    rv = 0;
    exit(1);
  }
  fp_in.close();
  return(rv);
}

int IsNAN(double val) {
  int nF = 0;

  if (val == val) {
    nF = 0;
  } else {
    nF = 1;
  }
  return(nF);
}

void Alloc2f(double**& arr, int r, int c) {
  arr = new double*[r];
  for (int i = 0; i < r; i++) {
    arr[i] = new double[c];
    for (int j = 0; j < c; j++) {
      arr[i][j] = 0;
    }
  }
}

void Delete2f(double**& arr, int r) {
  for (int i = 0; i < r; i++) {
    delete [] arr[i];
  }
  delete [] arr;
}

void Alloc1f(double*& arr, int r) {
  arr = new double[r];
  for (int i = 0; i < r; i++) {
    arr[i] = 0;
  }
}
void Delete1f(double*& arr) {
  delete [] arr;
}

void Alloc2d(int**& arr, int r, int c) {
  arr = new int*[r];
  for (int i = 0; i < r; i++) {
    arr[i] = new int[c];
    for (int j = 0; j < c; j++) {
      arr[i][j] = 0;
    }
  }
}

void Delete2d(int**& arr, int r) {
  for (int i = 0; i < r; i++) {
    delete [] arr[i];
  }
  delete [] arr;
}

void Alloc1d(int*& arr, int r) {
  arr = new int[r];
  for (int i = 0; i < r; i++) {
    arr[i] = 0;
  }
}
void Delete1d(int*& arr) {
  delete [] arr;
}

#endif  // SRC_EHMM_SRC_HEADER_H_
