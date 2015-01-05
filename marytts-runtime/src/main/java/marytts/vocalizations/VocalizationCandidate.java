/**
 * Copyright 2010 DFKI GmbH.
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
package marytts.vocalizations;

/**
 * Class represents a vocalization candidate
 * 
 * @author sathish
 */
public class VocalizationCandidate implements Comparable<VocalizationCandidate> {

	int unitIndex;
	double cost;

	public VocalizationCandidate(int unitIndex, double cost) {
		this.unitIndex = unitIndex;
		this.cost = cost;
	}

	public int compareTo(VocalizationCandidate other) {
		if (cost == other.cost)
			return 0;
		if (cost < other.cost)
			return -1;
		return 1;
	}

	public boolean equals(Object dc) {
		if (!(dc instanceof VocalizationCandidate))
			return false;
		VocalizationCandidate other = (VocalizationCandidate) dc;
		if (cost == other.cost)
			return true;
		return false;
	}

	public String toString() {
		return unitIndex + " " + cost;
	}

}
