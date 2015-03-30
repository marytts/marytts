/*************************************************************************/
/*                                                                       */
/*                  Language Technologies Institute                      */
/*                     Carnegie Mellon University                        */
/*                         Copyright (c) 2010                            */
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
/*             Author:  Alok Parlikar (aup@cs.cmu.edu)                   */
/*               Date:  November 2011                                    */
/*************************************************************************/
/*                                                                       */
/*  CC/Threaded implementation of scripts/scale_feat.pl                  */
/*  Normalize and Scale the Feature Vectors                              */
/*                                                                       */
/*                                                                       */
/*************************************************************************/

#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<math.h>

#ifndef FESTVOX_NO_THREADS
#include "./threading.h"
#endif

const char* kUnscaledFeatureFileExtension = "ft";
const char* kScaledFeatureFileExtension   = "scaledft";

#ifndef FESTVOX_NO_THREADS
Mutex *global_stdout_mutex;
Mutex *global_utt_consumer_mutex;
int global_next_utt_id_index_to_process;
int global_total_number_of_sentences;
#endif

struct FeatureScalingParameters {
  char** utt_id_list;
  float scaling_factor;
  double* feature_means;
  double* feature_variances;
};

void CalculateFeatureMeansAndVariances(const char** utt_id_list, int num_utts,
                                    double** feature_means,
                                    double** feature_variances);
void GenerateScaledFeatureFile(const char* utt_id, float scaling_factor,
                               const double* feature_means,
                               const double* feature_variances);
#ifndef FESTVOX_NO_THREADS
void* ThreadStart(void* userdata);
#endif

int main(int argc, char** argv) {
  char *wavelist_filename;
  float scaling_factor;

  char **utt_id_list;
  int num_utts;
  int num_threads = 1;
  char tmp[255];

  double *feature_means = NULL;
  double *feature_variances = NULL;

  // Commandline Help
#ifndef FESTVOX_NO_THREADS
  if (argc < 3) {
    printf("Usage: %s ehmm/etc/mywavelist scaling-factor num_threads\n",
           argv[0]);
    return -1;
  }
#else
  if (argc < 3) {
    printf("Usage: %s ehmm/etc/mywavelist scaling-factor\n", argv[0]);
    return -1;
  }
#endif

  // Mutexes if required
#ifndef FESTVOX_NO_THREADS
  global_stdout_mutex = new PthreadMutex();
  global_utt_consumer_mutex = new PthreadMutex();
#endif

  // Parse Commandline arguments
  wavelist_filename = argv[1];
  scaling_factor = atof(argv[2]);
  num_threads = 1;

#ifndef FESTVOX_NO_THREADS
  if (argc > 3)
    num_threads = atoi(argv[3]);
#endif

  // Start Processing
  FILE *wavelist_file = fopen(wavelist_filename, "r");
  if (wavelist_file == NULL) {
    perror("Could not open wavelist file");
    exit(-1);
  }

  fscanf(wavelist_file, "NoOfFiles: %d", &num_utts);

  // Build Utterance ID list by reading the items in wavelist_file
  utt_id_list = new char*[num_utts];
  for (int i = 0; i < num_utts; i++) {
    fscanf(wavelist_file, "%s", tmp);
    utt_id_list[i] = new char[strlen(tmp)+1];
    snprintf(utt_id_list[i], strlen(tmp)+1, "%s", tmp);
  }
  fclose(wavelist_file);

  CalculateFeatureMeansAndVariances((const char**)utt_id_list, num_utts,
                                 &feature_means, &feature_variances);

#ifndef FESTVOX_NO_THREADS
  FeatureScalingParameters params;
  params.utt_id_list = utt_id_list;
  params.scaling_factor = scaling_factor;
  params.feature_means = feature_means;
  params.feature_variances = feature_variances;

  global_next_utt_id_index_to_process = 0;
  global_total_number_of_sentences = num_utts;
  Thread** threadList = new Thread*[num_threads];
  for (int i = 0; i < num_threads; i++) {
    threadList[i] = StartJoinableThread(ThreadStart, &params);
  }
  for (int i = 0; i < num_threads; i++) {
    threadList[i]->Join();
    delete threadList[i];
  }
  delete[] threadList;
#else
  for (int i = 0; i < num_utts; i++) {
    GenerateScaledFeatureFile(utt_id_list[i], scaling_factor,
                              feature_means, feature_variances);
  }
#endif

  // Cleanup
  for (int i = 0; i < num_utts; i++) {
    delete [] utt_id_list[i];
  }

  delete[] utt_id_list;
  if (feature_means != NULL) {
    delete[] feature_means;
  }
  if (feature_variances != NULL) {
    delete[] feature_variances;
  }

#ifndef FESTVOX_NO_THREADS
  delete global_utt_consumer_mutex;
  delete global_stdout_mutex;
#endif
}

void CalculateFeatureMeansAndVariances(const char** utt_id_list, int num_utts,
                                    double** feature_means_ptr,
                                    double** feature_variances_ptr) {
  char utt_feat_filename[256];
  FILE *utt_feat_file;
  int utt_num_rows = 0;
  int utt_num_cols = 0;
  int total_num_rows = 0;  // To calculate mean and stdev

  double* feature_means = NULL;
  double* feature_variances = NULL;
  double* datarow = NULL;

  for (int i = 0; i < num_utts; i++) {
    // Read the feature file
    snprintf(utt_feat_filename, sizeof(utt_feat_filename),
             "ehmm/binfeat/%s.%s",
             utt_id_list[i], kUnscaledFeatureFileExtension);
    printf("Scaling Features. Pass 1 of 2. Processing utt %s\n",
           utt_id_list[i]);
    utt_feat_file = fopen(utt_feat_filename, "rb");
    if (utt_feat_file == NULL) {
      perror("Could not open Feature file");
      exit(-1);
    }
    fread(&utt_num_rows, sizeof(utt_num_rows), 1, utt_feat_file);
    fread(&utt_num_cols, sizeof(utt_num_cols), 1, utt_feat_file);

    if (i == 0) {
      // First file. We initialize the means and standard devs
      feature_means = new double[utt_num_cols];
      feature_variances = new double[utt_num_cols];
      for (int col = 0; col < utt_num_cols; col++) {
        feature_means[col] = 0;
        feature_variances[col] = 0;
      }
      datarow = new double[utt_num_cols];
    }

    for (int row = 0; row < utt_num_rows; row++) {
      fread(datarow, sizeof(datarow[0]), utt_num_cols, utt_feat_file);
      total_num_rows++;
      for (int col = 0; col < utt_num_cols; col++) {
        feature_means[col] += datarow[col];
        feature_variances[col] += (datarow[col] * datarow[col]);
      }
    }
    fclose(utt_feat_file);
  }

  // Calculate Means and Variances now
  for (int col = 0; col < utt_num_cols; col++) {
    feature_means[col] /= total_num_rows;
    feature_variances[col] = sqrt(feature_variances[col] / total_num_rows -
                                  (feature_means[col] * feature_means[col]));
  }

  delete[] datarow;
  *feature_means_ptr = feature_means;
  *feature_variances_ptr = feature_variances;
}

void GenerateScaledFeatureFile(const char* utt_id,
                               const float scaling_factor,
                               const double* feature_means,
                               const double* feature_variances) {
#ifndef FESTVOX_NO_THREADS
  {
    ScopedLock sl(global_stdout_mutex);
    printf("Scaling Features. Pass 2 of 2. Processing utt %s\n", utt_id);
  }
#else
  printf("Scaling Features. Pass 2 of 2. Processing utt %s\n", utt_id);
#endif

  FILE *input_file;
  FILE *output_file;

  int utt_num_rows = 0;
  int utt_num_cols = 0;

  double* datarow;

  char tmp[256];

  // Open input file
  snprintf(tmp, sizeof(tmp), "ehmm/binfeat/%s.%s",
           utt_id, kUnscaledFeatureFileExtension);
  input_file = fopen(tmp, "rb");
  if (input_file == NULL) {
    perror("Could not load feature file");
    exit(-1);
  }

  // Open output file
  snprintf(tmp, sizeof(tmp), "ehmm/binfeat/%s.%s",
           utt_id, kScaledFeatureFileExtension);
  output_file = fopen(tmp, "wb");
  if (output_file == NULL) {
    perror("Could not open scaled feature file for writing");
    exit(-1);
  }

  // File Header
  fread(&utt_num_rows, sizeof(utt_num_rows), 1, input_file);
  fread(&utt_num_cols, sizeof(utt_num_cols), 1, input_file);

  fwrite(&utt_num_rows, sizeof(utt_num_rows), 1, output_file);
  fwrite(&utt_num_cols, sizeof(utt_num_cols), 1, output_file);


  datarow = new double[utt_num_cols];
  for (int row = 0; row < utt_num_rows; row++) {
    fread(datarow, sizeof(datarow[0]), utt_num_cols, input_file);
    for (int col = 0; col < utt_num_cols; col++) {
      datarow[col] = (datarow[col] - feature_means[col]) /
          feature_variances[col];
      datarow[col] *= scaling_factor;
    }
    fwrite(datarow, sizeof(datarow[0]), utt_num_cols, output_file);
  }

  delete[] datarow;

  fclose(input_file);
  fclose(output_file);
}

#ifndef FESTVOX_NO_THREADS
void* ThreadStart(void* userdata) {
  int current_sentence_number_to_process;

  while (1) {
    // See if the next sentence number to be processed exists
    {
      // Scope to define a mutex lock
      ScopedLock sl(global_utt_consumer_mutex);
      if (global_next_utt_id_index_to_process
          >= global_total_number_of_sentences)
        break;  // We are done. Thread can exit.

      current_sentence_number_to_process = global_next_utt_id_index_to_process;
      global_next_utt_id_index_to_process++;
      // End of mutex lock scope
    }

    FeatureScalingParameters* params =
        reinterpret_cast<FeatureScalingParameters*>(userdata);

    char* utt_id = params->utt_id_list[current_sentence_number_to_process];
    float scaling_factor = params->scaling_factor;
    double* feature_means = params->feature_means;
    double* feature_variances = params->feature_variances;

    // Don't lock this function with the mutex!
    GenerateScaledFeatureFile(utt_id, scaling_factor,
                              feature_means, feature_variances);
  }
  return NULL;
}
#endif  // FESTVOX_NO_THREADS
