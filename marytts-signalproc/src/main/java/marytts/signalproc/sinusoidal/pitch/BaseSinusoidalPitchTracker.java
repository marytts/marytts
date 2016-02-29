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

import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechFrame;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechSignal;
import marytts.util.math.MathUtils;

/**
 * Sinusoidal model based pitch tracker
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class BaseSinusoidalPitchTracker {
	float[] f0s; // f0 values in Hz
	double[] Qs; // Performance measure for each frame

	public class F0Value {
		public float f0;
		public double maxQ;

		public F0Value() {
			f0 = 0.0f;
			maxQ = 0.0;
		}
	}

	public BaseSinusoidalPitchTracker() {

	}

	public float[] pitchTrack(NonharmonicSinusoidalSpeechSignal sinSignal, int samplingRate, float searchStepInHz,
			float minFreqInHz, float maxFreqInHz) {
		f0s = null;
		Qs = null;

		if (sinSignal.framesSins.length > 0) {
			f0s = new float[sinSignal.framesSins.length];
			Qs = new double[sinSignal.framesSins.length];

			F0Value v;
			int i;
			for (i = 0; i < sinSignal.framesSins.length; i++) {
				v = pitchAnalyzeFrame(sinSignal.framesSins[i], samplingRate, searchStepInHz, minFreqInHz, maxFreqInHz);
				f0s[i] = v.f0;
				Qs[i] = v.maxQ;
				// System.out.println("f0=" + String.valueOf(f0s[i]) + " " + String.valueOf(i+1) + " of " +
				// String.valueOf(sinSignal.framesSins.length));
			}

			f0s = postProcessTrack(f0s, Qs);
		}

		return f0s;
	}

	public F0Value pitchAnalyzeFrame(NonharmonicSinusoidalSpeechFrame sinFrame, int samplingRate, float searchStepInHz,
			float minFreqInHz, float maxFreqInHz) {
		F0Value v = new F0Value();

		double[] Q = null;
		float w0;
		int i;

		if (sinFrame != null) {
			int numCandidates = (int) Math.floor((maxFreqInHz - minFreqInHz) / searchStepInHz + 1 + 0.5);

			Q = new double[numCandidates];

			int stInd, enInd;

			for (i = 0; i < numCandidates; i++) {
				w0 = i * searchStepInHz + minFreqInHz;

				Q[i] = performanceCriterion(sinFrame, w0, samplingRate);
			}

			// MaryUtils.plot(Q, true, 1000);
		}

		// Search for distinct peaks in the Q-function
		if (Q != null) {
			v.maxQ = MathUtils.getMax(Q);
			int numNeighs = (int) Math.floor(10.0f / searchStepInHz + 0.5);
			int[] maxInds = MathUtils.getExtrema(Q, numNeighs, numNeighs, true, 0, Q.length - 1, 0.1 * v.maxQ);
			if (maxInds != null) {
				int maxInd = 0;
				v.maxQ = Q[maxInds[maxInd]];

				for (i = 1; i < maxInds.length; i++) {
					if (Q[maxInds[i]] > v.maxQ) {
						v.maxQ = Q[maxInds[i]];
						maxInd = i;
					}
				}

				v.f0 = maxInds[maxInd] * searchStepInHz + minFreqInHz;
			}
		}

		if (v.maxQ < 5.0e-5)
			v.f0 = 0.0f;

		char chTab = 9;
		System.out.println(String.valueOf(v.f0) + chTab + String.valueOf(v.maxQ));

		return v;
	}

	// Baseline version that does nothing, implement functionality in derived classes
	public double performanceCriterion(NonharmonicSinusoidalSpeechFrame sinFrame, float f0Candidate, int samplingRate) {
		return -1.0f;
	}

	// Post process f0 values to eliminate obvious pitch halving errors
	// Note that comb filter based sinusoidal pitch tracker solves pitch doubling automatically.
	// But there is always a pitch halving possibility
	// This function tries to eliminate obvious halving errors,
	// i.e. isolated f0 values that are approximately half of the neighboring f0 values
	// The function also checks for isolated voiced or unvoiced f0 values and tries to correct them
	public float[] postProcessTrack(float[] f0sIn, double[] QsIn) {
		float[] f0sOut = null;
		if (f0sIn != null) {
			int i, j;
			int numfrm = f0sIn.length;
			float avgContextF0;
			int contextCount = 1;

			f0sOut = new float[numfrm];
			System.arraycopy(f0sIn, 0, f0sOut, 0, numfrm);

			// Search for isolated unvoiceds & voiceds:
			for (i = 1; i < numfrm - 1; i++) {
				if (f0sOut[i] <= 10.0f && f0sOut[i - 1] > 10.0f && f0sOut[i + 1] > 10.0f) // isolated unvoiced
					f0sOut[i] = 0.5f * (f0sOut[i - 1] + f0sOut[i + 1]);
				else if (f0sOut[i] > 10.0f && f0sOut[i - 1] <= 10.0f && f0sOut[i + 1] <= 10.0f) // isolated voiced
					f0sOut[i] = 0.0f;
			}

			// Search for isolated halvings & doublings:
			// (Doubling check should not be necessary for sinusoidal based pitch trackers!)

			for (i = contextCount; i < numfrm - contextCount; i++) {
				boolean bAllVoiced = true;

				for (j = -contextCount; j <= contextCount; j++) {
					if (f0sOut[i + j] < 10.0f) {
						bAllVoiced = false;
						break;
					}
				}

				if (bAllVoiced) {
					avgContextF0 = 0.0f;
					for (j = -contextCount; j < 0; j++)
						avgContextF0 += f0sOut[i + j];

					for (j = 1; j <= contextCount; j++)
						avgContextF0 += f0sOut[i + j];

					avgContextF0 = avgContextF0 / (2.0f * contextCount);

					if (Math.abs(f0sOut[i] - 0.5 * avgContextF0) < 10.0f) // isolated halving
						f0sOut[i] *= 2.0f;
					else if (Math.abs(f0sOut[i] - 2.0 * avgContextF0) < 10.0f) // isolated doubling
						f0sOut[i] *= 0.5f;
				}
			}
		}

		return f0sOut;
	}
}
