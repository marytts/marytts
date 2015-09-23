/**
 * Copyright 2000-2007 DFKI GmbH.
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
package marytts.util.data.audio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.EnergyAnalyser;
import marytts.signalproc.filter.LowPassFilter;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.signal.SignalProcUtils;

/**
 * 
 * Audio Convertion Utilities
 * 
 * @author Sathish Chandra Pammi
 *
 */

public class AudioConverterUtils {
	public static class SequenceAudioProcessor implements AudioProcessor {
		private List<AudioProcessor> procs;

		public SequenceAudioProcessor(List<AudioProcessor> procs) {
			this.procs = procs;
		}

		public AudioInputStream apply(AudioInputStream ais) {
			AudioInputStream soFar = ais;
			for (AudioProcessor p : procs) {
				soFar = p.apply(soFar);
			}
			return soFar;
		}
	}

	public static class Stereo2Mono implements AudioProcessor {
		private int mode;

		/**
		 * Convert a stereo audio input stream to a mono audio input stream, using both channels.
		 */
		public Stereo2Mono() {
			this(AudioPlayer.STEREO);
		}

		/**
		 * Convert a stereo audio input stream, using the channels as indicated by mode.
		 * 
		 * @param mode
		 *            AudioPlayer.LEFT_ONLY, AudioPlayer.RIGHT_ONLY or AudioPlayer.STEREO.
		 */
		public Stereo2Mono(int mode) {
			this.mode = mode;
		}

		public AudioInputStream apply(AudioInputStream ais) {
			return new MonoAudioInputStream(ais, mode);
		}
	}

	/**
	 * A high-pass filter with flexible cutoff frequency and transition bandwidth.
	 * 
	 * @author marc
	 *
	 */
	public static class HighPassFilter implements AudioProcessor {
		private double cutoffFrequency;
		private double transitionBandwidth;

		public HighPassFilter(double cutoffFrequency, double transitionBandwidth) {
			this.cutoffFrequency = cutoffFrequency;
			this.transitionBandwidth = transitionBandwidth;
		}

		public AudioInputStream apply(AudioInputStream ais) {
			float samplingRate = ais.getFormat().getSampleRate();
			double cutOff = cutoffFrequency / samplingRate;
			double transition = transitionBandwidth / samplingRate;
			marytts.signalproc.filter.HighPassFilter hFilter = new marytts.signalproc.filter.HighPassFilter(cutOff, transition);
			DoubleDataSource audio = new AudioDoubleDataSource(ais);
			DoubleDataSource filtered = hFilter.apply(audio);
			return new DDSAudioInputStream(filtered, ais.getFormat());
		}
	}

	/**
	 * 24-Bit Audio to 16-bit Audio converter
	 * 
	 * @param ais
	 *            ais
	 * @return AudioInputStream audio input stream
	 * @throws Exception
	 *             exception
	 */

	public static AudioInputStream convertBit24ToBit16(AudioInputStream ais) throws Exception {

		int bitsPerSample = 24;
		int targetBitsPerSample = 16;

		int noOfbitsPerSample = ais.getFormat().getSampleSizeInBits();
		if (noOfbitsPerSample != bitsPerSample) {
			throw new Exception("24-Bit Audio Data Expected. But given Audio Data is " + noOfbitsPerSample + "-Bit data");
		}
		if (ais.getFormat().getChannels() != 1) {
			throw new Exception("Expected Audio type is Mono. But given Audio Data has " + ais.getFormat().getChannels()
					+ " channels");
		}

		float samplingRate = ais.getFormat().getSampleRate();
		int channels = ais.getFormat().getChannels();
		int nBytes = ais.available();
		boolean bigEndian = ais.getFormat().isBigEndian();
		byte[] byteBuf = new byte[nBytes];
		int nBytesRead = ais.read(byteBuf, 0, nBytes); // Reading all Bytes at a time
		int currentPos = 0;
		int noOfSamples = nBytes / 3;
		int[] sample = new int[noOfSamples];

		for (int i = 0; i < nBytesRead; i += 3, currentPos++) {
			byte lobyte;
			byte midbyte;
			byte hibyte;
			if (!bigEndian) {
				lobyte = byteBuf[i];
				midbyte = byteBuf[i + 1];
				hibyte = byteBuf[i + 2];
			} else {
				lobyte = byteBuf[i + 2];
				midbyte = byteBuf[i + 1];
				hibyte = byteBuf[i];
			}
			sample[currentPos] = hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
		}

		int maxBitPos = 0;
		int valueAfterShift;

		for (int i = 0; i < sample.length; i++) {
			for (int j = bitsPerSample; j >= 1; j--) {
				valueAfterShift = Math.abs(sample[i]) >> j;
				if (valueAfterShift != 0) {
					if (maxBitPos < j)
						maxBitPos = j;
					break;
				}
			}
		}

		int shiftBits = maxBitPos - targetBitsPerSample + 2; // need to change 24 to 16
		int sign;
		for (int i = 0; (shiftBits > 0 && i < sample.length); i++) {
			if (sample[i] < 0)
				sign = -1;
			else
				sign = 1;
			sample[i] = sign * (Math.abs(sample[i]) >> shiftBits);
		}

		currentPos = 0; // off
		int nRead = sample.length;
		byte[] b = new byte[2 * sample.length];
		int MAX_AMPLITUDE = 32767;

		// Conversion to BYTE ARRAY
		for (int i = 0; i < nRead; i++, currentPos += 2) {

			int samp = sample[i];
			if (samp > MAX_AMPLITUDE || samp < -MAX_AMPLITUDE) {
				System.err.println("Warning: signal amplitude out of range: " + samp);
			}
			byte hibyte = (byte) (samp >> 8);
			byte lobyte = (byte) (samp & 0xFF);
			if (!bigEndian) {
				b[currentPos] = lobyte;
				b[currentPos + 1] = hibyte;
			} else {
				b[currentPos] = hibyte;
				b[currentPos + 1] = lobyte;
			}
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		boolean signed = true; // true,false

		AudioFormat af = new AudioFormat(samplingRate, targetBitsPerSample, channels, signed, bigEndian);

		long lengthInSamples = b.length / (targetBitsPerSample / 8);

		return new AudioInputStream(bais, af, lengthInSamples);
	}

	/**
	 * 24-Bit Audio to 16-bit Audio converter
	 * 
	 * @param ais
	 *            ais
	 * @param shiftBits
	 *            shift bits
	 * @return AudioInputStream
	 * @throws Exception
	 *             exception
	 */

	public static AudioInputStream convertBit24ToBit16(AudioInputStream ais, int shiftBits) throws Exception {

		int bitsPerSample = 24;
		int targetBitsPerSample = 16;

		int noOfbitsPerSample = ais.getFormat().getSampleSizeInBits();
		if (noOfbitsPerSample != bitsPerSample) {
			throw new Exception("24-Bit Audio Data Expected. But given Audio Data is " + noOfbitsPerSample + "-Bit data");
		}
		if (ais.getFormat().getChannels() != 1) {
			throw new Exception("Expected Audio type is Mono. But given Audio Data has " + ais.getFormat().getChannels()
					+ " channels");
		}
		// System.out.println("Shift bits: "+shiftBits);
		float samplingRate = ais.getFormat().getSampleRate();
		int channels = ais.getFormat().getChannels();
		int nBytes = ais.available();
		boolean bigEndian = ais.getFormat().isBigEndian();
		byte[] byteBuf = new byte[nBytes];
		int nBytesRead = ais.read(byteBuf, 0, nBytes); // Reading all Bytes at a time
		int currentPos = 0;
		int noOfSamples = nBytes / 3;
		int[] sample = new int[noOfSamples];

		for (int i = 0; i < nBytesRead; i += 3, currentPos++) {
			byte lobyte;
			byte midbyte;
			byte hibyte;
			if (!bigEndian) {
				lobyte = byteBuf[i];
				midbyte = byteBuf[i + 1];
				hibyte = byteBuf[i + 2];
			} else {
				lobyte = byteBuf[i + 2];
				midbyte = byteBuf[i + 1];
				hibyte = byteBuf[i];
			}
			sample[currentPos] = hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
		}

		int sign;
		for (int i = 0; (shiftBits > 0 && i < sample.length); i++) {
			if (sample[i] < 0)
				sign = -1;
			else
				sign = 1;
			sample[i] = sign * (Math.abs(sample[i]) >> shiftBits);
		}

		currentPos = 0; // off
		int nRead = sample.length;
		byte[] b = new byte[2 * sample.length];
		int MAX_AMPLITUDE = 32767;

		// Conversion to BYTE ARRAY
		for (int i = 0; i < nRead; i++, currentPos += 2) {

			int samp = sample[i];
			if (samp > MAX_AMPLITUDE || samp < -MAX_AMPLITUDE) {
				System.err.println("Warning: signal amplitude out of range: " + samp);
			}
			byte hibyte = (byte) (samp >> 8);
			byte lobyte = (byte) (samp & 0xFF);
			if (!bigEndian) {
				b[currentPos] = lobyte;
				b[currentPos + 1] = hibyte;
			} else {
				b[currentPos] = hibyte;
				b[currentPos + 1] = lobyte;
			}
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		boolean signed = true; // true,false

		AudioFormat af = new AudioFormat(samplingRate, targetBitsPerSample, channels, signed, bigEndian);

		long lengthInSamples = b.length / (targetBitsPerSample / 8);

		return new AudioInputStream(bais, af, lengthInSamples);
	}

	/**
	 * Get samples in Integer Format (un-normalized) from AudioInputStream
	 * 
	 * @param ais
	 *            ais
	 * @return samples
	 * @throws Exception
	 *             exception
	 */
	public static int[] getSamples(AudioInputStream ais) throws Exception {

		int noOfbitsPerSample = ais.getFormat().getSampleSizeInBits();
		float samplingRate = ais.getFormat().getSampleRate();
		int channels = ais.getFormat().getChannels();
		int nBytes = ais.available();
		boolean bigEndian = ais.getFormat().isBigEndian();
		byte[] byteBuf = new byte[nBytes];
		int nBytesRead = ais.read(byteBuf, 0, nBytes); // Reading all Bytes at a time
		int noOfBytesPerSample = noOfbitsPerSample / 8;

		int[] samples = new int[nBytes / noOfBytesPerSample];
		int currentPos = 0; // off

		if (noOfBytesPerSample == 1) {
			for (int i = 0; i < nBytesRead; i++, currentPos++) {
				samples[currentPos] = (byteBuf[i] << 8);
			}

		} else if (noOfBytesPerSample == 2) { // 16 bit
			for (int i = 0; i < nBytesRead; i += 2, currentPos++) {
				int sample;
				byte lobyte;
				byte hibyte;
				if (!bigEndian) {
					lobyte = byteBuf[i];
					hibyte = byteBuf[i + 1];
				} else {
					lobyte = byteBuf[i + 1];
					hibyte = byteBuf[i];
				}
				samples[currentPos] = hibyte << 8 | lobyte & 0xFF;
			}

		} else { // noOfBytesPerSample == 3, i.e. 24 bit
			for (int i = 0; i < nBytesRead; i += 3, currentPos++) {
				int sample;
				byte lobyte;
				byte midbyte;
				byte hibyte;
				if (!bigEndian) {
					lobyte = byteBuf[i];
					midbyte = byteBuf[i + 1];
					hibyte = byteBuf[i + 2];
				} else {
					lobyte = byteBuf[i + 2];
					midbyte = byteBuf[i + 1];
					hibyte = byteBuf[i];
				}
				samples[currentPos] = hibyte << 16 | (midbyte & 0xFF) << 8 | lobyte & 0xFF;
			}
		}

		return samples;
	}

	/**
	 * DownSampling given Audio Input Stream
	 * 
	 * @param ais
	 *            ais
	 * @param targetSamplingRate
	 *            target sampling rate
	 * @return oais
	 * @throws Exception
	 *             exception
	 */
	public static AudioInputStream downSampling(AudioInputStream ais, int targetSamplingRate) throws Exception {

		float currentSamplingRate = ais.getFormat().getSampleRate();
		if (targetSamplingRate >= currentSamplingRate) {
			throw new Exception("Requested sampling rate " + targetSamplingRate
					+ " is greater than or equal to Audio sampling rate " + currentSamplingRate);
		}
		int noOfbitsPerSample = ais.getFormat().getSampleSizeInBits();
		int channels = ais.getFormat().getChannels();
		int nBytes = ais.available();

		boolean bigEndian = ais.getFormat().isBigEndian();
		double[] samples = new AudioDoubleDataSource(ais).getAllData();

		// **** Filtering to Remove Aliasing ******
		double filterCutof = 0.5 * (double) targetSamplingRate / currentSamplingRate;
		// System.out.println("filterCutof: "+filterCutof);
		LowPassFilter filter = new LowPassFilter(filterCutof);
		samples = filter.apply(samples);
		double duration = (double) samples.length / currentSamplingRate;
		// System.out.println("duration: "+duration);
		int newSampleLen = (int) Math.floor(duration * targetSamplingRate);
		// System.out.println("New Sample Length: "+newSampleLen);
		double fraction = (double) currentSamplingRate / targetSamplingRate;
		// System.out.println("Fraction: "+fraction);

		double[] newSignal = new double[newSampleLen];
		for (int i = 0; i < newSignal.length; i++) {
			double posIdx = fraction * i;
			int nVal = (int) Math.floor(posIdx);
			double diffVal = posIdx - nVal;

			// Linear Interpolation
			newSignal[i] = (diffVal * samples[nVal + 1]) + ((1 - diffVal) * samples[nVal]);

		}
		boolean signed = true; // true,false
		AudioFormat af = new AudioFormat(targetSamplingRate, noOfbitsPerSample, channels, signed, bigEndian);

		DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(newSignal), af);

		return oais;
	}

	/**
	 * Removes endpoints from given file.
	 * 
	 * @param inputFile
	 *            input file
	 * @param outputFile
	 *            output file
	 * @param energyBufferLength
	 *            energyBufferLength
	 * @param speechStartLikelihood
	 *            speechStartLikelihood
	 * @param speechEndLikelihood
	 *            speechEndLikelihood
	 * @param shiftFromMinimumEnergyCenter
	 *            shiftFromMinimumEnergyCenter
	 * @param numClusters
	 *            numClusters
	 * @param minimumStartSilenceInSeconds
	 *            minimumStartSilenceInSeconds
	 * @param minimumEndSilenceInSeconds
	 *            minimumEndSilenceInSeconds
	 * @throws IOException
	 *             IOException
	 * @throws UnsupportedAudioFileException
	 *             UnsupportedAudioFileException
	 */
	public static void removeEndpoints(String inputFile, String outputFile, int energyBufferLength, double speechStartLikelihood,
			double speechEndLikelihood, double shiftFromMinimumEnergyCenter, int numClusters,
			double minimumStartSilenceInSeconds, double minimumEndSilenceInSeconds) throws IOException,
			UnsupportedAudioFileException {
		/*
		 * 1. identify and remove end points 2. make sure at least some desired amount of silence in the beginning and at the end
		 * 3. store as output wavefile
		 */

		AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inputFile));

		if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
		}
		if (ais.getFormat().getChannels() > 1) {
			throw new IllegalArgumentException("Can only deal with mono audio signals");
		}
		int samplingRate = (int) ais.getFormat().getSampleRate();
		DoubleDataSource signal = new AudioDoubleDataSource(ais);

		int framelength = (int) (0.01 /* seconds */* samplingRate);
		EnergyAnalyser ea = new EnergyAnalyser(signal, framelength, framelength, samplingRate);
		// double[][] speechStretches = ea.getSpeechStretches();

		double[][] speechStretches = ea.getSpeechStretchesUsingEnergyHistory(energyBufferLength, speechStartLikelihood,
				speechEndLikelihood, shiftFromMinimumEnergyCenter, numClusters);

		ais.close();

		try {
			ais = AudioSystem.getAudioInputStream(new File(inputFile));
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		signal = new AudioDoubleDataSource(ais);
		double[] x = signal.getAllData();

		ais.close();

		if (speechStretches.length == 0) {
			System.out.println("No segments detected in " + inputFile + " copying whole file...");

			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x), ais.getFormat());
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));
		} else {
			int numStretches = speechStretches.length;
			int speechStartIndex = (int) (samplingRate * speechStretches[0][0]);
			int speechEndIndex = (int) (samplingRate * speechStretches[numStretches - 1][1]);

			// Check if sufficient silence exists in the input waveform, if not generate as required
			int silStartRequired = Math.max(0, (int) (samplingRate * minimumStartSilenceInSeconds));
			int silStartLen = 0;
			if (speechStartIndex < silStartRequired) {
				silStartLen = silStartRequired - speechStartIndex;
				speechStartIndex = 0;
			} else
				speechStartIndex -= silStartRequired;

			double[] silStart = null;
			if (silStartLen > 0)
				silStart = SignalProcUtils.getWhiteNoise(silStartLen, 1e-20);

			int silEndRequired = Math.max(0, (int) (samplingRate * minimumEndSilenceInSeconds));
			int silEndLen = 0;
			if (x.length - speechEndIndex < silEndRequired) {
				silEndLen = silEndRequired - (x.length - speechEndIndex);
				speechEndIndex = x.length - 1;
			} else
				speechEndIndex += silEndRequired;

			double[] silEnd = null;
			if (silEndLen > 0)
				silEnd = SignalProcUtils.getWhiteNoise(silEndLen, 1e-20);
			//

			double[] y = null;
			if (speechEndIndex - speechStartIndex + silStartLen + silEndLen > 0)
				y = new double[speechEndIndex - speechStartIndex + silStartLen + silEndLen];
			else
				throw new Error("No output samples to write for " + inputFile);

			int start = 0;
			if (silStartLen > 0) {
				System.arraycopy(silStart, 0, y, start, silStartLen);
				start += silStartLen;
			}

			if (speechEndIndex - speechStartIndex > 0) {
				System.arraycopy(x, speechStartIndex, y, start, speechEndIndex - speechStartIndex);
				start += (speechEndIndex - speechStartIndex);
			}

			if (silEndLen > 0) {
				System.arraycopy(silEnd, 0, y, start, silEndLen);
				start += silEndLen;
			}

			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), ais.getFormat());
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));
		}
	}
}
