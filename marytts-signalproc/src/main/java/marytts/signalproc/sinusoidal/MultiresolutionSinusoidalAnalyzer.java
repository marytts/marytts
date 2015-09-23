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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.filter.ComplementaryFilterBankAnalyser;
import marytts.signalproc.filter.FIRBandPassFilterBankAnalyser;
import marytts.signalproc.filter.FIRWaveletFilterBankAnalyser;
import marytts.signalproc.filter.FilterBankAnalyserBase;
import marytts.signalproc.filter.Subband;
import marytts.signalproc.window.Window;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * A basic multi-resolution version of the sinusoidal analyzer. The idea is to adjust time-frequency resolution to resolve
 * sinusoids better using a wavelet transform like approach. For this purpose, the original signal is subband filtered and
 * sinusoidal parameters are extracted from the subbands using different window and skip sizes. This class has not been tested
 * sufficiently and the subband decomposition procedure does not seem to be appropriate for this kind of analysis.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class MultiresolutionSinusoidalAnalyzer extends BaseSinusoidalAnalyzer {
	public FilterBankAnalyserBase filterbankAnalyser;
	public int multiresolutionFilterbankType;
	public int numBands;
	public int samplingRate;

	public MultiresolutionSinusoidalAnalyzer(int multiresolutionFilterbankTypeIn, int numBandsIn, int samplingRateIn) {
		multiresolutionFilterbankType = multiresolutionFilterbankTypeIn;
		numBands = numBandsIn;
		samplingRate = samplingRateIn;

		filterbankAnalyser = null;

		if (multiresolutionFilterbankType == FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK) {
			double overlapAround1000Hz = 100.0;

			filterbankAnalyser = new FIRBandPassFilterBankAnalyser(numBands, samplingRate, overlapAround1000Hz);
		} else if (multiresolutionFilterbankType == FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK) {
			double overlapAround1000Hz = 100.0;

			filterbankAnalyser = new FIRWaveletFilterBankAnalyser(numBands, samplingRate);
		} else if (multiresolutionFilterbankType == FilterBankAnalyserBase.COMPLEMENTARY_FILTERBANK) {
			if (!MathUtils.isPowerOfTwo(numBands)) {
				int tmpNumBands = 2;
				while (tmpNumBands < numBands)
					tmpNumBands *= 2;
				numBands = tmpNumBands;
				System.out.println("Number of bands should be a power of two for the complementary filterbank");
			}

			int baseFilterOrder = SignalProcUtils.getFIRFilterOrder(samplingRate);
			int numLevels = numBands - 1;
			filterbankAnalyser = new ComplementaryFilterBankAnalyser(numLevels, baseFilterOrder);
		}
	}

	// Fixed rate version
	public SinusoidalTracks[] analyze(double[] x, double lowestBandWindowSizeInSeconds, int windowType,
			boolean bRefinePeakEstimatesParabola, boolean bRefinePeakEstimatesBias, boolean bSpectralReassignment,
			boolean bAdjustNeighFreqDependent, boolean bFreqLimitedAnalysis) {
		return analyze(x, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias,
				bSpectralReassignment, bAdjustNeighFreqDependent, bFreqLimitedAnalysis, false, null, 0.0f);
	}

	// Fixed rate and pitch synchronous version.
	// Set bPitchSynchronousAnalysis=false to get fixed rate version. In this case pitchMarks can be anything (i.e. null)
	// and numPeriods can be a dummy value since they are only used for ptich synchronous processing
	public SinusoidalTracks[] analyze(double[] x, double lowestBandWindowSizeInSeconds, int windowType,
			boolean bRefinePeakEstimatesParabola, boolean bRefinePeakEstimatesBias, boolean bSpectralReassignment,
			boolean bAdjustNeighFreqDependent, boolean bFreqLimitedAnalysis, boolean bPitchSynchronousAnalysis, PitchMarks pm, // Only
																																// used
																																// when
																																// bPitchSynchronousAnalysis=true
			float numPeriods) // Only used when bPitchSynchronousAnalysis=true
	{
		SinusoidalTracks[] subbandTracks = new SinusoidalTracks[numBands];
		Subband[] subbands = null;

		// When there is downsampling, no need for frequency limited analysis
		if (multiresolutionFilterbankType != FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK)
			bFreqLimitedAnalysis = false;

		if (filterbankAnalyser != null) {
			subbands = filterbankAnalyser.apply(x);
			SinusoidalAnalysisParams params = null;
			for (int i = 0; i < subbands.length; i++) {
				if (!bPitchSynchronousAnalysis || i > 0) // Pitch synchrounous subband analysis is only performed at the lowest
															// frequency subband
				{
					SinusoidalAnalyzer sa = null;
					if (bFreqLimitedAnalysis) {
						params = new SinusoidalAnalysisParams((int) (subbands[i].samplingRate), subbands[i].lowestFreqInHz,
								subbands[i].highestFreqInHz, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias,
								bSpectralReassignment, bAdjustNeighFreqDependent);

						sa = new SinusoidalAnalyzer(params);
					} else {
						params = new SinusoidalAnalysisParams((int) (subbands[i].samplingRate), 0.0,
								0.5 * subbands[i].samplingRate, windowType, bRefinePeakEstimatesParabola,
								bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);

						sa = new SinusoidalAnalyzer(params);
					}

					float winSizeInSeconds = (float) (lowestBandWindowSizeInSeconds / Math.pow(2.0, i));
					float skipSizeInSeconds = 0.5f * winSizeInSeconds;
					float deltaInHz = 50.0f; // Also make this frequency range dependent??

					if (multiresolutionFilterbankType == FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK)
						subbandTracks[i] = sa.analyzeFixedRate(subbands[i].waveform, winSizeInSeconds, skipSizeInSeconds,
								deltaInHz, SinusoidalAnalysisParams.LP_SPEC);
					else
						subbandTracks[i] = sa.analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz,
								SinusoidalAnalysisParams.LP_SPEC);

					// Normalize overlapping frequency region gains if an overlapping subband stucture is used
					if (multiresolutionFilterbankType == FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK)
						normalizeSinusoidalAmplitudes(subbandTracks[i], samplingRate,
								((FIRBandPassFilterBankAnalyser) filterbankAnalyser).normalizationFilterTransformedIR);
				} else {
					PitchSynchronousSinusoidalAnalyzer sa = null;
					if (bFreqLimitedAnalysis) {
						params = new SinusoidalAnalysisParams((int) (subbands[i].samplingRate), subbands[i].lowestFreqInHz,
								subbands[i].highestFreqInHz, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias,
								bSpectralReassignment, bAdjustNeighFreqDependent);

						sa = new PitchSynchronousSinusoidalAnalyzer(params);
					} else {
						params = new SinusoidalAnalysisParams((int) (subbands[i].samplingRate), 0.0,
								0.5 * subbands[i].samplingRate, windowType, bRefinePeakEstimatesParabola,
								bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);

						sa = new PitchSynchronousSinusoidalAnalyzer(params);
					}

					float winSizeInSeconds = (float) (lowestBandWindowSizeInSeconds / Math.pow(2.0, i)); // This is computed only
																											// for determining
																											// skip rate
					float skipSizeInSeconds = 0.5f * winSizeInSeconds;
					float deltaInHz = 50.0f; // Also make this frequency range dependent??
					float numPeriodsCurrent = (float) (numPeriods / Math.pow(2.0, i)); // This iteratively halves the effective
																						// window size for higher frequency
																						// subbands

					subbandTracks[i] = sa.analyzePitchSynchronous(x, pm, numPeriodsCurrent, skipSizeInSeconds, deltaInHz,
							SinusoidalAnalysisParams.LP_SPEC);
				}
			}
		}

		return subbandTracks;
	}

	// Normalizes sinusoidal amplitudes when an overlapping subband filterbank structure is used
	public void normalizeSinusoidalAmplitudes(SinusoidalTracks sinTracks, int samplingRateIn,
			double[] normalizationFilterTransformedIR) {
		int i, j, k;
		int maxFreq = normalizationFilterTransformedIR.length - 1;
		for (i = 0; i < sinTracks.tracks.length; i++) {
			for (j = 0; j < sinTracks.tracks[i].totalSins; j++) {
				k = SignalProcUtils.freq2index(SignalProcUtils.radian2hz(sinTracks.tracks[i].freqs[j], sinTracks.fs),
						samplingRateIn, maxFreq);
				sinTracks.tracks[i].amps[j] *= normalizationFilterTransformedIR[k];
			}
		}
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

		int numBands = 4;
		double lowestBandWindowSizeInSeconds = 0.020;
		double startFreqInHz = 0.0;
		double endFreqInHz = 0.5 * samplingRate;
		int windowType = Window.HAMMING;
		boolean bRefinePeakEstimatesParabola = true;
		boolean bRefinePeakEstimatesBias = true;
		boolean bSpectralReassignment = true;
		boolean bAdjustNeighFreqDependent = true;
		boolean bFreqLimitedAnalysis = false;
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
	}
}
