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
package marytts.signalproc.sinusoidal.test;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

/**
 * All tester classes should be derived from this baseline class
 * 
 * @author Oytun T&uuml;rk
 */
public class BaseTester {
	public static double DEFAULT_ABS_MAX_VAL = 26000.0;
	public static float DEFAULT_AMP = 0.8f;
	public static float DEFAULT_DUR = 1.0f;
	public static int DEFAULT_FS = 16000;
	public static float DEFAULT_WINDOW_SIZE_FOR_PITCH_CONTOUR = 0.020f;
	public static float DEFAULT_SKIP_SIZE_FOR_PITCH_CONTOUR = 0.010f;
	public double[] signal;
	public int[] pitchMarks;
	public double[] f0s;
	public int fs;
	public float ws; // Window size in seconds
	public float ss; // Skip size in seconds

	public BaseTester() {
		ws = DEFAULT_WINDOW_SIZE_FOR_PITCH_CONTOUR;
		ss = DEFAULT_SKIP_SIZE_FOR_PITCH_CONTOUR;
	}

	public void write(String outWavFile) throws IOException {
		write(outWavFile, DEFAULT_ABS_MAX_VAL);
	}

	public void write(String outWavFile, double defaultAbsMaxVal) throws IOException {
		String outPtcFile = StringUtils.modifyExtension(outWavFile, ".ptc");
		write(outWavFile, outPtcFile, defaultAbsMaxVal);
	}

	public void write(String outWavFile, String outPtcFile) throws IOException {
		write(outWavFile, outPtcFile, DEFAULT_ABS_MAX_VAL);
	}

	public void write(String outWavFile, String outPtcFile, double defaultAbsMaxVal) throws IOException {
		if (signal != null) {
			if (signal != null && (outWavFile != null && !outWavFile.equals(""))) {
				double maxVal = MathUtils.getAbsMax(signal);
				for (int i = 0; i < signal.length; i++)
					signal[i] *= (defaultAbsMaxVal / 32767.0) / maxVal;

				AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fs, // samples per second
						16, // bits per sample
						1, // mono
						2, // nr. of bytes per frame
						fs, // nr. of frames per second
						true); // big-endian;

				DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(signal), format);
				AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outWavFile));
			}

			if (pitchMarks != null && (outPtcFile != null && !outWavFile.equals(""))) {
				PitchReaderWriter.write_pitch_file(outPtcFile, f0s, ws, ss, fs);
				// FileUtils.writeToBinaryFile(pitchMarks, outPtcFile); //Pitch mark file
			}

		}
	}
}
