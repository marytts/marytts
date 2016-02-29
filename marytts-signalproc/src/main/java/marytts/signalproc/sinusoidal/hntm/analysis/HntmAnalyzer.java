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
package marytts.signalproc.sinusoidal.hntm.analysis;

import java.io.IOException;
import java.util.Arrays;

import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPostWarpedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPreWarpedCepstrumEstimator;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechFrame;
import marytts.signalproc.sinusoidal.PeakMatchedSinusoidalSynthesizer;
import marytts.signalproc.sinusoidal.SinusoidalAnalysisParams;
import marytts.signalproc.sinusoidal.SinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.SinusoidalTracks;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.HnmPitchVoicingAnalyzer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HarmonicPartLinearPhaseInterpolatorSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.signalproc.sinusoidal.hntm.synthesis.hybrid.HarmonicsToTrackConverter;
import marytts.signalproc.window.GaussWindow;
import marytts.signalproc.window.Window;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexNumber;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import Jampack.H;
import Jampack.Inv;
import Jampack.JampackException;
import Jampack.Parameters;
import Jampack.Times;
import Jampack.Z;
import Jampack.Zdiagmat;
import Jampack.Zmat;

/**
 * This class implements a harmonic+noise model for speech as described in
 * 
 * Stylianou, Y., 1996, "Harmonic plus Noise Models for Speech, combined with Statistical Methods, for Speech and Speaker
 * Modification", Ph.D. thesis, Ecole Nationale Supérieure des Télécommunications. (Chapter 3, A Harmonic plus Noise Model, HNM)
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class HntmAnalyzer {

	public HntmAnalyzer() {

	}

	public HntmSpeechSignal analyze(short[] x, int fs, PitchReaderWriter f0) {
		double[] xDouble = ArrayUtils.copyShort2Double(x);

		return analyze(xDouble, fs, f0);
	}

	public HntmSpeechSignal analyze(double[] x, int fs, PitchReaderWriter f0) {
		HntmAnalyzerParams analysisParams = new HntmAnalyzerParams(); // Using default analysis parameters
		HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis = new HntmSynthesizerParams(); // Using defaulot synthesis
																								// parameters before noise
																								// analysis

		return analyze(x, fs, f0, null, analysisParams, synthesisParamsBeforeNoiseAnalysis, null);
	}

	public HntmSpeechSignal analyze(short[] x, int fs, PitchReaderWriter f0, Labels labels, HntmAnalyzerParams analysisParams,
			HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis) {
		return analyze(x, fs, f0, labels, analysisParams, synthesisParamsBeforeNoiseAnalysis, null);
	}

	public HntmSpeechSignal analyze(short[] x, int fs, PitchReaderWriter f0, Labels labels, HntmAnalyzerParams analysisParams,
			HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis, String analysisResultsFile) {
		int pitchMarkOffset = 0;
		PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, x.length, f0.header.windowSizeInSeconds,
				f0.header.skipSizeInSeconds, false, pitchMarkOffset);

		return analyze(x, fs, pm, f0.header.windowSizeInSeconds, f0.header.skipSizeInSeconds,
				ArrayUtils.copyDouble2Float(f0.contour), labels, analysisParams, synthesisParamsBeforeNoiseAnalysis,
				analysisResultsFile);
	}

	public HntmSpeechSignal analyze(short[] x, int fs, PitchMarks pm, double f0WindowSizeInSeconds, double f0SkipSizeInSeconds,
			float[] f0Contour, Labels labels, HntmAnalyzerParams analysisParams,
			HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis, String analysisResultsFile) {
		double[] xDouble = ArrayUtils.copyShort2Double(x);

		return analyze(xDouble, fs, pm, f0WindowSizeInSeconds, f0SkipSizeInSeconds, f0Contour, labels, analysisParams,
				synthesisParamsBeforeNoiseAnalysis, analysisResultsFile);
	}

	public HntmSpeechSignal analyze(double[] x, int fs, PitchReaderWriter f0, Labels labels, HntmAnalyzerParams analysisParams,
			HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis, String analysisResultsFile) {
		int pitchMarkOffset = 0;
		PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, x.length, f0.header.windowSizeInSeconds,
				f0.header.skipSizeInSeconds, false, pitchMarkOffset);

		return analyze(x, fs, pm, f0.header.windowSizeInSeconds, f0.header.skipSizeInSeconds,
				ArrayUtils.copyDouble2Float(f0.contour), labels, analysisParams, synthesisParamsBeforeNoiseAnalysis,
				analysisResultsFile);
	}

	public HntmSpeechSignal analyze(double[] x, int fs, PitchMarks pm, double f0WindowSizeInSeconds, double f0SkipSizeInSeconds,
			float[] f0Contour, Labels labels, HntmAnalyzerParams analysisParams,
			HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis, String analysisResultsFile) {
		HarmonicAndTransientAnalysisOutput output = analyzeHarmonicAndTransientParts(x, fs, pm, f0WindowSizeInSeconds,
				f0SkipSizeInSeconds, f0Contour, labels, analysisParams);
		analyzeNoisePart(x, output.hnmSignal, analysisParams, synthesisParamsBeforeNoiseAnalysis, output.isInTransientSegments);

		if (analysisResultsFile != null) {
			try {
				output.hnmSignal.write(analysisResultsFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return output.hnmSignal;
	}

	public HarmonicAndTransientAnalysisOutput analyzeHarmonicAndTransientParts(double[] x, int fs, PitchReaderWriter f0,
			Labels labels, HntmAnalyzerParams analysisParams) {
		int pitchMarkOffset = 0;
		PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, x.length, f0.header.windowSizeInSeconds,
				f0.header.skipSizeInSeconds, false, pitchMarkOffset);

		return analyzeHarmonicAndTransientParts(x, fs, pm, f0.header.windowSizeInSeconds, f0.header.skipSizeInSeconds,
				ArrayUtils.copyDouble2Float(f0.contour), labels, analysisParams);
	}

	public HarmonicAndTransientAnalysisOutput analyzeHarmonicAndTransientParts(double[] x, int fs, PitchMarks pm,
			double f0WindowSizeInSeconds, double f0SkipSizeInSeconds, float[] f0Contour, Labels labels,
			HntmAnalyzerParams analysisParams) {
		HarmonicAndTransientAnalysisOutput output = new HarmonicAndTransientAnalysisOutput();
		output.hnmSignal = null;

		float originalDurationInSeconds = SignalProcUtils.sample2time(x.length, fs);
		int lpOrder = SignalProcUtils.getLPOrder(fs);

		int i, j, k;

		// // TO DO
		// Step1. Initial pitch estimation: Current version just reads from a file
		if (pm != null) {
			// FileUtils.writeTextFile(pm.pitchMarks, "d:\\pm.txt");
			float[] initialF0s = ArrayUtils.subarray(f0Contour, 0, f0Contour.length);
			// float[] initialF0s = HnmPitchVoicingAnalyzer.estimateInitialPitch(x, samplingRate, windowSizeInSeconds,
			// skipSizeInSeconds, f0MinInHz, f0MaxInHz, windowType);
			//

			// Step2: Do for each frame (at 10 ms skip rate):
			// 2.a. Voiced/Unvoiced decision

			// 2.b. If voiced, maximum frequency of voicing estimation
			// Otherwise, maximum frequency of voicing is set to 0.0
			analysisParams.hnmPitchVoicingAnalyzerParams.f0AnalysisWindowSizeInSeconds = (float) f0WindowSizeInSeconds;
			analysisParams.hnmPitchVoicingAnalyzerParams.f0AnalysisSkipSizeInSeconds = (float) f0SkipSizeInSeconds;
			float[] maxFrequencyOfVoicings = HnmPitchVoicingAnalyzer.analyzeVoicings(x, fs, initialF0s,
					analysisParams.hnmPitchVoicingAnalyzerParams, analysisParams.isSilentAnalysis);
			float maxFreqOfVoicingInHz;
			// maxFreqOfVoicingInHz = HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST; //This should come from the above
			// automatic analysis

			// 2.c. Refined pitch estimation
			float[] f0s = ArrayUtils.subarray(f0Contour, 0, f0Contour.length);
			// float[] f0s = HnmPitchVoicingAnalyzer.estimateRefinedPitch(fftSize, fs, leftNeighInHz, rightNeighInHz,
			// searchStepInHz, initialF0s, maxFrequencyOfVoicings);
			// //

			// Step3. Determine analysis time instants based on refined pitch values.
			// (Pitch synchronous if voiced, 10 ms skip if unvoiced)
			double numPeriods = analysisParams.numPeriodsHarmonicsExtraction;

			double f0InHz = f0s[0];
			double T0Double;
			double assumedF0ForUnvoicedInHz = 100.0;
			boolean isVoiced, isNoised;
			if (f0InHz > 10.0)
				isVoiced = true;
			else {
				isVoiced = false;
				f0InHz = assumedF0ForUnvoicedInHz;
			}

			int ws;

			// int totalFrm = (int)Math.floor(pm.pitchMarks.length-numPeriods+0.5);
			int totalFrm = pm.pitchMarks.length - 1;
			if (totalFrm > pm.pitchMarks.length - 1)
				totalFrm = pm.pitchMarks.length - 1;

			// Extract frames and analyze them
			double[] frm = null; // Extracted pitch synchronously
			double[] frmWindowed = null;

			int pmInd = 0;

			boolean isOutputToTextFile = false;
			Window win;
			int closestInd;

			String[] transientPhonemesList = { "p", "t", "k", "pf", "ts", "tS" };

			if (analysisParams.harmonicModel == HntmAnalyzerParams.HARMONICS_PLUS_NOISE)
				output.hnmSignal = new HntmSpeechSignal(totalFrm, fs, originalDurationInSeconds);
			else if (analysisParams.harmonicModel == HntmAnalyzerParams.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE && labels != null)
				output.hnmSignal = new HntmPlusTransientsSpeechSignal(totalFrm, fs, originalDurationInSeconds,
						labels.items.length);

			boolean isPrevVoiced = false;

			int numHarmonics = 0;
			int prevNumHarmonics = 0;
			ComplexNumber[] harmonics = null;
			ComplexNumber[] noiseHarmonics = null;

			double[] phases;
			double[] dPhases;
			double[] dPhasesPrev = null;
			int MValue;

			int maxVoicingIndex;
			int currentLabInd = 0;
			boolean isInTransientSegment = false;
			int transientSegmentInd = 0;
			output.isInTransientSegments = new boolean[totalFrm];
			Arrays.fill(output.isInTransientSegments, false);

			for (i = 0; i < totalFrm; i++) {
				f0InHz = pm.f0s[i];
				// T0 = pm.pitchMarks[i+1]-pm.pitchMarks[i];
				if (f0InHz > 10.0)
					T0Double = SignalProcUtils.time2sampleDouble(1.0 / f0InHz, fs);
				else
					T0Double = SignalProcUtils.time2sampleDouble(1.0 / assumedF0ForUnvoicedInHz, fs);

				ws = (int) Math.floor(numPeriods * T0Double + 0.5);
				// if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
				// ws++;

				output.hnmSignal.frames[i].tAnalysisInSeconds = (((float) pm.pitchMarks[i + 1]) / fs); // Middle of analysis frame

				if (analysisParams.harmonicModel == HntmAnalyzerParams.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE && labels != null) {
					while (labels.items[currentLabInd].time < output.hnmSignal.frames[i].tAnalysisInSeconds) {
						currentLabInd++;
						if (currentLabInd > labels.items.length - 1) {
							currentLabInd = labels.items.length - 1;
							break;
						}
					}

					if (!isInTransientSegment) // Perhaps start of a new transient segment
					{
						for (j = 0; j < transientPhonemesList.length; j++) {
							if (labels.items[currentLabInd].phn.compareTo(transientPhonemesList[j]) == 0) {
								isInTransientSegment = true;
								((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments[transientSegmentInd] = new TransientSegment();
								((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments[transientSegmentInd].startTime = Math
										.max(0.0f, (((float) pm.pitchMarks[i]) / fs)
												- analysisParams.overlapBetweenTransientAndNontransientSectionsInSeconds);
								break;
							}
						}
					} else // Perhaps end of an existing transient segment
					{
						boolean isTransientPhoneme = false;
						for (j = 0; j < transientPhonemesList.length; j++) {
							if (labels.items[currentLabInd].phn.compareTo(transientPhonemesList[j]) == 0) {
								isTransientPhoneme = true;
								break;
							}
						}

						if (!isTransientPhoneme) // End of transient segment, put it in transient part
						{
							float endTime = Math.min((((float) pm.pitchMarks[i] + 0.5f * ws) / fs)
									+ analysisParams.overlapBetweenTransientAndNontransientSectionsInSeconds,
									output.hnmSignal.originalDurationInSeconds);
							int waveformStartInd = Math
									.max(0,
											SignalProcUtils
													.time2sample(
															((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments[transientSegmentInd].startTime,
															fs));
							int waveformEndInd = Math.min(x.length - 1, SignalProcUtils.time2sample(endTime, fs));
							if (waveformEndInd - waveformStartInd + 1 > 0) {
								((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments[transientSegmentInd].waveform = new int[waveformEndInd
										- waveformStartInd + 1];
								for (j = waveformStartInd; j <= waveformEndInd; j++)
									((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments[transientSegmentInd].waveform[j
											- waveformStartInd] = (int) x[j];
							}

							transientSegmentInd++;
							isInTransientSegment = false;
						}
					}
				}

				maxVoicingIndex = SignalProcUtils.time2frameIndex(output.hnmSignal.frames[i].tAnalysisInSeconds,
						analysisParams.hnmPitchVoicingAnalyzerParams.mvfAnalysisWindowSizeInSeconds,
						analysisParams.hnmPitchVoicingAnalyzerParams.mvfAnalysisSkipSizeInSeconds);
				maxVoicingIndex = Math.min(maxVoicingIndex, maxFrequencyOfVoicings.length - 1);
				maxFreqOfVoicingInHz = maxFrequencyOfVoicings[maxVoicingIndex];
				// if (hnmSignal.frames[i].tAnalysisInSeconds<0.7 && f0InHz>10.0)
				if (f0InHz > 10.0)
					output.hnmSignal.frames[i].maximumFrequencyOfVoicingInHz = maxFreqOfVoicingInHz;
				else
					output.hnmSignal.frames[i].maximumFrequencyOfVoicingInHz = 0.0f;

				numHarmonics = (int) Math.floor(output.hnmSignal.frames[i].maximumFrequencyOfVoicingInHz / f0InHz + 0.5);
				isVoiced = numHarmonics > 0 ? true : false;
				isNoised = output.hnmSignal.frames[i].maximumFrequencyOfVoicingInHz < 0.5 * fs ? true : false;

				if (isInTransientSegment) {
					output.hnmSignal.frames[i].h = null;
					output.hnmSignal.frames[i].n = null;
				} else {
					if (!isVoiced) {
						f0InHz = assumedF0ForUnvoicedInHz;

						T0Double = SignalProcUtils.time2sampleDouble(1.0 / f0InHz, fs);

						ws = (int) Math.floor(numPeriods * T0Double + 0.5);
						// if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
						// ws++;

						// output.hnmSignal.frames[i].tAnalysisInSeconds = (float)((pm.pitchMarks[i]+0.5*ws)/fs); //Middle of
						// analysis frame
					}

					frm = new double[ws];
					Arrays.fill(frm, 0.0);
					int frmStartIndex;
					if (i == 0)
						frmStartIndex = 0;
					else
						frmStartIndex = SignalProcUtils.time2sample(output.hnmSignal.frames[i].tAnalysisInSeconds - 0.5
								* numPeriods / f0InHz, fs);
					int frmEndIndex = frmStartIndex + ws - 1;
					// System.out.println(String.valueOf(frmStartIndex) + " " + String.valueOf(frmEndIndex));
					int count = 0;
					for (j = Math.max(0, frmStartIndex); j < Math.min(frmEndIndex, x.length - 1); j++)
						frm[count++] = x[j];

					/*
					 * for (j=pm.pitchMarks[i]; j<Math.min(pm.pitchMarks[i]+ws-1, x.length); j++) frm[j-pm.pitchMarks[i]] = x[j];
					 */

					win = Window.get(analysisParams.harmonicAnalysisWindowType, ws);
					win.normalizePeakValue(1.0f);
					double[] wgt = win.getCoeffs();
					// double[] wgtSquared = new double[wgt.length];
					// for (j=0; j<wgt.length; j++)
					// wgtSquared[j] = wgt[j]*wgt[j];

					// Step4. Estimate complex amplitudes of harmonics if voiced
					// The phase of the complex amplitude is the phase and its magnitude is the absolute amplitude of the harmonic
					if (isVoiced) {
						// Time-domain full cross-correlation, i.e. harmonics are correlated
						if (!analysisParams.useJampackInAnalysis)
							harmonics = estimateComplexAmplitudes(frm, wgt, f0InHz, numHarmonics, fs,
									analysisParams.hnmPitchVoicingAnalyzerParams.lastCorrelatedHarmonicNeighbour);
						else {
							try {
								harmonics = estimateComplexAmplitudesJampack(frm, wgt, f0InHz, numHarmonics, fs,
										analysisParams.hnmPitchVoicingAnalyzerParams.lastCorrelatedHarmonicNeighbour);
							} catch (JampackException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						// harmonics = estimateComplexAmplitudesTD(frm, f0InHz, numHarmonics,fs);
						// harmonics = estimateComplexAmplitudesSplitOptimization(frm, wgt, f0InHz, numHarmonics, fs);
						// harmonicAmps = estimateComplexAmplitudesUncorrelated(frm, wgtSquared, numHarmonics, f0InHz, fs);

						numHarmonics = harmonics.length;

						// Only for visualization
						// double[] absMags = MathUtils.magnitudeComplex(harmonicAmps);
						// double[] dbMags = MathUtils.amp2db(absMags);
						// MaryUtils.plot(dbMags);
						//

						output.hnmSignal.frames[i].f0InHz = (float) f0InHz;
						output.hnmSignal.frames[i].h = new FrameHarmonicPart();
					} else {
						output.hnmSignal.frames[i].f0InHz = 0.0f;
						numHarmonics = 0;
					}

					output.hnmSignal.frames[i].n = null;

					// Step6. Estimate amplitude envelopes
					if (numHarmonics > 0) {
						if (isVoiced) {
							frmWindowed = win.apply(frm, 0);
							LpCoeffs lpcs = LpcAnalyser.calcLPC(frmWindowed, lpOrder, 0.0f);
							output.hnmSignal.frames[i].n = new FrameNoisePartLpc();
							((FrameNoisePartLpc) output.hnmSignal.frames[i].n).setLpCoeffs(lpcs.getA(), (float) lpcs.getGain());

							/*
							 * //Only for display purposes double[] envelope = new
							 * double[SignalProcUtils.halfSpectrumSize(fftSize)]; for (int ff=0; ff<envelope.length; ff++) {
							 * envelope[ff] = LpcAnalyser.calcSpecValLinear(hnmSignal.frames[i].lpCoeffs,
							 * hnmSignal.frames[i].lpGain, SignalProcUtils.index2freq(ff, fs, envelope.length-1), fs); }
							 * MaryUtils.plot(MathUtils.linear2db(envelope)); SignalProcUtils.displayDFTSpectrumInDB(frm,
							 * fftSize); MaryUtils.plot(MathUtils.linear2db(linearAmps)); //
							 */

							output.hnmSignal.frames[i].h.complexAmps = ArrayUtils.copy(harmonics);

							if (i == 10) {
								// The following is only for visualization
								int fftSize = SignalProcUtils.getDFTSize(fs);
								double[] frameDftDB = MathUtils.amp2db(SignalProcUtils
										.getFrameHalfMagnitudeSpectrum(frm, fftSize));
								double[] dbAmps = output.hnmSignal.frames[i].h.getDBAmps();
								double[] vocalTractDB = RegularizedPreWarpedCepstrumEstimator.cepstrum2dbSpectrumValues(
										output.hnmSignal.frames[i].h.getCeps(output.hnmSignal.frames[i].f0InHz, fs,
												analysisParams), SignalProcUtils.halfSpectrumSize(fftSize) - 1, fs);
								// FileUtils.toTextFile(freqsInHz, "d:\\freqsInHz.txt");
								// FileUtils.toTextFile(frameDftDB, "d:\\frameDftDB.txt");
								// FileUtils.toTextFile(dbAmps, "d:\\dbAmps.txt");
								// FileUtils.toTextFile(vocalTractDB, "d:\\vocalTractDB.txt");
							}
							//
						}
						//
					}
				}

				if (isVoiced && !isInTransientSegment)
					isPrevVoiced = true;
				else {
					prevNumHarmonics = 0;
					isPrevVoiced = false;
				}

				output.isInTransientSegments[i] = isInTransientSegment;

				if (!analysisParams.isSilentAnalysis) {
					if (analysisParams.harmonicModel == HntmAnalyzerParams.HARMONICS_PLUS_NOISE)
						System.out.println("Harmonic analysis completed at "
								+ String.valueOf(output.hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame "
								+ String.valueOf(i + 1) + " of " + String.valueOf(totalFrm));
					else if (analysisParams.harmonicModel == HntmAnalyzerParams.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
						System.out.println("Harmonic and transient analysis completed at "
								+ String.valueOf(output.hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame "
								+ String.valueOf(i + 1) + " of " + String.valueOf(totalFrm));
				}
			}

			// Set delta times
			for (i = 0; i < output.hnmSignal.frames.length; i++) {
				if (i == 0)
					output.hnmSignal.frames[i].deltaAnalysisTimeInSeconds = output.hnmSignal.frames[i].tAnalysisInSeconds;
				else if (i == output.hnmSignal.frames.length - 1)
					output.hnmSignal.frames[i].deltaAnalysisTimeInSeconds = output.hnmSignal.originalDurationInSeconds
							- output.hnmSignal.frames[i].tAnalysisInSeconds;
				else
					output.hnmSignal.frames[i].deltaAnalysisTimeInSeconds = output.hnmSignal.frames[i + 1].tAnalysisInSeconds
							- output.hnmSignal.frames[i].tAnalysisInSeconds;
			}
		}

		if (output.hnmSignal instanceof HntmPlusTransientsSpeechSignal) {
			int numTransientSegments = 0;
			for (i = 0; i < ((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments.length; i++) {
				if (((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments[i] != null)
					numTransientSegments++;
			}

			if (numTransientSegments > 0) {
				TransientPart tempPart = new TransientPart(numTransientSegments);
				int count = 0;
				for (i = 0; i < ((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments.length; i++) {
					if (((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments[i] != null) {
						tempPart.segments[count++] = new TransientSegment(
								((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients.segments[i]);
						if (count >= numTransientSegments)
							break;
					}
				}

				((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients = new TransientPart(tempPart);
			} else
				((HntmPlusTransientsSpeechSignal) output.hnmSignal).transients = null;
		}

		// NOT EFFECTIVE SINCE we do not use the newPhases!
		// Synthesis uses complexAmps only!
		// Phase envelope estimation and unwrapping to ensure phase continuity in frequency domain
		double[][] modifiedPhases = null;
		if (HntmAnalyzerParams.UNWRAP_PHASES_ALONG_HARMONICS_AFTER_ANALYSIS)
			modifiedPhases = unwrapPhasesAlongHarmonics(output.hnmSignal);
		//

		return output;
	}

	public void analyzeNoisePart(double[] originalSignal, HntmSpeechSignal hnmSignal, HntmAnalyzerParams analysisParams,
			HntmSynthesizerParams synthesisParamsForNoiseAnalysis, boolean[] isInTransientSegments) {
		// Re-synthesize harmonic and transient parts, obtain noise waveform by simple subtraction
		HntmSynthesizedSignal s = new HntmSynthesizedSignal();

		// The following lines enable direct amplitudes to be used when synthesizing harmonic part to estimate the noise part
		// and makes this estimation independent of actual harmonic part amplitude representation used in synthesis and
		// modifications
		boolean useHarmonicAmplitudesDirectlyTemp = analysisParams.useHarmonicAmplitudesDirectly;
		if (!analysisParams.useHarmonicAmplitudesDirectly)
			analysisParams.useHarmonicAmplitudesDirectly = true;
		//

		if (analysisParams.harmonicSynthesisMethodBeforeNoiseAnalysis == HntmSynthesizerParams.LINEAR_PHASE_INTERPOLATION) {
			// s.harmonicPart = HarmonicPartLinearPhaseInterpolatorSynthesizer.synthesize(hnmSignal, analysisParams,
			// synthesisParamsForNoiseAnalysis);
			HarmonicPartLinearPhaseInterpolatorSynthesizer hs = new HarmonicPartLinearPhaseInterpolatorSynthesizer(hnmSignal,
					analysisParams, synthesisParamsForNoiseAnalysis);
			s.harmonicPart = hs.synthesizeAll();
		} else if (analysisParams.harmonicSynthesisMethodBeforeNoiseAnalysis == HntmSynthesizerParams.CUBIC_PHASE_INTERPOLATION) {
			// Convert to pure sinusoidal tracks
			SinusoidalTracks st = HarmonicsToTrackConverter.convert(hnmSignal, analysisParams);
			//

			PeakMatchedSinusoidalSynthesizer ss = new PeakMatchedSinusoidalSynthesizer(hnmSignal.samplingRateInHz);
			s.harmonicPart = ss.synthesize(st, true);
		}

		analysisParams.useHarmonicAmplitudesDirectly = useHarmonicAmplitudesDirectlyTemp;

		double[] xHarmTransResynth = SignalProcUtils.addSignals(s.harmonicPart, s.transientPart);
		double[] xDiff = SignalProcUtils.addSignals(originalSignal, 1.0, xHarmTransResynth, -1.0);

		float originalDurationInSeconds = SignalProcUtils.sample2time(xDiff.length, hnmSignal.samplingRateInHz);
		int lpOrder;
		if (analysisParams.computeNoisePartLpOrderFromSamplingRate)
			lpOrder = SignalProcUtils.getLPOrder(hnmSignal.samplingRateInHz);
		else
			lpOrder = analysisParams.noisePartLpOrder;

		// Only effective for FREQUENCY_DOMAIN_PEAK_PICKING_HARMONICS_ANALYSIS
		SinusoidalAnalysisParams sinAnaParams = null;
		boolean bRefinePeakEstimatesParabola = false;
		boolean bRefinePeakEstimatesBias = false;
		boolean bSpectralReassignment = false;
		boolean bAdjustNeighFreqDependent = false;
		//

		int i, j, k;

		double[] xPreemphasized = null;
		if (analysisParams.preemphasisCoefNoise > 0.0)
			xPreemphasized = SignalProcUtils.applyPreemphasis(xDiff, analysisParams.preemphasisCoefNoise);
		else
			xPreemphasized = ArrayUtils.copy(xDiff);

		// // TO DO
		// Step1. Initial pitch estimation: Current version just reads from a file
		if (hnmSignal != null) {
			// Step3. Determine analysis time instants based on refined pitch values.
			// (Pitch synchronous if voiced, 10 ms skip if unvoiced)
			double numPeriods = analysisParams.numPeriodsHarmonicsExtraction;

			double f0InHz = hnmSignal.frames[0].f0InHz;
			double T0Double;
			double assumedF0ForUnvoicedInHz = 100.0;
			boolean isVoiced, isNoised;
			if (f0InHz > 10.0)
				isVoiced = true;
			else {
				isVoiced = false;
				f0InHz = assumedF0ForUnvoicedInHz;
			}

			int wsNoise = SignalProcUtils.time2sample(analysisParams.noiseAnalysisWindowDurationInSeconds,
					hnmSignal.samplingRateInHz);
			if (wsNoise % 2 == 0) // Always use an odd window size to have a zero-phase analysis window
				wsNoise++;

			Window winNoise = Window.get(analysisParams.noiseAnalysisWindowType, wsNoise);
			winNoise.normalizePeakValue(1.0f);
			double[] wgtNoise = winNoise.getCoeffs();
			double[] wgtSquaredNoise = new double[wgtNoise.length];
			for (j = 0; j < wgtNoise.length; j++)
				wgtSquaredNoise[j] = wgtNoise[j] * wgtNoise[j];

			int fftSizeNoise = SignalProcUtils.getDFTSize(hnmSignal.samplingRateInHz);

			int totalFrm = hnmSignal.frames.length;

			// Extract frames and analyze them
			double[] frmNoise = new double[wsNoise]; // Extracted at fixed window size around analysis time instant since LP
														// analysis requires longer windows (40 ms)
			int noiseFrmStartInd;

			boolean isPrevVoiced = false;

			int numHarmonics = 0;
			int prevNumHarmonics = 0;
			ComplexNumber[] noiseHarmonics = null;

			int numNoiseHarmonics = (int) Math.floor((0.5 * hnmSignal.samplingRateInHz) / analysisParams.noiseF0InHz + 0.5);
			double[] freqsInHzNoise = new double[numNoiseHarmonics];
			for (j = 0; j < numNoiseHarmonics; j++)
				freqsInHzNoise[j] = analysisParams.noiseF0InHz * (j + 1);

			double[][] M = null;
			double[][] MTransW = null;
			double[][] MTransWM = null;
			double[][] lambdaR = null;
			double[][] inverted = null;
			if (analysisParams.noiseModel == HntmAnalyzerParams.PSEUDO_HARMONIC) {
				if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
					M = RegularizedPreWarpedCepstrumEstimator.precomputeM(freqsInHzNoise, hnmSignal.samplingRateInHz,
							analysisParams.noisePartCepstrumOrder);
				else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
					M = RegularizedPostWarpedCepstrumEstimator.precomputeM(freqsInHzNoise, hnmSignal.samplingRateInHz,
							analysisParams.noisePartCepstrumOrder);

				if (M != null) {
					MTransW = RegularizedCepstrumEstimator.precomputeMTransW(M, null);
					MTransWM = RegularizedCepstrumEstimator.precomputeMTransWM(MTransW, M);
					lambdaR = RegularizedCepstrumEstimator.precomputeLambdaR(
							analysisParams.regularizedCepstrumEstimationLambdaNoise, analysisParams.noisePartCepstrumOrder);
					inverted = RegularizedCepstrumEstimator.precomputeInverted(MTransWM, lambdaR);
				}
			}

			int waveformNoiseStartInd = 0;
			int waveformNoiseEndInd = 0;

			double[][] frameWaveforms = null;
			if (analysisParams.noiseModel == HntmAnalyzerParams.WAVEFORM
					|| analysisParams.noiseModel == HntmAnalyzerParams.VOICEDNOISE_LPC_UNVOICEDNOISE_WAVEFORM
					|| analysisParams.noiseModel == HntmAnalyzerParams.UNVOICEDNOISE_LPC_VOICEDNOISE_WAVEFORM) {
				frameWaveforms = new double[totalFrm][];
				for (i = 0; i < totalFrm; i++)
					frameWaveforms[i] = null;
			}

			for (i = 0; i < totalFrm; i++) {
				f0InHz = hnmSignal.frames[i].f0InHz;

				if (f0InHz > 10.0) {
					T0Double = SignalProcUtils.time2sampleDouble(1.0 / f0InHz, hnmSignal.samplingRateInHz);
					numHarmonics = (int) Math.floor(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz / f0InHz + 0.5);
				} else {
					T0Double = SignalProcUtils.time2sampleDouble(1.0 / assumedF0ForUnvoicedInHz, hnmSignal.samplingRateInHz);
					numHarmonics = 0;
				}

				isVoiced = numHarmonics > 0 ? true : false;
				isNoised = hnmSignal.frames[i].maximumFrequencyOfVoicingInHz < 0.5 * hnmSignal.samplingRateInHz ? true : false;

				if (i > 0) {
					prevNumHarmonics = (int) Math.floor(hnmSignal.frames[i - 1].maximumFrequencyOfVoicingInHz / f0InHz + 0.5);
					isPrevVoiced = prevNumHarmonics > 0 ? true : false;
				}

				if (!isInTransientSegments[i]) {
					if (!isVoiced)
						f0InHz = assumedF0ForUnvoicedInHz;

					T0Double = SignalProcUtils.time2sampleDouble(1.0 / f0InHz, hnmSignal.samplingRateInHz);

					// Perform full-spectrum LPC analysis for generating noise part
					Arrays.fill(frmNoise, 0.0);
					noiseFrmStartInd = Math.max(
							0,
							SignalProcUtils.time2sample(hnmSignal.frames[i].tAnalysisInSeconds - 0.5f
									* analysisParams.noiseAnalysisWindowDurationInSeconds, hnmSignal.samplingRateInHz));

					for (j = noiseFrmStartInd; j < Math.min(noiseFrmStartInd + wsNoise, xDiff.length); j++)
						frmNoise[j - noiseFrmStartInd] = xPreemphasized[j];

					// hnmSignal.frames[i].harmonicTotalEnergyRatio = 1.0f;

					if (isNoised) {
						double[] y = null;

						if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz
								- analysisParams.overlapBetweenHarmonicAndNoiseRegionsInHz > 0.0f)
							y = SignalProcUtils.fdFilter(frmNoise, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz
									- analysisParams.overlapBetweenHarmonicAndNoiseRegionsInHz,
									0.5f * hnmSignal.samplingRateInHz, hnmSignal.samplingRateInHz, fftSizeNoise);

						if (analysisParams.hpfBeforeNoiseAnalysis && y != null)
							frmNoise = ArrayUtils.copy(y); // Use fdfo only for computing energy ratio between noise and speech
															// (if we get this working, we can remove filtering from above and
															// include only gain ratio computation)

						frmNoise = SignalProcUtils.replaceNaNsWith(frmNoise, 0.0);
						frmNoise = MathUtils.add(frmNoise, MathUtils.random(frmNoise.length, -1.0e-20, 1.0e-20));

						float origAverageSampleEnergy = (float) SignalProcUtils.getAverageSampleEnergy(frmNoise);
						float origNoiseStd = (float) MathUtils.standardDeviation(frmNoise);

						if (analysisParams.noiseModel == HntmAnalyzerParams.LPC
								|| (analysisParams.noiseModel == HntmAnalyzerParams.VOICEDNOISE_LPC_UNVOICEDNOISE_WAVEFORM && isVoiced)
								|| (analysisParams.noiseModel == HntmAnalyzerParams.UNVOICEDNOISE_LPC_VOICEDNOISE_WAVEFORM && !isVoiced)) {
							frmNoise = winNoise.apply(frmNoise, 0);
							LpCoeffs lpcs = LpcAnalyser.calcLPC(frmNoise, lpOrder, 0.0f);
							hnmSignal.frames[i].n = new FrameNoisePartLpc();
							((FrameNoisePartLpc) hnmSignal.frames[i].n).setLpCoeffs(lpcs.getA(), (float) lpcs.getGain());
							// hnmSignal.frames[i].setLpCoeffs(lpcs.getLPRefc(), 1.0f); //Reflection coefficients (Lattice filter
							// required for synthesis!

							((FrameNoisePartLpc) hnmSignal.frames[i].n).origAverageSampleEnergy = origAverageSampleEnergy;
							((FrameNoisePartLpc) hnmSignal.frames[i].n).origNoiseStd = origNoiseStd;

							// Only for display purposes...
							// SignalProcUtils.displayLPSpectrumInDB(((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs,
							// ((FrameNoisePartLpc)hnmSignal.frames[i].n).gain, fftSizeNoise);
						} else if (analysisParams.noiseModel == HntmAnalyzerParams.PSEUDO_HARMONIC) {
							// Note that for noise we use the uncorrelated version of the complex amplitude estimator
							// Correlated version resulted in ill-conditioning
							// Also, analysis was pretty slow since the number of harmonics is large for pseudo-harmonics of
							// noise,
							// i.e. for 16 KHz 5 to 8 KHz bandwidth in steps of 100 Hz produces 50 to 80 pseudo-harmonics

							// (1) Uncorrelated approach as in Stylianou´s thesis
							// noiseHarmonics= estimateComplexAmplitudesUncorrelated(frmNoise, wgtSquaredNoise, numNoiseHarmonics,
							// NOISE_F0_IN_HZ, fs);
							// OR... (2)Expensive approach which does not work very well
							noiseHarmonics = estimateComplexAmplitudes(frmNoise, wgtNoise, analysisParams.noiseF0InHz,
									numNoiseHarmonics, hnmSignal.samplingRateInHz,
									analysisParams.hnmPitchVoicingAnalyzerParams.lastCorrelatedHarmonicNeighbour);
							// OR... (3) Uncorrelated approach using full autocorrelation matrix (checking if there is a problem
							// in estimateComplexAmplitudesUncorrelated
							// noiseHarmonics= estimateComplexAmplitudesUncorrelated2(frmNoise, wgtSquared, numNoiseHarmonics,
							// NOISE_F0_IN_HZ, fs);

							double[] linearAmpsNoise = new double[numNoiseHarmonics];
							for (j = 0; j < numNoiseHarmonics; j++)
								linearAmpsNoise[j] = MathUtils.magnitudeComplex(noiseHarmonics[j]);

							// double[] vocalTractDB = MathUtils.amp2db(linearAmpsNoise);
							// MaryUtils.plot(vocalTractDB);

							hnmSignal.frames[i].n = new FrameNoisePartPseudoHarmonic();

							if (!analysisParams.useNoiseAmplitudesDirectly) {
								double[] noiseWeights = null;
								if (analysisParams.useWeightingInRegularizedCesptrumEstimationNoise) {
									GaussWindow g = new GaussWindow(2 * linearAmpsNoise.length);
									g.normalizeRange(0.1f, 1.0f);
									noiseWeights = g.getCoeffsRightHalf();

									if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
										((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps = RegularizedPreWarpedCepstrumEstimator
												.freqsLinearAmps2cepstrum(linearAmpsNoise, freqsInHzNoise,
														hnmSignal.samplingRateInHz, analysisParams.noisePartCepstrumOrder,
														noiseWeights, analysisParams.regularizedCepstrumEstimationLambdaNoise);
									else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
										((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps = RegularizedPostWarpedCepstrumEstimator
												.freqsLinearAmps2cepstrum(linearAmpsNoise, freqsInHzNoise,
														hnmSignal.samplingRateInHz, analysisParams.noisePartCepstrumOrder,
														noiseWeights, analysisParams.regularizedCepstrumEstimationLambdaNoise);
								} else {
									// (1) This is how amplitudes are represented in Stylianou´s thesis
									((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps = RegularizedCepstrumEstimator
											.freqsLinearAmps2cepstrum(linearAmpsNoise, MTransW, inverted);
								}
							} else
								((FrameNoisePartPseudoHarmonic) hnmSignal.frames[i].n).ceps = ArrayUtils.subarrayDouble2Float(
										linearAmpsNoise, 0, linearAmpsNoise.length); // Use amplitudes directly

							/*
							 * //The following is only for visualization //int fftSize = 4096; //double[] vocalTractDB =
							 * RegularizedPreWarpedCepstrumEstimator
							 * .cepstrum2logAmpHalfSpectrum(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, fftSize,
							 * hnmSignal.samplingRateInHz); double[] vocalTractDB = new double[numNoiseHarmonics]; for (j=0;
							 * j<numNoiseHarmonics; j++) vocalTractDB[j] =
							 * RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue
							 * (((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, (j+1)*HnmAnalyzer.NOISE_F0_IN_HZ,
							 * hnmSignal.samplingRateInHz); vocalTractDB = MathUtils.amp2db(vocalTractDB);
							 * MaryUtils.plot(vocalTractDB); //
							 */
						} else if (analysisParams.noiseModel == HntmAnalyzerParams.WAVEFORM
								|| (analysisParams.noiseModel == HntmAnalyzerParams.VOICEDNOISE_LPC_UNVOICEDNOISE_WAVEFORM && !isVoiced)
								|| (analysisParams.noiseModel == HntmAnalyzerParams.UNVOICEDNOISE_LPC_VOICEDNOISE_WAVEFORM && isVoiced)) {
							if (i < totalFrm - 1)
								waveformNoiseEndInd = Math.max(0, SignalProcUtils.time2sample(
										hnmSignal.frames[i + 1].tAnalysisInSeconds, hnmSignal.samplingRateInHz));
							else
								waveformNoiseEndInd = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds,
										hnmSignal.samplingRateInHz);

							frameWaveforms[i] = ArrayUtils.copy(frmNoise);

							if (!analysisParams.overlapNoiseWaveformModel)
								frameWaveforms[i] = ArrayUtils.subarray(frameWaveforms[i], 0, waveformNoiseEndInd
										- waveformNoiseStartInd + 1);

							if (isVoiced && analysisParams.hpfBeforeNoiseAnalysis && analysisParams.decimateNoiseWaveform)
								frameWaveforms[i] = SignalProcUtils.decimate(frameWaveforms[i], 0.5 * hnmSignal.samplingRateInHz
										/ (0.5 * hnmSignal.samplingRateInHz - hnmSignal.frames[i].maximumFrequencyOfVoicingInHz));

							waveformNoiseStartInd = waveformNoiseEndInd + 1;
						}
					} else
						hnmSignal.frames[i].n = null;
					//
				}

				if (!analysisParams.isSilentAnalysis)
					System.out.println("Noise analysis completed at " + String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds)
							+ "s. for frame " + String.valueOf(i + 1) + " of " + String.valueOf(totalFrm));
			}

			if (analysisParams.noiseModel == HntmAnalyzerParams.WAVEFORM
					|| analysisParams.noiseModel == HntmAnalyzerParams.VOICEDNOISE_LPC_UNVOICEDNOISE_WAVEFORM
					|| analysisParams.noiseModel == HntmAnalyzerParams.UNVOICEDNOISE_LPC_VOICEDNOISE_WAVEFORM) {
				// Synthesize the noise waveform from overlapping waveform frames
				double[] noisePartWaveform = HntmAnalyzerNoisePartWaveformSynthesizer.synthesize(hnmSignal, frameWaveforms,
						analysisParams, synthesisParamsForNoiseAnalysis);
				packNoisePartWaveforms(hnmSignal, noisePartWaveform);
			}
		}
	}

	// Pack the noise part waveform in non-overlapping chunks in hnmSignal
	public static void packNoisePartWaveforms(HntmSpeechSignal hnmSignal, double[] noisePartWaveform) {
		int frameStartInd = 0;
		int frameEndInd;
		double[] frameWaveform = null;
		for (int i = 0; i < hnmSignal.frames.length; i++) {
			if (i < hnmSignal.frames.length - 1)
				frameEndInd = SignalProcUtils.time2sample(hnmSignal.frames[i].tAnalysisInSeconds, hnmSignal.samplingRateInHz);
			else
				frameEndInd = noisePartWaveform.length - 1;

			frameWaveform = ArrayUtils.subarray(noisePartWaveform, frameStartInd, frameEndInd - frameStartInd + 1);

			hnmSignal.frames[i].n = new FrameNoisePartWaveform(frameWaveform);

			frameStartInd = frameEndInd + 1;
		}
	}

	public ComplexNumber[] estimateComplexAmplitudes(double[] s, double[] wgt, double f0InHz, int L, double samplingRateInHz,
			int lastCorrelatedHarmonicNeighbour) {
		int t, i, k;

		ComplexNumber[] xpart = null;

		double harmonicSample;
		double noiseSample;

		int M = s.length;
		// assert M % 2==1; //Frame length should be odd
		int N;

		double tShift;
		if (M % 2 == 1) {
			N = (M - 1) / 2;
			tShift = 0.0;
		} else {
			N = M / 2;
			tShift = 0.5 / samplingRateInHz;
		}

		ComplexNumber[][] B = new ComplexNumber[M][2 * L + 1];

		double omega;

		ComplexNumber[][] W = MathUtils.diagonalComplexMatrix(wgt);

		for (k = -L; k <= L; k++) {
			for (t = 0; t < M; t++) {
				omega = MathUtils.TWOPI * k * f0InHz * ((t + tShift) / samplingRateInHz);
				B[t][k + L] = new ComplexNumber((float) Math.cos(omega), (float) Math.sin(omega));
			}
		}

		ComplexNumber[][] BTWTW = MathUtils.matrixProduct(MathUtils.hermitianTranspoze(B), MathUtils.transpoze(W));
		BTWTW = MathUtils.matrixProduct(BTWTW, W);

		ComplexNumber[] b = MathUtils.matrixProduct(BTWTW, s); // MathUtils.multiply(s,2.0));

		ComplexNumber[][] R = MathUtils.matrixProduct(BTWTW, B);

		// Set some R entries equal to zero to neglect interaction between far harmonics
		if (lastCorrelatedHarmonicNeighbour > -1 && lastCorrelatedHarmonicNeighbour < L) {
			for (i = 0; i < 2 * L + 1; i++) {
				for (k = 0; k < 2 * L + 1; k++) {
					if (i > k + lastCorrelatedHarmonicNeighbour || k > i + lastCorrelatedHarmonicNeighbour)
						R[i][k] = new ComplexNumber(0.0f, 0.0f);
				}
			}
		}
		//

		/*
		 * //Use matrix inversion ComplexNumber[][] invR = MathUtils.inverse(R); ComplexNumber[] x =
		 * MathUtils.matrixProduct(invR,MathUtils.multiplyComplex(b, 1.0));
		 */

		// Use generalized Levinson
		ComplexNumber[] r = new ComplexNumber[R.length];
		for (i = 0; i < R.length; i++)
			r[i] = new ComplexNumber(R[i][0]);

		// FileUtils.toTextFile(R, "d:/string_Rall.txt");
		// FileUtils.toTextFile(r, "d:/string_r.txt");
		// FileUtils.toTextFile(b, "d:/string_b.txt");

		ComplexNumber[] x = MathUtils.levinson(r, MathUtils.multiplyComplex(b, 1.0));

		// FileUtils.toTextFile(x, "d:/string_x.txt");

		xpart = new ComplexNumber[L];

		for (k = L + 1; k <= 2 * L; k++)
			xpart[k - (L + 1)] = new ComplexNumber(2.0f * x[k].real, 2.0f * x[k].imag);

		// MaryUtils.plot(MathUtils.amp2db(MathUtils.magnitudeComplex(xpart)));

		// Display
		// MaryUtils.plot(MathUtils.amp2db(MathUtils.magnitudeComplex(x)));
		// MaryUtils.plot(MathUtils.amp2db(MathUtils.magnitudeComplex(xpart)));
		//

		// StringUtils.toTextFile(MathUtils.phaseInRadians(xpart), "d:\\hamming.txt");

		return xpart;
	}

	public ComplexNumber[] estimateComplexAmplitudesJampack(double[] frm, double[] wgt, double f0InHz, int L,
			double samplingRateInHz, int lastCorrelatedHarmonicNeighbour) throws JampackException {
		if (Parameters.getBaseIndex() != 0)
			Parameters.setBaseIndex(0);

		int t, i, k;

		ComplexNumber[] xpart = null;

		double harmonicSample;
		double noiseSample;

		int M = frm.length;
		// assert M % 2==1; //Frame length should be odd
		int N;

		double tShift;
		if (M % 2 == 1) {
			N = (M - 1) / 2;
			tShift = 0.0;
		} else {
			N = M / 2;
			tShift = 0.5 / samplingRateInHz;
		}

		Zmat B = new Zmat(M, 2 * L + 1);

		double omega;

		Zdiagmat W = new Zdiagmat(wgt.length);
		for (i = 0; i < wgt.length; i++)
			W.put(i, new Z(wgt[i], 0.0));

		for (k = -L; k <= L; k++) {
			for (t = 0; t < M; t++) {
				omega = MathUtils.TWOPI * k * f0InHz * ((t + tShift) / samplingRateInHz);
				B.put(t, k + L, new Z(Math.cos(omega), Math.sin(omega)));
			}
		}

		Zmat s = new Zmat(frm.length, 1);
		for (i = 0; i < frm.length; i++)
			s.put(i, 0, new Z(frm[i], 0.0));

		Zmat BT = H.o(B);
		Zmat BTWTW = Times.o(BT, W);
		BTWTW = Times.o(BTWTW, W);
		Zmat b = Times.o(BTWTW, s);
		Zmat R = Times.o(BTWTW, B);

		// Set some R entries equal to zero to neglect interaction between far harmonics
		if (lastCorrelatedHarmonicNeighbour > -1 && lastCorrelatedHarmonicNeighbour < L) {
			for (i = 0; i < 2 * L + 1; i++) {
				for (k = 0; k < 2 * L + 1; k++) {
					if (i > k + lastCorrelatedHarmonicNeighbour || k > i + lastCorrelatedHarmonicNeighbour)
						R.put(i, k, new Z(0.0, 0.0));
				}
			}
		}
		//

		// Use matrix inversion
		Zmat invR = Inv.o(R);
		Zmat x = Times.o(invR, b);

		xpart = new ComplexNumber[L];

		for (k = L + 1; k <= 2 * L; k++)
			xpart[k - (L + 1)] = new ComplexNumber(2.0f * x.get(k, 0).re, 2.0f * x.get(k, 0).im);

		return xpart;
	}

	public ComplexNumber[] estimateComplexAmplitudesTD(double[] x, double f0InHz, int L, double samplingRateInHz) {
		int N = x.length;
		double[][] Q = new double[N][2 * L];

		double w0InRadians = SignalProcUtils.hz2radian(f0InHz, (int) samplingRateInHz);
		int i, j;
		for (i = 0; i < N; i++) {
			for (j = 1; j <= L; j++)
				Q[i][j - 1] = Math.cos(i * j * w0InRadians);

			for (j = L + 1; j <= 2 * L; j++)
				Q[i][j - 1] = Math.sin(i * (j - L) * w0InRadians);
		}

		double[][] QT = MathUtils.transpoze(Q);
		double[][] QTQInv = MathUtils.inverse(MathUtils.matrixProduct(QT, Q));
		double[] hopt = MathUtils.matrixProduct(MathUtils.matrixProduct(QTQInv, QT), x);

		ComplexNumber[] xpart = new ComplexNumber[L];
		for (i = 0; i < L; i++)
			xpart[i] = new ComplexNumber((float) hopt[i], (float) hopt[i + L]);

		return xpart;
	}

	// Complex amplitude estimation for harmonics in time domain (Diagonal correlation matrix approach, harmonics assumed
	// independent)
	// The main advantage is the operation being in time domain.
	// Therefore, we can use window sizes as short as two pitch periods and track rapid changes in amplitudes and phases
	// N: local pitch period in samples
	// wgtSquared: window weights squared
	// frm: speech frame to be analysed (its length should be 2*N+1)
	// Uses Equation 3.32 in Stylianou`s thesis
	// This requires harmonics to be uncorrelated.
	// We use this for estimating pseudo-harmonic amplitudes of the noise part.
	// Note that this function is equivalent to peak-picking in the frequency domain in Quatieri´s sinusoidal framework.
	public ComplexNumber[] estimateComplexAmplitudesUncorrelated(double[] frm, double[] wgtSquared, int L, double f0InHz,
			double samplingRateInHz) {
		int M = frm.length;
		int N;
		double tShift;
		if (M % 2 == 1) {
			N = (M - 1) / 2;
			tShift = 0.0;
		} else {
			N = M / 2;
			tShift = 0.5 / samplingRateInHz;
		}

		ComplexNumber tmp;

		int t, k;
		double omega;

		double denum = 0.0;
		for (t = 0; t < M; t++)
			denum += wgtSquared[t];

		ComplexNumber[] Ak = new ComplexNumber[L];
		for (k = 1; k <= L; k++) {
			Ak[k - 1] = new ComplexNumber(0.0f, 0.0f);
			for (t = 0; t < M; t++) {
				omega = -1.0 * MathUtils.TWOPI * k * f0InHz * ((double) (t + tShift) / samplingRateInHz);
				tmp = new ComplexNumber((float) (wgtSquared[t] * frm[t] * Math.cos(omega)),
						(float) (wgtSquared[t] * frm[t] * Math.sin(omega)));
				Ak[k - 1] = MathUtils.addComplex(Ak[k - 1], tmp);
			}
			Ak[k - 1] = MathUtils.divide(Ak[k - 1], denum);
		}

		return Ak;
	}

	public ComplexNumber[] estimateComplexAmplitudesPeakPicking(double[] windowedFrm, int spectralEnvelopeType, boolean isVoiced,
			float f0, float maximumFreqOfVoicingInHz, boolean bEstimateHNMVoicing, SinusoidalAnalysisParams params) {
		int k;
		ComplexNumber[] x = null;
		int numHarmonics = (int) Math.floor(maximumFreqOfVoicingInHz / f0 + 0.5);

		if (numHarmonics > 0) {
			/*
			 * float[] initialPeakLocationsInHz = new float[numHarmonics+1]; initialPeakLocationsInHz[0] = 0.0f; for (int i=1;
			 * i<numHarmonics+1; i++) initialPeakLocationsInHz[i] = i*f0;
			 */

			float[] initialPeakLocationsInHz = new float[numHarmonics];
			for (int i = 0; i < numHarmonics; i++)
				initialPeakLocationsInHz[i] = (i + 1) * f0;

			NonharmonicSinusoidalSpeechFrame nhs = SinusoidalAnalyzer.analyze_frame(windowedFrm, false, spectralEnvelopeType,
					isVoiced, f0, maximumFreqOfVoicingInHz, bEstimateHNMVoicing, params, initialPeakLocationsInHz);

			x = new ComplexNumber[nhs.sinusoids.length];
			for (int i = 0; i < nhs.sinusoids.length; i++)
				x[i] = MathUtils.ampPhase2ComplexNumber(nhs.sinusoids[i].amp, nhs.sinusoids[i].phase);
		}

		return x;
	}

	// This is an implementation of the harmonics parameter estimation algorithm
	// described in Stylianou`s PhD Thesis, Appendix A
	public ComplexNumber[] estimateComplexAmplitudesSplitOptimization(double[] x, double[] w, double f0InHz, int L,
			double samplingRateInHz) {
		int M = x.length;
		if (M % 2 != 1) {
			System.out.println("Error! Frame length should be odd...");
			return null;
		}
		int N = (M - 1) / 2;

		double w0InRadians = SignalProcUtils.hz2radian(f0InHz, (int) samplingRateInHz);
		double[][] W = MathUtils.diagonalMatrix(w);
		double[][] B = new double[M][2 * L + 1];
		int i, j;
		for (i = 1; i <= L; i++) {
			for (j = -N; j <= N; j++) {
				B[j + N][2 * (i - 1)] = Math.cos(i * j * w0InRadians);
				B[j + N][2 * (i - 1) + 1] = Math.sin(i * j * w0InRadians);
			}
		}

		for (j = -N; j <= N; j++)
			B[j + N][2 * L] = 1.0;

		double[][] BTWT = MathUtils.matrixProduct(MathUtils.transpoze(B), MathUtils.transpoze(W));
		double[] Ws = MathUtils.matrixProduct(W, x);
		double[] b = MathUtils.matrixProduct(BTWT, Ws);

		double[][] Aodd = new double[L + 1][L + 1];
		double[][] Aeven = new double[L][L];
		double[] r = new double[2 * L + 1];
		int n;
		for (i = 0; i <= 2 * L; i++) {
			r[i] = 0.0;
			for (n = 1; n <= N; n++)
				r[i] += w[n + N] * w[n + N] * Math.cos(i * n * w0InRadians);
		}

		for (i = 1; i <= L; i++) {
			for (j = 1; j <= L; j++)
				Aeven[i - 1][j - 1] = r[Math.abs(i - j)] - r[i + j];
		}

		for (i = 1; i <= L; i++) {
			for (j = 1; j <= L; j++)
				Aodd[i - 1][j - 1] = r[Math.abs(i - j)] - r[i + j];
		}

		for (i = 1; i <= L; i++)
			Aodd[i - 1][L] = 2 * r[i] + w[N] * w[N];

		Aodd[L][L] = 2 * r[0] + w[N] * w[N];

		double[] bodd = new double[L + 1];
		double[] beven = new double[L];
		for (i = 0; i < L + 1; i++)
			bodd[i] = b[2 * i];
		for (i = 0; i < L; i++)
			beven[i] = b[2 * i + 1];

		// Direct solutions using matrix inversion
		double[] cSol = MathUtils.matrixProduct(MathUtils.inverse(Aodd), bodd);
		double[] sSol = MathUtils.matrixProduct(MathUtils.inverse(Aeven), beven); //
		//
		// TO DO: We can use Gauss-Seidel method, or Successive Over Relaxation method to solve the two systems of linear
		// equations without matrix inversion
		// We´ll have to convert Matlab codes of these methods into Java...

		// Note that:
		// cSol = [c1 c2 ... sL a0]^T
		// sSol = [s1 s2 ... sL]^T
		// The harmonic amplitudes are {ak} = {a0, sqrt(c1^2+s1^2), sqrt(c2^2+s2^2), ..., sqrt(cL^2+sL^2)
		// and the phases are {phik} = {0.0, -arctan(s1/c1), -arctan(s2/c2), ..., -arctan(sL/cL)
		ComplexNumber[] xpart = null;
		int k;
		xpart = new ComplexNumber[L];
		for (k = 1; k <= L; k++)
			xpart[k - 1] = new ComplexNumber((float) cSol[k - 1], (float) (-1.0 * sSol[k - 1]));

		return xpart;
	}

	// Phase envelope estimation and unwrapping to ensure phase continuity in frequency domain
	public static double[][] unwrapPhasesAlongHarmonics(HntmSpeechSignal hntmSignal) {

		double[] maximumFrequencyOfVoicingsInHz = hntmSignal.getMaximumFrequencyOfVoicings();
		double[][] phases = hntmSignal.getPhasesInRadians();

		double[][] newPhases = null;

		if (phases != null) {
			int i, k;
			int maxNumHarmonics = 0;
			int numHarmonicsCurrentFrame;
			int totalFrames = maximumFrequencyOfVoicingsInHz.length;
			assert phases.length == totalFrames;

			for (i = 0; i < totalFrames; i++) {
				if (maximumFrequencyOfVoicingsInHz[i] > 0.0f && phases[i] != null) {
					numHarmonicsCurrentFrame = phases.length;
					if (numHarmonicsCurrentFrame > maxNumHarmonics)
						maxNumHarmonics = numHarmonicsCurrentFrame;
				}
			}

			double[] dphaseks = new double[maxNumHarmonics];
			Arrays.fill(dphaseks, 0.0f);

			newPhases = new double[phases.length][];
			for (i = 0; i < phases.length; i++) {
				if (phases[i] != null)
					newPhases[i] = new double[phases[i].length];
			}

			boolean isPrevTrackVoiced;
			int Mk;
			for (i = 0; i < totalFrames; i++) {
				if (maximumFrequencyOfVoicingsInHz[i] > 0.0f && phases[i] != null) {
					System.arraycopy(phases[i], 0, newPhases[i], 0, phases[i].length);

					for (k = 1; k < phases[i].length - 1; k++) {
						isPrevTrackVoiced = false;

						if (i > 0 && phases[i - 1] != null && phases[i - 1].length > k)
							isPrevTrackVoiced = true;

						if (!isPrevTrackVoiced) // First voiced frame of a voiced segment
							dphaseks[k - 1] = phases[i][k] - phases[i][k - 1];

						Mk = (int) (Math.floor((dphaseks[k - 1] + phases[i][k] - phases[i][k + 1]) / (MathUtils.TWOPI) + 0.5));
						newPhases[i][k + 1] += Mk * MathUtils.TWOPI;

						dphaseks[k] = newPhases[i][k + 1] - phases[i][k];
					}
				}
			}
		}

		return newPhases;
	}
}
