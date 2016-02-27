/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.signalproc.sinusoidal.test;

import java.io.IOException;

import marytts.signalproc.sinusoidal.Sinusoid;

/**
 * Generates test signals that consist of artificially generated harmonics
 * 
 * @author oytun.turk
 * 
 */
public class HarmonicsTester extends SinusoidsTester {

	public HarmonicsTester(float freqInHz, int numHarmonics) {
		this(freqInHz, numHarmonics, DEFAULT_DUR, DEFAULT_FS);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds) {
		this(freqInHz, numHarmonics, durationInSeconds, DEFAULT_FS);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds, int samplingRateInHz) {
		this(freqInHz, numHarmonics, durationInSeconds, samplingRateInHz, null, null);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float[] amps) {
		this(freqInHz, numHarmonics, amps, null);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float[] amps, float[] phases) {
		this(freqInHz, numHarmonics, DEFAULT_DUR, DEFAULT_FS, amps, phases);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds, float[] amps) {
		this(freqInHz, numHarmonics, durationInSeconds, DEFAULT_FS, amps, null);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds, int samplingRateInHz, float[] amps) {
		this(freqInHz, numHarmonics, durationInSeconds, samplingRateInHz, amps, null);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds, int samplingRateInHz, float[] amps,
			float[] phases) {
		if (numHarmonics > 0) {
			Sinusoid[] sins = new Sinusoid[numHarmonics];

			float currentAmp, currentPhase;
			for (int i = 0; i < numHarmonics; i++) {
				if (amps != null && amps.length > i)
					currentAmp = amps[i];
				else
					currentAmp = DEFAULT_AMP;

				if (phases != null && phases.length > i)
					currentPhase = phases[i];
				else
					currentPhase = DEFAULT_PHASE;

				sins[i] = new Sinusoid(currentAmp, freqInHz * (i + 1), currentPhase);
			}

			init(sins, durationInSeconds, samplingRateInHz);
		}
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float startTimeInSeconds, float endTimeInSeconds) {
		this(freqInHz, numHarmonics, startTimeInSeconds, endTimeInSeconds, DEFAULT_FS);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float startTimeInSeconds, float endTimeInSeconds,
			int samplingRateInHz) {
		this(freqInHz, numHarmonics, startTimeInSeconds, endTimeInSeconds, samplingRateInHz, null);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float startTimeInSeconds, float endTimeInSeconds,
			int samplingRateInHz, float[] amps) {
		this(freqInHz, numHarmonics, startTimeInSeconds, endTimeInSeconds, samplingRateInHz, amps, null);
	}

	public HarmonicsTester(float freqInHz, int numHarmonics, float startTimeInSeconds, float endTimeInSeconds,
			int samplingRateInHz, float[] amps, float[] phases) {
		if (numHarmonics > 0) {
			Sinusoid[] sins = new Sinusoid[numHarmonics];

			float currentAmp, currentPhase;
			for (int i = 0; i < numHarmonics; i++) {
				if (amps != null && amps.length > i)
					currentAmp = amps[i];
				else
					currentAmp = DEFAULT_AMP;

				if (phases != null && phases.length > i)
					currentPhase = phases[i];
				else
					currentPhase = DEFAULT_PHASE;

				sins[i] = new Sinusoid(currentAmp, freqInHz * (i + 1), currentPhase);
			}

			init(sins, startTimeInSeconds, endTimeInSeconds, samplingRateInHz);
		}
	}

	public static void main(String[] args) throws IOException {
		HarmonicsTester s = null;

		// Single sinusoid, time-invariant
		float f1 = 115.0f;
		int numHarmonics = 8;
		double defaultAbsMaxVal = 13000.0;
		// s = new HarmonicsTester(f1, numHarmonics);
		float[] amps = new float[numHarmonics];
		for (int i = 0; i < numHarmonics; i++)
			amps[i] = 0.5f / numHarmonics; // (float)Math.pow(2.0, -1.0*i);
		s = new HarmonicsTester(f1, numHarmonics, amps);
		//

		if (args.length > 1)
			s.write(args[0], args[1], defaultAbsMaxVal);
		else if (args.length == 1)
			s.write(args[0], defaultAbsMaxVal);
		else {
			String outWavFile = "d:\\h" + String.valueOf(numHarmonics) + "_" + String.valueOf(f1) + "_"
					+ String.valueOf(defaultAbsMaxVal) + ".wav";
			s.write(outWavFile, defaultAbsMaxVal);
		}

	}
}
