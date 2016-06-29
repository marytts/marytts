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

package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.HMMVoice;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.htsengine.HTSVocoder;
import marytts.htsengine.HTSEngineTest.PhonemeDuration;
import marytts.modules.synthesis.Voice;
import marytts.unitselection.select.Target;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

/**
 * HTSEngine: a compact HMM-based speech synthesis engine.
 *
 * Java port and extension of HTS engine API version 1.04 Extension: mixed excitation
 *
 * @author Marc Schr&ouml;der, Marcela Charfuelan
 */
public class HTSEngine extends InternalModule {
	private Logger loggerHts = MaryUtils.getLogger("HTSEngine");
	private String realisedDurations; // HMM realised duration to be save in a file
	private boolean phoneAlignmentForDurations;
	private boolean stateAlignmentForDurations = false;
	private Vector<PhonemeDuration> alignDur = null; // list of external duration per phone for alignment
	// this are durations loaded from a external file
	private double newStateDurationFactor = 0.5; // this is a factor that extends or shrinks the duration of a state
	// it can be used to try to syncronise the duration specified in a external
	// file
	// and the number of frames in a external lf0 file

	public String getRealisedDurations() {
		return realisedDurations;
	}

	public boolean getPhonemeAlignmentForDurations() {
		return phoneAlignmentForDurations;
	}

	public boolean getStateAlignmentForDurations() {
		return stateAlignmentForDurations;
	}

	public Vector<PhonemeDuration> getAlignDurations() {
		return alignDur;
	}

	public double getNewStateDurationFactor() {
		return newStateDurationFactor;
	}

	public void setRealisedDurations(String str) {
		realisedDurations = str;
	}

	public void setStateAlignmentForDurations(boolean bval) {
		stateAlignmentForDurations = bval;
	}

	public void setPhonemeAlignmentForDurations(boolean bval) {
		phoneAlignmentForDurations = bval;
	}

	public void setAlignDurations(Vector<PhonemeDuration> val) {
		alignDur = val;
	}

	public void setNewStateDurationFactor(double dval) {
		newStateDurationFactor = dval;
	}

	public HTSEngine() {
		super("HTSEngine", MaryDataType.TARGETFEATURES, MaryDataType.AUDIO, null);
		phoneAlignmentForDurations = false;
		stateAlignmentForDurations = false;
		alignDur = null;
	}

	/**
	 * This module is actually tested as part of the HMMSynthesizer test, for which reason this method does nothing.
	 *
	 * @throws Error
	 *             Error
	 */
	public synchronized void powerOnSelfTest() throws Error {
	}

	/**
	 * This functions process directly the target features list: targetFeaturesList when using external prosody, duration and f0
	 * are read from acoustparams: segmentsAndBoundaries realised durations and f0 are set in: tokensAndBoundaries when calling
	 * this function HMMVoice must be initialised already, that is TreeSet and ModelSet must be loaded already.
	 *
	 * @param d
	 *            : to get the default voice and locale
	 * @param targetFeaturesList
	 *            : the actual input data to HTS based synthesis
	 * @param segmentsAndBoundaries
	 *            : to update segment timings that are influenced by HMM state selection
	 * @param tokensAndBoundaries
	 *            :
	 * @throws Exception
	 *             Exception
	 * @return output
	 */
	public MaryData process(MaryData d, List<Target> targetFeaturesList, List<Element> segmentsAndBoundaries,
			List<Element> tokensAndBoundaries) throws Exception {

		Voice v = d.getDefaultVoice(); /* This is the way of getting a Voice through a MaryData type */
		assert v instanceof HMMVoice;
		HMMVoice hmmv = (HMMVoice) v;

		/**
		 * The utterance model, um, is a Vector (or linked list) of Model objects. It will contain the list of models for current
		 * label file.
		 */
		/* Process label file of Mary context features and creates UttModel um */
		HTSUttModel um = processTargetList(targetFeaturesList, segmentsAndBoundaries, hmmv.getHMMData());

		/* Process UttModel */
		HTSParameterGeneration pdf2par = new HTSParameterGeneration();

		/* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
		pdf2par.htsMaximumLikelihoodParameterGeneration(um, hmmv.getHMMData());

		/* set parameters for generation: f0Std, f0Mean and length, default values 1.0, 0.0 and 0.0 */
		/* These values are fixed in HMMVoice */

		/* Process generated parameters */
		HTSVocoder par2speech = new HTSVocoder();

		/* Synthesize speech waveform, generate speech out of sequence of parameters */
		AudioInputStream ais = par2speech.htsMLSAVocoder(pdf2par, hmmv.getHMMData());

		MaryData output = new MaryData(outputType(), d.getLocale());
		if (d.getAudioFileFormat() != null) {
			output.setAudioFileFormat(d.getAudioFileFormat());
			if (d.getAudio() != null) {
				// This (empty) AppendableSequenceAudioInputStream object allows a
				// thread reading the audio data on the other "end" to get to our data as we are producing it.
				assert d.getAudio() instanceof AppendableSequenceAudioInputStream;
				output.setAudio(d.getAudio());
			}
		}
		output.appendAudio(ais);

		// set the actualDurations in tokensAndBoundaries
		if (tokensAndBoundaries != null)
			setRealisedProsody(tokensAndBoundaries, um);

		return output;

	}

	public static void setRealisedProsody(List<Element> tokensAndBoundaries, HTSUttModel um) throws SynthesisException {
		int i, j, index;
		NodeList no1, no2;
		NamedNodeMap att;
		Scanner s = null;
		String line, str[];
		float totalDur = 0f; // total duration, in seconds
		double f0[];
		HTSModel m;

		int numModel = 0;

		for (Element e : tokensAndBoundaries) {
			// System.out.println("TAG: " + e.getTagName());
			if (e.getTagName().equals(MaryXML.TOKEN)) {
				NodeIterator nIt = MaryDomUtils.createNodeIterator(e, MaryXML.PHONE);
				Element phone;

				while ((phone = (Element) nIt.nextNode()) != null) {
					String p = phone.getAttribute("p");
					m = um.getUttModel(numModel++);

					// CHECK THIS!!!!!!!

					// System.out.println("realised p=" + p + " phoneName=" + m.getPhoneName());
					// int currentDur = m.getTotalDurMillisec();
					totalDur += m.getTotalDurMillisec() * 0.001f;
					// phone.setAttribute("d", String.valueOf(currentDur));
					phone.setAttribute("d", m.getMaryXmlDur());
					// phone.setAttribute("end", String.valueOf(totalDur));

					// phone.setAttribute("f0", m.getUnit_f0ArrayStr());
					phone.setAttribute("f0", m.getMaryXmlF0());

				}
			} else if (e.getTagName().contentEquals(MaryXML.BOUNDARY)) {
				int breakindex = 0;
				try {
					breakindex = Integer.parseInt(e.getAttribute("breakindex"));
				} catch (NumberFormatException nfe) {
				}
				if (e.hasAttribute("duration") || breakindex >= 3) {
					m = um.getUttModel(numModel++);
					if (m.getPhoneName().contentEquals("_")) {
						int currentDur = m.getTotalDurMillisec();
						// index = ph.indexOf("_");
						totalDur += currentDur * 0.001f;
						e.setAttribute("duration", String.valueOf(currentDur));
					}
				}
			} // else ignore whatever other label...
		}
	}

	public HTSUttModel processUttFromFile(String feaFile, HMMData htsData) throws Exception {

		List<Target> targetFeaturesList = getTargetsFromFile(feaFile, htsData);
		return processTargetList(targetFeaturesList, null, htsData);

	}

	/**
	 * Reads the Label file, the file which contains the Mary context features, creates an scanner object and calls getTargets
	 *
	 * @param LabFile
	 *            LabFile
	 * @param htsData
	 *            htsData
	 * @throws Exception
	 *             Exception
	 * @return targets
	 */
	public static List<Target> getTargetsFromFile(String LabFile, HMMData htsData) throws Exception {
		List<Target> targets = null;
		Scanner s = null;
		try {
			/* parse text in label file */
			s = new Scanner(new BufferedReader(new FileReader(LabFile)));
			targets = getTargets(s, htsData);

		} catch (FileNotFoundException e) {
			System.err.println("FileNotFoundException: " + e.getMessage());
		} finally {
			if (s != null)
				s.close();
		}
		return targets;
	}

	/**
	 * Creates a scanner object with the Mary context features contained in Labtext and calls getTargets
	 *
	 * @param LabText
	 *            LabText
	 * @param htsData
	 *            htsData
	 * @throws Exception
	 *             Exception
	 * @return targets
	 */
	public List<Target> getTargetsFromText(String LabText, HMMData htsData) throws Exception {
		List<Target> targets;
		Scanner s = null;
		try {
			s = new Scanner(LabText);
			targets = getTargets(s, htsData);
		} finally {
			if (s != null)
				s.close();
		}
		return targets;
	}

	public static List<Target> getTargets(Scanner s, HMMData htsData) {
		int i;
		// Scanner s = null;
		String nextLine;
		FeatureDefinition feaDef = htsData.getFeatureDefinition();
		List<Target> targets = new ArrayList<Target>();
		FeatureVector fv;
		Target t;
		/* Skip mary context features definition */
		while (s.hasNext()) {
			nextLine = s.nextLine();
			if (nextLine.trim().equals(""))
				break;
		}
		/* skip until byte values */
		int numLines = 0;
		while (s.hasNext()) {
			nextLine = s.nextLine();
			if (nextLine.trim().equals(""))
				break;
			numLines++;
		}
		/* get feature vectors from byte values */
		i = 0;
		while (s.hasNext()) {
			nextLine = s.nextLine();
			// System.out.println("STR: " + nextLine);
			fv = feaDef.toFeatureVector(0, nextLine);
			t = new Target(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef), null);
			t.setFeatureVector(fv);
			targets.add(t);
		}
		return targets;
	}

	/***
	 * Process feature vectors in target list to generate a list of models for generation and realisation
	 *
	 * @param targetFeaturesList
	 *            : each target must contain the corresponding feature vector
	 * @param segmentsAndBoundaries
	 *            : if applying external prosody provide acoust params as a list of elements
	 * @param htsData
	 *            : parameters and configuration of the voice
	 * @throws Exception
	 *             Exception
	 * @return um
	 */
	protected HTSUttModel processTargetList(List<Target> targetFeaturesList, List<Element> segmentsAndBoundaries, HMMData htsData)
			throws Exception {
		HTSUttModel um = new HTSUttModel();
		CartTreeSet cart = htsData.getCartTreeSet();
		realisedDurations = "#\n";
		int numLab = 0;
		double diffdurOld = 0.0;
		int alignDurSize = 0;
		final float fperiodmillisec = ((float) htsData.getFperiod() / (float) htsData.getRate()) * 1000;
		final float fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());
		boolean firstPh = true;
		float durVal = 0.0f;
		FeatureDefinition feaDef = htsData.getFeatureDefinition();

		int featureIndex = feaDef.getFeatureIndex("phone");
		if (htsData.getUseAcousticModels()) {
			phoneAlignmentForDurations = true;
			loggerHts.info("Using prosody from acoustparams.");
		} else {
			phoneAlignmentForDurations = false;
			loggerHts.info("Estimating state durations from (Gaussian) state duration model.");
		}

		// process feature vectors in targetFeatureList
		int i = 0;
		for (Target target : targetFeaturesList) {

			FeatureVector fv = target.getFeatureVector(); // feaDef.toFeatureVector(0, nextLine);
			HTSModel m = new HTSModel(cart.getNumStates());
			um.addUttModel(m);
			m.setPhoneName(fv.getFeatureAsString(featureIndex, feaDef));

			// Check if context-dependent gv (gv without sil)
			if (htsData.getUseContextDependentGV()) {
				if (m.getPhoneName().contentEquals("_"))
					m.setGvSwitch(false);
			}
			// System.out.println("HTSEngine: phone=" + m.getPhoneName());

			double diffdurNew;

			// get the duration and f0 values from the acoustparams = segmentsAndBoundaries
			if (segmentsAndBoundaries != null) {
				Element e = segmentsAndBoundaries.get(i);

				// get the durations of the Gaussians, because we need to know how long each estate should be
				// knowing the duration of each state we can modified it so the 5 states reflect the external duration
				// Here the duration for phones and sil (_) are calcualted
				diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, false, diffdurOld);

				if (e.getTagName().contentEquals("ph")) {
					// No duration => predict one !
					if ((e.getAttribute("d") == null) || (e.getAttribute("d").equals(""))) {
						diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, false, diffdurOld);
					}
					// Use phone duration
					else {
						m.setMaryXmlDur(e.getAttribute("d"));
						durVal = Float.parseFloat(m.getMaryXmlDur());
						// get proportion of this duration for each state; m.getTotalDur() contains total duration of the 5 states
						// in
						// frames
						// double durationsFraction = durVal / (fperiodmillisec * m.getTotalDur());
						m.setTotalDur(0);
						int total = 0;
						for (int k = 0; k < cart.getNumStates(); k++)
							total += m.getDur(k);
						// System.out.println("durval = " + durVal);
						for (int k = 0; k < cart.getNumStates(); k++) {
							// System.out.print(" state: " + k + " durFromGaussians=" + m.getDur(k));
							int newStateDuration = Math.round((durVal * m.getDur(k)) / (total * fperiodmillisec));
							newStateDuration = Math.max(1, newStateDuration);
							m.setDur(k, newStateDuration);
							m.incrTotalDur(newStateDuration);
							// System.out.println(" durNew=" + m.getDur(k));
						}
					}

				}
				// the duration for boundaries predicted in the AcousticModeller is not calculated with HMMs
				else if (e.getTagName().contentEquals("boundary")) {
					durVal = 0;
					if (!e.getAttribute("duration").isEmpty())
						durVal = Float.parseFloat(e.getAttribute("duration"));

					// TODO: here we need to differentiate a duration coming from outside and one fixed by the BoundaryModel
					// the marytts.modules.acoustic.BoundaryModel fix always duration="400" for breakindex
					// durations different from 400 milisec. are used here otherwise it is ignored and use the
					// the duration calculated from the gaussians instead.
					if (durVal != 0) {
						// if duration comes from a specified duration in miliseconds, i use that
						int durValFrames = Math.round(durVal / fperiodmillisec);
						int totalDurGaussians = m.getTotalDur();
						m.setTotalDur(durValFrames);
						// System.out.println(" boundary attribute:duration=" + durVal + " in frames=" + durValFrames);

						// the specified duration has to be split among the five states
						float durationsFraction = durVal / (fperiodmillisec * m.getTotalDur());
						m.setTotalDur(0);
						for (int k = 0; k < cart.getNumStates(); k++) {
							// System.out.print(" state: " + k + " durFromGaussians=" + m.getDur(k));
							int newStateDuration = Math.round(((float) m.getDur(k) / (float) totalDurGaussians) * durValFrames);
							newStateDuration = Math.max(newStateDuration, 1);
							m.setDur(k, newStateDuration);
							m.setTotalDur(m.getTotalDur() + m.getDur(k));
							// System.out.println(" durNew=" + m.getDur(k));
						}

					} else {
						if (!e.getAttribute("breakindex").isEmpty()) {
							durVal = Float.parseFloat(e.getAttribute("breakindex"));
							// System.out.print(" boundary attribute:breakindex=" + durVal);
						}
						durVal = (m.getTotalDur() * fperiodmillisec);
					}
					// System.out.println(" setMaryXml(durVal)=" + durVal);
					m.setMaryXmlDur(Float.toString(durVal));
				}

				// set F0 values
				if (e.hasAttribute("f0")) {
					m.setMaryXmlF0(e.getAttribute("f0"));
					// System.out.println(" f0=" + e.getAttribute("f0"));
				}

			}
			// Estimate state duration from state duration model (Gaussian)
			else {
				diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, false, diffdurOld);
			}

			um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());
			// System.out.println(" model=" + m.getPhoneName() + " TotalDurFrames=" + m.getTotalDur() + " TotalDurMilisec=" +
			// (fperiodmillisec * m.getTotalDur()) + "\n");

			// Set realised durations
			m.setTotalDurMillisec((int) (fperiodmillisec * m.getTotalDur()));

			double durSec = um.getTotalFrame() * fperiodsec;
			realisedDurations += Double.toString(durSec) + " " + numLab + " " + m.getPhoneName() + "\n";
			numLab++;

			diffdurOld = diffdurNew; // to calculate the duration of next phoneme

			/*
			 * Find pdf for LF0, this function sets the pdf for each state. here it is also set whether the model is voiced or not
			 */
			// if ( ! htsData.getUseUnitDurationContinuousFeature() )
			// Here according to the HMM models it is decided whether the states of this model are voiced or unvoiced
			// even if f0 is taken from maryXml here we need to set the voived/unvoiced values per model and state
			cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());

			/* Find pdf for Mgc, this function sets the pdf for each state. */
			cart.searchMgcInCartTree(m, fv, feaDef);

			/* Find pdf for strengths, this function sets the pdf for each state. */
			if (htsData.getTreeStrStream() != null)
				cart.searchStrInCartTree(m, fv, feaDef);

			/* Find pdf for Fourier magnitudes, this function sets the pdf for each state. */
			if (htsData.getTreeMagStream() != null)
				cart.searchMagInCartTree(m, fv, feaDef);

			/* increment number of models in utterance model */
			um.setNumModel(um.getNumModel() + 1);
			/* update number of states */
			um.setNumState(um.getNumState() + cart.getNumStates());
			i++;

			firstPh = false;
		}

		if (alignDur != null)
			if (um.getNumUttModel() != alignDurSize)
				throw new Exception("The number of durations provided for phone alignment (" + alignDurSize
                                    + ") is greater than the number of feature vectors (" + um.getNumUttModel() + ").");

		for (i = 0; i < um.getNumUttModel(); i++) {
			HTSModel m = um.getUttModel(i);
			for (int mstate = 0; mstate < cart.getNumStates(); mstate++)
				if (m.getVoiced(mstate))
					for (int frame = 0; frame < m.getDur(mstate); frame++)
						um.setLf0Frame(um.getLf0Frame() + 1);
			// System.out.println("Vector m[" + i + "]=" + m.getPhoneName() );
		}

		loggerHts.info("Number of models in sentence numModel=" + um.getNumModel() + "  Total number of states numState="
                       + um.getNumState());
		loggerHts.info("Total number of frames=" + um.getTotalFrame() + "  Number of voiced frames=" + um.getLf0Frame());

		// System.out.println("REALISED DURATIONS:" + realisedDurations);

		return um;
	} /* method processTargetList */

	/**
	 * Stand alone testing using a TARGETFEATURES file as input.
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
	public static void main(String[] args) throws IOException, InterruptedException, Exception {

		int j;
		/* configure log info */
		org.apache.log4j.BasicConfigurator.configure();

		/*
		 * The input for creating a sound file is a TARGETFEATURES file in MARY format, there is an example indicated in the
		 * configuration file as well. For synthesising other text generate first a TARGETFEATURES file with the MARY system save
		 * it in a file and use it as feaFile.
		 */
		HTSEngine hmm_tts = new HTSEngine();

		/*
		 * htsData contains: Data in the configuration file, .pdf, tree-xxx.inf file names and other parameters. After initHMMData
		 * it contains: ModelSet: Contains the .pdf's (means and variances) for dur, lf0, Mgc, str and mag these are all the HMMs
		 * trained for a particular voice TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, Mgc, str and mag these are all the
		 * trees trained for a particular voice.
		 */
		HMMData htsData = new HMMData();

		/* stand alone with cmu-slt-hsmm voice */
		String MaryBase = "/project/mary/marcela/marytts/";
		String voiceDir = MaryBase + "voice-cmu-slt-hsmm/src/main/resources/";
		String voiceName = "cmu-slt-hsmm"; /* voice name */
		String voiceConfig = "marytts/voice/CmuSltHsmm/voice.config"; /* voice configuration file name. */
		String durFile = MaryBase + "tmp/tmp.lab"; /* to save realised durations in .lab format */
		String parFile = MaryBase + "tmp/tmp"; /* to save generated parameters tmp.mfc and tmp.f0 in Mary format */
		String outWavFile = MaryBase + "tmp/tmp.wav"; /* to save generated audio file */

		// The settings for using GV and MixExc can be changed in this way:
		htsData.initHMMData(voiceName, voiceDir, voiceConfig);

		htsData.setUseGV(true);
		htsData.setUseMixExc(true);

		// Important: the stand alone works without the acoustic modeler, so it should be de-activated
		htsData.setUseAcousticModels(false);

		/**
		 * The utterance model, um, is a Vector (or linked list) of Model objects. It will contain the list of models for current
		 * label file.
		 */
		HTSUttModel um;
		HTSParameterGeneration pdf2par = new HTSParameterGeneration();
		HTSVocoder par2speech = new HTSVocoder();
		AudioInputStream ais;

		/** Example of context features file */
		String feaFile = voiceDir + "marytts/voice/CmuSltHsmm/cmu_us_arctic_slt_b0487.pfeats";

		try {
			/*
			 * Process Mary context features file and creates UttModel um, a linked list of all the models in the utterance. For
			 * each model, it searches in each tree, dur, cmp, etc, the pdf index that corresponds to a triphone context feature
			 * and with that index retrieves from the ModelSet the mean and variance for each state of the HMM.
			 */
			um = hmm_tts.processUttFromFile(feaFile, htsData);

			/* save realised durations in a lab file */
			FileWriter outputStream = new FileWriter(durFile);
			outputStream.write(hmm_tts.getRealisedDurations());
			outputStream.close();

			/* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
			/* the generated parameters will be saved in tmp.mfc and tmp.f0, including Mary header. */
			boolean debug = true; /* so it save the generated parameters in parFile */
			pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

			/* Synthesize speech waveform, generate speech out of sequence of parameters */
			ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

			System.out.println("Saving to file: " + outWavFile);
			System.out.println("Realised durations saved to file: " + durFile);
			File fileOut = new File(outWavFile);

			if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
				AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
			}

			System.out.println("Calling audioplayer:");
			AudioPlayer player = new AudioPlayer(fileOut);
			player.start();
			player.join();
			System.out.println("Audioplayer finished...");

		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		}
	} /* main method */

} /* class HTSEngine */
