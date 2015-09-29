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
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Sinusoidal Modeling Synthesis Module Given tracks of sinusoids estimated during analysis and after possible modifications,
 * output speech is synthesized.
 * 
 * References: Quatieri, T. F. Discrete-Time Speech Signal Processing: Principles and Practice. Prentice-Hall Inc. 2001. (Chapter
 * 9 – Sinusoidal Analysis/Synthesis)
 * 
 * R.J. McAulay and T.F. Quatieri, "Speech Analysis/Synthesis Based on a Sinusoidal Representation," IEEE Transactions on
 * Acoustics, Speech and Signal Processing, vol. ASSP-34, no. 4, August 1986.
 * 
 * @author Oytun T&uuml;rk
 */
public class PeakMatchedSinusoidalSynthesizer extends BaseSinusoidalSynthesizer {

	public PeakMatchedSinusoidalSynthesizer(int samplingRate) {
		super(samplingRate);
	}

	public double[] synthesize(SinusoidalTracks st) {
		SinusoidalTracks[] sts = new SinusoidalTracks[1];
		sts[0] = st;

		return synthesize(sts, false);
	}

	public double[] synthesize(SinusoidalTracks[] sts) {
		return synthesize(sts, false);
	}

	public double[] synthesize(SinusoidalTracks[] sts, boolean isSilentSynthesis) {
		double[] y = null;
		double[] tmpy = null;
		for (int i = 0; i < sts.length; i++) {
			if (y == null)
				y = synthesize(sts[i], isSilentSynthesis);
			else {
				tmpy = synthesize(sts[i], isSilentSynthesis);

				if (tmpy.length > y.length) {
					double[] tmpy2 = new double[y.length];
					System.arraycopy(y, 0, tmpy2, 0, y.length);
					y = new double[tmpy.length];
					Arrays.fill(y, 0.0);
					System.arraycopy(tmpy2, 0, y, 0, tmpy2.length);

					for (int j = 0; j < tmpy.length; j++)
						y[i] += tmpy[i];
				}
			}
		}

		return y;
	}

	// st: Sinusoidal tracks
	// absMaxDesired: Desired absolute maximum of the output
	public double[] synthesize(SinusoidalTracks st, boolean isSilentSynthesis) {
		int n; // discrete time index
		int i, j;
		int nStart, nEnd, pStart, pEnd;
		float t; // continuous time
		float t2; // continuous time squared
		float t3; // continuous time cubed

		float tFinal = st.getOriginalDuration();
		int nFinal = (int) (Math.floor(tFinal * st.fs + 0.5));
		double[] y = new double[nFinal + 1];
		Arrays.fill(y, 0.0);
		float currentAmp;
		float currentTheta;
		double alpha, beta;
		int M;
		float T; // Number of samples between consecutive frames (equals to pitch period in pitch synchronous analysis/synthesis)
		float T2; // T squared
		float T3; // T cubed
		double oneOverTwoPi = 1.0 / MathUtils.TWOPI;
		double term1, term2;

		float currentTime; // For debugging purposes

		for (i = 0; i < st.totalTracks; i++) {
			for (j = 0; j < st.tracks[i].totalSins - 1; j++) {
				if (st.tracks[i].states[j] != SinusoidalTrack.TURNED_OFF) {
					pStart = (int) Math.floor(st.tracks[i].times[j] * st.fs + 0.5);
					pEnd = (int) Math.floor(st.tracks[i].times[j + 1] * st.fs + 0.5);

					nStart = Math.max(0, pStart);
					nEnd = Math.max(0, pEnd);
					nStart = Math.min(y.length - 1, nStart);
					nEnd = Math.min(y.length - 1, nEnd);

					// currentTime = 0.5f*(nEnd+nStart)/st.fs;
					// System.out.println("currentTime=" + String.valueOf(currentTime));

					for (n = nStart; n < nEnd; n++) {
						if (false) // Direct synthesis
						{
							currentAmp = st.tracks[i].amps[j];
							currentTheta = (n - nStart) * st.tracks[i].freqs[j] + st.tracks[i].phases[j];
							y[n] += currentAmp * Math.cos(currentTheta);
						} else // Synthesis with interpolation
						{
							// Amplitude interpolation
							currentAmp = st.tracks[i].amps[j] + (st.tracks[i].amps[j + 1] - st.tracks[i].amps[j])
									* ((float) n - pStart) / (pEnd - pStart + 1);

							T = (pEnd - pStart);

							if (n == nStart && st.tracks[i].states[j] == SinusoidalTrack.TURNED_ON) // Turning on a track
							{
								// Quatieri
								currentTheta = st.tracks[i].phases[j + 1] - T * st.tracks[i].freqs[j + 1];
								currentAmp = 0.0f;
							} else if (n == nStart && st.tracks[i].states[j] == SinusoidalTrack.TURNED_OFF && j > 0) // Turning
																														// off a
																														// track
							{
								// Quatieri
								currentTheta = st.tracks[i].phases[j - 1] + T * st.tracks[i].freqs[j - 1];
								currentAmp = 0.0f;
							} else // Cubic phase interpolation
							{
								// Quatieri
								M = (int) (Math
										.floor(oneOverTwoPi
												* ((st.tracks[i].phases[j] + T * st.tracks[i].freqs[j] - st.tracks[i].phases[j + 1]) + (st.tracks[i].freqs[j + 1] - st.tracks[i].freqs[j])
														* 0.5 * T) + 0.5));
								term1 = st.tracks[i].phases[j + 1] - st.tracks[i].phases[j] - T * st.tracks[i].freqs[j] + M
										* MathUtils.TWOPI;
								term2 = st.tracks[i].freqs[j + 1] - st.tracks[i].freqs[j];

								T2 = T * T;
								T3 = T * T2;
								alpha = 3.0 * term1 / T2 - term2 / T;
								beta = -2 * term1 / T3 + term2 / T2;

								t = ((float) n - nStart);
								t2 = t * t;
								t3 = t * t2;

								// Quatieri
								currentTheta = (float) (st.tracks[i].phases[j] + st.tracks[i].freqs[j] * t + alpha * t2 + beta
										* t3);
							}

							// Synthesis
							y[n] += currentAmp * Math.cos(currentTheta);
						}

						// System.out.println(String.valueOf(currentTheta));
					}
				}
			}

			if (!isSilentSynthesis)
				System.out.println("Synthesized track " + String.valueOf(i + 1) + " of " + String.valueOf(st.totalTracks));
		}

		y = MathUtils.multiply(y, st.absMaxOriginal / MathUtils.getAbsMax(y));

		return y;
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		// File input
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();
		double maxOrig = MathUtils.getAbsMax(x);

		SinusoidalAnalyzer sa = null;
		SinusoidalTracks st = null;
		PitchSynchronousSinusoidalAnalyzer pa = null;
		//

		// Analysis
		float deltaInHz = SinusoidalAnalysisParams.DEFAULT_DELTA_IN_HZ;
		float numPeriods = PitchSynchronousSinusoidalAnalyzer.DEFAULT_ANALYSIS_PERIODS;

		boolean isSilentSynthesis = false;

		int windowType = Window.HANNING;

		boolean bRefinePeakEstimatesParabola = false;
		boolean bRefinePeakEstimatesBias = false;
		boolean bSpectralReassignment = false;
		boolean bAdjustNeighFreqDependent = false;

		// int spectralEnvelopeType = SinusoidalAnalysisParams.LP_SPEC;
		int spectralEnvelopeType = SinusoidalAnalysisParams.SEEVOC_SPEC;
		float[] initialPeakLocationsInHz = null;
		initialPeakLocationsInHz = new float[1];
		for (int i = 0; i < 1; i++)
			initialPeakLocationsInHz[i] = (i + 1) * 350.0f;

		boolean isFixedRateAnalysis = false;
		boolean isRealSpeech = true;
		double startFreqInHz = 0.0;
		double endFreqInHz = 0.5 * samplingRate;

		SinusoidalAnalysisParams params = new SinusoidalAnalysisParams(samplingRate, startFreqInHz, endFreqInHz, windowType,
				bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);

		if (isFixedRateAnalysis) {
			// Fixed window size and skip rate analysis
			double[] f0s = null;
			float ws_f0 = -1.0f;
			float ss_f0 = -1.0f;
			sa = new SinusoidalAnalyzer(params);

			if (spectralEnvelopeType == SinusoidalAnalysisParams.SEEVOC_SPEC) // Pitch info needed
			{
				String strPitchFile = args[0].substring(0, args[0].length() - 4) + ".ptc";
				PitchReaderWriter f0 = new PitchReaderWriter(strPitchFile);
				f0s = f0.contour;
				ws_f0 = (float) f0.header.windowSizeInSeconds;
				ss_f0 = (float) f0.header.skipSizeInSeconds;
			}

			st = sa.analyzeFixedRate(x, 0.020f, 0.010f, deltaInHz, spectralEnvelopeType, f0s, ws_f0, ss_f0);
			//
		} else {
			// Pitch synchronous analysis
			String strPitchFile = args[0].substring(0, args[0].length() - 4) + ".ptc";
			PitchReaderWriter f0 = new PitchReaderWriter(strPitchFile);
			int pitchMarkOffset = 0;
			PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length,
					f0.header.windowSizeInSeconds, f0.header.skipSizeInSeconds, true, pitchMarkOffset);
			pa = new PitchSynchronousSinusoidalAnalyzer(params);

			st = pa.analyzePitchSynchronous(x, pm, numPeriods, -1.0f, deltaInHz, spectralEnvelopeType, initialPeakLocationsInHz);
			isSilentSynthesis = false;
		}
		//

		// Resynthesis
		PeakMatchedSinusoidalSynthesizer ss = new PeakMatchedSinusoidalSynthesizer(samplingRate);
		x = ss.synthesize(st, isSilentSynthesis);
		//

		// File output
		DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x), inputAudio.getFormat());
		String outFileName = args[0].substring(0, args[0].length() - 4) + "_sinResynthFullbandPitchSynch.wav";
		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		//
	}
}
