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

	/**
	 * Mix a number of audio files to each of a set of audio files, normalizing these audio files to the average power of the
	 * reference audio files.
	 * 
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		if (true) {
			List audio = new ArrayList(); // to play in parallel to each argument
			double[] audioData = null;
			List referenceAudio = new ArrayList(); // to normalise power
			List noiseSpecs = new ArrayList();
			double maxDuration = 0;
			int i = 0;
			String prop;
			// The audio format of the first argument is the target format!
			AudioFormat format;
			if (args.length > 0)
				format = AudioSystem.getAudioInputStream(new File(args[0])).getFormat();
			else
				format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, // samples per second
						16, // bits per sample
						1, // mono
						2, // nr. of bytes per frame
						16000, // nr. of frames per second
						false // little-endian
				);
			while (!(prop = System.getProperty("audio." + (++i), "")).equals("")) {
				DoubleDataSource dds = null;
				if (prop.startsWith("noise:")) {
					noiseSpecs.add(prop);
				} else {
					String[] info = prop.split(":");
					String filename = info[info.length - 1];
					double start = 0;
					if (info.length > 1)
						start = Double.valueOf(info[0]).doubleValue();
					AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filename));
					if (!format.equals(ais.getFormat())) // convert to target format
						ais = AudioSystem.getAudioInputStream(format, ais);
					double[] signal = new AudioDoubleDataSource(ais).getAllData();
					double duration = signal.length / format.getSampleRate();
					if (duration > maxDuration)
						maxDuration = duration;
					referenceAudio.add(new BufferedDoubleDataSource(signal));
					dds = new BufferedDoubleDataSource(signal);
					if (start > 0)
						dds = new SequenceDoubleDataSource(new DoubleDataSource[] {
								new SilenceDoubleDataSource((long) (start * format.getSampleRate())), dds });
					audio.add(dds);
				}
			}

			EnergyNormaliser powerNormaliser = null;
			if (referenceAudio.size() > 0) {
				powerNormaliser = new EnergyNormaliser(new SequenceDoubleDataSource(referenceAudio));
				System.err.println("Reference power: " + powerNormaliser.getReferencePower());
			}

			for (Iterator it = noiseSpecs.iterator(); it.hasNext();) {
				String spec = (String) it.next();
				String[] info = spec.split(":");
				double start = 0;
				if (info.length > 2)
					start = Double.valueOf(info[1]).doubleValue();
				double duration = maxDuration - start;
				if (info.length > 3)
					duration = Double.valueOf(info[2]).doubleValue();
				double db = Double.valueOf(info[info.length - 1]).doubleValue();
				DoubleDataSource noise = new NoiseDoubleDataSource((long) (duration * format.getSampleRate()), db);
				if (start > 0)
					noise = new SequenceDoubleDataSource(new DoubleDataSource[] {
							new SilenceDoubleDataSource((long) (start * format.getSampleRate())), noise });
				audio.add(noise);
			}

			if (audio.size() > 0)
				audioData = new MixerDoubleDataSource(audio).getAllData();

			// If no arguments are present:
			if (args.length == 0) {
				AudioInputStream audioStream = new DDSAudioInputStream(new BufferedDoubleDataSource(audioData), format);
				String prefix = System.getProperty("prefix", "mixed_");
				File outFile = new File(prefix + ".wav"); // in the current directory
				AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outFile);
				System.out.println("Wrote " + outFile.getPath());
				System.exit(0);
			}

			// How to time the audio given as the arguments:
			double argsStart = Double.valueOf(System.getProperty("audio.args", "0")).doubleValue();
			for (int k = 0; k < args.length; k++) {
				List result = new ArrayList();
				if (audioData != null) {
					result.add(new BufferedDoubleDataSource(audioData));
				}
				File inFile = new File(args[k]);
				AudioInputStream ais = AudioSystem.getAudioInputStream(inFile);
				if (!format.equals(ais.getFormat()))
					ais = AudioSystem.getAudioInputStream(format, ais);
				DoubleDataSource dds = new AudioDoubleDataSource(ais);
				if (powerNormaliser != null)
					dds = powerNormaliser.apply(dds);
				if (argsStart > 0) {
					dds = new SequenceDoubleDataSource(new DoubleDataSource[] {
							new SilenceDoubleDataSource((long) (argsStart * format.getSampleRate())), dds });
				}
				result.add(dds);
				DoubleDataSource resultDDS = new MixerDoubleDataSource(result);
				AudioInputStream resultStream = new DDSAudioInputStream(resultDDS, format);
				String prefix = System.getProperty("prefix", "mixed_");
				String filename = inFile.getName();
				filename = prefix + filename.substring(0, filename.lastIndexOf('.')) + ".wav";
				File outFile = new File(filename); // in the current directory
				AudioSystem.write(resultStream, AudioFileFormat.Type.WAVE, outFile);
				System.out.println("Wrote " + outFile.getPath());
			}
		} else // Simple mixing of two files
		{
			mixTwoFiles("d:/1_2klpf_sinTScaled.wav", 1.0, "d:/1_2khpf.wav", 1.0, "d:/1_merged.wav");
		}
	}

}
