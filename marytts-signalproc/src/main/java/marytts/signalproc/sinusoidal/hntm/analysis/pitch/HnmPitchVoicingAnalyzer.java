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
package marytts.signalproc.sinusoidal.hntm.analysis.pitch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.window.Window;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.display.DisplayUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Initial pitch, voicing, maximum frequency of voicing, and refined pitch estimation as described in:
 * 
 * Y. Stylianou (1996) “A pitch and maximum voiced frequency estimation technique adapted to harmonic models of speech,” IEEE
 * Nordic Signal Processing Symposium, Helsinki, Finland, Sept. 1996.
 * 
 * @author Oytun T&uuml;rk
 */
public class HnmPitchVoicingAnalyzer {

	public HnmPitchVoicingAnalyzer() {

	}

	public static int getDefaultFFTSize(int samplingRate) {
		if (samplingRate < 10000)
			return 2048;
		else if (samplingRate < 20000)
			return 4096;
		else
			return 8192;
	}

	public static float[] estimateInitialPitch(double[] x, int samplingRate, float f0MinInHz, float f0MaxInHz, int windowType,
			HnmPitchVoicingAnalyzerParams params) {
		int PMax = (int) Math.floor(samplingRate / f0MinInHz + 0.5);
		int PMin = (int) Math.floor(samplingRate / f0MaxInHz + 0.5);

		int ws = SignalProcUtils.time2sample(params.mvfAnalysisWindowSizeInSeconds, samplingRate);
		ws = Math.max(ws, (int) Math.floor(params.numPeriodsInitialPitchEstimation * PMin + 0.5));

		int ss = (int) Math.floor(params.mvfAnalysisSkipSizeInSeconds * samplingRate + 0.5);
		int numfrm = (int) Math.floor(((double) x.length - ws) / ss + 0.5);

		int numCandidates = PMax - PMin + 1;

		double[] E = new double[numCandidates];

		int P;
		int i, t, l, k;
		double term1, term2, term3, r;

		double[] frm = new double[ws];
		Window win = Window.get(windowType, ws);
		double[] wgt2 = win.getCoeffs();
		wgt2 = MathUtils.normalizeToSumUpTo(wgt2, 1.0);

		for (t = 0; t < ws; t++)
			wgt2[t] = wgt2[t] * wgt2[t];

		double tmpSum = 0.0;
		for (t = 0; t < ws; t++)
			tmpSum += wgt2[t];

		for (t = 0; t < ws; t++)
			wgt2[t] = wgt2[t] / tmpSum;

		double[] wgt4 = new double[ws];
		System.arraycopy(wgt2, 0, wgt4, 0, ws);
		for (t = 0; t < ws; t++)
			wgt4[t] = wgt4[t] * wgt4[t];

		double termTmp = 0.0;
		for (t = 0; t < ws; t++)
			termTmp += wgt4[t];

		float[] initialF0s = new float[numfrm];
		int minInd;
		int startIndex, endIndex;
		for (i = 0; i < numfrm; i++) {
			Arrays.fill(frm, 0.0);
			System.arraycopy(x, i * ss, frm, 0, Math.min(ws, x.length - i * ss));
			startIndex = i * ss;
			endIndex = startIndex + Math.min(ws, x.length - i * ss) - 1;

			// MaryUtils.plot(frm);

			term1 = 0.0;
			for (t = 0; t < ws; t++)
				term1 += frm[t] * frm[t] * wgt2[t];

			float lLim;
			for (P = PMin; P <= PMax; P++) {
				lLim = ((float) ws) / P;
				term2 = 0.0;
				for (l = (int) (Math.floor(-1.0f * lLim) + 1); l < lLim; l++) {
					r = 0.0;
					for (t = 0; t < ws; t++) {
						if (t + l * P >= 0 && t + l * P < ws)
							r += frm[t] * wgt2[t] * frm[t + l * P] * wgt2[t + l * P];
					}

					term2 += r;
				}
				term2 *= P;

				term3 = 1.0 - P * termTmp;

				E[P - PMin] = (term1 - term2) / (term1 * term3);
			}

			// MaryUtils.plot(E);

			minInd = MathUtils.getMinIndex(E);
			if (E[minInd] < 0.5)
				initialF0s[i] = 1.0f / SignalProcUtils.sample2time(minInd + PMin, samplingRate);
			else
				initialF0s[i] = 0.0f;
		}

		return initialF0s;
	}

	public static float[] analyzeVoicings(double[] x, int samplingRate, float[] initialF0s, HnmPitchVoicingAnalyzerParams params,
			boolean isSilentAnalysis) {
		// First remove dc-bias and normalize signal energy
		// MaryUtils.plot(x);
		double[] xNorm = MathUtils.add(x, -1.0 * MathUtils.mean(x));
		double xEn = SignalProcUtils.energy(x);
		xNorm = MathUtils.multiply(x, 500.0 / Math.sqrt(xEn / x.length));
		// MaryUtils.plot(xNorm);
		//

		double durationInSeconds = SignalProcUtils.sample2time(x.length, samplingRate);
		int numfrm = (int) Math.floor((durationInSeconds - 0.5 * params.mvfAnalysisWindowSizeInSeconds)
				/ params.mvfAnalysisSkipSizeInSeconds + 0.5);

		float[] mappedF0s = new float[numfrm];
		double[] voicingErrors = new double[numfrm];

		int ws = SignalProcUtils.time2sample(params.mvfAnalysisWindowSizeInSeconds, samplingRate);
		int ss = (int) Math.floor(params.mvfAnalysisSkipSizeInSeconds * samplingRate + 0.5);

		int startIndex, endIndex;
		boolean isVoiced;
		float[] maxFrequencyOfVoicings = new float[numfrm];
		float currentTime;
		double[] frm = new double[ws];
		Window w = Window.get(Window.HANNING, ws);
		int currentF0Index;
		int i;

		while (params.fftSize < ws)
			params.fftSize *= 2;

		int maxFreq = (int) (Math.floor(0.5 * params.fftSize + 0.5) + 1);
		ComplexArray Y = new ComplexArray(params.fftSize);
		double[][] allDBSpectra = new double[numfrm][maxFreq];
		float prevMaxFreqVoicing, prevPrevMaxFreqVoicing;

		for (i = 0; i < numfrm; i++) {
			Arrays.fill(frm, 0.0);
			// System.arraycopy(xNorm, i*ss, frm, 0, Math.min(ws, x.length-i*ss));
			if (i * ss + ws < x.length)
				System.arraycopy(xNorm, i * ss, frm, 0, ws);
			else
				System.arraycopy(xNorm, x.length - ws, frm, 0, ws); // Here is a trick to avoid zeros in the last frame

			frm = w.apply(frm, 0);
			startIndex = i * ss;
			endIndex = startIndex + Math.min(ws, x.length - i * ss) - 1;

			currentTime = i * params.mvfAnalysisSkipSizeInSeconds + 0.5f * params.mvfAnalysisWindowSizeInSeconds;
			currentF0Index = SignalProcUtils.time2frameIndex(currentTime, params.f0AnalysisWindowSizeInSeconds,
					params.f0AnalysisSkipSizeInSeconds);
			currentF0Index = MathUtils.CheckLimits(currentF0Index, 0, initialF0s.length - 1);
			mappedF0s[i] = initialF0s[currentF0Index];

			Arrays.fill(Y.real, 0.0);
			Arrays.fill(Y.imag, 0.0);
			System.arraycopy(frm, 0, Y.real, 0, frm.length);
			//

			// Compute DFT
			if (MathUtils.isPowerOfTwo(params.fftSize))
				FFT.transform(Y.real, Y.imag, false);
			else
				Y = FFTMixedRadix.fftComplex(Y);
			//

			// Compute magnitude spectrum in dB as peak frequency estimates tend to be more accurate
			double[] YAbs = MathUtils.abs(Y, 0, maxFreq - 1);
			double[] YAbsDB = MathUtils.amp2db(YAbs);
			allDBSpectra[i] = ArrayUtils.copy(YAbsDB);
			//

			// MaryUtils.plot(YAbsDB);

			isVoiced = (mappedF0s[i] > 10.0f) ? true : false;

			/*
			 * isVoiced = false; if (mappedF0s[i]>10.0f) { voicingErrors[i] = estimateVoicingFromFrameSpectrum(YAbs, samplingRate,
			 * mappedF0s[i], params.vuvSearchMinHarmonicMultiplier, params.vuvSearchMaxHarmonicMultiplier);
			 * 
			 * if (voicingErrors[i]<-1.0) isVoiced = true; }
			 */

			VoicingAnalysisOutputData vo = null;
			if (isVoiced) {
				prevMaxFreqVoicing = 0.0f;
				prevPrevMaxFreqVoicing = 0.0f;

				if (i >= 1)
					prevMaxFreqVoicing = maxFrequencyOfVoicings[i - 1];

				if (i >= 2)
					prevPrevMaxFreqVoicing = maxFrequencyOfVoicings[i - 2];

				vo = estimateMaxFrequencyOfVoicingsFrame(YAbsDB, samplingRate, mappedF0s[i], isVoiced, prevMaxFreqVoicing,
						prevPrevMaxFreqVoicing, params, isSilentAnalysis);
				maxFrequencyOfVoicings[i] = vo.maxFreqOfVoicing;
			} else
				maxFrequencyOfVoicings[i] = 0.0f;

			if (!isSilentAnalysis) {
				if (isVoiced)
					System.out.println("Time=" + String.valueOf(currentTime) + " sec." + " f0=" + String.valueOf(mappedF0s[i])
							+ " Hz." + " Voiced" + " MaxVFreq=" + String.valueOf(maxFrequencyOfVoicings[i]));
				else
					System.out.println("Time=" + String.valueOf(currentTime) + " sec." + " f0=" + String.valueOf(mappedF0s[i])
							+ " Hz." + " Unvoiced" + " MaxVFreq=" + String.valueOf(maxFrequencyOfVoicings[i]));
			}
		}

		// MaryUtils.plot(maxFrequencyOfVoicings);

		maxFrequencyOfVoicings = smoothUsingPeaks(maxFrequencyOfVoicings);
		// MaryUtils.plot(maxFrequencyOfVoicings);

		maxFrequencyOfVoicings = smoothUsingFilters(maxFrequencyOfVoicings, params);
		// MaryUtils.plot(maxFrequencyOfVoicings);

		maxFrequencyOfVoicings = applyConstraints(maxFrequencyOfVoicings, mappedF0s, samplingRate, params);
		// MaryUtils.plot(maxFrequencyOfVoicings);

		// Arrays.fill(maxFrequencyOfVoicings, samplingRate*0.5f);

		// MaryUtils.plot(voicingErrors);
		// MaryUtils.plot(maxFrequencyOfVoicings);

		return maxFrequencyOfVoicings;
	}

	public static float[] smoothUsingFilters(float[] maxFrequencyOfVoicings, HnmPitchVoicingAnalyzerParams params) {
		for (int i = 0; i < params.numFilteringStages; i++) {
			// Smooth with a median filter
			if (params.medianFilterLength > 1) {
				maxFrequencyOfVoicings = SignalProcUtils.medianFilter(maxFrequencyOfVoicings, params.medianFilterLength);
				// maxFrequencyOfVoicings = SignalProcUtils.shift(maxFrequencyOfVoicings,
				// -1*(int)Math.floor(0.5*params.medianFilterLength+0.5));
			}

			// Smooth with moving average filters
			if (params.movingAverageFilterLength > 1) {
				maxFrequencyOfVoicings = SignalProcUtils.meanFilter(maxFrequencyOfVoicings, params.movingAverageFilterLength);
				// maxFrequencyOfVoicings = SignalProcUtils.shift(maxFrequencyOfVoicings,
				// -1*(int)Math.floor(0.5*params.movingAverageFilterLength+0.5));
			}
		}

		return maxFrequencyOfVoicings;
		//
	}

	public static float[] smoothUsingPeaks(float[] maxFrequencyOfVoicings) {
		int i, j;
		// Peak picking based smoothing here
		// Find segments
		int numSegments = 0;
		boolean bSegmentStarted = false;
		int startInd = -1;
		int endInd = -1;
		if (maxFrequencyOfVoicings[0] > 10.0 && maxFrequencyOfVoicings[1] > 10.0) {
			startInd = 0;
			bSegmentStarted = true;
		}
		for (i = 2; i < maxFrequencyOfVoicings.length; i++) {
			if (!bSegmentStarted) {
				if (maxFrequencyOfVoicings[i - 2] < 10.0 && maxFrequencyOfVoicings[i - 1] < 10.0
						&& maxFrequencyOfVoicings[i] > 10.0) {
					bSegmentStarted = true;
					startInd = i;
				}
			} else {
				if (maxFrequencyOfVoicings[i] < 10.0) {
					bSegmentStarted = false;
					endInd = i;
					if (endInd - startInd > 5)
						numSegments++;

					startInd = -1;
					endInd = -1;
				}
			}
		}

		if (numSegments > 0) {
			float[] tmpMaxFrequencyOfVoicings = ArrayUtils.copy(maxFrequencyOfVoicings);
			Arrays.fill(maxFrequencyOfVoicings, 0.0f);
			int[][] segmentInds = new int[numSegments][2];

			int currentSegment = 0;
			bSegmentStarted = false;
			startInd = -1;
			endInd = -1;
			if (tmpMaxFrequencyOfVoicings[0] > 10.0 && tmpMaxFrequencyOfVoicings[1] > 10.0) {
				startInd = 0;
				bSegmentStarted = true;
			}
			for (i = 2; i < tmpMaxFrequencyOfVoicings.length; i++) {
				if (!bSegmentStarted) {
					if (tmpMaxFrequencyOfVoicings[i - 2] < 10.0 && tmpMaxFrequencyOfVoicings[i - 1] < 10.0
							&& tmpMaxFrequencyOfVoicings[i] > 10.0) {
						bSegmentStarted = true;
						startInd = i;
					}
				} else {
					if (tmpMaxFrequencyOfVoicings[i] < 10.0) {
						bSegmentStarted = false;
						endInd = i;

						if (endInd - startInd > 5) {
							segmentInds[currentSegment][0] = startInd;
							segmentInds[currentSegment][1] = endInd;
						}

						currentSegment++;
						startInd = -1;
						endInd = -1;

						if (currentSegment > numSegments - 1)
							break;
					}
				}
			}

			// Find peaks in each segment
			for (i = 0; i < numSegments; i++) {
				double[] tmpSegment = new double[segmentInds[i][1] - segmentInds[i][0] + 1];
				for (j = segmentInds[i][0]; j <= segmentInds[i][1]; j++)
					tmpSegment[j - segmentInds[i][0]] = tmpMaxFrequencyOfVoicings[j];

				int[] tmpPeakInds = MathUtils.getExtrema(tmpSegment, 3, 3, true);
				int[] peakInds = null;
				int peakIndsLen = 0;
				if (tmpPeakInds != null) {
					peakInds = new int[tmpPeakInds.length + 2];
					peakInds[0] = 0;
					for (j = 0; j < tmpPeakInds.length; j++)
						peakInds[j + 1] = tmpPeakInds[j];
					peakInds[peakInds.length - 1] = segmentInds[i][1] - segmentInds[i][0];
				}

				// Interpolate peaks within each segment and assign final values to maxFrequencyOfVoicings
				if (peakInds != null) {
					for (j = 0; j < peakInds.length; j++)
						peakInds[j] += segmentInds[i][0];

					double[] peakVals = new double[peakInds.length];
					for (j = 0; j < peakInds.length; j++)
						peakVals[j] = tmpMaxFrequencyOfVoicings[peakInds[j]];

					int[] inds = new int[segmentInds[i][1] - segmentInds[i][0] + 1];
					for (j = segmentInds[i][0]; j <= segmentInds[i][1]; j++)
						inds[j - segmentInds[i][0]] = j;

					tmpSegment = MathUtils.interpolate_linear(peakInds, peakVals, inds);

					for (j = segmentInds[i][0]; j <= segmentInds[i][1]; j++)
						maxFrequencyOfVoicings[j] = (float) tmpSegment[j - segmentInds[i][0]];
				} else {
					for (j = segmentInds[i][0]; j <= segmentInds[i][1]; j++)
						maxFrequencyOfVoicings[j] = tmpMaxFrequencyOfVoicings[j];
				}
			}
		}
		//

		return maxFrequencyOfVoicings;
	}

	public static float[] applyConstraints(float[] maxFrequencyOfVoicings, float[] mappedF0s, int samplingRate,
			HnmPitchVoicingAnalyzerParams params) {
		int i;

		// MaryUtils.plot(maxFrequencyOfVoicings);

		for (i = 0; i < maxFrequencyOfVoicings.length; i++) {
			if (mappedF0s[i] < 10.0f)
				maxFrequencyOfVoicings[i] = 0.0f;
		}

		// MaryUtils.plot(maxFrequencyOfVoicings);

		for (i = 0; i < maxFrequencyOfVoicings.length; i++) {
			if (mappedF0s[i] < 10.0f)
				maxFrequencyOfVoicings[i] = 0.0f;
			else {
				maxFrequencyOfVoicings[i] = MathUtils.CheckLimits(maxFrequencyOfVoicings[i], params.minimumTotalHarmonics
						* mappedF0s[i] - 0.3f * mappedF0s[i], params.maximumTotalHarmonics * mappedF0s[i] + 0.3f * mappedF0s[i]);
				maxFrequencyOfVoicings[i] = MathUtils.CheckLimits(maxFrequencyOfVoicings[i],
						params.minimumVoicedFrequencyOfVoicing, params.maximumVoicedFrequencyOfVoicing);
			}
		}
		// MaryUtils.plot(maxFrequencyOfVoicings);

		maxFrequencyOfVoicings = MathUtils.add(maxFrequencyOfVoicings, params.maximumFrequencyOfVoicingFinalShift);

		// MaryUtils.plot(maxFrequencyOfVoicings);

		// Final check for voicings and number of harmonics constraints for voiced portions
		for (i = 0; i < maxFrequencyOfVoicings.length; i++) {
			if (mappedF0s[i] < 10.0f)
				maxFrequencyOfVoicings[i] = 0.0f;
			else
				maxFrequencyOfVoicings[i] = MathUtils.CheckLimits(maxFrequencyOfVoicings[i], 0.0f, 0.5f * samplingRate);
		}

		// MaryUtils.plot(maxFrequencyOfVoicings);

		for (i = 0; i < maxFrequencyOfVoicings.length; i++) {
			if (mappedF0s[i] < 10.0f)
				maxFrequencyOfVoicings[i] = 0.0f;
		}

		// MaryUtils.plot(maxFrequencyOfVoicings);

		return maxFrequencyOfVoicings;
	}

	public static VoicingAnalysisOutputData estimateMaxFrequencyOfVoicingsFrame(double[] absDBSpec, int samplingRate, float f0,
			boolean isVoiced, HnmPitchVoicingAnalyzerParams params) {
		return estimateMaxFrequencyOfVoicingsFrame(absDBSpec, samplingRate, f0, isVoiced, -1.0f, -1.0f, params, false);
	}

	public static VoicingAnalysisOutputData estimateMaxFrequencyOfVoicingsFrame(double[] absDBSpec, int samplingRate, float f0,
			boolean isVoiced, float prevMaxFreqVoicing, float prevPrevMaxFreqVoicing, HnmPitchVoicingAnalyzerParams params,
			boolean isSilentAnalysis) {
		// double[] ampSpec = MathUtils.db2amp(absDBSpec);
		VoicingAnalysisOutputData output = new VoicingAnalysisOutputData();
		int i, n;
		output.maxFreqOfVoicing = 0.0f; // Means the spectrum is completely unvoiced
		if (!isVoiced)
			f0 = 100.0f;

		int maxFreqIndex = absDBSpec.length - 1;
		int numHarmonics = (int) Math.floor((0.5 * samplingRate - 1.5 * f0) / f0 + 0.5); // Leave some space at the end of
																							// spectrum, i.e. 1.5*f0 to avoid very
																							// narrow bands there
		int[] bandInds = new int[numHarmonics + 1]; // 0.5f0 1.5f0 2.5f0 ... (numHarmonics-0.5)f0 (numHarmonics+0.5)f0
		for (i = 0; i < bandInds.length; i++)
			bandInds[i] = SignalProcUtils.freq2index((i + 0.5) * f0, samplingRate, maxFreqIndex);

		double[] voiceds = new double[numHarmonics];
		double[] vals1 = new double[numHarmonics];
		double[] vals2 = new double[numHarmonics];
		double[] vals3 = new double[numHarmonics];
		Arrays.fill(vals1, Double.NEGATIVE_INFINITY);
		Arrays.fill(vals2, Double.NEGATIVE_INFINITY);
		Arrays.fill(vals3, Double.NEGATIVE_INFINITY);

		Arrays.fill(voiceds, 0.0);
		int[] valleyInds = MathUtils.getExtrema(absDBSpec, 2, 2, false);
		int[] tmpPeakIndices = new int[numHarmonics];

		double bandExtremaVal;
		int fcIndex;
		for (i = 0; i < numHarmonics; i++) {
			// Get local peak
			// int[] fcIndices = MathUtils.getExtrema(absDBSpec, 2, 2, true, bandInds[i], bandInds[i+1]);
			int neigh = (int) (Math.floor(params.neighsPercent / 100.0 * 0.5 * (bandInds[i + 1] - bandInds[i]) + 0.5));
			if (neigh < 1)
				neigh = 1;
			int[] fcIndices = MathUtils.getExtrema(absDBSpec, 1, 1, true, bandInds[i], bandInds[i + 1]);
			if (fcIndices != null) {
				fcIndex = fcIndices[0];
				bandExtremaVal = absDBSpec[fcIndex];
				for (n = 1; n < fcIndices.length; n++) {
					if (absDBSpec[fcIndices[n]] > bandExtremaVal) {
						fcIndex = fcIndices[n];
						bandExtremaVal = absDBSpec[fcIndex];
					}
				}
			} else {
				fcIndex = MathUtils.getAbsMaxInd(absDBSpec, bandInds[i], bandInds[i + 1]);
				if (fcIndex == -1)
					fcIndex = (int) Math.floor(0.5 * (bandInds[i] + bandInds[i + 1]) + 0.5);
			}

			double fc = SignalProcUtils.index2freq(fcIndex, samplingRate, maxFreqIndex);

			// From db spec
			double Am = absDBSpec[fcIndex];
			double Amc = computeAmc(absDBSpec, fcIndex, valleyInds);
			//

			// From linear spec
			// double Am = ampSpec[fcIndex];
			// double Amc = computeAmc(ampSpec, fcIndex, valleyInds);

			// Search for other peaks and compute Ams and Amcs
			int startInd = SignalProcUtils.freq2index(fc - 0.5 * f0, samplingRate, maxFreqIndex);
			int endInd = SignalProcUtils.freq2index(fc + 0.5 * f0, samplingRate, maxFreqIndex);
			int[] peakInds = MathUtils.getExtrema(absDBSpec, 1, 1, true, startInd, endInd);
			// MaryUtils.plot(absDBSpec, startInd, endInd);
			double[] Ams = null;
			double[] Amcs = null;
			voiceds[i] = 0.0;
			double bandMeanVal;
			double bandMinVal;
			if (peakInds != null) {
				if (peakInds.length > 1) {
					int total = 0;
					for (n = 0; n < peakInds.length; n++) {
						if (peakInds[n] != fcIndex)
							total++;
					}

					Ams = new double[total];
					Amcs = new double[total];
					int counter = 0;
					for (n = 0; n < peakInds.length; n++) {
						if (peakInds[n] != fcIndex) {
							// From db spec
							Ams[counter] = absDBSpec[peakInds[n]];
							Amcs[counter] = computeAmc(absDBSpec, peakInds[n], valleyInds);
							//

							// From linear spec
							// Ams[counter] = ampSpec[peakInds[n]];
							// Amcs[counter] = computeAmc(ampSpec, peakInds[n], valleyInds);
							//

							counter++;
						}
					}
				} else // A very sharp single peak might exist in this range, check by comparing with range mean
				{
					// MaryUtils.plot(absDBSpec, startInd, endInd);
					// bandMeanVal = MathUtils.mean(absDBSpec, startInd, endInd);
					bandMinVal = absDBSpec[MathUtils.getMinIndex(absDBSpec, startInd, endInd)];
					if (Am - bandMinVal > params.sharpPeakAmpDiffInDB)
						voiceds[i] = 1.0;
				}
			}
			//

			// Now do harmonic tests
			if (voiceds[i] != 1.0) {
				if (Amcs != null) {
					double meanAmcs = MathUtils.mean(Amcs);

					vals1[i] = Amc / meanAmcs;
					vals3[i] = (Math.abs(fc - (double) (i + 1) * f0) / ((double) (i + 1) * f0));

					if (vals1[i] > params.cumulativeAmpThreshold && vals3[i] < (params.harmonicDeviationPercent / 100.0))
						voiceds[i] = 1.0;
				}
				/*
				 * else { vals3[i] = (Math.abs(fc-(double)(i+1)*f0)/((double)(i+1)*f0));
				 * 
				 * if (vals3[i]<(HntmAnalyzer.HARMONIC_DEVIATION_PERCENT/100.0)) voiceds[i]=1.0; }
				 */
			}

			if (voiceds[i] != 1.0)
			// if (voiceds[i]==1.0)
			{
				if (Ams != null) {
					double maxAms = MathUtils.max(Ams);

					vals2[i] = Am - maxAms;
					// val2 = (MathUtils.amp2db(Am)-MathUtils.amp2db(maxAms)); //in amp should be converted to db
					vals3[i] = (Math.abs(fc - (double) (i + 1) * f0) / ((double) (i + 1) * f0));

					if (vals2[i] > params.maximumAmpThresholdInDB && vals3[i] < (params.harmonicDeviationPercent / 100.0))
						voiceds[i] = 1.0;
				}
				/*
				 * else { vals3[i] = (Math.abs(fc-(double)(i+1)*f0)/((double)(i+1)*f0));
				 * 
				 * if (vals3[i]<(HntmAnalyzer.HARMONIC_DEVIATION_PERCENT/100.0)) voiceds[i]=1.0; }
				 */
			}
			//

			// Save for sending peak indices to output
			tmpPeakIndices[i] = fcIndex;
		}

		/*
		 * //Median filter voicing cut-off int numPoints = 3; voiceds = SignalProcUtils.medianFilter(voiceds, numPoints);
		 * //Forward look int maxVoicedHarmonicBand = -1; for (i=0; i<voiceds.length-2; i++) { if (voiceds[i]==1.0 &&
		 * voiceds[i+1]==0.0 && voiceds[i+2]==0.0) { maxVoicedHarmonicBand=i; break; } }
		 */

		// Running average computer for voicing cut-off
		double[] runningMeans = new double[voiceds.length];
		for (i = 0; i < voiceds.length; i++)
			runningMeans[i] = MathUtils.mean(voiceds, 0, i);

		int numPoints = 3;
		runningMeans = SignalProcUtils.meanFilter(runningMeans, numPoints);
		int maxVoicedHarmonicBand = -1;
		for (i = 0; i < runningMeans.length - 2; i++) {
			if (runningMeans[i] < params.runningMeanVoicingThreshold && runningMeans[i + 1] < params.runningMeanVoicingThreshold
					&& runningMeans[i + 1] < params.runningMeanVoicingThreshold) {
				break;
			} else
				maxVoicedHarmonicBand = i + 2;
		}
		//

		// Get the max freq. of voicing considering the shift by filtering
		if (maxVoicedHarmonicBand > -1)
			output.maxFreqOfVoicing = (float) Math.min((maxVoicedHarmonicBand + 0.5) * f0 + 0.5 * numPoints * f0,
					0.5 * samplingRate);
		else
			output.maxFreqOfVoicing = 0.0f;

		output.maxFreqOfVoicing = MathUtils.CheckLimits(output.maxFreqOfVoicing, 0.0f, 0.5f * samplingRate);

		// Put harmonic peak indices into output as well
		if (output.maxFreqOfVoicing > 0.0f) {
			int count = (int) Math.floor(output.maxFreqOfVoicing / f0 + 0.5);
			count = MathUtils.CheckLimits(count, 0, numHarmonics);
			if (count > 0) {
				output.peakIndices = new int[count];
				System.arraycopy(tmpPeakIndices, 0, output.peakIndices, 0, count);
			}
		}
		//

		// String strDebug = "";
		// for (i=0; i<voiceds.length; i++)
		// System.out.println(String.valueOf(voiceds[i]) + " " + String.valueOf(vals1[i]) + " " + String.valueOf(vals2[i]) + " " +
		// String.valueOf(vals3[i]) + " ");

		if (!isSilentAnalysis)
			System.out.println("Max freq of voicing=" + String.valueOf(output.maxFreqOfVoicing));

		// MaryUtils.plot(absDBSpec);
		// MaryUtils.plot(runningMeans);

		if (prevMaxFreqVoicing > -1.0f) {
			if (prevPrevMaxFreqVoicing > -1.0f)
				output.maxFreqOfVoicing = 0.45f * output.maxFreqOfVoicing + 0.35f * prevMaxFreqVoicing + 0.2f
						* prevPrevMaxFreqVoicing;
			else
				output.maxFreqOfVoicing = 0.60f * output.maxFreqOfVoicing + 0.40f * prevMaxFreqVoicing;
		}

		return output;
	}

	private static double computeAmc(double[] spec, int fcIndex, int[] valleyInds) {
		double Amc = spec[fcIndex];

		if (valleyInds != null) {
			// Find closest valley indices
			int vLeftInd = -1;
			int vRightInd = -1;
			int counter = 0;
			vLeftInd = 0;
			while (fcIndex > valleyInds[counter]) {
				vLeftInd = valleyInds[counter];
				if (counter == valleyInds.length - 1)
					break;

				counter++;
			}

			counter = valleyInds.length - 1;
			vRightInd = spec.length - 1;
			while (valleyInds[counter] > fcIndex) {
				vRightInd = valleyInds[counter];
				if (counter == 0)
					break;

				counter--;
			}

			for (int i = vLeftInd; i <= vRightInd; i++) {
				if (i != fcIndex)
					Amc += spec[i];
			}
		}

		return Amc;
	}

	public static double estimateVoicingFromFrameSpectrum(double[] absSpec, int samplingRate, float f0,
			double vuvSearchMinHarmonicMultiplier, double vuvSearchMaxHarmonicMultiplier) {
		boolean isVoiced = false;

		int maxFreq = absSpec.length;
		int minFreqInd = SignalProcUtils.freq2index(vuvSearchMinHarmonicMultiplier * f0, samplingRate, maxFreq - 1);
		int maxFreqInd = SignalProcUtils.freq2index(vuvSearchMaxHarmonicMultiplier * f0, samplingRate, maxFreq - 1);
		int harmonicNoFirst = ((int) Math.floor(vuvSearchMinHarmonicMultiplier)) + 1;
		int harmonicNoLast = ((int) Math.floor(vuvSearchMaxHarmonicMultiplier));
		int numHarmonicsInRange = harmonicNoLast - harmonicNoFirst + 1;
		int currentHarmonicInd;
		int i, j;
		int[] harmonicsInds = new int[numHarmonicsInRange];
		for (i = harmonicNoFirst; i <= harmonicNoLast; i++)
			harmonicsInds[i - harmonicNoFirst] = SignalProcUtils.freq2index(i * f0, samplingRate, maxFreq - 1);

		double num = 0.0;
		double denum = 0.0;

		for (j = minFreqInd; j < harmonicsInds[0] - 2; j++)
			num += absSpec[j] * absSpec[j];

		for (i = 0; i < numHarmonicsInRange - 1; i++) {
			for (j = harmonicsInds[i] + 1 + 2; j < harmonicsInds[i + 1] - 2; j++)
				num += absSpec[j] * absSpec[j];
		}

		for (j = harmonicsInds[numHarmonicsInRange - 1] + 1 + 2; j <= maxFreqInd; j++)
			num += absSpec[j] * absSpec[j];

		for (j = minFreqInd; j <= maxFreqInd; j++)
			denum += absSpec[j] * absSpec[j];

		double E = num / denum;
		E = MathUtils.db(E);

		return E;
	}

	public static float[] estimateRefinedPitch(int fftSize, int samplingRateInHz, float leftNeighInHz, float rightNeighInHz,
			float searchStepInHz, float[] initialF0s, float[] maxFrequencyOfVoicings) {
		float[] f0s = new float[initialF0s.length];
		for (int i = 0; i < initialF0s.length; i++)
			f0s[i] = estimateRefinedFramePitch(initialF0s[i], maxFrequencyOfVoicings[i], fftSize, samplingRateInHz,
					leftNeighInHz, rightNeighInHz, searchStepInHz);

		return f0s;
	}

	// Searches for a refined f0 value by searching the range [f0InHz-leftNeighInHz, f0InHz+rightNeighInHz] in steps
	// searchStepInHz
	// This is done by evaluating the error function
	// E(f0New) = sum_i=0^maxVoicedIndex |iInHz-i*f0New|^2
	// for f0New=f0InHz-leftNeighInHz,f0InHz-leftNeighInHz+searchStepInHz,f0InHz-leftNeighInHz+2*searchStepInHz,
	// ...,f0InHz+rightNeighInHz
	// and finding the value of f0New that minimizes it
	public static float estimateRefinedFramePitch(float f0InHz, float maxFreqOfVoicingInHz, int fftSize, int samplingRateInHz,
			float leftNeighInHz, float rightNeighInHz, float searchStepInHz) {
		float refinedF0InHz = 0.0f;
		double E, Emin;
		int maxFreqIndex = fftSize / 2 + 1;
		int maxVoicedFreqInd = SignalProcUtils.freq2index(maxFreqOfVoicingInHz, samplingRateInHz, maxFreqIndex);

		float f0New;
		int i;

		float[] freqIndsInHz = new float[maxVoicedFreqInd];
		for (i = 0; i < maxVoicedFreqInd; i++)
			freqIndsInHz[i] = (float) SignalProcUtils.index2freq(i, samplingRateInHz, maxFreqIndex - 1);

		Emin = Double.MAX_VALUE;
		refinedF0InHz = f0InHz;
		for (f0New = f0InHz - leftNeighInHz; f0New <= f0InHz + rightNeighInHz; f0New += searchStepInHz) {
			E = 0.0;
			for (i = 0; i < maxVoicedFreqInd; i++)
				E += Math.abs(freqIndsInHz[i] - i * f0New);

			if (E < Emin) {
				Emin = E;
				refinedF0InHz = f0New;
			}
		}

		return refinedF0InHz;
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		HnmPitchVoicingAnalyzerParams params = new HnmPitchVoicingAnalyzerParams();

		params.mvfAnalysisWindowSizeInSeconds = 0.040f;
		params.mvfAnalysisSkipSizeInSeconds = 0.010f;
		int windowType = Window.BLACKMAN;
		float f0MinInHz = 60.0f;
		float f0MaxInHz = 500.0f;

		// Pitch refinement parameters
		float leftNeighInHz = 20.0f;
		float rightNeighInHz = 20.0f;
		float searchStepInHz = 0.01f;
		//

		params.fftSize = getDefaultFFTSize(samplingRate);

		float[] initialF0s = HnmPitchVoicingAnalyzer.estimateInitialPitch(x, samplingRate, f0MinInHz, f0MaxInHz, windowType,
				params);
		float[] maxFrequencyOfVoicings = HnmPitchVoicingAnalyzer.analyzeVoicings(x, samplingRate, initialF0s, params, false);
		float[] f0s = HnmPitchVoicingAnalyzer.estimateRefinedPitch(params.fftSize, samplingRate, leftNeighInHz, rightNeighInHz,
				searchStepInHz, initialF0s, maxFrequencyOfVoicings);

		for (int i = 0; i < f0s.length; i++)
			System.out.println(String.valueOf(i * params.mvfAnalysisSkipSizeInSeconds + 0.5f
					* params.mvfAnalysisWindowSizeInSeconds)
					+ " sec. InitialF0=" + String.valueOf(initialF0s[i]) + " RefinedF0=" + String.valueOf(f0s[i]));

		DisplayUtils.plot(initialF0s);
		DisplayUtils.plot(f0s);
	}
}
