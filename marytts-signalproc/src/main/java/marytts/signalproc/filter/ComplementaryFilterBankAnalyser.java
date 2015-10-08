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
 * This class implements the complementary filter bank used in [Levine, et. al., 1999] for multiresolution sinusoidal modeling.
 * The filter bank consists of a collection of filter channels (See ComplementaryFilterChannel.java for details).
 * 
 * [Levine, et. al., 1999] Levine, S. N., Verma, T. S., and Smith III, J. O., "Multiresolution sinusoidal modeling for wideband
 * audio with modifications", in Proc. of the IEEE ICASSP 1998, Volume 6, Issue , 12-15 May 1998, pp. 3585-3588.
 * 
 * @author Oytun T&uuml;rk
 */
public class ComplementaryFilterBankAnalyser extends FilterBankAnalyserBase {
	public int numLevels;
	public int numBands; // We always have 2^numLevels subbands
	public int baseFilterOrder;

	protected ComplementaryFilterBankChannelAnalyser[] channelAnalysers;
	protected double originalEnergy;

	public ComplementaryFilterBankAnalyser(int numLevelsIn, int baseFilterOrderIn) {
		if (numLevelsIn >= 0) {
			numLevels = numLevelsIn;
			numBands = (int) Math.pow(2.0, numLevels);
			channelAnalysers = new ComplementaryFilterBankChannelAnalyser[numLevels];
			baseFilterOrder = baseFilterOrderIn;

			int N = baseFilterOrder;
			for (int i = 0; i < numLevels; i++) {
				channelAnalysers[i] = new ComplementaryFilterBankChannelAnalyser(N);
				N = (int) (N * 0.5);
			}
		}
	}

	public Subband[] apply(double[] x, int samplingRateInHz) {
		Subband[] subbands = null;
		int i;

		originalEnergy = SignalProcUtils.energy(x);

		// Multiresolution analysis
		channelAnalysers[0].apply(x);
		for (i = 1; i < numLevels; i++)
			channelAnalysers[i].apply(channelAnalysers[i - 1].lpfOut);
		//

		// Rearrange results from lower to higher frequencies
		subbands = new Subband[numLevels + 1];
		int currentSamplingRate = (int) (samplingRateInHz / Math.pow(2.0, numLevels));
		double startFreqInHz = 0.0;
		double endFreqInHz = 0.5 * currentSamplingRate;
		int subbandInd = 0;
		subbands[subbandInd++] = new Subband(channelAnalysers[numLevels - 1].lpfOut, currentSamplingRate, startFreqInHz,
				endFreqInHz);

		startFreqInHz = endFreqInHz;
		for (i = numLevels; i >= 1; i--) {
			currentSamplingRate = 2 * currentSamplingRate;
			endFreqInHz = 0.5 * currentSamplingRate;
			subbands[subbandInd++] = new Subband(channelAnalysers[i - 1].hpfOut, currentSamplingRate, startFreqInHz, endFreqInHz);
			startFreqInHz = endFreqInHz;
		}
		//

		return subbands;
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		int numLevels = 4;
		int N = 512;
		ComplementaryFilterBankAnalyser analyser = new ComplementaryFilterBankAnalyser(numLevels, N);
		Subband[] subbands = analyser.apply(x, samplingRate);

		DDSAudioInputStream outputAudio;
		AudioFormat outputFormat;
		String outFileName;

		// Write highpass components 0 to numLevels-1
		for (int i = 0; i < subbands.length; i++) {
			outputFormat = new AudioFormat((int) (subbands[i].samplingRate), inputAudio.getFormat().getSampleSizeInBits(),
					inputAudio.getFormat().getChannels(), true, true);
			outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(subbands[i].waveform), outputFormat);
			outFileName = args[0].substring(0, args[0].length() - 4) + "_sb" + String.valueOf(i + 1) + ".wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}
	}
}
