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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class HMMVoiceFeatureSelection extends VoiceImportComponent {

	// protected File hmmFeatureFile11;
	protected String features;
	protected String locale;
	protected boolean success = true;

	protected DatabaseLayout db = null;

	public String HMMFEATUREFILE = "HMMVoiceFeatureSelection.hmmFeatureFile";
	public String FEATUREFILE = "HMMVoiceFeatureSelection.featureFile";

	public String getName() {
		return "HMMVoiceFeatureSelection";
	}

	@Override
	protected void initialiseComp() {
		locale = db.getProp(db.LOCALE);
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(HMMFEATUREFILE, db.getProp(DatabaseLayout.CONFIGDIR) + "hmmFeatures.txt");
			props.put(FEATUREFILE, db.getProp(DatabaseLayout.CONFIGDIR) + "features.txt");

		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help
				.put(HMMFEATUREFILE,
						"file containing the extra features, appart from phone and phonological, that will be used to train the HMMs."
								+ " The file will be created by reading mary/features.txt, normally hmmFeatures.txt is a subset of features.txt. "
								+ " Delete the features that will no be used to train the HMMs. When running this program "
								+ " a small set will be presented on top, (pos_in_syl, syl_break, prev_syl_break, position_type) separated by an empty line, "
								+ " if you are not sure about using other features, use just these and delete the others.");
		props2Help.put(FEATUREFILE,
				"file containing the list of features used to create the phonefeatures, this file should had been created "
						+ "with the FeatureSelection component.");
	}

	protected void saveFeatures(String newFeatures) {
		File hmmFeatureFile = new File(getProp(HMMFEATUREFILE));
		System.out.println("Saving extra features for training HMMs to " + hmmFeatureFile.getAbsolutePath());
		features = newFeatures;
		/*
		 * if (!features.contains(PhoneUnitFeatureComputer.PHONEFEATURE)) { JOptionPane.showMessageDialog(null,
		 * "The features '"+PhoneUnitFeatureComputer.PHONEFEATURE
		 * +"' is not present.\nThis will lead to problems in the further processing.", "Important feature missing",
		 * JOptionPane.WARNING_MESSAGE); }
		 */
		try {
			PrintWriter pw = new PrintWriter(hmmFeatureFile, "UTF-8");
			pw.println(features);
			pw.close();
		} catch (IOException e) {
			System.err.println("Cannot save features:");
			e.printStackTrace();
			success = false;
		}
	}

	protected void setSuccess(boolean val) {
		success = val;
	}

	public boolean compute() throws IOException {
		features = loadFeatureList();
		if (!features.contentEquals("")) {
			features = features.replaceAll(" ", "\n");
			features = features.replaceFirst("unit_duration", "");
			features = features.replaceFirst("unit_logf0delta", "");
			features = features.replaceFirst("unit_logf0", "");
		}

		final JFrame frame = new JFrame("Extra features for training HMMs");
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
				saveFeatures(editPane.getText());
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new Error("Error defining replacements");
			}
		}

		return success;
	}

	private String loadFeatureList() throws IOException {
		String featureList = "";
		String recommendedFeatureList = "";
		Scanner feaList = null;
		try {
			feaList = new Scanner(new BufferedReader(new FileReader(getProp(FEATUREFILE))));
			String line;
			while (feaList.hasNext()) {
				line = feaList.nextLine();
				// Exclude phone and phonological, those are by default used in makeLabes and makeQuestions
				// Also exclude the halfphone features not used in HMM voices
				if (!(line.contains("_vc") || line.contains("_vlng") || line.contains("_vheight") || line.contains("_vfront")
						|| line.contains("_vrnd") || line.contains("_ctype") || line.contains("_cplace")
						|| line.contains("_cvox") || line.contains("_phone") || line.contains("ph_")
						|| line.contains("halfphone_") || line.contentEquals("phone"))) {

					featureList += line + " ";
				}

			}
			if (feaList != null) {
				feaList.close();
			}
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException();
		}
		return featureList;

	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return -1;
	}

}
