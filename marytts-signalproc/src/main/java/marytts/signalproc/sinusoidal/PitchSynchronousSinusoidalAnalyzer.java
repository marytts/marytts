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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.window.Window;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * A pitch synchronous analyzer for sinusoidal models
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class PitchSynchronousSinusoidalAnalyzer extends SinusoidalAnalyzer {
	public static float DEFAULT_ANALYSIS_PERIODS = 2.5f;

	// fs: Sampling rate in Hz
	// windowType: Type of window (See class Window for details)
	// bRefinePeakEstimatesParabola: Refine peak and frequency estimates by fitting parabolas?
	// bRefinePeakEstimatesBias: Further refine peak and frequency estimates by correcting bias?
	// (Only effective when bRefinePeakEstimatesParabola=true)
	public PitchSynchronousSinusoidalAnalyzer(SinusoidalAnalysisParams paramsIn) {
		super(paramsIn);
	}

	//

	// Pitch synchronous analysis
	public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm) {
		return analyzePitchSynchronous(x, pm, DEFAULT_ANALYSIS_PERIODS, -1.0f);
	}

	// Pitch synchronous analysis
	public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods) {
		return analyzePitchSynchronous(x, pm, numPeriods, -1.0f);
	}

	// Pitch synchronous analysis using a fixed skip size
	public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds) {
		return analyzePitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, SinusoidalAnalysisParams.DEFAULT_DELTA_IN_HZ);
	}

	public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds,
			float deltaInHz) {
		return analyzePitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, deltaInHz, SinusoidalAnalysisParams.LP_SPEC);
	}

	public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds,
			float deltaInHz, int spectralEnvelopeType) {
		return analyzePitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, deltaInHz, spectralEnvelopeType, null);
	}

	/*
	 * Pitch synchronous analysis
	 * 
	 * x: Speech/Audio signal to be analyzed pitchMarks: Integer array of sample indices for pitch period start instants
	 * numPeriods: Number of pitch periods to be used in analysis skipSizeInSeconds: Skip size for fixed skip rate but pitch
	 * synchronous analysis (Enter -1.0f for using adaptive skip rates of one complete pitch periods) deltaInHz: Maximum allowed
	 * frequency deviation when creating sinusoidal tracks spectralEnvelopeType: Spectral envelope estimation method with possible
	 * values NO_SPEC (do not compute spectral envelope) LP_SPEC (linear prediction based envelope) SEEVOC_SPEC (Spectral Envelope
	 * Estimation Vocoder based envelope) REGULARIZED_CEPS (Regularized cepstrum based envelope)
	 */
	public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds,
			float deltaInHz, int spectralEnvelopeType, float[] initialPeakLocationsInHz) {
		NonharmonicSinusoidalSpeechSignal sinSignal = extracSinusoidsPitchSynchronous(x, pm, numPeriods, skipSizeInSeconds,
				deltaInHz, spectralEnvelopeType, initialPeakLocationsInHz);

		// Extract sinusoidal tracks
		TrackGenerator tg = new TrackGenerator();
		SinusoidalTracks sinTracks = tg.generateTracks(sinSignal, deltaInHz, params.fs);

		if (sinTracks != null) {
			sinTracks.getTrackStatistics();
			getGrossStatistics(sinTracks);
		}

		sinTracks.absMaxOriginal = (float) params.absMax;
		sinTracks.totalEnergy = (float) params.totalEnergy;

		// Add post-processing functionality to here

		return sinTracks;
	}

	public NonharmonicSinusoidalSpeechSignal extracSinusoidsPitchSynchronous(double[] x, PitchMarks pm, float numPeriods,
			float skipSizeInSeconds, float deltaInHz) {
		return extracSinusoidsPitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, deltaInHz, SinusoidalAnalysisParams.LP_SPEC);
	}

	public NonharmonicSinusoidalSpeechSignal extracSinusoidsPitchSynchronous(double[] x, PitchMarks pm, float numPeriods,
			float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType) {
		return extracSinusoidsPitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, deltaInHz, SinusoidalAnalysisParams.LP_SPEC,
				null);
	}

	public NonharmonicSinusoidalSpeechSignal extracSinusoidsPitchSynchronous(double[] x, PitchMarks pm, float numPeriods,
			float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType, float[] initialPeakLocationsInHz) {
		params.absMax = MathUtils.getAbsMax(x);
		params.totalEnergy = SignalProcUtils.energy(x);

		boolean bFixedSkipRate = false;
		if (skipSizeInSeconds > 0.0f) // Perform fixed skip rate but pitch synchronous analysis. This is useful for time/pitch
										// scale modification
		{
			params.ss = (int) Math.floor(skipSizeInSeconds * params.fs + 0.5);
			bFixedSkipRate = true;
		}

		int totalFrm;

		if (!bFixedSkipRate) {
			totalFrm = (int) Math.floor(pm.pitchMarks.length - numPeriods + 0.5);
			if (totalFrm > pm.pitchMarks.length - 1)
				totalFrm = pm.pitchMarks.length - 1;
		} else
			totalFrm = (int) (x.length / params.ss + 0.5);

		// Extract frames and analyze them
		double[] frm = null;
		int i, j;
		int T0;

		NonharmonicSinusoidalSpeechSignal sinSignal = new NonharmonicSinusoidalSpeechSignal(totalFrm);
		boolean[] isSinusoidNulls = new boolean[totalFrm];
		Arrays.fill(isSinusoidNulls, false);
		int totalNonNull = 0;

		int pmInd = 0;
		int currentTimeInd = 0;
		float f0;
		float currentTime;
		boolean isOutputToTextFile = false;
		boolean isVoiced;

		for (i = 0; i < totalFrm; i++) {
			if (!bFixedSkipRate) {
				T0 = pm.pitchMarks[i + 1] - pm.pitchMarks[i];
				isVoiced = pm.f0s[i] > 10.0 ? true : false;
				f0 = pm.f0s[i];
			} else {
				while (pm.pitchMarks[pmInd] < currentTimeInd) {
					pmInd++;
					if (pmInd > pm.pitchMarks.length - 1) {
						pmInd = pm.pitchMarks.length - 1;
						break;
					}
				}

				if (pmInd < pm.pitchMarks.length - 1) {
					T0 = pm.pitchMarks[pmInd + 1] - pm.pitchMarks[pmInd];
					isVoiced = pm.f0s[pmInd] > 10.0 ? true : false;
				} else {
					T0 = pm.pitchMarks[pmInd] - pm.pitchMarks[pmInd - 1];
					isVoiced = pm.f0s[pmInd - 1] > 10.0 ? true : false;
				}

				f0 = ((float) params.fs) / T0;
			}

			params.ws = (int) Math.floor(numPeriods * T0 + 0.5);
			if (params.ws % 2 == 0) // Always use an odd window size to have a zero-phase analysis window
				params.ws++;

			// System.out.println("ws=" + String.valueOf(ws) + " minWindowSize=" + String.valueOf(minWindowSize));
			params.ws = Math.max(params.ws, params.minWindowSize);

			frm = new double[params.ws];

			Arrays.fill(frm, 0.0);

			if (!bFixedSkipRate) {
				for (j = pm.pitchMarks[i]; j < Math.min(pm.pitchMarks[i] + params.ws - 1, x.length); j++)
					frm[j - pm.pitchMarks[i]] = x[j];
			} else {
				for (j = currentTimeInd; j < Math.min(currentTimeInd + params.ws - 1, x.length); j++)
					frm[j - currentTimeInd] = x[j];
			}

			params.win = Window.get(params.windowType, params.ws);
			params.win.applyInline(frm, 0, params.ws);

			if (!bFixedSkipRate) {
				// currentTime = (float)(0.5*(pitchMarks[i+1]+pitchMarks[i])/fs);
				currentTime = (float) ((pm.pitchMarks[i] + 0.5f * params.ws) / params.fs); // Middle of analysis frame
			} else {
				// currentTime = (currentTimeInd+0.5f*T0)/fs;
				currentTime = (currentTimeInd + 0.5f * params.ws) / params.fs; // Middle of analysis frame
				currentTimeInd += params.ss;
			}

			/*
			 * if (currentTime>0.500 && currentTime<0.520) isOutputToTextFile = true; else isOutputToTextFile = false;
			 */

			if (initialPeakLocationsInHz == null)
				sinSignal.framesSins[i] = (NonharmonicSinusoidalSpeechFrame) analyze_frame(frm, isOutputToTextFile,
						spectralEnvelopeType, isVoiced, f0, params);
			else
				sinSignal.framesSins[i] = (NonharmonicSinusoidalSpeechFrame) analyze_frame(frm, isOutputToTextFile,
						spectralEnvelopeType, isVoiced, f0,
						initialPeakLocationsInHz[initialPeakLocationsInHz.length - 1] + 50.0f, false, params,
						initialPeakLocationsInHz);

			if (sinSignal.framesSins[i] != null) {
				for (j = 0; j < sinSignal.framesSins[i].sinusoids.length; j++)
					sinSignal.framesSins[i].sinusoids[j].frameIndex = i;
			}

			int peakCount = 0;
			if (sinSignal.framesSins[i] == null)
				isSinusoidNulls[i] = true;
			else {
				isSinusoidNulls[i] = false;
				totalNonNull++;
				peakCount = sinSignal.framesSins[i].sinusoids.length;
			}

			if (sinSignal.framesSins[i] != null)
				sinSignal.framesSins[i].time = currentTime;

			System.out.println("Analysis complete at " + String.valueOf(currentTime) + "s. for frame " + String.valueOf(i + 1)
					+ " of " + String.valueOf(totalFrm) + "(found " + String.valueOf(peakCount) + " peaks)");
		}
		//

		NonharmonicSinusoidalSpeechSignal sinSignal2 = null;
		float[] voicings2 = null;
		if (totalNonNull > 0) {
			// Collect non-null sinusoids only
			sinSignal2 = new NonharmonicSinusoidalSpeechSignal(totalNonNull);
			int ind = 0;
			for (i = 0; i < totalFrm; i++) {
				if (!isSinusoidNulls[i]) {
					sinSignal2.framesSins[ind] = new NonharmonicSinusoidalSpeechFrame(sinSignal.framesSins[i]);

					ind++;
					if (ind > totalNonNull - 1)
						break;
				}
			}
			//

			sinSignal2.originalDurationInSeconds = ((float) x.length) / params.fs;
		}

		return sinSignal2;
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		String strPitchFile = args[0].substring(0, args[0].length() - 4) + ".ptc";
		PitchReaderWriter f0 = new PitchReaderWriter(strPitchFile);
		int pitchMarkOffset = 0;
		PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length,
				f0.header.windowSizeInSeconds, f0.header.skipSizeInSeconds, true, pitchMarkOffset);

		double startFreqInHz = 0.0;
		double endFreqInHz = 0.5 * samplingRate;
		int windowType = Window.HAMMING;
		boolean bRefinePeakEstimatesParabolaIn = true;
		boolean bRefinePeakEstimatesBiasIn = true;
		boolean bSpectralReassignmentIn = true;
		boolean bAdjustNeighFreqDependentIn = true;

		SinusoidalAnalysisParams params = new SinusoidalAnalysisParams(samplingRate, startFreqInHz, endFreqInHz, windowType,
				bRefinePeakEstimatesParabolaIn, bRefinePeakEstimatesBiasIn, bSpectralReassignmentIn, bAdjustNeighFreqDependentIn);

		PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(params);

		SinusoidalTracks st = sa.analyzePitchSynchronous(x, pm);
	}
}
