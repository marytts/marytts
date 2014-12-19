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

package marytts.signalproc.sinusoidal.hntm.synthesis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPostWarpedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPreWarpedCepstrumEstimator;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartPseudoHarmonic;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * A pseudo-harmonic representation based synthesizer for the noise part.
 * 
 * Reference: Stylianou, Y., 1996, "Harmonic plus Noise Models for Speech, combined with Statistical Methods, for Speech and
 * Speaker Modification", Ph.D. thesis, Ecole Nationale Supérieure des Télécommunications.
 * 
 * @author oytun.turk
 * 
 */
public class NoisePartPseudoHarmonicSynthesizer {

	// Pseudo harmonics based noise generation for pseudo periods
	public static double[] synthesize(HntmSpeechSignal hnmSignal, HntmAnalyzerParams analysisParams,
			HntmSynthesizerParams synthesisParams, String referenceFile) {
		double[] noisePart = null;
		int trackNoToExamine = 1;

		int i, k, n;
		double t; // Time in seconds

		double tsik = 0.0; // Synthesis time in seconds
		double tsikPlusOne = 0.0; // Synthesis time in seconds

		double trackStartInSeconds, trackEndInSeconds;
		// double lastPeriodInSeconds = 0.0;
		int trackStartIndex, trackEndIndex;
		double akt;
		int numHarmonicsCurrentFrame, numHarmonicsPrevFrame, numHarmonicsNextFrame;
		int harmonicIndexShiftPrev, harmonicIndexShiftCurrent, harmonicIndexShiftNext;
		int maxNumHarmonics = 0;
		for (i = 0; i < hnmSignal.frames.length; i++) {
			if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz > 0.0f && hnmSignal.frames[i].n != null) {
				numHarmonicsCurrentFrame = (int) Math.floor(hnmSignal.samplingRateInHz / analysisParams.noiseF0InHz + 0.5);
				numHarmonicsCurrentFrame = Math.max(0, numHarmonicsCurrentFrame);
				if (numHarmonicsCurrentFrame > maxNumHarmonics)
					maxNumHarmonics = numHarmonicsCurrentFrame;
			}
		}

		double aksi;
		double aksiPlusOne;

		float[] phasekis = null;
		float phasekiPlusOne;

		double ht;
		float phasekt = 0.0f;

		float phasekiEstimate = 0.0f;
		float phasekiPlusOneEstimate = 0.0f;
		int Mk;
		boolean isPrevNoised, isNoised, isNextNoised;
		boolean isTrackNoised, isNextTrackNoised, isPrevTrackNoised;
		int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);

		noisePart = new double[outputLen]; // In fact, this should be prosody scaled length when you implement prosody
											// modifications
		Arrays.fill(noisePart, 0.0);

		// Write separate tracks to output
		double[][] noiseTracks = null;

		if (maxNumHarmonics > 0) {
			noiseTracks = new double[maxNumHarmonics][];
			for (k = 0; k < maxNumHarmonics; k++) {
				noiseTracks[k] = new double[outputLen];
				Arrays.fill(noiseTracks[k], 0.0);
			}

			phasekis = new float[maxNumHarmonics];
			for (k = 0; k < maxNumHarmonics; k++)
				phasekis[k] = (float) (MathUtils.TWOPI * (Math.random() - 0.5));
		}
		//

		int transitionLen = SignalProcUtils.time2sample(synthesisParams.unvoicedVoicedTrackTransitionInSeconds,
				hnmSignal.samplingRateInHz);
		Window transitionWin = Window.get(Window.HAMMING, transitionLen * 2);
		transitionWin.normalizePeakValue(1.0f);
		double[] halfTransitionWinLeft = transitionWin.getCoeffsLeftHalf();
		float halfFs = hnmSignal.samplingRateInHz;

		for (i = 0; i < hnmSignal.frames.length; i++) {
			isPrevNoised = false;
			isNoised = false;
			isNextNoised = false;

			if (i > 0 && hnmSignal.frames[i - 1].n != null && hnmSignal.frames[i - 1].maximumFrequencyOfVoicingInHz < halfFs
					&& ((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i - 1].n).ceps != null)
				isPrevNoised = true;

			if (i > 0 && hnmSignal.frames[i].n != null && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz < halfFs
					&& ((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps != null)
				isNoised = true;

			if (i < hnmSignal.frames.length - 1 && hnmSignal.frames[i + 1].maximumFrequencyOfVoicingInHz < halfFs
					&& hnmSignal.frames[i + 1].n != null
					&& ((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i + 1].n).ceps != null)
				isNextNoised = true;

			numHarmonicsPrevFrame = 0;
			numHarmonicsCurrentFrame = 0;
			numHarmonicsNextFrame = 0;
			harmonicIndexShiftPrev = 0;
			harmonicIndexShiftCurrent = 0;
			harmonicIndexShiftNext = 0;

			if (isPrevNoised) {
				numHarmonicsPrevFrame = (int) Math
						.floor((hnmSignal.samplingRateInHz - hnmSignal.frames[i - 1].maximumFrequencyOfVoicingInHz)
								/ analysisParams.noiseF0InHz + 0.5);
				numHarmonicsPrevFrame = Math.max(0, numHarmonicsPrevFrame);
				harmonicIndexShiftPrev = (int) Math.floor(hnmSignal.frames[i - 1].maximumFrequencyOfVoicingInHz
						/ analysisParams.noiseF0InHz + 0.5);
				harmonicIndexShiftPrev = Math.max(1, harmonicIndexShiftPrev);
			}

			if (isNoised) {
				numHarmonicsCurrentFrame = (int) Math
						.floor((hnmSignal.samplingRateInHz - hnmSignal.frames[i].maximumFrequencyOfVoicingInHz)
								/ analysisParams.noiseF0InHz + 0.5);
				numHarmonicsCurrentFrame = Math.max(0, numHarmonicsCurrentFrame);
				harmonicIndexShiftCurrent = (int) Math.floor(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz
						/ analysisParams.noiseF0InHz + 0.5);
				harmonicIndexShiftCurrent = Math.max(1, harmonicIndexShiftCurrent);
			} else if (!isNoised && isNextNoised) {
				numHarmonicsCurrentFrame = (int) Math
						.floor((hnmSignal.samplingRateInHz - hnmSignal.frames[i + 1].maximumFrequencyOfVoicingInHz)
								/ analysisParams.noiseF0InHz + 0.5);
				numHarmonicsCurrentFrame = Math.max(0, numHarmonicsCurrentFrame);
				harmonicIndexShiftCurrent = (int) Math.floor(hnmSignal.frames[i + 1].maximumFrequencyOfVoicingInHz
						/ analysisParams.noiseF0InHz + 0.5);
				harmonicIndexShiftCurrent = Math.max(1, harmonicIndexShiftCurrent);
			}

			if (isNextNoised) {
				numHarmonicsNextFrame = (int) Math
						.floor((hnmSignal.samplingRateInHz - hnmSignal.frames[i + 1].maximumFrequencyOfVoicingInHz)
								/ analysisParams.noiseF0InHz + 0.5);
				numHarmonicsNextFrame = Math.max(0, numHarmonicsNextFrame);
				harmonicIndexShiftNext = (int) Math.floor(hnmSignal.frames[i + 1].maximumFrequencyOfVoicingInHz
						/ analysisParams.noiseF0InHz + 0.5);
				harmonicIndexShiftNext = Math.max(1, harmonicIndexShiftNext);
			}

			for (k = 0; k < numHarmonicsCurrentFrame; k++) {
				aksi = 0.0;
				aksiPlusOne = 0.0;

				phasekiPlusOne = 0.0f;

				isPrevTrackNoised = false;
				isTrackNoised = false;
				isNextTrackNoised = false;

				if (i > 0 && hnmSignal.frames[i - 1].n != null && numHarmonicsPrevFrame > k)
					isPrevTrackNoised = true;

				if (hnmSignal.frames[i].n != null && numHarmonicsCurrentFrame > k)
					isTrackNoised = true;

				if (i < hnmSignal.frames.length - 1 && hnmSignal.frames[i + 1].n != null && numHarmonicsNextFrame > k)
					isNextTrackNoised = true;

				tsik = hnmSignal.frames[i].tAnalysisInSeconds;

				if (i == 0)
					trackStartInSeconds = 0.0;
				else
					trackStartInSeconds = tsik;

				if (i == hnmSignal.frames.length - 1)
					tsikPlusOne = hnmSignal.originalDurationInSeconds;
				else
					tsikPlusOne = hnmSignal.frames[i + 1].tAnalysisInSeconds;

				trackEndInSeconds = tsikPlusOne;

				trackStartIndex = SignalProcUtils.time2sample(trackStartInSeconds, hnmSignal.samplingRateInHz);
				trackEndIndex = SignalProcUtils.time2sample(trackEndInSeconds, hnmSignal.samplingRateInHz);

				if (isTrackNoised && trackEndIndex - trackStartIndex + 1 > 0) {
					// Amplitudes
					if (isTrackNoised) {
						if (!analysisParams.useNoiseAmplitudesDirectly) {
							if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
								aksi = RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(
										((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps,
										(k + harmonicIndexShiftCurrent) * analysisParams.noiseF0InHz, hnmSignal.samplingRateInHz);
							else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
								aksi = RegularizedPostWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(
										((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps,
										(k + harmonicIndexShiftCurrent) * analysisParams.noiseF0InHz, hnmSignal.samplingRateInHz);
						} else {
							if (k < ((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps.length)
								aksi = ((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps[k]; // Use amplitudes directly
																										// without cepstrum method
							else
								aksi = 0.0;
						}
					} else
						aksi = 0.0;

					if (isNextTrackNoised) {
						if (!analysisParams.useNoiseAmplitudesDirectly) {
							if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
								aksiPlusOne = RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(
										((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i + 1].n).ceps,
										(k + harmonicIndexShiftNext) * analysisParams.noiseF0InHz, hnmSignal.samplingRateInHz);
							else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
								aksiPlusOne = RegularizedPostWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(
										((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i + 1].n).ceps,
										(k + harmonicIndexShiftNext) * analysisParams.noiseF0InHz, hnmSignal.samplingRateInHz);
						} else {
							if (k < ((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i + 1].n).ceps.length)
								aksiPlusOne = ((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i + 1].n).ceps[k]; // Use
																													// amplitudes
																													// directly
																													// without
																													// cepstrum
																													// method
							else
								aksiPlusOne = 0.0;
						}
					} else
						aksiPlusOne = 0.0;
					//

					// Phases
					phasekis[k] = (float) (MathUtils.TWOPI * (Math.random() - 0.5));
					phasekiPlusOne = (float) (phasekis[k] + (k + harmonicIndexShiftCurrent) * MathUtils.TWOPI
							* analysisParams.noiseF0InHz * (tsikPlusOne - tsik)); // Equation (3.55)
					//

					if (!isPrevTrackNoised)
						trackStartIndex = Math.max(0, trackStartIndex - transitionLen);

					for (n = trackStartIndex; n <= Math.min(trackEndIndex, outputLen - 1); n++) {
						t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

						// if (t>=tsik && t<tsikPlusOne)
						{
							// Amplitude estimate
							akt = MathUtils.interpolatedSample(tsik, t, tsikPlusOne, aksi, aksiPlusOne);
							//

							// Phase estimate
							phasekt = (float) (phasekiPlusOne * (t - tsik) / (tsikPlusOne - tsik));
							//

							if (!isPrevTrackNoised && n - trackStartIndex < transitionLen)
								noiseTracks[k][n] = halfTransitionWinLeft[n - trackStartIndex] * akt * Math.cos(phasekt);
							else
								noiseTracks[k][n] = akt * Math.cos(phasekt);
						}
					}

					phasekis[k] = phasekiPlusOne;
				}
			}
		}

		for (k = 0; k < noiseTracks.length; k++) {
			for (n = 0; n < noisePart.length; n++)
				noisePart[n] += noiseTracks[k][n];
		}

		// Write separate tracks to output
		if (noiseTracks != null) {
			for (k = 0; k < noiseTracks.length; k++) {
				for (n = 0; n < noisePart.length; n++)
					noisePart[n] += noiseTracks[k][n];
			}

			if (referenceFile != null && FileUtils.exists(referenceFile) && synthesisParams.writeSeparateHarmonicTracksToOutputs) {
				// Write separate tracks to output
				AudioInputStream inputAudio = null;
				try {
					inputAudio = AudioSystem.getAudioInputStream(new File(referenceFile));
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (inputAudio != null) {
					// k=1;
					for (k = 0; k < noiseTracks.length; k++) {
						noiseTracks[k] = MathUtils.divide(noiseTracks[k], 32767.0);

						DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(noiseTracks[k]),
								inputAudio.getFormat());
						String outFileName = StringUtils.getFolderName(referenceFile) + "noiseTrack" + String.valueOf(k + 1)
								+ ".wav";
						try {
							AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			//
		}

		return noisePart;
	}
}
