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
 * @author Oytun T&uuml;rk
 * 
 */
public class FIRWaveletFilterBankAnalyser extends FilterBankAnalyserBase {
	public FIRFilter[][] filters;
	public double[][] normalizationFilterTransformedIRs;
	public int numLevels;
	double samplingRateInHz;
	double[] samplingRates;

	public FIRWaveletFilterBankAnalyser(int numLevelsIn, double samplingRateInHzIn) {
		numLevels = Math.max(0, numLevelsIn);

		samplingRateInHz = samplingRateInHzIn;
		int i, j;

		samplingRates = new double[numLevels + 1];

		samplingRates[0] = 0.5 * samplingRateInHz;
		for (i = 1; i < numLevels; i++)
			samplingRates[i] = 0.5 * samplingRates[i - 1];
		samplingRates[numLevels] = samplingRates[numLevels - 1];

		int filterOrder;
		int maxFreq;
		filters = new FIRFilter[numLevels][2];
		normalizationFilterTransformedIRs = new double[numLevels][];
		for (i = 0; i < numLevels; i++) {
			filterOrder = SignalProcUtils.getFIRFilterOrder((int) (samplingRates[i]));
			filters[i][0] = new LowPassFilter(0.25, filterOrder);
			filters[i][1] = new HighPassFilter(0.25, filterOrder);

			maxFreq = filters[i][0].transformedIR.length / 2 + 1;
			normalizationFilterTransformedIRs[i] = new double[maxFreq];

			Arrays.fill(normalizationFilterTransformedIRs[i], 0.0);

			normalizationFilterTransformedIRs[i][0] += Math.abs(filters[i][0].transformedIR[0]);
			normalizationFilterTransformedIRs[i][0] += Math.abs(filters[i][1].transformedIR[0]);
			normalizationFilterTransformedIRs[i][maxFreq - 1] += Math.abs(filters[i][0].transformedIR[1]);
			normalizationFilterTransformedIRs[i][maxFreq - 1] += Math.abs(filters[i][1].transformedIR[1]);
			for (j = 1; j < maxFreq - 1; j++) {
				normalizationFilterTransformedIRs[i][j] += Math.sqrt(filters[i][0].transformedIR[2 * j]
						* filters[i][0].transformedIR[2 * j] + filters[i][0].transformedIR[2 * j + 1]
						* filters[i][0].transformedIR[2 * j + 1]);
				normalizationFilterTransformedIRs[i][j] += Math.sqrt(filters[i][1].transformedIR[2 * j]
						* filters[i][1].transformedIR[2 * j] + filters[i][1].transformedIR[2 * j + 1]
						* filters[i][1].transformedIR[2 * j + 1]);
			}

			for (j = 0; j < maxFreq; j++)
				normalizationFilterTransformedIRs[i][j] = 1.0 / normalizationFilterTransformedIRs[i][j];
		}
	}

	public Subband[] apply(double[] x) {
		Subband[] subbands = null;

		if (filters != null && x != null && numLevels >= 0) {
			int i;
			subbands = new Subband[numLevels + 1];
			double[] lowBand = null;
			double[] highBand = null;

			int count = 0;
			for (i = 0; i < numLevels; i++) {
				if (i == 0) {
					lowBand = filters[i][0].apply(x);
					lowBand = SignalProcUtils.decimate(lowBand, 2.0);

					highBand = filters[i][1].apply(x);
					highBand = SignalProcUtils.decimate(highBand, 2.0);
				} else {
					highBand = filters[i][1].apply(lowBand);
					highBand = SignalProcUtils.decimate(highBand, 2.0);

					lowBand = filters[i][0].apply(lowBand);
					lowBand = SignalProcUtils.decimate(lowBand, 2.0);
				}

				subbands[i] = new Subband(highBand, samplingRates[i]);
			}

			subbands[numLevels] = new Subband(lowBand, samplingRates[numLevels]);
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
		int numLevels = 1;

		FIRWaveletFilterBankAnalyser analyser = new FIRWaveletFilterBankAnalyser(numLevels, samplingRate);
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
