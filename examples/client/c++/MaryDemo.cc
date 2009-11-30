/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
// This version, adapted to MARY 4.0, provided by Sebastian Ptock.

#include <iostream>
#include <fstream>
#include <stdlib.h>

#include "MaryClient.h"

using namespace std;

/**
 * Demonstration code for using the MaryClient.
 + Call this as:
 * ./MaryDemo
 * or
 * ./MaryDemo > output.wav
 */
int main() {
  int server_port = 59125;
  string server_host = "localhost";
  string inputText = "Welcome to the world of speech synthesis!";
  string maryInFormat = "TEXT";
  string maryOutFormat = "AUDIO";
  //string maryOutFormat = "REALISED_DURATIONS";
  string locale = "en-US";
  string audioType = "WAV_FILE";
  string voice = "cmu-slt-hsmm";
  string effects;
//  effects += "Volume(amount:5.0;)+";
//  effects += "TractScaler(amount:1.5;)+";
//  effects += "F0Scale(f0Scale:2.0;)+";
//  effects += "F0Add(f0Add:50.0;)+";
//  effects += "Rate(durScale:1.5;)+";
//  effects += "Robot(amount:100.0;)+";
//  effects += "Whisper(amount:100.0;)+";
//  effects += "Stadium(amount:100.0)+";
//  effects += "Chorus(delay1:466;amp1:0.54;delay2:600;amp2:-0.10;delay3:250;amp3:0.30)+";
//  effects += "FIRFilter(type:3;fc1:500.0;fc2:2000.0)+";
//  effects += "JetPilot";
  string result;

  MaryClient maryClient;
  maryClient.maryQuery( server_port, server_host, result, inputText, maryInFormat, maryOutFormat, locale, audioType, voice, effects);

  if (maryOutFormat == "AUDIO") {
    // write result into a file
    const char *filename = "output.wav";
    ofstream file( filename );
    file << result;

    // play output
    //system("play output.wav");
  } else {
    cout << "RESULT: " << endl << result << endl;
  }

  return 0;
}

