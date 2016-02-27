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

import marytts.signalproc.analysis.CepstrumSpeechAnalyser;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPostWarpedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPreWarpedCepstrumEstimator;
import marytts.signalproc.analysis.SeevocAnalyser;
import marytts.signalproc.analysis.SpectrumWithPeakIndices;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.HnmPitchVoicingAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.VoicingAnalysisOutputData;
import marytts.signalproc.window.Window;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.io.FileUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Sinusoidal Modeling Analysis Module Given a speech/audio signal, a set of amplitudes, frequencies and phases are estimated on a
 * frame-by-frame basis Then, sinusoids that are close in frequency are grouped together to form sinusoidal tracks Optional
 * amplitude and phase continuity constraints can be employed during track generation The implementation consists of ideas and
 * algorithms from various papers as described in function headers
 * 
 * References: Quatieri, T. F. Discrete-Time Speech Signal Processing: Principles and Practice. Prentice-Hall Inc. 2001. (Chapter
 * 9 – Sinusoidal Analysis/Synthesis)
 * 
 * R.J. McAulay and T.F. Quatieri, "Speech Analysis/Synthesis Based on a Sinusoidal Representation," IEEE Transactions on
 * Acoustics, Speech and Signal Processing, vol. ASSP-34, no. 4, August 1986.
 * 
 * @author Oytun T&uuml;rk
 */
public class SinusoidalAnalyzer extends BaseSinusoidalAnalyzer {

	public SinusoidalAnalysisParams params;

	public SinusoidalAnalyzer(SinusoidalAnalysisParams paramsIn) {
		params = new SinusoidalAnalysisParams(paramsIn);
	}

	//

	// Returns number of neighbouring samples to search for a peak in the spectrum
	// Set default search range for peak detection for different frequency intervals
	// The aim is to eliminate some of the peaks, especially in the high frequency region to reduce phase mismatches at frame
	// boundaries
	// However, we should not set the range to broad as it is required to estimate a peak per 100 Hz on the average theoretically
	// for an accurate representation
	// We simply divide the spectrum into non-overlapping bands using Bark scale
	// Groups of 5-equal Bark ranges are assigned the same peak search range
	// The search ranges are increased as frequency increases, i.e. a higher freq peak candidate needs to be greater than a larger
	// number of neighbours
	// to be selected as a peak
	public static int[] setNeighFreq(int fftSize, boolean bAdjustNeighFreqDependent, float fs) {
		int maxFreq = (int) (Math.floor(0.5 * fftSize + 0.5) + 1);

		int[] freqSampNeighs = new int[maxFreq];

		int i;
		if (!bAdjustNeighFreqDependent) {
			for (i = 0; i < maxFreq; i++)
				freqSampNeighs[i] = SinusoidalAnalysisParams.DEFAULT_FREQ_SAMP_NEIGHS_LOW;
		} else {
			float freq;
			int[] vals = new int[6];
			float maxSeparationInHz = 100.0f; // Maximum average separation allowed for peaks of noise
			for (i = 0; i < vals.length; i++)
				vals[i] = Math.min(i + 1, (int) Math.floor(0.5 * maxSeparationInHz / fs * fftSize + 0.5));

			for (i = 0; i < maxFreq; i++) {
				freq = ((float) i) / (maxFreq - 1.0f) * 0.5f * fs;
				if (freq < 500.0f)
					freqSampNeighs[i] = vals[0];
				else if (freq < 1270.0f)
					freqSampNeighs[i] = vals[1];
				else if (freq < 2700.0f)
					freqSampNeighs[i] = vals[2];
				else if (freq < 6400.0f)
					freqSampNeighs[i] = vals[3];
				else if (freq < 15500.0f)
					freqSampNeighs[i] = vals[4];
				else
					freqSampNeighs[i] = vals[5];
			}
		}

		return freqSampNeighs;
	}

	//

	/*
	 * Fixed rate analysis
	 * 
	 * x: Speech/Audio signal to be analyzed
	 */
	public SinusoidalTracks analyzeFixedRate(double[] x) {
		return analyzeFixedRate(x, SinusoidalAnalysisParams.DEFAULT_ANALYSIS_WINDOW_SIZE);
	}

	/*
	 * Fixed rate analysis
	 * 
	 * x: Speech/Audio signal to be analyzed winSizeInSeconds: Integer array of sample indices for pitch period start instants
	 */
	public SinusoidalTracks analyzeFixedRate(double[] x, float winSizeInSeconds) {
		return analyzeFixedRate(x, winSizeInSeconds, SinusoidalAnalysisParams.DEFAULT_ANALYSIS_SKIP_SIZE);
	}

	/*
	 * Fixed rate analysis
	 * 
	 * x: Speech/Audio signal to be analyzed winSizeInSeconds: Integer array of sample indices for pitch period start instants
	 * skipSizeInSeconds: Number of pitch periods to be used in analysis
	 */
	public SinusoidalTracks analyzeFixedRate(double[] x, float winSizeInSeconds, float skipSizeInSeconds) {
		return analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, SinusoidalAnalysisParams.DEFAULT_DELTA_IN_HZ);
	}

	/*
	 * Fixed rate analysis
	 * 
	 * x: Speech/Audio signal to be analyzed winSizeInSeconds: Integer array of sample indices for pitch period start instants
	 * skipSizeInSeconds: Number of pitch periods to be used in analysis deltaInHz: Maximum allowed frequency deviance when
	 * creating sinusoidal tracks
	 */
	public SinusoidalTracks analyzeFixedRate(double[] x, float winSizeInSeconds, float skipSizeInSeconds, float deltaInHz) {
		return analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, SinusoidalAnalysisParams.LP_SPEC);
	}

	/*
	 * Fixed rate analysis
	 * 
	 * x: Speech/Audio signal to be analyzed winSizeInSeconds: Integer array of sample indices for pitch period start instants
	 * skipSizeInSeconds: Number of pitch periods to be used in analysis deltaInHz: Maximum allowed frequency deviance when
	 * creating sinusoidal tracks spectralEnvelopeType: Spectral envelope estimation method with possible values NO_SPEC (do not
	 * compute spectral envelope) LP_SPEC (linear prediction based envelope) SEEVOC_SPEC (Spectral Envelope Estimation Vocoder
	 * based envelope) REGULARIZED_CEPS (Regularized cepstrum based envelope) See below for details...
	 */
	public SinusoidalTracks analyzeFixedRate(double[] x, float winSizeInSeconds, float skipSizeInSeconds, float deltaInHz,
			int spectralEnvelopeType) {
		return analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, spectralEnvelopeType, null, -1.0f, -1.0f);
	}

	/*
	 * Fixed rate analysis
	 * 
	 * x: Speech/Audio signal to be analyzed winSizeInSeconds: Integer array of sample indices for pitch period start instants
	 * skipSizeInSeconds: Number of pitch periods to be used in analysis deltaInHz: Maximum allowed frequency deviation when
	 * creating sinusoidal tracks spectralEnvelopeType: Spectral envelope estimation method with possible values NO_SPEC (do not
	 * compute spectral envelope) LP_SPEC (linear prediction based envelope) SEEVOC_SPEC (Spectral Envelope Estimation Vocoder
	 * based envelope) REGULARIZED_CEPS (Regularized cepstrum based envelope) f0s: f0 values in Hz (optional, required for SEEVOC
	 * based spectral envelope estimation. If not specified, SEEVOC based estimation will be performed at a fixed f0 value of
	 * 100.0 Hz ws_f0s: Window size in seconds used for f0 extraction (Functional only for SEEVOC based envelope estimation and
	 * when f0s are not null) ss_f0s: Skip size in seconds used for f0 extraction (Functional only for SEEVOC based envelope
	 * estimation and when f0s are not null)
	 */
	public SinusoidalTracks analyzeFixedRate(double[] x, float winSizeInSeconds, float skipSizeInSeconds, float deltaInHz,
			int spectralEnvelopeType, double[] f0s, float ws_f0, float ss_f0) {
		NonharmonicSinusoidalSpeechSignal sinSignal = extractSinusoidsFixedRate(x, winSizeInSeconds, skipSizeInSeconds,
				deltaInHz, spectralEnvelopeType, f0s, ws_f0, ss_f0);

		// Extract sinusoidal tracks
		TrackGenerator tg = new TrackGenerator();
		SinusoidalTracks sinTracks = tg.generateTracks(sinSignal, deltaInHz, params.fs);

		if (sinTracks != null) {
			sinTracks.getTrackStatistics(winSizeInSeconds, skipSizeInSeconds);
			getGrossStatistics(sinTracks);
		}
		//

		sinTracks.absMaxOriginal = (float) params.absMax;
		sinTracks.totalEnergy = (float) params.totalEnergy;

		return sinTracks;
	}

	public NonharmonicSinusoidalSpeechSignal extractSinusoidsFixedRate(double[] x, float winSizeInSeconds,
			float skipSizeInSeconds, float deltaInHz) {
		return extractSinusoidsFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, SinusoidalAnalysisParams.LP_SPEC);
	}

	public NonharmonicSinusoidalSpeechSignal extractSinusoidsFixedRate(double[] x, float winSizeInSeconds,
			float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType) {
		return extractSinusoidsFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, spectralEnvelopeType, null, -1.0f,
				-1.0f);
	}

	// ws_f0: Window size in pitch extraction in seconds
	// ss_f0: Skip size in pitch extraction in seconds
	public NonharmonicSinusoidalSpeechSignal extractSinusoidsFixedRate(double[] x, float winSizeInSeconds,
			float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType, double[] f0s, float ws_f0, float ss_f0) {
		int i, j;
		int f0Ind;
		params.absMax = MathUtils.getAbsMax(x);
		params.totalEnergy = SignalProcUtils.energy(x);

		params.ws = (int) Math.floor(winSizeInSeconds * params.fs + 0.5);
		if (params.ws % 2 == 0) // Always use an odd window size to have a zero-phase analysis window
			params.ws++;

		// System.out.println("ws=" + String.valueOf(params.ws) + " minWindowSize=" + String.valueOf(params.minWindowSize));
		params.ws = Math.max(params.ws, params.minWindowSize);

		params.ss = (int) Math.floor(skipSizeInSeconds * params.fs + 0.5);

		params.win = Window.get(params.windowType, params.ws);
		params.win.normalize(1.0f); // Normalize to sum up to unity

		int totalFrm = (int) ((x.length - 0.5 * params.ws) / params.ss);

		// Extract frames and analyze them
		double[] frm = new double[params.ws];

		NonharmonicSinusoidalSpeechSignal sinSignal = new NonharmonicSinusoidalSpeechSignal(totalFrm);
		boolean[] isSinusoidNulls = new boolean[totalFrm];
		Arrays.fill(isSinusoidNulls, false);
		int totalNonNull = 0;
		int peakCounter;
		float currentTime;
		boolean isOutputToTextFile = false;

		for (i = 0; i < totalFrm; i++) {
			Arrays.fill(frm, 0.0);
			for (j = i * params.ss; j < Math.min(i * params.ss + params.ws, x.length); j++)
				frm[j - i * params.ss] = x[j];

			params.win.applyInline(frm, 0, params.ws);

			currentTime = (float) ((i * params.ss + 0.5 * params.ws) / params.fs); // Middle of analysis frame

			/*
			 * if (currentTime>0.500 && currentTime<0.520) isOutputToTextFile = true; else isOutputToTextFile = false;
			 */

			if (spectralEnvelopeType == SinusoidalAnalysisParams.SEEVOC_SPEC && f0s != null) {
				f0Ind = (int) Math.floor((currentTime - 0.5 * ws_f0) / ss_f0 + 0.5);
				f0Ind = Math.min(f0Ind, f0s.length - 1);
				f0Ind = Math.max(0, f0Ind);
				boolean isVoiced = false;
				if (f0s[f0Ind] > 10.0f)
					isVoiced = true;

				sinSignal.framesSins[i] = analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, isVoiced,
						(float) f0s[f0Ind], params);
			} else
				sinSignal.framesSins[i] = analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, true, params);

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

			if (sinSignal.framesSins[i] != null) {
				sinSignal.framesSins[i].time = currentTime;
				System.out.println("Analysis complete at " + String.valueOf(sinSignal.framesSins[i].time) + "s. for frame "
						+ String.valueOf(i + 1) + " of " + String.valueOf(totalFrm) + "(found " + String.valueOf(peakCount)
						+ " peaks)");
			}
		}
		//

		NonharmonicSinusoidalSpeechSignal sinSignal2 = null;
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

	public static void getGrossStatistics(SinusoidalTracks sinTracks) {
		int totalSins = 0;
		int i, j;
		for (i = 0; i < sinTracks.totalTracks; i++) {
			for (j = 0; j < sinTracks.tracks[i].totalSins; j++) {
				if (sinTracks.tracks[i].states[j] == SinusoidalTrack.ACTIVE)
					totalSins++;
			}
		}

		System.out.println("Total sinusoids to model this file = " + String.valueOf(totalSins));
	}

	public static NonharmonicSinusoidalSpeechFrame analyze_frame(double[] frm, boolean isOutputToTextFile, boolean isVoiced,
			SinusoidalAnalysisParams params) {
		return analyze_frame(frm, isOutputToTextFile, SinusoidalAnalysisParams.LP_SPEC, isVoiced, -1.0f, params);
	}

	public static NonharmonicSinusoidalSpeechFrame analyze_frame(double[] frm, boolean isOutputToTextFile,
			int spectralEnvelopeType, boolean isVoiced, SinusoidalAnalysisParams params) {
		if (spectralEnvelopeType == SinusoidalAnalysisParams.SEEVOC_SPEC)
			return analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, isVoiced, 100.0f, params);
		else
			return analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, isVoiced, -1.0f, params);
	}

	public static NonharmonicSinusoidalSpeechFrame analyze_frame(double[] frm, boolean isOutputToTextFile,
			int spectralEnvelopeType, boolean isVoiced, float f0, SinusoidalAnalysisParams params) {
		return analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, isVoiced, f0, -1.0f, false, params, null);
	}

	// Extract sinusoidal model parameter from a windowed speech frame using the DFT peak-picking algorithm
	// frm: Windowed speech frame
	// spectralEnvelopeType: Desired spectral envelope (See above, i.e LP_SPEC, SEEVOC_SPEC, REGULARIZED_CEPS)
	//
	public static NonharmonicSinusoidalSpeechFrame analyze_frame(double[] windowedFrm, boolean isOutputToTextFile,
			int spectralEnvelopeType, boolean isVoiced, float f0, float maxFreqOfVoicing, boolean bEstimateHNMVoicing,
			SinusoidalAnalysisParams params, float[] initialPeakLocationsInHz) {
		VoicingAnalysisOutputData vo = null;
		float maxVoicingFreqInHz = 0.0f;
		NonharmonicSinusoidalSpeechFrame frameSins = null;

		if (params.fftSize < windowedFrm.length)
			params.fftSize = windowedFrm.length;

		if (params.fftSize % 2 == 1)
			params.fftSize++;

		int[] freqSampNeighs = setNeighFreq(params.fftSize, params.bAdjustNeighFreqDependent, params.fs);

		int maxFreqInd = SignalProcUtils.halfSpectrumSize(params.fftSize) - 1;
		ComplexArray frameDft = new ComplexArray(params.fftSize);
		int i;

		// Perform circular buffering as described in (Quatieri, 2001) to provide correct phase estimates
		int midPoint = (int) Math.floor(0.5 * windowedFrm.length + 0.5);
		System.arraycopy(windowedFrm, midPoint, frameDft.real, 0, windowedFrm.length - midPoint);
		System.arraycopy(windowedFrm, 0, frameDft.real, params.fftSize - midPoint, midPoint);
		// System.arraycopy(windowedFrm, 0, frameDft.real, 0, windowedFrm.length); //No circular buffer
		//

		// Take windowed frame derivative´s FFT for spectral reassignment
		ComplexArray windowedFrameDerivativeFFT = null;
		if (params.bSpectralReassignment) {
			windowedFrameDerivativeFFT = new ComplexArray(params.fftSize);
			double[] dfrm = new double[windowedFrm.length];

			dfrm[0] = windowedFrm[0];
			for (i = 1; i < windowedFrm.length; i++)
				dfrm[i] = windowedFrm[i] - windowedFrm[i - 1];

			System.arraycopy(dfrm, midPoint, windowedFrameDerivativeFFT.real, 0, dfrm.length - midPoint);
			System.arraycopy(dfrm, 0, windowedFrameDerivativeFFT.real, params.fftSize - midPoint, midPoint);
		}

		// Compute DFT
		if (MathUtils.isPowerOfTwo(params.fftSize))
			FFT.transform(frameDft.real, frameDft.imag, false);
		else
			frameDft = FFTMixedRadix.fftComplex(frameDft);
		//

		// Compute magnitude spectrum in dB as peak frequency estimates tend to be more accurate
		double[] frameDftAbs = MathUtils.abs(frameDft, 0, maxFreqInd);
		double[] frameDftDB = MathUtils.amp2db(frameDftAbs);
		//

		int[] freqIndsLow = null;
		int[] freqIndsHigh = null;
		int[] freqInds = null;
		int[] freqIndsLowest = null;
		FreqIndicesWithAmplitudes fiwa;

		double frmEn = SignalProcUtils.getEnergy(windowedFrm);
		if (frmEn > SinusoidalAnalysisParams.MIN_ENERGY_TH) {
			// The following lines are for testing purposes: Fixed frequency tracks will be created
			// Set bManualPeakPickingTest = true to enable this test.
			// Otherwise, the peaks will be automatically estimated (i.e. normal operation mode)
			// below by int [] freqInds = MathUtils.getExtrema...
			boolean bManualPeakPickingTest = false;
			if (bManualPeakPickingTest) {
				int w;
				int numSins = 0;
				int numNoises = 0;
				float noiseRange1 = 0.0f;
				float noiseRange2 = 0.0f;
				float deltaNoise = 1.0f;

				numSins = 4;
				float[] sinFreqs = new float[numSins];
				sinFreqs[0] = 180.0f;
				sinFreqs[1] = 1580.0f;
				sinFreqs[2] = 2580.0f;
				sinFreqs[3] = 4780.0f;

				/*
				 * noiseRange1=2000.0f; noiseRange2=4000.0f; deltaNoise=50.0f; numNoises = Math.max(1,
				 * (int)Math.floor((noiseRange2-noiseRange1)/deltaNoise+0.5));
				 */

				freqInds = new int[numSins + numNoises];

				for (w = 0; w < numSins; w++)
					freqInds[w] = SignalProcUtils.freq2index(sinFreqs[w], params.fs, maxFreqInd);

				for (w = numSins; w < numSins + numNoises; w++)
					freqInds[w] = SignalProcUtils.freq2index(noiseRange1 + (w - numSins) * deltaNoise, params.fs, maxFreqInd);
			}
			//

			/*
			 * int startInd = 0; int endInd = maxFreq-1; if (startFreq>=0) startInd = SignalProcUtils.freq2index(startFreq, fs,
			 * maxFreq); if (endFreq>=0) endInd = SignalProcUtils.freq2index(endFreq, fs, maxFreq);
			 */

			// Vocal tract magnitude spectrum (linear) & phase analysis
			double[] vocalTractSpec = null;
			// SpectrumWithPeakIndices swpi = SeevocAnalyser.calcSpecEnvelopeLinear(frameDftDB, fs, f0); //Note that frameDftDB is
			// in dB but the computed envelope is returned as linear

			if (spectralEnvelopeType == SinusoidalAnalysisParams.LP_SPEC)
				vocalTractSpec = LpcAnalyser.calcSpecFrameLinear(windowedFrm, params.LPOrder, params.fftSize);
			else if (spectralEnvelopeType == SinusoidalAnalysisParams.SEEVOC_SPEC) {
				SpectrumWithPeakIndices swpi = SeevocAnalyser.calcSpecEnvelopeLinear(frameDftDB, params.fs, f0); // Note that
																													// frameDftDB
																													// is in dB
																													// but the
																													// computed
																													// envelope is
																													// returned as
																													// linear
				vocalTractSpec = swpi.spec;
			} else if (spectralEnvelopeType == SinusoidalAnalysisParams.REGULARIZED_CEPS) {
				SpectrumWithPeakIndices swpi = SeevocAnalyser.calcSpecEnvelopeLinear(frameDftDB, params.fs, f0); // Note that
																													// frameDftDB
																													// is in dB
																													// but the
																													// computed
																													// envelope is
																													// returned as
																													// linear
				int cepsOrderPre = 13;
				int cepsOrder = 19;
				int numPeaks = swpi.indices.length;
				double[] linearAmps = new double[numPeaks];
				double[] freqsInHz = new double[numPeaks];
				for (i = 0; i < numPeaks; i++) {
					linearAmps[i] = frameDftAbs[swpi.indices[i]];
					freqsInHz[i] = SignalProcUtils.index2freq(swpi.indices[i], params.fs, maxFreqInd);
				}

				if (params.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
					vocalTractSpec = RegularizedPreWarpedCepstrumEstimator.spectralEnvelopeLinear(linearAmps, freqsInHz,
							params.fs, cepsOrder, params.fftSize);
				else if (params.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
					vocalTractSpec = RegularizedPostWarpedCepstrumEstimator.spectralEnvelopeLinear(linearAmps, freqsInHz,
							params.fs, cepsOrderPre, cepsOrder, params.fftSize);
			}

			/*
			 * double[] vocalTractSpecDB = MathUtils.amp2db(vocalTractSpec); double[] excSpecAbs = new double[maxFreq]; for (i=0;
			 * i<maxFreq; i++) excSpecAbs[i] = frameDftAbs[i]/vocalTractSpec[i]; double[] excSpecDB =
			 * MathUtils.amp2db(excSpecAbs); MaryUtils.plot(frameDftDB); MaryUtils.plot(vocalTractSpecDB);
			 * MaryUtils.plot(excSpecDB);
			 */

			if (maxFreqOfVoicing < 0.0f) {
				// Use abs dft in db for maximum frequency of voicing estimation
				vo = HnmPitchVoicingAnalyzer.estimateMaxFrequencyOfVoicingsFrame(frameDftDB, params.fs, (float) f0, isVoiced,
						params.hnmPitchVoicingAnalyzerParams);

				maxVoicingFreqInHz = (float) Math.min(vo.maxFreqOfVoicing, SinusoidalAnalysisParams.MAX_VOICED_FREQ_IN_HZ); // From
																															// hnm,
																															// not
																															// working
																															// very
																															// properly
																															// yet
				if (maxVoicingFreqInHz > 0.0f)
					maxVoicingFreqInHz = (float) Math.max(maxVoicingFreqInHz, SinusoidalAnalysisParams.MIN_VOICED_FREQ_IN_HZ); // From
																																// hnm,
																																// not
																																// working
																																// very
																																// properly
																																// yet
			} else
				maxVoicingFreqInHz = maxFreqOfVoicing;

			// maxVoicingFreqInHz = 3600.0f; //manual

			float upperFreqSamplingStepInHz = 100.0f;
			int startIndLow = SignalProcUtils.freq2index(params.startFreq, params.fs, maxFreqInd);
			int endIndLow = SignalProcUtils.freq2index(maxVoicingFreqInHz, params.fs, maxFreqInd);
			int startIndHigh = endIndLow + 1;
			int endIndHigh = SignalProcUtils.freq2index(params.endFreq, params.fs, maxFreqInd);
			int halfF0Ind = SignalProcUtils.freq2index(0.5 * f0, params.fs, maxFreqInd);

			// Determine peak amplitude indices and the corresponding amplitudes, frequencies, and phases
			if (initialPeakLocationsInHz != null) // Some initial peak locations given, search peaks around them
			{
				/*
				 * freqInds = new int[initialPeakLocationsInHz.length]; for (i=0; i<initialPeakLocationsInHz.length; i++) { int
				 * startInd =
				 * SignalProcUtils.freq2index(initialPeakLocationsInHz[i]*(1.0-HntmAnalyzer.HARMONIC_DEVIATION_PERCENT/100.0),
				 * params.fs, maxFreqInd); int endInd =
				 * SignalProcUtils.freq2index(initialPeakLocationsInHz[i]*(1.0+HntmAnalyzer.HARMONIC_DEVIATION_PERCENT/100.0),
				 * params.fs, maxFreqInd); freqInds[i] = MathUtils.getAbsMaxInd(frameDftDB, startInd, endInd); if (freqInds[i]<0)
				 * freqInds[i] = SignalProcUtils.freq2index(initialPeakLocationsInHz[i], params.fs, maxFreqInd); }
				 */

				// Return directly at specified locations without peak search
				freqInds = new int[initialPeakLocationsInHz.length];
				for (i = 0; i < initialPeakLocationsInHz.length; i++)
					freqInds[i] = SignalProcUtils.freq2index(initialPeakLocationsInHz[i], params.fs, maxFreqInd);
				//
			} else if (!bManualPeakPickingTest) {
				// Method A: Conventional method gets local extrema from db spectrum
				freqInds = MathUtils.getExtrema(frameDftDB, freqSampNeighs, freqSampNeighs, true, startIndLow, endIndHigh,
						SinusoidalAnalysisParams.MIN_PEAK_IN_DB_LOW);
				if (!ArrayUtils.isOneOf(freqInds, 0)) // Add zeroth index since it is the dc bias
					freqInds = ArrayUtils.appendToStart(freqInds, 0);

				/*
				 * //Method B: Peak picking in lower freqs + Peak picking in upper freqs but with different parameters freqIndsLow
				 * = MathUtils.getExtrema(frameDftDB, DEFAULT_FREQ_SAMP_NEIGHS_LOW, DEFAULT_FREQ_SAMP_NEIGHS_LOW, true,
				 * startIndLow, endIndLow, MIN_PEAK_IN_DB_LOW); freqIndsHigh = MathUtils.getExtrema(frameDftDB,
				 * DEFAULT_FREQ_SAMP_NEIGHS_HIGH, DEFAULT_FREQ_SAMP_NEIGHS_HIGH, true, startIndHigh, endIndHigh,
				 * MIN_PEAK_IN_DB_HIGH); //
				 */

				/*
				 * //Method C: Peak picking in lower freqs + Uniform sampling in upper freqs freqIndsLow =
				 * MathUtils.getExtrema(frameDftDB, DEFAULT_FREQ_SAMP_NEIGHS_LOW, DEFAULT_FREQ_SAMP_NEIGHS_LOW, true, startIndLow,
				 * endIndLow, MIN_PEAK_IN_DB_LOW); int totalHighs =
				 * (int)(Math.floor(0.5*fs-maxVoicingFreqInHz)/upperFreqSamplingStepInHz); if (totalHighs>0) { freqIndsHigh = new
				 * int[totalHighs]; for (i=0; i<totalHighs; i++) freqIndsHigh[i] =
				 * SignalProcUtils.freq2index(maxVoicingFreqInHz+(i-0.5)*upperFreqSamplingStepInHz, fs, maxFreqInd); }
				 */

				/*
				 * //Method D: Lower peaks from HNM analysis, upper peaks with peak picking freqIndsLow = vo.peakIndices; if
				 * (freqIndsLow!=null) { if (!ArrayUtils.isOneOf(freqIndsLow, 0)) //Add zeroth index since it is the dc bias
				 * freqIndsLow = ArrayUtils.appendToStart(freqIndsLow, 0); } freqIndsHigh = MathUtils.getExtrema(frameDftDB,
				 * DEFAULT_FREQ_SAMP_NEIGHS_HIGH, DEFAULT_FREQ_SAMP_NEIGHS_HIGH, true, startIndHigh, endIndHigh,
				 * MIN_PEAK_IN_DB_HIGH); //
				 * 
				 * int numInds = 0; if (freqIndsLow!=null) numInds += freqIndsLow.length; if (freqIndsHigh!=null) numInds +=
				 * freqIndsHigh.length;
				 * 
				 * if (numInds>0) { freqInds=new int[numInds]; int currentInd = 0; if (freqIndsLow!=null) {
				 * System.arraycopy(freqIndsLow, 0, freqInds, 0, freqIndsLow.length); currentInd+=freqIndsLow.length; } if
				 * (freqIndsHigh!=null) System.arraycopy(freqIndsHigh, 0, freqInds, currentInd, freqIndsHigh.length); } //
				 */

				/*
				 * //A hybrid method: // Gets lower freq peaks from SEEVOC, i.e. harmonic peaks // and upper freq peaks from local
				 * extrema int[] maxInds = MathUtils.getExtrema(frameDftDB, freqSampNeighs, freqSampNeighs, true, startInd,
				 * endInd, MIN_PEAK_IN_DB);
				 * 
				 * int cutoffInd = SignalProcUtils.freq2index(5.5*f0, fs, maxFreqInd); int totalFromHarmonics = 0; int
				 * totalFromExtrema = 0; for (i=0; i<swpi.indices.length; i++) { if (swpi.indices[i]<cutoffInd)
				 * totalFromHarmonics++; }
				 * 
				 * for (i=0; i<maxInds.length; i++) { if (maxInds[i]>=cutoffInd) totalFromExtrema++; }
				 * 
				 * freqInds = new int[totalFromHarmonics+totalFromExtrema]; int counter=0; for (i=0; i<swpi.indices.length; i++) {
				 * if (swpi.indices[i]<cutoffInd) freqInds[counter++] = swpi.indices[i]; }
				 * 
				 * for (i=0; i<maxInds.length; i++) { if (maxInds[i]>=cutoffInd) freqInds[counter++] = maxInds[i]; }
				 */
			}

			if (freqInds != null) {
				int numFrameSinusoids = freqInds.length;
				frameSins = new NonharmonicSinusoidalSpeechFrame(numFrameSinusoids);

				// Perform parabola fitting around peak estimates to refine frequency estimation (Ref. - PARSHL, see the function
				// for more details)
				fiwa = new FreqIndicesWithAmplitudes(numFrameSinusoids);

				float[] ampsDB = new float[numFrameSinusoids];
				for (i = 0; i < numFrameSinusoids; i++)
					ampsDB[i] = (float) frameDftDB[freqInds[i]];

				if (params.bRefinePeakEstimatesParabola) {
					fiwa = refinePeakEstimatesParabola(frameDftDB, freqInds);

					/*
					 * //Need to check this function, does not work properly if (params.bRefinePeakEstimatesBias) fiwa =
					 * refinePeakEstimatesBias(frameDftDB, fiwa.freqIndsRefined, params.windowType);
					 */
				} else {
					System.arraycopy(ampsDB, 0, fiwa.ampsRefined, 0, numFrameSinusoids);

					for (i = 0; i < numFrameSinusoids; i++)
						fiwa.freqIndsRefined[i] = (float) freqInds[i];
				}

				if (params.bSpectralReassignment)
					fiwa.freqIndsRefined = refineBySpectralReassignment(frameDft, windowedFrameDerivativeFFT,
							fiwa.freqIndsRefined, params.fftSize, params.fs);

				if (initialPeakLocationsInHz == null) {
					for (i = 0; i < numFrameSinusoids; i++) {
						frameSins.sinusoids[i] = new Sinusoid((float) (MathUtils.db2amp(fiwa.ampsRefined[i])), // amp in linear
																												// scale
								(float) ((Math.PI * fiwa.freqIndsRefined[i]) / maxFreqInd), // freq in radians
								// (float)((0.5*fs*freqIndsRefined[i])/maxFreqInd), //freq in Hz
								(float) (Math.atan2(frameDft.imag[freqInds[i]], frameDft.real[freqInds[i]]))); // phase in radians

						// Possible improvement: Refinement of phase values here...

					}
				} else {
					for (i = 0; i < numFrameSinusoids; i++) {
						frameSins.sinusoids[i] = new Sinusoid((float) frameDftAbs[freqInds[i]], // amp in linear scale
								(float) ((Math.PI * fiwa.freqIndsRefined[i]) / maxFreqInd), // freq in radians
								// (float)((0.5*fs*freqIndsRefined[i])/maxFreqInd), //freq in Hz
								(float) (Math.atan2(frameDft.imag[freqInds[i]], frameDft.real[freqInds[i]]))); // phase in radians
					}
				}

				// For visualization purposes:
				if (isOutputToTextFile) {
					// FileUtils.writeToTextFile(excDftAbs, "d:/out_exc.txt");
					FileUtils.writeToTextFile(vocalTractSpec, "d:/out_vt.txt");
					FileUtils.writeToTextFile(frameDftAbs, "d:/out_spec.txt");
				}
				//

				double[] vocalTractPhase = null;
				if (vocalTractSpec != null) {
					double[] tmpSpec = null;
					// tmpSpec = SignalProcUtils.cepstralSmoothedSpectrumInNeper(frm, fftSize, lifterOrder); //Use cepstral
					// processing to find minimum phase system response
					// Use LP spectrum to find a minimum phase system response
					tmpSpec = new double[params.fftSize];
					System.arraycopy(vocalTractSpec, 0, tmpSpec, 0, vocalTractSpec.length);
					for (i = params.fftSize - 1; i >= vocalTractSpec.length; i--)
						tmpSpec[i] = tmpSpec[params.fftSize - i];
					tmpSpec = MathUtils.amp2neper(tmpSpec);
					//

					// MaryUtils.plot(vocalTractSpec);
					// MaryUtils.plot(tmpSpec);

					double[] tmpPhase = CepstrumSpeechAnalyser.minimumPhaseResponseInRadians(tmpSpec);
					vocalTractPhase = new double[params.fftSize / 2 + 1];
					System.arraycopy(tmpPhase, 0, vocalTractPhase, 0, vocalTractPhase.length);

					// Arrays.fill(vocalTractSpec, 1.0); //Set system amps to one for testing purposes
					// Arrays.fill(vocalTractPhase, 0.0); //Set system phase to zero for testing purposes

					frameSins.setSystemAmps(vocalTractSpec);
					frameSins.setSystemPhases(vocalTractPhase);
					frameSins.setFrameDfts(frameDft);

					// This is from Van Santen´s et.al.´s book - Chapter 5
					// (van Santen, et. al., Progress in Speech Synthesis)
					float[] ceps = SignalProcUtils.specLinear2cepstrum(vocalTractSpec, 32);
					frameSins.setSystemCeps(ceps);
					//
				}
				//
			}
			//
		}

		if (frameSins != null) {
			// frameSins.voicing = (float)SignalProcUtils.getVoicingProbability(windowedFrm, params.fs);
			frameSins.voicing = isVoiced ? 1.0f : 0.0f;
			frameSins.maxFreqOfVoicing = SignalProcUtils.hz2radian(maxVoicingFreqInHz, params.fs);
		}

		return frameSins;
	}

	// Refine peak detection to get more accurate frequency and amplitude values as described in (Smith III and Serra, 1985)(*)
	//
	// (*) Julius O. Smith III and Xavier Serra, 1985, "PARSHL: An Analysis/Synthesis Program for Non-Harmonic Sounds
	// Based on a Sinusoidal Representation", Technical Report, Stanford University, CCRMA STAN-M-43.
	//
	// The basic idea is to fit a parabola to each of the peak detected by simple peak picking from the dB spectrum
	// The previous and next frequency bin is used along with each peak to fit the parabola
	// Then, the peak of the parabola is returned as the peak amplitude estimate and
	// its location as a floating point frequency index for refined peak location
	//
	// Parameters:
	// powSpecdB: Power spectrum estimate in dB
	// freqInds: Peak locations (frequency bins) which we want to refine
	// freqIndsRefined: (OUTPUT) - Refined peak locations as floating point frequency bins
	// ampsRefined: (OUTPUT) - Refined peak amplitude estimates corresponding to the peak value of the parabola fit to each
	// amplitude triplet
	public static FreqIndicesWithAmplitudes refinePeakEstimatesParabola(double[] powSpecdB, int[] freqInds) {
		double alpha, beta, gamma, p;

		FreqIndicesWithAmplitudes fiwa = new FreqIndicesWithAmplitudes(freqInds.length);
		for (int i = 0; i < freqInds.length; i++) {
			// Make sure the peak is not at the first or last freq bin
			if (freqInds[i] > 0 && freqInds[i] < freqInds.length - 1) {
				alpha = powSpecdB[freqInds[i] - 1];
				beta = powSpecdB[freqInds[i]];
				gamma = powSpecdB[freqInds[i] + 1];

				p = 0.5 * (alpha - gamma) / (alpha - 2 * beta + gamma);

				fiwa.freqIndsRefined[i] = (float) (freqInds[i] + p);
				fiwa.ampsRefined[i] = (float) (beta - 0.25 * p * (alpha - gamma));
				// fiwa.ampsRefined[i] = (float)((p*p*(alpha-beta)+p*2*(alpha-2*beta)-beta)/(2*(alpha-beta)));
			} else // otherwise do not refine
			{
				fiwa.freqIndsRefined[i] = (float) freqInds[i];
				fiwa.ampsRefined[i] = (float) powSpecdB[freqInds[i]];
			}
		}

		return fiwa;
	}

	// Need to check this function, does not work properly!
	// Further refine peak detection to get more accurate frequency and amplitude values as described in (Abe and Smith III,
	// 2004)(**)
	//
	// (**) Mototsugu Abe and Julius O. Smith III, 2004, "CQIFFT: Correcting Bias in a Sinusoidal Parameter Estimator based
	// on Quadratic Interpolation of FFT Magnitude Peaks", Technical Report, Center for Computer Research in Music
	// and Acoustics, Department of Music, Stanford University, STAN-M-117.
	//
	// The basic idea is to measure and correct the window-dependent bias of the quadratic refinement method in the previous
	// function
	//
	// Parameters:
	// powSpecdB: Power spectrum estimate in dB
	// freqInds: Peak locations (frequency bins) which we want to refine
	// freqIndsRefined: (OUTPUT) - Refined peak locations as floating point frequency bins
	// ampsRefined: (OUTPUT) - Refined peak amplitude estimates corresponding to the peak value of the parabola fit to each
	// amplitude triplet
	public static FreqIndicesWithAmplitudes refinePeakEstimatesBias(double[] powSpecdB, float[] freqInds, int windowType) {
		FreqIndicesWithAmplitudes fiwa = new FreqIndicesWithAmplitudes(freqInds.length);
		double delHat, Zpf, ZpA;
		double[] c = new double[4];

		// The Zp values are for a max bias of 0.01% in frequency and amplitude as given in Table 3 (Abe and Smith III, 2004)
		switch (windowType) {
		case Window.HANNING:
			Zpf = 1.5;
			ZpA = 1.9;
			c[0] = 0.247560;
			c[1] = 0.084372;
			c[2] = -0.090608;
			c[3] = -0.055781;
			break;
		case Window.HAMMING:
			Zpf = 1.5;
			ZpA = 2.0;
			c[0] = 0.256498;
			c[1] = 0.075977;
			c[2] = -0.116927;
			c[3] = -0.062882;
			break;
		case Window.BLACKMAN:
			Zpf = 1.2;
			ZpA = 1.7;
			c[0] = 0.124188;
			c[1] = 0.013752;
			c[2] = -0.038073;
			c[3] = -0.006195;
			break;
		default: // These are for rectangular window in fact
			Zpf = 2.9;
			ZpA = 3.5;
			c[0] = 1.279369;
			c[1] = 1.756245;
			c[2] = -1.173273;
			c[3] = -3.241966;
		}

		double EZpf = c[0] * Math.pow(Zpf, -2.0) + c[1] * Math.pow(Zpf, -4.0);
		double nZpA = c[2] * Math.pow(ZpA, -4.0) + c[3] * Math.pow(ZpA, -6.0);
		for (int i = 0; i < freqInds.length; i++) {
			delHat = fiwa.freqIndsRefined[i] - freqInds[i];
			fiwa.freqIndsRefined[i] = fiwa.freqIndsRefined[i]
					+ (float) (delHat + EZpf * (delHat - 0.5) * (delHat + 0.5) * delHat);
			fiwa.ampsRefined[i] = (float) (fiwa.ampsRefined[i] + nZpA * delHat * delHat);
		}

		return fiwa;
	}

	// References: Auger, F. and Flandrin, P., 1995,
	// "Improving the readability of time-frequency and time-scale representations by the reassignment method",
	// in IEEE Trans. on Signal Proc., Vol. 43, Issue 5, May 1995, pp. 1068-1089.
	// Borum, S. and Jensen, K., 1999, "Additive analysis/synthesis using analytically derived windows",
	// in Proc. of the 2nd COST G-6 Workshop on Digital Audio Effects (DAFx-99), NTNU, Trondheim,
	public static float[] refineBySpectralReassignment(ComplexArray windowedFrameFFT, ComplexArray windowedFrameDerivativeFFT,
			float[] freqInds, int fftSize, int fs) {
		float[] freqIndsRefined = null;

		if (freqInds != null) {
			freqIndsRefined = new float[freqInds.length];

			if (windowedFrameFFT != null && windowedFrameDerivativeFFT != null) {
				int km;
				double f0InRadians, f0RefinedInRadians, f0RefinedInHz, f0RefinedInd;
				int maxFreqInd = (int) (fftSize / 2 + 1) - 1;
				for (int i = 0; i < freqInds.length; i++) {
					km = (int) Math.floor(freqInds[i] + 0.5);
					f0InRadians = SignalProcUtils.hz2radian(SignalProcUtils.index2freq(km, fs, maxFreqInd), fs);
					f0RefinedInRadians = f0InRadians
							- (windowedFrameFFT.real[km] * windowedFrameDerivativeFFT.imag[km] - windowedFrameFFT.imag[km]
									* windowedFrameDerivativeFFT.real[km])
							/ (windowedFrameFFT.real[km] * windowedFrameFFT.real[km] + windowedFrameFFT.imag[km]
									* windowedFrameFFT.imag[km]) / MathUtils.TWOPI;
					f0RefinedInHz = SignalProcUtils.radian2hz(f0RefinedInRadians, fs);
					freqIndsRefined[i] = (float) SignalProcUtils.freq2indexDouble(f0RefinedInHz, fs, maxFreqInd);
				}
			} else {
				freqIndsRefined = new float[freqInds.length];
				System.arraycopy(freqInds, 0, freqIndsRefined, 0, freqInds.length);
			}
		}

		return freqIndsRefined;
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		double startFreqInHz = 0.0;
		double endFreqInHz = 0.5 * samplingRate;
		int windowType = Window.HAMMING;
		boolean bRefinePeakEstimatesParabola = false;
		boolean bRefinePeakEstimatesBias = false;
		boolean bSpectralReassignment = true;
		boolean bAdjustNeighFreqDependent = false;

		SinusoidalAnalysisParams params = new SinusoidalAnalysisParams(samplingRate, startFreqInHz, endFreqInHz, windowType,
				bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);

		SinusoidalAnalyzer sa = new SinusoidalAnalyzer(params);

		float winSizeInSeconds = 0.020f;
		float skipSizeInSeconds = 0.010f;
		float deltaInHz = 50.0f;
		int spectralEnvelopeType = SinusoidalAnalysisParams.SEEVOC_SPEC;

		String strPitchFile = args[0].substring(0, args[0].length() - 4) + ".ptc";
		PitchReaderWriter f0 = new PitchReaderWriter(strPitchFile);
		float ws_f0 = (float) f0.header.windowSizeInSeconds;
		float ss_f0 = (float) f0.header.skipSizeInSeconds;

		SinusoidalTracks st = sa.analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, spectralEnvelopeType,
				f0.contour, ws_f0, ss_f0);
	}
}
