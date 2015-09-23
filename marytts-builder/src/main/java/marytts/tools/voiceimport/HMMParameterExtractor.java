/**
 * Copyright 2009 DFKI GmbH.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.htsengine.HMMData;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.htsengine.HTSVocoder;
import marytts.modules.HTSEngine;

/**
 * A component for extracting mfccs, lab, state labels, and hmm generated waves (options given to choose)
 * 
 * @author Marcela, Sathish Pammi
 *
 */
public class HMMParameterExtractor extends VoiceImportComponent {

	public final String MARYBASE = "HMMParameterExtractor.maryBase";
	public final String VOICECONFIG = "HMMParameterExtractor.voiceConfigFile";
	public final String VOICENAME = "HMMParameterExtractor.voiceName";
	public final String PHONEFEATS = "HMMParameterExtractor.phonefeaturesDir";
	public final String OUTHMMDIR = "HMMParameterExtractor.outputDir";
	public final String PPARAMETERS = "HMMParameterExtractor.printParameters";
	public final String PLAB = "HMMParameterExtractor.printLab";
	public final String PWAVE = "HMMParameterExtractor.printWave";
	public final String PSLAB = "HMMParameterExtractor.PrintStateLab";
	public final String USEGV = "HMMParameterExtractor.useGV";

	private DatabaseLayout db;
	private String sCostDirectory;
	private String mfccExt = ".mfcc";
	private String pfeatExt = ".pfeats";
	private String slabExt = ".slab";
	private String labExt = ".lab";
	private String wavExt = ".wav";
	private String hplabExt = ".hplab";
	protected int percent = 0;
	private HTSEngine hmm_tts;
	private HMMData htsData;

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(MARYBASE, "Mary Base.  ex: /home/user/MARY400/ ");
		props2Help.put(VOICECONFIG, "locale ex: english");
		props2Help.put(VOICENAME, "Voice name ex: slt-arctic");
		props2Help.put(PHONEFEATS, "Phonefeatures directory");
		props2Help.put(OUTHMMDIR, "Output directory to store hmm generated parameters");
		props2Help.put(PPARAMETERS, "Generate parameters like MFCC, PITCH files?");
		props2Help.put(PLAB, "Generate HMM Label files?");
		props2Help.put(PWAVE, "Generate HMM WAVE files?");
		props2Help.put(PSLAB, "Generate HMM State Label files?");
		props2Help.put(USEGV, "Use Global variance(GV) in parameter generation?");
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(MARYBASE, db.getProp(db.MARYBASE));
			props.put(VOICECONFIG, "english-hsmm-slt.config");
			props.put(VOICENAME, "hsmm-slt");
			props.put(PHONEFEATS, db.getProp(db.ROOTDIR) + File.separator + "phonefeatures");
			props.put(OUTHMMDIR, db.getProp(db.ROOTDIR) + File.separator + "sCost" + File.separator + "hmmparams");
			props.put(PPARAMETERS, "true");
			props.put(PLAB, "true");
			props.put(PWAVE, "false");
			props.put(PSLAB, "false");
			props.put(USEGV, "false");
		}
		return props;
	}

	public final String getName() {
		return "HMMParameterExtractor";
	}

	@Override
	protected void initialiseComp() {
		// sCost dir creation, if doesn't exists
		sCostDirectory = db.getProp(db.ROOTDIR) + File.separator + "sCost";
		File sCostDir = new File(sCostDirectory);
		if (!sCostDir.exists()) {
			System.out.print(sCostDir.getAbsolutePath() + " does not exist; ");
			if (!sCostDir.mkdir()) {
				throw new Error("Could not create " + sCostDir.getAbsolutePath());
			}
			System.out.print("Created successfully.\n");
		}
	}

	public boolean compute() throws Exception {

		// output mfcc dir creator
		File hmmParamDir = new File(getProp(OUTHMMDIR));
		if (!hmmParamDir.exists()) {
			System.out.print(hmmParamDir.getAbsolutePath() + " does not exist; ");
			if (!hmmParamDir.mkdir()) {
				throw new Error("Could not create " + hmmParamDir.getAbsolutePath());
			}
			System.out.print("Created successfully.\n");
		}

		/*
		 * For initialise provide the name of the hmm voice and the name of its configuration file, also indicate the name of your
		 * MARY_BASE directory.
		 */
		String MaryBase = getProp(MARYBASE);
		// String locale = getProp(VOICECONFIG);
		String voice = getProp(VOICENAME);
		String configFile = getProp(VOICECONFIG);
		// directory where the context features of each file are
		String contextFeaDir = getProp(PHONEFEATS);
		// the output dir has to be created already
		String outputDir = getProp(OUTHMMDIR);
		// Create a htsengine object
		hmm_tts = new HTSEngine();

		// Create and set HMMData
		htsData = new HMMData();
		htsData.initHMMData(voice, MaryBase, configFile);

		// Settings for using GV
		if (getProp(USEGV).equals("true")) {
			htsData.setUseGV(true);
		} else {
			htsData.setUseGV(false);
		}

		// Settings for mixed excitation
		htsData.setUseMixExc(true);

		// Now process all files, one by one
		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			generateParameters(bnl.getName(i), contextFeaDir, outputDir);
			boolean success = createHalfPhoneLab(bnl.getName(i));
			if (!success)
				return false;
		}

		return true;
	}

	public int getProgress() {
		return percent;
	}

	/**
	 * Stand alone testing using a TARGETFEATURES list of files as input.
	 * 
	 * @param file
	 *            file
	 * @param contextFeaDir
	 *            contextFeaDir
	 * @param outputDir
	 *            outputDir
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	public void generateParameters(String file, String contextFeaDir, String outputDir) throws IOException, InterruptedException {

		float fperiodmillisec = ((float) htsData.getFperiod() / (float) htsData.getRate()) * 1000;
		float fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());
		/* generate files out of HMMs */
		String feaFile, parFile, durStateFile, durFile, mgcModifiedFile, outWavFile;
		try {

			/*
			 * The utterance model, um, is a Vector (or linked list) of Model objects. It will contain the list of models for the
			 * current label file.
			 */
			HTSUttModel um = new HTSUttModel();
			HTSParameterGeneration pdf2par = new HTSParameterGeneration();
			HTSVocoder par2speech = new HTSVocoder();
			AudioInputStream ais;

			/* Process label file of Mary context features and creates UttModel um. */
			feaFile = contextFeaDir + file + pfeatExt;
			um = hmm_tts.processUttFromFile(feaFile, htsData);

			if (getProp(PLAB).equals("true")) {
				/* save realised durations in a lab file */
				FileWriter outputStream;
				durFile = outputDir + file + labExt; /* realised durations */
				outputStream = new FileWriter(durFile);
				outputStream.write(hmm_tts.getRealisedDurations());
				outputStream.close();
			}
			if (getProp(PSLAB).equals("true")) {
				/* save realised durations at state label in a slab file */
				float totalDur = 0;
				int numStates = htsData.getCartTreeSet().getNumStates();
				durStateFile = outputDir + file + slabExt; /* state level realised durations */
				FileWriter outputStream = new FileWriter(durStateFile);
				outputStream.write("#\n");
				for (int i = 0; i < um.getNumModel(); i++) {
					for (int j = 0; j < numStates; j++) {
						totalDur += (um.getUttModel(i).getDur(j) * fperiodsec);
						if (j < (numStates - 1))
							outputStream.write(totalDur + " 0 " + um.getUttModel(i).getPhoneName() + "\n");
						else
							outputStream.write(totalDur + " 1 " + um.getUttModel(i).getPhoneName() + "\n");
					}
				}
				outputStream.close();
			}

			if (getProp(PPARAMETERS).equals("true")) {
				/* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
				boolean debug = true; /*
									 * with debug=true it saves the generated parameters f0 and mfcc in parFile.f0 and
									 * parFile.mfcc in Mary format.
									 */
				parFile = outputDir + file; /* generated parameters mfcc and f0, Mary format */
				pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);
			}

			if (getProp(PWAVE).equals("true")) {
				/* Synthesize speech waveform, generate speech out of sequence of parameter */
				ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
				outWavFile = outputDir + file + wavExt; /* generated wav file */
				System.out.println("saving to file: " + outWavFile);
				File fileOut = new File(outWavFile);
				if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE, ais)) {
					AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
				}
			}
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		}

	} /* main method */

	private boolean createHalfPhoneLab(String baseName) {
		String hmmDir = getProp(OUTHMMDIR);
		if (!getProp(PLAB).equals("true"))
			return true;

		String labFile = hmmDir + File.separator + baseName + labExt;
		try {
			UnitLabel[] unitLab = UnitLabel.readLabFile(labFile);
			PrintWriter pw = new PrintWriter(new FileWriter(new File(hmmDir + File.separator + baseName + hplabExt)));
			pw.println("#");
			int unitIndex = 0;
			for (int i = 0; i < unitLab.length; i++) {
				double duration = unitLab[i].endTime - unitLab[i].startTime;
				assert duration > 0 : "Duration is not > 0 for phone " + unitLab[i].unitName + " (" + baseName + ")";
				double midTime = unitLab[i].startTime + duration / 2;
				unitIndex++;
				String leftUnit = midTime + " " + unitIndex + " " + unitLab[i].unitName + "_L";
				unitIndex++;
				String rightUnit = unitLab[i].endTime + " " + unitIndex + " " + unitLab[i].unitName + "_R";
				pw.println(leftUnit);
				pw.println(rightUnit);
			}
			pw.flush();
			pw.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
