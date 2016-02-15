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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import marytts.config.MaryConfig;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.server.MaryProperties;
import marytts.util.FeatureUtils;
import marytts.htsengine.HMMData.FeatureType;
import marytts.util.MaryUtils;
import marytts.util.io.PropertiesAccessor;

import org.apache.log4j.Logger;

/**
 * Configuration files and global variables for HTS engine.
 * 
 * Java port and extension of HTS engine API version 1.04 Extension: mixed excitation
 * 
 * @author Marcela Charfuelan
 */
public class HMMData {

	/** Number of model and identificator for the models */
	public static final int HTS_NUMMTYPE = 5;

	public static enum FeatureType {
		DUR, // duration
		MGC, // MGC mel-generalized cepstral coefficients
		LF0, // log(fundamental frequency)
		STR, // strength of excitation?
		MAG, // fourier magnitudes for pulse generation
	};

	public enum PdfFileFormat {
		dur, lf0, mgc, str, mag, join
	};

	private Logger logger = MaryUtils.getLogger("HMMData");

	/**
	 * Global variables for some functions, initialised with default values, so these values can be loaded from a configuration
	 * file.
	 */
	private int rate = 16000; /* sampling rate default: 16Khz */
	private int fperiod = 80; /* frame period or frame shift (point) default: 0.005sec = rate*0.005 = 80 */
	private double rho = 0.0; /* variable for speaking rate control */

	/*
	 * MGC: stage=gamma=0.0 alpha=0.42 linear gain LSP: gamma>0.0 LSP: gamma=1.0 alpha=0.0 Mel-LSP: gamma=1.0 alpha=0.42 MGC-LSP:
	 * gamma=3.0 alpha=0.42
	 */
	private int stage = 0; /* defines gamma=-1/stage : if stage=0 then Gamma=0 */
	private double alpha = 0.55; // 0.42; /* variable for frequency warping parameter */
	private double beta = 0.0; /* variable for postfiltering */
	private boolean useLogGain = false; /* log gain flag (for LSP) */

	private double uv = 0.5; /* variable for U/V threshold */
	private boolean algnst = false; /* use state level alignment for duration */
	private boolean algnph = false; /* use phone level alignment for duration */
	private boolean useMixExc = true; /* use Mixed Excitation */
	private boolean useFourierMag = false; /* use Fourier magnitudes for pulse generation */

	/** Global variance (GV) settings */
	private boolean useGV = false; /* use global variance in parameter generation */
	private boolean useContextDependentGV = false; /* Variable for allowing context-dependent GV for sil */
	private boolean gvMethodGradient = true; /* GV method: gradient or derivative (default gradient) */

	/* Max number of GV iterations when using gradient method, for derivative 5 is used by default */
	private int maxMgcGvIter = 100;
	private int maxLf0GvIter = 100;
	private int maxStrGvIter = 100;
	private int maxMagGvIter = 100;

	/* GV weights for each parameter: between 0.0-2.0 */
	private double gvWeightMgc = 1.0;
	private double gvWeightLf0 = 1.0;
	private double gvWeightStr = 1.0;
	private double gvWeightMag = 1.0;

	private boolean useAcousticModels = false; /* true is using AcousticModeller, is true for MARY 4.1 voices */

	/**
	 * variables for controlling generation of speech in the vocoder these variables have default values but can be fixed and read
	 * from the audio effects component. [Default][min--max]
	 */
	private double f0Std = 1.0; /* variable for f0 control, multiply f0 [1.0][0.0--5.0] */
	private double f0Mean = 0.0; /* variable for f0 control, add f0 [0.0][0.0--100.0] */
	private double length = 0.0; /* total number of frame for generated speech */
	/* length of generated speech (in seconds) [N/A][0.0--30.0] */
	private double durationScale = 1.0; /* less than 1.0 is faster and more than 1.0 is slower, min=0.1 max=3.0 */

	/** Tree files and TreeSet object */
	private InputStream treeDurStream; /* durations tree file */
	private InputStream treeLf0Stream; /* lf0 tree file */
	private InputStream treeMgcStream; /* Mgc tree file */
	private InputStream treeStrStream; /* Strengths tree file */
	private InputStream treeMagStream; /* Fourier magnitudes tree file */

	private FeatureDefinition feaDef; /* The feature definition is used for loading the tree using questions in MARY format */

	/**
	 * CartTreeSet contains the tree-xxx.inf, xxx: dur, lf0, Mgc, str and mag these are all the trees trained for a particular
	 * voice. the Cart tree also contains the corresponding pdfs.
	 */
	private CartTreeSet cart = new CartTreeSet();

	/** HMM pdf model files and ModelSet object */
	private InputStream pdfDurStream; /* durations Pdf file */
	private InputStream pdfLf0Stream; /* lf0 Pdf file */
	private InputStream pdfMgcStream; /* Mgc Pdf file */
	private InputStream pdfStrStream; /* Strengths Pdf file */
	private InputStream pdfMagStream; /* Fourier magnitudes Pdf file */

	/** GV pdf files */
	/** Global variance file, it contains one global mean vector and one global diagonal covariance vector */
	private InputStream pdfLf0GVStream; /* lf0 GV pdf file */
	private InputStream pdfMgcGVStream; /* Mgc GV pdf file */
	private InputStream pdfStrGVStream; /* Str GV pdf file */
	private InputStream pdfMagGVStream; /* Mag GV pdf file */
	private InputStream switchGVStream; /* File for allowing context dependent GV. */
	/* This file contains the phones, sil or pause, for which GV is not calculated (not used yet) */
	/* this tree does not have a corresponding pdf file, because it just indicate which labels in context to avoid for GV. */

	/** GVModelSet contains the global covariance and mean for lf0, mgc, str and mag */
	private GVModelSet gv = new GVModelSet();

	/** Variables for mixed excitation */
	private int numFilters;
	private int orderFilters;
	private double mixFilters[][]; /* filters for mixed excitation */

	/** tricky phones file if generated during training of HMMs. */
	private PhoneTranslator trickyPhones;

	public int getRate() {
		return rate;
	}

	public int getFperiod() {
		return fperiod;
	}

	public double getRho() {
		return rho;
	}

	public double getAlpha() {
		return alpha;
	}

	public double getBeta() {
		return beta;
	}

	public int getStage() {
		return stage;
	}

	public double getGamma() {
		return (stage != 0) ? -1.0 / stage : 0.0;
	}

	public boolean getUseLogGain() {
		return useLogGain;
	}

	public double getUV() {
		return uv;
	}

	public boolean getAlgnst() {
		return algnst;
	}

	public boolean getAlgnph() {
		return algnph;
	}

	public double getF0Std() {
		return f0Std;
	}

	public double getF0Mean() {
		return f0Mean;
	}

	public double getLength() {
		return length;
	}

	public double getDurationScale() {
		return durationScale;
	}

	public InputStream getTreeDurStream() {
		return treeDurStream;
	}

	public InputStream getTreeLf0Stream() {
		return treeLf0Stream;
	}

	public InputStream getTreeMgcStream() {
		return treeMgcStream;
	}

	public InputStream getTreeStrStream() {
		return treeStrStream;
	}

	public InputStream getTreeMagStream() {
		return treeMagStream;
	}

	public FeatureDefinition getFeatureDefinition() {
		return feaDef;
	}

	public InputStream getPdfDurStream() {
		return pdfDurStream;
	}

	public InputStream getPdfLf0Stream() {
		return pdfLf0Stream;
	}

	public InputStream getPdfMgcStream() {
		return pdfMgcStream;
	}

	public InputStream getPdfStrStream() {
		return pdfStrStream;
	}

	public InputStream getPdfMagStream() {
		return pdfMagStream;
	}

	public boolean getUseAcousticModels() {
		return useAcousticModels;
	}

	public void setUseAcousticModels(boolean bval) {
		useAcousticModels = bval;
	}

	public boolean getUseMixExc() {
		return useMixExc;
	}

	public boolean getUseFourierMag() {
		return useFourierMag;
	}

	public boolean getUseGV() {
		return useGV;
	}

	public boolean getUseContextDependentGV() {
		return useContextDependentGV;
	}

	public boolean getGvMethodGradient() {
		return gvMethodGradient;
	}

	public int getMaxMgcGvIter() {
		return maxMgcGvIter;
	}

	public int getMaxLf0GvIter() {
		return maxLf0GvIter;
	}

	public int getMaxStrGvIter() {
		return maxStrGvIter;
	}

	public int getMaxMagGvIter() {
		return maxMagGvIter;
	}

	public double getGvWeightMgc() {
		return gvWeightMgc;
	}

	public double getGvWeightLf0() {
		return gvWeightLf0;
	}

	public double getGvWeightStr() {
		return gvWeightStr;
	}

	public double getGvWeightMag() {
		return gvWeightMag;
	}

	public InputStream getPdfLf0GVStream() {
		return pdfLf0GVStream;
	}

	public InputStream getPdfMgcGVStream() {
		return pdfMgcGVStream;
	}

	public InputStream getPdfStrGVStream() {
		return pdfStrGVStream;
	}

	public InputStream getPdfMagGVStream() {
		return pdfMagGVStream;
	}

	public InputStream getSwitchGVStream() {
		return switchGVStream;
	}

	public int getNumFilters() {
		return numFilters;
	}

	public int getOrderFilters() {
		return orderFilters;
	}

	public double[][] getMixFilters() {
		return mixFilters;
	}

	public void setRate(int ival) {
		rate = ival;
	}

	public void setFperiod(int ival) {
		fperiod = ival;
	}

	public void setAlpha(double dval) {
		alpha = dval;
	}

	public void setBeta(double dval) {
		beta = dval;
	}

	public void setStage(int ival) {
		stage = ival;
	}

	public void setUseLogGain(boolean bval) {
		useLogGain = bval;
	}

	/*
	 * These variables have default values but can be modified with setting in audio effects component.
	 */
	public void setF0Std(double dval) {
		/* default=1.0, min=0.0, max=3.0 */
		if (dval >= 0.0 && dval <= 3.0)
			f0Std = dval;
		else
			f0Std = 1.0;
	}

	public void setF0Mean(double dval) {
		/* default=0.0, min=-300.0, max=300.0 */
		if (dval >= -300.0 && dval <= 300.0)
			f0Mean = dval;
		else
			f0Mean = 0.0;
	}

	public void setLength(double dval) {
		length = dval;
	}

	public void setDurationScale(double dval) {
		/* default=1.0, min=0.1, max=3.0 */
		if (dval >= 0.1 && dval <= 3.0)
			durationScale = dval;
		else
			durationScale = 1.0;

	}

	public CartTreeSet getCartTreeSet() {
		return cart;
	}

	public GVModelSet getGVModelSet() {
		return gv;
	}

	public void setPdfStrStream(InputStream str) {
		pdfStrStream = str;
	}

	public void setPdfMagStream(InputStream mag) {
		pdfMagStream = mag;
	}

	public void setUseMixExc(boolean bval) {
		useMixExc = bval;
	}

	public void setUseFourierMag(boolean bval) {
		useFourierMag = bval;
	}

	public void setUseGV(boolean bval) {
		useGV = bval;
	}

	public void setUseContextDepenendentGV(boolean bval) {
		useContextDependentGV = bval;
	}

	public void setGvMethod(String sval) {
		if (sval.contentEquals("gradient"))
			gvMethodGradient = true;
		else
			gvMethodGradient = false; // then simple derivative method is used
	}

	public void setMaxMgcGvIter(int val) {
		maxMgcGvIter = val;
	}

	public void setMaxLf0GvIter(int val) {
		maxLf0GvIter = val;
	}

	public void setMaxStrGvIter(int val) {
		maxStrGvIter = val;
	}

	public void setGvWeightMgc(double dval) {
		gvWeightMgc = dval;
	}

	public void setGvWeightLf0(double dval) {
		gvWeightLf0 = dval;
	}

	public void setGvWeightStr(double dval) {
		gvWeightStr = dval;
	}

	public void setNumFilters(int val) {
		numFilters = val;
	}

	public void setOrderFilters(int val) {
		orderFilters = val;
	}

	public void loadCartTreeSet() throws IOException, MaryConfigurationException {
		cart.loadTreeSet(this, feaDef, trickyPhones);
	}

	public void loadGVModelSet() throws IOException {
		gv.loadGVModelSet(this, feaDef);
	}

	public void initHMMData(PropertiesAccessor p, String voiceName) throws IOException, MaryConfigurationException {
		logger.debug("Reached new initHMMData");
		String prefix = "voice." + voiceName;
		rate = p.getInteger(prefix + ".samplingRate", rate);
		fperiod = p.getInteger(prefix + ".framePeriod", fperiod);
		alpha = p.getDouble(prefix + ".alpha", alpha);
		stage = p.getInteger(prefix + ".gamma", stage);
		useLogGain = p.getBoolean(prefix + ".logGain", useLogGain);
		beta = p.getDouble(prefix + ".beta", beta);

		treeDurStream = p.getStream(prefix + ".Ftd"); /* Tree DUR */
		treeLf0Stream = p.getStream(prefix + ".Ftf"); /* Tree LF0 */
		treeMgcStream = p.getStream(prefix + ".Ftm"); /* Tree MCP */
		treeStrStream = p.getStream(prefix + ".Fts"); /* Tree STR */
		treeMagStream = p.getStream(prefix + ".Fta"); /* Tree MAG */

		pdfDurStream = p.getStream(prefix + ".Fmd"); /* Model DUR */
		pdfLf0Stream = p.getStream(prefix + ".Fmf"); /* Model LF0 */
		pdfMgcStream = p.getStream(prefix + ".Fmm"); /* Model MCP */
		pdfStrStream = p.getStream(prefix + ".Fms"); /* Model STR */
		pdfMagStream = p.getStream(prefix + ".Fma"); /* Model MAG */

		useAcousticModels = p.getBoolean(prefix + ".useAcousticModels"); /*
																		 * use AcousticModeller, so prosody modification is
																		 * enabled
																		 */
		useMixExc = p.getBoolean(prefix + ".useMixExc"); /* Use Mixed excitation */
		useFourierMag = p.getBoolean(prefix + ".useFourierMag"); /* Use Fourier magnitudes for pulse generation */

		useGV = p.getBoolean(prefix + ".useGV"); /* Use Global Variance in parameter generation */
		if (useGV) {
			useContextDependentGV = p.getBoolean(prefix + ".useContextDependentGV"); /* Use context-dependent GV, (gv without sil) */
			String gvMethod = p.getProperty(prefix + ".gvMethod"); /* GV method: gradient or derivative (default gradient) */
			// this feature is new for MARY 5.0 so it will not appear in old config files
			if (gvMethod != null)
				setGvMethod(gvMethod);

			// Number of iteration for GV
			maxMgcGvIter = p.getInteger(prefix + ".maxMgcGvIter", maxMgcGvIter); /*
																				 * Max number of iterations for MGC gv
																				 * optimisation
																				 */
			maxLf0GvIter = p.getInteger(prefix + ".maxLf0GvIter", maxLf0GvIter); /*
																				 * Max number of iterations for LF0 gv
																				 * optimisation
																				 */
			maxStrGvIter = p.getInteger(prefix + ".maxStrGvIter", maxStrGvIter); /*
																				 * Max number of iterations for STR gv
																				 * optimisation
																				 */

			// weights for GV
			gvWeightMgc = p.getDouble(prefix + ".gvWeightMgc", gvWeightMgc); /* GV weight for mgc between 0.0-2.0 default 1.0 */
			gvWeightLf0 = p.getDouble(prefix + ".gvWeightLf0", gvWeightLf0); /* GV weight for lf0 between 0.0-2.0 default 1.0 */
			gvWeightStr = p.getDouble(prefix + ".gvWeightStr", gvWeightStr); /* GV weight for str between 0.0-2.0 default 1.0 */

			// GV pdf files: mean and variance (diagonal covariance)
			pdfLf0GVStream = p.getStream(prefix + ".Fgvf"); /* GV Model LF0 */
			pdfMgcGVStream = p.getStream(prefix + ".Fgvm"); /* GV Model MCP */
			pdfStrGVStream = p.getStream(prefix + ".Fgvs"); /* GV Model STR */
			pdfMagGVStream = p.getStream(prefix + ".Fgva"); /* GV Model MAG */
		}

		/* targetfeatures file, for testing */
		/* Example context feature file in TARGETFEATURES format */
		InputStream featureStream = p.getStream(prefix + ".FeaFile");
		feaDef = FeatureUtils.readFeatureDefinition(featureStream);

		/* trickyPhones file if any */
		trickyPhones = new PhoneTranslator(p.getStream(prefix + ".trickyPhonesFile")); /* tricky phones file, if any */

		/* Configuration for mixed excitation */
		InputStream mixFiltersStream = p.getStream(prefix + ".Fif"); /* Filter coefficients file for mixed excitation */
		if (mixFiltersStream != null) {
			numFilters = p.getInteger(prefix + ".in"); /* Number of filters */
			logger.debug("Loading Mixed Excitation Filters File:");
			readMixedExcitationFilters(mixFiltersStream);
		}

		/* Load TreeSet in CARTs. */
		logger.debug("Loading Tree Set in CARTs:");
		loadCartTreeSet();

		/* Load GV ModelSet gv */
		logger.debug("Loading GV Model Set:");
		loadGVModelSet();

		logger.debug("InitHMMData complete");
	}

	/**
	 * Reads from configuration file all the data files in this class this method is used when running HTSengine stand alone.
	 * 
	 * @param voiceName
	 *            voiceName
	 * @param marybase
	 *            marybase
	 * @param configFile
	 *            configFile
	 * @throws Exception
	 *             Exception
	 */
	public void initHMMData(String voiceName, String marybase, String configFile) throws Exception {

		Properties props = new Properties();

		FileInputStream fis = new FileInputStream(marybase + configFile);
		props.load(fis);
		fis.close();
		Map<String, String> maryBaseReplacer = new HashMap<String, String>();
		maryBaseReplacer.put("jar:", marybase);
		initHMMData(new PropertiesAccessor(props, false, maryBaseReplacer), voiceName);
	}

	public void initHMMData(String voiceName) throws IOException, MaryConfigurationException {
		initHMMData(MaryConfig.getVoiceConfig(voiceName).getPropertiesAccessor(true), voiceName);
	}

	/**
	 * Reads from configuration file tree and pdf data for duration and f0 this method is used by HMMModel
	 * 
	 * @param voiceName
	 *            voiceName
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void initHMMDataForHMMModel(String voiceName) throws IOException, MaryConfigurationException {
		PropertiesAccessor p = MaryConfig.getVoiceConfig(voiceName).getPropertiesAccessor(true);
		String prefix = "voice." + voiceName;
		treeDurStream = p.getStream(prefix + ".Ftd");
		pdfDurStream = p.getStream(prefix + ".Fmd");

		treeLf0Stream = p.getStream(prefix + ".Ftf");
		pdfLf0Stream = p.getStream(prefix + ".Fmf");
		useGV = p.getBoolean(prefix + ".useGV");
		if (useGV) {
			useContextDependentGV = p.getBoolean(prefix + ".useContextDependentGV", useContextDependentGV);
			if (p.getProperty(prefix + ".gvMethod") != null) {
				String sval = p.getProperty(prefix + ".gvMethod");
				setGvMethod(sval);
			}
			maxLf0GvIter = p.getInteger(prefix + ".maxLf0GvIter", maxLf0GvIter);
			gvWeightLf0 = p.getDouble(prefix + ".gvWeightLf0", gvWeightLf0);

			pdfLf0GVStream = p.getStream(prefix + ".Fgvf");
			maxLf0GvIter = p.getInteger(prefix + ".maxLf0GvIter", maxLf0GvIter);

		}

		/* Example context feature file in MARY format */
		InputStream feaStream = p.getStream(prefix + ".FeaFile");
		feaDef = FeatureUtils.readFeatureDefinition(feaStream);

		/*
		 * trickyPhones file, if any. If aliases for tricky phones were used during the training of HMMs then these aliases are in
		 * this file, if no aliases were used then the string is empty. This file will be used during the loading of HMM trees, so
		 * aliases of tricky phone are aplied back.
		 */
		InputStream trickyPhonesStream = p.getStream(prefix + ".trickyPhonesFile");
		trickyPhones = new PhoneTranslator(trickyPhonesStream);

		/* Load TreeSet ts and ModelSet ms for current voice */
		logger.info("Loading Tree Set in CARTs:");
		cart.loadTreeSet(this, feaDef, trickyPhones);

		logger.info("Loading GV Model Set:");
		gv.loadGVModelSet(this, feaDef);

	}

	/**
	 * Initialisation for mixed excitation : it loads the filter taps, they are read from MixFilterFile specified in the
	 * configuration file.
	 * 
	 * @param mixFiltersStream
	 *            mixFiltersStream
	 * @throws IOException
	 *             IOException
	 */
	public void readMixedExcitationFilters(InputStream mixFiltersStream) throws IOException {
		String line;
		// first read the taps and then divide the total amount equally among the number of filters
		Vector<Double> taps = new Vector<Double>();
		/* get the filter coefficients */
		Scanner s = null;
		int i, j;
		try {
			s = new Scanner(new BufferedReader(new InputStreamReader(mixFiltersStream, "UTF-8")));
			s.useLocale(Locale.US);

			logger.debug("reading mixed excitation filters");
			while (s.hasNext("#")) { /* skip comment lines */
				line = s.nextLine();
				// System.out.println("comment: " + line );
			}
			while (s.hasNextDouble())
				taps.add(s.nextDouble());
		} finally {
			if (s != null) {
				s.close();
			}
		}

		orderFilters = (int) (taps.size() / numFilters);
		mixFilters = new double[numFilters][orderFilters];
		int k = 0;
		for (i = 0; i < numFilters; i++) {
			for (j = 0; j < orderFilters; j++) {
				mixFilters[i][j] = taps.get(k++);
				// System.out.println("h["+i+"]["+j+"]="+h[i][j]);
			}
		}
		logger.debug("initMixedExcitation: loaded filter taps");
		logger.debug("initMixedExcitation: numFilters = " + numFilters + "  orderFilters = " + orderFilters);

	} /* method readMixedExcitationFiltersFile() */

	/**
	 * return the set of FeatureTypes that are available in this HMMData object
	 * 
	 * @return featureTypes
	 */
	public Set<FeatureType> getFeatureSet() {
		Set<FeatureType> featureTypes = EnumSet.noneOf(FeatureType.class);
		if (getPdfDurStream() != null)
			featureTypes.add(FeatureType.DUR);
		if (getPdfLf0Stream() != null)
			featureTypes.add(FeatureType.LF0);
		if (getPdfStrStream() != null)
			featureTypes.add(FeatureType.STR);
		if (getPdfMagStream() != null)
			featureTypes.add(FeatureType.MAG);
		if (getPdfMgcStream() != null)
			featureTypes.add(FeatureType.MGC);
		return featureTypes;
	}

}
