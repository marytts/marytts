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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.ESTTrackWriter;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.data.text.ESTTextfileDoubleDataSource;
import marytts.util.io.FileUtils;
import marytts.util.string.PrintfFormat;

/**
 * For the given texts, compute unit features and align them with the given unit labels.
 * 
 * @author schroed
 *
 */
public class LabelledFilesInspector extends VoiceImportComponent {
	protected File wavDir;
	protected File phoneLabDir;
	protected File pmDir;

	protected DatabaseLayout db = null;

	protected JList fileList;
	protected JList labels;
	protected JTextField saveFilename;

	protected double[] audioSignal;
	protected AudioFormat audioFormat;
	protected double[] pitchmarks;
	protected int samplingRate;
	protected double tStart;
	protected double tEnd;
	private boolean quit = false;

	protected String extractedDir;
	protected String extractedWavDir;
	protected String extractedLabDir;
	protected String extractedPmDir;

	public final String PMDIR = "db.pmDir";
	public final String PMEXT = "db.pmExtension";

	public String getName() {
		return "LabelledFilesInspector";
	}

	@Override
	protected void initialiseComp() {
		extractedDir = db.getProp(db.TEMPDIR);
		extractedWavDir = extractedDir + "wav/";
		extractedLabDir = extractedDir + "lab/";
		extractedPmDir = extractedDir + "pm/";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
	}

	public boolean compute() throws IOException {
		quit = false;
		wavDir = new File(db.getProp(db.WAVDIR));
		if (!wavDir.exists())
			throw new IOException("No such directory: " + wavDir);
		phoneLabDir = new File(db.getProp(db.LABDIR));
		pmDir = new File(db.getProp(PMDIR));

		File extractedDirFile = new File(extractedDir);
		if (!extractedDirFile.exists())
			extractedDirFile.mkdir();
		File extractedWavDirFile = new File(extractedWavDir);
		if (!extractedWavDirFile.exists())
			extractedWavDirFile.mkdir();
		File extractedLabDirFile = new File(extractedLabDir);
		if (!extractedLabDirFile.exists())
			extractedLabDirFile.mkdir();
		File extractedPmDirFile = new File(extractedPmDir);
		if (!extractedPmDirFile.exists())
			extractedPmDirFile.mkdir();

		System.out.println("Proposing for inspection " + bnl.getLength() + " files");

		JFrame jf = new JFrame("Inspecting labelled files");
		jf.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				requestQuit();
			}
		});
		Container cont = jf.getContentPane();
		cont.setLayout(new FlowLayout(FlowLayout.LEFT));
		fileList = new JList(bnl.getListAsArray());
		fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fileList.setVisibleRowCount(-1);
		JScrollPane fileScroll = new JScrollPane(fileList);
		fileScroll.setPreferredSize(new Dimension(250, 500));
		cont.add(fileScroll);
		fileList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					String basename = (String) fileList.getSelectedValue();
					if (basename != null) {
						loadFile(basename);
					}
				}
			}
		});

		labels = new JList();
		labels.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		labels.setVisibleRowCount(-1);
		JScrollPane scroll = new JScrollPane(labels);
		scroll.setPreferredSize(new Dimension(250, 500));
		cont.add(scroll);

		JPanel buttons = new JPanel();
		cont.add(buttons);
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.PAGE_AXIS));

		JButton play = new JButton("Play selection");
		buttons.add(play);
		play.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				playCurrentSelection();
			}
		});

		saveFilename = new JTextField(30);
		buttons.add(saveFilename);

		JButton save = new JButton("Save");
		buttons.add(save);
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				saveCurrentSelection();
			}
		});

		JButton quitButton = new JButton("Done");
		buttons.add(quitButton);
		quitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				requestQuit();
			}
		});

		jf.pack();
		jf.setVisible(true);

		while (!quit) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ie) {
			}
		}
		jf.setVisible(false);
		System.out.println("Finished inspecting label files.");
		return true;
	}

	private void loadFile(String basename) {
		try {
			File wavFile = new File(wavDir, basename + db.getProp(db.WAVEXT));
			if (!wavFile.exists())
				throw new IllegalArgumentException("File " + wavFile.getAbsolutePath() + " does not exist");
			File labFile = new File(phoneLabDir, basename + db.getProp(db.LABEXT));
			if (!labFile.exists())
				throw new IllegalArgumentException("File " + labFile.getAbsolutePath() + " does not exist");
			// pm file is optional
			File pmFile = new File(pmDir, basename + db.getProp(PMEXT));
			if (pmFile.exists()) {
				System.out.println("Loading pitchmarks file " + pmFile.getAbsolutePath());
				pitchmarks = new ESTTextfileDoubleDataSource(pmFile).getAllData();
			} else {
				System.out.println("Pitchmarks file " + pmFile.getAbsolutePath() + " does not exist");
				pitchmarks = null;
			}

			AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile);
			audioFormat = ais.getFormat();
			samplingRate = (int) audioFormat.getSampleRate();
			audioSignal = new AudioDoubleDataSource(ais).getAllData();

			String file = FileUtils.getFileAsString(labFile, "ASCII");
			String[] lines = file.split("\n");
			labels.setListData(lines);

			saveFilename.setText(basename);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void playCurrentSelection() {
		if (!delimitSelection()) { // no selection: play entire file
			tStart = 0;
			tEnd = audioSignal.length / (double) samplingRate;
		}
		// Now we have valid start and end times
		AudioInputStream playAIS = getSelectedAudio();
		System.out.println("Playing from " + tStart + " to " + tEnd);
		try {
			DataLine.Info clipInfo = new DataLine.Info(Clip.class, audioFormat);
			Clip clip = (Clip) AudioSystem.getLine(clipInfo);
			clip.open(playAIS);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void saveCurrentSelection() {
		System.out.println("Saving current selection");
		if (!delimitSelection())
			return;
		// Now we have valid start and end times
		File saveWav = new File(extractedWavDir, saveFilename.getText() + db.getProp(db.WAVEXT));
		File saveLab = new File(extractedLabDir, saveFilename.getText() + db.getProp(db.LABEXT));
		AudioInputStream selectedAudio = getSelectedAudio();
		try {
			// Write audio extract:
			AudioSystem.write(selectedAudio, AudioFileFormat.Type.WAVE, saveWav);
			System.out.println("Wrote audio to " + saveWav.getAbsolutePath());
			// Write label file extract:
			Object[] selection = labels.getSelectedValues();
			PrintWriter toLab = new PrintWriter(new FileWriter(saveLab));
			// these header lines, by the way, serve no discernible purpose here:
			toLab.println("separator ;");
			toLab.println("nfields 1");
			toLab.println("#");
			for (int i = 0; i < selection.length; i++) {
				String[] parts = ((String) selection[i]).trim().split("\\s+");
				double time = Double.parseDouble(parts[0]);
				double newTime = time - tStart;
				parts[0] = new PrintfFormat(Locale.ENGLISH, "%.5f").sprintf(newTime);
				for (int j = 0; j < parts.length; j++) {
					if (j > 0)
						toLab.print("    ");
					toLab.print(parts[j]);
				}
				toLab.println();
			}
			toLab.close();
			System.out.println("Wrote labels to " + saveLab.getAbsolutePath());
			// Optionally, write pitchmark extract:
			if (pitchmarks != null) {
				int firstpm = -1;
				int lastPlus1pm = -1;
				for (int i = 0; i < pitchmarks.length; i++) {
					if (firstpm == -1) {
						if (pitchmarks[i] > tStart)
							firstpm = i;
					} else if (lastPlus1pm == -1) {
						if (pitchmarks[i] > tEnd)
							lastPlus1pm = i;
					}
				}
				if (lastPlus1pm == -1)
					lastPlus1pm = pitchmarks.length;
				float[] pmExtract = new float[lastPlus1pm - firstpm];
				for (int i = 0; i < pmExtract.length; i++) {
					pmExtract[i] = (float) (pitchmarks[firstpm + i] - tStart);
				}
				String extractedPmFile = extractedPmDir + saveFilename.getText() + db.getProp(PMEXT);
				new ESTTrackWriter(pmExtract, null, "pitchmarks").doWriteAndClose(extractedPmFile, false, false);
				System.out.println("Wrote pitchmarks to " + extractedPmFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean delimitSelection() {
		int indices[] = labels.getSelectedIndices();
		if (indices == null || indices.length == 0)
			return false;

		if (indices[0] == 0)
			tStart = 0;
		else {
			String prevLine = (String) labels.getModel().getElementAt(indices[0] - 1);
			StringTokenizer t = new StringTokenizer(prevLine);
			String startTime = t.nextToken();
			try {
				tStart = Double.parseDouble(startTime);
			} catch (NumberFormatException nfe) {
				tStart = 0;
			}
		}
		String lastLine = (String) labels.getModel().getElementAt(indices[indices.length - 1]);
		StringTokenizer t = new StringTokenizer(lastLine);
		String endTime = t.nextToken();
		try {
			tEnd = Double.parseDouble(endTime);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	private AudioInputStream getSelectedAudio() {
		int sStart = (int) (tStart * samplingRate);
		int sEnd = (int) (tEnd * samplingRate);
		assert sStart < sEnd;
		assert sEnd <= audioSignal.length;
		double[] playSignal = new double[sEnd - sStart];
		System.arraycopy(audioSignal, sStart, playSignal, 0, sEnd - sStart);
		return new DDSAudioInputStream(new BufferedDoubleDataSource(playSignal), audioFormat);
	}

	private void requestQuit() {
		quit = true;
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
