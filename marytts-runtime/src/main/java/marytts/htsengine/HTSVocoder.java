/* ----------------------------------------------------------------- */
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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.process.AmplitudeNormalizer;
import marytts.util.MaryUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.ProducingDoubleDataSource;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.io.LEDataInputStream;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;

import marytts.htsengine.HMMData;

import org.apache.log4j.Logger;

/**
 * Synthesis of speech out of speech parameters. Mixed excitation MLSA vocoder.
 * 
 * Java port and extension of HTS engine API version 1.04 Extension: mixed excitation
 * 
 * @author Marcela Charfuelan
 */
public class HTSVocoder {

	public static final int IPERIOD = 1; /* interpolation period */
	public static final int SEED = 1;
	public static final int PADEORDER = 5; /* pade order for MLSA filter */
	public static final int IRLENG = 96; /* length of impulse response */

	public static final double ZERO = 1.0e-10; /* ~(0) */
	public static final double LZERO = (-1.0e+10); /* ~log(0) */

	/* ppade is a copy of pade in mlsadf() function : ppade = &( pade[pd*(pd+1)/2] ); */
	static final double[] pade = new double[] { /* used in mlsadf */
	1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0.4999273, 0.1067005, 0.01170221, 0.0005656279, 1, 0.4999391, 0.1107098, 0.01369984,
			0.0009564853, 0.00003041721 };
	static final int ppade = PADEORDER * (PADEORDER + 1) / 2; /* offset for vector pade */;

	private static final Logger logger = MaryUtils.getLogger("Vocoder");

	private Random rand;
	private int stage; /* Gamma=-1/stage : if stage=0 then Gamma=0 */
	private double gamma; /* Gamma */
	private boolean use_log_gain; /* log gain flag (for LSP) */
	private int fprd; /* frame shift */
	private double p1; /* used in excitation generation */
	private double pc; /* used in excitation generation */

	private double C[]; /* used in the MLSA/MGLSA filter */
	private double CC[]; /* used in the MLSA/MGLSA filter */
	private double CINC[]; /* used in the MLSA/MGLSA filter */
	private double D1[]; /* used in the MLSA/MGLSA filter */

	private double rate;
	int pt2; /* used in mlsadf2 */
	private final int pt3[] = new int[PADEORDER + 1]; /* used in mlsadf2 */

	/* mixed excitation variables */
	private int numM; /* Number of bandpass filters for mixed excitation */
	private int orderM; /* Order of filters for mixed excitation */
	private double h[][]; /* filters for mixed excitation */
	private double xpulseSignal[]; /* the size of this should be orderM */
	private double xnoiseSignal[]; /* the size of this should be orderM */
	private boolean mixedExcitation = false;
	private boolean fourierMagnitudes = false;

	/**
	 * The initialisation of VocoderSetup should be done when there is already information about the number of feature vectors to
	 * be processed, size of the mcep vector file, etc.
	 * 
	 * @param mcep_order
	 *            mcep_order
	 * @param mcep_vsize
	 *            mcep_vsize
	 * @param htsData
	 *            htsData
	 */
	private void initVocoder(int mcep_order, int mcep_vsize, HMMData htsData) {

		stage = htsData.getStage();
		gamma = htsData.getGamma();
		use_log_gain = htsData.getUseLogGain();

		fprd = htsData.getFperiod();
		rate = htsData.getRate();

		rand = new Random(SEED);

		C = new double[mcep_order];
		CC = new double[mcep_order];
		CINC = new double[mcep_order];

		if (stage == 0) { /* for MGC */

			/* mcep_order=74 and pd=PADEORDER=5 (if no HTS_EMBEDDED is used) */
			int vector_size = (mcep_vsize * (3 + PADEORDER) + 5 * PADEORDER + 6) - (3 * (mcep_order));
			D1 = new double[vector_size];

			pt2 = (2 * (PADEORDER + 1)) + (PADEORDER * (mcep_order + 1));

			for (int i = PADEORDER; i >= 1; i--)
				pt3[i] = (2 * (PADEORDER + 1)) + ((i - 1) * (mcep_order + 1));

		} else { /* for LSP */
			int vector_size = ((mcep_vsize + 1) * (stage + 3)) - (3 * (mcep_order));
			D1 = new double[vector_size];
		}

		/* excitation initialisation */
		p1 = -1;
		pc = 0.0;

	} /* method initVocoder */

	/**
	 * HTS_MLSA_Vocoder: Synthesis of speech out of mel-cepstral coefficients. This procedure uses the parameters generated in
	 * pdf2par stored in: PStream mceppst: Mel-cepstral coefficients PStream strpst : Filter bank stregths for mixed excitation
	 * PStream magpst : Fourier magnitudes PStream lf0pst : Log F0
	 * 
	 * @param pdf2par
	 *            pdf2par
	 * @param htsData
	 *            htsData
	 * @throws Exception
	 *             Exception
	 * @return DDSAudioInputStream
	 */
	public AudioInputStream htsMLSAVocoder(HTSParameterGeneration pdf2par, HMMData htsData) throws Exception {

		int audioSize = computeAudioSize(pdf2par.getMcepPst(), htsData);
		HTSVocoderDataProducer producer = new HTSVocoderDataProducer(audioSize, pdf2par, htsData);
		producer.start();
		return new DDSAudioInputStream(producer, getHTSAudioFormat(htsData));

		/*
		 * double [] audio_double = null;
		 * 
		 * audio_double = htsMLSAVocoder(pdf2par.getlf0Pst(), pdf2par.getMcepPst(), pdf2par.getStrPst(), pdf2par.getMagPst(),
		 * pdf2par.getVoicedArray(), htsData);
		 * 
		 * long lengthInSamples = (audio_double.length * 2 ) / (sampleSizeInBits/8); logger.debug("length in samples=" +
		 * lengthInSamples );
		 * 
		 * // Normalise the signal before return, this will normalise between 1 and -1 double MaxSample =
		 * MathUtils.getAbsMax(audio_double); for (int i=0; i<audio_double.length; i++) audio_double[i] = ( audio_double[i] /
		 * MaxSample ); //audio_double[i] = 0.3 * ( audio_double[i] / MaxSample );
		 * 
		 * return new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);
		 */
	} // method htsMLSAVocoder()

	/**
	 * get the audio format produced by the hts vocoder
	 * 
	 * @param htsData
	 *            htsData
	 * @return audioformat
	 */
	public static AudioFormat getHTSAudioFormat(HMMData htsData) {
		float sampleRate = htsData.getRate(); // 8000,11025,16000,22050,44100,48000
		int sampleSizeInBits = 16; // 8,16
		int channels = 1; // 1,2
		boolean signed = true; // true,false
		boolean bigEndian = false; // true,false
		AudioFormat af = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		return af;
	}

	public double[] htsMLSAVocoder(HTSPStream lf0Pst, HTSPStream mcepPst, HTSPStream strPst, HTSPStream magPst, boolean[] voiced,
			HMMData htsData, HTSVocoderDataProducer audioProducer) throws Exception {

		double inc, x, MaxSample;
		double xp = 0.0, xn = 0.0, fxp, fxn, mix; /* samples for pulse and for noise and the filtered ones */
		int k, m, mcepframe, lf0frame;
		double alpha = htsData.getAlpha();
		double beta = htsData.getBeta();
		double[] magPulse = null; /* pulse generated from Fourier magnitudes */
		int magSample, magPulseSize;

		double f0Std, f0Shift, f0MeanOri;
		double hp[] = null; /* pulse shaping filter, it is initialised once it is known orderM */
		double hn[] = null; /* noise shaping filter, it is initialised once it is known orderM */

		/*
		 * Initialise vocoder and mixed excitation, once initialised it is known the order of the filters so the shaping filters
		 * hp and hn can be initialised.
		 */
		m = mcepPst.getOrder();
		initVocoder(m, mcepPst.getVsize() - 1, htsData);
		double pulse[] = new double[fprd];
		double noise[] = new double[fprd];
		double source[] = new double[fprd];

		double[] d = new double[m];
		mixedExcitation = htsData.getUseMixExc();
		fourierMagnitudes = htsData.getUseFourierMag();

		if (mixedExcitation && htsData.getPdfStrStream() != null) {
			numM = htsData.getNumFilters();
			orderM = htsData.getOrderFilters();

			xpulseSignal = new double[orderM];
			xnoiseSignal = new double[orderM];
			/* initialise xp_sig and xn_sig */// -> automatically initialized to 0.0

			h = htsData.getMixFilters();
			hp = new double[orderM];
			hn = new double[orderM];

			// Check if the number of filters is equal to the order of strpst
			// i.e. the number of filters is equal to the number of generated strengths per frame.
			if (numM != strPst.getOrder()) {
				logger.debug("htsMLSAVocoder: error num mix-excitation filters =" + numM
						+ " in configuration file is different from generated str order=" + strPst.getOrder());
				throw new Exception("htsMLSAVocoder: error num mix-excitation filters = " + numM
						+ " in configuration file is different from generated str order=" + strPst.getOrder());
			}
			logger.debug("HMM speech generation with mixed-excitation.");
		} else
			logger.debug("HMM speech generation without mixed-excitation.");

		if (fourierMagnitudes && htsData.getPdfMagStream() != null)
			logger.debug("Pulse generated with Fourier Magnitudes.");
		// else
		// logger.info("Pulse generated as a unit pulse.");

		if (beta != 0.0)
			logger.debug("Postfiltering applied with beta=" + beta);
		else
			logger.debug("No postfiltering applied.");

		f0Std = htsData.getF0Std();
		f0Shift = htsData.getF0Mean();
		f0MeanOri = 0.0;

		for (mcepframe = 0, lf0frame = 0; mcepframe < mcepPst.getT(); mcepframe++) {
			if (voiced[mcepframe]) {
				f0MeanOri = f0MeanOri + Math.exp(lf0Pst.getPar(lf0frame, 0));
				// System.out.println("voiced t=" + mcepframe + "  " + lf0Pst.getPar(lf0frame, 0) + "  ");
				lf0frame++;
			}
			// else
			// System.out.println("unvoiced t=" + mcepframe + "  0.0  ");
		}
		f0MeanOri = f0MeanOri / lf0frame;

		/* _______________________Synthesize speech waveforms_____________________ */
		/* generate Nperiod samples per mcepframe */
		int s = 0; /* number of samples */
		int s_double = 0;
		int audio_size = computeAudioSize(mcepPst, htsData); /* audio size in samples, calculated as num frames * frame period */
		double[] audio_double = new double[audio_size]; /* initialise buffer for audio */

		magSample = 1;
		magPulseSize = 0;
		for (mcepframe = 0, lf0frame = 0; mcepframe < mcepPst.getT(); mcepframe++) { /* for each mcep frame */

			/** feature vector for a particular frame */
			double mc[] = new double[m]; /* feature vector for a particular frame */
			/* get current feature vector mgc */
			for (int i = 0; i < m; i++)
				mc[i] = mcepPst.getPar(mcepframe, i);

			/* f0 modification through the MARY audio effects */
			double f0 = 0.0;
			if (voiced[mcepframe]) {
				f0 = f0Std * Math.exp(lf0Pst.getPar(lf0frame, 0)) + (1 - f0Std) * f0MeanOri + f0Shift;
				lf0frame++;
				f0 = Math.max(0.0, f0);
			}

			/*
			 * if mixed excitation get shaping filters for this frame the strength of pulse, is taken from the predicted value,
			 * which can be maximum 1.0, and the strength of noise is the rest -> 1.0 - strPulse
			 */
			double str = 0.0;
			if (mixedExcitation) {
				for (int j = 0; j < orderM; j++) {
					hp[j] = hn[j] = 0.0;
					for (int i = 0; i < numM; i++) {

						str = strPst.getPar(mcepframe, i);
						hp[j] += str * h[i][j];
						hn[j] += (1 - str) * h[i][j];

						// hp[j] += strPst.getPar(mcepframe, i) * h[i][j];
						// hn[j] += ( 0.9 - strPst.getPar(mcepframe, i) ) * h[i][j];
					}
				}
			}

			/* f0 -> pitch , in original code here it is used p, so f0=p in the c code */
			if (f0 != 0.0)
				f0 = rate / f0;

			/* p1 is initialised in -1, so this will be done just for the first frame */
			if (p1 < 0) {
				p1 = f0;
				pc = p1;
				/* for LSP */
				if (stage != 0) {
					C[0] = (use_log_gain) ? LZERO : ZERO;
					double PI_m = Math.PI / m;
					for (int i = 0; i < m; i++)
						C[i] = i * PI_m;
					/* LSP -> MGC */
					lsp2mgc(C, C, (m - 1), alpha);
					mc2b(C, C, (m - 1), alpha);
					gnorm(C, C, (m - 1), gamma);
					for (int i = 1; i < m; i++)
						C[i] *= gamma;
				}
			}

			if (stage == 0) {
				/* postfiltering, this is done if beta>0.0 */
				postfilter_mgc(mc, (m - 1), alpha, beta);
				/* mc2b: transform mel-cepstrum to MLSA digital filter coefficients */
				mc2b(mc, CC, (m - 1), alpha);
				for (int i = 0; i < m; i++)
					CINC[i] = (CC[i] - C[i]) * IPERIOD / fprd;
			} else {

				lsp2mgc(mc, CC, (m - 1), alpha);

				mc2b(CC, CC, (m - 1), alpha);

				gnorm(CC, CC, (m - 1), gamma);

				for (int i = 1; i < m; i++)
					CC[i] *= gamma;

				for (int i = 0; i < m; i++)
					CINC[i] = (CC[i] - C[i]) * IPERIOD / fprd;

			}

			/* p=f0 in c code!!! */

			if (p1 != 0.0 && f0 != 0.0) {
				inc = (f0 - p1) * (double) IPERIOD / (double) fprd;
			} else {
				inc = 0.0;
				pc = f0;
				p1 = 0.0;
				// System.out.println("  inc=" + inc + "  ***pc=" + pc + "  p1=" + p1);
			}

			/* Here i need to generate both xp:pulse and xn:noise signals separately */
			// gauss = false; /* Mixed excitation works better with nomal noise */

			/* Generate fperiod samples per feature vector, normally 80 samples per frame */
			// p1=0.0;
			for (int j = fprd - 1, i = (IPERIOD + 1) / 2; j >= 0; j--) {
				if (p1 == 0.0) {

					x = uniformRand(); /* returns 1.0 or -1.0 uniformly distributed */

					if (mixedExcitation) {
						xn = x;
						xp = 0.0;
					}
				} else {
					if ((pc += 1.0) >= p1) {
						if (fourierMagnitudes) {
							magPulse = genPulseFromFourierMag(magPst, mcepframe, p1);
							magSample = 0;
							magPulseSize = magPulse.length;
							x = magPulse[magSample];
							magSample++;
						} else
							x = Math.sqrt(p1);

						pc = pc - p1;
					} else {

						if (fourierMagnitudes) {
							if (magSample >= magPulseSize) {
								x = 0.0;
							} else
								x = magPulse[magSample];
							magSample++;
						} else
							x = 0.0;
					}

					if (mixedExcitation) {
						xp = x;
						xn = uniformRand();
					}
				}
				// System.out.print("    x=" + x);

				/* apply the shaping filters to the pulse and noise samples */
				/* i need memory of at least for M samples in both signals */
				if (mixedExcitation) {
					fxp = 0.0;
					fxn = 0.0;
					for (k = orderM - 1; k > 0; k--) {
						fxp += hp[k] * xpulseSignal[k];
						fxn += hn[k] * xnoiseSignal[k];
						xpulseSignal[k] = xpulseSignal[k - 1];
						xnoiseSignal[k] = xnoiseSignal[k - 1];
					}
					fxp += hp[0] * xp;
					fxn += hn[0] * xn;
					xpulseSignal[0] = xp;
					xnoiseSignal[0] = xn;

					/* x is a pulse noise excitation and mix is mixed excitation */
					mix = fxp + fxn;
					pulse[j] = fxp;
					noise[j] = fxn;
					source[j] = mix;
					// System.out.format("%d = %f \n", j, mix);

					/* comment this line if no mixed excitation, just pulse and noise */
					x = mix; /* excitation sample */
				}

				if (stage == 0) {
					if (x != 0.0)
						x *= Math.exp(C[0]);
					x = mlsadf(x, C, m, alpha, D1, pt2, pt3);

				} else {
					x *= C[0];
					x = mglsadf(x, C, (m - 1), alpha, stage, D1);
				}

				// System.out.format("%f ", x);
				audio_double[s_double] = x;
				if (audioProducer != null) {
					audioProducer.putOneDataPoint(x);
				}

				s_double++;

				if ((--i) == 0) {
					p1 += inc;
					for (k = 0; k < m; k++) {
						C[k] += CINC[k];
					}
					i = IPERIOD;
				}

			} /* for each sample in a period fprd */

			/*********
			 * For debuging if(voiced[mcepframe]) { double magf[] = SignalProcUtils.getFrameHalfMagnitudeSpectrum(source, 512, 1);
			 * MaryUtils.plot(magf, "magf"); } System.out.format("str=%.2f\n", str);
			 */

			p1 = f0;

			/* move elements in c */
			System.arraycopy(CC, 0, C, 0, m);

		} /* for each mcep frame */

		logger.debug("Finish processing " + mcepframe + " mcep frames.");

		return (audio_double);

	} /* method htsMLSAVocoder() */

	/**
	 * Compute the audio size, in samples, that this vocoder is going to produce for the given data.
	 * 
	 * @param mcepPst
	 *            mcepPst
	 * @param htsData
	 *            htsData
	 * @return mcepPst.getT * htsData.getFperiod
	 */
	private int computeAudioSize(HTSPStream mcepPst, HMMData htsData) {
		return mcepPst.getT() * htsData.getFperiod();
	}

	private void printVector(String val, int m, double vec[]) {
		int i;
		System.out.println(val);
		for (i = 0; i < m; i++)
			System.out.println("v[" + i + "]=" + vec[i]);
	}

	/**
	 * mlsafir: sub functions for MLSA filter
	 * 
	 * @param x
	 *            x
	 * @param b
	 *            b
	 * @param m
	 *            m
	 * @param a
	 *            a
	 * @param d
	 *            d
	 * @param _pt3
	 *            _pt3
	 * @return y
	 */
	private static double mlsafir(double x, double b[], int m, double a, double d[], int _pt3) {
		d[_pt3 + 0] = x;
		d[_pt3 + 1] = (1 - a * a) * d[_pt3 + 0] + (a * d[_pt3 + 1]);

		for (int i = 2; i <= m; i++) {
			d[_pt3 + i] += a * (d[_pt3 + i + 1] - d[_pt3 + i - 1]);
		}

		double y = 0.0;
		for (int i = 2; i <= m; i++) {
			y += d[_pt3 + i] * b[i];
		}

		for (int i = m + 1; i > 1; i--) {
			d[_pt3 + i] = d[_pt3 + i - 1];
		}

		return y;
	}

	/**
	 * mlsdaf1: sub functions for MLSA filter
	 * 
	 * @param x
	 *            x
	 * @param b
	 *            b
	 * @param m
	 *            m
	 * @param a
	 *            a
	 * @param d
	 *            d
	 * @return out
	 */
	private static double mlsadf1(double x, double b[], int m, double a, double d[]) {
		// pt1 --> pt = &d1[pd+1]

		double out = 0.0;
		for (int i = PADEORDER; i > 0; i--) {
			d[i] = (1 - a * a) * d[PADEORDER + i] + a * d[i];
			d[PADEORDER + 1 + i] = d[i] * b[1];
			double v = d[PADEORDER + 1 + i] * pade[ppade + i];

			x += ((1 & i) == 1) ? v : -v;
			/*
			 * if(i == 1 || i == 3 || i == 5) x += v; else x += -v;
			 */
			out += v;
		}
		d[PADEORDER + 1] = x;
		out += x;

		return out;

	}

	/**
	 * mlsdaf2: sub functions for MLSA filter
	 * 
	 * @param x
	 *            x
	 * @param b
	 *            b
	 * @param m
	 *            m
	 * @param a
	 *            a
	 * @param d
	 *            d
	 * @param pt2
	 *            pt2
	 * @param pt3
	 *            pt3
	 * @return out
	 */
	private static double mlsadf2(double x, double b[], int m, double a, double d[], int pt2, int pt3[]) {
		double out = 0.0;
		// pt2 --> pt = &d1[pd * (m+2)]
		// pt3 --> pt = &d1[ 2*(pd+1) ]

		for (int i = PADEORDER; i > 0; i--) {
			int pt2_plus_i = pt2 + i;
			d[pt2_plus_i] = mlsafir(d[pt2_plus_i - 1], b, m, a, d, pt3[i]);
			double v = d[pt2_plus_i] * pade[ppade + i];

			x += ((1 & i) == 1) ? v : -v;
			/*
			 * if(i == 1 || i == 3 || i == 5) x += v; else x += -v;
			 */
			out += v;

		}
		d[pt2 /* +0 */] = x;
		out += x;

		return out;
	}

	/**
	 * mlsadf: HTS Mel Log Spectrum Approximation filter
	 * 
	 * @param x
	 *            x
	 * @param b
	 *            b
	 * @param m
	 *            m
	 * @param a
	 *            a
	 * @param d
	 *            d
	 * @param pt2
	 *            pt2
	 * @param pt3
	 *            pt3
	 * @return x
	 */
	public static double mlsadf(double x, double b[], int m, double a, double d[], int pt2, int pt3[]) {
		x = mlsadf1(x, b, m, a, d);
		x = mlsadf2(x, b, m - 1, a, d, pt2, pt3);

		return x;
	}

	/**
	 * uniform_rand: generate uniformly distributed random numbers 1 or -1
	 * 
	 * @return rand.nextboolean
	 */
	public double uniformRand() {
		return (rand.nextBoolean()) ? 1.0 : -1.0;
	}

	/**
	 * mc2b: transform mel-cepstrum to MLSA digital filter coefficients
	 * 
	 * @param mc
	 *            mc
	 * @param b
	 *            b
	 * @param m
	 *            m
	 * @param a
	 *            a
	 */
	public static void mc2b(double mc[], double b[], int m, double a) {
		b[m] = mc[m];
		for (m--; m >= 0; m--) {
			b[m] = mc[m] - a * b[m + 1];
		}
	}

	/**
	 * b2mc: transform MLSA digital filter coefficients to mel-cepstrum
	 * 
	 * @param b
	 *            b
	 * @param mc
	 *            mc
	 * @param m
	 *            m
	 * @param a
	 *            a
	 */
	public static void b2mc(double b[], double mc[], int m, double a) {
		double d = mc[m] = b[m];
		for (int i = m--; i >= 0; i--) {
			double o = b[i] + (a * d);
			d = b[i];
			mc[i] = o;
		}
	}

	/**
	 * freqt: frequency transformation
	 * 
	 * @param c1
	 *            c1
	 * @param m1
	 *            m1
	 * @param c2
	 *            c2
	 * @param m2
	 *            m2
	 * @param a
	 *            a
	 */
	public static void freqt(double c1[], int m1, double c2[], int m2, double a) {
		double b = 1 - a * a;

		double freqt_buff[] = new double[(m2 + m2 + 2)]; /* used in freqt */
		int g = m2 + 1; /* offset of freqt_buff */

		for (int i = -m1; i <= 0; i++) {
			if (0 <= m2)
				freqt_buff[g + 0] = c1[-i] + a * (freqt_buff[0] = freqt_buff[g + 0]);
			if (1 <= m2)
				freqt_buff[g + 1] = b * freqt_buff[0] + a * (freqt_buff[1] = freqt_buff[g + 1]);

			for (int j = 2; j <= m2; j++)
				freqt_buff[g + j] = freqt_buff[j - 1] + a * ((freqt_buff[j] = freqt_buff[g + j]) - freqt_buff[g + j - 1]);

		}

		/* move memory */
		System.arraycopy(freqt_buff, g, c2, 0, m2);

	}

	/**
	 * c2ir: The minimum phase impulse response is evaluated from the minimum phase cepstrum
	 * 
	 * @param c
	 *            c
	 * @param nc
	 *            nc
	 * @param hh
	 *            hh
	 * @param leng
	 *            leng
	 */
	public static void c2ir(double c[], int nc, double hh[], int leng) {
		hh[0] = Math.exp(c[0]);
		for (int n = 1; n < leng; n++) {
			double d = 0;
			int upl = (n >= nc) ? nc - 1 : n;
			for (int k = 1; k <= upl; k++)
				d += k * c[k] * hh[n - k];
			hh[n] = d / n;
		}
	}

	/**
	 * b2en: functions for postfiltering
	 * 
	 * @param b
	 *            b
	 * @param m
	 *            m
	 * @param a
	 *            a
	 * @return en
	 */
	public static double b2en(double b[], int m, double a) {
		double cep[], ir[];
		int arrayLength = (m + 1) + 2 * IRLENG;
		double[] spectrum2en_buff = new double[arrayLength];
		cep = new double[arrayLength]; /* CHECK! these sizes!!! */
		ir = new double[arrayLength];

		b2mc(b, spectrum2en_buff, m, a);
		/* freqt(vs->mc, m, vs->cep, vs->irleng - 1, -a); */
		freqt(spectrum2en_buff, m, cep, IRLENG - 1, -a);
		/* HTS_c2ir(vs->cep, vs->irleng, vs->ir, vs->irleng); */
		c2ir(cep, IRLENG, ir, IRLENG);
		double en = 0.0;

		for (int i = 0; i < IRLENG; i++)
			en += ir[i] * ir[i];

		return en;
	}

	/**
	 * ignorm: inverse gain normalization
	 * 
	 * @param c1
	 *            c1
	 * @param c2
	 *            c2
	 * @param m
	 *            m
	 * @param ng
	 *            ng
	 */
	public static void ignorm(double c1[], double c2[], int m, double ng) {
		if (ng != 0.0) {
			double k = Math.pow(c1[0], ng);
			for (int i = m; i >= 1; i--)
				c2[i] = k * c1[i];
			c2[0] = (k - 1.0) / ng;
		} else {
			/* movem */
			System.arraycopy(c1, 1, c2, 1, m - 1);
			c2[0] = Math.log(c1[0]);
		}
	}

	/**
	 * ignorm: gain normalization
	 * 
	 * @param c1
	 *            c1
	 * @param c2
	 *            c2
	 * @param m
	 *            m
	 * @param g
	 *            g
	 */
	public static void gnorm(double c1[], double c2[], int m, double g) {
		if (g != 0.0) {
			double k = 1.0 + g * c1[0];
			for (; m >= 1; m--)
				c2[m] = c1[m] / k;
			c2[0] = Math.pow(k, 1.0 / g);
		} else {
			/* movem */
			System.arraycopy(c1, 1, c2, 1, m - 1);
			c2[0] = Math.exp(c1[0]);
		}

	}

	/**
	 * lsp2lpc: transform LSP to LPC. lsp[1..m] &rarr; a=lpc[0..m] a[0]=1.0
	 * 
	 * @param lsp
	 *            lsp
	 * @param a
	 *            a
	 * @param m
	 *            m
	 */
	public static void lsp2lpc(double lsp[], double a[], int m) {
		int i, k, mh1, mh2, flag_odd;
		double xx, xf, xff;
		int p, q; /* offsets of lsp2lpc_buff */
		int a0, a1, a2, b0, b1, b2; /* offsets of lsp2lpc_buff */

		flag_odd = 0;
		if (m % 2 == 0)
			mh1 = mh2 = m / 2;
		else {
			mh1 = (m + 1) / 2;
			mh2 = (m - 1) / 2;
			flag_odd = 1;
		}

		double[] lsp2lpc_buff = new double[(5 * m + 6)];
		int lsp2lpc_size = m;

		/* offsets of lsp2lpcbuff */
		p = m;
		q = p + mh1;
		a0 = q + mh2;
		a1 = a0 + (mh1 + 1);
		a2 = a1 + (mh1 + 1);
		b0 = a2 + (mh1 + 1);
		b1 = b0 + (mh2 + 1);
		b2 = b1 + (mh2 + 1);

		/* move lsp -> lsp2lpc_buff */
		System.arraycopy(lsp, 1, lsp2lpc_buff, 0, m);

		for (i = 0; i < mh1 + 1; i++)
			lsp2lpc_buff[a0 + i] = 0.0;
		for (i = 0; i < mh1 + 1; i++)
			lsp2lpc_buff[a1 + i] = 0.0;
		for (i = 0; i < mh1 + 1; i++)
			lsp2lpc_buff[a2 + i] = 0.0;
		for (i = 0; i < mh2 + 1; i++)
			lsp2lpc_buff[b0 + i] = 0.0;
		for (i = 0; i < mh2 + 1; i++)
			lsp2lpc_buff[b1 + i] = 0.0;
		for (i = 0; i < mh2 + 1; i++)
			lsp2lpc_buff[b2 + i] = 0.0;

		/* lsp filter parameters */
		for (i = k = 0; i < mh1; i++, k += 2)
			lsp2lpc_buff[p + i] = -2.0 * Math.cos(lsp2lpc_buff[k]);
		for (i = k = 0; i < mh2; i++, k += 2)
			lsp2lpc_buff[q + i] = -2.0 * Math.cos(lsp2lpc_buff[k + 1]);

		/* impulse response of analysis filter */
		xx = 1.0;
		xf = xff = 0.0;

		for (k = 0; k <= m; k++) {
			if (flag_odd == 1) {
				lsp2lpc_buff[a0 + 0] = xx;
				lsp2lpc_buff[b0 + 0] = xx - xff;
				xff = xf;
				xf = xx;
			} else {
				lsp2lpc_buff[a0 + 0] = xx + xf;
				lsp2lpc_buff[b0 + 0] = xx - xf;
				xf = xx;
			}

			for (i = 0; i < mh1; i++) {
				lsp2lpc_buff[a0 + i + 1] = lsp2lpc_buff[a0 + i] + lsp2lpc_buff[p + i] * lsp2lpc_buff[a1 + i]
						+ lsp2lpc_buff[a2 + i];
				lsp2lpc_buff[a2 + i] = lsp2lpc_buff[a1 + i];
				lsp2lpc_buff[a1 + i] = lsp2lpc_buff[a0 + i];
			}

			for (i = 0; i < mh2; i++) {
				lsp2lpc_buff[b0 + i + 1] = lsp2lpc_buff[b0 + i] + lsp2lpc_buff[q + i] * lsp2lpc_buff[b1 + i]
						+ lsp2lpc_buff[b2 + i];
				lsp2lpc_buff[b2 + i] = lsp2lpc_buff[b1 + i];
				lsp2lpc_buff[b1 + i] = lsp2lpc_buff[b0 + i];
			}

			if (k != 0)
				a[k - 1] = -0.5 * (lsp2lpc_buff[a0 + mh1] + lsp2lpc_buff[b0 + mh2]);
			xx = 0.0;
		}

		for (i = m - 1; i >= 0; i--)
			a[i + 1] = -a[i];
		a[0] = 1.0;

	}

	/**
	 * gc2gc: generalized cepstral transformation
	 * 
	 * @param c1
	 *            c1
	 * @param m1
	 *            m1
	 * @param g1
	 *            g1
	 * @param c2
	 *            c2
	 * @param m2
	 *            m2
	 * @param g2
	 *            g2
	 */
	public static void gc2gc(double c1[], int m1, double g1, double c2[], int m2, double g2) {
		double[] gc2gc_buff = Arrays.copyOf(c1, m1 + 1);
		c2[0] = gc2gc_buff[0];

		for (int i = 1; i <= m2; i++) {
			double ss1 = 0.0;
			double ss2 = 0.0;
			int min = m1 < i ? m1 : i - 1;
			for (int k = 1; k <= min; k++) {
				int mk = i - k;
				double cc = gc2gc_buff[k] * c2[mk];
				ss2 += k * cc;
				ss1 += mk * cc;
			}

			if (i <= m1)
				c2[i] = gc2gc_buff[i] + (g2 * ss2 - g1 * ss1) / i;
			else
				c2[i] = (g2 * ss2 - g1 * ss1) / i;
		}
	}

	/**
	 * mgc2mgc: frequency and generalized cepstral transformation
	 * 
	 * @param c1
	 *            c1
	 * @param m1
	 *            m1
	 * @param a1
	 *            a1
	 * @param g1
	 *            g1
	 * @param c2
	 *            c2
	 * @param m2
	 *            m2
	 * @param a2
	 *            a2
	 * @param g2
	 *            g2
	 */
	public static void mgc2mgc(double c1[], int m1, double a1, double g1, double c2[], int m2, double a2, double g2) {

		if (a1 == a2) {
			gnorm(c1, c1, m1, g1);
			gc2gc(c1, m1, g1, c2, m2, g2);
			ignorm(c2, c2, m2, g2);
		} else {
			double a = (a2 - a1) / (1 - a1 * a2);
			freqt(c1, m1, c2, m2, a);
			gnorm(c2, c2, m2, g1);
			gc2gc(c2, m2, g1, c2, m2, g2);
			ignorm(c2, c2, m2, g2);

		}

	}

	/**
	 * lsp2mgc: transform LSP to MGC. lsp=C[0..m] mgc=C[0..m]
	 * 
	 * @param lsp
	 *            lsp
	 * @param mgc
	 *            mgc
	 * @param m
	 *            m
	 * @param alpha
	 *            alpha
	 */
	public void lsp2mgc(double lsp[], double mgc[], int m, double alpha) {
		/* lsp2lpc */
		lsp2lpc(lsp, mgc, m); /* lsp starts in 1! lsp[1..m] --> mgc[0..m] */
		if (use_log_gain)
			mgc[0] = Math.exp(lsp[0]);
		else
			mgc[0] = lsp[0];

		/* mgc2mgc */
		ignorm(mgc, mgc, m, gamma);
		for (int i = m; i >= 1; i--)
			mgc[i] *= -stage;
		mgc2mgc(mgc, m, alpha, gamma, mgc, m, alpha, gamma); /* input and output is in mgc=C */
	}

	/**
	 * mglsadff: sub functions for MGLSA filter
	 * 
	 * @param x
	 *            x
	 * @param b
	 *            b
	 * @param m
	 *            m
	 * @param a
	 *            a
	 * @param n
	 *            n
	 * @param d
	 *            d
	 * @return x
	 */
	public static double mglsadf(double x, double b[], int m, double a, int n, double d[]) {
		for (int i = 0; i < n; i++)
			x = mglsadff(x, b, m, a, d, (i * (m + 1)));

		return x;
	}

	/**
	 * mglsadf: sub functions for MGLSA filter
	 * 
	 * @param x
	 *            x
	 * @param b
	 *            b
	 * @param m
	 *            m
	 * @param a
	 *            a
	 * @param d
	 *            d
	 * @param d_offset
	 *            d_offset
	 * @return x
	 */
	private static double mglsadff(double x, double b[], int m, double a, double d[], int d_offset) {
		double y = d[d_offset + 0] * b[1];

		for (int i = 1; i < m; i++) {
			d[d_offset + i] += a * (d[d_offset + i + 1] - d[d_offset + i - 1]);
			y += d[d_offset + i] * b[i + 1];
		}
		x -= y;

		for (int i = m; i > 0; i--)
			d[d_offset + i] = d[d_offset + i - 1];
		d[d_offset + 0] = a * d[d_offset + 0] + (1 - a * a) * x;

		return x;
	}

	/**
	 * posfilter: postfilter for mel-cepstrum. It uses alpha and beta defined in HMMData
	 * 
	 * @param mgc
	 *            mgc
	 * @param m
	 *            m
	 * @param alpha
	 *            alpha
	 * @param beta
	 *            beta
	 */
	public static void postfilter_mgc(double mgc[], int m, double alpha, double beta) {
		if (beta > 0.0 && m > 1) {
			double[] postfilter_buff = new double[m + 1];
			mc2b(mgc, postfilter_buff, m, alpha);
			double e1 = b2en(postfilter_buff, m, alpha);

			postfilter_buff[1] -= beta * alpha * mgc[2];
			for (int k = 2; k < m; k++)
				postfilter_buff[k] *= (1.0 + beta);
			double e2 = b2en(postfilter_buff, m, alpha);
			postfilter_buff[0] += Math.log(e1 / e2) / 2;
			b2mc(postfilter_buff, mgc, m, alpha);

		}
	}

	public static double[] genPulseFromFourierMag(HTSPStream mag, int n, double f0) {
		return genPulseFromFourierMag(mag.getParVec(n), f0);
	}

	/**
	 * Generate one pitch period from Fourier magnitudes
	 * 
	 * @param mag
	 *            mag
	 * @param f0
	 *            f0
	 * @return pulse
	 */
	public static double[] genPulseFromFourierMag(double[] mag, double f0) {

		int numHarm = mag.length;
		int currentF0 = (int) Math.round(f0);
		int T;
		if (currentF0 < 512)
			T = 512;
		else
			T = 1024;
		int T2 = 2 * T;

		/* since is FFT2 no aperiodicFlag or jitter of 25% is applied */

		/* get the pulse */
		double[] pulse = new double[T];
		double[] real = new double[T2];
		double[] imag = new double[T2];

		/* copy Fourier magnitudes (Wai C. Chu "Speech Coding algorithms foundation and evolution of standardized coders" pg. 460) */
		real[0] = real[T] = 0.0; /* DC component set to zero */
		for (int i = 1; i <= numHarm; i++) {
			real[i] = real[T - i] = real[T + i] = real[T2 - i] = mag[i - 1]; /* Symetric extension */
			imag[i] = imag[T - i] = imag[T + i] = imag[T2 - i] = 0.0;
		}
		for (int i = (numHarm + 1); i < (T - numHarm); i++) { /* Default components set to 1.0 */
			real[i] = real[T - i] = real[T + i] = real[T2 - i] = 1.0;
			imag[i] = imag[T - i] = imag[T + i] = imag[T2 - i] = 0.0;
		}

		/* Calculate inverse Fourier transform */
		FFT.transform(real, imag, true);

		/* circular shift and normalise multiplying by sqrt(F0) */
		double sqrt_f0 = Math.sqrt(currentF0);
		for (int i = 0; i < T; i++)
			pulse[i] = real[(i - numHarm) % T] * sqrt_f0;

		return pulse;
	}

	private void circularShift(double y[], int T, int n) {

		double x[] = new double[T];
		for (int i = 0; i < T; i++)
			x[i] = y[modShift(i - n, T)];
		for (int i = 0; i < T; i++)
			y[i] = x[i];
	}

	private int modShift(int n, int N) {
		if (n < 0)
			while (n < 0)
				n = n + N;
		else
			while (n >= N)
				n = n - N;

		return n;
	}

	/**
	 * Stand alone testing reading parameters from files in SPTK format
	 * 
	 * @param args
	 *            args
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws Exception
	 *             Exception
	 */
	public static void main1(String[] args) throws IOException, InterruptedException, Exception {
		/* configure log info */
		// org.apache.log4j.BasicConfigurator.configure();

		HMMData htsData = new HMMData();
		HTSPStream lf0Pst, mcepPst, strPst, magPst;
		boolean[] voiced = null;
		LEDataInputStream lf0Data, mcepData, strData, magData;

		String lf0File, mcepFile, strFile, magFile, outFile, residualFile;
		String voiceName, voiceConfig, outDir, voiceExample, hmmTrainDir;

		String MaryBase = "/project/mary/marcela/openmary/";
		outDir = "/project/mary/marcela/openmary/tmp/";
		outFile = outDir + "tmp.wav";

		// Voice
		/*
		 * voiceName = "hsmm-slt"; voiceConfig = "en_US-hsmm-slt.config"; voiceExample = "cmu_us_arctic_slt_a0001"; hmmTrainDir =
		 * "/project/mary/marcela/HMM-voices/HTS-demo_CMU-ARCTIC-SLT/"; // The directory where the voice was trained
		 */
		voiceName = "hsmm-ot";
		voiceConfig = "tr-hsmm-ot.config";
		voiceExample = "ot0010";
		hmmTrainDir = "/project/mary/marcela/HMM-voices/turkish/"; // The directory where the voice was trained

		htsData.initHMMData(voiceName, MaryBase, voiceConfig);
		htsData.setUseMixExc(true);
		htsData.setUseFourierMag(true); /* use Fourier magnitudes for pulse generation */

		/* parameters extracted from real data with SPTK and snack */
		lf0File = hmmTrainDir + "data/lf0/" + voiceExample + ".lf0";
		mcepFile = hmmTrainDir + "data/mgc/" + voiceExample + ".mgc";
		strFile = hmmTrainDir + "data/str/" + voiceExample + ".str";
		magFile = hmmTrainDir + "data/mag/" + voiceExample + ".mag";

		int mcepVsize = htsData.getCartTreeSet().getMcepVsize();
		int strVsize = htsData.getCartTreeSet().getStrVsize();
		int lf0Vsize = htsData.getCartTreeSet().getLf0Stream();
		int magVsize = htsData.getCartTreeSet().getMagVsize();

		int totalFrame = 0;
		int lf0VoicedFrame = 0;
		float fval;
		int i, j;
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

		/* CHECK: I do not know why mcep has totalframe-2 frames less than lf0 and str ??? */
		totalFrame = totalFrame - 2;
		System.out.println("Total number of Frames = " + totalFrame);
		voiced = new boolean[totalFrame];

		/* Initialise HTSPStream-s */
		lf0Pst = new HTSPStream(lf0Vsize, totalFrame, HMMData.FeatureType.LF0, 0);
		mcepPst = new HTSPStream(mcepVsize, totalFrame, HMMData.FeatureType.MGC, 0);
		strPst = new HTSPStream(strVsize, totalFrame, HMMData.FeatureType.STR, 0);
		magPst = new HTSPStream(magVsize, totalFrame, HMMData.FeatureType.MAG, 0);

		/* load lf0 data */
		/* for lf0 i just need to load the voiced values */
		lf0VoicedFrame = 0;
		lf0Data = new LEDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
		for (i = 0; i < totalFrame; i++) {
			fval = lf0Data.readFloat();

			// lf0Pst.setPar(i, 0, fval);
			if (fval < 0)
				voiced[i] = false;
			else {
				voiced[i] = true;
				lf0Pst.setPar(lf0VoicedFrame, 0, fval);
				lf0VoicedFrame++;
			}
		}
		lf0Data.close();

		/* load mgc data */
		mcepData = new LEDataInputStream(new BufferedInputStream(new FileInputStream(mcepFile)));
		for (i = 0; i < totalFrame; i++) {
			for (j = 0; j < mcepPst.getOrder(); j++)
				mcepPst.setPar(i, j, mcepData.readFloat());
		}
		mcepData.close();

		/* load str data */
		strData = new LEDataInputStream(new BufferedInputStream(new FileInputStream(strFile)));
		for (i = 0; i < totalFrame; i++) {
			for (j = 0; j < strPst.getOrder(); j++)
				strPst.setPar(i, j, strData.readFloat());
		}
		strData.close();

		/* load mag data */
		magData = new LEDataInputStream(new BufferedInputStream(new FileInputStream(magFile)));
		for (i = 0; i < totalFrame; i++) {
			for (j = 0; j < magPst.getOrder(); j++)
				magPst.setPar(i, j, magData.readFloat());
			// System.out.println("i:" + i + "  f0=" + Math.exp(lf0Pst.getPar(i, 0)) + "  mag(1)=" + magPst.getPar(i, 0) +
			// "  str(1)=" + strPst.getPar(i, 0) );
		}
		magData.close();

		AudioFormat af = getHTSAudioFormat(htsData);
		double[] audio_double = null;

		HTSVocoder par2speech = new HTSVocoder();

		// par2speech.setUseLpcVocoder(true);

		audio_double = par2speech.htsMLSAVocoder(lf0Pst, mcepPst, strPst, magPst, voiced, htsData, null);
		// audio_double = par2speech.htsMLSAVocoder_residual(htsData, mcepPst, resFile);

		long lengthInSamples = (audio_double.length * 2) / (af.getSampleSizeInBits() / 8);
		par2speech.logger.debug("length in samples=" + lengthInSamples);

		/* Normalise the signal before return, this will normalise between 1 and -1 */
		double MaxSample = MathUtils.getAbsMax(audio_double);
		for (i = 0; i < audio_double.length; i++)
			audio_double[i] = 0.3 * (audio_double[i] / MaxSample);

		DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);

		File fileOut = new File(outFile);
		System.out.println("saving to file: " + outFile);

		if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, oais)) {
			AudioSystem.write(oais, AudioFileFormat.Type.WAVE, fileOut);
		}

		System.out.println("Calling audioplayer:");
		AudioPlayer player = new AudioPlayer(fileOut);
		player.start();
		player.join();
		System.out.println("audioplayer finished...");

	}

	/**
	 * Stand alone vocoder reading parameters from files in SPTK format, parameters in args[] array in the following order:
	 * <p>
	 * The type of spectrum parameters is set through the parameters gamma and alpha
	 * </p>
	 * 
	 * @param args
	 * 
	 *            <p>
	 *            example iput parameters:
	 *            <p>
	 *            0 0.45 0 0.0 16000 80 cmu_us_arctic_slt_a0001.mgc 75 cmu_us_arctic_slt_a0001.lf0 3 vocoder_out.wav
	 *            cmu_us_arctic_slt_a0001.str 15 mix_excitation_filters.txt 5 48 cmu_us_arctic_slt_a0001.mag 30
	 *            <p>
	 *            example input parameters without mixed excitation:
	 *            <p>
	 *            0 0.45 0 0.0 16000 80 cmu_us_arctic_slt_a0001.mgc 75 cmu_us_arctic_slt_a0001.lf0 3 vocoder_out.wav
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws Exception
	 *             Exception
	 * */
	public static void htsMLSAVocoderCommand(String[] args) throws IOException, InterruptedException, Exception {

		HMMData htsData = new HMMData();
		HTSPStream lf0Pst, mcepPst, strPst = null, magPst = null;
		boolean[] voiced = null;
		LEDataInputStream lf0Data, mcepData, strData, magData;

		String lf0File, mcepFile, strFile = "", magFile = "", outDir, outFile;
		int mcepVsize, lf0Vsize, strVsize = 0, magVsize = 0;
		// -----------------------------------
		// Values for FEMALE:
		// LOUD:
		float f0LoudFemale = 0.01313791f;
		float strLoudFemale[] = { -0.002995137f, -0.042511885f, 0.072285673f, 0.127030178f, 0.006603170f };
		float magLoudFemale[] = { 0.0417336550f, 0.0002531457f, -0.0436839922f, -0.0335192265f, -0.0217501786f, -0.0166272925f,
				-0.0424825309f, -0.0460119758f, -0.0307114900f, -0.0327369397f };
		float mcepLoudFemale[] = { -0.245401838f, -0.062825965f, -0.360973095f, 0.117120506f, 0.917223265f, 0.138920770f,
				0.338553265f, -0.004857140f, 0.285192007f, -0.358292740f, -0.062907335f, -0.008040502f, 0.029470562f,
				-0.485079992f, -0.006727651f, -1.313869583f, -0.353797651f, 0.797097747f, -0.164614609f, -0.311173881f,
				-0.205134527f, -0.478116992f, -0.311340181f, -1.485855332f, -0.045632626f };
		// SOFT:
		float f0SoftFemale = 0.3107256f;
		float strSoftFemale[] = { 0.22054621f, 0.11091616f, 0.06378487f, 0.02110654f, -0.05118725f };
		float magSoftFemale[] = { 0.5747024f, 0.3248238f, 0.2356782f, 0.2441387f, 0.2702851f, 0.2895966f, 0.2437654f, 0.2959747f,
				0.2910529f, 0.2508167f };
		float mcepSoftFemale[] = { -0.103318169f, 0.315698439f, 0.170000964f, 0.223589719f, 0.262139649f, -0.062646758f,
				-4.998160141f, 0.008026212f, 1.742740835f, 1.990719666f, 0.548177521f, 0.999093856f, 0.262868363f, 1.755019406f,
				0.330058590f, -5.241305159f, -0.021005177f, -5.890942393f, 0.344385084f, 0.242179454f, 0.200936671f,
				-1.630683357f, 0.110674201f, -53.525043676f, -0.223682764f };

		// -----------------------------------
		// Values for MALE:
		// LOUD:
		float f0LoudMale = -0.08453168f;
		float strLoudMale[] = { 0.07092900f, 0.41149292f, 0.24479925f, 0.01326785f, -0.01517731f };
		float magLoudMale[] = { -0.21923620f, -0.11031120f, -0.02786084f, -0.10640244f, -0.12020442f, -0.08508762f, -0.08171423f,
				-0.08000552f, -0.07291968f, -0.09478534f };
		float mcepLoudMale[] = { 0.15335238f, 0.30880292f, -0.22922052f, -0.01116095f, 1.04088351f, -0.31693632f, -19.36510752f,
				-0.12210441f, 0.81743415f, -0.19799409f, 0.44572112f, -0.24845725f, -1.39545409f, -0.88788491f, 8.83006358f,
				-1.26623882f, 0.52428102f, -1.02615700f, -0.28092043f, -0.82543015f, 0.33081815f, 0.39498874f, 0.20100945f,
				0.60890790f, -0.37892217f };
		// SOFT:
		float f0SoftMale = 0.05088677f;
		float strSoftMale[] = { 0.07595702f, 0.02348965f, -0.02038628f, -0.08572970f, -0.06090386f };
		float magSoftMale[] = { 0.08869109f, 0.05517088f, 0.08902098f, 0.09263865f, 0.04866824f, 0.04554406f, 0.04937004f,
				0.05082076f, 0.04988959f, 0.03459440f };
		float mcepSoftMale[] = { 0.098129393f, 0.124686819f, 0.195709008f, -0.007066379f, -1.795620578f, 0.089982916f,
				15.371711686f, -0.051023831f, -0.213521945f, 0.009725292f, 0.361488718f, 0.118609995f, 1.794143134f,
				0.100130942f, 0.005999542f, -0.593128934f, -0.165385304f, 0.101705681f, 0.175534153f, 0.049246302f, 0.009530379f,
				-0.272557042f, -0.043030771f, 0.158694874f, 0.099107970f };

		float f0Trans = 0f;
		float strTrans[] = null;
		float magTrans[] = null;
		float mcepTrans[] = null;

		// set values that the vocoder needs
		// Type of features:
		int ind = 0;
		htsData.setStage(Integer.parseInt(args[ind++])); // sets gamma
		htsData.setAlpha(Float.parseFloat(args[ind++])); // set alpha
		if (args[ind++].contentEquals("1"))
			htsData.setUseLogGain(true); // use log gain
		else
			htsData.setUseLogGain(false);
		htsData.setBeta(Float.parseFloat(args[ind++])); // set beta: for postfiltering
		htsData.setRate(Integer.parseInt(args[ind++])); // rate
		htsData.setFperiod(Integer.parseInt(args[ind++])); // period

		/* parameters extracted from real data with SPTK and snack */
		mcepFile = args[ind++];
		mcepVsize = Integer.parseInt(args[ind++]);

		lf0File = args[ind++];
		lf0Vsize = Integer.parseInt(args[ind++]);

		// output wav file
		outFile = args[ind++];

		// Optional:
		// if using mixed excitation
		if (args.length > (ind + 1)) {
			htsData.setUseMixExc(true);
			strFile = args[ind++];
			strVsize = Integer.parseInt(args[ind++]);
			FileInputStream mixedFiltersStream = new FileInputStream(args[ind++]);
			htsData.setNumFilters(Integer.parseInt(args[ind++]));
			htsData.readMixedExcitationFilters(mixedFiltersStream);
			htsData.setPdfStrStream(null);
		} else {
			htsData.setUseMixExc(false);
		}

		// Optional:
		// if using Fourier magnitudes in mixed excitation
		if (args.length > (ind + 1)) {
			htsData.setUseFourierMag(true);
			magFile = args[ind++];
			magVsize = Integer.parseInt(args[ind++]);
			htsData.setPdfMagStream(null);
		} else {
			htsData.setUseFourierMag(false);
		}

		// last argument true or false to play the file
		boolean play = Boolean.parseBoolean(args[ind++]);

		boolean trans = true;
		if (args[ind].contentEquals("loud")) {
			f0Trans = f0LoudFemale;
			strTrans = strLoudFemale;
			magTrans = magLoudFemale;
			mcepTrans = mcepLoudFemale;
			System.out.println("Generating loud voice");
		} else if (args[ind].contentEquals("soft")) {
			f0Trans = f0SoftFemale;
			strTrans = strSoftFemale;
			magTrans = magSoftFemale;
			mcepTrans = mcepSoftFemale;
			System.out.println("Generating soft voice");
		} else {
			trans = false;
			System.out.println("Generating modal voice");
		}

		// Change these for voice effects:
		// [min][max]
		htsData.setF0Std(1.0); // variable for f0 control, multiply f0 [1.0][0.0--5.0]
		htsData.setF0Mean(0.0); // variable for f0 control, add f0 [0.0][0.0--100.0]

		int totalFrame = 0;
		int lf0VoicedFrame = 0;
		float fval;
		int i, j;
		lf0Data = new LEDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));

		/* First i need to know the size of the vectors */
		File lf0 = new File(lf0File);
		long lengthLf0 = lf0.length(); // Get the number of bytes in the file
		lengthLf0 = lengthLf0 / ((lf0Vsize / 3) * 4); // 4 bytes per float

		File mcep = new File(mcepFile);
		long lengthMcep = mcep.length();
		lengthMcep = lengthMcep / ((mcepVsize / 3) * 4);
		int numSize = 2;
		long lengthStr;
		if (htsData.getUseMixExc()) {
			File str = new File(strFile);
			lengthStr = str.length();
			lengthStr = lengthStr / ((strVsize / 3) * 4);
			numSize++;
		} else
			lengthStr = 0;

		long lengthMag;
		if (htsData.getUseFourierMag()) {
			File mag = new File(magFile);
			lengthMag = mag.length();
			lengthMag = lengthMag / ((magVsize / 3) * 4);
			numSize++;
		} else
			lengthMag = 0;

		float sizes[] = new float[numSize];
		int n = 0;
		sizes[n++] = lengthMcep;
		sizes[n++] = lengthLf0;
		if (lengthStr > 0)
			sizes[n++] = lengthStr;
		if (lengthMag > 0)
			sizes[n++] = lengthMag;

		// choose the lowest
		// float sizes[] = {lengthLf0, lengthMcep, lengthStr, lengthMag};

		totalFrame = (int) MathUtils.getMin(sizes);
		System.out.println("Total number of Frames = " + totalFrame);
		voiced = new boolean[totalFrame];

		/* Initialise HTSPStream-s */
		lf0Pst = new HTSPStream(lf0Vsize, totalFrame, HMMData.FeatureType.LF0, 0);
		mcepPst = new HTSPStream(mcepVsize, totalFrame, HMMData.FeatureType.MGC, 0);

		/* load lf0 data */
		/* for lf0 i just need to load the voiced values */
		lf0VoicedFrame = 0;
		lf0Data = new LEDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
		for (i = 0; i < totalFrame; i++) {
			fval = lf0Data.readFloat();
			// lf0Pst.setPar(i, 0, fval);
			if (fval < 0)
				voiced[i] = false;
			else {
				voiced[i] = true;

				// apply here the change to loud
				if (trans) {
					fval = (float) Math.exp(fval);
					fval = fval + (fval * f0Trans);
					fval = (float) Math.log(fval);
				}
				lf0Pst.setPar(lf0VoicedFrame, 0, fval);
				lf0VoicedFrame++;
			}
		}
		lf0Data.close();

		/* load mgc data */
		mcepData = new LEDataInputStream(new BufferedInputStream(new FileInputStream(mcepFile)));
		for (i = 0; i < totalFrame; i++) {
			for (j = 0; j < mcepPst.getOrder(); j++) {
				// apply here the change to loud
				fval = mcepData.readFloat();
				if (trans & j < 4)
					fval = fval + (fval * mcepTrans[j]);
				mcepPst.setPar(i, j, fval);
			}
		}
		mcepData.close();

		/* load str data */
		if (htsData.getUseMixExc()) {
			strPst = new HTSPStream(strVsize, totalFrame, HMMData.FeatureType.STR, 0);
			strData = new LEDataInputStream(new BufferedInputStream(new FileInputStream(strFile)));
			for (i = 0; i < totalFrame; i++) {
				for (j = 0; j < strPst.getOrder(); j++) {
					// apply here the change to loud/soft
					fval = strData.readFloat();
					if (trans)
						fval = fval + (fval * strTrans[j]);
					strPst.setPar(i, j, fval);
				}
			}
			strData.close();
		}

		/* load mag data */
		n = 0;
		if (htsData.getUseFourierMag()) {
			magPst = new HTSPStream(magVsize, totalFrame, HMMData.FeatureType.MAG, 0);
			magData = new LEDataInputStream(new BufferedInputStream(new FileInputStream(magFile)));
			for (i = 0; i < totalFrame; i++) {
				// System.out.print(n + " : ");
				for (j = 0; j < magPst.getOrder(); j++) {
					n++;
					fval = magData.readFloat();
					if (trans)
						fval = fval + (fval * magTrans[j]);
					magPst.setPar(i, j, fval);
					// System.out.format("mag(%d,%d)=%.2f ",i, j, magPst.getPar(i, j) );
				}
				// System.out.println();
			}
			magData.close();
		}

		AudioFormat af = getHTSAudioFormat(htsData);
		double[] audio_double = null;

		HTSVocoder par2speech = new HTSVocoder();

		// par2speech.setUseLpcVocoder(true);
		// audio_double = par2speech.htsMLSAVocoder_residual(htsData, mcepPst, resFile);

		audio_double = par2speech.htsMLSAVocoder(lf0Pst, mcepPst, strPst, magPst, voiced, htsData, null);

		long lengthInSamples = (audio_double.length * 2) / (af.getSampleSizeInBits() / 8);
		logger.debug("length in samples=" + lengthInSamples);

		/* Normalise the signal before return, this will normalise between 1 and -1 */
		double MaxSample = MathUtils.getAbsMax(audio_double);
		for (i = 0; i < audio_double.length; i++)
			audio_double[i] = (audio_double[i] / MaxSample);

		DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);

		File fileOut = new File(outFile);
		System.out.println("saving to file: " + outFile);

		if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, oais)) {
			AudioSystem.write(oais, AudioFileFormat.Type.WAVE, fileOut);
		}

		if (play) {
			System.out.println("Calling audioplayer:");
			AudioPlayer player = new AudioPlayer(fileOut);
			player.start();
			player.join();
			System.out.println("audioplayer finished...");
		}

	}

	public static void main(String[] args) throws IOException, InterruptedException, Exception {
		/* configure log info */
		org.apache.log4j.BasicConfigurator.configure();

		// copy synthesis: requires a hmm voice
		// main1(args);

		// copy synthesis: requires parameters, see description
		// example of parameters:
		/*
		 * 0 0.45 0 16000 80 /project/mary/marcela/HMM-voices/roger/hts/data/mgc/roger_5739.mgc 75
		 * /project/mary/marcela/HMM-voices/roger/hts/data/lf0/roger_5739.lf0 3
		 * /project/mary/marcela/HMM-voices/roger/vocoder_out.wav
		 * /project/mary/marcela/HMM-voices/roger/hts/data/str/roger_5739.str 15
		 * /project/mary/marcela/HMM-voices/roger/hts/data/filters/mix_excitation_filters.txt 5 48
		 * /project/mary/marcela/HMM-voices/roger/hts/data/mag/roger_5739.mag 30
		 * 
		 * example input parameters without mixed excitation: 0 0.45 0 16000 80
		 * /project/mary/marcela/HMM-voices/roger/hts/data/mgc/roger_5739.mgc 75
		 * /project/mary/marcela/HMM-voices/roger/hts/data/lf0/roger_5739.lf0 3
		 * /project/mary/marcela/HMM-voices/roger/vocoder_out1.wav
		 */

		/*
		 * String topic = "pru013"; String path = "/project/mary/marcela/HMM-voices/prudence/hts/data/"; // with mixed excitation
		 * String args1[] = {"0", "0.45", "0", "16000", "80", path + "mgc/" + topic + ".mgc", "75", path + "lf0/" + topic +
		 * ".lf0", "3", path + "vocoder/" + topic + ".wav", path + "str/" + topic + ".str", "15", path +
		 * "filters/mix_excitation_filters.txt", "5", "48", path + "mag/" + topic + ".mag", "30", "true"};
		 * 
		 * // without mixed excitation String args2[] = {"0", "0.45", "0", "16000", "80", path + "mgc/" + topic + ".mgc", "75",
		 * path + "lf0/" + topic + ".lf0", "3", path + "/" + topic + ".wav", "true"};
		 * 
		 * HTSVocoder vocoder = new HTSVocoder(); vocoder.htsMLSAVocoderCommand(args2);
		 */

		/*
		 * String path = "/project/mary/marcela/HMM-voices/BITS/bits1/hts/data/"; String args3[] = {"0", "0.42", "0.05", "0.3",
		 * "16000", "80", path + "mgc/US10010046_0.mgc", "75", path + "lf0-100-270/US10010046_0.lf0", "3", path +
		 * "vocoder_out-100-270.wav", path + "str-100-270/US10010046_0.str", "15", path + "filters/mix_excitation_filters.txt",
		 * "5", "true"}; HTSVocoder vocoder = new HTSVocoder(); vocoder.htsMLSAVocoderCommand(args3);
		 */
		/*
		 * String path = "/project/mary/marcela/quality_parameters/necadbs/hts/data/"; String args3[] = {"0", "0.42", "0.05",
		 * "0.15", "16000", "80", path + "mgc/modal0001.mgc", "75", path + "lf0/modal0001.lf0", "3", path +
		 * "vocoder_out-modal-soft.wav", path + "str/soft0001.str", "15", path + "filters/mix_excitation_filters.txt", "5",
		 * "true"}; HTSVocoder vocoder = new HTSVocoder(); vocoder.htsMLSAVocoderCommand(args3);
		 */

		/*
		 * String path = "/project/mary/marcela/HMM-voices/arctic_slt/hts/data/"; String fileName = "modal0002"; //String fileName
		 * = "de_0001"; String args4[] = {"0", "0.42", "0.05", "0.25", "16000", "80", path + "mgc/" + fileName + ".mgc", "75",
		 * path + "lf0/" + fileName + ".lf0", "3", path + "vocoder/" + fileName + "_vocoder_soft.wav", path + "str/" + fileName +
		 * ".str", "15", path + "filters/mix_excitation_filters.txt", "5", path + "mag/" + fileName + ".mag", "30", "true",
		 * "soft"}; HTSVocoder vocoder = new HTSVocoder(); vocoder.htsMLSAVocoderCommand(args4);
		 */

		/* Use this for running HTSVocoder for a list, see vocoderList for the parameters */

		/*
		 * HTSVocoder vocoder = new HTSVocoder(); vocoder.vocoderList(args);
		 */

	}

	public static void vocoderList(String[] args) throws IOException, InterruptedException, Exception {

		// String path = "/project/mary/marcela/HMM-voices/SEMAINE/prudence/hts/data/";
		// String path = "/project/mary/marcela/HMM-voices/arctic_test/hts/data/";
		// String path = "/project/mary/marcela/HMM-voices/SEMAINE/spike/hts/data/";
		// String path = "/project/mary/marcela/HMM-voices/arctic_slt/hts/data/";
		// String path = "/project/mary/marcela/HMM-voices/BITS/bits1/hts/data/";
		String path = "/project/mary/marcela/quality_parameters/necadbs/hts/data/";

		File outDir = new File(path + "vocoder");
		if (!outDir.exists())
			outDir.mkdir();
		File directory = new File(path + "raw");
		String files[] = FileUtils.listBasenames(directory, ".raw");

		// the output will be in path/vocoder directory, it has to be created beforehand

		for (int i = 0; i < files.length; i++) {

			System.out.println("file: " + files[i]);

			// MGC stage=0.0 alpha=0.42 logGain=0 (false)
			// MGC-LSP stage=3.0 alpha=0.42 loggain=1 (true)

			/*
			 * String args1[] = {"0", "0.42", "0", "0.15", "16000", "80", path + "mgc/" + files[i] + ".mgc", "75", path + "lf0/" +
			 * files[i] + ".lf0", "3", path + "vocoder/" + files[i] + ".wav", path + "str/" + files[i] + ".str", "15", path +
			 * "filters/mix_excitation_filters.txt", "5", path + "mag/" + files[i] + ".mag", "30", "true"}; // the last true/false
			 * is for playing or not the generated file
			 */

			// without Fourier magnitudes
			String args1[] = { "0", "0.42", "0.05", "0.15", "16000", "80", path + "mgc/" + files[i] + ".mgc", "75",
					path + "lf0/" + files[i] + ".lf0", "3", path + "vocoder/" + files[i] + ".wav",
					path + "str/" + files[i] + ".str", "15", path + "filters/mix_excitation_filters.txt", "5", "true" }; // the
																															// last
																															// true/false
																															// is
																															// for
																															// playing
																															// or
																															// not
																															// the
																															// generated
																															// file

			// without Mixed excitation and Fourier magnitudes
			/*
			 * String args1[] = {"0", "0.42", "0", "0.0", "16000", "80", path + "mgc/" + files[i] + ".mgc", "75", path + "lf0/" +
			 * files[i] + ".lf0", "3", path + "vocoder/" + files[i] + ".wav", "true"}; // the last true/false is for playing or
			 * not the generated file
			 */
			htsMLSAVocoderCommand(args1);

		}

	}

	protected class HTSVocoderDataProducer extends ProducingDoubleDataSource {
		private static final double INITIAL_MAX_AMPLITUDE = 17000.;

		// Values used by the synthesis thread
		private HTSPStream lf0Pst;
		private HTSPStream mcepPst;
		private HTSPStream strPst;
		private HTSPStream magPst;
		private boolean[] voiced;
		private HMMData htsData;

		public HTSVocoderDataProducer(int audioSize, HTSParameterGeneration pdf2par, HMMData htsData) {
			super(audioSize, new AmplitudeNormalizer(INITIAL_MAX_AMPLITUDE));
			lf0Pst = pdf2par.getlf0Pst();
			mcepPst = pdf2par.getMcepPst();
			strPst = pdf2par.getStrPst();
			magPst = pdf2par.getMagPst();
			voiced = pdf2par.getVoicedArray();
			this.htsData = htsData;

		}

		public void run() {
			try {
				htsMLSAVocoder(lf0Pst, mcepPst, strPst, magPst, voiced, htsData, this);
				putEndOfStream();
			} catch (Exception e) {
				logger.error("Cannot vocode", e);
			}
		}

	}

} /* class HTSVocoder */
