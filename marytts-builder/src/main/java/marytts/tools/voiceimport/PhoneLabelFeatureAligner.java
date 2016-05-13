/**
 * Copyright 2000-2009 DFKI GmbH.
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
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.io.FileUtils;

/**
 * Compare unit label and unit feature files. If they don't align, flag a problem; let the user decide how to fix it -- either by
 * editing the unit label file or by editing a rawmaryxml file and recomputing the features file.
 * 
 * @author schroed
 *
 */
public class PhoneLabelFeatureAligner extends VoiceImportComponent {

	protected PhoneUnitFeatureComputer featureComputer;
	protected AllophonesExtractor allophoneExtractor;
	protected PhoneUnitLabelComputer labelComputer;
	protected TranscriptionAligner transcriptionAligner;
	protected String pauseSymbol;

	protected DatabaseLayout db = null;
	protected int percent = 0;
	protected Map<String, String> problems;
	protected boolean correctedPauses = false;
	// protected boolean wait =false;

	protected String featsExt;
	protected String labExt;
	protected String labDir;
	protected String featsDir;

	protected static final int TRYAGAIN = 0;
	protected static final int SKIP = 1;
	protected static final int SKIPALL = 2;
	protected static final int REMOVE = 3;
	protected static final int REMOVEALL = 4;

	public String getName() {
		return "PhoneLabelFeatureAligner";
	}

	protected void customInitialisation() {
		featureComputer = (PhoneUnitFeatureComputer) db.getComponent("PhoneUnitFeatureComputer");
		allophoneExtractor = (AllophonesExtractor) db.getComponent("AllophonesExtractor");
		labelComputer = (PhoneUnitLabelComputer) db.getComponent("PhoneUnitLabelComputer");
		transcriptionAligner = (TranscriptionAligner) db.getComponent("TranscriptionAligner");
		featsExt = ".pfeats";
		labExt = ".lab";
		featsDir = db.getProp(db.PHONEFEATUREDIR);
		labDir = db.getProp(db.PHONELABDIR);
	}

	@Override
	protected final void initialiseComp() throws Exception {
		customInitialisation();
		db.initialiseComponent(featureComputer);

		pauseSymbol = db.getAllophoneSet().getSilence().name();
		File unitfeatureDir = new File(featsDir);
		if (!unitfeatureDir.exists()) {
			System.out.println("Feature directory " + featsDir + " does not exist; ");
			if (!unitfeatureDir.mkdir()) {
				throw new Error("Could not create FEATUREDIR");
			}
			System.out.println("Created successfully.");
		}
		File unitlabelDir = new File(labDir);
		if (!unitlabelDir.exists()) {
			System.out.print("Label directory " + labDir + " does not exist; ");
			if (!unitlabelDir.mkdir()) {
				throw new Error("Could not create LABELDIR");
			}
			System.out.println("Created successfully.");
		}
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
	}

	/**
	 * Align labels and features. For each .phonelab file in the phone label directory, verify whether the chain of units given is
	 * identical to the chain of units in the corresponding unit feature file. For those files that are not perfectly aligned,
	 * give the user the opportunity to correct alignment.
	 * 
	 * @return a boolean indicating whether or not the database is fully aligned.
	 * @throws Exception
	 *             Exception
	 */
	public boolean compute() throws Exception {
		int bnlLengthIn = bnl.getLength();
		System.out.println("Verifying feature-label alignment for " + bnlLengthIn + " utterances.");
		problems = new TreeMap<String, String>();

		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			// call firstVerifyAlignment for first alignment test
			String errorMessage = verifyAlignment(bnl.getName(i));
			System.out.print("    " + bnl.getName(i));
			if (errorMessage == null) {
				System.out.println(" OK");
			} else {
				problems.put(bnl.getName(i), errorMessage);
				System.out.println(" " + errorMessage);
			}
		}
		System.out.println("Found " + problems.size() + " problems");
		int remainingProblems = problems.keySet().size();

		if (remainingProblems > 0) {
			// show option for automatically correcting pauses
			// remainingProblems = correctPausesYesNo(remainingProblems);
			remainingProblems = correctPauses();
		}

		int guiReturn = SKIP;
		boolean removeAll = false;
		boolean skipAll = false;
		boolean tryAgain = true;
		for (Iterator<String> it = problems.keySet().iterator(); it.hasNext();) {
			String basename = it.next();
			String errorMessage;
			if (!(removeAll || skipAll)) { // These may be set true after a previous call to letUserCorrect()
				do {
					errorMessage = (String) problems.get(basename);
					System.out.println("    " + basename + ": " + errorMessage);
					/* Let the user make a first correction */
					guiReturn = letUserCorrect(basename, errorMessage);
					// while(wait=true){}
					/* Check if an error remains */
					errorMessage = verifyAlignment(basename);
					/* If there is no error, proceed with the next file. */
					if (errorMessage == null) {
						System.out.println(" -> OK");
						remainingProblems--;
						tryAgain = false;
					}
					/* If the error message is (still) not null, manage the GUI return code: */
					else {
						problems.put(basename, errorMessage);
						/* Manage the error according to the GUI return: */
						switch (guiReturn) {

						case TRYAGAIN:
							tryAgain = true;
							break;

						case SKIP:
							tryAgain = false;
							System.out.println(" -> Skipped this utterance ! This problem remains.");
							break;

						case SKIPALL:
							tryAgain = false;
							skipAll = true;
							System.out.println(" -> Skipping all utterances ! The problems remain.");
							break;

						case REMOVE:
							tryAgain = false;
							bnl.remove(basename);
							deleteProblemsYesNo(null, basename);
							remainingProblems--;
							System.out.println(" -> Removed from the utterance list. OK");
							break;

						case REMOVEALL:
							tryAgain = false;
							removeAll = true;
							System.out.println(" -> Removing all problematic utterances. OK");
							break;

						default:
							throw new RuntimeException("The letUserCorrect() GUI returned an unknown return code.");
						}
					}
				} while (tryAgain);

			}

			/* Additional management for the removeAll option: */
			if (removeAll) {
				bnl.remove(basename);
				remainingProblems--;
			}
		}
		if (removeAll) {
			// ask user if asscociated files should be deleted
			deleteProblemsYesNo(problems, null);
		}

		System.out.println("Removed [" + (bnlLengthIn - bnl.getLength()) + "/" + bnlLengthIn + "] utterances from the list, ["
				+ bnl.getLength() + "] utterances remain," + " among which [" + remainingProblems + "/" + bnl.getLength()
				+ "] still have problems.");

		return remainingProblems == 0; // true exactly if all problems have been solved
	}

	/**
	 * Let the user select if he wants to run the the automatic correction of pauses.
	 * 
	 * @param numProblems
	 *            the number of problems
	 * @throws IOException
	 *             IOException
	 * @return the number of problems remaining
	 */
	protected int correctPausesYesNo(int numProblems) throws IOException {
		int choice = JOptionPane.showOptionDialog(null, "Found " + numProblems + " problems. Automatically correct pauses?",
				"Automatic pause correction", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {
						"Yes", "No" }, null);

		if (choice == 0)
			return correctPauses();
		return numProblems;
	}

	/**
	 * Let the user select if he wants to run the the automatic correction of pauses.
	 * 
	 * @param someProblems
	 *            someProblems
	 * @param basename
	 *            basename
	 * @throws IOException
	 *             IOException
	 */
	protected void deleteProblemsYesNo(Map<String, String> someProblems, String basename) throws IOException {
		int choice = JOptionPane.showOptionDialog(null, "Removed problematic utterance(s) from List. Also delete file(s)?",
				"Delete problematic file(s)", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {
						"Yes", "No" }, null);

		if (choice == 0) {
			if (someProblems != null) {
				// we have a map basenames->problems
				for (Iterator<String> it = someProblems.keySet().iterator(); it.hasNext();) {
					String nextBasename = it.next();
					File nextLabFile = new File(labDir + nextBasename + labExt);
					nextLabFile.delete();
					File nextFeatFile = new File(featsDir + nextBasename + featsExt);
					nextFeatFile.delete();
				}
			}
			if (basename != null) {
				// there is just one basename
				File nextLabFile = new File(labDir + basename + labExt);
				nextLabFile.delete();
				File nextFeatFile = new File(featsDir + basename + featsExt);
				nextFeatFile.delete();
			}
			// before anything else happens, ensure that deleted files will not be missed later
			// by forcing basename file to be written/updated; this is just a q&d fix!
			String basenameFilename = db.getProp("db.basenameFile");
			bnl.write(basenameFilename);
		}
	}

	protected void defineReplacementWindow() {

		final JFrame frame = new JFrame("Define Replacements");
		GridBagLayout gridBagLayout = new GridBagLayout();
		GridBagConstraints gridC = new GridBagConstraints();
		frame.getContentPane().setLayout(gridBagLayout);

		final JEditorPane editPane = new JEditorPane();
		editPane.setPreferredSize(new Dimension(500, 500));
		editPane.setText("#Whenever a problem occurs, the problematic phone in the label file\n"
				+ "#will be replaced by the phone you define here.\n\n" + "#Define replacements like this:\n"
				+ "#labelPhone newLabelPhone\n");

		JButton saveButton = new JButton("Apply to problems");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
				try {
					defineReplacements(editPane.getText());
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new Error("Error defining replacements");
				}
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
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
	}

	protected void defineReplacementInfo(String text) {
		int choice = JOptionPane.showOptionDialog(null, "Error applying replacements: Syntax error in line \"" + text + "\"",
				"Error in replacement definition", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new String[] { "Correct", "Cancel" }, null);
		if (choice == 0)
			defineReplacementWindow();
	}

	/**
	 * Try to automatically correct misalignment caused by pauses: If there is a pause in the label file and not in the feature
	 * file, it is removed in the label file. If there is a pause in the feature file and not in the label file, a pause of length
	 * zero is inserted in the label file
	 * 
	 * 
	 * @return the number of problems remaining
	 * @throws IOException
	 *             IOException
	 */
	protected int correctPauses() throws IOException {
		correctedPauses = true;
		// clear the list of problems
		problems = new TreeMap<String, String>();
		// go through all files
		for (int l = 0; l < bnl.getLength(); l++) {
			percent = 100 * l / bnl.getLength();
			String basename = bnl.getName(l);
			System.out.print("    " + basename);
			String line;

			BufferedReader labels;
			try {
				labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(labDir + basename + labExt)),
						"UTF-8"));
			} catch (FileNotFoundException fnfe) {
				continue;
			}
			// store header of label file in StringBuffer
			StringBuilder labelFileHeader = new StringBuilder();
			boolean foundHeader = false;
			while ((line = labels.readLine()) != null) {
				labelFileHeader.append(line + "\n");
				if (line.startsWith("#")) {
					foundHeader = true;
					break; // line starting with "#" marks end of header
				}
			}

			if (!foundHeader) {
				throw new IOException("File " + labDir + basename + labExt + " does not contain a file header!");
			}

			// store units of label file in List
			List<String> labelUnits = new ArrayList<String>();
			while ((line = labels.readLine()) != null) {
				labelUnits.add(line + "\n");
			}

			BufferedReader features;
			try {
				features = new BufferedReader(new InputStreamReader(
						new FileInputStream(new File(featsDir + basename + featsExt)), "UTF-8"));
			} catch (FileNotFoundException fnfe) {
				continue;
			}
			while ((line = features.readLine()) != null) {
				if (line.trim().equals(""))
					break; // empty line marks end of header
			}

			// store text units of feature file in list
			List<String> featureUnits = new ArrayList<String>();
			while ((line = features.readLine()) != null) {
				if (line.trim().equals(""))
					break; // empty line marks end of section
				featureUnits.add(line);
			}

			labels.close();
			features.close();

			ArrayList<String> labelUnitData;
			String labelUnit;
			String featureUnit;
			String returnString = null;

			int numLabelUnits = labelUnits.size();
			int numFeatureUnits = featureUnits.size();

			int i = 0, j = 0;
			while (i < numLabelUnits && j < numFeatureUnits) {
				// System.out.println("featureUnit : "+featureUnit
				// +" labelUnit : "+labelUnit);
				labelUnitData = getLabelUnitData(labelUnits.get(i));
				labelUnit = labelUnitData.get(2);
				featureUnit = getFeatureUnit(featureUnits.get(j));

				if (!featureUnit.equals(labelUnit)) {

					if (featureUnit.equals("_")) {
						// add pause in labels
						System.out.println(" Adding pause unit in labels before unit " + i);
						String pauseUnit;
						if (i - 1 >= 0) {
							ArrayList<String> previousUnitData = getLabelUnitData(labelUnits.get(i - 1));
							pauseUnit = (String) previousUnitData.get(0) + " " + (String) labelUnitData.get(1) + " _\n";
						} else {
							pauseUnit = "0.00000 " + (String) labelUnitData.get(1) + " _\n";
						}
						labelUnits.add(i, pauseUnit);
						i++;
						j++;
						numLabelUnits = labelUnits.size();
						continue;
					} else if (featureUnit.equals("__L")) {
						// add two pause units in labels
						System.out.println(" Adding pause units in labels before unit " + i);
						String pauseUnit;
						if (i - 1 >= 0) {
							ArrayList<String> previousUnitData = getLabelUnitData(labelUnits.get(i - 1));
							pauseUnit = previousUnitData.get(0) + " " + (String) labelUnitData.get(1) + " __L\n";
							labelUnits.add(i, pauseUnit);
							i++;
							pauseUnit = (String) previousUnitData.get(0) + " " + (String) labelUnitData.get(1) + " __R\n";
							labelUnits.add(i, pauseUnit);
						} else {
							// this is the first label-unit
							pauseUnit = "0.00000 " + (String) labelUnitData.get(1) + " __L\n";
							labelUnits.add(i, pauseUnit);
							i++;
							pauseUnit = "0.00000 " + (String) labelUnitData.get(1) + " __R\n";
							labelUnits.add(i, pauseUnit);
						}
						i++;
						j += 2;
						numLabelUnits = labelUnits.size();
						continue;
					} else if (labelUnit.equals("_")) {
						// remove pause in labels
						System.out.println(" Removing pause unit in labels at index " + i);
						labelUnits.remove(i);
						numLabelUnits = labelUnits.size();
						continue;
					} else if (labelUnit.equals("__L")) {
						// remove two pause units in labels
						System.out.println(" Removing pause units in labels at index " + i);
						if (i - 1 >= 0) {
							// lengthen the unit before the pause
							ArrayList<String> previousUnitData = getLabelUnitData(labelUnits.get(i - 1));
							labelUnits.set(i - 1, (String) labelUnitData.get(0) + " " + (String) previousUnitData.get(1) + " "
									+ (String) previousUnitData.get(2) + "\n");
						}
						// remove the pauses
						labelUnits.remove(i);
						labelUnits.remove(i);
						numLabelUnits = labelUnits.size();
						continue;
					} else {
						// truly not matching
						if (returnString == null) {
							// only remember the first mismatch
							int unitIndex = i - 1;
							returnString = " Non-matching units found: feature file '" + featureUnit + "' vs. label file '"
									+ labelUnit + "' (Unit " + unitIndex + ")";
						}
					}
				}
				// increase both counters if you did not delete a pause
				i++;
				j++;
			}
			if (numLabelUnits < numFeatureUnits) {
				// check if the final pause is missing in the label file
				featureUnit = getFeatureUnit((String) featureUnits.get(numFeatureUnits - 1));
				labelUnitData = getLabelUnitData((String) labelUnits.get(numLabelUnits - 1));
				labelUnit = (String) labelUnitData.get(2);
				// add a pause at the end of label file
				if (featureUnit.equals("_") && numLabelUnits + 1 == numFeatureUnits) {
					String lastFeatureUnit = getFeatureUnit((String) featureUnits.get(numFeatureUnits - 2));
					if (lastFeatureUnit.equals(labelUnit)) {
						// add pause at the end
						System.out.println(" Adding pause unit in labels after last unit");
						String pauseUnit = (String) labelUnitData.get(0) + " " + numLabelUnits + " _\n";
						labelUnits.add(pauseUnit);
						numLabelUnits = labelUnits.size();
					}
				} else if (featureUnit.equals("__R") && numLabelUnits + 2 == numFeatureUnits) {
					String lastFeatureUnit = getFeatureUnit((String) featureUnits.get(numFeatureUnits - 3));
					if (lastFeatureUnit.equals(labelUnit)) {
						// add two pause units at the end of label file
						System.out.println(" Adding pause units in labels after last unit");
						int unitIndex = numLabelUnits - 1;
						String pauseUnit = (String) labelUnitData.get(0) + " " + unitIndex + " __L\n";
						labelUnits.add(pauseUnit);

						pauseUnit = (String) labelUnitData.get(0) + " " + numLabelUnits + " __R\n";
						labelUnits.add(pauseUnit);

						numLabelUnits = labelUnits.size();
					}
				} else { // feature file is truly longer than label file
					if (returnString == null) {
						returnString = " Feature file is longer than label file: " + " unit " + numLabelUnits
								+ " and greater do not exist in label file";
					}
				}
			}
			// return an error if label file is longer than feature file
			if (returnString == null && numLabelUnits > numFeatureUnits) {
				returnString = " Label file is longer than feature file: " + " unit " + numFeatureUnits
						+ " and greater do not exist in feature file";
			}

			// now overwrite the label file
			PrintWriter labelFileWriter = new PrintWriter(new FileWriter(new File(labDir + basename + labExt)));
			// print header
			labelFileWriter.print(labelFileHeader.toString());
			// print units
			numLabelUnits = labelUnits.size();
			for (int k = 0; k < numLabelUnits; k++) {
				String nextUnit = labelUnits.get(k);
				if (nextUnit != null) {
					// correct the unit index
					ArrayList<String> nextUnitData = getLabelUnitData(nextUnit);
					labelFileWriter.println(nextUnitData.get(0) + " " + k + " " + nextUnitData.get(2));
				}
			}

			labelFileWriter.flush();
			labelFileWriter.close();

			// returnString is null if all units matched,
			// otherwise the first error is given back
			if (returnString == null) {
				System.out.println(" OK");
			} else {
				problems.put(basename, returnString);
				System.out.println(returnString);
			}
		}
		System.out.println("Remaining problems: " + problems.size());
		return problems.size();
	}

	protected void defineReplacements(String text) throws Exception {
		/* read the replacements into a map */
		Map<String, String> phone2Replace = new HashMap<String, String>();
		String error = null;
		String[] textlines = text.split("\n");
		for (int i = 0; i < textlines.length; i++) {
			if (!textlines[i].startsWith("#") && !textlines[i].equals("")) {
				StringTokenizer tok = new StringTokenizer(textlines[i].trim());
				try {
					phone2Replace.put(tok.nextToken(), tok.nextToken());
				} catch (NoSuchElementException nsee) {
					error = textlines[i];
					break;
				}
			}
		}
		if (error != null) {
			// wait = true;
			// TODO: Does not work properly
			defineReplacementInfo(error);
		} else {
			/*
			 * go through the problems and try to replace the phones in the labels with the specified replacements
			 */

			// clear the list of problems
			problems = new TreeMap<String, String>();
			// go through all files
			for (int l = 0; l < bnl.getLength(); l++) {
				percent = 100 * l / bnl.getLength();
				String basename = bnl.getName(l);
				System.out.print("    " + basename);
				String line;

				BufferedReader labels;
				try {
					labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(labDir + basename + labExt)),
							"UTF-8"));
				} catch (FileNotFoundException fnfe) {
					continue;
				}
				// store header of label file in StringBuffer
				StringBuilder labelFileHeader = new StringBuilder();
				while ((line = labels.readLine()) != null) {
					labelFileHeader.append(line + "\n");
					if (line.startsWith("#"))
						break; // line starting with "#" marks end of header
				}

				// store units of label file in List
				List<String> labelUnits = new ArrayList<String>();
				while ((line = labels.readLine()) != null) {
					labelUnits.add(line + "\n");
				}

				BufferedReader features;
				try {
					features = new BufferedReader(new InputStreamReader(new FileInputStream(new File(featsDir + basename
							+ featsExt)), "UTF-8"));
				} catch (FileNotFoundException fnfe) {
					continue;
				}
				while ((line = features.readLine()) != null) {
					if (line.trim().equals(""))
						break; // empty line marks end of header
				}

				// store text units of feature file in list
				List<String> featureUnits = new ArrayList<String>();
				while ((line = features.readLine()) != null) {
					if (line.trim().equals(""))
						break; // empty line marks end of section
					featureUnits.add(line);
				}

				ArrayList<String> labelUnitData;
				String labelUnit;
				String featureUnit;
				String returnString = null;

				int numLabelUnits = labelUnits.size();
				int numFeatureUnits = featureUnits.size();

				int i = 0, j = 0;
				boolean alteredLabel = false;
				while (i < numLabelUnits && j < numFeatureUnits) {
					// System.out.println("featureUnit : "+featureUnit
					// +" labelUnit : "+labelUnit);
					labelUnitData = getLabelUnitData((String) labelUnits.get(i));
					labelUnit = (String) labelUnitData.get(2);
					featureUnit = getFeatureUnit((String) featureUnits.get(j));

					if (!featureUnit.equals(labelUnit)) {
						// try to replace label Unit
						if (phone2Replace.containsKey(labelUnit)) {
							System.out.print(" Replacing " + labelUnit);
							String newlabelUnit = (String) phone2Replace.get(labelUnit);
							System.out.print(" with " + newlabelUnit + "... ");
							if (featureUnit.equals(newlabelUnit)) {
								labelUnits.remove(i);
								labelUnits.add(i, (String) labelUnitData.get(0) + " " + (String) labelUnitData.get(1) + " "
										+ newlabelUnit);
								alteredLabel = true;
								System.out.print("successful!\n");
								i++;
								j++;
								continue;
							}
							System.out.print("failed!\n");
						}
						// else we have a problem
						if (returnString == null) {
							// only remember the the first mismatch
							int unitIndex = i;
							returnString = " Non-matching units found: feature file '" + featureUnit + "' vs. label file '"
									+ labelUnit + "' (Unit " + unitIndex + ")";
						}

					}
					i++;
					j++;
				}
				// return an error if label file is longer than feature file
				if (returnString == null && numLabelUnits > numFeatureUnits) {
					returnString = " Label file is longer than feature file: " + " unit " + numFeatureUnits
							+ " and greater do not exist in feature file";
				}

				if (alteredLabel) {
					// overwrite the label file
					PrintWriter labelFileWriter = new PrintWriter(new FileWriter(new File(labDir + basename + labExt)));
					// print header
					labelFileWriter.print(labelFileHeader.toString());
					// print units
					numLabelUnits = labelUnits.size();
					for (int k = 0; k < numLabelUnits; k++) {
						String nextUnit = (String) labelUnits.get(k);
						if (nextUnit != null) {
							// correct the unit index
							ArrayList<String> nextUnitData = getLabelUnitData(nextUnit);
							labelFileWriter.print((String) nextUnitData.get(0) + " " + k + " " + (String) nextUnitData.get(2)
									+ "\n");
						}
					}

					labelFileWriter.flush();
					labelFileWriter.close();
				}
				// returnString is null if all units matched,
				// otherwise the first error is given back
				if (returnString == null) {
					System.out.println(" OK");
				} else {
					problems.put(basename, returnString);
					System.out.println(returnString);
				}
			}
			System.out.println("Remaining problems: " + problems.size());
			// wait = false;
		}

	}

	/**
	 * Verify if the feature and label files for basename align OK. This method should be called after firstVerifyAlignment for
	 * subsequent alignment tries.
	 * 
	 * @param basename
	 *            basename
	 * @return null if the alignment was OK, or a String containing an error message.
	 * @throws IOException
	 *             IOException
	 */
	protected String verifyAlignment(String basename) throws IOException {
		BufferedReader labels = null;
		BufferedReader features = null;
		try {
			try {
				labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(labDir + basename + labExt)),
						"UTF-8"));
			} catch (FileNotFoundException fnfe) {
				return "No label file " + labDir + basename + labExt;
			}
			try {
				features = new BufferedReader(new InputStreamReader(
						new FileInputStream(new File(featsDir + basename + featsExt)), "UTF-8"));
			} catch (FileNotFoundException fnfe) {
				return "No feature file " + featsDir + basename + featsExt;
			}

			String line;
			// Skip label file header:
			while ((line = labels.readLine()) != null) {
				if (line.startsWith("#"))
					break; // line starting with "#" marks end of header
			}
			// Skip features file header:
			while ((line = features.readLine()) != null) {
				if (line.trim().equals(""))
					break; // empty line marks end of header
			}

			// Now go through all feature file units
			boolean correct = true;
			int unitIndex = 0;
			while (correct) {
				line = labels.readLine();
				String labelUnit = null;
				if (line != null) {
					List<String> labelUnitData = getLabelUnitData(line);
					labelUnit = (String) labelUnitData.get(2);
					unitIndex = Integer.parseInt((String) labelUnitData.get(1));
				}

				String featureUnit = getFeatureUnit(features);
				if (featureUnit == null)
					throw new IOException("Incomplete feature file: " + basename);
				// when featureUnit is the empty string, we have found an empty line == end of feature section
				if ("".equals(featureUnit)) {
					if (labelUnit == null) {
						// we have reached the end in both labels and features
						break;
					} else {
						// label file is longer than feature file
						return "Label file is longer than feature file: " + " unit " + unitIndex
								+ " and greater do not exist in feature file";
					}
				}
				if (labelUnit == null) {
					// feature file is longer than label file
					unitIndex++;
					return "Feature file is longer than label file: " + " unit " + unitIndex
							+ " and greater do not exist in label file";
				}
				if (!featureUnit.equals(labelUnit)) {
					// label and feature unit do not match
					return "Non-matching units found: feature file '" + featureUnit + "' vs. label file '" + labelUnit
							+ "' (Unit " + unitIndex + ")";
				}
			}
		} finally {
			if (labels != null)
				labels.close();
			if (features != null)
				features.close();

		}
		return null; // success
	}

	private ArrayList<String> getLabelUnitData(String line) throws IOException {
		if (line == null)
			return null;
		ArrayList<String> unitData = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(line.trim());
		// the first token is the time
		unitData.add(st.nextToken());
		// the second token is the unit index
		unitData.add(st.nextToken());
		// the third token is the phone
		unitData.add(st.nextToken());
		return unitData;
	}

	private String getFeatureUnit(String line) throws IOException {
		if (line == null)
			return null;
		if (line.trim().equals(""))
			return ""; // empty line -- signal end of section
		StringTokenizer st = new StringTokenizer(line.trim());
		// The expect that the first token in each line is the label
		return st.nextToken();
	}

	private String getLabelUnit(BufferedReader labelReader) throws IOException {
		String line = labelReader.readLine();
		if (line == null)
			return null;
		StringTokenizer st = new StringTokenizer(line.trim());
		// The third token in each line is the label
		st.nextToken();
		st.nextToken();
		return st.nextToken();
	}

	private String getFeatureUnit(BufferedReader featureReader) throws IOException {
		String line = featureReader.readLine();
		if (line == null)
			return null;
		if (line.trim().equals(""))
			return ""; // empty line -- signal end of section
		StringTokenizer st = new StringTokenizer(line.trim());
		// The expect that the first token in each line is the label
		return st.nextToken();
	}

	protected int letUserCorrect(String basename, String errorMessage) throws Exception {
		String[] options;
		/*
		 * if (correctedPauses){ options = new String[] {"Edit RAWMARYXML", "Edit unit labels", "Remove from list",
		 * "Remove all problems", "Skip", "Skip all","Replace labels in unit file","Define replacements"}; } else { options = new
		 * String[] {"Edit RAWMARYXML", "Edit unit labels", "Remove from list", "Remove all problems", "Skip",
		 * "Skip all","Define replacements"}; }
		 */
		if (correctedPauses) {
			options = new String[] { "Edit RAWMARYXML", "Edit unit labels", "Remove from list", "Remove all problems", "Skip",
					"Skip all" };
		} else {
			options = new String[] { "Edit RAWMARYXML", "Edit unit labels", "Remove from list", "Remove all problems", "Skip",
					"Skip all" };
		}
		int choice;
		try {
			choice = JOptionPane.showOptionDialog(null, "Misalignment problem for " + basename + ":\n" + errorMessage,
					"Correct alignment for " + basename, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					options, null);
		} catch (HeadlessException e) {
			return SKIP;
		}
		switch (choice) {
		case 0:
			editMaryXML(basename);
			return TRYAGAIN;
		case 1:
			editUnitLabels(basename);
			return TRYAGAIN;
		case 2:
			return REMOVE;
		case 3:
			return REMOVEALL;
		case 4:
			return SKIP;
		case 5:
			return SKIPALL;
			/**
			 * case 6: if (correctedPauses){ replaceUnitLabels(basename); } else { defineReplacementWindow(); } return TRYAGAIN;
			 * case 7: defineReplacementWindow(); return TRYAGAIN;
			 **/
		default: // JOptionPane.CLOSED_OPTION
			return SKIP; // don't verify again.
		}
	}

	/**
	 * Replace all label units which do not match the feature units with the feature units This method should only be called after
	 * automatic pause alignment.
	 * 
	 * @param basename
	 *            the filename of the label/feature file
	 * @throws IOException
	 *             IOException
	 */
	private void replaceUnitLabels(String basename) throws IOException {
		String line;

		BufferedReader labels = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(labDir + basename + labExt)), "UTF-8"));
		// store header of label file in StringBuffer
		StringBuilder labelFileHeader = new StringBuilder();
		while ((line = labels.readLine()) != null) {
			labelFileHeader.append(line + "\n");
			if (line.startsWith("#"))
				break; // line starting with "#" marks end of header
		}

		// store units of label file in List
		List<String> labelUnits = new ArrayList<String>();
		while ((line = labels.readLine()) != null) {
			labelUnits.add(line + "\n");
		}

		BufferedReader features = new BufferedReader(new InputStreamReader(new FileInputStream(new File(featsDir + basename
				+ featsExt)), "UTF-8"));
		while ((line = features.readLine()) != null) {
			if (line.trim().equals(""))
				break; // empty line marks end of header
		}

		// store text units of feature file in list
		List<String> featureTextUnits = new ArrayList<String>();
		while ((line = features.readLine()) != null) {
			if (line.trim().equals(""))
				break; // empty line marks end of section
			featureTextUnits.add(line);
		}

		labels.close();
		features.close();

		ArrayList<String> labelUnitData;
		String labelUnit;
		String featureUnit;
		String returnString = null;

		int numLabelUnits = labelUnits.size();
		int numFeatureUnits = featureTextUnits.size();

		int i = 0, j = 0;
		while (i < numLabelUnits && j < numFeatureUnits) {
			// System.out.println("featureUnit : "+featureUnit
			// +" labelUnit : "+labelUnit);
			labelUnitData = getLabelUnitData((String) labelUnits.get(i));
			labelUnit = (String) labelUnitData.get(2);
			featureUnit = getFeatureUnit((String) featureTextUnits.get(j));

			if (!featureUnit.equals(labelUnit)) {
				// take over label of feature file
				labelUnits.set(i, (String) labelUnitData.get(0) + " " + (String) labelUnitData.get(1) + " " + featureUnit + "\n");
			}
			i++;
			j++;
		}

		// now overwrite the label file
		PrintWriter labelFileWriter = new PrintWriter(new FileWriter(new File(labDir + basename + labExt)));
		// print header
		labelFileWriter.print(labelFileHeader.toString());
		// print units
		numLabelUnits = labelUnits.size();
		for (int k = 0; k < numLabelUnits; k++) {
			String nextUnit = (String) labelUnits.get(k);
			if (nextUnit != null) {
				// correct the unit index
				ArrayList<String> nextUnitData = getLabelUnitData(nextUnit);
				labelFileWriter.print((String) nextUnitData.get(0) + " " + k + " " + (String) nextUnitData.get(2) + "\n");
			}
		}

		labelFileWriter.flush();
		labelFileWriter.close();

	}

	private void editMaryXML(String basename) throws Exception {
		final File maryxmlFile = new File(db.getProp(db.MARYXMLDIR) + basename + db.getProp(db.MARYXMLEXT));
		if (!maryxmlFile.exists()) {
			// need to create it
			String text = FileUtils
					.getFileAsString(new File(db.getProp(db.TEXTDIR) + basename + db.getProp(db.TEXTEXT)), "UTF-8");
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(maryxmlFile), "UTF-8"));
			pw.println(PhoneUnitFeatureComputer.getMaryXMLHeaderWithInitialBoundary(db.getProp(db.LOCALE)));
			pw.println(text);
			pw.println("</maryxml>");
			pw.close();
		}
		boolean edited = new EditFrameShower(maryxmlFile).display();
		if (edited) {
			allophoneExtractor.generateAllophonesFile(basename);
			try {
				transcriptionAligner.alignTranscription(basename);
			} catch (Exception e) {
				e.printStackTrace();
			}
			labelComputer.computePhoneLabel(basename);
			featureComputer.computeFeaturesFor(basename);
		}
	}

	private void editUnitLabels(String basename) throws IOException {
		new EditFrameShower(new File(labDir + basename + labExt)).display();
	}

	public static void main(String[] args) throws Exception {
		PhoneLabelFeatureAligner lfa = new PhoneLabelFeatureAligner();
		new DatabaseLayout(lfa);
		boolean isAligned = lfa.compute();
		System.out.println("The database is " + (isAligned ? "" : "NOT") + " perfectly aligned");
	}

	public static class EditFrameShower {
		protected final File file;
		protected boolean saved;

		public EditFrameShower(File file) {
			this.file = file;
			this.saved = false;
		}

		/**
		 * Show a frame allowing the user to edit the file.
		 * 
		 * 
		 * 
		 * @return a boolean indicating whether the file was saved.
		 * @throws IOException
		 *             IOException
		 * @throws UnsupportedEncodingException
		 *             UnsupportedEncodingException
		 * @throws FileNotFoundException
		 *             FileNotFoundException
		 */
		public boolean display() throws IOException, UnsupportedEncodingException, FileNotFoundException {
			final JFrame frame = new JFrame("Edit " + file.getName());
			GridBagLayout gridBagLayout = new GridBagLayout();
			GridBagConstraints gridC = new GridBagConstraints();
			frame.getContentPane().setLayout(gridBagLayout);

			final JEditorPane editPane = new JEditorPane();
			editPane.setPreferredSize(new Dimension(500, 500));
			editPane.read(new InputStreamReader(new FileInputStream(file), "UTF-8"), null);
			JButton saveButton = new JButton("Save & Exit");
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
						editPane.write(pw);
						pw.flush();
						pw.close();
						frame.setVisible(false);
						setSaved(true);
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			});
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.setVisible(false);
					setSaved(false);
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
					Thread.sleep(10); // OK, this is ugly, but I don't mind today...
				} catch (InterruptedException e) {
				}
			} while (frame.isVisible());
			frame.dispose();
			return saved;
		}

		protected void setSaved(boolean saved) {
			this.saved = saved;
		}

	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

}
