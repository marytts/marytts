/*************************************************************************/
/*                                                                       */
/*                     Copyright (C) 2002-2005                           */
/*              ISRI, Carnegie Mellon University, USA and                */
/*  International Institute of Information Technology, Hyderabad, India. */
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
/*  IIIT HYDERABAD, CARNEGIE MELLON UNIVERSITY AND THE CONTRIBUTORS TO   */
/*  THIS WORK DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,      */
/*  INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS,     */
/*  IN NO EVENT SHALL IIIT HYDERBAD, CARNEGIE MELLON UNIVERSITY NOR THE  */
/*  CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL    */
/*  DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA   */
/*  OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER    */
/*  TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR     */
/*  PERFORMANCE OF THIS SOFTWARE.                                        */
/*************************************************************************/
/*                                                                       */
/* Author: S.P. Kishore (skishore@cs.cmu.edu)                       */
/* Last Modified: 29 Dec 2005.                                      */
/*                                                                       */
/*************************************************************************/

#include<ctype.h>
#include<math.h>
#include<stdio.h>
#include<stdlib.h>
#include<string.h>

#include<iostream>
#include<fstream>

using std::cin;
using std::cout;
using std::endl;
using std::ifstream;
using std::ofstream;
using std::ios;

#ifndef M_PI
#define M_PI (3.14159265358979323846)
#endif /* M_PI */

#define PI 3.14159265358
#define pi M_PI
#define HAMMING_WIN(i, npts) (0.54-0.46*cos(2*PI*((i)-1)/((npts)-1)))
#define kMaxStringLength 250
#define kMaxNameLength 500
#define MFCC_ONLY 1

// int bytesPerSample = 2;  //Assumed two bytes per sample

char _wavDir[kMaxStringLength];
int _header = 44;
int _samplingFreq = 16000;
int _frameSize = 160;
int _frameShift = 80;
int _lpOrder = 12;
int _noOfCeps = 16;
char _featDir[kMaxStringLength];
char _extn[kMaxStringLength];

// MFCC related structures:

#define MEL_SCALE 1
#define LOG_LINEAR 2

/* Default values */
#define DEFAULT_SAMPLING_RATE 16000.0
#define DEFAULT_FRAME_RATE 100
#define DEFAULT_FRAME_SHIFT 160
#define DEFAULT_WINDOW_LENGTH 0.025625
#define DEFAULT_FFT_SIZE 512
#define DEFAULT_FB_TYPE MEL_SCALE
#define DEFAULT_NUM_CEPSTRA 13
#define DEFAULT_NUM_FILTERS 40
#define DEFAULT_LOWER_FILT_FREQ 133.33334
#define DEFAULT_UPPER_FILT_FREQ 6855.4976
#define DEFAULT_PRE_EMPHASIS_ALPHA 0.97
#define DEFAULT_START_FLAG 0
#define DEFAULT_BLOCKSIZE 200000

#define int32 int
#define int16 short
#define float32 float
#define float64 double

#define FORWARD_FFT 1
#define INVERSE_FFT -1

typedef struct {
  float64 r, i;
} complex;

typedef struct {
  float32 sampling_rate;
  int32 num_cepstra;
  int32 num_filters;
  int32 fft_size;
  float32 lower_filt_freq;
  float32 upper_filt_freq;
  float32 **filter_coeffs;
  float32 **mel_cosine;
  float32 *left_apex;
  int32 *width;
} melfb_t;

typedef struct {
  float32 SAMPLING_RATE;
  int32 FRAME_RATE;
  int32 FRAME_SHIFT;
  float32 WINDOW_LENGTH;
  int32 FRAME_SIZE;
  int32 FFT_SIZE;
  int32 FB_TYPE;
  int32 NUM_CEPSTRA;
  float32 PRE_EMPHASIS_ALPHA;
  int16 *OVERFLOW_SAMPS;
  int32 NUM_OVERFLOW_SAMPS;
  melfb_t *MEL_FB;
  int32 START_FLAG;
  int16 PRIOR;
  float64 *HAMMING_WINDOW;
} fe_t;

void Compute_Residual_v1(float *tempspeechSegment, float *residualSignal,
                         int noOfSamples, int frameSize, int frameShift,
                         int lpOrder, int noOfCeps,
                         char *dimFileName, char *resFileName,
                         char *autoFileName, char *lpcFileName,
                         char *lpccFileName, char *energyFileName,
                         char *vpFileName, fe_t& FE, char *mfccFileName);

void Preemphasis(float *speechSegment, int noOfSamples);
void Window_Segment(float *speechSegment, int noOfSamples);
void AutoCorrelation_Coefficients(float *speechSegment, float *r,
                                  int noOfSamples, int lpOrder);
float  LinearPredictors_Analysis(float *r, float *a, int M);
void AllPoleFilterTheSamples(float *Samples, int No_Of_Samples,
                             float *Filter, int Lporder);
int binarytoascii(char *WRFileName, char *AsciiFileName, int HeaderLength);
int binarytoascii_new(char *inF, char *ouF, int hdrL);
void LPCtoCepstrum(float *r, float *a, float *c, int M, int No_Of_Ceps);
void GetPrefix(char *prefix, char *nameString);
void Initialize_MelFilterBank(melfb_t& melFB, fe_t& FE);
int32 fe_build_melfilters(melfb_t& MEL_FB);
float32 fe_mel(float32 x);
float32 fe_melinv(float32 x);
int32 fe_compute_melcosine(melfb_t& MEL_FB);
void fe_frame_to_fea(fe_t& FE, float64 *in, float64 *fea);
void fe_spec_magnitude(float64 *data, int32 data_len,
                       float64 *spec, int32 fftsize);
int32 fe_fft(complex *in, complex *out, int32 N, int32 invert);
void fe_mel_cep(fe_t *FE, float64 *mfspec, float64 *mfcep);
void fe_mel_spec(fe_t *FE, float64 *spec, float64 *mfspec);
void fe_create_hamming(float64 *in, int32 in_len);
void fe_hamming_window(float64 *in, float64 *window, int32 in_len);

void memAlloc_2f(float**& arr, int rows, int cols);
void memAlloc_2dbl(double**& arr, int rows, int cols);
void memAlloc_2d(int**& arr, int rows, int cols);
void memAlloc_2s(short**& arr, int rows, int cols);
void memAlloc_2c(char**& arr, int rows, int cols);
void memAlloc_1f(float*& arr, int rows);
void memAlloc_1dbl(double*& arr, int rows);
void memAlloc_1d(int*& arr, int rows);
void memAlloc_1s(short*& arr, int rows);

void memDel_2f(float**& arr, int rows);
void memDel_2dbl(double**& arr, int rows);
void memDel_2d(int**& arr, int rows);
void memDel_2s(short**& arr, int rows);
void memDel_2c(char**& arr, int rows);
void memDel_1f(float*& arr);
void memDel_1dbl(double*& arr);
void memDel_1d(int*& arr);
void memDel_1s(short*& arr);
void memDel_1c(char*& arr);


double *_sumX;
double *_sumSq;
double *_mSumX;
double *_mSumSq;
int _tFrames = 0;

int main(int argc, char *argv[]) {
  if (argc < 3) {
    cout << "**************************************************************************************************" << endl;
    cout << "This program is open source software " << endl;
    cout << "Copyright (c) 2002-2003, ISRI, Carnegie Mellon University, USA and IIIT Hyderabad, India " << endl;
    cout << "Author: S.P. Kishore (skishore@cs.cmu.edu)" << endl;

    cout << "***************************************************************************************************" << endl << endl;

    cout << "Given a wave file, this program computes ascii form of signal, LP residual, autocorrelations, LP coefficients, " << endl;
    cout << " LP cepstrals, MF Cepstrals, energy and normalized residual energy ......" << endl;
    cout << "*Note the LP cepstral coefficients are linearly weighted from index 1 " << endl << endl;
    cout << "***You need to pass two files 1. Feature Settings and 2. List of wavefiles.  " << endl << endl;
    cout << "Usage: FeatureExtraction_v3 <mysp_settings> <mywavelist> " << endl << endl;
    cout << "Do you want me to create a dummy files for your reference (y/n): ";
    char choice;
    cin >> choice;

    if (choice == 'y' || choice == 'Y') {
      cout << endl << " *** I am generating these two dummy files 'mysp_settings' and 'mywavelist' please feel free to modify them .." << endl << endl;

      ofstream fp_out1, fp_out2;
      fp_out1.open("mysp_settings", ios::out);
      fp_out1 << "WaveDir: ./wav " << endl;
      fp_out1 << "HeaderBytes: 44" << endl;
      fp_out1 << "SamplingFreq: 16000" << endl; fp_out1 << "FrameSize: 160" << endl; fp_out1 << "FrameShift: 80" << endl;
      fp_out1 << "Lporder: 12" << endl; fp_out1 << "CepsNum: 16" << endl; fp_out1 << "FeatDir: ./feat" << endl;
      fp_out1 << "Ext: .wav\n";
      fp_out1.close();

      fp_out2.open("mywavelist", ios::out);
      fp_out2 << "NOfFiles: 1000" << endl;
      fp_out2 << "files list .. " << endl;
      fp_out2.close();
    }

    exit(1);
  }

  ifstream fp_in;

  fp_in.open(argv[1], ios::in);

  if (fp_in == 0) {
    cout << "Cannot open file " << argv[1] << endl;
    exit(1);
  }

  char tmpString[kMaxStringLength];

  //  cout << "The parameters you are using are the following...." << endl;

  fp_in >> tmpString >> _wavDir;
  //  cout << tmpString << " " << _wavDir << endl;

  fp_in >> tmpString >> _header;
  //  cout << tmpString << " " << _header << endl;

  fp_in >> tmpString >> _samplingFreq;
  //  cout << tmpString << " " << _samplingFreq << endl;

  fp_in >> tmpString >> _frameSize;
  //  cout << tmpString << " " << _frameSize << endl;

  fp_in >> tmpString >> _frameShift;
  //  cout << tmpString << " " << _frameShift << endl;

  fp_in >> tmpString >> _lpOrder;
  //  cout << tmpString << " " << _lpOrder << endl;

  fp_in >> tmpString >> _noOfCeps;
  //  cout << tmpString << " " << _noOfCeps << endl;

  fp_in >> tmpString >> _featDir;
  //  cout << tmpString << " " << _featDir << endl;

  fp_in >> tmpString >> _extn;
  //  cout << tmpString << " " << _extn << endl;

  fp_in.close();

  fp_in.open(argv[2], ios::in);

  int noOfFiles;
  char **fileNames;

  fp_in >> tmpString >> noOfFiles;
  //  cout << tmpString << " " << noOfFiles << endl;

  fileNames = new char*[noOfFiles];

  for (int i = 0; i < noOfFiles; i++) {
    fileNames[i] = new char[kMaxStringLength];
  }

  for (int i = 0; i < noOfFiles; i++) {
    fp_in >> fileNames[i];
    strcat(fileNames[i], _extn);
    // cout << fileNames[i] << endl;
  }

  fp_in.close();

  char prefixStr[kMaxStringLength];
  char dimFile[kMaxNameLength];
  char resFile[kMaxNameLength];
  char lpcFile[kMaxNameLength];
  char autoFile[kMaxNameLength];
  char lpccFile[kMaxNameLength];
  char energyFile[kMaxNameLength];
  char vpFile[kMaxNameLength];
  char mfccFile[kMaxNameLength];

  char wavFile[kMaxNameLength];
  char wav_bin[kMaxNameLength];


  float *samples;
  float *residualSamples;

  // New: Mean Vector and Variaces;
  _sumX = new double[_noOfCeps + 1];
  _sumSq = new double[_noOfCeps + 1];


  for (int i = 0; i < _noOfCeps + 1; i++) {
    _sumX[i]  = 0;
    _sumSq[i] = 0;
    _tFrames = 0;
  }

  melfb_t melFB;
  fe_t    FE;

  Initialize_MelFilterBank(melFB, FE);
  fe_build_melfilters(melFB);
  fe_compute_melcosine(melFB);

  _mSumX = new double[FE.NUM_CEPSTRA];
  _mSumSq = new double[FE.NUM_CEPSTRA];

  for (int i = 0; i < FE.NUM_CEPSTRA; i++) {
    _mSumX[i]  = 0;
    _mSumSq[i] = 0;
  }

  for (int i = 0; i < noOfFiles; i++) {
    snprintf(wav_bin, kMaxNameLength, "%s/%s", _wavDir, fileNames[i]);

    GetPrefix(prefixStr, fileNames[i]);

    snprintf(dimFile, kMaxNameLength, "%s/%s.dim", _featDir, prefixStr);
    snprintf(resFile, kMaxNameLength, "%s/%s.res", _featDir, prefixStr);
    snprintf(lpcFile, kMaxNameLength, "%s/%s.lpc", _featDir, prefixStr);
    snprintf(lpccFile, kMaxNameLength, "%s/%s.lpcc", _featDir, prefixStr);
    snprintf(energyFile, kMaxNameLength, "%s/%s.en", _featDir, prefixStr);
    snprintf(vpFile, kMaxNameLength, "%s/%s.vp", _featDir, prefixStr);
    snprintf(autoFile, kMaxNameLength, "%s/%s.auto", _featDir, prefixStr);
    snprintf(wavFile, kMaxNameLength, "%s/%s.txt", _featDir, prefixStr);
    snprintf(mfccFile, kMaxNameLength, "%s/%s.mfcc", _featDir, prefixStr);

    // /int sampleCount = binarytoascii(wav_bin, wavFile,_header);
    int sampleCount = binarytoascii_new(wav_bin, wavFile, _header);


    samples = new float[sampleCount];
    residualSamples = new float[sampleCount];

    ifstream fp_wave;
    fp_wave.open(wavFile, ios::in);
    for (int k = 0; k < sampleCount; k++) {
      fp_wave >> samples[k];
    }
    fp_wave.close();

    cout << "Feature extract " << fileNames[i] << " ..." << endl;

    Compute_Residual_v1(samples, residualSamples,
                        sampleCount, _frameSize, _frameShift,
                        _lpOrder, _noOfCeps, dimFile, resFile,
                        autoFile, lpcFile, lpccFile, energyFile,
                        vpFile, FE, mfccFile);

    delete [] samples;
    delete [] residualSamples;
  }

  //  cout << "Total Frames are: " << _tFrames << endl;

  for (int i = 0; i < _noOfCeps + 1; i++) {
    _sumX[i]   = _sumX[i]/_tFrames;
    _sumSq[i]  = (_sumSq[i]/_tFrames) - (_sumX[i] * _sumX[i]);
  }

  for (int i = 0; i < FE.NUM_CEPSTRA; i++) {
    _mSumX[i] = _mSumX[i]/_tFrames;
    _mSumSq[i] = (_mSumSq[i]/_tFrames) - (_mSumX[i] * _mSumX[i]);
  }

  ofstream fp_mv;
  fp_mv.open("mvar.txt", ios::out);
  fp_mv << _noOfCeps + 1 << endl;
  for (int i = 0; i < _noOfCeps + 1; i++) {
    fp_mv << (float)_sumX[i] << " ";
  }
  fp_mv << endl;

  for (int i = 0; i < _noOfCeps + 1; i++) {
    fp_mv << (float)_sumSq[i] << " ";
  }
  fp_mv << endl;

  fp_mv << FE.NUM_CEPSTRA << endl;

  for (int i = 0; i < FE.NUM_CEPSTRA; i++) {
    fp_mv << _mSumX[i] << " ";
  }
  fp_mv << endl;

  for (int i = 0; i < FE.NUM_CEPSTRA; i++) {
    fp_mv << _mSumSq[i] << " ";
  }
  fp_mv << endl;

  fp_mv.close();

  memDel_1s(FE.OVERFLOW_SAMPS);
  memDel_1dbl(FE.HAMMING_WINDOW);
  memDel_2f(melFB.filter_coeffs, melFB.num_filters);
  memDel_1f(melFB.left_apex);
  memDel_1d(melFB.width);
  memDel_2f(melFB.mel_cosine, melFB.num_cepstra);

  delete [] _sumX;
  delete [] _sumSq;

  delete [] _mSumX;
  delete [] _mSumSq;
}

void Compute_Residual_v1(float *tempspeechSegment, float *residualSignal,
                         int noOfSamples, int frameSize, int frameShift,
                         int lpOrder, int noOfCeps,
                         char *dimFileName, char *resFileName,
                         char *autoFileName, char *lpcFileName,
                         char *lpccFileName, char *energyFileName,
                         char *vpFileName, fe_t& FE, char *mfccFileName) {
  float *speechSegment;

  speechSegment = tempspeechSegment;

  Preemphasis(speechSegment, noOfSamples);

  if (noOfSamples < frameSize) {
    frameSize = noOfSamples;
  }

  int noOfFrames = (noOfSamples - frameSize)/frameShift + 1;
  float *block;
  float *residualBlock;
  float *s;
  float *autoCC;  // autoCC - Autocorrelation Coefficients
  float *lpc;  // lpc - linear predictor coefficients
  float *lpcc;  // lpcc -linear prediction cepstral coefficients

  double *in;
  double *fea;
  float *mfcc;

  if (!MFCC_ONLY) {
    autoCC = new float[lpOrder+1];
    lpc = new float[lpOrder+1];
    block = new float[frameSize];
    lpcc = new float[noOfCeps+1];
    residualBlock = new float[frameSize+lpOrder-1];
  }
  in = new double[frameSize];
  fea = new double[FE.NUM_CEPSTRA];
  mfcc = new float[FE.NUM_CEPSTRA];

  ofstream fp_dim, fp_lpcc, fp_mfcc, fp_vp;
  if (!MFCC_ONLY) {
    fp_dim.open(dimFileName, ios::out);
    fp_lpcc.open(lpccFileName, ios::out);
    fp_vp.open(vpFileName, ios::out);
    fp_dim << "Samples: " << noOfSamples << endl;
    fp_dim << "SampFreq: " << _samplingFreq << endl;
    fp_dim << "FrameSize: " << frameSize << endl;
    fp_dim << "Frames: " << noOfFrames << endl;
    fp_dim << "AutoDim: " << lpOrder+1 << endl;
    fp_dim << "LpcDim:  " << lpOrder+1 << endl;
    fp_dim << "LpccDim: " << noOfCeps+1 << endl;
    fp_dim << "Mfccdim: " << FE.NUM_CEPSTRA << endl;
    fp_lpcc << noOfFrames << " " << noOfCeps+1 << endl;
    fp_vp << noOfFrames << " 1" << endl;
  }
  fp_mfcc.open(mfccFileName, ios::out);

  /* No print....
     ofstream fp_res, fp_auto, fp_lpc, fp_en;
     fp_res.open(resFileName, ios::out);
     fp_auto.open(autoFileName, ios::out);
     fp_lpc.open(lpcFileName, ios::out);
     fp_en.open(energyFileName, ios::out); */

  // Writing the dimension of LPCC and MFCC into the files itself
  // Modification on 29 Dec 2004

  /* No Print...
     fp_lpc << noOfFrames << " " << lpOrder+1 << endl;
  */
  fp_mfcc << noOfFrames << " " << FE.NUM_CEPSTRA << endl;


  float normalizedError;

  int residualCount = 0;

  // int bound = frameSize/4;
  // int bound = frameSize - frameShift;

  // This is a good thing for synthesis while the above are pitch tracking
  int bound = 0;

  /* Initially fill the Residual signal with zeros for "Bound" number
     of samples, as for these samples residual cannot be computed
     using previous samples*/

  // for (residualCount=0;residualCount<bound;residualCount++)
  // {
  //        residualSignal[residualCount]=0.01;
  // }

  s = speechSegment;

  for (int frameNo = 0; frameNo < noOfFrames; frameNo++) {
    if (!MFCC_ONLY) {
      for (int i = 0; i < frameSize; i++) {
        block[i] = s[i];
        if (block[i] == 0) {
          block[i] = 0.001;
        }
      }

      Window_Segment(block, frameSize);
      AutoCorrelation_Coefficients(block, autoCC, frameSize, _lpOrder);
      normalizedError = LinearPredictors_Analysis(autoCC, lpc, _lpOrder);
      LPCtoCepstrum(autoCC, lpc, lpcc, _lpOrder, noOfCeps);

      /* No print: .....
         fp_en << autoCC[0] << endl;
         for (int i = 0; i <= _lpOrder; i++) {
         fp_auto << autoCC[i] << " ";
         fp_lpc << lpc[i] << " ";
         }
         fp_auto << endl;
         fp_lpc << endl;  */

      fp_vp << normalizedError << endl;
      fp_lpcc << lpcc[0] << " ";

      for (int i = 1; i <= noOfCeps; i++) {
        fp_lpcc << lpcc[i]*i << " ";
      }
      fp_lpcc << endl;

      // Modifying the LPCC values - weighted....
      for (int i = 1; i <= noOfCeps; i++) {
        lpcc[i] = lpcc[i]*i;
      }

      for (int i = 0; i <= noOfCeps; i++) {
        _sumX[i] += lpcc[i];
        _sumSq[i] += lpcc[i] * lpcc[i];
      }
    }
    _tFrames++;

    /***********Compute Residual *******/

    for (int i = 0; i < frameSize; i++) {
      if (!MFCC_ONLY) {
        block[i]=s[i];

        if (block[i] == 0) {
          block[i] = 0.01;
        }
      }

      in[i] = (double)s[i];

      if (in[i] == 0) {
        in[i] = 0.01;
      }
    }

    /* for (int i = 0; i < frameSize; i++) {
       printf("%-8.6f ", in[i]);
       }
       printf("\n --- \n"); */

    fe_hamming_window(in, FE.HAMMING_WINDOW, frameSize);

    /* for (int i = 0; i < frameSize; i++) {
       printf("%-8.6f : %-8.6f ", in[i], FE.HAMMING_WINDOW[i]);
       }
       printf("\n"); */


    fe_frame_to_fea(FE, in, fea);

    for (int i = 0; i < FE.NUM_CEPSTRA; i++) {
      mfcc[i] = (float) fea[i];
      fp_mfcc << mfcc[i] << " ";
      _mSumX[i] += mfcc[i];
      _mSumSq[i] += mfcc[i] * mfcc[i];
    }
    fp_mfcc << endl;

    if (!MFCC_ONLY) {
      for (int i = 0; i < frameSize; i++) {
        residualBlock[i] = 0.0;
        for (int k = 0; k <= _lpOrder; k++) {
          if (i-k >= 0) {
            residualBlock[i] = residualBlock[i]
                + lpc[k]*block[i-k];
          }
        }
      }

      // for (int i=bound;i<frameSize;i++)
      for (int i = bound; i< frameShift; i++) {
        residualSignal[residualCount] = residualBlock[i];
        residualCount++;
      }
    }

    s = s + frameShift;
  }

  /* Filling the Remaining Samples with zeros*/

  if (!MFCC_ONLY) {
    for (int i = residualCount; i < noOfSamples; i++) {
      residualSignal[i]=0;
    }
  }

  /* No Print....
     for (int i = 0; i < noOfSamples; i++) {
     fp_res << residualSignal[i] << endl;
     } */

  if (!MFCC_ONLY) {
    delete [] block;
    delete [] residualBlock;
    delete [] lpc;
    delete [] autoCC;
    // delete [] speechSegment;
    delete [] lpcc;
  }
  delete [] in;
  delete [] fea;
  delete [] mfcc;

  if (!MFCC_ONLY) {
    fp_dim.close();
    fp_lpcc.close();
    fp_vp.close();
  }
  fp_mfcc.close();

  /* No Print...
     fp_auto.close();
     fp_lpc.close();
     fp_res.close();
     fp_en.close(); */
}

void Preemphasis(float *speechSegment, int noOfSamples) {
  /* for (int i=0;i<noOfSamples-1;i++)
     {
     speechSegment[i] = speechSegment[i+1] - speechSegment[i];
     }
     speechSegment[noOfSamples -1 ] = speechSegment[noOfSamples -2 ];  */

  float *ns;
  ns = new float[noOfSamples];

  ns[0] = speechSegment[0];

  for (int i = 1; i < noOfSamples; i++) {
    ns[i] = speechSegment[i] - speechSegment[i-1];
  }
  for (int i = 0; i < noOfSamples; i++) {
    speechSegment[i] = ns[i];
  }

  delete [] ns;

  /* for (int i=noOfSamples-1;i>0;i--)
     {
     speechSegment[i] = speechSegment[i-1];
     }*/
}

void Window_Segment(float *speechSegment, int noOfSamples) {
  for (int i = 0; i < noOfSamples; i++) {
    speechSegment[i] = speechSegment[i] *(HAMMING_WIN(i+1, noOfSamples));
  }
}

void AutoCorrelation_Coefficients(float *speechSegment, float *r,
                                  int noOfSamples, int lpOrder) {
  for (int lag = 0; lag <= lpOrder; lag ++) {
    r[lag] = 0.0;

    for (int n = 0; n < noOfSamples - lag; n++) {
      r[lag] = r[lag] + speechSegment[n] * speechSegment[n+lag];
    }
  }
}

// The following is the routine for LP analysis:

float  LinearPredictors_Analysis(float *r, float *a, int M) {
  // Input:
  //    r   - autocorrelations array with 0 - M + 1
  //    M - LP order
  //  Output:
  //     a  - array of linear predictors: 0 - M + 1;

  float *k;  // array of reflection coefficients
  float *b;  // Temporary array to hold predictors of previous iterations
  float E;   // Energy
  float Sum = 0;

  k = new float[M+1];
  b = new float[M+1];
  E = r[0];

  // cout << "Energy = " << E << endl;

  for (int l = 1; l <= M; l++) {
    Sum = 0;
    for (int i = 1; i <= l-1; i++) {
      Sum = Sum + a[i] * r[l-i];
    }

    k[l] =    - (r[l] + Sum) / E;
    a[l] = k[l];

    for (int i = 1; i <= l-1; i++) {
      b[i] = a[i];
    }

    for (int i = 1; i <= l-1; i++) {
      a[i] = a[i] + k[l] * b[l-i];
    }

    E = E * (1.0 - (k[l] * k[l]));
  }

  E = E / r[0];

  a[0] = 1.0;
  delete [] k;
  delete [] b;
  return (E);
}


void AllPoleFilterTheSamples(float *Samples, int No_Of_Samples,
                             float *Filter, int Lporder) {

  /* The Filter is accessed from 0 to Lporder-1 */

  float *FilteredSamples, *a;
  float SUM = 0;

  // a=Filter;

  FilteredSamples = new float[No_Of_Samples];

  FilteredSamples[0]=0;
  float *myFilter;

  myFilter = new float[Lporder-1];

  for (int j = 0; j < Lporder-1; j++) {
    myFilter[j] = Filter[j+1];
  }

  a = myFilter;

  Lporder = Lporder -1;
  for (int n = 0; n < No_Of_Samples; n++) {
    SUM = 0;

    for (int k = 1; k <= Lporder; k++) {
      if ((n-k) >= 0) {
        SUM = SUM + a[k-1]*FilteredSamples[n-k];
      }
    }

    FilteredSamples[n]=Samples[n]-SUM;

    // cout << "ALLP " << FilteredSamples[n] << endl;
  }

  for (int n = 0; n < No_Of_Samples; n++) {
    Samples[n]=FilteredSamples[n];
  }

  delete [] FilteredSamples;

  delete [] myFilter;
}

int binarytoascii_new(char *inF, char *ouF, int hdrL) {
  ifstream fp_in;
  ofstream fp_out;
  short int amp;  // amp for amplitude
  // Short int declaration assumes that
  // each sample is 16 bits ie 2 bytes
  int sampleSize = 2;
  //First hdrL bytes of MS RIFF wave file is header
  //Rest of the file is data
  int nS = 0;

  fp_in.open(inF, ios::in | ios::binary);
  if (fp_in == 0) {
    cout << "Cannot open " << inF << " for binarytoascii_new()\n";
  }

  fp_out.open(ouF, ios::out);

  // Skip header number of bytes
  fp_in.seekg(hdrL, ios::beg);
  fp_in.read((char *)&amp, sampleSize);

  while (!fp_in.eof()) {
    nS++;
    fp_out << amp << endl;
    fp_in.read((char *)&amp, sampleSize);
  }
  fp_in.close();
  fp_out.close();
  return nS;
}

int binarytoascii(char *WRFileName, char *AsciiFileName, int HeaderLength) {
  char lb, hb;
  int count;
  short int tempvalue;
  FILE *fip, *fop;

  fip = fopen(WRFileName, "r");
  if (fip == NULL) {
    printf("File %s Cannot be opened\n", WRFileName);
    exit(1);
  }

  fop = fopen(AsciiFileName, "w");
  for (count = 0; count < HeaderLength; count = count+1) {
    lb = fgetc(fip);
  }

  count = 0;
  lb = fgetc(fip);
  hb = fgetc(fip);
  tempvalue = lb;
  tempvalue = tempvalue&0x00ff;
  tempvalue = hb*256|tempvalue;

  while (!feof(fip)) {
    count = count+1;
    fprintf(fop, "%d\n", tempvalue);
    lb = fgetc(fip);
    hb = fgetc(fip);
    tempvalue = lb;
    tempvalue = tempvalue&0x00ff;
    tempvalue = hb*256|tempvalue;
  }

  fclose(fip);
  fclose(fop);
  return count;
}

void LPCtoCepstrum(float *r, float *a, float *c, int M, int No_Of_Ceps) {
  float Gain;
  Gain = r[0];

  for (int i = 1; i <= M; i = i+1) {
    Gain = Gain + (a[i]*r[i]);
  }

  // cout << "Gain is " << Gain << endl;

  c[0] = (float)log(double(Gain));
  for (int m = 1; m <= No_Of_Ceps; m++) {
    if (m <= M) {
      c[m] = a[m];
      for (int k = 1; k < m; k++) {
        c[m] = c[m] - ((float)k/(float)m) * c[k] * a[m-k];
      }
    } else {
      c[m] = 0.0;
      for (int k =(m-M); k < m; k++) {
        c[m] = c[m] - (float)k/(float)m * c[k] * a[m-k];
      }
    }
  }
}

void GetPrefix(char *prefix, char *nameString) {
  int len = strlen(nameString);
  int i;

  strcpy(prefix, "");

  for (i = 0; i < len; i++) {
    if (nameString[i] == '.') {
      break;
    }
    prefix[i] = nameString[i];
  }

  prefix[i] = '\0';
}


void Initialize_MelFilterBank(melfb_t& melFB, fe_t& FE) {
  FE.WINDOW_LENGTH = DEFAULT_WINDOW_LENGTH;
  FE.PRE_EMPHASIS_ALPHA = DEFAULT_PRE_EMPHASIS_ALPHA;
  FE.NUM_CEPSTRA = DEFAULT_NUM_CEPSTRA;
  FE.FFT_SIZE    = DEFAULT_FFT_SIZE;

  FE.FRAME_SHIFT = _frameShift;
  FE.FRAME_SIZE  = _frameSize;
  FE.SAMPLING_RATE = _samplingFreq;
  FE.PRIOR = 0;

  memAlloc_1s(FE.OVERFLOW_SAMPS, FE.FRAME_SIZE);
  memAlloc_1dbl(FE.HAMMING_WINDOW, FE.FRAME_SIZE);

  fe_create_hamming(FE.HAMMING_WINDOW, _frameSize);

  /* for (int i = 0; i < _frameSize; i++) {
     FE.HAMMING_WINDOW[i] = HAMMING_WIN(i+1, _frameSize);
     } */

  FE.FB_TYPE = MEL_SCALE;

  melFB.sampling_rate = _samplingFreq;
  melFB.fft_size      = DEFAULT_FFT_SIZE;
  melFB.num_cepstra   = DEFAULT_NUM_CEPSTRA;
  melFB.num_filters   = DEFAULT_NUM_FILTERS;
  melFB.upper_filt_freq = DEFAULT_UPPER_FILT_FREQ;
  melFB.lower_filt_freq = DEFAULT_LOWER_FILT_FREQ;

  // Linking FE AND MEL_FB

  FE.MEL_FB = &melFB;
}


int32 fe_build_melfilters(melfb_t& MEL_FB) {
  int32 i, whichfilt, start_pt;
  float32 leftfr, centerfr, rightfr, fwidth, height, *filt_edge;
  float32 melmax, melmin, dmelbw, freq, dfreq, leftslope, rightslope;

  /*estimate filter coefficients*/

  /* MEL_FB->filter_coeffs = (float32 **)fe_create_2d(MEL_FB->num_filters, MEL_FB->fft_size, sizeof(float32));
     MEL_FB->left_apex = (float32 *) calloc(MEL_FB->num_filters, sizeof(float32));
     MEL_FB->width = (int32 *) calloc(MEL_FB->num_filters, sizeof(int32)); */

  memAlloc_2f(MEL_FB.filter_coeffs, MEL_FB.num_filters, MEL_FB.fft_size);
  memAlloc_1f(MEL_FB.left_apex, MEL_FB.num_filters);
  memAlloc_1d(MEL_FB.width, MEL_FB.num_filters);

  // filt_edge = (float32 *) calloc(MEL_FB->num_filters+2, sizeof(float32));

  memAlloc_1f(filt_edge, MEL_FB.num_filters+2);

  if (MEL_FB.filter_coeffs == NULL ||
      MEL_FB.left_apex == NULL ||
      MEL_FB.width == NULL ||
      filt_edge == NULL) {
    fprintf(stderr, "memory alloc failed in fe_build_mel_filters()\n...exiting\n");
    exit(0);
  }

  dfreq = MEL_FB.sampling_rate/(float32)MEL_FB.fft_size;

  melmax = fe_mel(MEL_FB.upper_filt_freq);
  melmin = fe_mel(MEL_FB.lower_filt_freq);
  dmelbw = (melmax-melmin)/(MEL_FB.num_filters+1);

  for (i = 0; i <= MEL_FB.num_filters+1; ++i) {
    filt_edge[i] = fe_melinv(i*dmelbw + melmin);
  }

  for (whichfilt = 0; whichfilt < MEL_FB.num_filters; ++whichfilt) {
    /*line triangle edges up with nearest dft points... */
    /*
      leftfr = (float32)rint((float64)(filt_edge[whichfilt]/dfreq))*dfreq;
      centerfr = (float32)rint((float64)(filt_edge[whichfilt+1]/dfreq))*dfreq;
      rightfr = (float32)rint((float64)(filt_edge[whichfilt+2]/dfreq))*dfreq;
    */

    leftfr   = (float32)((int32)((filt_edge[whichfilt]/dfreq)+0.5))*dfreq;
    centerfr = (float32)((int32)((filt_edge[whichfilt+1]/dfreq)+0.5))*dfreq;
    rightfr  = (float32)((int32)((filt_edge[whichfilt+2]/dfreq)+0.5))*dfreq;

    MEL_FB.left_apex[whichfilt] = leftfr;

    fwidth = rightfr - leftfr;

    /* 2/fwidth for triangles of area 1 */
    height = 2/(float32)fwidth;
    leftslope = height/(centerfr-leftfr);
    rightslope = height/(centerfr-rightfr);

    start_pt = 1 + (int32)(leftfr/dfreq);
    freq = (float32)start_pt*dfreq;
    i=0;

    while (freq <= centerfr) {
      MEL_FB.filter_coeffs[whichfilt][i] = (freq-leftfr)*leftslope;
      freq += dfreq;
      i++;
    }
    while (freq < rightfr){
      MEL_FB.filter_coeffs[whichfilt][i] = (freq-rightfr)*rightslope;
      freq += dfreq;
      i++;
    }

    MEL_FB.width[whichfilt] = i;
  }

  // free(filt_edge);
  memDel_1f(filt_edge);

  return(0);
}

float32 fe_mel(float32 x) {
  return(2595.0*(float32)log10(1.0+x/700.0));
}

float32 fe_melinv(float32 x) {
  return(700.0*((float32)pow(10.0, x/2595.0) - 1.0));
}

int32 fe_compute_melcosine(melfb_t& MEL_FB) {
  float32 period, freq;
  int32 i, j;

  period = (float32)2*MEL_FB.num_filters;

  /* if ((MEL_FB->mel_cosine = (float32 **) fe_create_2d(MEL_FB->num_cepstra,MEL_FB->num_filters,
     sizeof(float32)))==NULL){
     fprintf(stderr,"memory alloc failed in fe_compute_melcosine()\n...exiting\n");
     exit(0);
     } */

  memAlloc_2f(MEL_FB.mel_cosine, MEL_FB.num_cepstra, MEL_FB.num_filters);


  for (i = 0; i < MEL_FB.num_cepstra; i++) {
    freq = 2*(float32)M_PI*(float32)i/period;
    for (j = 0; j< MEL_FB.num_filters; j++)
      MEL_FB.mel_cosine[i][j] = (float32)cos((float64)(freq*(j+0.5)));
  }

  return(0);
}

void fe_frame_to_fea(fe_t& FE, float64 *in, float64 *fea) {
  float64 *spec, *mfspec;

  memAlloc_1dbl(spec, FE.FFT_SIZE);
  memAlloc_1dbl(mfspec, FE.MEL_FB->num_filters);

  fe_spec_magnitude(in, FE.FRAME_SIZE, spec, FE.FFT_SIZE);
  fe_mel_spec(&FE, spec, mfspec);
  fe_mel_cep(&FE, mfspec, fea);

  memDel_1dbl(spec);
  memDel_1dbl(mfspec);
}

void fe_mel_spec(fe_t *FE, float64 *spec, float64 *mfspec) {
  int32 whichfilt, start, i;
  float32 dfreq;

  dfreq = FE->SAMPLING_RATE/(float32)FE->FFT_SIZE;

  for (whichfilt = 0; whichfilt < FE->MEL_FB->num_filters; whichfilt++) {
    start = (int32)(FE->MEL_FB->left_apex[whichfilt]/dfreq) + 1;
    mfspec[whichfilt] = 0;
    for (i = 0; i < FE->MEL_FB->width[whichfilt]; i++)
      mfspec[whichfilt] +=
          FE->MEL_FB->filter_coeffs[whichfilt][i]*spec[start+i];
  }
}

void fe_mel_cep(fe_t *FE, float64 *mfspec, float64 *mfcep) {
  int32 i, j;
  int32 period;
  float32 beta;

  period = FE->MEL_FB->num_filters;

  for (i = 0; i < FE->MEL_FB->num_filters; ++i) {
    if (mfspec[i]>0)
      mfspec[i] = log(mfspec[i]);
    else
      mfspec[i] = -1.0e+5;
  }

  for (i = 0; i < FE->NUM_CEPSTRA; ++i) {
    mfcep[i] = 0;
    for (j = 0; j < FE->MEL_FB->num_filters; j++) {
      if (j == 0)
        beta = 0.5;
      else
        beta = 1.0;
      mfcep[i] += beta*mfspec[j]*FE->MEL_FB->mel_cosine[i][j];
    }
    mfcep[i] /= (float32)period;
  }
  return;
}



void fe_spec_magnitude(float64 *data, int32 data_len,
                       float64 *spec, int32 fftsize) {
  int32  j, wrap;
  complex  *FFT, *IN;

  /*fftsize defined at top of file*/

  // FFT = (complex *) calloc(fftsize, sizeof(complex));
  // IN = (complex *) calloc(fftsize, sizeof(complex));

  FFT = new complex[fftsize];
  IN  = new complex[fftsize];

  if (FFT == NULL || IN == NULL) {
    fprintf(stderr, "memory alloc failed in fe_spec_magnitude()\n...exiting\n");
    exit(0);
  }

  if (data_len > fftsize) { /*aliasing */
    for (j = 0; j < fftsize; j++) {
      IN[j].r = data[j];
      IN[j].i = 0.0;
    }
    for (wrap = 0; j < data_len; wrap++, j++) {
      IN[wrap].r += data[j];
      IN[wrap].i += 0.0;
    }
  } else {
    for (j = 0; j < data_len; j++) {
      IN[j].r = data[j];
      IN[j].i = 0.0;
    }
    for ( ; j < fftsize; j++) {  /*pad zeros if necessary */
      IN[j].r = 0.0;
      IN[j].i = 0.0;
    }
  }

  fe_fft(IN, FFT, fftsize, FORWARD_FFT);

  for (j = 0; j <= fftsize/2; j++) {
    spec[j] = FFT[j].r*FFT[j].r + FFT[j].i*FFT[j].i;
  }

  // free(FFT);
  // free(IN);

  delete [] FFT;
  delete [] IN;

  return;
}

int32 fe_fft(complex *in, complex *out, int32 N, int32 invert) {
  static int32
      s, k,   /* as above    */
      lgN;   /* log2(N)    */
  complex
      *f1, *f2,   /* pointers into from array  */
      *t1, *t2,   /* pointers into to array  */
      *ww;   /* pointer into w array   */
  static complex
      *w, *from, *to,  /* as above    */
      wwf2,   /* temporary for ww*f2   */
      *buffer,   /* from and to flipflop btw out and buffer */
      *exch,   /* temporary for exchanging from and to */
      *wEnd;   /* to keep ww from going off end */
  static float64
      div,   /* amount to divide result by: N or 1 */
      x;    /* misc.    */


  /* check N, compute lgN      */
  for (k = N, lgN = 0; k > 1; k /= 2, lgN++) {
    if (k%2 != 0 || N < 0) {
      fprintf(stderr, "fft: N must be a power of 2 (is %d)\n", N);
      return(-1);
    }
  }

  /* check invert, compute div      */
  if (invert == 1) {
    div = 1.0;
  } else if (invert == -1) {
    div = N;
  } else {
    fprintf(stderr, "fft: invert must be either +1 or -1 (is %d)\n", invert);
    return(-1);
  }

  /* get the to, from buffers right, and init    */

  // buffer = (complex *)calloc(N, sizeof(complex));
  buffer = new complex[N];

  if (lgN%2 == 0) {
    from = out;
    to = buffer;
  } else {
    to = out;
    from = buffer;
  }


  for (s = 0; s < N; s++) {
    from[s].r = in[s].r/div;
    from[s].i = in[s].i/div;
  }

  /* w = exp(-2*PI*i/N), w[k] = w^k     */

  // w = (complex *) calloc(N/2, sizeof(complex));
  w = new complex[N/2];

  for (k = 0; k < N/2; k++) {
    x = -6.28318530717958647*invert*k/N;
    w[k].r = cos(x);
    w[k].i = sin(x);
  }
  wEnd = &w[N/2];

  /* go for it!        */
  for (k = N/2; k > 0; k /= 2) {
    for (s = 0; s < k; s++) {
      /* initialize pointers      */
      f1 = &from[s];
      f2 = &from[s+k];
      t1 = &to[s];
      t2 = &to[s+N/2];
      ww = &w[0];
      /* compute <s, k>       */
      while (ww < wEnd) {
        /* wwf2 = ww*f2       */
        wwf2.r = f2->r*ww->r - f2->i*ww->i;
        wwf2.i = f2->r*ww->i + f2->i*ww->r;
        /* t1 = f1+wwf2       */
        t1->r = f1->r + wwf2.r;
        t1->i = f1->i + wwf2.i;
        /* t2 = f1-wwf2       */
        t2->r = f1->r - wwf2.r;
        t2->i = f1->i - wwf2.i;
        /* increment       */
        f1 += 2*k;
        f2 += 2*k;
        t1 += k;
        t2 += k;
        ww += k;
      }
    }
    exch = from;
    from = to;
    to = exch;
  }

  // free(buffer);
  // free(w);

  delete [] buffer;
  delete [] w;

  return(0);
}

void fe_create_hamming(float64 *in, int32 in_len) {
  int i;
  if (in_len > 1) {
    for (i = 0; i < in_len; i++)
      in[i] = 0.54 - 0.46*cos(2*M_PI*i/((float64)in_len-1.0));
  }
  return;
}

void fe_hamming_window(float64 *in, float64 *window, int32 in_len) {
  int i;
  if (in_len > 1) {
    for (i = 0; i < in_len; i++)
      in[i] *= window[i];
  }
  return;
}


void memAlloc_2f(float**& arr, int rows, int cols) {
  arr = new float*[rows];
  for (int i = 0; i < rows; i++) {
    arr[i] = new float[cols];
  }
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }

  for (int i = 0; i < rows; i++) {
    for (int j = 0; j < cols; j++) {
      arr[i][j] = 0;
    }
  }
}

void memAlloc_2dbl(double**& arr, int rows, int cols) {
  arr = new double*[rows];
  for (int i = 0; i < rows; i++) {
    arr[i] = new double[cols];
  }
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }

  for (int i = 0; i < rows; i++) {
    for (int j = 0; j < cols; j++) {
      arr[i][j] = 0;
    }
  }
}


void memAlloc_2d(int**& arr, int rows, int cols) {
  arr = new int*[rows];
  for (int i = 0; i < rows; i++) {
    arr[i] = new int[cols];
  }
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }

  for (int i = 0; i < rows; i++) {
    for (int j = 0; j < cols; j++) {
      arr[i][j] = 0;
    }
  }
}

void memAlloc_2s(short**& arr, int rows, int cols) {
  arr = new short*[rows];
  for (int i = 0; i < rows; i++) {
    arr[i] = new short[cols];
  }
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }

  for (int i = 0; i < rows; i++) {
    for (int j = 0; j < cols; j++) {
      arr[i][j] = 0;
    }
  }
}

void memAlloc_1f(float*& arr, int rows) {
  arr = new float[rows];
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }
  for (int i = 0; i < rows; i++) {
    arr[i] = 0;
  }
}

void memAlloc_1dbl(double*& arr, int rows) {
  arr = new double[rows];
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }
  for (int i = 0; i < rows; i++) {
    arr[i] = 0;
  }
}

void memDel_1f(float*& arr) {
  delete [] arr;
  arr = 0;
}

void memDel_1dbl(double*& arr) {
  delete [] arr;
  arr = 0;
}

void memAlloc_1d(int*& arr, int rows) {
  arr = new int[rows];
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }
  for (int i = 0; i < rows; i++) {
    arr[i] = 0;
  }
}

void memAlloc_1s(short*& arr, int rows) {
  arr = new short[rows];
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }
  for (int i = 0; i < rows; i++) {
    arr[i] = 0;
  }
}

void memDel_1d(int*& arr) {
  delete [] arr;
  arr = 0;
}

void memDel_1s(short*& arr) {
  delete [] arr;
  arr = 0;
}

void memDel_1c(char*& arr) {
  delete [] arr;
  arr = 0;
}


void memDel_2f(float**& arr, int rows) {
  for (int i = 0; i < rows; i++) {
    delete [] arr[i];
  }
  delete [] arr;
  arr = 0;
}

void memDel_2dbl(double**& arr, int rows) {
  for (int i = 0; i < rows; i++) {
    delete [] arr[i];
  }
  delete [] arr;
  arr = 0;
}


void memDel_2d(int**& arr, int rows) {
  for (int i = 0; i < rows; i++) {
    delete [] arr[i];
  }
  delete [] arr;
  arr = 0;
}

void memDel_2s(short**& arr, int rows) {
  for (int i = 0; i < rows; i++) {
    delete [] arr[i];
  }
  delete [] arr;
  arr = 0;
}

void memAlloc_2c(char**& arr, int rows, int cols) {
  arr = new char*[rows];
  for (int i = 0; i < rows; i++) {
    arr[i] = new char[cols];
  }
  if (arr == 0) {
    cout << "Cannot Allocte Memory ....." << endl;
    exit(1);
  }
}

void memDel_2c(char**& arr, int rows) {
  for (int i = 0; i < rows; i++) {
    delete [] arr[i];
  }
  delete [] arr;
}
