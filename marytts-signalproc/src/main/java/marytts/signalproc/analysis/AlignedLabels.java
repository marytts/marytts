/**
 * Copyright 2009 DFKI GmbH.
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author marc
 *
 */
public class AlignedLabels {
	private Labels first;
	private Labels second;
	private int[] indexMap;
	private List<AlignedTimeStretch> stretches;

	public AlignedLabels(Labels first, Labels second, int[] indexMap) {
		this.first = first;
		this.second = second;
		this.indexMap = indexMap;
		// Convert index numbers into time codes of stretches mapped onto one another.
		stretches = new ArrayList<AlignedTimeStretch>();
		// For mapping stretches, there are three possibilities:
		// 1. a one-to-one mapping, i.e. indexMap[i] == indexMap[i-1] + 1;
		// 2. a one-to-many mapping, i.e. indexMap[i] == indexMap[i-1] + n, where n > 1;
		// 3. a many-to-one mapping, i.e. indexMap[i] == indexMap[i-n], where n >= 1.

		// Index numbers in label files, representing start/end of stretches to map:
		int iFirstStart = 0;
		int iFirstEnd = 0;
		int iSecondStart = 0;
		int iSecondEnd = 0;
		while (iFirstStart < first.items.length) {
			iFirstEnd = iFirstStart;
			iSecondEnd = indexMap[iFirstStart];
			// check for case 3 (many-to-one mapping):
			while (iFirstEnd < first.items.length && indexMap[iFirstEnd] == iSecondEnd) {
				iFirstEnd++;
			}
			iFirstEnd--; // we went one too far
			// Now all first labels from iFirstStart to iFirstEnd map to the interval from iSecondStart to iSecondEnd
			// Now, the start times are the end times of the previous label (or 0 if this is the first label):
			double tFirstStart = (iFirstStart == 0) ? 0. : first.items[iFirstStart - 1].time;
			double tFirstEnd = first.items[iFirstEnd].time;
			double tSecondStart = (iSecondStart == 0) ? 0. : second.items[iSecondStart - 1].time;
			double tSecondEnd = second.items[iSecondEnd].time;
			stretches.add(new AlignedTimeStretch(tFirstStart, tFirstEnd, tSecondStart, tSecondEnd));
			// For next round:
			iFirstStart = iFirstEnd + 1;
			iSecondStart = iSecondEnd + 1;
		}
	}

	public Labels getFirst() {
		return first;
	}

	public Labels getSecond() {
		return second;
	}

	public int[] getIndexMap() {
		return indexMap;
	}

	/**
	 * Given the label sequences and their alignment, map a time in the first sequence to the corresponding time in the second
	 * sequence.
	 * 
	 * @param time1
	 *            time1
	 * @return the corresponding time, or a negative value if no corresponding time could be determined
	 */
	public double mapTimeFromFirstToSecond(double time1) {
		// Look up the corresponding time stretch, and map linearly within the time stretch:
		for (AlignedTimeStretch t : stretches) {
			if (time1 >= t.firstStart && time1 <= t.firstStart + t.firstDuration) {
				if (t.firstDuration == 0.) {
					return t.secondStart;
				} else {
					return t.secondStart + (time1 - t.firstStart) / t.firstDuration * t.secondDuration;
				}
			}
		}
		// not found
		return -1;
	}

	/**
	 * Given the label sequences and their alignment, map a time in the second sequence to the corresponding time in the first
	 * sequence.
	 * 
	 * @param time2
	 *            time2
	 * @return the corresponding time, or a negative value if no corresponding time could be determined
	 */
	public double mapTimeFromSecondToFirst(double time2) {
		// Look up the corresponding time stretch, and map linearly within the time stretch:
		for (AlignedTimeStretch t : stretches) {
			if (time2 >= t.secondStart && time2 <= t.secondStart + t.secondDuration) {
				if (t.secondDuration == 0.) {
					return t.firstStart;
				} else {
					return t.firstStart + (time2 - t.secondStart) / t.secondDuration * t.firstDuration;
				}
			}
		}
		// not found
		return -1;
	}

	public List<AlignedTimeStretch> getAlignedTimeStretches() {
		return stretches;
	}

	public static class AlignedTimeStretch {
		public final double firstStart, firstDuration, secondStart, secondDuration;

		public AlignedTimeStretch(double tFirstStart, double tFirstEnd, double tSecondStart, double tSecondEnd) {
			this.firstStart = tFirstStart;
			this.firstDuration = tFirstEnd - tFirstStart;
			this.secondStart = tSecondStart;
			this.secondDuration = tSecondEnd - tSecondStart;
			assert firstDuration >= 0.;
			assert secondDuration >= 0.;
		}

		public String toString() {
			return String.format("%.3f+%.3f %.3f+%.3f", firstStart, firstDuration, secondStart, secondDuration);
		}
	}
}
