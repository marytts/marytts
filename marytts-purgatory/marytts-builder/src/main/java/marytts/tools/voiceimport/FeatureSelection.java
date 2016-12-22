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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import marytts.client.http.MaryHttpClient;
import marytts.util.http.Address;
import marytts.util.io.FileUtils;

/**
 * For the given texts, compute unit features and align them with the given unit labels.
 * 
 * @author schroed
 *
 */
public class FeatureSelection extends VoiceImportComponent {
	protected File featureFile;
	protected String features;
	protected String locale;
	protected MaryHttpClient mary;
	protected boolean success = true;

	protected DatabaseLayout db = null;

	public String FEATUREFILE = "FeatureSelection.featureFile";
	public String MARYSERVERHOST = "FeatureSelection.maryServerHost";
	public String MARYSERVERPORT = "FeatureSelection.maryServerPort";

	public String getName() {
		return "FeatureSelection";
	}

	@Override
	protected void initialiseComp() {
		locale = db.getProp(db.LOCALE);

		mary = null; // initialised only if needed
		featureFile = new File(getProp(FEATUREFILE));
		if (featureFile.exists()) {
			System.out.println("Loading features from file " + getProp(FEATUREFILE));
			try {
				features = FileUtils.getFileAsString(featureFile, "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(FEATUREFILE, db.getProp(db.CONFIGDIR) + "features.txt");
			props.put(MARYSERVERHOST, "localhost");
			props.put(MARYSERVERPORT, "59125");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(FEATUREFILE, "file containing the list of features to use."
				+ "Will be created by querying MARY server if it does not exist");
		props2Help.put(MARYSERVERHOST, "the host were the Mary server is running, default: \"localhost\"");
		props2Help.put(MARYSERVERPORT, "the port were the Mary server is listening, default: \"59125\"");
	}

	public MaryHttpClient getMaryClient() throws IOException {
		if (mary == null) {
			try {
				mary = new MaryHttpClient(new Address(getProp(MARYSERVERHOST), Integer.parseInt(getProp(MARYSERVERPORT))));
			} catch (IOException e) {
				throw new IOException("Could not connect to Maryserver at " + getProp(MARYSERVERHOST) + " "
						+ getProp(MARYSERVERPORT));
			}
		}
		return mary;
	}

	protected void saveFeatures(String newFeatures) {
		System.out.println("Saving features to " + featureFile.getAbsolutePath());
		features = newFeatures;
		if (!features.contains(PhoneUnitFeatureComputer.PHONEFEATURE)) {
			JOptionPane.showMessageDialog(null, "The features '" + PhoneUnitFeatureComputer.PHONEFEATURE
					+ "' is not present.\nThis will lead to problems in the further processing.", "Important feature missing",
					JOptionPane.WARNING_MESSAGE);
		}
		try {
			PrintWriter pw = new PrintWriter(featureFile, "UTF-8");
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
		if (features == null) {
			mary = getMaryClient();
			features = mary.getDiscreteFeatures(locale);
			features = features.replaceAll(" ", "\n");
		}

		final JFrame frame = new JFrame("Features to use for building voice");
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

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return -1;
	}

}
