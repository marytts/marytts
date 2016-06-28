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
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.htsengine.HMMData.FeatureType;
import marytts.signalproc.analysis.Mfccs;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.MaryUtils;
import marytts.util.io.LEDataInputStream;

import org.apache.log4j.Logger;

/**
 * Parameter generation out of trained HMMs.
 *
 * Java port and extension of HTS engine API version 1.04 Extension: mixed excitation
 *
 * @author Marcela Charfuelan
 */
public class HTSParameterGeneration {

	public static final double INFTY = ((double) 1.0e+38);
	public static final double INFTY2 = ((double) 1.0e+19);
	public static final double INVINF = ((double) 1.0e-38);
	public static final double INVINF2 = ((double) 1.0e-19);

	private HTSPStream mcepPst = null;
	private HTSPStream strPst = null;
	private HTSPStream magPst = null;
	private HTSPStream lf0Pst = null;
	private boolean voiced[];
	private int totalUttFrame; // total number of frames in a mcep, str or mag Pst
	private int totalLf0Frame; // total number of f0 voiced frames in a lf0 Pst

	private Logger logger = MaryUtils.getLogger("ParameterGeneration");

	public HTSPStream getMcepPst() {
		return mcepPst;
	}

	public void setMcepPst(HTSPStream var) {
		mcepPst = var;
	};

	public HTSPStream getStrPst() {
		return strPst;
	}

	public void setStrPst(HTSPStream var) {
		strPst = var;
	};

	public HTSPStream getMagPst() {
		return magPst;
	}

	public void setMagPst(HTSPStream var) {
		magPst = var;
	};

	public HTSPStream getlf0Pst() {
		return lf0Pst;
	}

	public void setlf0Pst(HTSPStream var) {
		lf0Pst = var;
	};

	public boolean[] getVoicedArray() {
		return voiced;
	}

	public void setVoicedArray(boolean[] var) {
		voiced = var;
	} // only used in HTSEngineTest

	/* Inverse of a given double */
	/* We actually need the inverse of the matrix of covariance, but since this matrix */
	/* is a diagonal matrix, then we just need to calculate the inverse of each of the */
	/* numbers in the diagonal. */
	static public double finv(double x) {

		if (x >= INFTY2)
			return 0.0;
		if (x <= -INFTY2)
			return 0.0;
		if (x <= INVINF2 && x >= 0)
			return INFTY;
		if (x >= -INVINF2 && x < 0)
			return -INFTY;

		return 1.0 / x;

	}

	/**
	 * HTS maximum likelihood parameter generation
	 *
	 * @param um
	 *            : utterance model sequence after processing Mary context features
	 * @param htsData
	 *            : HMM pdfs model set.
	 * @throws Exception
	 *             Exception
	 */
	public void htsMaximumLikelihoodParameterGeneration(HTSUttModel um, final HMMData htsData) throws Exception {
		CartTreeSet ms = htsData.getCartTreeSet();

		/* Initialisation of PStream objects */
		/* Initialise Parameter generation using UttModel um and Modelset ms */
		/* initialise PStream objects for all the parameters that are going to be generated: */
		/* mceppst, strpst, magpst, lf0pst */
		/* Here i should pass the window files to initialise the dynamic windows dw */
		/* for the moment the dw are all the same and hard-coded */
		if (htsData.getPdfMgcStream() != null)
			mcepPst = new HTSPStream(ms.getMcepVsize(), um.getTotalFrame(), HMMData.FeatureType.MGC, htsData.getMaxMgcGvIter());
		/* for lf0 count just the number of lf0frames that are voiced or non-zero */
		if (htsData.getPdfLf0Stream() != null)
			lf0Pst = new HTSPStream(ms.getLf0Stream(), um.getLf0Frame(), HMMData.FeatureType.LF0, htsData.getMaxLf0GvIter());

		/* The following are optional in case of generating mixed excitation */
		if (htsData.getPdfStrStream() != null)
			strPst = new HTSPStream(ms.getStrVsize(), um.getTotalFrame(), HMMData.FeatureType.STR, htsData.getMaxStrGvIter());
		if (htsData.getPdfMagStream() != null)
			magPst = new HTSPStream(ms.getMagVsize(), um.getTotalFrame(), HMMData.FeatureType.MAG, htsData.getMaxMagGvIter());

		int lf0Frame = 0; // counts voiced frames
		int uttFrame = 0; // counts all frames
		voiced = new boolean[um.getTotalFrame()];

		// local variables for faster access
		int msNumStates = ms.getNumStates();
		int totalFrames = um.getTotalFrame();
		for (int i = 0; i < um.getNumUttModel(); i++) {
			HTSModel m = um.getUttModel(i);
			int numVoicedInModel = 0;
			for (int state = 0; state < msNumStates; state++) {
				int dur = m.getDur(state);
				Arrays.fill(voiced, uttFrame, uttFrame += dur, m.getVoiced(state));
				if (m.getVoiced(state))
					lf0Frame += dur;

			}
		}

		/* mcepframe and lf0frame are used in the original code to initialise the T field */
		/* in each pst, but here the pst are already initialised .... */
		logger.debug("utteranceFrame=" + uttFrame + " lf0frame=" + lf0Frame);

		// Step 1: initialize fields in the parameter streams
		uttFrame = 0;
		lf0Frame = 0;

		/* copy pdfs */
		for (int i = 0; i < um.getNumUttModel(); i++) {
			HTSModel m = um.getUttModel(i);
			boolean gvSwitch = m.getGvSwitch();
			for (int state = 0; state < msNumStates; state++) {

				for (int frame = 0; frame < m.getDur(state); frame++) {

					/* copy pdfs for mcep */
					if (mcepPst != null) {
						mcepPst.setMseq(uttFrame, m.getMean(FeatureType.MGC, state));
						mcepPst.setVseq(uttFrame, m.getVariance(FeatureType.MGC, state));
						if (!gvSwitch)
							mcepPst.setGvSwitch(uttFrame, false);
					}

					/* copy pdf for str */
					if (strPst != null) {
						strPst.setMseq(uttFrame, m.getMean(FeatureType.STR, state));
						strPst.setVseq(uttFrame, m.getVariance(FeatureType.STR, state));
						if (!gvSwitch)
							strPst.setGvSwitch(uttFrame, false);
					}

					/* copy pdf for mag */
					if (magPst != null) {
						magPst.setMseq(uttFrame, m.getMean(FeatureType.MAG, state));
						magPst.setVseq(uttFrame, m.getVariance(FeatureType.MAG, state));
						if (!gvSwitch)
							magPst.setGvSwitch(uttFrame, false);
					}

					/* copy pdfs for lf0 */
					if (lf0Pst != null && !htsData.getUseAcousticModels()) {
						for (int k = 0; k < ms.getLf0Stream(); k++) {
							boolean nobound = true;
							/* check if current frame is voiced/unvoiced boundary or not */
							for (int n = lf0Pst.getDWLeftBoundary(k); n <= lf0Pst.getDWRightBoundary(k); n++)
								if ((uttFrame + n) <= 0 || totalFrames <= (uttFrame + n))
									nobound = false;
								else
									nobound = (nobound && voiced[uttFrame + n]);
							/* copy pdfs */
							if (voiced[uttFrame]) {
								lf0Pst.setMseq(lf0Frame, k, m.getLf0Mean(state, k));
								if (nobound || k == 0)
									lf0Pst.setIvseq(lf0Frame, k, finv(m.getLf0Variance(state, k)));
								else
									/* the variances for dynamic features are set to inf on v/uv boundary */
									lf0Pst.setIvseq(lf0Frame, k, 0.0);
							}
						}
					}
					if (voiced[uttFrame]) {
						if (!gvSwitch)
							lf0Pst.setGvSwitch(lf0Frame, false);
						lf0Frame++;
					}
					uttFrame++;

				} /* for each frame in this state */
			} /* for each state in this model */
		} /* for each model in this utterance */

		GVModelSet gvms = htsData.getGVModelSet();

		// Step 2: set dynamic features to infinity on the borders for MGC/STR/MAG
		if (mcepPst != null)
			mcepPst.fixDynFeatOnBoundaries();
		if (strPst != null)
			strPst.fixDynFeatOnBoundaries();
		if (magPst != null)
			magPst.fixDynFeatOnBoundaries();

		// Step 3: optimize individual parameter streams

		/* parameter generation for mcep */
		if (mcepPst != null) {
			logger.info("Parameter generation for MGC: ");
			if (htsData.getUseGV() && (htsData.getPdfMgcGVStream() != null))
				mcepPst.setGvMeanVar(gvms.getGVmeanMgc(), gvms.getGVcovInvMgc());
			mcepPst.mlpg(htsData, htsData.getUseGV());
		}

		// parameter generation for lf0 */
		if (htsData.getUseAcousticModels())
			loadMaryXmlF0(um, htsData);
		else if (lf0Pst != null) {
			logger.info("Parameter generation for LF0: ");
			if (htsData.getUseGV() && (htsData.getPdfLf0GVStream() != null))
				lf0Pst.setGvMeanVar(gvms.getGVmeanLf0(), gvms.getGVcovInvLf0());
			lf0Pst.mlpg(htsData, htsData.getUseGV());
			// here we need set realisedF0
			setRealisedF0(lf0Pst, um, msNumStates);
		}

		/* parameter generation for str */
		boolean useGV = false;
		if (strPst != null) {
			logger.debug("Parameter generation for STR ");
			if (htsData.getUseGV() && (htsData.getPdfStrGVStream() != null)) {
				useGV = true;
				strPst.setGvMeanVar(gvms.getGVmeanStr(), gvms.getGVcovInvStr());
			}
			strPst.mlpg(htsData, useGV);
		}

		/* parameter generation for mag */
		useGV = false;
		if (magPst != null) {
			logger.info("Parameter generation for MAG ");
			if (htsData.getUseGV() && (htsData.getPdfMagGVStream() != null)) {
				useGV = true;
				magPst.setGvMeanVar(gvms.getGVmeanMag(), gvms.getGVcovInvMag());
			}
			magPst.mlpg(htsData, useGV);
		}

	} /* method htsMaximumLikelihoodParameterGeneration */

	/* Save generated parameters in a binary file */
	public void saveParamMaryFormat(String fileName, HTSPStream par, HMMData.FeatureType type) {
		int t, m, i;
		double ws = 0.025; /* window size in seconds */
		double ss = 0.005; /* skip size in seconds */
		int fs = 16000; /* sampling rate */

		try {

			if (type == HMMData.FeatureType.LF0) {
				fileName += ".ptc";
				/*
				 * DataOutputStream data_out = new DataOutputStream (new FileOutputStream (fileName));
				 * data_out.writeFloat((float)(ws*fs)); data_out.writeFloat((float)(ss*fs)); data_out.writeFloat((float)fs);
				 * data_out.writeFloat(voiced.length);
				 *
				 * i=0; for(t=0; t<voiced.length; t++){ // here par.getT are just the voiced!!! so the actual length of frames can
				 * be taken from the voiced array if( voiced[t] ){ data_out.writeFloat((float)Math.exp(par.getPar(i,0))); i++;
				 * }System.out.println("GEN f0s[" + t + "]=" + Math.exp(lf0Pst.getPar(i,0))); else
				 * data_out.writeFloat((float)0.0); } data_out.close();
				 */

				i = 0;
				double f0s[] = new double[voiced.length];
				// System.out.println("voiced.length=" + voiced.length);
				for (t = 0; t < voiced.length; t++) { // here par.getT are just the voiced!!! so the actual length of frames can
					// be taken from the voiced array
					if (voiced[t]) {
						f0s[t] = Math.exp(par.getPar(i, 0));
						i++;
					} else
						f0s[t] = 0.0;
					System.out.println("GEN f0s[" + t + "]=" + f0s[t]);

				}
				/*
				 * i am using this function but it changes the values of sw, and ss *samplingrate+0.5??? for the HTS values
				 * ss=0.005 and sw=0.025 is not a problem though
				 */
				PitchReaderWriter.write_pitch_file(fileName, f0s, (float) (ws), (float) (ss), fs);

			} else if (type == HMMData.FeatureType.MGC) {

				int numfrm = par.getT();
				int dimension = par.getOrder();
				Mfccs mgc = new Mfccs(numfrm, dimension);

				fileName += ".mfc";

				for (t = 0; t < par.getT(); t++)
					for (m = 0; m < par.getOrder(); m++)
						mgc.mfccs[t][m] = par.getPar(t, m);

				mgc.params.samplingRate = fs; /* samplingRateInHz */
				mgc.params.skipsize = (float) ss; /* skipSizeInSeconds */
				mgc.params.winsize = (float) ws; /* windowSizeInSeconds */

				mgc.writeMfccFile(fileName);

				/*
				 * The whole set for header is in the following order: ler.writeInt(numfrm); ler.writeInt(dimension);
				 * ler.writeFloat(winsize); ler.writeFloat(skipsize); ler.writeInt(samplingRate);
				 */

			}

			logger.info("saveParam in file: " + fileName);

		} catch (IOException e) {
			logger.info("IO exception = " + e);
		}
	}

	/* Save generated parameters in a binary file */
	public void saveParam(String fileName, HTSPStream par, HMMData.FeatureType type) {
		int t, m, i;
		try {

			if (type == HMMData.FeatureType.LF0) {
				fileName += ".f0";
				DataOutputStream data_out = new DataOutputStream(new FileOutputStream(fileName));
				i = 0;
				for (t = 0; t < voiced.length; t++) { /* here par.getT are just the voiced!!! */
					if (voiced[t]) {
						data_out.writeFloat((float) Math.exp(par.getPar(i, 0)));
						i++;
					} else
						data_out.writeFloat((float) 0.0);
				}
				data_out.close();

			} else if (type == HMMData.FeatureType.MGC) {
				fileName += ".mgc";
				DataOutputStream data_out = new DataOutputStream(new FileOutputStream(fileName));
				for (t = 0; t < par.getT(); t++)
					for (m = 0; m < par.getOrder(); m++)
						data_out.writeFloat((float) par.getPar(t, m));
				data_out.close();
			}

			logger.info("saveParam in file: " + fileName);

		} catch (IOException e) {
			logger.info("IO exception = " + e);
		}
	}

	private void loadMaryXmlF0(HTSUttModel um, HMMData htsData) throws Exception {
		logger.info("Using f0 from maryXML acoustparams");
		int i, n, numVoiced;
		HTSModel m;
		double[] dval;
		double lastF0 = 0.0;
		numVoiced = 0;
		Vector<Double> f0Vector = new Vector<Double>();

		for (i = 0; i < um.getNumUttModel(); i++) {
			m = um.getUttModel(i);
			// System.out.format("\nmodel=%s totalDur=%d numVoicedFrames=%d F0=%s\n", m.getPhoneName(), m.getTotalDur(),
			// m.getNumVoiced(), m.getMaryXmlF0());
			// get contour for this model if voiced frames and maryXml has f0 values
			dval = getContourSegment(m.getMaryXmlF0(), m.getNumVoiced());
			// accumulate the values
			for (n = 0; n < dval.length; n++)
				f0Vector.add(dval[n]);
		}
		// interpolate values if necessary
		interpolateSegments(f0Vector);

		// create a new Lf0Pst with the values from maryXML
		HTSPStream newLf0Pst = new HTSPStream(3, f0Vector.size(), HMMData.FeatureType.LF0, htsData.getMaxLf0GvIter());
		for (n = 0; n < f0Vector.size(); n++)
			newLf0Pst.setPar(n, 0, Math.log(f0Vector.get(n)));

		setlf0Pst(newLf0Pst);

	}

	private double[] getContourSegment(String maryXmlF0, int numVoiced) throws Exception {
		int i, t = 0, k = 0, f = 0; // f is number of f0 in xml string

		// just fill the values in approx. position
		double[] f0Vector = new double[numVoiced];

		int index[] = new int[2];
		double value[] = new double[2];
		int key, n, interval;
		double valF0, lastValF0;

		if (maryXmlF0 != null) {
			Pattern p = Pattern.compile("(\\d+,\\d+)");
			Matcher xml = p.matcher(maryXmlF0);
			SortedMap<Integer, Double> f0Map = new TreeMap<Integer, Double>();
			int numF0s = 0;
			while (xml.find()) {
				String[] f0Values = (xml.group().trim()).split(",");
				f0Map.put(new Integer(f0Values[0]), new Double(f0Values[1]));
				numF0s++;
			}
			Set<Map.Entry<Integer, Double>> s = f0Map.entrySet();
			Iterator<Map.Entry<Integer, Double>> if0 = s.iterator();

			if (numF0s == numVoiced) {
				t = 0;
				while (if0.hasNext() && t < numVoiced) {
					Map.Entry<Integer, Double> mf0 = if0.next();
					key = (Integer) mf0.getKey();
					valF0 = (Double) mf0.getValue();
					f0Vector[t++] = valF0;
				}
			} else {
				if (numF0s < numVoiced) {
					for (i = 0; i < numVoiced; i++)
						// then just some values will be filled, so the other must be 0
						f0Vector[i] = 0.0;
				}
				while (if0.hasNext() && t < numVoiced) {
					Map.Entry<Integer, Double> mf0 = if0.next();
					key = (Integer) mf0.getKey();
					valF0 = (Double) mf0.getValue();
					if (key == 0)
						n = 0;
					else if (key == 100)
						n = numVoiced - 1;
					else
						n = (int) ((numVoiced * key) / 100.0);
					if (n >= 0 && n < numVoiced)
						f0Vector[n] = valF0;
				} // while(if0.hasNext())
			} // numF0s == numVoiced
		} // if maryXML != null

		// for(i=0; i<numVoiced; i++) // then just some values will be filled, so the other must be 0
		// System.out.format("%.1f ", f0Vector[i]);
		// System.out.println();

		return f0Vector;
	}

	private void interpolateSegments(Vector<Double> f0) {
		int i, n, interval;
		double slope;
		// check where there are zeros and interpolate
		int[] index = new int[2];
		double[] value = new double[2];

		index[0] = 0;
		value[0] = 0.0;
		for (i = 0; i < f0.size(); i++) {
			if (f0.get(i) > 0.0) {
				index[1] = i;
				value[1] = f0.get(i);

				interval = index[1] - index[0];
				if (interval > 1) {
					// System.out.format("Interval to interpolate index[0]=%d index[1]=%d\n",index[0],index[1]);
					slope = ((value[1] - value[0]) / interval);
					for (n = index[0]; n < index[1]; n++) {
						double newVal = (slope * (n - index[0])) + value[0];
						f0.set(n, newVal);
						// System.out.format(" n=%d value:%.1f\n",n,newVal);
					}
				}
				index[0] = index[1];
				value[0] = value[1];
			}
		}
	}

	private void setRealisedF0(HTSPStream lf0Pst, HTSUttModel um, int numStates) {
		int t = 0;
		int vt = 0;
		for (int i = 0; i < um.getNumUttModel(); i++) {
			HTSModel m = um.getUttModel(i);
			int numVoicedInModel = m.getNumVoiced();
			String formattedF0 = "";
			int k = 1;
			for (int state = 0; state < numStates; state++) {
				for (int frame = 0; frame < m.getDur(state); frame++) {
					if (voiced[t++]) {
						float f0 = (float) Math.exp(lf0Pst.getPar(vt++, 0));
						formattedF0 += "(" + Integer.toString((int) ((k * 100.0) / numVoicedInModel)) + ","
								+ Integer.toString((int) f0) + ")";
						k++;
					}
				} // for unvoiced frame
			} // for state
			if (!formattedF0.contentEquals("")) {
				m.setMaryXmlF0(formattedF0);
				// m.setUnit_f0ArrayStr(formattedF0);
				// System.out.println("ph=" + m.getPhoneName() + " " + formattedF0);
			}
		} // for model in utterance model list
	}

} /* class ParameterGeneration */
