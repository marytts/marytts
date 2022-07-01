/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.MixerDoubleDataSource;
import marytts.util.data.NoiseDoubleDataSource;
import marytts.util.data.SequenceDoubleDataSource;
import marytts.util.data.SilenceDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;

public class AudioFileMixer {
	public static void mixTwoFiles(String inputFile1, double mixAmount1, String inputFile2, double mixAmount2, String outputFile)
			throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio1 = AudioSystem.getAudioInputStream(new File(inputFile1));
		int samplingRate1 = (int) inputAudio1.getFormat().getSampleRate();
		AudioDoubleDataSource signal1 = new AudioDoubleDataSource(inputAudio1);
		double[] x1 = signal1.getAllData();

		AudioInputStream inputAudio2 = AudioSystem.getAudioInputStream(new File(inputFile2));
		int samplingRate2 = (int) inputAudio1.getFormat().getSampleRate();
		AudioDoubleDataSource signal2 = new AudioDoubleDataSource(inputAudio2);
		double[] x2 = signal2.getAllData();

		if (samplingRate1 != samplingRate2)
			System.out.println("Error! Sampling rates must be identical for mixing...");
		else {
			int i;
			double[] x3 = new double[Math.max(x1.length, x2.length)];

			if (x1.length > x2.length) {
				for (i = 0; i < x2.length; i++)
					x3[i] = mixAmount1 * x1[i] + mixAmount2 * x2[i];
				for (i = x2.length; i < x3.length; i++)
					x3[i] = mixAmount1 * x1[i];
			} else {
				for (i = 0; i < x1.length; i++)
					x3[i] = mixAmount1 * x1[i] + mixAmount2 * x2[i];
				for (i = x1.length; i < x3.length; i++)
					x3[i] = mixAmount2 * x2[i];
			}

			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x3), inputAudio1.getFormat());
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));
		}
	}
}
