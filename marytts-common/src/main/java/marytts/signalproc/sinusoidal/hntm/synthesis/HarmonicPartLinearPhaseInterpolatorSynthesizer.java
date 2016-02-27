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
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * Synthesizes the harmonic part using the linear phase interpolation and phase unwrapping technique described in:
 * 
 * Stylianou, Y., 1996, "Harmonic plus Noise Models for Speech, combined with Statistical Methods, for Speech and Speaker
 * Modification", Ph.D. thesis, Ecole Nationale Supérieure des Télécommunications. (Chapter 3, A Harmonic plus Noise Model, HNM)
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class HarmonicPartLinearPhaseInterpolatorSynthesizer {
	// TO DO: Decrease the buffer sizes since with the latest implementation, we do not have to keep all signal
	// When the user enters a reference file to write separate output tracks to files, set the buffer sizes as we do previously,
	// i.e. sufficient to keep all signal
	// Note that, in normal operation mode, we do not write the harmonic tracks to separate files, i.e. reference file is null.
	private double[] harmonicPart = null;
	private double[][] harmonicTracks;
	private double[][] winOverlapWgt;
	//

	private HntmAnalyzerParams analysisParams;
	private HntmSynthesizerParams synthesisParams;

	private int transitionLen;
	private double[] halfTransitionWinLeft;
	private double[] halfTransitionWinRight;

	private String referenceFile; // Reference if the user wants to write the separate tracks to output

	private int pipeOutStartIndex;
	private int pipeOutEndIndex;
	private int currentFrameIndex;

	private HntmSpeechSignal hnmSignal;
	private boolean isReseted;

	public HarmonicPartLinearPhaseInterpolatorSynthesizer(HntmSpeechSignal hnmSignalIn, HntmAnalyzerParams analysisParamsIn,
			HntmSynthesizerParams synthesisParamsIn) {
		this(hnmSignalIn, analysisParamsIn, synthesisParamsIn, null);
	}

	public HarmonicPartLinearPhaseInterpolatorSynthesizer(HntmSpeechSignal hnmSignalIn, HntmAnalyzerParams analysisParamsIn,
			HntmSynthesizerParams synthesisParamsIn, String referenceFileIn) {
		hnmSignal = hnmSignalIn;
		harmonicPart = null;
		harmonicTracks = null;
		winOverlapWgt = null;

		analysisParams = analysisParamsIn;
		synthesisParams = synthesisParamsIn;
		referenceFile = referenceFileIn;

		transitionLen = SignalProcUtils.time2sample(synthesisParams.unvoicedVoicedTrackTransitionInSeconds,
				hnmSignal.samplingRateInHz);
		Window transitionWin = Window.get(Window.HAMMING, transitionLen * 2);
		transitionWin.normalizePeakValue(1.0f);
		halfTransitionWinLeft = transitionWin.getCoeffsLeftHalf();
		halfTransitionWinRight = transitionWin.getCoeffsRightHalf();

		isReseted = false;

		reset();
	}

	// Reset synthesis variables to start synthesis from the beginning
	public void reset() {
		if (!isReseted) {
			isReseted = true;
			int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);

			harmonicPart = new double[outputLen]; // In fact, this should be prosody scaled length when you implement prosody
													// modifications
			Arrays.fill(harmonicPart, 0.0);

			// Separate tracks
			int k;
			if (analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics > 0) {
				harmonicTracks = new double[analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics][];
				winOverlapWgt = new double[analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics][];
				for (k = 0; k < analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics; k++) {
					harmonicTracks[k] = new double[outputLen];
					Arrays.fill(harmonicTracks[k], 0.0);

					if (synthesisParams.overlappingHarmonicPartSynthesis) {
						winOverlapWgt[k] = new double[outputLen];
						Arrays.fill(winOverlapWgt[k], 0.0);
					}
				}
			}
			//

			pipeOutStartIndex = 0;
			pipeOutEndIndex = -1; // You should increase this appropriately during frame based synthesis. Make sure it always
									// precedes any overlap region

			currentFrameIndex = 0;
			//
		}
	}

	// Is reseted for starting synthesis from the beginning?
	public boolean isReseted() {
		return isReseted;
	}

	public boolean nextFrameAvailable() {
		return currentFrameIndex + 1 < hnmSignal.frames.length;
	}

	// For frame based synthesis from outside, create the same loop as this function does
	// Make sure to call reset() if you want to do synthesis with the identical object more than once
	public double[] synthesizeAll() {
		reset();

		double[] output = null;
		int harmonicPartIndex = 0;
		while (nextFrameAvailable()) {
			output = synthesizeNext();
			if (output != null) {
				System.arraycopy(output, 0, harmonicPart, harmonicPartIndex, output.length);
				harmonicPartIndex += output.length;
			}
		}

		// Generate remaining output
		output = generateOutput(true);
		if (output != null) {
			System.arraycopy(output, 0, harmonicPart, harmonicPartIndex, output.length);
			harmonicPartIndex += output.length;
		}
		//

		return harmonicPart;
	}

	public double[] synthesizeNext() {
		assert currentFrameIndex < hnmSignal.frames.length;

		double[] output = null;

		HntmSpeechFrame prevFrame, nextFrame;

		if (currentFrameIndex > 0)
			prevFrame = hnmSignal.frames[currentFrameIndex - 1];
		else
			prevFrame = null;

		if (currentFrameIndex < hnmSignal.frames.length - 1)
			nextFrame = hnmSignal.frames[currentFrameIndex + 1];
		else
			nextFrame = null;

		boolean isFirstSynthesisFrame = false;
		if (currentFrameIndex == 0)
			isFirstSynthesisFrame = true;

		boolean isLastSynthesisFrame = false;
		if (currentFrameIndex == hnmSignal.frames.length - 1)
			isLastSynthesisFrame = true;

		processFrame(prevFrame, hnmSignal.frames[currentFrameIndex], nextFrame, isFirstSynthesisFrame, isLastSynthesisFrame);

		// Start to generate output as soon as a few frames are processed
		if (currentFrameIndex > synthesisParams.synthesisFramesToAccumulateBeforeAudioGeneration) {
			pipeOutEndIndex = SignalProcUtils.time2sample(hnmSignal.frames[currentFrameIndex
					- synthesisParams.synthesisFramesToAccumulateBeforeAudioGeneration].tAnalysisInSeconds,
					hnmSignal.samplingRateInHz);
			output = generateOutput(false);
		}
		//

		isReseted = false;
		currentFrameIndex++;

		return output;
	}

	private void processFrame(HntmSpeechFrame prevFrame, HntmSpeechFrame currentFrame, HntmSpeechFrame nextFrame,
			boolean isFirstSynthesisFrame, boolean isLastSynthesisFrame) {
		int i, k, n;
		int currentHarmonicNo;
		int numHarmonicsCurrentFrame;

		float f0InHz, f0InHzNext;
		float f0Average, f0AverageNext;

		float[] currentCeps = null;
		float[] nextCeps = null;

		boolean isPrevVoiced = false;
		boolean isVoiced = false;
		boolean isNextVoiced = false;

		double aksi;
		double aksiPlusOne;

		double phaseki;
		double phasekiPlusOne;

		double ht;
		double phasekt = 0.0;

		double phasekiEstimate = 0.0;
		double phasekiPlusOneEstimate = 0.0;
		int Mk;

		boolean isTrackVoiced, isNextTrackVoiced, isPrevTrackVoiced;

		double tsik = 0.0; // Synthesis time in seconds
		double tsikPlusOne = 0.0; // Synthesis time in seconds

		double trackStartInSeconds, trackEndInSeconds;
		int trackStartIndex, trackEndIndex;

		double akt;

		double currentOverlapWinWgt;

		if (prevFrame != null && prevFrame.h != null && prevFrame.h.complexAmps != null && prevFrame.h.complexAmps.length > 0)
			isPrevVoiced = true;

		if (currentFrame.h != null && currentFrame.h.complexAmps != null && currentFrame.h.complexAmps.length > 0)
			isVoiced = true;

		if (nextFrame != null && nextFrame.h != null && nextFrame.h.complexAmps != null && nextFrame.h.complexAmps.length > 0)
			isNextVoiced = true;

		if (isVoiced)
			numHarmonicsCurrentFrame = currentFrame.h.complexAmps.length;
		else if (!isVoiced && isNextVoiced)
			numHarmonicsCurrentFrame = nextFrame.h.complexAmps.length;
		else
			numHarmonicsCurrentFrame = 0;

		f0InHz = currentFrame.f0InHz;

		if (isNextVoiced)
			f0InHzNext = nextFrame.f0InHz;
		else
			f0InHzNext = f0InHz;

		f0Average = 0.5f * (f0InHz + f0InHzNext);

		if (!analysisParams.useHarmonicAmplitudesDirectly) {
			currentCeps = currentFrame.h.getCeps(f0InHz, hnmSignal.samplingRateInHz, analysisParams);
			if (nextFrame != null)
				nextCeps = nextFrame.h.getCeps(f0InHzNext, hnmSignal.samplingRateInHz, analysisParams);
			else
				nextCeps = null;
		}

		for (k = 0; k < numHarmonicsCurrentFrame; k++) {
			currentHarmonicNo = k + 1;

			aksi = 0.0;
			aksiPlusOne = 0.0;

			phaseki = 0.0f;
			phasekiPlusOne = 0.0f;

			isPrevTrackVoiced = false;
			isTrackVoiced = false;
			isNextTrackVoiced = false;

			if (prevFrame != null && prevFrame.h != null && prevFrame.h.complexAmps != null && prevFrame.h.complexAmps.length > k)
				isPrevTrackVoiced = true;

			if (currentFrame != null && currentFrame.h != null && currentFrame.h.complexAmps != null
					&& currentFrame.h.complexAmps.length > k)
				isTrackVoiced = true;

			if (nextFrame != null && nextFrame.h != null && nextFrame.h.complexAmps != null && nextFrame.h.complexAmps.length > k)
				isNextTrackVoiced = true;

			tsik = currentFrame.tAnalysisInSeconds;

			if (isFirstSynthesisFrame)
				trackStartInSeconds = 0.0;
			else
				trackStartInSeconds = tsik;

			if (isLastSynthesisFrame || nextFrame == null)
				tsikPlusOne = hnmSignal.originalDurationInSeconds;
			else
				tsikPlusOne = nextFrame.tAnalysisInSeconds;

			trackEndInSeconds = tsikPlusOne;

			if (synthesisParams.overlappingHarmonicPartSynthesis) {
				trackStartInSeconds -= synthesisParams.harmonicSynthesisOverlapInSeconds;
				trackEndInSeconds += synthesisParams.harmonicSynthesisOverlapInSeconds;
			}

			trackStartIndex = SignalProcUtils.time2sample(trackStartInSeconds, hnmSignal.samplingRateInHz);
			trackEndIndex = SignalProcUtils.time2sample(trackEndInSeconds, hnmSignal.samplingRateInHz);

			if (!synthesisParams.overlappingHarmonicPartSynthesis) {
				if (!isPrevTrackVoiced)
					trackStartIndex -= transitionLen;
				if (!isNextTrackVoiced)
					trackEndIndex += transitionLen;
			}

			Window overlapWin = null;
			double[] overlapWinWgt = null;
			if (synthesisParams.overlappingHarmonicPartSynthesis) {
				overlapWin = Window.get(Window.HAMMING, trackEndIndex - trackStartIndex + 1);
				overlapWin.normalizePeakValue(1.0f);
				overlapWinWgt = overlapWin.getCoeffs();
			}

			if (isTrackVoiced && trackEndIndex - trackStartIndex + 1 > 0) {
				// Amplitudes
				if (isTrackVoiced) {
					if (!analysisParams.useHarmonicAmplitudesDirectly) {
						if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
							aksi = RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(currentCeps,
									currentHarmonicNo * f0InHz, hnmSignal.samplingRateInHz);
						else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
							aksi = RegularizedPostWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(currentCeps,
									currentHarmonicNo * f0InHz, hnmSignal.samplingRateInHz);
					} else {
						if (k < currentFrame.h.complexAmps.length)
							aksi = MathUtils.magnitudeComplex(currentFrame.h.complexAmps[k]); // Use amplitudes directly without
																								// cepstrum method
					}
				} else
					aksi = 0.0;

				if (isNextTrackVoiced) {
					if (!analysisParams.useHarmonicAmplitudesDirectly) {
						if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
							aksiPlusOne = RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(nextCeps,
									currentHarmonicNo * f0InHzNext, hnmSignal.samplingRateInHz);
						else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
							aksiPlusOne = RegularizedPostWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(nextCeps,
									currentHarmonicNo * f0InHzNext, hnmSignal.samplingRateInHz);
					} else {
						if (k < nextFrame.h.complexAmps.length)
							aksiPlusOne = MathUtils.magnitudeComplex(nextFrame.h.complexAmps[k]); // Use amplitudes directly
																									// without cepstrum method
					}
				} else
					aksiPlusOne = 0.0;
				//

				// Phases
				if (isTrackVoiced) {
					if (currentHarmonicNo == 0)
						phaseki = 0.0f;
					else
						phaseki = MathUtils.phaseInRadians(currentFrame.h.complexAmps[k]);
				}
				if (isNextTrackVoiced) {
					if (currentHarmonicNo == 0)
						phasekiPlusOne = 0.0f;
					else
						phasekiPlusOne = MathUtils.phaseInRadians(nextFrame.h.complexAmps[k]);
				}

				// phaseki += MathUtils.degrees2radian(-4.0);
				// phasekiPlusOne += MathUtils.degrees2radian(-4.0);

				if (!isTrackVoiced && isNextTrackVoiced) {
					phaseki = (float) (phasekiPlusOne - currentHarmonicNo * MathUtils.TWOPI * f0InHzNext * (tsikPlusOne - tsik)); // Equation
																																	// (3.54)
					aksi = 0.0;
				} else if (isTrackVoiced && !isNextTrackVoiced) {
					phasekiPlusOne = phaseki + currentHarmonicNo * MathUtils.TWOPI * f0InHz * (tsikPlusOne - tsik); // Equation
																													// (3.55)
					aksiPlusOne = 0.0;
				}

				phasekiPlusOneEstimate = phaseki + currentHarmonicNo * MathUtils.TWOPI * f0Average * (tsikPlusOne - tsik);
				// phasekiPlusOneEstimate = MathUtils.TWOPI*(Math.random()-0.5); //Random phase

				// System.out.println(String.valueOf(f0Average) + " - " + String.valueOf(f0InHz) + " - " +
				// String.valueOf(f0InHzNext));

				Mk = (int) Math.floor((phasekiPlusOneEstimate - phasekiPlusOne) / MathUtils.TWOPI + 0.5);
				//

				for (n = Math.max(0, trackStartIndex); n <= Math.min(trackEndIndex, harmonicPart.length - 1); n++) {
					double t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

					// if (t>=tsik && t<tsikPlusOne)
					{
						// Amplitude estimate
						if (t < tsik)
							akt = MathUtils.interpolatedSample(tsik - synthesisParams.unvoicedVoicedTrackTransitionInSeconds, t,
									tsik, 0.0, aksi);
						else if (t > tsikPlusOne)
							akt = MathUtils.interpolatedSample(tsikPlusOne, t, tsikPlusOne
									+ synthesisParams.unvoicedVoicedTrackTransitionInSeconds, aksiPlusOne, 0.0);
						else
							akt = MathUtils.interpolatedSample(tsik, t, tsikPlusOne, aksi, aksiPlusOne);
						//

						// Phase estimate
						phasekt = phaseki + (phasekiPlusOne + MathUtils.TWOPI * Mk - phaseki) * (t - tsik) / (tsikPlusOne - tsik);
						//

						if (synthesisParams.overlappingHarmonicPartSynthesis) {
							currentOverlapWinWgt = overlapWinWgt[n - Math.max(0, trackStartIndex)];
							winOverlapWgt[k][n] += currentOverlapWinWgt;
						} else
							currentOverlapWinWgt = 1.0;

						if (!isPrevTrackVoiced && n - trackStartIndex < transitionLen)
							harmonicTracks[k][n] = currentOverlapWinWgt * halfTransitionWinLeft[n - trackStartIndex] * akt
									* Math.cos(phasekt);
						else if (!isNextTrackVoiced && trackEndIndex - n < transitionLen)
							harmonicTracks[k][n] = currentOverlapWinWgt
									* halfTransitionWinRight[transitionLen - (trackEndIndex - n) - 1] * akt * Math.cos(phasekt);
						else
							harmonicTracks[k][n] = currentOverlapWinWgt * akt * Math.cos(phasekt);
					}
				}
			}
		}
	}

	public double[] generateOutput(boolean pipeOutAllOutput) {
		double[] output = null;

		if (harmonicTracks != null) {
			int k, n;

			if (pipeOutAllOutput)
				pipeOutEndIndex = harmonicPart.length;

			output = new double[Math.min(pipeOutEndIndex, harmonicPart.length - 1) - pipeOutStartIndex + 1];
			if (!synthesisParams.overlappingHarmonicPartSynthesis) {
				for (k = 0; k < harmonicTracks.length; k++) {
					// for (n=0; n<harmonicPart.length; n++)
					for (n = pipeOutStartIndex; n <= Math.min(pipeOutEndIndex, harmonicPart.length - 1); n++) {
						// harmonicPart[n] += harmonicTracks[k][n];
						output[n - pipeOutStartIndex] += harmonicTracks[k][n];
					}
				}
			} else {
				for (k = 0; k < harmonicTracks.length; k++) {
					// for (n=0; n<harmonicPart.length; n++)
					for (n = pipeOutStartIndex; n <= Math.min(pipeOutEndIndex, harmonicPart.length - 1); n++) {
						if (winOverlapWgt[k][n] > 0.0f) {
							// harmonicPart[n] += harmonicTracks[k][n]/winOverlapWgt[k][n];
							output[n - pipeOutStartIndex] += harmonicTracks[k][n] / winOverlapWgt[k][n];
						} else {
							// harmonicPart[n] += harmonicTracks[k][n];
							output[n - pipeOutStartIndex] += harmonicTracks[k][n];
						}
					}
				}
			}

			pipeOutStartIndex = pipeOutEndIndex + 1;

			if (pipeOutAllOutput && referenceFile != null && FileUtils.exists(referenceFile)
					&& synthesisParams.writeSeparateHarmonicTracksToOutputs) {
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
					for (k = 0; k < harmonicTracks.length; k++) {
						harmonicTracks[k] = MathUtils.divide(harmonicTracks[k], 32767.0);

						DDSAudioInputStream outputAudio = new DDSAudioInputStream(
								new BufferedDoubleDataSource(harmonicTracks[k]), inputAudio.getFormat());
						String outFileName = StringUtils.getFolderName(referenceFile) + "harmonicTrack" + String.valueOf(k + 1)
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

		return output;
	}
}
