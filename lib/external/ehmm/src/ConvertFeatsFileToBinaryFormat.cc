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
/*  Convert a Feature File used by EHMM into Binary Format               */
/*                                                                       */
/*************************************************************************/

#include<stdio.h>
#include<stdlib.h>

void ConvertFileToBinaryFormat(char* input_filename, char* output_filename);


void ConvertFileToBinaryFormat(char* input_filename, char* output_filename) {
  FILE *input_file, *output_file;
  bool file_open_error;
  int num_rows, num_cols;
  double** feats;
  double feat;


  file_open_error = false;
  input_file = fopen(input_filename, "r");
  output_file = fopen(output_filename, "wb");

  if (input_file == NULL) {
    printf("Could not open file %s for reading. Aborting.\n",
           input_filename);
    file_open_error = true;
  }
  if (output_file == NULL) {
    printf("Could not open file %s for writing. Aborting.\n",
           output_filename);
    file_open_error = true;
  }

  if (file_open_error) {
    exit(-1);
  }

  fscanf(input_file, "%d", &num_rows);
  fscanf(input_file, "%d", &num_cols);

  fwrite(&num_rows, sizeof(num_rows), 1, output_file);
  fwrite(&num_cols, sizeof(num_cols), 1, output_file);

  feats = new double*[num_rows];
  for (int row = 0; row < num_rows; row++) {
    feats[row] = new double[num_cols];
    for (int col = 0; col < num_cols; col++) {
      fscanf(input_file, "%lf", &feats[row][col]);
    }
  }

  // Use an element of feats that gets passed to sizeof, instead of
  // the type double. That way, the fwrite statement will be in sync
  // if data type of feats changes in the future.
  feat = feats[0][0];

  for (int row = 0; row < num_rows; row++) {
    fwrite(feats[row], sizeof(feat), num_cols, output_file);
  }

  fclose(input_file);
  fclose(output_file);

  for (int row = 0; row < num_rows; row++) {
    delete[] feats[row];
  }
  delete[] feats;

  printf("%s: Converted %d rows and %d columns to binary format\n",
         input_filename, num_rows, num_cols);
}

int main(int argc, char** argv) {
  char *input_filename, *output_filename;

  if (argc != 3) {
    printf("Usage: %s input_feat_file output_feat_file\n", argv[0]);
    return -1;
  }

  input_filename = argv[1];
  output_filename = argv[2];

  ConvertFileToBinaryFormat(input_filename, output_filename);
}
