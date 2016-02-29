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
public class FIRBandPassFilterBankSynthesiser {
	public FIRBandPassFilterBankSynthesiser() {

	}

	public double[] apply(FIRBandPassFilterBankAnalyser analyser, Subband[] subbands) {
		return apply(analyser, subbands, true);
	}

	public double[] apply(FIRBandPassFilterBankAnalyser analyser, Subband[] subbands, boolean bNormalizeInOverlappingRegions) {
		double[] x = null;

		if (analyser != null && analyser.filters != null && subbands != null) {
			int i, j, maxLen;

			assert analyser.filters.length == subbands.length;

			// Add all subbands up and then apply the smooth gain normalization filter
			maxLen = subbands[0].waveform.length;
			for (i = 1; i < subbands.length; i++)
				maxLen = Math.max(maxLen, subbands[i].waveform.length);

			x = new double[maxLen];
			Arrays.fill(x, 0.0);
			for (i = 0; i < subbands.length; i++) {
				for (j = 0; j < subbands[i].waveform.length; j++)
					x[j] += subbands[i].waveform[j];
			}

			x = SignalProcUtils.filterfd(analyser.normalizationFilterTransformedIR, x, subbands[0].samplingRate);
		}

		return x;
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

		FIRBandPassFilterBankSynthesiser synthesiser = new FIRBandPassFilterBankSynthesiser();
		double[] y = synthesiser.apply(analyser, subbands);

		outputFormat = new AudioFormat((int) (subbands[0].samplingRate), inputAudio.getFormat().getSampleSizeInBits(), inputAudio
				.getFormat().getChannels(), true, true);
		outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), outputFormat);
		outFileName = args[0].substring(0, args[0].length() - 4) + "_resynthesis" + ".wav";
		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
	}
}
