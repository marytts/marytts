/**
 * Copyright 2010 DFKI GmbH.
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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.machinelearning.SFFS;
import marytts.machinelearning.SoP;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;

/***
 * Modelling duration using Sum of products (SoP) SoP is modelled using multiple linear regression Selection of features is
 * performed with Sequential Floating Forward Search(SFFS):
 * 
 * @author marcela
 */
public class DurationSoPTrainer extends VoiceImportComponent {
	// protected String features;
	protected DatabaseLayout db = null;
	protected int percent = 0;
	protected boolean success = true;
	protected boolean interceptTerm;
	protected boolean logDuration;
	protected int solutionSize;
	protected File unitlabelDir;
	protected File unitfeatureDir;

	private final String name = "DurationSoPTrainer";
	private final String LABELDIR = name + ".labelDir";
	private final String FEATUREDIR = name + ".featureDir";
	private final String FEATUREFILE = name + ".featureFile";
	private final String UNITFILE = name + ".unitFile";
	private final String SOLUTIONSIZE = name + ".solutionSize";
	private final String INTERCEPTTERM = name + ".interceptTerm";
	private final String LOGDURATION = name + ".logDuration";
	private final String DURSOPFILE = name + ".durSopFile";

	public String getName() {
		return name;
	}

	@Override
	protected void initialiseComp() {
		this.unitlabelDir = new File(getProp(LABELDIR));
		this.unitfeatureDir = new File(getProp(FEATUREDIR));
		String rootDir = db.getProp(db.ROOTDIR);
		this.interceptTerm = Boolean.valueOf(getProp(INTERCEPTTERM)).booleanValue();
		this.logDuration = Boolean.valueOf(getProp(LOGDURATION)).booleanValue();
		this.solutionSize = Integer.parseInt(getProp(SOLUTIONSIZE));
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout dbl) {
		this.db = dbl;
		String fileDir = db.getProp(db.FILEDIR);
		if (props == null) {
			props = new TreeMap<String, String>();
			String fileSeparator = System.getProperty("file.separator");
			props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "phonefeatures" + fileSeparator);
			props.put(LABELDIR, db.getProp(db.ROOTDIR) + "phonelab" + fileSeparator);
			props.put(FEATUREFILE, db.getProp(db.FILEDIR) + "phoneFeatures" + db.getProp(db.MARYEXT));
			props.put(UNITFILE, db.getProp(db.FILEDIR) + "phoneUnits" + db.getProp(db.MARYEXT));
			props.put(INTERCEPTTERM, "true");
			props.put(LOGDURATION, "true");
			props.put(SOLUTIONSIZE, "10");
			props.put(DURSOPFILE, fileDir + "dur.sop");

		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(FEATUREDIR, "directory containing the phonefeatures");
		props2Help.put(LABELDIR, "directory containing the phone labels");
		props2Help.put(FEATUREFILE, "file containing all phone units and their target cost features");
		props2Help.put(UNITFILE, "file containing all phone units");
		props2Help.put(INTERCEPTTERM, "whether to include interceptTerm (b0) on the solution equation : b0 + b1X1 + .. bnXn");
		props2Help.put(LOGDURATION, "whether to use log(independent variable)");
		props2Help.put(SOLUTIONSIZE, "size of the solution, number of dependend variables");
		props2Help.put(DURSOPFILE, "file containing the dur SoP model. Will be created by this module");
	}

	protected void setSuccess(boolean val) {
		success = val;
	}

	public boolean compute() throws Exception {

		String durDir = db.getProp(db.TEMPDIR);

		String vowelsFile = durDir + "vowels.feats";
		String consonantsFile = durDir + "consonants.feats";
		String pauseFile = durDir + "pause.feats";

		String[] lingFactorsVowel;
		String[] lingFactorsConsonant;
		String[] lingFactorsPause;

		AllophoneSet allophoneSet = db.getAllophoneSet();

		FeatureFileReader featureFile = FeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
		UnitFileReader unitFile = new UnitFileReader(getProp(UNITFILE));

		FeatureDefinition featureDefinition = featureFile.getFeatureDefinition();
		FeatureVector fv;
		int nUnitsVowel = 0;
		int nUnitsConsonant = 0;
		int nUnitsPause = 0;

		// System.out.println("Feature names: " + featureDefinition.getFeatureNames());
		// select features that will be used as linguistic factors on the regression
		lingFactorsVowel = selectLinguisticFactors(featureDefinition.getFeatureNames(), "Select linguistic factors for vowels:");
		lingFactorsConsonant = selectLinguisticFactors(featureDefinition.getFeatureNames(),
				"Select linguistic factors for consonants:");
		lingFactorsPause = selectLinguisticFactors(featureDefinition.getFeatureNames(), "Select linguistic factors for pause:");

		// the following files contain all the feature files in columns
		PrintWriter toVowelsFile = new PrintWriter(new FileOutputStream(vowelsFile));
		PrintWriter toConsonantsFile = new PrintWriter(new FileOutputStream(consonantsFile));
		PrintWriter toPauseFile = new PrintWriter(new FileOutputStream(pauseFile));

		int k = 0;
		int numVowels = 0;
		int numConsonants = 0;
		int numPause = 0;
		// index of phone
		int phoneIndex = featureDefinition.getFeatureIndex("phone");
		for (int i = 0, len = unitFile.getNumberOfUnits(); i < len; i++) {
			// We estimate that feature extraction takes 1/10 of the total time
			// (that's probably wrong, but never mind)
			percent = 10 * i / len;

			Unit u = unitFile.getUnit(i);
			double dur = u.duration / (float) unitFile.getSampleRate();

			fv = featureFile.getFeatureVector(i);

			// first select pause, then vowell and last consonant phones
			if (fv.getByteFeature(phoneIndex) > 0 && dur >= 0.01) {
				if (allophoneSet.getAllophone(fv.getFeatureAsString(phoneIndex, featureDefinition)).isPause()) {
					for (int j = 0; j < lingFactorsPause.length; j++)
						toPauseFile.print(fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsPause[j])) + " ");
					if (logDuration)
						toPauseFile.println(Math.log(dur)); // last column is the dependent variable, in this case duration
					else
						toPauseFile.println(dur);
					numPause++;
				} else if (allophoneSet.getAllophone(fv.getFeatureAsString(phoneIndex, featureDefinition)).isVowel()) {
					for (int j = 0; j < lingFactorsVowel.length; j++) {
						byte feaVal = fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsVowel[j]));
						toVowelsFile.print(feaVal + " ");
					}
					if (logDuration)
						toVowelsFile.println(Math.log(dur)); // last column is the dependent variable, in this case duration
					else
						toVowelsFile.println(dur);
					numVowels++;
				} else { // everything else will be considered consonant! is this correct?
					for (int j = 0; j < lingFactorsConsonant.length; j++) {
						byte feaVal = fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsConsonant[j]));
						toConsonantsFile.print(feaVal + " ");
					}
					if (logDuration)
						toConsonantsFile.println(Math.log(dur));
					else
						toConsonantsFile.println(dur);
					numConsonants++;
				}
			}
		}
		toVowelsFile.close();
		toConsonantsFile.close();
		toPauseFile.close();
		percent = 10;
		int cols, rows;

		double percentToTrain = 0.7;

		// the final regression will be saved in this file, one line for vowels, one for consonants and another for pause
		PrintWriter toSopFile = new PrintWriter(new FileOutputStream(getProp(DURSOPFILE)));

		// Save first the features definition on the output file
		featureDefinition.writeTo(toSopFile, false);
		toSopFile.println();

		SFFS sffs = new SFFS(solutionSize, interceptTerm, logDuration);

		System.out.println("\n==================================\nProcessing Vowels:");
		SoP sopVowel = new SoP(featureDefinition);
		sffs.trainModel(lingFactorsVowel, vowelsFile, numVowels, percentToTrain, sopVowel);
		toSopFile.println("vowel");
		sopVowel.saveSelectedFeatures(toSopFile);

		System.out.println("\n==================================\nProcessing Consonants:");
		SoP sopConsonant = new SoP(featureDefinition);
		sffs.trainModel(lingFactorsConsonant, consonantsFile, numConsonants, percentToTrain, sopConsonant);
		toSopFile.println("consonant");
		sopConsonant.saveSelectedFeatures(toSopFile);

		System.out.println("\n==================================\nProcessing Pause:");
		SoP sopPause = new SoP(featureDefinition);
		sffs.trainModel(lingFactorsPause, pauseFile, numPause, percentToTrain, sopPause);
		toSopFile.println("pause");
		sopPause.saveSelectedFeatures(toSopFile);

		toSopFile.close();

		percent = 100;

		return true;
	}

	public String[] selectLinguisticFactors(String featureNames, String label) throws IOException {
		String[] lingFactors = null;
		String features = checkFeatureList(featureNames);

		final JFrame frame = new JFrame(label);
		GridBagLayout gridBagLayout = new GridBagLayout();
		GridBagConstraints gridC = new GridBagConstraints();
		frame.getContentPane().setLayout(gridBagLayout);

		final JEditorPane editPane = new JEditorPane();
		editPane.setPreferredSize(new Dimension(500, 500));
		editPane.setText(features);

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSuccess(true);
				frame.setVisible(false);
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSuccess(false);
				frame.setVisible(false);
			}
		});

		gridC.gridx = 0;
		gridC.gridy = 0;
		// resize scroll pane:
		gridC.weightx = 1;
		gridC.weighty = 1;
		gridC.fill = GridBagConstraints.HORIZONTAL;
		JScrollPane scrollPane = new JScrollPane(editPane);
		scrollPane.setPreferredSize(editPane.getPreferredSize());
		gridBagLayout.setConstraints(scrollPane, gridC);
		frame.getContentPane().add(scrollPane);
		gridC.gridy = 1;
		// do not resize buttons:
		gridC.weightx = 0;
		gridC.weighty = 0;
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		gridBagLayout.setConstraints(buttonPanel, gridC);
		frame.getContentPane().add(buttonPanel);
		frame.pack();
		frame.setVisible(true);

		do {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		} while (frame.isVisible());
		frame.dispose();

		if (success) {
			try {
				lingFactors = saveFeatures(editPane.getText());
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new Error("Error defining replacements");
			}
		}
		// return true;
		return lingFactors;
	}

	private String checkFeatureList(String featureNames) throws IOException {
		String featureList = "";
		String recommendedFeatureList = "";
		String feaList[] = featureNames.split(" ");
		String line;

		for (int i = 0; i < feaList.length; i++) {
			line = feaList[i];

			/*
			 * // CHECK: Maybe we need to exclude some features from the selection list??? // The following have variance 0 if(
			 * !(line.contains("style") || line.contains("sentence_punc") || line.contains("next_punctuation") ||
			 * line.contains("prev_punctuation") || line.contains("ph_cplace") || line.contains("ph_cvox") ||
			 * line.contains("ph_vc") || line.contains("onsetcoda") || line.contains("edge") )) {
			 * 
			 * // CHECK: here i am including arbitrarily some.... // put in front the recomended ones:
			 * "ph_vfront","ph_vrnd","position_type","pos_in_syl" if( line.contentEquals("ph_vfront") ||
			 * line.contentEquals("ph_height") || line.contentEquals("ph_vlng") || line.contentEquals("ph_vrnd") ||
			 * line.contentEquals("ph_cplace") || line.contentEquals("ph_ctype") || line.contentEquals("ph_cvox") ||
			 * line.contentEquals("phone") || line.contentEquals("position_type") ) recommendedFeatureList += line + "\n"; else
			 * featureList += line + "\n"; }
			 */
			featureList += line + "\n";
		}
		// return recommendedFeatureList + "\n" + featureList;
		return featureList;
		// return "";

	}

	private String[] saveFeatures(String newFeatures) {
		String fea[] = newFeatures.split("\n");
		String[] lingFactors = new String[fea.length];
		System.out.print("Selected linguistic factors (" + fea.length + "):");
		for (int i = 0; i < fea.length; i++) {
			System.out.print(fea[i] + " ");
			lingFactors[i] = fea[i];
		}
		System.out.println();
		return lingFactors;
	}

	public int getProgress() {
		return percent;
	}

	public static void main(String[] args) throws Exception {
		/*
		 * DurationSoPTrainer sop = new DurationSoPTrainer(); DatabaseLayout db = new DatabaseLayout(sop); sop.compute();
		 */
		String sopFileName = "/project/mary/marcela/UnitSel-voices/slt-arctic/temp/dur.sop";
		// String contextFile = "/project/mary/marcela/UnitSel-voices/slt-arctic/phonefeatures/arctic_a0001.pfeats";
		File sopFile = new File(sopFileName);

		// Read dur.sop file
		// the first line corresponds to vowels and the second to consonants
		String nextLine;
		String strContext = "";
		Scanner s = null;
		try {
			s = new Scanner(new BufferedReader(new FileReader(sopFileName)));

			while (s.hasNext()) {
				nextLine = s.nextLine();
				if (nextLine.trim().equals(""))
					break;
				else
					strContext += nextLine + "\n";
			}
			// the featureDefinition is the same for vowel, consonant and Pause
			FeatureDefinition voiceFeatDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);

			// vowel line
			if (s.hasNext()) {
				nextLine = s.nextLine();
				System.out.println("line vowel = " + nextLine);
				SoP sopVowel = new SoP(nextLine, voiceFeatDef);
				sopVowel.printCoefficients();
			}

			// consonant line
			if (s.hasNext()) {
				nextLine = s.nextLine();
				System.out.println("line consonants = " + nextLine);
				SoP sopConsonants = new SoP(nextLine, voiceFeatDef);
				sopConsonants.printCoefficients();
			}

		} finally {
			if (s != null)
				s.close();
		}

	}

}
