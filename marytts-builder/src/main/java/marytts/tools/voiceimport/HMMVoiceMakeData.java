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

package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.htsengine.PhoneTranslator;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.io.FileUtils;
import marytts.util.io.General;
import marytts.util.string.StringUtils;

public class HMMVoiceMakeData extends VoiceImportComponent {

	private DatabaseLayout db;
	private String name = "HMMVoiceMakeData";

	/** Tree files and TreeSet object */
	public final String MGC = name + ".makeMGC";
	public final String LF0 = name + ".makeLF0";
	public final String STR = name + ".makeSTR";
	public final String CMPMARY = name + ".makeCMPMARY";
	public final String LABELMARY = name + ".makeLABELMARY";
	public final String QUESTIONSMARY = name + ".makeQUESTIONSMARY";
	public final String LIST = name + ".makeLIST";
	public final String SCP = name + ".makeSCP";
	public final String questionsFile = name + ".questionsFile";
	public final String questionsUttFile = name + ".questionsUttFile";
	public final String featureListFile = name + ".featureListFile";
	public final String featureListMapFile = name + ".featureListMapFile";
	public final String trickyPhonesFile = name + ".trickyPhonesFile";
	public final String ADAPTSCRIPTS = name + ".adaptScripts";
	private FilenameFilter featFileFilter;

	public String getName() {
		return name;
	}

	/**
	 * Get the map of properties2values containing the default values
	 * 
	 * @param db
	 *            db
	 * @return map of props2values
	 */
	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();

			props.put(MGC, "1");
			props.put(LF0, "1");
			props.put(STR, "1");
			props.put(CMPMARY, "1");
			props.put(LABELMARY, "1");
			props.put(QUESTIONSMARY, "1");
			props.put(LIST, "1");
			props.put(SCP, "1");
			props.put(questionsFile, "hts/data/questions/questions_qst001.hed");
			props.put(questionsUttFile, "hts/data/questions/questions_utt_qst001.hed");
			props.put(featureListFile, "mary/hmmFeatures.txt");
			props.put(featureListMapFile, "mary/hmmFeaturesMap.txt");
			props.put(trickyPhonesFile, "mary/trickyPhones.txt");
			props.put(ADAPTSCRIPTS, "false");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();

		props2Help.put(MGC, "Extracting MGC or MGC-LSP coefficients from raw audio.");
		props2Help.put(LF0, "Extracting log f0 sequence from raw audio.");
		props2Help.put(STR, "Extracting strengths from 5 filtered bands from raw audio.");
		props2Help.put(CMPMARY, "Composing training data files from mgc, lf0 and str files.");
		props2Help.put(LABELMARY, "Extracting monophone and fullcontext labels from phonelab and phonefeature files.");
		props2Help.put(QUESTIONSMARY, "Creating questions .hed file.");
		props2Help.put(LIST, "Generating a fullcontext model list occurred in the training data.");
		props2Help.put(SCP, "Generating a trainig data script.");
		props2Help.put(questionsFile, "Name of the file that will contain the questions (This file will be created).");
		props2Help.put(questionsUttFile,
				"Name of the file that will contain the questions for context dependent GV (This file will be created).");
		props2Help
				.put(featureListFile,
						"A file that contains additional context features used for training HMMs, normally it"
								+ " should be a subset of mary/features.txt. This file is automatically created by the HMMVoiceFeatureSelection component.");
		props2Help
				.put(featureListMapFile,
						"A map of features, so the training is not done with long names but aliases, this file contains the names and aliases used.");
		props2Help.put(trickyPhonesFile, "list of aliases for tricky phones, so the HTK-HHEd command can handle them. (This file"
				+ " will be created automatically if aliases are necessary.)");
		props2Help.put(ADAPTSCRIPTS, "ADAPTSCRIPTS=false: speaker dependent scripts, ADAPTSCRIPTS=true: "
				+ " speaker adaptation/adaptive scripts.  ");

	}

	/**
	 * Do the computations required by this component.
	 * 
	 * @throws Exception
	 *             Exception
	 * @return true on success, false on failure
	 */
	public boolean compute() throws Exception {

		String cmdLine;
		String voiceDir = db.getProp(DatabaseLayout.ROOTDIR);

		if (Integer.parseInt(getProp(MGC)) == 1) {
			cmdLine = "cd " + voiceDir + "hts/data\nmake mgc\n";
			General.launchBatchProc(cmdLine, "", voiceDir);
		}
		if (Integer.parseInt(getProp(LF0)) == 1) {
			cmdLine = "cd " + voiceDir + "hts/data\nmake lf0\n";
			General.launchBatchProc(cmdLine, "", voiceDir);
		}
		if (Integer.parseInt(getProp(STR)) == 1) {
			cmdLine = "cd " + voiceDir + "hts/data\nmake str-mary\n";
			General.launchBatchProc(cmdLine, "", voiceDir);
		}
		if (Integer.parseInt(getProp(CMPMARY)) == 1) {
			// Check, at least, that there is data in the directories mgc, lf0 and str
			System.out.println("\nConcatenating mgc, lf0 and str data:");
			File dirMgc = new File(voiceDir + "hts/data/mgc");
			if (dirMgc.exists() && dirMgc.list().length > 0) {
				System.out.println(voiceDir + "hts/data/mgc contains files");
			} else {
				throw new Exception("Error: directory " + voiceDir + "hts/data/mgc "
						+ " does not exist or does not contain data files.\n"
						+ "These data files can be generated setting the option HMMVoiceMakeData.makeMGC = 1");
			}
			File dirLf0 = new File(voiceDir + "hts/data/lf0");
			if (dirLf0.exists() && dirLf0.list().length > 0) {
				System.out.println(voiceDir + "hts/data/lf0 contains files");
			} else {
				throw new Exception("Error: directory " + voiceDir + "hts/data/lf0 "
						+ " does not exist or does not contain files.\n"
						+ "These data files can be generated setting the option HMMVoiceMakeData.makeLF0 = 1");
			}
			File dirStr = new File(voiceDir + "hts/data/str");
			if (dirStr.exists() && dirStr.list().length > 0) {
				System.out.println(voiceDir + "hts/data/str contains files");
			} else {
				throw new Exception("Error: directory " + voiceDir + "hts/data/str "
						+ " does not exist or does not contain files.\n"
						+ "These data files can be generated setting the option HMMVoiceMakeData.makeSTR = 1");
			}

			// If the directories at leas contain files
			cmdLine = "cd " + voiceDir + "hts/data\nmake cmp-mary\n";
			General.launchBatchProc(cmdLine, "", voiceDir);
		}

		featFileFilter = new FilenameFilter() {
			public boolean accept(File dir, String fileName) {
				return fileName.endsWith(".pfeats");
			}
		};

		if (Integer.parseInt(getProp(LABELMARY)) == 1) {
			// uses: contextFile (example)
			// featureListFile
			if (getProp(ADAPTSCRIPTS).contentEquals("false"))
				makeLabels(voiceDir);
			else
				makeLabelsAdapt(voiceDir);
		}
		if (Integer.parseInt(getProp(QUESTIONSMARY)) == 1) {
			// uses: questionsFile
			// contextFile (example)
			// featureListFile
			makeQuestions(voiceDir);
		}
		if (Integer.parseInt(getProp(LIST)) == 1) {
			cmdLine = "cd " + voiceDir + "hts/data\nmake list\n";
			General.launchBatchProc(cmdLine, "", voiceDir);
		}
		if (Integer.parseInt(getProp(SCP)) == 1) {
			cmdLine = "cd " + voiceDir + "hts/data\nmake scp\n";
			General.launchBatchProc(cmdLine, "", voiceDir);
		}

		/* delete the temporary file */
		File tmpBatch = new File(voiceDir + "tmp.bat");
		tmpBatch.delete();

		return true;

	}

	/***
	 * This function checks if replacements or aliases for tricky phones are necessary (so HTK-HHEd can handle the phone names),
	 * if so it will create a trickyFile containing the replacements. This file should be used afterwards to create the
	 * PhoneTranslator object used in makeLabels, makeQuestions and JoinModelller. Also it is necessary when loading the HTS trees
	 * to replace back the tricky phones. If a trickyFile is created when training a voice, the tricky file name will be included
	 * in the configuration file of the voice. CHECK not sure how/where to keep this file for the JoinModeller?
	 * 
	 * @param allophoneSet
	 *            allophonesFile for the voice or language (full path).
	 * @param trickyFile
	 *            name of the file where the tricky phone replacements are saved (full path).
	 * @return true if trickyPhones.txt file is created, false otherwise.
	 */
	public static boolean checkTrickyPhones(AllophoneSet allophoneSet, String trickyFile) {

		boolean trickyPhones = false;
		try {

			String lang = allophoneSet.getLocale().getLanguage();

			System.out.println("Checking if there are tricky phones (problematic phone names):");
			FileWriter outputStream = null;
			String phonOri;
			Set<String> phonesList = allophoneSet.getAllophoneNames();
			// Left-righ phone ID context questions
			Iterator<String> it = phonesList.iterator();
			int numReplacements = 0;
			while (it.hasNext()) {
				phonOri = it.next();
				// System.out.println("  phon=" + phonOri);
				for (int i = 0; i < phonOri.length(); i++) {
					if (!(marytts.util.string.StringUtils.isLetterOrModifier(phonOri.codePointAt(i)))
					// if this phone is not lower-case and differs from another phone only in case, it should also be considered
					// "tricky":
							|| (!phonOri.equals(phonOri.toLowerCase()) && phonesList.contains(phonOri.toLowerCase()))) {
						if (numReplacements == 0) {
							// just if there is replacements to make then create the trickyPhones.txt file
							outputStream = new FileWriter(trickyFile);
							trickyPhones = true;
						}
						System.out.println("  phon=" + phonOri + "  replace --> " + lang + numReplacements);
						if (outputStream != null)
							outputStream.write(phonOri + " " + lang + numReplacements + "\n");
						numReplacements++;
						break;
					}
				}
			}
			if (outputStream != null) {
				outputStream.close();
				System.out.println("Created tricky phones file: " + trickyFile);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return trickyPhones;
	}

	/***
	 * Java version of the makeQuestions script (hts/data/scripts/make_questions.pl) uses: questionsFile contextFile
	 * featureListFile
	 * 
	 * @param voiceDir
	 *            voiceDir
	 * @throws Exception
	 *             Exception
	 */
	private void makeQuestions(String voiceDir) throws Exception {

		System.out.println("\n Making questions:");
		String hmmFeatureListFile = voiceDir + getProp(featureListFile);

		// check first if questions directory exists
		String dirNameQuestions = marytts.util.string.StringUtils.getFolderName(voiceDir + getProp(questionsFile));
		File dirQuestions = new File(dirNameQuestions);
		if (!dirQuestions.exists())
			dirQuestions.mkdir();

		FileWriter out = new FileWriter(voiceDir + getProp(questionsFile));
		int i, j;
		String phon;

		// Check if there are tricky phones
		PhoneTranslator phTranslator;
		if (checkTrickyPhones(db.getAllophoneSet(), voiceDir + getProp(trickyPhonesFile)))
			phTranslator = new PhoneTranslator(new FileInputStream(voiceDir + getProp(trickyPhonesFile)));
		else
			phTranslator = new PhoneTranslator(null);

		System.out.println("Generating questions file: " + voiceDir + getProp(questionsFile));

		// Get feature definition, whatever context feature file used for training can be used here.
		// select a featureListFile from phonefeatures, it will be used to get the feature definition
		FeatureDefinition feaDef;
		String feaExample;
		File dirFea = new File(db.getProp(DatabaseLayout.PHONEFEATUREDIR));
		String[] dirFeaList;
		if (getProp(ADAPTSCRIPTS).contentEquals("false")) {
			dirFeaList = dirFea.list(featFileFilter);
			if (dirFea.exists() && dirFeaList.length > 0) {
				feaExample = db.getProp(DatabaseLayout.PHONEFEATUREDIR) + "/" + dirFeaList[0];
				System.out.println("phonefeatures file example for getting featureDefition = " + feaExample);
			} else {
				throw new IOException(
						"HMMVoiceMakeData: problem getting phonefeatures file example for extracting featureDefition,"
								+ " check phonefeatures directory, it seems empty!");
			}
		} else {
			dirFeaList = dirFea.list();
			File dirFeaSpeaker = new File(db.getProp(DatabaseLayout.PHONEFEATUREDIR) + "/" + dirFeaList[0]);
			String[] dirFeaListSpeaker = dirFeaSpeaker.list(featFileFilter);
			if (dirFeaSpeaker.exists() && dirFeaListSpeaker.length > 0) {
				feaExample = db.getProp(DatabaseLayout.PHONEFEATUREDIR) + "/" + dirFeaList[0] + "/" + dirFeaListSpeaker[0];
				System.out.println("phonefeatures file example for getting featureDefition = " + feaExample);
			} else {
				throw new IOException(
						"HMMVoiceMakeData: problem getting phonefeatures file example for extracting featureDefition,"
								+ " check phonefeatures directory, it seems empty!");
			}
		}
		Scanner context = new Scanner(new BufferedReader(new FileReader(feaExample)));
		String strContext = "";
		System.out.println("FeatureDefinition extracted from context file example: " + feaExample);
		while (context.hasNext()) {
			strContext += context.nextLine();
			strContext += "\n";
		}
		context.close();
		feaDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);

		// list of additional context features used for training
		// Since the features are provided by the user, it should be checked that the feature exist
		// Set <String> featureList = new HashSet<String>();
		Map<String, String> hmmFeatureList = new HashMap<String, String>();
		Scanner feaList = new Scanner(new BufferedReader(new FileReader(hmmFeatureListFile)));
		String fea, aliasFea;
		String prefix = "f";
		int numFea = 0;
		System.out.println("The following are other context features used for training HMMs, they are extracted from file: "
				+ hmmFeatureListFile);
		while (feaList.hasNext()) {
			fea = feaList.nextLine();
			if (fea.trim().length() == 0) {
				break;
			}
			// Check if the feature exist
			if (feaDef.hasFeature(fea)) {
				aliasFea = prefix + Integer.toString(numFea);
				hmmFeatureList.put(fea, aliasFea);
				System.out.println("  Added to featureList = " + fea + "  " + aliasFea);
				numFea++;
			} else {
				throw new Exception("Error: feature \"" + fea + "\" in feature list file: " + hmmFeatureListFile
						+ " does not exist in FeatureDefinition.");
			}
		}
		feaList.close();

		// Get possible values of phonological features, and initialise a set of phones
		// that have that value (new HashSet<String>)
		HashMap<String, Set<String>> mary_vc = new HashMap<String, Set<String>>();
		HashMap<String, Set<String>> mary_vlng = new HashMap<String, Set<String>>();
		HashMap<String, Set<String>> mary_vheight = new HashMap<String, Set<String>>();
		HashMap<String, Set<String>> mary_vfront = new HashMap<String, Set<String>>();
		HashMap<String, Set<String>> mary_vrnd = new HashMap<String, Set<String>>();
		HashMap<String, Set<String>> mary_ctype = new HashMap<String, Set<String>>();
		HashMap<String, Set<String>> mary_cplace = new HashMap<String, Set<String>>();
		HashMap<String, Set<String>> mary_cvox = new HashMap<String, Set<String>>();
		String val_vc[] = null;
		String val_vlng[] = null;
		String val_vheight[] = null;
		String val_vfront[] = null;
		String val_vrnd[] = null;
		String val_ctype[] = null;
		String val_cplace[] = null;
		String val_cvox[] = null;

		// mary_vc
		if (feaDef.hasFeature("ph_vc")) {
			val_vc = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vc"));
			for (i = 0; i < val_vc.length; i++)
				mary_vc.put(val_vc[i], new HashSet<String>());
		}
		// mary_vlng
		if (feaDef.hasFeature("ph_vlng")) {
			val_vlng = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vlng"));
			for (i = 0; i < val_vlng.length; i++)
				mary_vlng.put(val_vlng[i], new HashSet<String>());
		}

		// mary_vheight
		if (feaDef.hasFeature("ph_vheight")) {
			val_vheight = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vheight"));
			for (i = 0; i < val_vheight.length; i++)
				mary_vheight.put(val_vheight[i], new HashSet<String>());
		}

		// mary_vfront
		if (feaDef.hasFeature("ph_vfront")) {
			val_vfront = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vfront"));
			for (i = 0; i < val_vfront.length; i++)
				mary_vfront.put(val_vfront[i], new HashSet<String>());
		}

		// mary_vrnd
		if (feaDef.hasFeature("ph_vrnd")) {
			val_vrnd = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_vrnd"));
			for (i = 0; i < val_vrnd.length; i++)
				mary_vrnd.put(val_vrnd[i], new HashSet<String>());
		}

		// mary_ctype
		if (feaDef.hasFeature("ph_ctype")) {
			val_ctype = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_ctype"));
			for (i = 0; i < val_ctype.length; i++)
				mary_ctype.put(val_ctype[i], new HashSet<String>());
		}

		// mary_cplace
		if (feaDef.hasFeature("ph_cplace")) {
			val_cplace = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_cplace"));
			for (i = 0; i < val_cplace.length; i++)
				mary_cplace.put(val_cplace[i], new HashSet<String>());
		}

		// mary_cvox
		if (feaDef.hasFeature("ph_cvox")) {
			val_cvox = feaDef.getPossibleValues(feaDef.getFeatureIndex("ph_cvox"));
			for (i = 0; i < val_cvox.length; i++)
				mary_cvox.put(val_cvox[i], new HashSet<String>());
		}

		AllophoneSet allophoneSet = db.getAllophoneSet();

		String phoneSeq, phonOri;
		Set<String> phonesList = allophoneSet.getAllophoneNames();
		// Left-righ phone ID context questions
		Iterator<String> it = phonesList.iterator();
		while (it.hasNext()) {
			phonOri = it.next();
			phon = phTranslator.replaceTrickyPhones(phonOri);
			out.write("");
			out.write("QS \"prev_prev_phone=" + phon + "\"\t{" + phon + "^*}\n");
			out.write("QS \"prev_phone=" + phon + "\"\t\t{*^" + phon + "-*}\n");
			out.write("QS \"phone=" + phon + "\"\t\t\t{*-" + phon + "+*}\n");
			out.write("QS \"next_phone=" + phon + "\"\t\t{*+" + phon + "=*}\n");
			out.write("QS \"next_next_phone=" + phon + "\"\t{*=" + phon + "||*}\n");
			out.write("\n");

			// Get the phonological value of each phone, and add it to the corresponding
			// set of phones that have that value.
			// System.out.println(phon + " vc = " + allophoneSet.getPhoneFeature(phonOri, "vc"));
			if (allophoneSet.getPhoneFeature(phonOri, "vc") != null)
				mary_vc.get(allophoneSet.getPhoneFeature(phonOri, "vc")).add(phon);

			// System.out.println(phon + " vlng = " + allophoneSet.getPhoneFeature(phonOri, "vlng"));
			if (allophoneSet.getPhoneFeature(phonOri, "vlng") != null)
				mary_vlng.get(allophoneSet.getPhoneFeature(phonOri, "vlng")).add(phon);

			// System.out.println(phon + " vheight = " + allophoneSet.getPhoneFeature(phonOri, "vheight"));
			if (allophoneSet.getPhoneFeature(phonOri, "vheight") != null)
				mary_vheight.get(allophoneSet.getPhoneFeature(phonOri, "vheight")).add(phon);

			// System.out.println(phon + " vfront = " + allophoneSet.getPhoneFeature(phonOri, "vfront"));
			if (allophoneSet.getPhoneFeature(phonOri, "vfront") != null)
				mary_vfront.get(allophoneSet.getPhoneFeature(phonOri, "vfront")).add(phon);

			// System.out.println(phon + " vrnd = " + allophoneSet.getPhoneFeature(phonOri, "vrnd"));
			if (allophoneSet.getPhoneFeature(phonOri, "vrnd") != null)
				mary_vrnd.get(allophoneSet.getPhoneFeature(phonOri, "vrnd")).add(phon);

			// System.out.println(phon + " ctype = " + allophoneSet.getPhoneFeature(phonOri, "ctype"));
			if (allophoneSet.getPhoneFeature(phonOri, "ctype") != null)
				mary_ctype.get(allophoneSet.getPhoneFeature(phonOri, "ctype")).add(phon);

			// System.out.println(phon + " cplace = " + allophoneSet.getPhoneFeature(phonOri, "cplace"));
			if (allophoneSet.getPhoneFeature(phonOri, "cplace") != null)
				mary_cplace.get(allophoneSet.getPhoneFeature(phonOri, "cplace")).add(phon);

			// System.out.println(phon + " cvox = " + allophoneSet.getPhoneFeature(phonOri, "cvox"));
			if (allophoneSet.getPhoneFeature(phonOri, "cvox") != null)
				mary_cvox.get(allophoneSet.getPhoneFeature(phonOri, "cvox")).add(phon);

		}

		// phonological features questions
		// String val, prev_prev, prev, ph, next, next_next;
		out.write("\n");
		if (feaDef.hasFeature("ph_vc"))
			writePhonologicalFeatures("vc", val_vc, mary_vc, out);
		if (feaDef.hasFeature("ph_vlng"))
			writePhonologicalFeatures("vlng", val_vlng, mary_vlng, out);
		if (feaDef.hasFeature("ph_vheight"))
			writePhonologicalFeatures("vheight", val_vheight, mary_vheight, out);
		if (feaDef.hasFeature("ph_vfront"))
			writePhonologicalFeatures("vfront", val_vfront, mary_vfront, out);
		if (feaDef.hasFeature("ph_vrnd"))
			writePhonologicalFeatures("vrnd", val_vrnd, mary_vrnd, out);
		if (feaDef.hasFeature("ph_ctype"))
			writePhonologicalFeatures("ctype", val_ctype, mary_ctype, out);
		if (feaDef.hasFeature("ph_cplace"))
			writePhonologicalFeatures("cplace", val_cplace, mary_cplace, out);
		if (feaDef.hasFeature("ph_cvox"))
			writePhonologicalFeatures("cvox", val_cvox, mary_cvox, out);

		// Questions for other features, the additional features used for training.
		String hmmfea, hmmfeaAlias;
		int hmmfeaValInt;
		Iterator ite = hmmFeatureList.entrySet().iterator();
		while (ite.hasNext()) {
			Map.Entry pairs = (Map.Entry) ite.next();
			hmmfea = (String) pairs.getKey();
			hmmfeaAlias = (String) pairs.getValue();
			String val_fea[] = feaDef.getPossibleValues(feaDef.getFeatureIndex(hmmfea));
			// write the feature value as string
			for (i = 0; i < val_fea.length; i++) {
				if (hmmfea.contains("sentence_punc") || hmmfea.contains("prev_punctuation")
						|| hmmfea.contains("next_punctuation"))
					out.write("QS \"" + hmmfeaAlias + "=" + phTranslator.replacePunc(val_fea[i]) + "\" \t{*|" + hmmfeaAlias + "="
							+ phTranslator.replacePunc(val_fea[i]) + "|*}\n");
				else if (hmmfea.contains("tobi_"))
					out.write("QS \"" + hmmfeaAlias + "=" + phTranslator.replaceToBI(val_fea[i]) + "\" \t{*|" + hmmfeaAlias + "="
							+ phTranslator.replaceToBI(val_fea[i]) + "|*}\n");
				else
					out.write("QS \"" + hmmfeaAlias + "=" + val_fea[i] + "\" \t{*|" + hmmfeaAlias + "=" + val_fea[i] + "|*}\n");
			}
			out.write("\n");
		}

		out.close();
		System.out.println("Created question file: " + voiceDir + getProp(questionsFile) + "\n");

		// we need to put in this file questions for number of syls and words in a Utterance and num phrases in a Utterance.
		// in Mary:
		// one sentence can contain one or more phrases
		// one phrase contains one or more words
		// HTS-festival Mary
		// Num-Syls_in_Utterance -> (not available, can be added in: marytts.features and the feature processors in there...)
		// Num-Words_in_Utterance -> sentence_numwords
		// Num-Phrases_in_Utterance -> sentence_numphrases
		// phrase_numsyls
		// phrase_numwords
		FileWriter outUtt = new FileWriter(voiceDir + getProp(questionsUttFile));
		System.out.println("Generating questions utterance file for GV: " + voiceDir + getProp(questionsUttFile));
		System.out.println("Features used:");
		String hmmFeasForGv[] = { "phrase_numsyls", "phrase_numwords", "sentence_numwords", "sentence_numphrases" };

		// the folowing in case we need to add alias for the hmmFeasFor Gv
		int numFeaGv = 0;
		String prefixGv = "fgv";
		String hmmFeatureListMapFile = voiceDir + getProp(featureListMapFile);
		FileWriter outFeaMap = new FileWriter(hmmFeatureListMapFile, true);

		for (i = 0; i < hmmFeasForGv.length; i++) {
			fea = hmmFeasForGv[i];
			System.out.println("  " + fea);
			hmmfeaAlias = hmmFeatureList.get(fea);
			// if the feature for utterance question is NOT in the hmmFeatureList, then there is not yet an alias for this feature
			// here we will added first to the hmmFeatureList assigning an alias
			if (hmmfeaAlias == null) {
				// Check if the feature exist
				if (feaDef.hasFeature(fea)) {
					hmmfeaAlias = prefixGv + Integer.toString(numFeaGv);
					hmmFeatureList.put(fea, hmmfeaAlias);
					System.out.println("  Added to featureList = " + fea + "  " + hmmfeaAlias);
					numFeaGv++;
					outFeaMap.append(fea + " " + hmmfeaAlias + "\n");
				} else {
					throw new Exception("Error: feature \"" + fea + "\" in feature list file: " + hmmFeatureListFile
							+ " does not exist in FeatureDefinition.");
				}
			}
			String val_fea[] = feaDef.getPossibleValues(feaDef.getFeatureIndex(fea));
			// write the feature value as string
			for (j = 0; j < val_fea.length; j++) {
				outUtt.write("QS \"" + hmmfeaAlias + "=" + val_fea[j] + "\" \t{*|" + hmmfeaAlias + "=" + val_fea[j] + "|*}\n");
			}
			outUtt.write("\n");
		}
		outUtt.close();
		outFeaMap.close();
		System.out.println("Created question file for GV: " + voiceDir + getProp(questionsUttFile) + "\n");
	}

	private void writePhonologicalFeatures(String fea, String fval[], HashMap<String, Set<String>> mary_fea, FileWriter out)
			throws Exception {
		String val, prev_prev, prev, ph, next, next_next;
		for (int i = 0; i < fval.length; i++) {
			prev_prev = "QS \"prev_prev_" + fea + "=" + fval[i] + "\"\t\t{";
			prev = "QS \"prev_" + fea + "=" + fval[i] + "\"\t\t{";
			ph = "QS \"ph_" + fea + "=" + fval[i] + "\"\t\t\t{";
			next = "QS \"next_" + fea + "=" + fval[i] + "\"\t\t{";
			next_next = "QS \"next_next_" + fea + "=" + fval[i] + "\"\t\t{";
			Iterator<String> it = mary_fea.get(fval[i]).iterator();
			while (it.hasNext()) {
				val = it.next();
				prev_prev += val + "^*,";
				prev += "*^" + val + "-*,";
				ph += "*-" + val + "+*,";
				next += "*+" + val + "=*,";
				next_next += "*=" + val + "||*,";
			}
			// remove last unnecessary comma, and close curly brackets
			out.write(prev_prev.substring(0, prev_prev.lastIndexOf(",")) + "}\n");
			out.write(prev.substring(0, prev.lastIndexOf(",")) + "}\n");
			out.write(ph.substring(0, ph.lastIndexOf(",")) + "}\n");
			out.write(next.substring(0, next.lastIndexOf(",")) + "}\n");
			out.write(next_next.substring(0, next_next.lastIndexOf(",")) + "}\n");
			out.write("\n");
		}

	}

	/***
	 * Java version of the make labels script (hts/data/scripts/make_labels.pl) uses:
	 * 
	 * @param voiceDir
	 *            voiceDir
	 * @throws Exception
	 *             Exception
	 */
	private void makeLabels(String voiceDir) throws Exception {

		System.out.println("\n Making labels:");
		String hmmFeatureListFile = voiceDir + getProp(featureListFile);
		String hmmFeatureListMapFile = voiceDir + getProp(featureListMapFile);
		File dirFea = new File(voiceDir + "/phonefeatures");
		File dirLab = new File(voiceDir + "/phonelab");

		String[] feaFiles;
		if (dirFea.exists() && dirFea.list().length > 0 && dirLab.exists() && dirLab.list().length > 0) {
			feaFiles = dirFea.list(featFileFilter);
		} else {
			throw new Exception("Error: directories " + voiceDir + "/phonefeatures and/or " + voiceDir
					+ "/phonelab do not contain files.");
		}

		// Check if there are tricky phones
		PhoneTranslator phTranslator;
		if (checkTrickyPhones(db.getAllophoneSet(), voiceDir + getProp(trickyPhonesFile)))
			phTranslator = new PhoneTranslator(new FileInputStream(voiceDir + getProp(trickyPhonesFile)));
		else
			phTranslator = new PhoneTranslator(null);

		// Get feature definition, whatever context feature file used for training can be passed here.
		// here we take the first in the feaFiles list.
		Scanner context = new Scanner(new BufferedReader(new FileReader(voiceDir + "/phonefeatures/" + feaFiles[0])));
		String strContext = "";
		System.out.println("FeatureDefinition extracted from context file: " + voiceDir + "/phonefeatures/" + feaFiles[0]);
		while (context.hasNext()) {
			strContext += context.nextLine();
			strContext += "\n";
		}
		context.close();
		FeatureDefinition feaDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);

		// list of context features used for creating HTS context features --> features used for training HMMs
		// Since the features are provided by the user, it should be checked that the features exist
		Map<String, String> hmmFeatureList = new HashMap<String, String>();
		FileWriter outFeaMap = new FileWriter(hmmFeatureListMapFile);

		Scanner feaList = new Scanner(new BufferedReader(new FileReader(hmmFeatureListFile)));
		String fea, feaAlias;
		System.out.println("The following are other context features used for training Hmms: ");
		String prefix = "f";
		int numFea = 0;
		while (feaList.hasNext()) {
			fea = feaList.nextLine();
			if (fea.trim().length() == 0) {
				break;
			}
			// Check if the feature exist
			if (feaDef.hasFeature(fea)) {
				feaAlias = prefix + Integer.toString(numFea);
				hmmFeatureList.put(fea, feaAlias);
				System.out.println("  " + fea + "  " + feaAlias);
				// we need to keep this maping in a file
				outFeaMap.write(fea + " " + feaAlias + "\n");
				numFea++;
			} else
				throw new Exception("Error: feature \"" + fea + "\" in feature list file: " + hmmFeatureListFile
						+ " does not exist in FeatureDefinition.");
		}
		feaList.close();
		outFeaMap.close();
		// Save now features and alias in hmmFeaturesMap, this map should be used again as the trickiPhones, when reading the
		// trees

		System.out.println("The previous context features were extracted from file: " + hmmFeatureListFile);

		// Process all the files in phonefeatures and phonelab and create the directories:
		// hts/data/labels/full
		// hts/data/labels/mono
		// hts/data/labels/gen (contains some examples from full, here we copy 10 examples)
		// Create also the HTK master label files: full.mlf and mono.mlf
		File labelsDir = new File(voiceDir + "/hts/data/labels");
		if (!labelsDir.exists())
			labelsDir.mkdir();
		File monoDir = new File(voiceDir + "/hts/data/labels/mono");
		if (!monoDir.exists()) {
			System.out.println("\nCreating a /hts/data/labels/mono directory");
			monoDir.mkdir();
		}
		File fullDir = new File(voiceDir + "/hts/data/labels/full");
		if (!fullDir.exists()) {
			System.out.println("\nCreating a /hts/data/labels/full directory");
			fullDir.mkdir();
		}
		File genDir = new File(voiceDir + "/hts/data/labels/gen");
		if (!genDir.exists()) {
			System.out
					.println("\nCreating a /hts/data/labels/gen directory, copying some HTS-HTK full context examples for testing");
			genDir.mkdir();
		}

		// Process all the files in phonefeatures and phonelab and create HTS-HTK full context feature files and label files.
		String basename;
		for (int i = 0; (i < feaFiles.length); i++) {
			basename = StringUtils.getFileName(feaFiles[i]);
			System.out.println("Extracting monophone and context features (" + (i + 1) + "): " + feaFiles[i] + " and " + basename
					+ ".lab");
			extractMonophoneAndFullContextLabels(voiceDir + "/phonefeatures/" + feaFiles[i], voiceDir + "/phonelab/" + basename
					+ ".lab", voiceDir + "/hts/data/labels/full/" + basename + ".lab", voiceDir + "/hts/data/labels/mono/"
					+ basename + ".lab", feaDef, phTranslator, hmmFeatureList);
		}
		System.out.println("Processed " + feaFiles.length + " files.");
		System.out.println("Created directories: \n  " + voiceDir + "hts/data/labels/full/" + "\n  " + voiceDir
				+ "hts/data/labels/mono/");

		// creating Master label files:
		FileWriter fullMlf = new FileWriter(voiceDir + "/hts/data/labels/full.mlf");
		fullMlf.write("#!MLF!#\n");
		fullMlf.write("\"*/*.lab\" -> \"" + voiceDir + "hts/data/labels/full\"\n");
		fullMlf.close();

		FileWriter monoMlf = new FileWriter(voiceDir + "/hts/data/labels/mono.mlf");
		monoMlf.write("#!MLF!#\n");
		monoMlf.write("\"*/*.lab\" -> \"" + voiceDir + "hts/data/labels/mono\"\n");
		monoMlf.close();

		System.out.println("Created Master Label Files: \n  " + voiceDir + "hts/data/labels/full.mlf" + "\n  " + voiceDir
				+ "hts/data/labels/mono.mlf");

		// Copy 10 files in gen directory to test with htsengine
		System.out.println("Copying 10 context feature files in gen directory for testing with the HTS htsengine.");
		String cmdLine;
		for (int i = 0; i < 10; i++) {
			basename = StringUtils.getFileName(feaFiles[i]);
			FileUtils.copy(voiceDir + "hts/data/labels/full/" + basename + ".lab", voiceDir + "hts/data/labels/gen/gen_"
					+ basename + ".lab");
		}

	}

	/***
	 * Java version of the make labels script (hts/data/scripts/make_labels.pl) uses:
	 * 
	 * @param voiceDir
	 *            voiceDir
	 * @throws Exception
	 *             Exception
	 */
	private void makeLabelsAdapt(String voiceDir) throws Exception {

		System.out.println("\n Making labels:");
		String hmmFeatureListFile = voiceDir + getProp(featureListFile);
		String hmmFeatureListMapFile = voiceDir + getProp(featureListMapFile);

		// Check if there are tricky phones
		PhoneTranslator phTranslator;
		if (checkTrickyPhones(db.getAllophoneSet(), voiceDir + getProp(trickyPhonesFile)))
			phTranslator = new PhoneTranslator(new FileInputStream(voiceDir + getProp(trickyPhonesFile)));
		else
			phTranslator = new PhoneTranslator(null);

		// Get the speakers directories
		File dirSpeakersFea = new File(voiceDir + "/phonefeatures");
		File dirSpeakersLab = new File(voiceDir + "/phonelab");

		String[] speakers;
		if (dirSpeakersFea.exists() && dirSpeakersFea.list().length > 0 && dirSpeakersLab.exists()
				&& dirSpeakersLab.list().length > 0) {
			speakers = dirSpeakersFea.list();
		} else {
			throw new Exception("Error: directories " + voiceDir + "phonefeatures and/or " + voiceDir
					+ "phonelab do not contain files.");
		}

		// Get feature definition, whatever context feature file used for training can be passed here.
		// here we take the first in the first speaker feaFiles list.
		File dirFeaSpeaker = new File(voiceDir + "/phonefeatures/" + speakers[0]);
		String[] feaFilesSpeaker;
		if (dirFeaSpeaker.exists() && dirFeaSpeaker.list().length > 0) {
			feaFilesSpeaker = dirFeaSpeaker.list();
		} else {
			throw new Exception("Error: directory " + voiceDir + "/phonefeatures/" + speakers[0] + " does not contain files.");
		}

		Scanner context = new Scanner(new BufferedReader(new FileReader(voiceDir + "/phonefeatures/" + speakers[0] + "/"
				+ feaFilesSpeaker[0])));
		String strContext = "";
		System.out.println("FeatureDefinition extracted from context file: " + voiceDir + "/phonefeatures/" + speakers[0] + "/"
				+ feaFilesSpeaker[0]);
		while (context.hasNext()) {
			strContext += context.nextLine();
			strContext += "\n";
		}
		context.close();
		FeatureDefinition feaDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);

		// list of context features used for creating HTS context features --> features used for training HMMs
		// Since the features are provided by the user, it should be checked that the features exist
		Map<String, String> hmmFeatureList = new HashMap<String, String>();
		FileWriter outFeaMap = new FileWriter(hmmFeatureListMapFile);

		Scanner feaList = new Scanner(new BufferedReader(new FileReader(hmmFeatureListFile)));
		String fea, feaAlias;
		System.out.println("The following are other context features used for training Hmms: ");
		String prefix = "f";
		int numFea = 0;
		while (feaList.hasNext()) {
			fea = feaList.nextLine();
			if (fea.trim().length() == 0) {
				break;
			}
			// Check if the feature exist
			if (feaDef.hasFeature(fea)) {
				feaAlias = prefix + Integer.toString(numFea);
				hmmFeatureList.put(fea, feaAlias);
				System.out.println("  " + fea + "  " + feaAlias);
				// we need to keep this maping in a file
				outFeaMap.write(fea + " " + feaAlias + "\n");
				numFea++;
			} else
				throw new Exception("Error: feature \"" + fea + "\" in feature list file: " + hmmFeatureListFile
						+ " does not exist in FeatureDefinition.");
		}
		feaList.close();
		outFeaMap.close();
		// Save now features and alias in hmmFeaturesMap, this map should be used again as the trickiPhones, when reading the
		// trees
		System.out.println("The previous context features were extracted from file: " + hmmFeatureListFile);

		// With feature definition and HMM feature list, process now all the speakers directories
		// Process all the files in phonefeatures and phonelab for all the speakers and create the directories:
		// hts/data/labels/full
		// hts/data/labels/mono
		// hts/data/labels/gen (contains some examples from full, here we copy 10 examples)
		// Create also the HTK master label files: full.mlf and mono.mlf
		File labelsDir = new File(voiceDir + "/hts/data/labels");
		if (!labelsDir.exists())
			labelsDir.mkdir();
		File monoDir = new File(voiceDir + "/hts/data/labels/mono");
		if (!monoDir.exists()) {
			System.out.println("\nCreating a /hts/data/labels/mono directory");
			monoDir.mkdir();
		}
		File fullDir = new File(voiceDir + "/hts/data/labels/full");
		if (!fullDir.exists()) {
			System.out.println("\nCreating a /hts/data/labels/full directory");
			fullDir.mkdir();
		}
		// gen directory should contain one subdirectory for each voice
		File genDir = new File(voiceDir + "/hts/data/labels/gen");
		if (!genDir.exists()) {
			System.out
					.println("\nCreating a /hts/data/labels/gen directory, copying some HTS-HTK full context examples for testing");
			genDir.mkdir();
		}

		// Create Master label files:
		FileWriter fullMlf = new FileWriter(voiceDir + "/hts/data/labels/full.mlf");
		fullMlf.write("#!MLF!#\n");
		FileWriter monoMlf = new FileWriter(voiceDir + "/hts/data/labels/mono.mlf");
		monoMlf.write("#!MLF!#\n");

		// Process each speaker
		for (int j = 0; j < speakers.length; j++) {

			File dirFea = new File(voiceDir + "/phonefeatures/" + speakers[j]);
			File dirLab = new File(voiceDir + "/phonelab/" + speakers[j]);

			if (dirFea.exists() && dirFea.list().length > 0 && dirLab.exists() && dirLab.list().length > 0) {
				feaFilesSpeaker = dirFea.list();
			} else {
				throw new Exception("Error: directories " + voiceDir + "/phonefeatures/" + speakers[j] + " and/or " + voiceDir
						+ "/phonelab/" + speakers[j] + " do not contain files.");
			}

			// Create directories in full, mono and gen for each seaker
			File monoDirSpeaker = new File(voiceDir + "/hts/data/labels/mono/" + speakers[j]);
			if (!monoDirSpeaker.exists()) {
				System.out.println("\nCreating a /hts/data/labels/mono/" + speakers[j] + " directory");
				monoDirSpeaker.mkdir();
			}
			File fullDirSpeaker = new File(voiceDir + "/hts/data/labels/full/" + speakers[j]);
			if (!fullDirSpeaker.exists()) {
				System.out.println("\nCreating a /hts/data/labels/full/" + speakers[j] + " directory");
				fullDirSpeaker.mkdir();
			}
			File genDirSpeaker = new File(voiceDir + "/hts/data/labels/gen/" + speakers[j]);
			if (!genDirSpeaker.exists()) {
				System.out.println("\nCreating a /hts/data/labels/gen" + speakers[j]
						+ " directory, copying some HTS-HTK full context examples for testing");
				genDirSpeaker.mkdir();
			}

			// Process all the files in phonefeatures and phonelab and create HTS-HTK full context feature files and label files.
			String basename;
			for (int i = 0; (i < feaFilesSpeaker.length); i++) {
				basename = StringUtils.getFileName(feaFilesSpeaker[i]);
				System.out.println("Extracting monophone and context features (" + (i + 1) + "): " + feaFilesSpeaker[i] + " and "
						+ basename + ".lab");
				extractMonophoneAndFullContextLabels(voiceDir + "/phonefeatures/" + speakers[j] + "/" + feaFilesSpeaker[i],
						voiceDir + "/phonelab/" + speakers[j] + "/" + basename + ".lab", voiceDir + "/hts/data/labels/full/"
								+ speakers[j] + "/" + basename + ".lab", voiceDir + "/hts/data/labels/mono/" + speakers[j] + "/"
								+ basename + ".lab", feaDef, phTranslator, hmmFeatureList); // CHECK: correct here null should not
																							// be there
			}
			System.out.println("Processed " + feaFilesSpeaker.length + " files.");
			System.out.println("Created directories: \n  " + voiceDir + "hts/data/labels/full/" + speakers[j] + "\n  " + voiceDir
					+ "hts/data/labels/mono/" + speakers[j]);

			// Add speaker directory to Master label files:
			fullMlf.write("\"*/*.lab\" -> \"" + voiceDir + "hts/data/labels/full/" + speakers[j] + "\"\n");
			monoMlf.write("\"*/*.lab\" -> \"" + voiceDir + "hts/data/labels/mono/" + speakers[j] + "\"\n");

			// Copy 10 files in gen directory to test with htsengine
			System.out.println("Copying 10 context feature files in gen directory for testing with the HTS htsengine.");
			String cmdLine;
			for (int i = 0; i < 10; i++) {
				basename = StringUtils.getFileName(feaFilesSpeaker[i]);
				FileUtils.copy(voiceDir + "hts/data/labels/full/" + speakers[j] + "/" + basename + ".lab", voiceDir
						+ "hts/data/labels/gen/" + speakers[j] + "/" + basename + ".lab");
			}
		}

		fullMlf.close();
		monoMlf.close();
		System.out.println("Created Master Label Files: \n  " + voiceDir + "hts/data/labels/full.mlf" + "\n  " + voiceDir
				+ "hts/data/labels/mono.mlf");
	}

	/**
	 * Java version of the make labels script (hts/data/scripts/make_labels.pl)
	 * 
	 * @param feaFileName
	 *            MARY phonefeatures file name
	 * @param labFileName
	 *            label file name
	 * @param outFeaFileName
	 *            output context features file name in HTS format
	 * @param outLabFileName
	 *            output label file name in HTK format
	 * @param feaDef
	 *            MARY feature definition
	 * @param phTranslator
	 *            phone translator object, including tricky phones if any (file mary/trickyphones.txt).
	 * @param hmmFeatureList
	 *            extra features to train HMMs (file mary/hmmFeatures.txt)
	 * @throws Exception
	 *             Exception
	 */
	private void extractMonophoneAndFullContextLabels(String feaFileName, String labFileName, String outFeaFileName,
			String outLabFileName, FeatureDefinition feaDef, PhoneTranslator phTranslator, Map<String, String> hmmFeatureList)
			throws Exception {

		FileWriter outFea = new FileWriter(outFeaFileName);
		FileWriter outLab = new FileWriter(outLabFileName);

		// Read label file
		UnitLabel ulab[] = UnitLabel.readLabFile(labFileName);
		// for(int i=0; i<ulab.length; i++){
		// System.out.println("start=" + ulab[i].getStartTime() + " end=" + ulab[i].getEndTime() + " " + ulab[i].unitName);
		// }

		// Read context features
		String nextLine;
		FeatureVector fv;

		Scanner sFea = null;
		sFea = new Scanner(new BufferedReader(new FileReader(feaFileName)));

		/* Skip mary context features definition */
		while (sFea.hasNext()) {
			nextLine = sFea.nextLine();
			if (nextLine.trim().equals(""))
				break;
		}
		/* skip until byte values */
		int numFeaVectors = 0;
		while (sFea.hasNext()) {
			nextLine = sFea.nextLine();
			if (nextLine.trim().equals(""))
				break;
			numFeaVectors++;
		}

		if (numFeaVectors != ulab.length)
			throw new Exception("Error: Number of context features in: " + feaFileName
					+ " is not the same as the number of labels in: " + labFileName);
		else {
			/* Parse byte values */
			int i = 0;
			while (sFea.hasNext()) {
				nextLine = sFea.nextLine();
				fv = feaDef.toFeatureVector(0, nextLine);

				// System.out.println("STR: " + nextLine);
				// check if phone name in feature file is the same as in the lab file
				if (ulab[i].unitName.contentEquals(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef))) {
					// We need here HTK time units, which are measured in hundreds of nanoseconds.
					// write in label file HTK-HTS format
					outLab.write(ulab[i].startTime * 1E7 + "  " + ulab[i].endTime * 1E7 + " "
							+ phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef))
							+ "\n");

					// write in features file HTK-HTS format
					outFea.write(ulab[i].startTime
							* 1E7
							+ "  "
							+ ulab[i].endTime
							* 1E7
							+ " "
							+ phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("prev_prev_phone"),
									feaDef))
							+ "^"
							+ phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("prev_phone"), feaDef))
							+ "-"
							+ phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef))
							+ "+"
							+ phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("next_phone"), feaDef))
							+ "="
							+ phTranslator.replaceTrickyPhones(fv.getFeatureAsString(feaDef.getFeatureIndex("next_next_phone"),
									feaDef)) + "|");

					String hmmfea, hmmfeaVal, hmmfeaAlias;
					int hmmfeaValInt;
					Iterator it = hmmFeatureList.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pairs = (Map.Entry) it.next();
						hmmfea = (String) pairs.getKey();
						hmmfeaAlias = (String) pairs.getValue();
						hmmfeaVal = fv.getFeatureAsString(feaDef.getFeatureIndex(hmmfea), feaDef);
						// If the string is longer than 2048 chars then the feature names need to be shorten
						// (phTranslator.shortenPfeat
						// can be used, but then when reading the HMMs the names have to be lengthen back in the HTSCARTReader).
						// if using punctuation features like: sentence_punc, next_punctuation or prev_punctuation or tobi
						// features
						// the values need to be mapped otherwise HTK-HHEd complains.
						if (hmmfea.contains("sentence_punc") || hmmfea.contains("prev_punctuation")
								|| hmmfea.contains("next_punctuation"))
							outFea.write("|" + hmmfeaAlias + "=" + phTranslator.replacePunc(hmmfeaVal));
						else if (hmmfea.contains("tobi_"))
							outFea.write("|" + hmmfeaAlias + "=" + phTranslator.replaceToBI(hmmfeaVal));
						else
							outFea.write("|" + hmmfeaAlias + "=" + hmmfeaVal);
					}
					outFea.write("||\n");
					i++; // next label
				} else {
					throw new Exception("Phone name mismatch: feature File:"
							+ fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef) + " lab file: " + ulab[i].unitName);
				}
			}
		}
		outLab.close();
		outFea.close();

	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return -1;
	}

	public static void main(String[] args) throws Exception {

		HMMVoiceMakeData data = new HMMVoiceMakeData();
		String voiceDir = "/project/mary/marcela/HMM-voices/turkish/";
		String featuresHmmVoice = "/project/mary/marcela/HMM-voices/turkish/mary/hmmFeatures.txt";
		// data.makeLabels(voiceDir);
		data.makeQuestions(voiceDir);
	}

}
