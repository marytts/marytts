/**
 * Copyright 2007 DFKI GmbH.
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
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.data.ESTTrackWriter;
import marytts.util.signal.SignalProcUtils;

/**
 * A pitch marker component that uses F0TrackerAutocorrelationHeuristic.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class AutocorrelationPitchmarker extends VoiceImportComponent {
	protected DatabaseLayout db = null;

	protected String ptcExt = ".ptc";

	private int percent = 0;

	public final String WINSIZE = "AutocorrelationPitchmarker.windowSizeInSeconds"; // Window size in seconds
	public final String SKIPSIZE = "AutocorrelationPitchmarker.skipSizeInSeconds"; // Skip size in seconds
	public final String VOICINGTH = "AutocorrelationPitchmarker.voicingThreshold"; // Voicing threshold
	public final String MINF0 = "AutocorrelationPitchmarker.minimumF0"; // Min f0 in Hz
	public final String MAXF0 = "AutocorrelationPitchmarker.maximumF0"; // Max f0 in Hz
	public final String PTCDIR = "AutocorrelationPitchmarker.ptcDir";

	public final String PMDIR = "db.pmDir";
	public final String PMEXT = "db.pmExtension";

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap();

			PitchFileHeader tmp = new PitchFileHeader();

			props2Help.put(WINSIZE, "window size in pitch detection. Default: " + String.valueOf(tmp.windowSizeInSeconds));
			props2Help.put(SKIPSIZE, "skip size in pitch detection. Default: " + String.valueOf(tmp.skipSizeInSeconds));
			props2Help
					.put(VOICINGTH,
							"threshold of voicing (decrease to get more voiced frames). Default: "
									+ String.valueOf(tmp.voicingThreshold));
			props2Help.put(MINF0, "minimum value for the pitch (in Hz). Default: " + String.valueOf(tmp.minimumF0));
			props2Help.put(MAXF0, "maximum value for the pitch (in Hz). Default: " + String.valueOf(tmp.maximumF0));
			props2Help.put(PTCDIR, "directory containing the binary f0 contour files. Will be created if" + "it does not exist");
		}
	}

	public final String getName() {
		return "AutocorrelationPitchmarker";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			PitchFileHeader tmp = new PitchFileHeader();

			props.put(WINSIZE, String.valueOf(tmp.windowSizeInSeconds));
			props.put(SKIPSIZE, String.valueOf(tmp.skipSizeInSeconds));
			props.put(VOICINGTH, String.valueOf(tmp.voicingThreshold));
			props.put(MINF0, String.valueOf(tmp.minimumF0));
			props.put(MAXF0, String.valueOf(tmp.maximumF0));

			String rootDir = db.getProp(db.ROOTDIR);

			props.put(PTCDIR, rootDir + "ptc" + System.getProperty("file.separator"));
		}
		return props;
	}

	private boolean extractPitchmarks(String basename, PitchFileHeader params) throws IOException {
		String wavFilename = new File(db.getProp(db.WAVDIR) + basename + db.getProp(db.WAVEXT)).getAbsolutePath();
		String ptcFile = getProp(PTCDIR) + basename + ptcExt;
		String pmFilename = db.getProp(PMDIR) + basename + db.getProp(PMEXT);

		WavReader wav = new WavReader(wavFilename);
		int fs = wav.getSampleRate();
		short[] x = wav.getSamples();

		F0TrackerAutocorrelationHeuristic pitchDetector = new F0TrackerAutocorrelationHeuristic(params);
		PitchReaderWriter f0 = null;
		try {
			f0 = pitchDetector.pitchAnalyzeWavFile(wavFilename, ptcFile);
		} catch (UnsupportedAudioFileException e) {
			System.out.println("Error! Cannot perform pitch detection...");
		}

		if (f0 != null) {
			PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, x.length, f0.header.windowSizeInSeconds,
					f0.header.skipSizeInSeconds, false, 0);

			// Now convert to EST pm format
			float[] pitchmarks = new float[pm.pitchMarks.length];
			for (int i = 0; i < pitchmarks.length; i++)
				pitchmarks[i] = SignalProcUtils.sample2time(pm.pitchMarks[i], fs);

			new ESTTrackWriter(pitchmarks, null, "pitchmarks").doWriteAndClose(pmFilename, false, false);

		}

		return true;
	}

	/**
	 * The standard compute() method of the VoiceImportComponent interface.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	public boolean compute() throws IOException {

		String[] baseNameArray = bnl.getListAsArray();
		System.out.println("Computing pitchmarks for " + baseNameArray.length + " utterances.");

		/* Ensure the existence of the target directory for corrected pitchmarks */
		File dir = new File(db.getProp(PMDIR));
		if (!dir.exists()) {
			System.out.println("Creating the directory [" + db.getProp(PMDIR) + "].");
			dir.mkdir();
		}

		dir = new File(getProp(PTCDIR));
		if (!dir.exists()) {
			System.out.println("Creating the directory [" + getProp(PTCDIR) + "].");
			dir.mkdir();
		}

		PitchFileHeader params = new PitchFileHeader();
		params.windowSizeInSeconds = Double.valueOf(getProp(WINSIZE));
		params.skipSizeInSeconds = Double.valueOf(getProp(SKIPSIZE));
		params.voicingThreshold = Double.valueOf(getProp(VOICINGTH));
		params.minimumF0 = Double.valueOf(getProp(MINF0));
		params.maximumF0 = Double.valueOf(getProp(MAXF0));

		System.out.println("Running autocorrelation based pitch marker...");
		for (int i = 0; i < baseNameArray.length; i++) {
			percent = 100 * i / baseNameArray.length;
			extractPitchmarks(baseNameArray[i], params);
		}
		System.out.println("Autocorrelation based pitch marking completed.");

		return true;
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	public static void main(String[] args) throws Exception {
		VoiceImportComponent vic = new AutocorrelationPitchmarker();
		DatabaseLayout db = new DatabaseLayout(vic);
		vic.compute();
	}
}
