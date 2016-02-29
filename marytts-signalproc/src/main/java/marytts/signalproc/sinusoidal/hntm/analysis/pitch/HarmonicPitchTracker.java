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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechFrame;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechSignal;
import marytts.signalproc.sinusoidal.PitchSynchronousSinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.SinusoidalAnalysisParams;
import marytts.signalproc.sinusoidal.pitch.BaseSinusoidalPitchTracker;
import marytts.signalproc.window.Window;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * This pitch tracker is based on Quatieri´s book
 * 
 * @author Oytun T&uuml;rk
 */
public class HarmonicPitchTracker extends BaseSinusoidalPitchTracker {

	public HarmonicPitchTracker() {

	}

	public double performanceCriterion(NonharmonicSinusoidalSpeechFrame sinFrame, float f0Candidate, int samplingRate) {
		int l, k, kw0Ind, Kw0, K;
		double kw0;

		double Q = 0.0;
		double tempSum, tempSum2;
		double freqHz;

		double maxFreqInHz = Math.max(1000.0, f0Candidate + 50.0);
		double maxFreqInRadians = SignalProcUtils.hz2radian(maxFreqInHz, samplingRate);

		Kw0 = Math.max(0, (int) Math.floor(maxFreqInHz / f0Candidate + 0.5 + 1));

		K = 0;
		while (sinFrame.sinusoids[K].freq < maxFreqInRadians) {
			K++;
			if (K >= sinFrame.sinusoids.length - 1) {
				K = sinFrame.sinusoids.length - 1;
				break;
			}
		}

		tempSum2 = 0.0;

		if (K < 1)
			Q = -1e+50;
		else {
			for (l = 1; l <= K; l++) {
				tempSum = 0.0;

				freqHz = SignalProcUtils.radian2hz(sinFrame.sinusoids[l - 1].freq, samplingRate);

				for (k = 1; k <= K; k++) {
					kw0 = k * f0Candidate;
					kw0Ind = SignalProcUtils.freq2index(kw0, samplingRate, sinFrame.systemAmps.length - 1);
					tempSum += sinFrame.systemAmps[kw0Ind]
							* Math.abs(MathUtils.sinc(freqHz - kw0, 10 * sinFrame.systemAmps.length));
				}

				Q += sinFrame.sinusoids[l - 1].amp * tempSum;
			}

			tempSum = 0.0;
			for (k = 1; k <= Kw0; k++) {
				kw0 = k * f0Candidate;
				kw0Ind = SignalProcUtils.freq2index(kw0, samplingRate, sinFrame.systemAmps.length - 1);
				tempSum += sinFrame.systemAmps[kw0Ind] * sinFrame.systemAmps[kw0Ind];
			}

			Q = Q - 0.5 * tempSum;
		}

		return Q;
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		float searchStepInHz = 0.5f;
		float minFreqInHz = 40.0f;
		float maxFreqInHz = 400.0f;

		float windowSizeInSeconds = SinusoidalAnalysisParams.DEFAULT_ANALYSIS_WINDOW_SIZE;
		float skipSizeInSeconds = SinusoidalAnalysisParams.DEFAULT_ANALYSIS_SKIP_SIZE;
		float deltaInHz = SinusoidalAnalysisParams.DEFAULT_DELTA_IN_HZ;

		// int spectralEnvelopeType = SinusoidalAnalysisParams.LP_SPEC;
		int spectralEnvelopeType = SinusoidalAnalysisParams.SEEVOC_SPEC;
		// int spectralEnvelopeType = SinusoidalAnalysisParams.REGULARIZED_CEPS;

		double startFreqInHz = 0.0;
		double endFreqInHz = 0.5 * samplingRate;
		int windowType = Window.HAMMING;
		boolean bRefinePeakEstimatesParabola = false;
		boolean bRefinePeakEstimatesBias = false;
		boolean bSpectralReassignment = false;
		boolean bAdjustNeighFreqDependent = false;

		SinusoidalAnalysisParams params = new SinusoidalAnalysisParams(samplingRate, startFreqInHz, endFreqInHz, windowType,
				bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);

		String strPitchFileIn = args[0].substring(0, args[0].length() - 4) + ".ptc";
		PitchReaderWriter f0 = new PitchReaderWriter(strPitchFileIn);
		int pitchMarkOffset = 0;
		PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length,
				f0.header.windowSizeInSeconds, f0.header.skipSizeInSeconds, true, 0);
		PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(params);

		NonharmonicSinusoidalSpeechSignal ss = sa.extractSinusoidsFixedRate(x, windowSizeInSeconds, skipSizeInSeconds, deltaInHz,
				spectralEnvelopeType, f0.contour, (float) f0.header.windowSizeInSeconds, (float) f0.header.skipSizeInSeconds);

		HarmonicPitchTracker p = new HarmonicPitchTracker();
		float[] f0s = p.pitchTrack(ss, samplingRate, searchStepInHz, minFreqInHz, maxFreqInHz);

		String strPitchFileOut = args[0].substring(0, args[0].length() - 4) + ".ptcSin";

		PitchReaderWriter.write_pitch_file(strPitchFileOut, f0s, windowSizeInSeconds, skipSizeInSeconds, samplingRate);

		for (int i = 0; i < f0s.length; i++)
			System.out.println(String.valueOf(i * skipSizeInSeconds + 0.5f * windowSizeInSeconds) + " sec. = "
					+ String.valueOf(f0s[i]));
	}
}
