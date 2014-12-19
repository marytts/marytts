/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.signalproc.sinusoidal.hntm.analysis;

import java.util.Arrays;

import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.signalproc.window.Window;
import marytts.util.math.ArrayUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 * 
 */
public class HntmAnalyzerNoisePartWaveformSynthesizer {

	// LPC based noise model + OLA approach + Gain normalization according to generated harmonic part gain
	public static double[] synthesize(HntmSpeechSignal hnmSignal, double[][] frameWaveforms, HntmAnalyzerParams analysisParams,
			HntmSynthesizerParams synthesisParams) {
		double[] noisePart = null;
		int i;
		boolean isPrevNoised, isNoised, isNextNoised;
		boolean isVoiced, isNextVoiced;
		float t;
		float tsi = 0.0f;
		int startIndex = 0;
		int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);

		isNoised = false;
		for (i = 0; i < hnmSignal.frames.length; i++) {
			if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz < 0.5f * hnmSignal.samplingRateInHz
					&& frameWaveforms[i] != null) {
				isNoised = true;
				break;
			}
		}

		if (isNoised) // At least one noisy frame with LP coefficients exist
		{
			noisePart = new double[outputLen]; // In fact, this should be prosody scaled length when you implement prosody
												// modifications
			Arrays.fill(noisePart, 0.0);
			double[] winWgtSum = new double[outputLen]; // In fact, this should be prosody scaled length when you implement
														// prosody modifications
			Arrays.fill(winWgtSum, 0.0);

			Window winNoise;
			int windowType = Window.HAMMING;
			double[] x;
			double[] xWindowed;
			double[] y;
			double[] yWindowed;
			double[] yFiltered;
			double[] wgt;
			int n;
			int fftSizeNoise = SignalProcUtils.getDFTSize(hnmSignal.samplingRateInHz);

			boolean isDisplay = false;

			// Noise source of full length
			double[] noiseSourceHpf = null;

			int transitionOverlapLen = SignalProcUtils.time2sample(synthesisParams.noiseSynthesisTransitionOverlapInSeconds,
					hnmSignal.samplingRateInHz);

			int wsNoise = SignalProcUtils.time2sample(analysisParams.noiseAnalysisWindowDurationInSeconds,
					hnmSignal.samplingRateInHz);
			if (wsNoise % 2 == 1)
				wsNoise++;
			int halfWsNoise = wsNoise / 2;
			y = new double[wsNoise];

			// Compute window
			winNoise = Window.get(windowType, wsNoise);
			winNoise.normalizePeakValue(1.0f);
			wgt = winNoise.getCoeffs();
			//

			for (i = 0; i < hnmSignal.frames.length; i++) {
				if (hnmSignal.frames[i].h != null && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz > 0.0f)
					isVoiced = true;
				else
					isVoiced = false;

				if (i < hnmSignal.frames.length - 1 && hnmSignal.frames[i + 1].h != null
						&& hnmSignal.frames[i + 1].maximumFrequencyOfVoicingInHz > 0.0f)
					isNextVoiced = true;
				else
					isNextVoiced = false;

				if (i > 0 && hnmSignal.frames[i - 1].maximumFrequencyOfVoicingInHz < 0.5f * hnmSignal.samplingRateInHz
						&& frameWaveforms[i - 1] != null)
					isPrevNoised = true;
				else
					isPrevNoised = false;

				if (frameWaveforms[i] != null
						&& hnmSignal.frames[i].maximumFrequencyOfVoicingInHz < 0.5f * hnmSignal.samplingRateInHz)
					isNoised = true;
				else
					isNoised = false;

				if (i < hnmSignal.frames.length - 1
						&& hnmSignal.frames[i + 1].maximumFrequencyOfVoicingInHz < 0.5f * hnmSignal.samplingRateInHz
						&& frameWaveforms[i + 1] != null)
					isNextNoised = true;
				else
					isNextNoised = false;

				if (i == 0)
					tsi = 0.0f;
				else
					tsi = Math.max(0.0f, hnmSignal.frames[i].tAnalysisInSeconds - 0.5f
							* analysisParams.noiseAnalysisWindowDurationInSeconds);

				startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);

				if (isNoised) {
					if (frameWaveforms[i] != null) {
						if (analysisParams.overlapNoiseWaveformModel) {
							y = ArrayUtils.copy(frameWaveforms[i]);

							if (isVoiced && analysisParams.hpfBeforeNoiseAnalysis && analysisParams.decimateNoiseWaveform)
								y = SignalProcUtils.interpolate(y, (0.5 * hnmSignal.samplingRateInHz)
										/ (0.5 * hnmSignal.samplingRateInHz - hnmSignal.frames[i].maximumFrequencyOfVoicingInHz));
						} else {
							// Fill left half
							boolean isTmpVoiced;
							int currentFrameInd = i;
							int count = 0;
							Arrays.fill(y, 0.0);
							double[] temp = ArrayUtils.copy(frameWaveforms[currentFrameInd]);
							if (isVoiced && analysisParams.hpfBeforeNoiseAnalysis && analysisParams.decimateNoiseWaveform)
								temp = SignalProcUtils
										.interpolate(
												temp,
												(0.5 * hnmSignal.samplingRateInHz)
														/ (0.5 * hnmSignal.samplingRateInHz - hnmSignal.frames[currentFrameInd].maximumFrequencyOfVoicingInHz));

							int count2 = temp.length - 1;
							while (count < halfWsNoise) {
								if (count2 < 0) {
									currentFrameInd--;
									if (currentFrameInd < 0 || hnmSignal.frames[currentFrameInd].n == null)
										break;

									temp = ArrayUtils.copy(frameWaveforms[currentFrameInd]);
									isTmpVoiced = false;
									if (hnmSignal.frames[currentFrameInd].h != null
											&& hnmSignal.frames[currentFrameInd].maximumFrequencyOfVoicingInHz > 0.0f)
										isTmpVoiced = true;

									if (isTmpVoiced && analysisParams.hpfBeforeNoiseAnalysis
											&& analysisParams.decimateNoiseWaveform)
										temp = SignalProcUtils
												.interpolate(
														temp,
														(0.5 * hnmSignal.samplingRateInHz)
																/ (0.5 * hnmSignal.samplingRateInHz - hnmSignal.frames[currentFrameInd].maximumFrequencyOfVoicingInHz));

									count2 = temp.length - 1;
								}

								y[halfWsNoise - count - 1] = temp[count2];
								count++;
								count2--;
							}
							//

							// Fill right half
							currentFrameInd = i + 1;
							count = halfWsNoise;
							if (currentFrameInd < hnmSignal.frames.length && hnmSignal.frames[currentFrameInd].n != null) {
								temp = ((FrameNoisePartWaveform) hnmSignal.frames[currentFrameInd].n).waveform2Doubles();

								isTmpVoiced = false;
								if (hnmSignal.frames[currentFrameInd].h != null
										&& hnmSignal.frames[currentFrameInd].maximumFrequencyOfVoicingInHz > 0.0f)
									isTmpVoiced = true;
								if (isTmpVoiced && analysisParams.hpfBeforeNoiseAnalysis && analysisParams.decimateNoiseWaveform)
									temp = SignalProcUtils
											.interpolate(
													temp,
													(0.5 * hnmSignal.samplingRateInHz)
															/ (0.5 * hnmSignal.samplingRateInHz - hnmSignal.frames[currentFrameInd].maximumFrequencyOfVoicingInHz));

								count2 = 0;
								while (count < wsNoise) {
									if (count2 >= temp.length) {
										currentFrameInd++;
										if (currentFrameInd > hnmSignal.frames.length - 1
												|| hnmSignal.frames[currentFrameInd].n == null)
											break;

										temp = ((FrameNoisePartWaveform) hnmSignal.frames[currentFrameInd].n).waveform2Doubles();

										isTmpVoiced = false;
										if (hnmSignal.frames[currentFrameInd].h != null
												&& hnmSignal.frames[currentFrameInd].maximumFrequencyOfVoicingInHz > 0.0f)
											isTmpVoiced = true;

										if (isTmpVoiced && analysisParams.hpfBeforeNoiseAnalysis
												&& analysisParams.decimateNoiseWaveform)
											temp = SignalProcUtils
													.interpolate(
															temp,
															(0.5 * hnmSignal.samplingRateInHz)
																	/ (0.5 * hnmSignal.samplingRateInHz - hnmSignal.frames[currentFrameInd].maximumFrequencyOfVoicingInHz));

										count2 = 0;
									}

									y[count] = temp[count2];
									count++;
									count2++;
								}
							}
						}
						//

						if (!synthesisParams.hpfAfterNoiseSynthesis)
							y = SignalProcUtils.fdFilter(y, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz,
									0.5f * hnmSignal.samplingRateInHz, hnmSignal.samplingRateInHz, fftSizeNoise);

						winNoise = Window.get(windowType, y.length);
						winNoise.normalizePeakValue(1.0f);
						wgt = winNoise.getCoeffs();

						// Overlap-add
						for (n = startIndex; n < Math.min(startIndex + y.length, noisePart.length); n++) {
							noisePart[n] += y[n - startIndex] * wgt[n - startIndex];
							winWgtSum[n] += wgt[n - startIndex];
						}
					}
					//
				}

				if (!analysisParams.isSilentAnalysis)
					System.out.println("Waveform noise synthesis for analysis complete at "
							+ String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame " + String.valueOf(i + 1)
							+ " of " + String.valueOf(hnmSignal.frames.length) + "...");
			}

			for (i = 0; i < winWgtSum.length; i++) {
				if (winWgtSum[i] > 0.0)
					noisePart[i] /= winWgtSum[i];
			}
		}

		if (analysisParams.preemphasisCoefNoise > 0.0f)
			noisePart = SignalProcUtils.removePreemphasis(noisePart, analysisParams.preemphasisCoefNoise);

		// MathUtils.adjustMean(noisePart, 0.0);

		return noisePart;
	}
}
