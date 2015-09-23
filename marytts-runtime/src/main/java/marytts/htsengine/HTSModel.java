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

import java.util.Arrays;
import marytts.htsengine.HMMData.FeatureType;

/**
 * HMM model for a particular phone (or line in context feature file) This model is the unit when building a utterance model
 * sequence. For every phone (or line)in the context feature file, one of these models is created.
 * 
 * Java port and extension of HTS engine API version 1.04 Extension: mixed excitation
 * 
 * @author Marcela Charfuelan
 */
public class HTSModel {

	// private String name; /* the name of this HMM, it includes ph(-2)^ph(-1)-ph(0)+ph(1)=ph(2) + context features */
	private String phoneName; /* the name of the phone corresponding to this model, ph(0) in name */

	private double durError;
	private int dur[]; /* duration for each state of this HMM */
	private int totalDur; /* total duration of this HMM in frames */
	private int totalDurMillisec; /* total duration of this model in milliseconds */
	private double lf0Mean[][]; /* mean vector of log f0 pdfs for each state of this HMM */
	private double lf0Variance[][]; /* variance (diag) elements of log f0 for each state of this HMM */
	private double mcepMean[][]; /* mean vector of mel-cepstrum pdfs for each state of this HMM */
	private double mcepVariance[][]; /* variance (diag) elements of mel-cepstrum for each state of this HMM */

	private double strMean[][]; /* mean vector of strengths pdfs for each state of this HMM */
	private double strVariance[][]; /* variance (diag) elements of strengths for each state of this HMM */
	private double magMean[][]; /* mean vector of fourier magnitude pdfs for each state of this HMM */
	private double magVariance[][]; /* variance (diag) elements of fourier magnitudes for each state of this HMM */

	private boolean voiced[]; /* voiced/unvoiced decision for each state of this HMM */

	private String maryXmlDur; /* duration in maryXML input acoustparams, format d="val" in millisec. */
	private String maryXmlF0; /*
							 * F0 values in maryXML input acoustparams, format f0="(1,val1)...(100,val2)" (%pos in total duration,
							 * f0 Hz)
							 */

	private boolean gvSwitch; /* GV switch, applies to all the states of this model */

	public void setPhoneName(String var) {
		phoneName = var;
	}

	public String getPhoneName() {
		return phoneName;
	}

	public void setDur(int i, int val) {
		dur[i] = val;
	}

	public int getDur(int i) {
		return dur[i];
	}

	public void setDurError(double e) {
		durError = e;
	}

	public double getDurError() {
		return durError;
	}

	public void setTotalDur(int val) {
		totalDur = val;
	}

	public int getTotalDur() {
		return totalDur;
	}

	public void incrTotalDur(int val) {
		totalDur += val;
	}

	public void setTotalDurMillisec(int val) {
		totalDurMillisec = val;
	}

	public int getTotalDurMillisec() {
		return totalDurMillisec;
	}

	public void setLf0Mean(int i, int j, double val) {
		lf0Mean[i][j] = val;
	}

	public double getLf0Mean(int i, int j) {
		return lf0Mean[i][j];
	}

	public void setLf0Variance(int i, int j, double val) {
		lf0Variance[i][j] = val;
	}

	public double getLf0Variance(int i, int j) {
		return lf0Variance[i][j];
	}

	// set the vector per state
	public void setLf0Mean(int i, double val[]) {
		lf0Mean[i] = val;
	}

	public void setLf0Variance(int i, double val[]) {
		lf0Variance[i] = val;
	}

	public void setMcepMean(int i, int j, double val) {
		mcepMean[i][j] = val;
	}

	public double getMcepMean(int i, int j) {
		return mcepMean[i][j];
	}

	public void setMcepVariance(int i, int j, double val) {
		mcepVariance[i][j] = val;
	}

	public double getMcepVariance(int i, int j) {
		return mcepVariance[i][j];
	}

	// set the vector per state
	public void setMcepMean(int i, double val[]) {
		mcepMean[i] = val;
	}

	public void setMcepVariance(int i, double val[]) {
		mcepVariance[i] = val;
	}

	public double[] getMean(FeatureType type, int i) {
		switch (type) {
		case MGC:
			return Arrays.copyOf(mcepMean[i], mcepMean[i].length);
		case STR:
			return Arrays.copyOf(strMean[i], strMean[i].length);
		case MAG:
			return Arrays.copyOf(magMean[i], magMean[i].length);
		case LF0:
			return Arrays.copyOf(lf0Mean[i], lf0Mean[i].length);
		default:
			throw new RuntimeException("You must not ask me about DUR");
		}
	}

	public double[] getVariance(FeatureType type, int i) {
		switch (type) {
		case MGC:
			return Arrays.copyOf(mcepVariance[i], mcepVariance[i].length);
		case STR:
			return Arrays.copyOf(strVariance[i], strVariance[i].length);
		case MAG:
			return Arrays.copyOf(magVariance[i], magVariance[i].length);
		case LF0:
			return Arrays.copyOf(lf0Variance[i], lf0Variance[i].length);
		default:
			throw new RuntimeException("You must not ask me about DUR");
		}
	}

	/**
	 * Print mean and variance of each state
	 */
	public void printMcepMean() {
		printVectors(mcepMean, mcepVariance);
	}

	/**
	 * Print mean and variance of each state
	 */
	public void printLf0Mean() {
		printVectors(lf0Mean, lf0Variance);
	}

	/**
	 * Print mean and variance vectors
	 * 
	 * @param m
	 *            m
	 * @param v
	 *            v
	 */
	public void printVectors(double m[][], double v[][]) {
		for (int i = 0; i < v.length; i++) {
			System.out.print("  mean[" + i + "]: ");
			for (int j = 0; j < m[i].length; j++)
				System.out.format("%.6f ", m[i][j]);
			System.out.print("\n  vari[" + i + "]: ");
			for (int j = 0; j < v[i].length; j++)
				System.out.format("%.6f ", v[i][j]);
			System.out.println();
		}
	}

	public void printDuration(int numStates) {
		System.out.print("phoneName: " + phoneName + "\t");
		for (int i = 0; i < numStates; i++)
			System.out.print("dur[" + i + "]=" + dur[i] + " ");
		System.out.println("  totalDur=" + totalDur + "  totalDurMillisec=" + totalDurMillisec);
	}

	/**
	 * NOT USED -- remove? public String getShortPhoneName(){ String aux; int l,r; l = name.indexOf("-"); r = name.indexOf("+");
	 * aux = name.substring(l+1, r);
	 * 
	 * return aux;
	 * 
	 * }
	 * 
	 * @param i
	 *            i
	 * @param j
	 *            j
	 * @param val
	 *            val
	 */

	public void setStrMean(int i, int j, double val) {
		strMean[i][j] = val;
	}

	public double getStrMean(int i, int j) {
		return strMean[i][j];
	}

	public void setStrVariance(int i, int j, double val) {
		strVariance[i][j] = val;
	}

	public double getStrVariance(int i, int j) {
		return strVariance[i][j];
	}

	// set the vector per state
	public void setStrMean(int i, double val[]) {
		strMean[i] = val;
	}

	public void setStrVariance(int i, double val[]) {
		strVariance[i] = val;
	}

	public void setMagMean(int i, int j, double val) {
		magMean[i][j] = val;
	}

	public double getMagMean(int i, int j) {
		return magMean[i][j];
	}

	public void setMagVariance(int i, int j, double val) {
		magVariance[i][j] = val;
	}

	public double getMagVariance(int i, int j) {
		return magVariance[i][j];
	}

	// set the vector per state
	public void setMagMean(int i, double val[]) {
		magMean[i] = val;
	}

	public void setMagVariance(int i, double val[]) {
		magVariance[i] = val;
	}

	public void setVoiced(int i, boolean val) {
		voiced[i] = val;
	}

	/**
	 * whether state i is voiced or not
	 * 
	 * @param i
	 *            i
	 * @return voiced[i]
	 */
	public boolean getVoiced(int i) {
		return voiced[i];
	}

	public int getNumVoiced() {
		int numVoiced = 0;
		for (int i = 0; i < voiced.length; i++) {
			if (getVoiced(i))
				numVoiced += getDur(i);
		}
		return numVoiced;
	}

	public void setMaryXmlDur(String str) {
		maryXmlDur = str;
	}

	public String getMaryXmlDur() {
		return maryXmlDur;
	}

	public void setMaryXmlF0(String str) {
		maryXmlF0 = str;
	}

	public String getMaryXmlF0() {
		return maryXmlF0;
	}

	public void setGvSwitch(boolean bv) {
		gvSwitch = bv;
	}

	public boolean getGvSwitch() {
		return gvSwitch;
	}

	/* Constructor */
	/* Every Model is initialised with the information in ModelSet */
	public HTSModel(int nstate) {
		int i;
		totalDur = 0;
		dur = new int[nstate];
		lf0Mean = new double[nstate][];
		lf0Variance = new double[nstate][];
		voiced = new boolean[nstate];

		mcepMean = new double[nstate][];
		mcepVariance = new double[nstate][];

		strMean = new double[nstate][];
		strVariance = new double[nstate][];

		magMean = new double[nstate][];
		magVariance = new double[nstate][];

		maryXmlDur = null;
		maryXmlF0 = null;

		gvSwitch = true;

	} /* method Model, initialise a Model object */

	@Override
	public String toString() {
		return getPhoneName();
	}

} /* class Model */
