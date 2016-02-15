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
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.DecisionNode.BinaryByteDecisionNode;
import marytts.features.FeatureDefinition;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * Set of Global Mean and (diagonal) Variance for log f0, mel-cepstrum, bandpass voicing strengths and Fourier magnitudes (
 * 
 * Java port and extension of HTS engine API version 1.04 Extension: mixed excitation
 * 
 * @author Marcela Charfuelan
 */
public class GVModelSet {

	/**
	 * ____________________ GV related variables ____________________ GV: Global mean and covariance (diagonal covariance only) it
	 * should be inverse for GV method Gradient weight by default is 1.0 but it can be between 0.0-2.0
	 */
	private double gvmeanMgc[];
	private double gvcovInvMgc[];

	private double gvmeanLf0[];
	private double gvcovInvLf0[];

	private double gvmeanStr[];
	private double gvcovInvStr[];

	private double gvmeanMag[];
	private double gvcovInvMag[];

	private Logger logger = MaryUtils.getLogger("GVModelSet");

	public double[] getGVmeanMgc() {
		return gvmeanMgc;
	}

	public double[] getGVcovInvMgc() {
		return gvcovInvMgc;
	}

	public double[] getGVmeanLf0() {
		return gvmeanLf0;
	}

	public double[] getGVcovInvLf0() {
		return gvcovInvLf0;
	}

	public double[] getGVmeanStr() {
		return gvmeanStr;
	}

	public double[] getGVcovInvStr() {
		return gvcovInvStr;
	}

	public double[] getGVmeanMag() {
		return gvmeanMag;
	}

	public double[] getGVcovInvMag() {
		return gvcovInvMag;
	}

	public void loadGVModelSet(HMMData htsData, FeatureDefinition featureDef) throws IOException {

		/* allocate memory for the arrays and load the data from file */
		int numMSDFlag, numStream, vectorSize, numDurPdf;
		double gvcov;
		DataInputStream data_in;
		InputStream gvStream;

		/* Here global variance vectors are loaded from corresponding files */
		int m, i, nmix;
		if (htsData.getUseGV()) {
			// GV for Mgc
			if ((gvStream = htsData.getPdfMgcGVStream()) != null)
				loadGvFromFile(gvStream, "mgc", htsData.getGvMethodGradient(), htsData.getGvWeightMgc());

			// GV for Lf0
			if ((gvStream = htsData.getPdfLf0GVStream()) != null)
				loadGvFromFile(gvStream, "lf0", htsData.getGvMethodGradient(), htsData.getGvWeightLf0());

			// GV for Str
			if ((gvStream = htsData.getPdfStrGVStream()) != null)
				loadGvFromFile(gvStream, "str", htsData.getGvMethodGradient(), htsData.getGvWeightStr());

			// GV for Mag
			if ((gvStream = htsData.getPdfMagGVStream()) != null)
				loadGvFromFile(gvStream, "mag", htsData.getGvMethodGradient(), htsData.getGvWeightMag());

			// gv-switch
			// if( (gvFile=htsData.getSwitchGVFile()) != null)
			// loadSwitchGvFromFile(gvFile, featureDef, trickyPhones);

		}

	}

	private void loadGvFromFile(InputStream gvStream, String par, boolean gradientMethod, double gvWeight) throws IOException {

		int numMSDFlag, numStream, vectorSize, numDurPdf;
		DataInputStream data_in;
		int m, i;

		data_in = new DataInputStream(new BufferedInputStream(gvStream));
		logger.debug("LoadGVModelSet reading model of type '" + par + "' with gvWeight = " + gvWeight);

		numMSDFlag = data_in.readInt();
		numStream = data_in.readInt();
		vectorSize = data_in.readInt();
		numDurPdf = data_in.readInt();

		if (par.contentEquals("mgc")) {
			gvmeanMgc = new double[vectorSize];
			gvcovInvMgc = new double[vectorSize];
			readBinaryFile(data_in, gvmeanMgc, gvcovInvMgc, vectorSize, gradientMethod, gvWeight);
		} else if (par.contentEquals("lf0")) {
			gvmeanLf0 = new double[vectorSize];
			gvcovInvLf0 = new double[vectorSize];
			readBinaryFile(data_in, gvmeanLf0, gvcovInvLf0, vectorSize, gradientMethod, gvWeight);
		} else if (par.contentEquals("str")) {
			gvmeanStr = new double[vectorSize];
			gvcovInvStr = new double[vectorSize];
			readBinaryFile(data_in, gvmeanStr, gvcovInvStr, vectorSize, gradientMethod, gvWeight);
		} else if (par.contentEquals("mag")) {
			gvmeanMag = new double[vectorSize];
			gvcovInvMag = new double[vectorSize];
			readBinaryFile(data_in, gvmeanMag, gvcovInvMag, vectorSize, gradientMethod, gvWeight);
		}
		data_in.close();
	}

	private void readBinaryFile(DataInputStream data_in, double mean[], double ivar[], int vectorSize, boolean gradientMethod,
			double gvWeight) throws IOException {
		int i;
		double var;
		if (gradientMethod) {
			for (i = 0; i < vectorSize; i++) {
				mean[i] = data_in.readFloat() * gvWeight;
				var = data_in.readFloat();
				assert var > 0.0;
				ivar[i] = 1.0 / var;
			}
		} else {
			for (i = 0; i < vectorSize; i++) {
				mean[i] = data_in.readFloat() * gvWeight;
				ivar[i] = data_in.readFloat();
			}
		}
	}

	public void loadSwitchGvFromFile(String gvFile, FeatureDefinition featDef, PhoneTranslator trickyPhones) throws Exception {

		// featDef = featDefinition;
		// phTrans = phoneTranslator;
		PhoneTranslator phTrans = trickyPhones;

		int i, j, length, state, feaIndex;
		BufferedReader s = null;
		String line, buf, aux;
		StringTokenizer sline;
		// phTrans = phTranslator;

		assert featDef != null : "Feature Definition was not set";

		try {
			/* read lines of tree-*.inf fileName */
			s = new BufferedReader(new InputStreamReader(new FileInputStream(gvFile)));
			logger.info("load: reading " + gvFile);

			// skip questions section
			while ((line = s.readLine()) != null) {
				if (line.indexOf("QS") < 0)
					break; /* a new state is indicated by {*}[2], {*}[3], ... */
			}

			while ((line = s.readLine()) != null) {
				if (line.indexOf("{*}") >= 0) { /* this is the indicator of a new state-tree */
					aux = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
					state = Integer.parseInt(aux);

					sline = new StringTokenizer(aux);

					/* 1: gets index node and looks for the node whose idx = buf */
					buf = sline.nextToken();

					/* 2: gets question name and question name val */
					buf = sline.nextToken();
					String[] fea_val = buf.split("="); /* splits featureName=featureValue */
					feaIndex = featDef.getFeatureIndex(fea_val[0]);

					/* Replace back punctuation values */
					/* what about tricky phones, if using halfphones it would not be necessary */
					if (fea_val[0].contentEquals("sentence_punc") || fea_val[0].contentEquals("prev_punctuation")
							|| fea_val[0].contentEquals("next_punctuation")) {
						// System.out.print("CART replace punc: " + fea_val[0] + " = " + fea_val[1]);
						fea_val[1] = phTrans.replaceBackPunc(fea_val[1]);
						// System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
					} else if (fea_val[0].contains("tobi_")) {
						// System.out.print("CART replace tobi: " + fea_val[0] + " = " + fea_val[1]);
						fea_val[1] = phTrans.replaceBackToBI(fea_val[1]);
						// System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
					} else if (fea_val[0].contains("phone")) {
						// System.out.print("CART replace phone: " + fea_val[0] + " = " + fea_val[1]);
						fea_val[1] = phTrans.replaceBackTrickyPhones(fea_val[1]);
						// System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
					}

					// add featureName and featureValue to the switch off gv phones

				}
			} /* while */
			if (s != null)
				s.close();

		} catch (FileNotFoundException e) {
			logger.debug("FileNotFoundException: " + e.getMessage());
			throw new FileNotFoundException("LoadTreeSet: " + e.getMessage());
		}

	}
}
