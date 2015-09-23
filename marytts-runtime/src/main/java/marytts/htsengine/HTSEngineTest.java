//* ----------------------------------------------------------------- */
/*           The HMM-Based Speech Synthesis Engine "hts_engine API"  */
/*           developed by HTS Working Group                          */
/*           http://hts-engine.sourceforge.net/                      */
/* ----------------------------------------------------------------- */
/*                                                                   */
/*  Copyright (c) 2001-2010  Nagoya Institute of Technology          */
/*                           Department of Computer Science          */
/*                                                                   */
/*                2001-2008  Tokyo Institute of Technology           */
/*                           Interdisciplinary Graduate School of    */
/*                           Science and Engineering                 */
/*                                                                   */
/* All rights reserved.                                              */
/*                                                                   */
/* Redistribution and use in source and binary forms, with or        */
/* without modification, are permitted provided that the following   */
/* conditions are met:                                               */
/*                                                                   */
/* - Redistributions of source code must retain the above copyright  */
/*   notice, this list of conditions and the following disclaimer.   */
/* - Redistributions in binary form must reproduce the above         */
/*   copyright notice, this list of conditions and the following     */
/*   disclaimer in the documentation and/or other materials provided */
/*   with the distribution.                                          */
/* - Neither the name of the HTS working group nor the names of its  */
/*   contributors may be used to endorse or promote products derived */
/*   from this software without specific prior written permission.   */
/*                                                                   */
/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            */
/* CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       */
/* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          */
/* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          */
/* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS */
/* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          */
/* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   */
/* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     */
/* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON */
/* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   */
/* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    */
/* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           */
/* POSSIBILITY OF SUCH DAMAGE.                                       */
/* ----------------------------------------------------------------- */
/**
 * Copyright 2011 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.htsengine;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.modules.HTSEngine;
import marytts.signalproc.analysis.Mfccs;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.text.SnackTextfileDoubleDataSource;
import marytts.util.io.LEDataInputStream;

/***
 * Several functions for running the htsEngine or other components stand alone
 * 
 * @author Marcela Charfuelan
 *
 */
public class HTSEngineTest {

	public class PhonemeDuration {
		private String phone;
		private float duration;

		public PhonemeDuration(String ph, float dur) {
			phone = ph;
			duration = dur;
		}

		public void setPhoneme(String str) {
			phone = str;
		}

		public void setDuration(float fval) {
			duration = fval;
		}

		public String getPhoneme() {
			return phone;
		}

		public float getDuration() {
			return duration;
		}

	}

	/**
	 * Generation of speech using external specification of duration: using ContinuousFeatureProcessors of TARGETFEATURES Input: a
	 * TARGETFEATURES (.pfeats) file, this file should contain ContinuousFeatureProcessors: unit_duration float unit_logf0 float
	 * unit_logf0delta float The features unit_duration and unit_logf0 are used as external prosody, unit_logf0Delta is not used.
	 * The TARGETFEATURES (.pfeats) file including ContinuousFeatureProcessors values can be generated with a unitselection voice
	 * or a mbrola voice, it can NOT be generated with HMM voices.
	 * 
	 * 
	 * @throws Exception
	 *             Exception
	 */
	public void synthesisWithContinuousFeatureProcessors() throws Exception {

		int i, j, n, t;
		// context features file
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/red_ball.pfeats";
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/THAT_ball.pfeats";
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/RED_ball.pfeats";
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/red_BALL.pfeats";
		String feaFile = "/project/mary/marcela/f0-hsmm-experiment/THAT_BALL.pfeats";
		//
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/us1-mbrola.pfeats";
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/cmu-slt-unit-selection.pfeats";
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/welcome.pfeats";
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/canadian.pfeats";
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/arctic_a0003.pfeats";
		// String feaFile = "/project/mary/marcela/f0-hsmm-experiment/author.pfeats";

		HTSEngine hmm_tts = new HTSEngine();
		HMMData htsData = new HMMData();

		/* For initialise provide the name of the hmm voice and the name of its configuration file, */
		String MaryBase = "/project/mary/marcela/openmary/"; /* MARY_BASE directory. */
		String voiceName = "cmu-slt-hsmm"; /* voice name */
		String voiceConfig = "en_US-cmu-slt-hsmm.config"; /* voice configuration file name. */
		String outWavFile = MaryBase + "tmp/tmp.wav"; /* to save generated audio file */

		htsData.initHMMData(voiceName, MaryBase, voiceConfig);

		// Set these variables so the htsEngine use the ContinuousFeatureProcessors features
		htsData.setUseAcousticModels(true);

		// The settings for using GV and MixExc can besynthesisWithExternalProsodySpecificationFiles changed in this way:
		htsData.setUseGV(true);
		htsData.setUseMixExc(true);
		htsData.setUseFourierMag(true); // if the voice was trained with Fourier magnitudes

		/**
		 * The utterance model, um, is a Vector (or linked list) of Model objects. It will contain the list of models for current
		 * label file.
		 */
		HTSUttModel um = new HTSUttModel();
		HTSParameterGeneration pdf2par = new HTSParameterGeneration();
		HTSVocoder par2speech = new HTSVocoder();
		AudioInputStream ais;

		try {
			/* Process Mary context features file and creates UttModel um. */
			um = hmm_tts.processUttFromFile(feaFile, htsData);

			/* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
			/* the generated parameters will be saved in tmp.mfc and tmp.f0, including Mary header. */
			boolean debug = false; /* so it DOES NOT save the generated parameters in parFile */
			pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

			/* Synthesize speech waveform, generate speech out of sequence of parameters */
			ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

			System.out.println("saving to file: " + outWavFile);
			File fileOut = new File(outWavFile);

			if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
				AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
			}

			System.out.println("Calling audioplayer:");
			AudioPlayer player = new AudioPlayer(fileOut);
			player.start();
			player.join();
			System.out.println("audioplayer finished...");

		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		}
	} /* main method */

	/**
	 * Generation of speech using external specification of duration: duration and logf0 in external files Input: a TARGETFEATURES
	 * (.pfeats) file
	 * 
	 * 
	 * @throws Exception
	 *             Exception
	 */
	public void synthesisWithProsodySpecificationInExternalFiles() throws Exception {

		int i, j, n, t;
		// context features file
		String feaFile = "/project/mary/marcela/openmary/lib/voices/cmu-slt-hsmm/cmu_us_arctic_slt_a0001.pfeats";

		HTSEngine hmm_tts = new HTSEngine();
		HMMData htsData = new HMMData();

		/* For initialise provide the name of the hmm voice and the name of its configuration file, */
		String MaryBase = "/project/mary/marcela/openmary/"; /* MARY_BASE directory. */
		String voiceName = "cmu-slt-hsmm"; /* voice name */
		String voiceConfig = "en_US-cmu-slt-hsmm.config"; /* voice configuration file name. */
		String outWavFile = MaryBase + "tmp/tmp.wav"; /* to save generated audio file */

		htsData.initHMMData(voiceName, MaryBase, voiceConfig);

		// The settings for using GV and MixExc can be changed in this way:
		htsData.setUseGV(true);
		htsData.setUseMixExc(true);
		htsData.setUseFourierMag(true); // if the voice was trained with Fourier magnitudes

		/**
		 * The utterance model, um, is a Vector (or linked list) of Model objects. It will contain the list of models for current
		 * label file.
		 */
		HTSUttModel um = new HTSUttModel();
		HTSParameterGeneration pdf2par = new HTSParameterGeneration();
		HTSVocoder par2speech = new HTSVocoder();
		AudioInputStream ais;

		// Specify external files:
		// external duration extracted with the voice import tools - EHMM
		String labFile = "/project/mary/marcela/f0-hsmm-experiment/cmu_us_arctic_slt_a0001.lab";
		// external duration obtained with MARY, there is a problem with this because it does not have an initial sil
		// String labFile = "/project/mary/marcela/f0-hsmm-experiment/cmu_us_arctic_slt_a0001.realised_durations";

		// external F0 contour obtained with SPTK during HMMs creation
		String lf0File = "/project/mary/marcela/f0-hsmm-experiment/cmu_us_arctic_slt_a0001.lf0";

		// Load and set external durations
		// ---this is not working in MARY 4.1
		// ---htsData.setUseDurationFromExternalFile(true);
		float totalDuration;
		int totalDurationFrames;
		float fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());
		hmm_tts.setPhonemeAlignmentForDurations(true);
		Vector<PhonemeDuration> durations = new Vector<PhonemeDuration>();
		totalDuration = loadDurationsForAlignment(labFile, durations);
		// set the external durations
		hmm_tts.setAlignDurations(durations);
		totalDurationFrames = (int) ((totalDuration / fperiodsec));
		// Depending on how well aligned the durations and the lfo file are
		// this factor can be used to extend or shrink the durations per phoneme so
		// it syncronize with the number of frames in the lf0 file
		hmm_tts.setNewStateDurationFactor(0.37);

		// set external logf0
		htsData.setUseAcousticModels(true);

		try {
			/* Process Mary context features file and creates UttModel um. */
			um = hmm_tts.processUttFromFile(feaFile, htsData);

			/* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
			/* the generated parameters will be saved in tmp.mfc and tmp.f0, including Mary header. */
			boolean debug = false; /* so it DOES NOT save the generated parameters in parFile */
			pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

			/* Synthesize speech waveform, generate speech out of sequence of parameters */
			ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

			System.out.println("saving to file: " + outWavFile);
			File fileOut = new File(outWavFile);

			if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
				AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
			}

			System.out.println("Calling audioplayer:");
			AudioPlayer player = new AudioPlayer(fileOut);
			player.start();
			player.join();
			System.out.println("audioplayer finished...");

		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		}
	} /* main method */

	/***
	 * Load durations for phone alignment when the durations have been generated by EHMMs.
	 * 
	 * @param fileName
	 *            the format is the same as for phonelab.
	 * @param alignDur
	 *            alignDur
	 * @return totalDuration
	 */
	public float loadDurationsForAlignment(String fileName, Vector<PhonemeDuration> alignDur) {

		Scanner s = null;
		String line;
		float totalDuration = 0;
		float previous = 0;
		float current = 0;
		try {
			s = new Scanner(new File(fileName));
			int i = 0;
			while (s.hasNext()) {
				line = s.nextLine();
				if (!line.startsWith("#") && !line.startsWith("format")) {
					String val[] = line.split(" ");
					current = Float.parseFloat(val[0]);
					PhonemeDuration var;
					if (previous == 0)
						alignDur.add(new PhonemeDuration(val[2], current));
					else
						alignDur.add(new PhonemeDuration(val[2], (current - previous)));

					totalDuration += alignDur.get(i).getDuration();
					System.out.println("phone = " + alignDur.get(i).getPhoneme() + " dur(" + i + ")="
							+ alignDur.get(i).getDuration() + " totalDuration=" + totalDuration);
					i++;
					previous = current;
				}
			}
			System.out.println();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// return alignDur;
		return totalDuration;
	}

	/***
	 * Load logf0, in HTS format, create a voiced array and set this values in pdf2par This contour should be aligned with the
	 * durations, so the total duration in frames should be the same as in the lf0 file
	 * 
	 * @param lf0File
	 *            : in HTS formant
	 * @param totalDurationFrames
	 *            : the total duration in frames can be calculated as: totalDurationFrames = totalDurationInSeconds /
	 *            (framePeriodInSamples / SamplingFrequencyInHz)
	 * @param pdf2par
	 *            : HTSParameterGeneration object
	 * @throws Exception
	 *             If the number of frames in the lf0 file is not the same as represented in the total duration (in frames).
	 */
	public void loadF0contour(String lf0File, int totalDurationFrames, HTSParameterGeneration pdf2par) throws Exception {
		HTSPStream lf0Pst = null;
		boolean[] voiced = null;
		LEDataInputStream lf0Data;

		int lf0Vsize = 3;
		int totalFrame = 0;
		int lf0VoicedFrame = 0;
		float fval;
		lf0Data = new LEDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
		/* First i need to know the size of the vectors */
		try {
			while (true) {
				fval = lf0Data.readFloat();
				totalFrame++;
				if (fval > 0)
					lf0VoicedFrame++;
			}
		} catch (EOFException e) {
		}
		lf0Data.close();

		// Here we need to check that the total duration in frames is the same as the number of frames
		// (NOTE: it can be a problem afterwards when the durations per phone are aligned to the lenght of each state
		// in htsEngine._processUtt() )
		if (totalDurationFrames != totalFrame) {
			System.out.println("The total duration in frames " + totalDurationFrames
					+ " is not the same as the number of frames " + totalFrame + " in the lf0 file: " + lf0File);
		} else
			System.out.println("totalDurationFrames = " + totalDurationFrames + "  totalF0Frames = " + totalFrame);

		voiced = new boolean[totalFrame];
		lf0Pst = new HTSPStream(lf0Vsize, totalFrame, HMMData.FeatureType.LF0, 0);

		/* load lf0 data */
		/* for lf0 i just need to load the voiced values */
		lf0VoicedFrame = 0;
		lf0Data = new LEDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
		for (int i = 0; i < totalFrame; i++) {
			fval = lf0Data.readFloat();
			if (fval < 0) {
				voiced[i] = false;
				System.out.println("frame: " + i + " = 0.0");
			} else {
				voiced[i] = true;
				lf0Pst.setPar(lf0VoicedFrame, 0, fval);
				lf0VoicedFrame++;
				System.out.format("frame: %d = %.2f\n", i, fval);
			}
		}
		lf0Data.close();

		// Set lf0 and voiced in pdf2par
		pdf2par.setlf0Pst(lf0Pst);
		pdf2par.setVoicedArray(voiced);

	}

	/**
	 * Stand alone testing using a TARGETFEATURES file as input. Generates duration: file.lab, duration state level: file.slab,
	 * f0: file.f0, mfcc: file.mfcc and sound file: file.wav out of HMM models
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws Exception
	 *             Exception
	 */
	public void generateParameters() throws IOException, InterruptedException, Exception {

		int i, j;
		/*
		 * For initialise provide the name of the hmm voice and the name of its configuration file, also indicate the name of your
		 * MARY_BASE directory.
		 */
		String MaryBase = "/project/mary/marcela/openmary/";
		String locale = "english";
		String voice = "hsmm-slt";
		String configFile = locale + "-" + voice + ".config";

		// directory where the context features of each file are
		String contextFeaDir = "/project/mary/marcela/quality-control-experiment/slt/phonefeatures/";
		// the output dir has to be created already
		String outputDir = "/project/mary/marcela/quality-control-experiment/slt/hmmGenerated/";
		// list of contex features files, the file names contain the basename without path and ext
		String filesList = "/project/mary/marcela/quality-control-experiment/slt/phonefeatures-list.txt";

		// Create a htsengine object
		HTSEngine hmm_tts = new HTSEngine();

		// Create and set HMMData
		HMMData htsData = new HMMData();
		htsData.initHMMData(voice, MaryBase, configFile);
		float fperiodmillisec = ((float) htsData.getFperiod() / (float) htsData.getRate()) * 1000;
		float fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());

		// Settings for using GV, mixed excitation
		htsData.setUseGV(true);
		htsData.setUseMixExc(true);

		/* generate files out of HMMs */
		String file, feaFile, parFile, durStateFile, durFile, mgcModifiedFile, outWavFile;
		try {
			Scanner filesScanner = new Scanner(new BufferedReader(new FileReader(filesList)));
			while (filesScanner.hasNext()) {

				file = filesScanner.nextLine();

				feaFile = contextFeaDir + file + ".pfeats";
				parFile = outputDir + file; /* generated parameters mfcc and f0, Mary format */
				durFile = outputDir + file + ".lab"; /* realised durations */
				durStateFile = outputDir + file + ".slab"; /* state level realised durations */
				outWavFile = outputDir + file + ".wav"; /* generated wav file */

				/*
				 * The utterance model, um, is a Vector (or linked list) of Model objects. It will contain the list of models for
				 * the current label file.
				 */
				HTSUttModel um = new HTSUttModel();
				HTSParameterGeneration pdf2par = new HTSParameterGeneration();
				HTSVocoder par2speech = new HTSVocoder();
				AudioInputStream ais;

				/* Process label file of Mary context features and creates UttModel um. */
				um = hmm_tts.processUttFromFile(feaFile, htsData);

				/* save realised durations in a lab file */
				FileWriter outputStream;
				outputStream = new FileWriter(durFile);
				outputStream.write(hmm_tts.getRealisedDurations());
				outputStream.close();

				/* save realised durations at state label in a slab file */
				float totalDur = 0;
				int numStates = htsData.getCartTreeSet().getNumStates();
				outputStream = new FileWriter(durStateFile);
				outputStream.write("#\n");
				for (i = 0; i < um.getNumModel(); i++) {
					for (j = 0; j < numStates; j++) {
						totalDur += (um.getUttModel(i).getDur(j) * fperiodsec);
						if (j < (numStates - 1))
							outputStream.write(totalDur + " 0 " + um.getUttModel(i).getPhoneName() + "\n");
						else
							outputStream.write(totalDur + " 1 " + um.getUttModel(i).getPhoneName() + "\n");
					}
				}
				outputStream.close();

				/* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
				boolean debug = true; /*
									 * with debug=true it saves the generated parameters f0 and mfcc in parFile.f0 and
									 * parFile.mfcc in Mary format.
									 */
				pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

				/* Synthesize speech waveform, generate speech out of sequence of parameter */
				ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

				System.out.println("saving to file: " + outWavFile);
				File fileOut = new File(outWavFile);

				if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
					AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
				}
				/*
				 * // uncomment to listen the files System.out.println("Calling audioplayer:"); AudioPlayer player = new
				 * AudioPlayer(fileOut); player.start(); player.join(); System.out.println("audioplayer finished...");
				 */

			} // while files in testFiles
			filesScanner.close();

		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		}

	} /* main method */

	/***
	 * Calculate mfcc using SPTK, uses sox to convert wav&rarr;raw
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws Exception
	 *             Exception
	 */
	public void getSptkMfcc() throws IOException, InterruptedException, Exception {

		String inFile = "/project/mary/marcela/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.wav";
		String outFile = "/project/mary/marcela/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.mfc";
		String tmpFile = "/project/mary/marcela/quality-control-experiment/slt/tmp.mfc";
		String tmpRawFile = "/project/mary/marcela/quality-control-experiment/slt/tmp.raw";
		String cmd;
		// SPTK parameters
		int fs = 16000;
		int frameLength = 400;
		int frameLengthOutput = 512;
		int framePeriod = 80;
		int mgcOrder = 24;
		int mgcDimension = 25;
		// Mary header parameters
		double ws = (frameLength / fs); // window size in seconds
		double ss = (framePeriod / fs); // skip size in seconds

		// SOX and SPTK commands
		String sox = "/usr/bin/sox";
		String x2x = " /project/mary/marcela/sw/SPTK-3.1/bin/x2x";
		String frame = " /project/mary/marcela/sw/SPTK-3.1/bin/frame";
		String window = " /project/mary/marcela/sw/SPTK-3.1/bin/window";
		String mcep = " /project/mary/marcela/sw/SPTK-3.1/bin/mcep";
		String swab = "/project/mary/marcela/sw/SPTK-3.1/bin/swab";

		// convert the wav file to raw file with sox
		cmd = sox + " " + inFile + " " + tmpRawFile;
		launchProc(cmd, "sox", inFile);

		System.out.println("Extracting MGC coefficients from " + inFile);

		cmd = x2x + " +sf " + tmpRawFile + " | " + frame + " +f -l " + frameLength + " -p " + framePeriod + " | " + window
				+ " -l " + frameLength + " -L " + frameLengthOutput + " -w 1 -n 1 | " + mcep + " -a 0.42 -m " + mgcOrder
				+ "  -l " + frameLengthOutput + " | " + swab + " +f > " + tmpFile;

		System.out.println("cmd=" + cmd);
		launchBatchProc(cmd, "getSptkMfcc", inFile);

		// Now get the data and add the Mary header
		int numFrames;
		DataInputStream mfcData = null;
		Vector<Float> mfc = new Vector<Float>();

		mfcData = new DataInputStream(new BufferedInputStream(new FileInputStream(tmpFile)));
		try {
			while (true) {
				mfc.add(mfcData.readFloat());
			}
		} catch (EOFException e) {
		}
		mfcData.close();

		numFrames = mfc.size();
		int numVectors = numFrames / mgcDimension;
		Mfccs mgc = new Mfccs(numVectors, mgcDimension);

		int k = 0;
		for (int i = 0; i < numVectors; i++) {
			for (int j = 0; j < mgcDimension; j++) {
				mgc.mfccs[i][j] = mfc.get(k);
				k++;
			}
		}
		// Mary header parameters
		mgc.params.samplingRate = fs; /* samplingRateInHz */
		mgc.params.skipsize = (float) ss; /* skipSizeInSeconds */
		mgc.params.winsize = (float) ws; /* windowSizeInSeconds */

		mgc.writeMfccFile(outFile);
	}

	/***
	 * Calculate mfcc using SPTK, uses sox to convert wav&rarr;raw
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws Exception
	 *             Exception
	 */
	public void getSptkSnackLf0() throws IOException, InterruptedException, Exception {

		String inFile = "/project/mary/marcela/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.wav";
		String outFile = "/project/mary/marcela/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.lf0";
		String tmpFile = "/project/mary/marcela/quality-control-experiment/slt/tmp.mfc";
		String tmpRawFile = "/project/mary/marcela/quality-control-experiment/slt/tmp.raw";
		String tmpRawLongFile = "/project/mary/marcela/quality-control-experiment/slt/tmp_long.raw";
		String scriptFileName = "/project/mary/marcela/quality-control-experiment/slt/lf0.tcl";
		String snackFile = "/project/mary/marcela/quality-control-experiment/slt/tmp.lf0";
		String MAXPITCH;
		String MINPITCH;
		String gender = "female";
		if (gender.contentEquals("female")) {
			MAXPITCH = "500";
			MINPITCH = "100";
		} else { // male
			MAXPITCH = "300";
			MINPITCH = "75";
		}
		String FRAMELENGTH = "0.005";
		String FRAMERATE = "16000";

		String cmd;

		// SOX and SPTK commands
		String sox = "/usr/bin/sox";
		String x2x = " /project/mary/marcela/sw/SPTK-3.1/bin/x2x";
		String step = "/project/mary/marcela/sw/SPTK-3.1/bin/step";
		String nrand = "/project/mary/marcela/sw/SPTK-3.1/bin/nrand";
		String sopr = "/project/mary/marcela/sw/SPTK-3.1/bin/sopr";
		String vopr = "/project/mary/marcela/sw/SPTK-3.1/bin/vopr";
		String SNACKDIR = "/project/mary/marcela/sw/snack2.2.10/";

		// convert the wav file to raw file with sox
		cmd = sox + " " + inFile + " " + tmpRawFile;
		launchProc(cmd, "sox", inFile);

		// create temporary raw file, with 0.005 ms of silence (with a bit noise) added
		// at the beginning and 0.025 at the end
		System.out.println("Create temporary raw file" + inFile);
		cmd = step + " -l 80 -v 0.0 | x2x +fs > tmp.head\n" + step + " -l 400 -v 0.0 | x2x +fs > tmp.tail\n" + "cat tmp.head "
				+ tmpRawFile + " tmp.tail | x2x +sf > tmp.long\n" + "leng=`x2x +fa tmp.long | /usr/bin/wc -l`\n"
				+ "echo \"leng=$leng\"\n" + nrand + " -l $leng | " + sopr + " -m 50 | " + vopr + " -a tmp.long | " + x2x
				+ " +fs > " + tmpRawLongFile + "\n" + "rm tmp.tail tmp.long tmp.head " + tmpRawFile + "\n";

		System.out.println("cmd=" + cmd);
		launchBatchProc(cmd, "getSptkSnackLf0", tmpRawFile);

		// Now extract F0 with snack and the modified raw file
		System.out.println("scriptFileName = " + scriptFileName);
		File script = new File(scriptFileName);

		System.out.println("Extracting LF0 coefficients from " + inFile);
		if (script.exists())
			script.delete();
		PrintWriter toScript = new PrintWriter(new FileWriter(script));
		toScript.println("#!" + SNACKDIR);
		toScript.println("");
		toScript.println("package require snack");
		toScript.println("");
		toScript.println("snack::sound s");
		toScript.println("");
		toScript.println("s read [lindex $argv 0] -fileformat RAW -rate [lindex $argv 1] -encoding Lin16 -byteorder littleEndian");
		toScript.println("");
		toScript.println("set fd [open [lindex $argv 2] w]");
		toScript.println("set tmp [s pitch -method esps -maxpitch [lindex $argv 3] "
				+ "-minpitch [lindex $argv 4] -framelength [lindex $argv 5]]\n" + "foreach line $tmp {\n"
				+ "  set x [lindex $line 0]\n" + "  if { $x == 0 } {\n" + "    puts $fd -1.0e+10\n" + "  } else {\n"
				+ "    puts $fd [expr log($x)]\n" + "  }\n" + "}\n");
		toScript.println("close $fd");
		toScript.println("");
		toScript.println("exit");
		toScript.println("");
		toScript.close();

		cmd = "tcl " + scriptFileName + " " + tmpRawLongFile + " " + FRAMERATE + " " + snackFile + " " + MAXPITCH + " "
				+ MINPITCH + " " + FRAMELENGTH;
		System.out.println("cmd=" + cmd);
		launchProc(cmd, "getSptkSnackLf0", tmpRawLongFile);

		double[] f0 = new SnackTextfileDoubleDataSource(new FileReader(snackFile)).getAllData();
		for (int j = 0; j < f0.length; j++) {
			System.out.println(j + "  f0[" + j + "]= " + f0[j]);
		}

	}

	/**
	 * A general process launcher for the various tasks (copied from ESTCaller.java)
	 * 
	 * @param cmdLine
	 *            the command line to be launched.
	 * @param task
	 *            a task tag for error messages, such as "Pitchmarks" or "LPC".
	 * @param baseName
	 *            basename of the file currently processed, for error messages.
	 */
	private void launchProc(String cmdLine, String task, String baseName) {

		Process proc = null;
		BufferedReader procStdout = null;
		String line = null;
		try {
			proc = Runtime.getRuntime().exec(cmdLine);

			/* Collect stdout and send it to System.out: */
			procStdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			while (true) {
				line = procStdout.readLine();
				if (line == null)
					break;
				System.out.println(line);
			}
			/* Wait and check the exit value */
			proc.waitFor();
			if (proc.exitValue() != 0) {
				throw new RuntimeException(task + " computation failed on file [" + baseName + "]!\n" + "Command line was: ["
						+ cmdLine + "].");
			}
		} catch (IOException e) {
			throw new RuntimeException(task + " computation provoked an IOException on file [" + baseName + "].", e);
		} catch (InterruptedException e) {
			throw new RuntimeException(task + " computation interrupted on file [" + baseName + "].", e);
		}

	}

	/**
	 * A general process launcher for the various tasks but using an intermediate batch file (copied from ESTCaller.java)
	 * 
	 * @param cmdLine
	 *            the command line to be launched.
	 * @param task
	 *            a task tag for error messages, such as "Pitchmarks" or "LPC".
	 * @param baseName
	 *            basename of the file currently processed, for error messages.
	 */
	private void launchBatchProc(String cmdLine, String task, String baseName) {

		Process proc = null;
		Process proctmp = null;
		BufferedReader procStdout = null;
		String line = null;
		String tmpFile = "./tmp.bat";

		try {
			FileWriter tmp = new FileWriter(tmpFile);
			tmp.write(cmdLine);
			tmp.close();

			/* make it executable... */
			proctmp = Runtime.getRuntime().exec("chmod +x " + tmpFile);
			proctmp.waitFor();
			proc = Runtime.getRuntime().exec(tmpFile);

			/* Collect stdout and send it to System.out: */
			procStdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			while (true) {
				line = procStdout.readLine();
				if (line == null)
					break;
				System.out.println(line);
			}
			/* Wait and check the exit value */
			proc.waitFor();
			if (proc.exitValue() != 0) {
				throw new RuntimeException(task + " computation failed on file [" + baseName + "]!\n" + "Command line was: ["
						+ cmdLine + "].");
			}

		} catch (IOException e) {
			throw new RuntimeException(task + " computation provoked an IOException on file [" + baseName + "].", e);
		} catch (InterruptedException e) {
			throw new RuntimeException(task + " computation interrupted on file [" + baseName + "].", e);
		}

	}

	public static void main(String[] args) throws Exception {
		/* configure log info */
		org.apache.log4j.BasicConfigurator.configure();

		HTSEngineTest test = new HTSEngineTest();

		// generate parameters out of a hsmm voice
		// test.generateParameters();

		// extract mfcc from a wav file using sptk
		// test.getSptkMfcc();

		// extract lf0 from a wav file using sptk and snack
		// test.getSptkSnackLf0();

		// Synthesis with external duration and f0
		// it requires ContinuousFeatureProcessors in the TARGETFEATURES file
		test.synthesisWithContinuousFeatureProcessors();

		// Synthesis with external duration and f0
		// it requires two external files: labels file .lab and logf0 file .lf0
		// The duration indicated in the lab file must correspond to the number of frames in the .lf0 file
		// The lf0 file must be generated frame syncronous.
		// test.synthesisWithProsodySpecificationInExternalFiles();

	}

}
