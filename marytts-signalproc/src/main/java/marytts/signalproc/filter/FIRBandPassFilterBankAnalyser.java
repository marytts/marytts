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
package marytts.signalproc.filter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.signal.SignalProcUtils;

/**
 * This is a simple FIR bandpass filterbank structure with no resampling operations The filters are overlapping and a simple
 * DFT-based frequency response estimation method is used for reducing reconstruction error due to non-ideal filtering scheme and
 * overlap among filters Given a sampling rate and a set of lower and upper cutoff frequency values in Hz, a set of bandpass
 * filters that overlap by some amount in frequency
 * 
 * @author Oytun T&uuml;rk
 */
public class FIRBandPassFilterBankAnalyser extends FilterBankAnalyserBase {
	public static final double OVERLAP_AROUND_1000HZ_DEFAULT = 100.0;
	public double overlapAround1000Hz;
	public int samplingRateInHz;

	public FIRFilter[] filters;
	public double[] normalizationFilterTransformedIR;
	public double[] lowerCutOffsInHz;
	public double[] upperCutOffsInHz;

	public FIRBandPassFilterBankAnalyser(int numBands, int samplingRateInHz) {
		this(numBands, samplingRateInHz, OVERLAP_AROUND_1000HZ_DEFAULT);
	}

	public FIRBandPassFilterBankAnalyser(int numBands, int samplingRateInHzIn, double overlapAround1000HzIn) {
		samplingRateInHz = samplingRateInHzIn;
		double halfSamplingRate = 0.5 * samplingRateInHz;
		lowerCutOffsInHz = new double[numBands];
		upperCutOffsInHz = new double[numBands];
		double overlapInHz;
		int i;
		overlapAround1000Hz = overlapAround1000HzIn;

		for (i = 0; i < numBands; i++) {
			if (i < numBands - 1)
				upperCutOffsInHz[i] = samplingRateInHz / Math.pow(2, numBands - i);
			else
				upperCutOffsInHz[i] = halfSamplingRate;

			if (i == 0)
				lowerCutOffsInHz[i] = 0.0;
			else
				lowerCutOffsInHz[i] = upperCutOffsInHz[i - 1];

			overlapInHz = 0.5 * (upperCutOffsInHz[i] + lowerCutOffsInHz[i]) / (1000.0 / overlapAround1000Hz);

			if (i > 0)
				lowerCutOffsInHz[i] -= overlapInHz;
			if (i < numBands - 1)
				upperCutOffsInHz[i] += overlapInHz;

			System.out.println("Subband #" + String.valueOf(i + 1) + " - Lower cutoff: " + String.valueOf(lowerCutOffsInHz[i])
					+ " Upper cutoff: " + String.valueOf(upperCutOffsInHz[i]));
		}

		initialise(lowerCutOffsInHz, upperCutOffsInHz, overlapAround1000HzIn);
	}

	public FIRBandPassFilterBankAnalyser(double[] lowerCutOffsInHzIn, double[] upperCutOffsInHzIn, int samplingRateInHzIn) {
		this(lowerCutOffsInHzIn, upperCutOffsInHzIn, samplingRateInHzIn, OVERLAP_AROUND_1000HZ_DEFAULT);
	}

	public FIRBandPassFilterBankAnalyser(double[] lowerCutOffsInHzIn, double[] upperCutOffsInHzIn, int samplingRateInHzIn,
			double overlapAround1000HzIn) {
		samplingRateInHz = samplingRateInHzIn;
		initialise(lowerCutOffsInHzIn, upperCutOffsInHzIn, overlapAround1000HzIn);
	}

	public void initialise(double[] lowerCutOffsInHzIn, double[] upperCutOffsInHzIn, double overlapAround1000HzIn) {
		normalizationFilterTransformedIR = null;

		if (lowerCutOffsInHzIn != null && upperCutOffsInHzIn != null) {
			assert lowerCutOffsInHzIn.length == upperCutOffsInHzIn.length;
			lowerCutOffsInHz = new double[lowerCutOffsInHzIn.length];
			upperCutOffsInHz = new double[upperCutOffsInHzIn.length];
			System.arraycopy(lowerCutOffsInHzIn, 0, lowerCutOffsInHz, 0, lowerCutOffsInHzIn.length);
			System.arraycopy(upperCutOffsInHzIn, 0, upperCutOffsInHz, 0, upperCutOffsInHzIn.length);

			int i;
			filters = new FIRFilter[lowerCutOffsInHz.length];
			int filterOrder = SignalProcUtils.getFIRFilterOrder(samplingRateInHz);
			double normalizedLowerCutoff;
			double normalizedUpperCutoff;

			overlapAround1000Hz = overlapAround1000HzIn;

			for (i = 0; i < lowerCutOffsInHz.length; i++)
				assert lowerCutOffsInHz[i] < upperCutOffsInHz[i];

			for (i = 0; i < lowerCutOffsInHz.length; i++) {
				if (lowerCutOffsInHz[i] <= 0.0) {
					normalizedUpperCutoff = Math.min(upperCutOffsInHz[i] / samplingRateInHz, 0.5);
					normalizedUpperCutoff = Math.max(normalizedUpperCutoff, 0.0);
					filters[i] = new LowPassFilter(normalizedUpperCutoff, filterOrder);
				} else if (upperCutOffsInHz[i] >= 0.5 * samplingRateInHz) {
					normalizedLowerCutoff = Math.max(lowerCutOffsInHz[i] / samplingRateInHz, 0.0);
					normalizedLowerCutoff = Math.min(normalizedLowerCutoff, 0.5);
					filters[i] = new HighPassFilter(normalizedLowerCutoff, filterOrder);
				} else {
					normalizedLowerCutoff = Math.max(lowerCutOffsInHz[i] / samplingRateInHz, 0.0);
					normalizedLowerCutoff = Math.min(normalizedLowerCutoff, 0.5);
					normalizedUpperCutoff = Math.min(upperCutOffsInHz[i] / samplingRateInHz, 0.5);
					normalizedUpperCutoff = Math.max(normalizedUpperCutoff, 0.0);

					assert normalizedLowerCutoff < normalizedUpperCutoff;

					filters[i] = new BandPassFilter(normalizedLowerCutoff, normalizedUpperCutoff, filterOrder);
				}
			}

			int maxFreq = filters[0].transformedIR.length / 2 + 1;

			// Estimate a smooth gain normalization filter
			normalizationFilterTransformedIR = new double[maxFreq];
			Arrays.fill(normalizationFilterTransformedIR, 0.0);

			int j;
			for (i = 0; i < filters.length; i++) {
				normalizationFilterTransformedIR[0] += Math.abs(filters[i].transformedIR[0]);
				normalizationFilterTransformedIR[maxFreq - 1] += Math.abs(filters[i].transformedIR[1]);
				for (j = 1; j < maxFreq - 1; j++)
					normalizationFilterTransformedIR[j] += Math.sqrt(filters[i].transformedIR[2 * j]
							* filters[i].transformedIR[2 * j] + filters[i].transformedIR[2 * j + 1]
							* filters[i].transformedIR[2 * j + 1]);
			}

			for (j = 0; j < maxFreq; j++)
				normalizationFilterTransformedIR[j] = 1.0 / normalizationFilterTransformedIR[j];

			// MaryUtils.plot(normalizationFilterTransformedIR, "Normalization filter");
			//
		}
	}

	public Subband[] apply(double[] x) {
		Subband[] subbands = null;

		if (filters != null && x != null) {
			int i;
			subbands = new Subband[filters.length];
			for (i = 0; i < filters.length; i++) {
				if (filters[i] instanceof LowPassFilter)
					subbands[i] = new Subband(filters[i].apply(x), samplingRateInHz, 0.0,
							((LowPassFilter) filters[i]).normalisedCutoffFrequency * samplingRateInHz);
				else if (filters[i] instanceof HighPassFilter)
					subbands[i] = new Subband(filters[i].apply(x), samplingRateInHz,
							((HighPassFilter) filters[i]).normalisedCutoffFrequency * samplingRateInHz, 0.5 * samplingRateInHz);
				else if (filters[i] instanceof BandPassFilter)
					subbands[i] = new Subband(filters[i].apply(x), samplingRateInHz,
							((BandPassFilter) filters[i]).lowerNormalisedCutoffFrequency * samplingRateInHz,
							((BandPassFilter) filters[i]).upperNormalisedCutoffFrequency * samplingRateInHz);
			}
		}
		//

		return subbands;
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		int i;
		int numBands = 4;
		double overlapAround1000Hz = 100.0;

		FIRBandPassFilterBankAnalyser analyser = new FIRBandPassFilterBankAnalyser(numBands, samplingRate, overlapAround1000Hz);
		Subband[] subbands = analyser.apply(x);

		DDSAudioInputStream outputAudio;
		AudioFormat outputFormat;
		String outFileName;

		// Write highpass components 0 to numLevels-1
		for (i = 0; i < subbands.length; i++) {
			outputFormat = new AudioFormat((int) (subbands[i].samplingRate), inputAudio.getFormat().getSampleSizeInBits(),
					inputAudio.getFormat().getChannels(), true, true);
			outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(subbands[i].waveform), outputFormat);
			outFileName = args[0].substring(0, args[0].length() - 4) + "_band" + String.valueOf(i + 1) + ".wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}
	}
}
