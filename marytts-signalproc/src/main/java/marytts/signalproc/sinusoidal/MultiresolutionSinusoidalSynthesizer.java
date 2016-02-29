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
package marytts.signalproc.sinusoidal;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.filter.FIRWaveletFilterBankAnalyser;
import marytts.signalproc.filter.FIRWaveletFilterBankSynthesiser;
import marytts.signalproc.filter.FilterBankAnalyserBase;
import marytts.signalproc.filter.Subband;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * A basic multi-resolution version of the sinusoidal synthesizer. This class has not been tested sufficiently and the subband
 * reconstruction procedure does not seem to be appropriate for this kind of synthesis.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class MultiresolutionSinusoidalSynthesizer {
	public MultiresolutionSinusoidalAnalyzer analyser;

	public MultiresolutionSinusoidalSynthesizer(MultiresolutionSinusoidalAnalyzer analyserIn) {
		analyser = analyserIn;
	}

	public double[] synthesize(SinusoidalTracks[] subbandTracks, boolean isSilentSynthesis) {
		double[] y = null;
		double[] tmpy = null;
		if (subbandTracks != null) {
			int i, j;

			// Sinusoidal resynthesis
			if (analyser.multiresolutionFilterbankType == FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK) {
				Subband[] subbands = new Subband[subbandTracks.length];
				for (i = 0; i < subbandTracks.length; i++) {
					PeakMatchedSinusoidalSynthesizer ss = new PeakMatchedSinusoidalSynthesizer(subbandTracks[i].fs);
					tmpy = ss.synthesize(subbandTracks[i], isSilentSynthesis);
					subbands[i] = new Subband(tmpy, subbandTracks[i].fs);
				}

				FIRWaveletFilterBankSynthesiser filterbankSynthesiser = new FIRWaveletFilterBankSynthesiser();
				y = filterbankSynthesiser.apply((FIRWaveletFilterBankAnalyser) (analyser.filterbankAnalyser), subbands, false);
			} else {
				for (i = 0; i < subbandTracks.length; i++) {
					PeakMatchedSinusoidalSynthesizer ss = new PeakMatchedSinusoidalSynthesizer(subbandTracks[i].fs);
					tmpy = ss.synthesize(subbandTracks[i], isSilentSynthesis);

					if (i == 0) {
						y = new double[tmpy.length];
						System.arraycopy(tmpy, 0, y, 0, tmpy.length);
					} else {
						for (j = 0; j < Math.min(y.length, tmpy.length); j++)
							y[j] += tmpy[j];
					}
				}
			}
		}

		return y;
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		int multiresolutionFilterbankType;
		// multiresolutionFilterbankType = FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK;
		multiresolutionFilterbankType = FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK;
		// multiresolutionFilterbankType = FilterBankAnalyserBase.COMPLEMENTARY_FILTERBANK;
		int numBands = 2;
		double lowestBandWindowSizeInSeconds = 0.020;
		double startFreqInHz = 0.0;
		double endFreqInHz = 0.5 * samplingRate;
		int windowType = Window.HANNING;
		boolean bRefinePeakEstimatesParabola = false;
		boolean bRefinePeakEstimatesBias = false;
		boolean bSpectralReassignment = false;
		boolean bAdjustNeighFreqDependent = false;
		boolean isSilentSynthesis = false;
		boolean bFreqLimitedAnalysis = false; // Only used for FIR_BANDPASS_FILTERBANK
		boolean bPitchSynchronous = false;
		float numPeriods = 2.5f;

		SinusoidalAnalysisParams params = new SinusoidalAnalysisParams(samplingRate, startFreqInHz, endFreqInHz, windowType,
				bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);

		MultiresolutionSinusoidalAnalyzer msa = new MultiresolutionSinusoidalAnalyzer(multiresolutionFilterbankType, numBands,
				samplingRate);

		SinusoidalTracks[] subbandTracks = null;

		if (!bPitchSynchronous)
			subbandTracks = msa.analyze(x, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola,
					bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, bFreqLimitedAnalysis);
		else {
			String strPitchFile = args[0].substring(0, args[0].length() - 4) + ".ptc";
			PitchReaderWriter f0 = new PitchReaderWriter(strPitchFile);
			int pitchMarkOffset = 0;
			PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length,
					f0.header.windowSizeInSeconds, f0.header.skipSizeInSeconds, true, pitchMarkOffset);
			PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(params);

			subbandTracks = msa.analyze(x, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola,
					bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, bFreqLimitedAnalysis, true, pm,
					numPeriods);
		}

		// Resynthesis
		MultiresolutionSinusoidalSynthesizer mss = new MultiresolutionSinusoidalSynthesizer(msa);
		x = mss.synthesize(subbandTracks, isSilentSynthesis);
		//

		// This scaling is only for comparison among different parameter sets, different synthesizer outputs etc
		double maxx = MathUtils.getAbsMax(x);
		for (int i = 0; i < x.length; i++)
			x[i] = x[i] / maxx * 0.9;
		//

		// File output
		DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x), inputAudio.getFormat());
		String outFileName = args[0].substring(0, args[0].length() - 4) + "_multiResWaveletFixedRate.wav";
		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		//
	}
}
