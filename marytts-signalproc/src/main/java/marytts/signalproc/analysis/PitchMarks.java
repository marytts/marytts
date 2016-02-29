/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc.analysis;

import marytts.util.data.ESTTrackReader;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * 
 * A wrapper class to store pitch marks as integer sample indices
 * 
 * @author Oytun T&uuml;rk
 */
public class PitchMarks {

	public int[] pitchMarks; // N+1 pitch marks if we have N periods
	public float[] f0s; // N f0 values, 0th corresponds to f0 of waveform between [pitchMarks[0],pitchMarks[1])
	public int totalZerosToPadd;

	public PitchMarks() {
		pitchMarks = null;
		f0s = null;
		totalZerosToPadd = 0;
	}

	// Initialize pitch marks from an ESTTrackReader
	// This does not handle unvoiced parts
	// So, f0s should be set to 0 in the unvoiced regions outside this code if required
	// One can use findAndSetUnvoicedF0s function to do this using the output of the autocorrelation based pitch tracker
	public PitchMarks(ESTTrackReader pmFile, int samplingRateInHz) {
		this();

		int frameEnd = 0;
		int frameStart = 0;
		if (pmFile != null && pmFile.getNumFrames() > 0) {
			frameEnd = (int) ((double) pmFile.getTime(0) * (double) (samplingRateInHz));
			if (frameEnd > 0) {
				pitchMarks = new int[pmFile.getNumFrames() + 1];
				f0s = new float[pmFile.getNumFrames()];
				pitchMarks[0] = 0;

				for (int f = 0; f < pmFile.getNumFrames(); f++) {
					frameEnd = (int) ((double) pmFile.getTime(f) * (double) (samplingRateInHz));
					pitchMarks[f + 1] = frameEnd;
					if (f > 0)
						f0s[f] = 1.0f / (pmFile.getTime(f) - pmFile.getTime(f - 1));
					else
						f0s[f] = 1.0f / pmFile.getTime(f);
				}
			} else {
				pitchMarks = new int[pmFile.getNumFrames()];
				f0s = new float[pmFile.getNumFrames() - 1];
				pitchMarks[0] = 0;

				for (int f = 1; f < pmFile.getNumFrames(); f++) {
					frameEnd = (int) ((double) pmFile.getTime(f) * (double) (samplingRateInHz));
					pitchMarks[f] = frameEnd;
					if (f > 0)
						f0s[f - 1] = 1.0f / (pmFile.getTime(f) - pmFile.getTime(f - 1));
					else
						f0s[f - 1] = 1.0f / pmFile.getTime(f);
				}
			}

		}
	}

	// count=total pitch marks
	public PitchMarks(int count, int[] pitchMarksIn, float[] f0sIn, int totalZerosToPaddIn) {
		if (count > 1) {
			pitchMarks = new int[count];
			f0s = new float[count - 1];

			System.arraycopy(pitchMarksIn, 0, pitchMarks, 0, Math.min(pitchMarksIn.length, count));
			System.arraycopy(f0sIn, 0, f0s, 0, Math.min(f0sIn.length, count - 1));
		} else {
			pitchMarks = null;
			f0s = null;
		}

		totalZerosToPadd = Math.max(0, totalZerosToPaddIn);
	}

	public void findAndSetUnvoicedF0s(float[] f0sRef, PitchFileHeader f0Header, int samplingRateInHz) {
		double[] f0sRefDouble = ArrayUtils.copyFloat2Double(f0sRef);

		findAndSetUnvoicedF0s(f0sRefDouble, f0Header, samplingRateInHz);
	}

	public void findAndSetUnvoicedF0s(double[] f0sRef, PitchFileHeader f0Header, int samplingRateInHz) {
		// MaryUtils.plot(f0s);
		// MaryUtils.plot(f0sRef);
		int i;

		if (pitchMarks != null && pitchMarks.length > 1) {
			float[] times = new float[f0sRef.length];
			for (i = 0; i < f0sRef.length; i++)
				times[i] = (float) SignalProcUtils.frameIndex2Time(i, f0Header.windowSizeInSeconds, f0Header.skipSizeInSeconds);

			int currentRefIndex;
			float currentTime;
			for (i = 0; i < f0s.length; i++) {
				currentTime = SignalProcUtils.sample2time(pitchMarks[i + 1], samplingRateInHz);
				currentRefIndex = MathUtils.findClosest(times, currentTime);

				if (f0sRef[currentRefIndex] < 10.0)
					f0s[i] = (float) f0sRef[currentRefIndex];
			}
		}

		// MaryUtils.plot(f0s);
	}
}
