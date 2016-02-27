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

import marytts.util.signal.SignalProcUtils;

/**
 * This class generates the sinusoidal tracks given individual peak amplitudes measured from the DFT spectrum.
 * 
 * Reference: R.J. McAulay and T.F. Quatieri, "Speech Analysis/Synthesis Based on a Sinusoidal Representation," IEEE Transactions
 * on Acoustics, Speech and Signal Processing, vol. ASSP-34, no. 4, August 1986.
 * 
 * @author Oytun T&uuml;rk
 */
public class TrackGenerator {
	public static float ZERO_AMP_SHIFT_IN_SECONDS = 0.001f; // Time instant before/after current time to insert a turning-on/off
															// event
															// The amplitudes and synthesis freqs/phases are accordingly
															// interpolated to provide a smooth transition

	public static int MEAN_FILTER_FREQ_AXIS = 3; // Median filter each tracks frequency values
	public static int MEAN_FILTER_AMP_AXIS = 3; // Median filter each tracks amplitude values

	public TrackGenerator() {

	}

	/*
	 * Group individual sinusoids into tracks by considering closeness in frequency Current version is a simple implementation of
	 * checking the frequency difference between neighbouring sinusoids and assigning them to same track if the absolute
	 * difference is less than a threshold Possible ways to improve this process would be to employ: - constraints on amplitude
	 * continuity - constraints on phase continuity (i.e. the phase difference between two consecutive sinusoids should not be
	 * larger or smaller than some percent of the period
	 * 
	 * framesSins[i][] : Array of sinusoidal parameters (amps, freqs, phases) extracted from ith speech frame framesSins[i][j]:
	 * Sinusoidal parameters of the jth peak sinusoid in the DFT spectrum of speech frame i Returns a number of sinusoidal tracks
	 * 
	 * This version uses a simple search mechanism to compare a current sinusoid frequecny with the previous and if the difference
	 * is smaller than +-deltaInHz, assigns the new sinusoid to the previous sinusoid´s track In the assignment, longer previous
	 * paths are favoured in a weighted manner, i.e. the longer a candidate track, the more likely the current sinusoid gets
	 * assigned to that track
	 */
	public SinusoidalTracks generateTracks(NonharmonicSinusoidalSpeechSignal sinSignal, float deltaInHz, int samplingRate) {
		int numFrames = sinSignal.framesSins.length;
		float deltaInRadians = SignalProcUtils.hz2radian(deltaInHz, samplingRate);

		SinusoidalTracks tr = null;
		int i;
		Sinusoid zeroAmpSin;

		if (numFrames > 0) {
			int j, k;
			float tmpDist, minDist;
			int trackInd;
			boolean[] bSinAssigneds = null;

			for (i = 0; i < numFrames; i++) {
				if (tr == null) // If no tracks yet, assign the current sinusoids to new tracks
				{
					tr = new SinusoidalTracks(sinSignal.framesSins[i].sinusoids.length, samplingRate);
					tr.setSysAmpsAndTimes(sinSignal.framesSins);

					for (j = 0; j < sinSignal.framesSins[i].sinusoids.length; j++) {
						// First add a zero amplitude sinusoid at previous time instant to allow smooth synthesis (i.e.
						// "turning on" the track)
						zeroAmpSin = new Sinusoid(0.0f, sinSignal.framesSins[i].sinusoids[j].freq, 0.0f,
								Sinusoid.NON_EXISTING_FRAME_INDEX);
						tr.add(new SinusoidalTrack(sinSignal.framesSins[i].time - ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin,
								sinSignal.framesSins[i].maxFreqOfVoicing, SinusoidalTrack.TURNED_ON));
						//

						tr.tracks[tr.currentIndex].add(sinSignal.framesSins[i].time, sinSignal.framesSins[i].sinusoids[j],
								sinSignal.framesSins[i].maxFreqOfVoicing, SinusoidalTrack.ACTIVE);
					}
				} else // If there are tracks, first check "continuations" by checking whether a given sinusoid is in the
						// +-deltaInRadians neighbourhood of the previous track.
						// Those tracks that do not continue are "turned off".
						// All sinusoids of the current frame that are not assigned to any of the "continuations" or "turned off"
						// are "birth"s of new tracks.
				{
					for (j = 0; j < tr.currentIndex + 1; j++) {
						if (tr.tracks[j] != null)
							tr.tracks[j].resetCandidate();
					}

					bSinAssigneds = new boolean[sinSignal.framesSins[i].sinusoids.length];

					// Continuations:
					for (k = 0; k < sinSignal.framesSins[i].sinusoids.length; k++) {
						minDist = Math.abs(sinSignal.framesSins[i].sinusoids[k].freq
								- tr.tracks[0].freqs[tr.tracks[0].currentIndex]);
						if (minDist < deltaInRadians)
							trackInd = 0;
						else
							trackInd = -1;

						for (j = 1; j < tr.currentIndex + 1; j++) {
							tmpDist = Math.abs(sinSignal.framesSins[i].sinusoids[k].freq
									- tr.tracks[j].freqs[tr.tracks[j].currentIndex]);

							if (tmpDist < deltaInRadians && (trackInd == -1 || tmpDist < minDist)) {
								minDist = tmpDist;
								trackInd = j;
							}
						}

						if (trackInd > -1) {
							if (tr.tracks[trackInd].newCandidateInd > -1)
								bSinAssigneds[tr.tracks[trackInd].newCandidateInd] = false;

							tr.tracks[trackInd].newCandidate = new Sinusoid(sinSignal.framesSins[i].sinusoids[k]);
							tr.tracks[trackInd].newCandidateInd = k;

							bSinAssigneds[k] = true; // The sinusoid might be assigned to an existing track provided that a closer
														// sinusoid is not found
						} else
							bSinAssigneds[k] = false; // This is the birth of a new track since it does not match any existing
														// tracks
					}

					// Here is the actual assignment of sinusoids to existing tracks
					for (j = 0; j < tr.currentIndex + 1; j++) {
						if (tr.tracks[j].newCandidate != null) {
							Sinusoid tmpSin = new Sinusoid(tr.tracks[j].newCandidate);

							if (tr.tracks[j].states[tr.tracks[j].currentIndex] != SinusoidalTrack.ACTIVE) {
								zeroAmpSin = new Sinusoid(0.0f, tr.tracks[j].freqs[tr.tracks[j].totalSins - 1], 0.0f,
										Sinusoid.NON_EXISTING_FRAME_INDEX);
								tr.tracks[j].add(sinSignal.framesSins[i].time - ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin,
										sinSignal.framesSins[i].maxFreqOfVoicing, SinusoidalTrack.TURNED_ON);
							}

							tr.tracks[j].add(sinSignal.framesSins[i].time, tmpSin, sinSignal.framesSins[i].maxFreqOfVoicing,
									SinusoidalTrack.ACTIVE);
						} else // Turn off tracks that are not assigned any new sinusoid
						{
							if (tr.tracks[j].states[tr.tracks[j].currentIndex] != SinusoidalTrack.TURNED_OFF) {
								zeroAmpSin = new Sinusoid(0.0f, tr.tracks[j].freqs[tr.tracks[j].totalSins - 1], 0.0f,
										Sinusoid.NON_EXISTING_FRAME_INDEX);
								tr.tracks[j].add(sinSignal.framesSins[i].time + ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin,
										sinSignal.framesSins[i].maxFreqOfVoicing, SinusoidalTrack.TURNED_OFF);
							}
						}
					}

					// Births: Create new tracks from sinusoids that are not assigned to existing tracks
					for (k = 0; k < bSinAssigneds.length; k++) {
						if (!bSinAssigneds[k]) {
							// First add a zero amplitude sinusoid to previous frame to allow smooth synthesis (i.e. "turning on"
							// the track)
							zeroAmpSin = new Sinusoid(0.0f, sinSignal.framesSins[i].sinusoids[k].freq, 0.0f,
									Sinusoid.NON_EXISTING_FRAME_INDEX);
							tr.add(new SinusoidalTrack(sinSignal.framesSins[i].time - ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin,
									sinSignal.framesSins[i].maxFreqOfVoicing, SinusoidalTrack.TURNED_ON));
							//

							tr.tracks[tr.currentIndex].add(sinSignal.framesSins[i].time, sinSignal.framesSins[i].sinusoids[k],
									sinSignal.framesSins[i].maxFreqOfVoicing, SinusoidalTrack.ACTIVE);
						}
					}

					System.out.println("Track generation using frame " + String.valueOf(i + 1) + " of "
							+ String.valueOf(numFrames));
				}

				// Turn-off all active tracks after the last speech frame
				if (i == numFrames - 1) {
					for (j = 0; j < tr.currentIndex + 1; j++) {
						if (Math.abs(sinSignal.framesSins[i].time - tr.tracks[j].times[tr.tracks[j].totalSins - 1]) < ZERO_AMP_SHIFT_IN_SECONDS) {
							if (tr.tracks[j].states[tr.tracks[j].currentIndex] == SinusoidalTrack.ACTIVE) {
								zeroAmpSin = new Sinusoid(0.0f, tr.tracks[j].freqs[tr.tracks[j].totalSins - 1], 0.0f,
										Sinusoid.NON_EXISTING_FRAME_INDEX);
								tr.tracks[j].add(sinSignal.framesSins[i].time + ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin,
										sinSignal.framesSins[i].maxFreqOfVoicing, SinusoidalTrack.TURNED_OFF);
							}
						}
					}
				}
				//
			}
		}

		for (i = 0; i <= tr.currentIndex; i++)
			tr.tracks[i].correctTrack();

		tr.setOriginalDurationManual(sinSignal.originalDurationInSeconds);

		SinusoidalTracks trOut = new SinusoidalTracks(tr, 0, tr.currentIndex);
		trOut = postProcess(trOut);

		return trOut;
	}

	// Simple median filtering along frequencies and amplitudes
	public static SinusoidalTracks postProcess(SinusoidalTracks st) {
		for (int i = 0; i < st.totalTracks; i++) {
			if (st.tracks[i].totalSins > 20) {
				st.tracks[i].freqs = SignalProcUtils.meanFilter(st.tracks[i].freqs, MEAN_FILTER_FREQ_AXIS);
				st.tracks[i].amps = SignalProcUtils.meanFilter(st.tracks[i].amps, MEAN_FILTER_AMP_AXIS);
			}
		}

		return st;
	}
}
