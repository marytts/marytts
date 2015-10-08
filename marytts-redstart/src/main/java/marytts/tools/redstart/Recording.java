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
package marytts.tools.redstart;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat.Encoding;

import marytts.signalproc.analysis.EnergyAnalyser;
import marytts.signalproc.analysis.EnergyAnalyser_dB;
import marytts.signalproc.analysis.FrameBasedAnalyser.FrameAnalysisResult;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.AudioProcessor;
import marytts.util.data.audio.AudioRecorder;
import marytts.util.math.MathUtils;

/**
 * 
 * @author Mat Wilson &lt;mwilson@dfki.de&gt;
 */
public class Recording extends Speech {

	// ______________________________________________________________________
	// Instance fields

	// ______________________________________________________________________
	// Class fields
	public static final AudioFormat audioFormat = new AudioFormat(Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false);
	public AudioRecorder.BufferingRecorder recorder = null;
	public boolean isAmpClipped = false; // Boolean flag to indicate if recording is saturated (amplitude clipping occurred)
	public boolean isTempClipped = false; // Boolean flag to indicate if temporal clipping occurred (no silence at either end)
	public boolean isAmpWarning = false; // Boolean flag to indicate recording is close to being saturated
	public static final double ampYellowThreshold = -1.5;
	public static final double ampRedThreshold = -0.5;

	// ______________________________________________________________________
	// Instance methods

	// ______________________________________________________________________
	// Class methods

	/**
	 * Record for a given number of milliseconds and save as a wav file
	 * 
	 * @param line
	 *            line
	 * @param inlineFilter
	 *            inlineFilter
	 * @param millis
	 *            millis
	 */
	public void timedRecord(TargetDataLine line, AudioProcessor inlineFilter, int millis) {
		AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;
		recorder = new AudioRecorder.BufferingRecorder(line, targetType, getFile(), millis);
		if (inlineFilter != null) {
			recorder.setAudioProcessor(inlineFilter);
		}

		recorder.start();
		try {
			recorder.join();
			recorder = null;
		} catch (InterruptedException ie) {
		}
	}

	/**
	 * Stop an ongoing recording before the time is up. This may be useful when the user presses the stop button.
	 * 
	 * @return true when a recording was stopped, false if no recording was going on.
	 */
	public boolean stopRecording() {
		if (recorder != null) {
			recorder.stopRecordingNOW();
			return true;
		}
		return false;
	}

	/**
	 * Rename the file (basename).wav by appending a suffix. Example: If spike0003 has three recordings already, then the
	 * assumption is that spike0003.wav, spike0003a.wav and spike0003b.wav are the names of the existing files. The new name for
	 * the most recent recording (spike0003.wav) will be spike0003c.wav. The newly recorded file will then take the spike0003.wav
	 * name.
	 * 
	 */
	public void archiveLatestRecording() {
		if (fileCount == 0)
			return;
		File latest = new File(filePath, basename + ".wav");
		if (!latest.exists())
			return;

		int suffixCodeBase = 96; // Code point for the character before 'a'
		// Need an upper boundary
		int suffixCode = suffixCodeBase + fileCount;
		if (fileCount > 26)
			suffixCode = suffixCodeBase + 26;
		File newName = new File(filePath, basename + ((char) suffixCode) + ".wav");
		latest.renameTo(newName);

		// TESTCODE
		Test.output("|Recording.getRename| Renamed " + basename + " to " + newName.getPath());

	}

	public void checkForAmpClipping() {
		File f = getFile();
		if (!f.exists())
			return;

		double amplitude = getPeakAmplitude();
		// System.err.println("Peak amplitude: "+amplitude+" dB");
		if (amplitude >= ampRedThreshold) {
			this.isAmpClipped = true;
			this.isAmpWarning = false;
		} else if (amplitude >= ampYellowThreshold) {
			this.isAmpWarning = true;
			this.isAmpClipped = false;
		} else {
			this.isAmpClipped = false;
			this.isAmpWarning = false;
		}
	}

	public void checkForTempClipping() {
		File f = getFile();
		if (!f.exists())
			return;

		this.isTempClipped = false;

		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(f);
			double[] audio = new AudioDoubleDataSource(ais).getAllData();
			int samplingRate = (int) ais.getFormat().getSampleRate();
			int frameLength = (int) (0.005 * samplingRate); // each frame is 5 ms long
			EnergyAnalyser silenceFinder = new EnergyAnalyser_dB(new BufferedDoubleDataSource(audio), frameLength, samplingRate);
			FrameAnalysisResult[] energies = silenceFinder.analyseAllFrames();
			if (energies.length < 20) { // too short anyway
				this.isTempClipped = true;
				return; // temporal clipping
			}

			double silenceCutoff = silenceFinder.getSilenceCutoffFromKMeansClustering(0.5, 4);
			System.out.println("Silence cutoff: " + silenceCutoff);
			// Need at least 100 ms of silence at the beginning and at the end:

			double energy = 0;
			for (int i = 0; i < 20; i++) {
				energy += ((Double) energies[i].get()).doubleValue();
			}
			energy /= 20;
			if (energy >= silenceCutoff) {
				System.out.println("Initial clipping");
				this.isTempClipped = true;
			}

			energy = 0;
			for (int i = 1, len = energies.length; i <= 20; i++) {
				energy += ((Double) energies[len - i].get()).doubleValue();
			}
			energy /= 20;
			if (energy >= silenceCutoff) {
				System.out.println("Final clipping");
				this.isTempClipped = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	public double getPeakAmplitude() {
		File f = getFile();
		assert f.exists();
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(f);
			double[] audio = new AudioDoubleDataSource(ais).getAllData();
			double max = MathUtils.absMax(audio);
			int bits = ais.getFormat().getSampleSizeInBits();
			double possibleMax = 1.; // normalised scale
			return MathUtils.db((max * max) / (possibleMax * possibleMax));

		} catch (Exception e) {
			e.printStackTrace();
			return -30;
		}

	}

	// ______________________________________________________________________
	// Constructors

	/**
	 * Creates a new instance of Recording
	 * 
	 * @param filePath
	 *            The file path for the wav recordings
	 * @param basename
	 *            The basename for the currently selected prompt
	 */
	public Recording(File filePath, String basename) {
		super(filePath, basename);
	}

}
