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
/*               Authors: Kishore Prahallad                              */
/*               Email:   skishore@cs.cmu.edu                            */
/*               Date Created: 05/25/2005                                */
/*               Last Modified: 05/25/2005                               */
/*               Purpose: A state class for HMM implementation           */
/*                                                                       */
/*    Modified Nov 2011 by aup and gopalakr for implementing threads     */
/*                                                                       */
/*************************************************************************/

#include <string.h>
#include <stdio.h>

#include "./header.h"
#include "./hmmstate.h"
#include "./hmmword.h"

#ifndef FESTVOX_NO_THREADS
#include "./threading.h"
#endif

using std::cout;
using std::endl;
using std::ifstream;
using std::ofstream;
using std::ios;

wrdC *hwrd;
stC  *hst;

double **trw;
double **trx;
double *denW;
int _tst;
int _now;
int _dim;
int _noc;
int _nog;

int **tarUL;
int *lenU;
char **wavF;
int _nsen;

int _git;
int _maxIT = 30;
double _eps = 0.004;
int _citr = 6;  // Iteration after which the model get changed a bit
int _ssilF = 0;
double _ssilT = 0.01;
int _sitr = 0;

int _seqF = 0;
int _readF = 0;
int _pauID;  // 17 July 2005
int _spauID;
int _fullF = 0;  // Flag to indicate the states in a unit are fully connected...

// Flag to indicate slightly perturbed initialization of states, as
// apposed to Flat start..
int _prbF = 0;

int _skipF = 0;  // Flag to skip initial state..
char* mF, *mF1;

char *fDir;
char *tFext;
double avglh;
int acpT;
int acpF;
int _numthreads;

int current_iteration;

int next_sentence_number_to_process;

#ifndef FESTVOX_NO_THREADS
Mutex *global_queue_mutex;
Mutex *global_mutex;
Mutex *stdout_mutex;
#endif

void BuildEnv(char *fnm, wrdC*& hwrd, stC*& hst, int& now, int& tst, int& dim);
void LoadPromptFile(char *fnm, int**& tarUL, int*& lenU, char**&wavF, int& nos);
void LoadFeatFile(char *fnm, double**& feat, int&r, int&c);
void LoadBinaryFeatFile(char *filename, double*** feats,
                        int* num_rows, int* num_cols);
void Emissions(wrdC *hwrd, stC *hst, double**& emt, int& er,
               int& ec, double **ft, int fr, int fc,
               int *tar, int ltar, int*& stMap);
void PrintMatrix(double **arr, int r, int c, char *nm);
void FillStateTrans(double **arcW, int *tar, int ltar,
                    int *begR, int *nullI, int er);
void FillWordTrans(double **arcW, int *tar, int ltar,
                   double **trw, int *bI, int *eI);
void FillWordTrans_Seq(double **arcW, int *tar, int ltar, double **trw, int *bI,
                       int *eI, int**& fwM, int**& bwM, int er, int ssilF);

void InitWordTrans(double **trw, int r);
double AlphaBetas(double **emt, int r, int c, double ** trp,
                  double **alp, double **bet, double *nrmF,
                  int *bI, int *eI, int& nanF);
double AlphaBetas_Seq(double **emt, int r, int c, double ** trp, double **alp,
                      double **bet, double *nrmF, int *bI, int *eI,
                      int **fwM, int **bwM, int *nullI, int& nanF);
void NullifyWordAccum(double **trx, double *den, int r);
void AccumScores(double **emt, int er, int ec, double **alp,
                 double **bet, double **trp, double **ft, int fc,
                 double *nrmF, int *stMap, int *bI, int *eI,
                 double **trx, double *denW , int *begR,
                 int **fwM, int *nullI);
void NullifyWordTran(double **trx, double *den, int r);
void ReEstWordTran(double **trw, double **trx, double *denW, int nw);
void AccumWordTran(double *gtrn, int s, int *bI, int ns,
                   double **trx, double *denW, int *stMap);
void StoreWordTran(double **trw, int nw, char *mF);
void ReadEnv(char *fnm, wrdC*& hwrd, stC*& hst,
             int& now, int& tst, int& dim, ifstream& fp_md);
int IsShortPause(char *nm);
int IsSilence(char *nm);

void RemoveShortPauses(int*& tarM, int& lenM, int *srcA,
                       int srcL, int itr, int ssF);

void GenerateShortPause(wrdC *hwrd, stC *hst);
void ProcessSentence(int sentnum);

#ifndef FESTVOX_NO_THREADS
void* ThreadStart(void* userdata);
#endif

int main(int argc, char *argv[]) {
#ifndef FESTVOX_NO_THREADS
  // Initialize global mutex
  global_mutex = new PthreadMutex();
  stdout_mutex = new PthreadMutex();
  global_queue_mutex = new PthreadMutex();
#endif

  char *mstring1;
  char *mstring2;
  char *mstring3;

  if (argc < 11) {
    cout << "Usage: ./a.out <ph-list.int> <prompt-file> <seq-flag> "
         << "<retrain-Flag> <feat-dir> <extn> <mod-dir> <fully-cF> ";
#ifndef FESTVOX_NO_THREADS
    cout << "<prb-F> <skip-F> <maxIters> <numThreads>" << endl;
#else
    cout << "<prb-F> <skip-F> <maxIters>" << endl;
#endif  // FESTVOX_NO_THREADS
    exit(1);
  }
  // Fully-cF - Fully Connected flag
  // prb-F  - Perturbation flag
  // Skip-F - Skip Flag indicating skip of first state....

  int max_string_length = strlen(argv[7])+20;

  mstring1 = new char[max_string_length];
  mstring2 = new char[max_string_length];
  mstring3 = new char[max_string_length];

  snprintf(mstring1, max_string_length, "%s/%s", argv[7], "model100.txt");
  char *mF = mstring1;

  snprintf(mstring2, max_string_length, "%s/%s", argv[7], "model101.txt");
  char *mF1 = mstring2;
  ofstream fp_mod;

  ofstream fp_log100;
  snprintf(mstring3, max_string_length, "%s/%s", argv[7], "log100.txt");

  fp_log100.open(mstring3, ios::out);

  char *phF;
  char *prmF;
  phF  = argv[1];
  prmF = argv[2];
  _seqF = atoi(argv[3]);
  // cout<<"Sequential Flag set to "<<_seqF<<endl;

  _readF = atoi(argv[4]);
  // cout<<"Read Flag set to "<<_readF<<endl;

  //  char tF[kNmLimit * 2];
  fDir = argv[5];
  tFext = argv[6];
  // cout<<"Feature Path: "<<fDir<<endl;
  // cout<<"Feature exten: "<<tFext<<endl;

  _fullF = atoi(argv[8]);
  // cout<<"Fully-Connected Flag set to "<<_fullF<<endl;
  _prbF = atoi(argv[9]);
  // cout<<"Perturbation  Flag set to "<<_prbF<<endl;

  // if (1 == _fullF) {
  // _prbF = 1;
  // cout<<"Perturbation  Flag reset to "<<_prbF
  //     <<" due to switch on Full-connected Flag.."<<endl;
  // }


  _skipF = atoi(argv[10]);
  // cout<<"Skip First State Flag set to "<<_skipF<<endl;
  // cout<<"SHORT PAUSE WOULD BE INTRODUCED AT "<<_citr<<" iteration "<<endl;
  // cout<<"SHORT PAUSE WOULD BE INTRODUCED WHEN diff is < "<<_ssilT<<endl;

  _maxIT = atoi(argv[11]);

  _numthreads = 1;

#ifndef FESTVOX_NO_THREADS
  if (argc > 12)
    _numthreads = atoi(argv[12]);
#endif

  double diff = 1000;
  double prev = -1.0e+32;

  if (1 == _readF) {
    ifstream fp_r;
    FileExist(mF1);
    fp_r.open(mF1, ios::in);

    // Read the ph_list file; create objects
    ReadEnv(phF, hwrd, hst, _now, _tst, _dim, fp_r);
    fp_r.close();

    fp_mod.open(mF1, ios::out);
    fp_mod.close();

  } else {
    fp_mod.open(mF1, ios::out);
    fp_mod.close();

    // Read the ph_list file; create objects
    BuildEnv(phF, hwrd, hst, _now, _tst, _dim);
  }

  fp_mod.open(mF, ios::out);
  fp_mod.close();

  Alloc2f(trw, _now, _now);  // transition weights
  Alloc2f(trx, _now, _now);
  Alloc1f(denW, _now);

  InitWordTrans(trw, _now);  // Random initialization....
  LoadPromptFile(prmF, tarUL, lenU, wavF, _nsen);  // Load Prompt file....

  // Store the models..............
  for (int i = 0; i < _tst; i++) {
    hst[i].StoreModel(mF);
  }
  StoreWordTran(trw, _now, mF);

  current_iteration = 0;

  cout << "Total Prompts: " << _nsen << endl;
  while ((diff > _eps || _sitr < 4) && current_iteration < _maxIT) {
    avglh = 0;
    acpT = 0;
    acpF = 0;
    _git = current_iteration + 1;

    if (_ssilF == 1) {
      _sitr++;
    }


    // Sentences will be processed in order from 0 to _nsen but in
    // different threads if threading is enabled

    next_sentence_number_to_process = 0;

#ifndef FESTVOX_NO_THREADS
    Thread** threadList = new Thread*[_numthreads];
    for (int i = 0; i < _numthreads; i++) {
      Thread *newthread   = StartJoinableThread(ThreadStart);
      threadList[i] = newthread;
    }

    for (int i = 0; i < _numthreads; i++) {
      threadList[i]->Join();
      delete threadList[i];
    }
    delete [] threadList;
#else
    for (int i = 0; i < _nsen; i++) {
      next_sentence_number_to_process = i;
      ProcessSentence(next_sentence_number_to_process);
    }
#endif  // FESTVOX_NO_THREADS

    // avglh /= _nsen;
    // avglh /= acpT;
    avglh /= acpF;

    for (int i = 0; i < _tst; i++) {
      hst[i].ReEstimate();
      hst[i].NullifyAccum();
    }

    if (_seqF != 1) {
      ReEstWordTran(trw, trx, denW, _now);
      NullifyWordTran(trx, denW, _now);
    }
    // PrintMatrix(trw, _now, _now, "TRANS....");

    // diff = (prev - avglh) / prev;
    diff = avglh - prev;
    cout << "Average Likelihood: (" << current_iteration+1<< ") "
         << avglh << " " << diff << endl;
    fp_log100 << "Avg. likelihood: (" << current_iteration+1 << ") "
              << avglh << " " << diff<< endl;
    cout << "Accepted sentences are: " << acpT << " / " << _nsen << endl;
    fp_log100 << "Accepted sentences are: " << acpT << " / " << _nsen << endl;
    prev = avglh;
    current_iteration++;

    // After _citr iterations...
    // Copy the silence middle state to short pause model
    // if (_git == _citr - 1) {  // Does this act only one time....
    if (diff < _ssilT && 0 == _ssilF) {  // Does this act only one time....
      cout << "GENERATING SHORT PAUSE: AT " << _git << " iteration" << endl;
      GenerateShortPause(hwrd, hst);
      _ssilF = 1;
    }

    // Store the models..............
    for (int i = 0; i < _tst; i++) {
      hst[i].StoreModel(mF);
    }
    StoreWordTran(trw, _now, mF);
  }

  // Store the final models..............
  for (int i = 0; i < _tst; i++) {
    hst[i].StoreModel(mF1);
  }
  StoreWordTran(trw, _now, mF1);


  fp_log100.close();

  Delete2f(trw, _now);
  Delete2f(trx, _now);
  Delete1f(denW);

#ifndef FESTVOX_NO_THREADS
  delete global_mutex;
  delete stdout_mutex;
  delete global_queue_mutex;
#endif

  for(int i = 0; i < _nsen; i++) {
    delete[] wavF[i];
    delete[] tarUL[i];
  }
  delete[] wavF;
  delete[] tarUL;
  delete[] lenU;

  int create_memory_for_accumulators = 1;
  for (int i = 0; i < _tst; i++) {
    hst[i].FreeStorage(create_memory_for_accumulators);
  }
  delete[] hwrd;
  delete[] hst;
  delete[] mstring1;
  delete[] mstring2;
  delete[] mstring3;
}

void RemoveShortPauses(int*& tarM, int& lenM,
                       int *srcA, int srcL, int itr, int ssF) {
  Alloc1d(tarM, srcL);

  // if (itr < _citr) {
  if ( 0 == ssF ) {
    // Ignore the short pauses...
    lenM = 0;
    for (int i = 0; i < srcL; i++) {
      if (srcA[i] != _spauID) {
        tarM[lenM] = srcA[i];
        lenM++;
      }
    }

  } else {
    // Align the short pauses too......
    // if (itr == _citr) {
    // cout<<"Introducing SILENCE PAUSE AT "<<itr<<endl;
    // }
    for (int i = 0; i < srcL; i++) {
      tarM[i] = srcA[i];
    }
    lenM = srcL;
  }
}

#ifndef FESTVOX_NO_THREADS
void* ThreadStart(void* /*unused*/) {
  int current_sentence_number_to_process;

  while (1) {
    // See if the next sentence number to be processed exists
    {
      // Scope to define a mutex lock
      ScopedLock sl(global_queue_mutex);
      if (next_sentence_number_to_process >= _nsen)
        break;  // We are done. Thread can exit.

      current_sentence_number_to_process = next_sentence_number_to_process;
      next_sentence_number_to_process++;
      // End of mutex lock scope
    }

    // Don't lock this function with the mutex!
    ProcessSentence(current_sentence_number_to_process);
  }
  return NULL;
}
#endif  // FESTVOX_NO_THREADS

void ProcessSentence(int sentnum) {
  double **ft;
  int fr;
  int fc, p;

  double **emt;
  double **arcW;
  int er;
  int ec;

  double **alp;
  double **bet;
  double *nrmF;

  int *bI;
  int *eI;
  int *stMap;

  double lh;
  int nanF;
  int *tarM;  // target Models
  int lenM;   // No. of models

  int *begR;  // array to hold begining (arc) rows of each state.
  // Each state is represented by a row in the transition matrix;
  // Thus row and state are interchangably used...

  int **fwM;
  int **bwM;
  int *nullI;  // Array to say whether a state is null or not;
  char tF[kNmLimit];

  p = sentnum;
  snprintf(tF, kNmLimit, "%s/%s.%s", fDir, wavF[p], tFext);

  {
    // Lock Stdout so that output comes clean and not interleaved
    // between threads
#ifndef FESTVOX_NO_THREADS
    ScopedLock sl(stdout_mutex);
#endif
    cout << "Processing sentence "<< sentnum <<" in iteration "
         << current_iteration+1 << " of max "<< _maxIT << endl;
  }
  // cout<<"Feat file loading....."<<endl;

  // LoadFeatFile(tF, ft, fr, fc);
  LoadBinaryFeatFile(tF, &ft, &fr, &fc);

  // PrintMatrix(ft, fr, fc, "FEAT-FILE");

  // Remove short pauses from the alignment for initial 6 iterations...
  RemoveShortPauses(tarM, lenM, tarUL[p], lenU[p], _git, _ssilF);

  // cout<<"In emissions"<<endl;
  // ****Emissions(hwrd, hst, emt, er, ec, ft,
  //               fr, fc, tarUL[p], lenU[p], stMap);

  Emissions(hwrd, hst, emt, er, ec, ft, fr, fc, tarM, lenM, stMap);
  // PrintMatrix(emt, er, 15, "EMISSIONS");
  // cout<<"Out emissions"<<endl;

  Alloc2f(arcW, er, er);
  Alloc1d(bI, er);
  Alloc1d(eI, er);
  // bI and eI matrices indicate which state are beginner and enders
  // of the words

  // This info is need to intialize the alpha at 0th column and n-1th
  // for beta

  // begR is an array holds the begining (arc) row number of a state model
  Alloc1d(begR, er);
  Alloc1d(nullI, er);

  // ***FillStateTrans(arcW, tarUL[p], lenU[p]);
  FillStateTrans(arcW, tarM, lenM, begR, nullI, er);
  // PrintMatrix(arcW, er, er, "ARC-MAT");

  if (1 == _seqF) {
    // **FillWordTrans_Seq(arcW, tarUL[p], lenU[p], trw, bI, eI, fwM, bwM, er);
    FillWordTrans_Seq(arcW, tarM, lenM, trw, bI, eI, fwM, bwM, er, _ssilF);
  } else {
    // ****FillWordTrans(arcW, tarUL[p], lenU[p], trw, bI, eI);
    FillWordTrans(arcW, tarM, lenM, trw, bI, eI);
  }
  // PrintMatrix(arcW, er, er, "ARC-MAT");

  Alloc2f(alp, er, ec);
  Alloc2f(bet, er, ec);
  Alloc1f(nrmF, ec);
  // cout<<"In Alp-Beta"<<endl;
  if (1 == _seqF) {
    lh = AlphaBetas_Seq(emt, er, ec, arcW, alp, bet, nrmF,
                        bI, eI, fwM, bwM, nullI, nanF);
  } else {
    lh = AlphaBetas(emt, er, ec, arcW, alp, bet, nrmF, bI, eI, nanF);
  }
  // cout<<"Out Alp-Beta, LKL HOOD: "<<lh<<endl;

  if (1 == nanF) {
    // continue;
    cout << "Prompt: "<< p+1 << " discarded due to NAN problem" << endl;
  } else {
    {
      // Scope that's thread protected
#ifndef FESTVOX_NO_THREADS
      ScopedLock sl(global_mutex);
#endif
      avglh += lh;
    }

    //    AccumScores(emt, er, ec, alp, bet, arcW, ft, fc, nrmF,
    //                stMap, bI, eI, trx, denW, begR, fwM, nullI);
    // cout<<"Accum back..."<<endl;
    {
#ifndef FESTVOX_NO_THREADS
      ScopedLock sl(global_mutex);
#endif
      AccumScores(emt, er, ec, alp, bet, arcW, ft, fc, nrmF,
                  stMap, bI, eI, trx, denW, begR, fwM, nullI);
      acpT++;
      acpF += ec;
    }
  }

  Delete2f(ft, fr);
  Delete2f(emt, er);
  Delete2f(arcW, er);
  Delete2f(alp, er);
  Delete2f(bet, er);
  Delete1f(nrmF);
  Delete1d(bI);
  Delete1d(eI);
  Delete1d(stMap);
  Delete1d(tarM);
  Delete1d(begR);
  Delete2d(fwM, er);
  Delete2d(bwM, er);
  Delete1d(nullI);
}

void StoreWordTran(double **trw, int nw, char *mF) {
  ofstream fp_out;
  fp_out.open(mF, ios::app);

  fp_out << nw << " " << nw << endl;

  for (int i = 0; i < nw; i++) {
    for (int j = 0; j < nw; j++) {
      fp_out << trw[i][j] << " ";
    }
    fp_out << endl;
  }
  fp_out.close();
}

void BuildEnv(char *fnm, wrdC*& hwrd, stC*& hst, int& now, int& tst, int& dim) {
  char tstr[kNmLimit];
  int noc;
  int nog;

  char *tfNM;
  int max_string_length = strlen(fnm)+20;
  tfNM = new char[max_string_length];
  snprintf(tfNM, max_string_length, "%s_log", fnm);

  ofstream fp_pl;
  fp_pl.open(tfNM, ios::out);

  FileExist(fnm);

  ifstream fp_in;
  fp_in.open(fnm, ios::in);

  // Read tstr and no-of-words
  fp_in >> tstr >> now;
  cout << tstr << " " << now << endl;
  fp_pl << now << endl;

  // Read tstr and no-of-states
  fp_in >> tstr >> tst;
  cout << tstr << " " << tst << endl;

  // Read tstr and feature dimension
  fp_in >> tstr >> dim;
  cout << tstr << " " << dim << endl;


  hwrd = new wrdC[now];
  hst  = new stC[tst];

  char nm[kNmLimit];
  int  nst;
  int  bst;
  int  est;
  int  wid = 0;
  int  sid = 0;
  int  st  = 0;

  int t_bs;
  int t_es;
  int t_noc;
  int skipF = 0;

  int conBS;
  int t_nullF;


  for (int i = 0; i < now; i++) {
    skipF = 0;
    fp_in >> wid >> nm >> nst >> noc >> nog;
    fp_pl << nm << " " << nst << " ";

    bst = st;
    est = st + (nst - 1);

    hwrd[wid].init(bst, est, nst, wid, nm);

    // Get SIL ID and short pause id from here....
    if (IsSilence(nm)) {
      _pauID  = wid;
    } else if (IsShortPause(nm)) {
      _spauID = wid;
    }

    // no of connections should NOT be greater than no of states....
    if (noc < 1) {
      cout << "No of connections should be atleast one..." << endl;
      cout << "Check at: " << nm << endl;
      exit(1);
    } else if (noc > nst) {
      cout << "No of connections -greater than- " << nst << endl;
      cout << "Check at: " << nm << endl;
      exit(1);
    }

    if (1 == _fullF && noc != nst) {
      cout << "Fully connected flag is up, but  " << noc
           << " should be equal to NOS: " << nst << endl;
      cout << "Check at: " << nm << endl;
      exit(1);
    }

    for (int j = 0; j < nst; j++) {
      t_bs = -1;
      t_es = -1;
      t_noc = noc;
      t_nullF = 0;

      // sid is the current state id;
      // bst is the begining state of the word.
      // est is the ending state of the word..

      if (1 == _fullF) {
        // conBS = bst;
        conBS = bst + 1;

        // States should not connect to begining state except the last
        // null state.
        t_noc = noc - 1;
      } else {
        conBS = sid;
        if (1 == _skipF) {
          t_noc = est - sid + 1;
        }
      }

      if (j == 0) {
        conBS = bst;  // It has to be bst always for j = 0;
        t_bs = wid;
        t_nullF = 1;
        if (1 == _skipF) {  // Skip Flag to indicate skipping first state.
          t_noc = noc;
          if (noc >= nst) {
            t_noc = noc - 1;
          }
        } else {
          t_noc = 2;  // Traditional way NullState --> First state.
        }
      } else if (j == nst - 1) {
        // Last state would get only one connection.
        t_es = wid;
        t_nullF = 1;
        t_noc = 1;
      }

      // Correcting the no of connections so that they would not
      // overflow the word model it is case of skip states....
      if (conBS + (t_noc - 1) > est) {
        // This if loop would be executed only for non-fully connected models
        t_noc = 2;
        if (j == nst-1) {
          t_noc = 1;  // confining the last node a self transition....
        }
        cout << "FOR STATE: " << sid << " RESETTING THE CONNECTIONS TO "
             << t_noc << endl;
      }

      int create_memory_for_accumulators = 1;
      hst[sid].Create(nog, t_noc, dim, t_bs, t_es, sid,
                      wid, conBS, t_nullF, create_memory_for_accumulators);

      // cout<<"Calling RADNOM INIT() "<<endl;
      hst[sid].RandomInit(_prbF);  // Perturbation Flag......

      hst[sid].NullifyAccum();
      fp_pl << sid << " ";
      sid++;
    }
    fp_pl << endl;

    // wid++; not required.....
    st = st + nst;
  }
  fp_in.close();
  fp_pl.close();

  delete[] tfNM;
}

void LoadPromptFile(char *fnm, int**& tarUL,
                    int*& lenU, char**&wavF, int& nos) {
  FileExist(fnm);

  ifstream fp_in;
  fp_in.open(fnm, ios::in);

  fp_in >> nos;  // Read no-of-sentences....

  wavF = new char*[nos];   // wavF contains wave file names
  tarUL = new int*[nos];   // tarUL[sen][words]
  lenU = new int[nos];     // lenU[sen] - tells the length of each sentence

  int tl;

  for (int i = 0; i < nos; i++) {
    wavF[i] = new char[kNmLimit];
    fp_in >> wavF[i];    // Read the wave/feat file prefix

    fp_in >> lenU[i];  // ReadLength
    tl = lenU[i];

    tarUL[i] = new int[tl];

    for (int j = 0; j < tl; j++) {
      fp_in >> tarUL[i][j];   // Read the units - words/phones/units
    }
  }
  fp_in.close();
}

void LoadFeatFile(char *fnm, double**& feat, int&r, int&c) {
  FileExist(fnm);
  ifstream fp_in;
  fp_in.open(fnm, ios::in);

  fp_in >> r >> c;
  Alloc2f(feat, r, c);
  for (int i = 0; i < r; i++) {
    for (int j = 0; j < c; j++) {
      fp_in >> feat[i][j];
    }
  }
  fp_in.close();
}

void LoadBinaryFeatFile(char *filename, double*** feats_ptr,
                        int* num_rows, int* num_cols) {
  FileExist(filename);

  FILE* input_file;
  double** feats;
  double feat;

  input_file = fopen(filename, "rb");
  if (input_file == NULL) {
    cout << "Error opening file: "
         << filename << endl;
    cout << "Aborting." << endl;
    exit(-1);
  }

  fread(num_rows, sizeof(*num_rows), 1, input_file);
  fread(num_cols, sizeof(*num_cols), 1, input_file);

  feats = new double*[*num_rows];
  for (int row = 0; row < *num_rows; row++) {
    feats[row] = new double[*num_cols];
    fread(feats[row], sizeof(feat), *num_cols, input_file);
  }
  *feats_ptr = feats;
  fclose(input_file);
}

void Emissions(wrdC *hwrd, stC *hst, double**& emt,
               int& er, int& ec, double **ft,
               int fr, int fc, int *tar, int ltar, int*& stMap) {
  int tw;
  int bs;
  int sid;
  int rn;

  ec = fr;
  er = 0;
  for (int i = 0; i < ltar; i++) {
    tw = tar[i];
    er += hwrd[tw].nst;
  }
  Alloc2f(emt, er, ec);
  Alloc1d(stMap, er);

  rn = 0;
  for (int i = 0; i < ltar; i++) {
    tw = tar[i];
    bs = hwrd[tw].bst;
    for (int j = 0; j < hwrd[tw].nst; j++) {
      sid = bs + j;
      stMap[rn] = sid;
      // for (int k = 0; k < ec; k++) {
      //  emt[rn][k] = hst[sid].GauProb(ft[k]);
      // }
      rn++;
    }
  }

  if (rn != er) {
    cout << "ER AND RN " << er << " " << rn << " are expected to match...";
    cout << "Possible error... aborting..." << endl;
    exit(1);
  }

  int ts;
  double *cshP;
  Alloc1f(cshP, _tst);
  for (int k = 0; k < ec; k++) {
    // Calculate Gaussian for all states.....
    for (int s = 0; s < _tst; s++) {
      cshP[s] = hst[s].GauProb(ft[k]);
    }

    for (int j = 0; j < er; j++) {
      ts = stMap[j];
      emt[j][k] = cshP[ts];
    }
  }
  Delete1f(cshP);
}

void PrintMatrix(double **arr, int r, int c, char *nm) {
  cout << "Printing " << nm << endl;
  for (int i = 0; i < r; i++) {
    cout << "I = " << i << " ";
    for (int j = 0; j < c; j++) {
      cout << arr[i][j] << " ";
    }
    cout << endl;
  }
}

void FillStateTrans(double **arcW, int *tar, int ltar,
                    int *begR, int *nullI, int er) {
  int rn;  // row number
  int tw;  // temporary word
  int bs;  // begin state
  int sid;  // state id
  int nxt;  // next state

  int srn;  // store row number...
  int barn;  // begining arc row number;;

  rn = 0;
  for (int i = 0; i < ltar; i++) {
    tw = tar[i];
    bs = hwrd[tw].bst;

    srn = rn;  // store the row number of the word begining state...

    for (int j = 0; j < hwrd[tw].nst; j++) {
      sid = bs + j;

      if (hst[sid].getconBS() == bs + 1) {
        // Useful for fully connected cases....
        barn = srn + 1;
      } else {
        barn = rn;  // rn indicates current row number...
      }

      begR[rn] = barn;

      // 1 or 0, depending on null state or not..
      nullI[rn] = hst[sid].getnullF();


      for (int k = 0; k < hst[sid].getnc(); k++) {
        nxt = barn + k;
        arcW[rn][nxt] = hst[sid].trans(k);
        if (nxt >= er) {
          cout << "ARRAY LIMITS CROSSED... NXT: "
               << nxt << " MAX: " << er << endl;
          exit(1);
        }
      }
      rn++;
    }
  }
}

void FillWordTrans(double **arcW, int *tar, int ltar,
                   double **trw, int *bI, int *eI) {
  int rn;
  int tw;
  int bs;
  int es;
  int cw;
  int nw;

  int *esa;
  int *bsa;
  int *wa;

  int nos;

  double sum;
  double estrn;
  double bias;

  Alloc1d(esa, ltar);
  Alloc1d(bsa, ltar);
  Alloc1d(wa, ltar);

  rn = 0;
  nos = 0;

  for (int i = 0; i < ltar; i++) {
    tw = tar[i];
    bsa[i] = rn;
    bI[rn] = 1;      // bI[st] = 1; useful for initialization of trellis....

    esa[i] = rn + (hwrd[tw].nst - 1);
    eI[esa[i]] = 1;  // eI[st] = 1; useful for initialization of trellis...

    wa[i]  = tw;
    rn += hwrd[tw].nst;

    nos += hwrd[tw].nst;
  }

  for (int i = 0; i < ltar; i++) {
    es = esa[i];
    cw = wa[i];

    /*This code stops transition leaks, if any.. */
    sum = 0;     // Compute the summations of the remianing trans.
    for (int j = 0; j < ltar; j++) {
      bs = bsa[j];
      nw = wa[j];
      sum += trw[cw][nw];
    }
    estrn = arcW[es][es];  // Take end state transition
    bias  = (1.0 - (estrn + sum)) / (ltar + 1);  // estrn + sum should be 1
    // if not get the bias to be distributed..
    // ltar + 1 is for self-transition.
    if ( bias < 0 ) {
      cout << "Bias is < 0 " << endl;
      // likely in the order of e-17, due to floating point errors..

      cout << "Bias: " << bias << endl;
      cout << "Rounding to 0" << endl;
      bias = 0;
    }

    // bias = 0; Make bias = 0 to nullify the effect of above piece of code */

    for (int j = 0; j < ltar; j++) {
      bs = bsa[j];
      nw = wa[j];
      arcW[es][bs] = trw[cw][nw] + bias;
    }
    arcW[es][es] += bias;  // Adding bias to the self transition tooo...
  }

  /* for (int i = 0; i < nos; i++) {
  //   double tot = 0;
  //   for (int j = 0; j < nos; j++) {
  //      tot += arcW[i][j];
  //   }
  } */

  Delete1d(esa);
  Delete1d(bsa);
  Delete1d(wa);
}

void InitWordTrans(double **trw, int r) {
  double mas = 0.2;
  double dis = mas/r;

  for (int i = 0; i < r; i++) {
    for (int j = 0; j < r; j++) {
      trw[i][j] = dis;
    }
  }
}

double AlphaBetas(double **emt, int r, int c,
                  double ** trp, double **alp,
                  double **bet, double *nrmF,
                  int *bI, int *eI, int& nanF) {
  int ns;
  int nt;
  int t;
  int s;
  int tm1;
  int tp1;
  double lhd;
  double maxV;

  ns = r;
  nt = c;
  t = 0;
  s = 0;
  maxV = -1.0e+32;
  for (s = 0; s < ns; s++) {
    if (1 == bI[s]) {
      alp[s][t] = emt[s][t];
    } else {
      alp[s][t] = 0;
    }

    if (maxV < alp[s][t]) {
      maxV = alp[s][t];
    }
  }
  nrmF[t] = maxV;
  for (s = 0; s < ns; s++) {
    alp[s][t] /= nrmF[t];
  }

  for (t = 1; t < nt; t++) {
    tm1 = t - 1;
    maxV = -1.0e+32;

    for (s = 0; s < ns; s++) {
      alp[s][t] = 0;

      int nj = 0;
      for (int p = 0; p < ns; p++) {
        alp[s][t] += alp[p][tm1] * trp[p][s];
        if (trp[p][s] > 0) {
          nj++;
        }
      }
      alp[s][t] *= emt[s][t];

      if (maxV < alp[s][t]) { maxV = alp[s][t];
      }
    }
    nrmF[t] = maxV;

    for (s = 0; s < ns; s++) {
      alp[s][t] /= nrmF[t];
    }
  }

  // Beta computation...

  t = nt - 1;
  for (s = ns - 1; s >= 0; s--) {
    if (1 == eI[s]) {
      bet[s][t] = 1.0;
    } else {
      bet[s][t] = 0;
    }
  }

  for (t = nt - 2; t >=0; t--) {
    tp1 = t + 1;

    for (s = ns - 1; s >= 0; s--) {
      bet[s][t] = 0;
      int nh = 0;
      for (int n = ns - 1; n >= 0; n--) {
        bet[s][t] += bet[n][tp1] * trp[s][n] * emt[n][tp1];
        if (trp[s][n] > 0) {
          nh++;
        }
      }
      bet[s][t] /= nrmF[tp1];
    }
  }

  // Check for NANs in the trellis.....

  nanF = 0;
  for (t = 0; t < nt; t++) {
    for (s = 0; s < ns; s++) {
      double gam = alp[s][t] * bet[s][t];
      if (IsNAN(gam)) {
        nanF = 1;
        break;
      }
    }
  }

  lhd = 0;
  for (t = 0; t < nt; t++) {
    lhd += log(nrmF[t]);
  }
  return (lhd);
}

void AccumScores(double **emt, int er, int ec,
                 double **alp, double **bet, double **trp,
                 double **ft, int fc, double *nrmF, int *stMap,
                 int *bI, int *eI, double **trx, double *denW ,
                 int *begR, int **fwM, int *nullI)  {
  // cout<<"In Accumscores... "<<endl;

  // int fr = ec;
  int nt = ec;
  int ns = er;
  int t = 0;
  int s = 0;
  int tp1 = 0;
  int sid;
  int wid;

  int barn;

  double *gtrn;
  double gam;
  // Alloc2f(gtrn, ns, ns);
  Alloc1f(gtrn, ns);
  // Alloc2f(gam, ns, nt);

  int myc = 0;
  int n = 0;
  int tit;

  for (t = 0; t < nt - 1; t++) {
    // Compute until nt - 2 to accommodate tp1...

    // The last column gammas are not to be accounted as there are no
    // further transitions....

    // Reset the cached time for each state.
    for (s = 0; s < _tst; s++) {
      hst[s].resetcchT();
    }

    tp1 = t + 1;
    double sumG = 0;
    for (s  = 0; s < ns; s++) {
      // gam[s][t] = alp[s][t] * bet[s][t];
      gam = alp[s][t] * bet[s][t];

      sid = stMap[s];
      wid = hst[sid].getwid();
      // hst[sid].AccumFeat(ft[t], gam[s][t], t);
      hst[sid].AccumFeat(ft[t], gam, t);

      for (myc = 0; myc < fwM[s][0]; myc++) {
        n = fwM[s][myc + 1];
        // cout<<"S: "<<s<<" N: "<<n<<" T: "<<trp[s][n]<<endl;

        if (1 == nullI[n]) {
          tit = t;
          gtrn[n] = alp[s][t] * trp[s][n] * bet[n][tit];
          // As there is no emission, there is no normalization..
        } else {
          tit = tp1;
          gtrn[n] = alp[s][t] * trp[s][n] *
              emt[n][tit] * bet[n][tit] / nrmF[tit];
        }
        // if (1 == nullI[n] && s != n) {
        // if (1 != nullI[n] && s != n) {
        //   cout<<"S: "<<s<<" N: "<<n<<" GT: "<<gtrn[n]<<endl;
        // }
      }

      barn = begR[s];
      hst[sid].AccumTran(gtrn, barn);

      if (1 == eI[s] && _seqF != 1) {
        AccumWordTran(gtrn, s, bI, ns, trx, denW, stMap);
        // denW[wid] += gam[s][t];
        denW[wid] += gam;
      }
      // sumG += gam[s][t];

      if (1 != nullI[s]) {
        sumG += gam;
      }
    }

    // if (_git > 0) {
    // printf("%d: %-10.8f\n", t, sumG);
    // cout<<sumG<<endl;
    // }
    // cout<<"sumg: "<<t<<" / "<<nt<<" "<<sumG<<endl<<endl;
  }

  Delete1f(gtrn);
  // Delete2f(gam, ns);
}

void AccumWordTran(double *gtrn, int s, int *bI, int ns,
                   double **trx, double *denW, int *stMap) {
  int sid = stMap[s];
  int wid = hst[sid].getwid();

  int tsd;
  int twd;

  for (int i = 0; i < ns; i++) {
    if (1 == bI[i]) {
      tsd = stMap[i];
      twd = hst[tsd].getwid();
      trx[wid][twd] += gtrn[i];
    }
  }
}

void ReEstWordTran(double **trw, double **trx, double *denW, int nw)  {
  double d;
  double tot;
  double tot1;

  for (int i = 0; i < nw; i++) {
    // int e1 = hwrd[i].est;
    // double stD = hst[e1].getstD();
    d  = denW[i];

    tot = 0;
    tot1 = 0;
    for (int j = 0; j < nw; j++) {
      trw[i][j] = trx[i][j] / d;
      tot += trw[i][j];
    }

    int est = hwrd[i].est;
    tot1 = hst[est].trans(0);
    tot1 += tot;
  }
}

void NullifyWordTran(double **trx, double *den, int r) {
  for (int i = 0; i < r; i++) {
    for (int j = 0; j < r; j++) {
      trx[i][j] = 0;
    }
    den[i] = 0;
  }
}

void FillWordTrans_Seq(double **arcW, int *tar, int ltar,
                       double **trw, int *bI, int *eI,
                       int**& fwM, int**& bwM, int er, int ssilF) {
  // There is no need of trw matrix here....

  int rn;
  int tw;
  int bs;
  int es;

  int *esa;
  int *bsa;
  int *wa;

  int nos;

  double estrn;
  double rest;

  int e1s;
  int b1s;
  double val;

  Alloc1d(esa, ltar);
  Alloc1d(bsa, ltar);
  Alloc1d(wa, ltar);

  rn = 0;
  nos = 0;

  for (int i = 0; i < ltar; i++) {
    tw = tar[i];
    bsa[i] = rn;
    bI[rn] = 1;      // bI[st] = 1; useful for initialization of trellis....

    esa[i] = rn + (hwrd[tw].nst - 1);
    eI[esa[i]] = 1;  // eI[st] = 1; useful for initialization of trellis...

    wa[i]  = tw;

    rn += hwrd[tw].nst;
    nos += hwrd[tw].nst;
  }

  if (rn != er) {
    cout << "Count error of er and rn in FillWordTrans_Seq()" << endl;
    exit(1);
  }

  for (int i = 0; i < ltar; i++) {
    es = esa[i];
    bs = bsa[i];

    estrn = 0;
    for (int z = bs; z <= es; z++) {
      estrn += arcW[es][z];  // Sum the transitions of the last state....
    }
    // estrn = arcW[es][es];  // Take end state transition

    rest  = 1.0 - estrn;  // estrn + sum should be 1

    // Break rest - remaining into half, if the wid is PAU - July 17 2005
    if (tar[i] == _pauID && i != ltar-1) {  // Don't split for the last state..
      rest = rest/2.0;
    }
    //

    if (i == ltar - 1) {
      // arcW[es][es] += rest;
      // The last state of the sentence will have a 1.0 self transition.
      // The Last state is a null state, so no need to add any transition
    } else {
      bs = bsa[i+1];
      arcW[es][bs] = rest;
    }

    // cout<<"Pause Id: "<<_pauID<<" Short Pause id: "<<_spauID<<endl;

    // if tar[i] matches with PAU, then assign the second half of the
    // rest to self-word transition - 17 July 2005

    if (tar[i] == _pauID) {
      if (1 == ssilF) {
        // Self loop only in the last stages...
        bs = bsa[i];
        arcW[es][bs] = rest;
        // arcW[es][bs] will not be zero in case of fully connnected....
        // 17 Oct: It will be zero as es is a null state now
      } else if (i < ltar - 1) {
        bs = bsa[i+1];
        arcW[es][bs] += rest;
      }
    } else if (tar[i] == _spauID) {
      bs = bsa[i];
      if ((i + 1 >= ltar)  || (i - 1 < 0)) {
        cout << "Cannot have a Short Pause at the end/begin of a sentence "
             << endl;
        exit(1);
      }
      e1s = esa[i-1];  // ending state of previous word
      b1s = bsa[i+1];  // begining state of next word

      val = arcW[e1s][bs] / 2.0;
      arcW[e1s][bs] = val;   // connect previous word and current word
      arcW[e1s][b1s] = val;  // connect previous word and next word
    }
    //
  }

  fwM = new int*[er];
  bwM = new int*[er];

  for (int i = 0; i < er; i++) {
    int cn = 0;
    for (int j = 0; j < er; j++) {
      if (arcW[i][j] > 0) {
        cn++;
      }
    }
    if (cn == 0 && i != er - 1) {
      cout << "State number is " << i
           << " : It does not seem to have any connections from it.."
           <<endl;
      cout << "Max states are: " << er << endl;
      exit(1);
    }

    fwM[i] = new int[cn + 1];
    fwM[i][0] = cn;

    int mi = 1;
    for (int j = 0; j < er; j++) {
      if (arcW[i][j] > 0) {
        fwM[i][mi] = j;
        mi++;
      }
    }
  }

  for (int j = 0; j < er; j++) {
    int cn = 0;
    for (int i = 0; i < er; i++) {
      if (arcW[i][j] > 0) {
        cn++;
      }
    }

    // if (j == 4) { cout<<" COUNT: "<<cn<<endl; }

    if (cn == 0 && j != 0) {
      cout << "State number is "<< j
           << " : It does not seem to have any connections to it.. "
           <<endl;
      cout << "Max states are: " << er << endl;
      exit(1);
    }

    bwM[j] = new int[cn + 1];
    bwM[j][0] = cn;

    int mj = 1;
    for (int i = 0; i < er; i++) {
      if (arcW[i][j] > 0) {
        bwM[j][mj] = i;
        mj++;
      }
    }
  }

  Delete1d(esa);
  Delete1d(bsa);
  Delete1d(wa);
}

double AlphaBetas_Seq(double **emt, int r, int c,
                      double ** trp, double **alp, double **bet,
                      double *nrmF, int *bI, int *eI,
                      int **fwM, int **bwM, int *nullI, int& nanF) {
  int ns;
  int nt;
  int t;
  int s;
  int tm1;
  int tp1;
  double lhd;
  double maxV;

  ns = r;
  nt = c;
  t = 0;
  s = 0;
  maxV = -1.0e+32;

  int p, myc, n;
  int tit;

  // cout<<"NS x NT: "<<ns<<" x "<<nt<<endl;

  alp[s][t] = 0;  // s = 0; and it is a null state..
  for (int myc = 0; myc < fwM[s][0]; myc++) {
    int n = fwM[s][myc + 1];
    alp[n][t] = emt[n][t] * trp[s][n];
    if (maxV < alp[n][t]) {
      maxV = alp[n][t];
    }
  }

  for (s = 0; s < ns; s++) {
    tit = t;
    if (1 == nullI[s]) {
      for (myc = 0; myc < bwM[s][0]; myc++) {
        p = bwM[s][myc+1];
        alp[s][t] += alp[p][tit] * trp[p][s];
        // cout<<" cn: "<<bwM[s][0]<<" P S "<<p<<" "
        //     <<s<<" TRP: "<<trp[p][s]<<" "<<alp[s][t]<<endl;
      }
      // cout<<"S: "<<s<<" ALP: "<<alp[s][t]<<" CN: "<<bwM[s][0]<<endl;
      for (myc = 0; myc < fwM[s][0]; myc++) {
        n = fwM[s][myc + 1];
        if (1 == nullI[n] && n < s) {
          alp[n][t] += alp[s][t] * trp[s][n];  // n being null, emission is 1;
          // cout<<" II cn: "<<fwM[s][0]<<" S "<<s
          //     <<" N "<<n<<" TRP: "<<trp[s][n]<<" "<<alp[n][t]<<endl;
          if (maxV < alp[n][t]) {
            maxV = alp[n][t];
          }
        }
      }
    }
    if (maxV < alp[s][t]) { maxV = alp[s][t];
    }
  }

  nrmF[t] = maxV;

  for (s = 0; s < ns; s++) {
    alp[s][t] /= nrmF[t];
  }

  // cout<<"Max: "<<t<<" / "<<nt<<" "<<nrmF[t]<<endl;

  for (t = 1; t < nt; t++) {
    tm1 = t - 1;
    maxV = -1.0e+32;

    for (s = 0; s < ns; s++) {
      alp[s][t] = 0;

      if (1 == nullI[s]) {
        tit = t;
      } else {
        tit = tm1;
      }

      for (myc = 0; myc < bwM[s][0]; myc++) {
        p = bwM[s][myc+1];
        alp[s][t] += alp[p][tit] * trp[p][s];
      }
      alp[s][t] *= emt[s][t];

      if (1 == nullI[s]) {
        for (myc = 0; myc < fwM[s][0]; myc++) {
          n = fwM[s][myc + 1];
          if (1 == nullI[n] && n < s) {
            alp[n][t] += alp[s][t] * trp[s][n];  // n being null, emission is 1;
            if (maxV < alp[n][t]) {
              maxV = alp[n][t];
            }
          }
        }
      }
      if (maxV < alp[s][t]) { maxV = alp[s][t];
      }
    }
    nrmF[t] = maxV;

    for (s = 0; s < ns; s++) {
      alp[s][t] /= nrmF[t];
    }
    // cout<<"Max: "<<t<<" / "<<nt<<" "<<nrmF[t]<<endl;
  }


  // Beta computation...

  t = nt - 1;
  s = ns - 1;
  bet[s][t] = 1;
  bet[ns-2][t] = 1;  // added 10 Mar 2007

  // commented on 10 Mar 2007
  /* for (myc = 0; myc < bwM[s][0]; myc++) {
     p = bwM[s][myc + 1];
     bet[p][t] = 1;
     } */

  for (t = nt - 2; t >=0; t--) {
    tp1 = t + 1;

    for (s = ns - 1; s >= 0; s--) {
      bet[s][t] = 0;
      for (myc = 0; myc < fwM[s][0]; myc++) {
        n = fwM[s][myc+1];
        if (0 == nullI[n]) {
          tit = tp1;
          bet[s][t] += bet[n][tit] * trp[s][n] * emt[n][tit];
        } else if (1 == nullI[n]) {
          tit = t;
          bet[s][t] += bet[n][tit] * trp[s][n] * emt[n][tit];
        } else {
          cout << "The nullI should be 0 / 1 " << endl;
          exit(1);
        }
      }
    }

    // saving the below for-loop computation on Mar 10 2007.
    /* for (s = ns - 1; s >=0; s--) {
       tit = t;
       for (myc = 0; myc < fwM[s][0]; myc++) {
       n = fwM[s][myc+1];
       if (1 == nullI[n]) {
       bet[s][t] += bet[n][tit] * trp[s][n]; // emt[n][tit] = 1;
       }
       }
       /////bet[s][t] /= nrmF[tp1];
       } */

    // cout<<"Beta: "<<t<<endl;
    for (s = ns - 1; s >= 0; s--) {
      bet[s][t] /= nrmF[tp1];
    }
  }


  // cout<<"Checking for NAN "<<endl;
  nanF = 0;
  for (t = 0; t < nt; t++) {
    // for (t = nt-1; t >=0 ; t--) {
    double GSUM = 0;
    for (s = 0; s < ns; s++) {
      // for (s = ns-1; s >= 0; s--) {
      double gam = alp[s][t] * bet[s][t];
      // cout<<" S T "<<s<<" "<<t<<" A B "<<alp[s][t]<<" "<<bet[s][t]<<endl;
      if (IsNAN(gam)) {
        nanF = 1;
        // cout<<"S T"<<s<<" - "<<t<<" "<<gam<<endl;
        break;
      }
      if (1 != nullI[s]) {
        GSUM += gam;
      }
    }
    // cout<<"TIME:  "<<t<<" DSUM: "<<GSUM<<endl;
  }

  lhd = 0;
  for (t = 0; t < nt; t++) {
    lhd += log(nrmF[t]);
  }
  return (lhd);
}

void ReadEnv(char *fnm, wrdC*& hwrd, stC*& hst,
             int& now, int& tst, int& dim, ifstream& fp_md) {
  char tstr[kNmLimit];

  FileExist(fnm);

  ifstream fp_in;
  fp_in.open(fnm, ios::in);

  // Read tstr and no-of-words
  fp_in >> tstr >> now;

  // Read tstr and no-of-states
  fp_in >> tstr >> tst;

  // Read tstr and feature dimension
  fp_in >> tstr >> dim;
  // cout<<tstr<<" "<<dim<<endl;

  hwrd = new wrdC[now];
  hst  = new stC[tst];

  char nm[kNmLimit];
  int  nst;
  int  bst;
  int  est;
  int  wid = 0;
  int  st  = 0;
  int  noc;
  int  nog;

  for (int i = 0; i < now; i++) {
    // Though noc & nog are read here, they are actually read from the
    // fp_md (model file)
    fp_in >> wid >> nm >> nst >> noc >> nog;

    bst = st;
    est = st + (nst - 1);

    hwrd[wid].init(bst, est, nst, wid, nm);

    // Get SIL ID and short pause id from here....
    if (IsSilence(nm)) {
      _pauID  = wid;
    } else if (IsShortPause(nm)) {
      _spauID = wid;
    }
    st = st + nst;
  }
  fp_in.close();

  for (int i = 0; i < tst; i++) {
    hst[i].ReadForTrain(fp_md);
  }
}

void GenerateShortPause(wrdC *hwrd, stC *hst) {
  // Copy code for copying middle state of SIL model to ssil
  int dng, dnc, ddim;
  double *dpr;
  double **dmn;
  double **dvr;
  double *dtrp;

  int paunst = hwrd[_pauID].getnst();
  UNUSED_PARAMETER(paunst);
  int paubst = hwrd[_pauID].getbst();
  int pauest = hwrd[_pauID].getest();

  int paust = (paubst + pauest) / 2;  // Take Middle State...
  int sslst = hwrd[_spauID].getbst() + 1;
  int sslnst = hwrd[_spauID].getnst();

  // cout<<"Number of pause states are: "<<paunst<<endl;
  // cout<<"Pau begin state: "<<paubst<<" end state: "<<pauest<<endl;
  // cout<<"Short silence state: "<<sslst<<endl;
  // cout<<"Source state number from Pause Model: "<<paust<<endl;

  if (sslnst != 3) {
    cout << "I am trying to middle state of pause model short silence model..." << endl;
    cout << "I assumed short pause to be of 3 states rather it is " << sslnst << endl;
    cout << "Aborting..." << endl;
    exit(1);
  }

  hst[paust].LoadParam(dng, ddim, dnc, dtrp, dpr, dmn, dvr);
  hst[sslst].CopyParam(dng, ddim, dnc, dtrp, dpr, dmn, dvr);

  Delete2f(dmn, dng);
  Delete2f(dvr, dng);
  Delete1f(dtrp);
  Delete1f(dpr);
}

int IsSilence(char *nm) {
  int rv = 0;
  if (0 == strcasecmp("sil", nm) ||
      0 == strcasecmp("pau", nm) ) {
    rv = 1;
  }
  return rv;
}

int IsShortPause(char *nm) {
  int rv = 0;
  if (0 == strcasecmp("ssil", nm)) {
    rv = 1;
  }
  return rv;
}

