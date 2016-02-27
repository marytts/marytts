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
package marytts.signalproc.sinusoidal.pitch;

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
import marytts.signalproc.window.Window;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * This class implements the comb-filter based pitch tracker in:
 * 
 * Quatieri, T. F. Discrete-Time Speech Signal Processing: Principles and Practice. Prentice-Hall Inc. 2001. (Chapter 9 –
 * Sinusoidal Analysis/Synthesis)
 * 
 * @author Oytun T&uuml;rk
 */
public class CombFilterPitchTracker extends BaseSinusoidalPitchTracker {

	public CombFilterPitchTracker() {

	}

	public double performanceCriterion(NonharmonicSinusoidalSpeechFrame sinFrame, float f0Candidate, int samplingRate) {
		double Q = 0.0;

		int endIndex = 0;
		float f0CandidateInRadians = SignalProcUtils.hz2radian(f0Candidate, samplingRate);

		while (sinFrame.sinusoids[endIndex].freq < f0CandidateInRadians) {
			if (endIndex >= sinFrame.sinusoids.length - 1)
				break;

			endIndex++;
		}

		for (int k = 0; k <= endIndex; k++)
			Q += sinFrame.sinusoids[k].amp
					* sinFrame.sinusoids[k].amp
					* Math.cos(MathUtils.TWOPI * SignalProcUtils.radian2hz(sinFrame.sinusoids[k].freq, samplingRate)
							/ f0Candidate);

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

		int windowType = Window.HAMMING;
		double startFreqInHz = 0.0;
		double endFreqInHz = 0.5 * samplingRate;
		boolean bRefinePeakEstimatesParabola = true;
		boolean bRefinePeakEstimatesBias = true;
		boolean bSpectralReassignment = true;
		boolean bAdjustNeighFreqDependent = true;

		SinusoidalAnalysisParams params = new SinusoidalAnalysisParams(samplingRate, startFreqInHz, endFreqInHz, windowType,
				bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);

		String strPitchFileIn = args[0].substring(0, args[0].length() - 4) + ".ptc";
		PitchReaderWriter f0 = new PitchReaderWriter(strPitchFileIn);
		int pitchMarkOffset = 0;
		PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length,
				f0.header.windowSizeInSeconds, f0.header.skipSizeInSeconds, true, pitchMarkOffset);
		PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(params);

		// sa.setSinAnaFFTSize(4096);
		NonharmonicSinusoidalSpeechSignal ss = sa.extractSinusoidsFixedRate(x, windowSizeInSeconds, skipSizeInSeconds, deltaInHz);

		CombFilterPitchTracker p = new CombFilterPitchTracker();
		float[] f0s = p.pitchTrack(ss, samplingRate, searchStepInHz, minFreqInHz, maxFreqInHz);

		String strPitchFileOut = args[0].substring(0, args[0].length() - 4) + ".ptcSin";

		PitchReaderWriter.write_pitch_file(strPitchFileOut, f0s, windowSizeInSeconds, skipSizeInSeconds, samplingRate);

		for (int i = 0; i < f0s.length; i++)
			System.out.println(String.valueOf(i * skipSizeInSeconds + 0.5f * windowSizeInSeconds) + " sec. = "
					+ String.valueOf(f0s[i]));
	}
}
