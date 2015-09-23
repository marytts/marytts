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

import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * A sinusoidal track is a collection of matched (amplitude,frequency,phase) triplets which represents a "relatively" stationary
 * time-frequency partition of a signal
 * 
 * @author Oytun T&uuml;rk
 */
public class SinusoidalTrack {

	public float[] amps; // Amplitudes of the sinusoids
	public float[] freqs; // Frequencies of the sinusoids
	public float[] phases; // Phases of the sinusoids in radians
	public int[] frameIndices; // Frame indices of sinusoids
	public float[] times; // Times of the sinusoids in seconds
	public float[] maxFreqOfVoicings; // Voicing probabilities of track components

	public int[] states; // State of the track for a given index (one of the flags below, by default: ACTIVE

	public static int ACTIVE = 0; // The track has been turned on in a previous frame and has not been turned off until now
	public static int TURNED_ON = 1; // The track is turned on at the current time instant
	public static int TURNED_OFF = 2; // The track is turned off until next turning on event

	public int currentIndex;
	public int totalSins;

	// These two parameters are used for keeping temporary information
	// on new sinusoid candidates to be appended to the current track during track generation
	public Sinusoid newCandidate;
	public int newCandidateInd;
	//

	// These are for checking some statistics and debugging only, not required for actual analysis/synthesis
	public float avgFreqInHz;
	public float minFreqInHz;
	public float maxFreqInHz;

	public float avgAmpLinear;
	public float minAmpLinear;
	public float maxAmpLinear;

	public float avgPhaseInDegrees;
	public float minPhaseInDegrees;
	public float maxPhaseInDegrees;

	public float minTimeInSeconds;
	public float maxTimeInSeconds;

	public void getStatistics(boolean isFreqRadian, boolean isPhaseRadian, int fs, int trackIndex) {
		avgFreqInHz = 0.0f;
		minFreqInHz = -1.0f;
		maxFreqInHz = -1.0f;

		avgAmpLinear = 0.0f;
		minAmpLinear = -1.0f;
		maxAmpLinear = -1.0f;

		avgPhaseInDegrees = 0.0f;
		minPhaseInDegrees = -1.0f;
		maxPhaseInDegrees = -1.0f;

		minTimeInSeconds = -1.0f;
		maxTimeInSeconds = -1.0f;

		if (totalSins > 0) {
			int i;
			for (i = 0; i < totalSins; i++) {
				avgFreqInHz += freqs[i];
				avgAmpLinear += amps[i];
				avgPhaseInDegrees += phases[i];
			}

			avgFreqInHz /= totalSins;
			avgAmpLinear /= totalSins;
			avgPhaseInDegrees /= totalSins;

			if (isFreqRadian)
				avgFreqInHz = SignalProcUtils.radian2hz(avgFreqInHz, fs);

			if (isPhaseRadian)
				avgPhaseInDegrees = MathUtils.radian2degrees(avgPhaseInDegrees);

			minFreqInHz = MathUtils.getMin(freqs);
			if (isFreqRadian)
				minFreqInHz = SignalProcUtils.radian2hz(minFreqInHz, fs);

			maxFreqInHz = MathUtils.getMax(freqs);
			if (isFreqRadian)
				maxFreqInHz = SignalProcUtils.radian2hz(maxFreqInHz, fs);

			minAmpLinear = MathUtils.getMin(amps);
			maxAmpLinear = MathUtils.getMax(amps);

			minPhaseInDegrees = MathUtils.getMin(phases);
			if (isPhaseRadian)
				minPhaseInDegrees = MathUtils.radian2degrees(minPhaseInDegrees);

			maxPhaseInDegrees = MathUtils.getMax(phases);
			if (isPhaseRadian)
				maxPhaseInDegrees = MathUtils.radian2degrees(maxPhaseInDegrees);

			minTimeInSeconds = MathUtils.getMin(times);
			maxTimeInSeconds = MathUtils.getMax(times);

			char chTab = 9;
			String str = "ind=" + String.valueOf(trackIndex) + " avgFreq=" + String.valueOf(avgFreqInHz) + " Hz." + chTab;
			str += "minFreq=" + String.valueOf(minFreqInHz) + " Hz." + chTab;
			str += "maxFreq=" + String.valueOf(maxFreqInHz) + " Hz." + chTab;
			str += "avgAmpl=" + String.valueOf(avgAmpLinear) + chTab;
			str += "minAmpl=" + String.valueOf(minAmpLinear) + chTab;
			str += "maxAmpl=" + String.valueOf(maxAmpLinear) + chTab;
			str += "avgPhas=" + String.valueOf(avgPhaseInDegrees) + "°" + chTab;
			str += "minPhas=" + String.valueOf(minPhaseInDegrees) + "°" + chTab;
			str += "maxPhas=" + String.valueOf(maxPhaseInDegrees) + "°" + chTab;
			str += "minTime=" + String.valueOf(minTimeInSeconds) + "s." + chTab;
			str += "maxTime=" + String.valueOf(maxTimeInSeconds) + "s.";

			System.out.println(str);
		}
	}

	//

	public SinusoidalTrack(int len) {
		initialize(len);
	}

	// Create a track from a single sinusoid
	public SinusoidalTrack(float time, Sinusoid sin, float maxFreqOfVoicing, int state) {
		add(time, sin.amp, sin.freq, sin.phase, sin.frameIndex, maxFreqOfVoicing, state);
	}

	// Create a track from a track
	public SinusoidalTrack(SinusoidalTrack trk) {
		initialize(trk.totalSins);
		copy(trk);
	}

	public void initialize(int len) {
		if (len > 0) {
			totalSins = len;
			times = new float[totalSins];
			amps = new float[totalSins];
			freqs = new float[totalSins];
			phases = new float[totalSins];
			frameIndices = new int[totalSins];
			maxFreqOfVoicings = new float[totalSins];
			states = new int[totalSins];
		} else {
			totalSins = 0;
			times = null;
			amps = null;
			freqs = null;
			phases = null;
			frameIndices = null;
			maxFreqOfVoicings = null;
			states = null;
		}

		currentIndex = -1;
		newCandidate = null;
		newCandidateInd = -1;
	}

	// Copy part of the existing track parameters in srcTrack into the current track
	// starting from startSinIndex and ending at endSinIndex
	// including startSinIndex and endSinIndex
	public void copy(SinusoidalTrack srcTrack, int startSinIndex, int endSinIndex) {
		if (startSinIndex < 0)
			startSinIndex = 0;
		if (endSinIndex < 0)
			endSinIndex = 0;

		if (endSinIndex > srcTrack.totalSins - 1)
			endSinIndex = srcTrack.totalSins - 1;
		if (startSinIndex > endSinIndex)
			startSinIndex = endSinIndex;

		if (totalSins < endSinIndex - startSinIndex + 1)
			initialize(endSinIndex - startSinIndex + 1);

		if (totalSins > 0) {
			System.arraycopy(srcTrack.times, startSinIndex, this.times, 0, endSinIndex - startSinIndex + 1);
			System.arraycopy(srcTrack.amps, startSinIndex, this.amps, 0, endSinIndex - startSinIndex + 1);
			System.arraycopy(srcTrack.freqs, startSinIndex, this.freqs, 0, endSinIndex - startSinIndex + 1);
			System.arraycopy(srcTrack.phases, startSinIndex, this.phases, 0, endSinIndex - startSinIndex + 1);
			System.arraycopy(srcTrack.frameIndices, startSinIndex, this.frameIndices, 0, endSinIndex - startSinIndex + 1);
			System.arraycopy(srcTrack.maxFreqOfVoicings, startSinIndex, this.maxFreqOfVoicings, 0, endSinIndex - startSinIndex
					+ 1);
			System.arraycopy(srcTrack.states, startSinIndex, this.states, 0, endSinIndex - startSinIndex + 1);
			currentIndex = endSinIndex - startSinIndex;
		}
	}

	// Copy an existing track (srcTrack) into the current track
	public void copy(SinusoidalTrack srcTrack) {
		copy(srcTrack, 0, srcTrack.totalSins - 1);
	}

	public void add(float time, Sinusoid newSin, float pVoicing, int state) {
		add(time, newSin.amp, newSin.freq, newSin.phase, newSin.frameIndex, pVoicing, state);
	}

	// Add a new sinusoid to the track
	public void add(float time, float amp, float freq, float phase, int frameIndex, float maxFreqOfVoicing, int state) {
		if (currentIndex + 1 >= totalSins) // Expand the current track to twice its length and then add
		{
			SinusoidalTrack tmpTrack = new SinusoidalTrack(totalSins);
			if (totalSins > 0) {
				tmpTrack.copy(this);

				initialize(tmpTrack.totalSins + 1);

				this.copy(tmpTrack);
			} else
				initialize(1);
		}

		currentIndex++;

		times[currentIndex] = time;
		amps[currentIndex] = amp;
		freqs[currentIndex] = freq;
		phases[currentIndex] = phase;
		frameIndices[currentIndex] = frameIndex;
		maxFreqOfVoicings[currentIndex] = maxFreqOfVoicing;
		states[currentIndex] = state;
	}

	// Update parameters of <index>th sinusoid in track
	public void update(int index, int time, float amp, float freq, float phase, int frameIndex, float sysAmp,
			float maxFreqOfVoicing, int state) {
		if (index < totalSins) {
			times[index] = time;
			amps[index] = amp;
			freqs[index] = freq;
			phases[index] = phase;
			frameIndices[index] = frameIndex;
			maxFreqOfVoicings[index] = maxFreqOfVoicing;
			states[index] = state;
		}
	}

	public void resetCandidate() {
		newCandidate = null;
		newCandidateInd = -1;
	}

	// Check turning on instants and if it is misplaced, correct its location
	public void correctTrack() {
		for (int i = 0; i < totalSins; i++) {
			if (states[i] == TURNED_OFF) {
				if (i > 0 && (times[i] - times[i - 1]) > 0.040f)
					times[i] = times[i - 1] + TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS;
			}

			if (states[i] == TURNED_ON) {
				if (i < totalSins - 1 && (times[i + 1] - times[i]) > 0.040f)
					times[i] = times[i + 1] - TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS;

			}
		}
	}
}
