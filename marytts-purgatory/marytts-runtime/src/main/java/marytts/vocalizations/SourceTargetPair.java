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
 * Class represents Source unit, target unit and contour distance between these units. Array of these pairs can sort with contour
 * distance
 * 
 * @author sathish
 *
 */
public class SourceTargetPair implements Comparable<SourceTargetPair> {

	private int targetUnitIndex;
	private int sourceUnitIndex;
	private double distance;

	public SourceTargetPair(int sourceUnitIndex, int targetUnitIndex, double distance) {
		this.sourceUnitIndex = sourceUnitIndex;
		this.targetUnitIndex = targetUnitIndex;
		this.distance = distance;
	}

	public int compareTo(SourceTargetPair other) {
		if (distance == other.distance)
			return 0;
		if (distance < other.distance)
			return -1;
		return 1;
	}

	public boolean equals(Object dc) {
		if (!(dc instanceof SourceTargetPair))
			return false;
		SourceTargetPair other = (SourceTargetPair) dc;
		if (distance == other.distance)
			return true;
		return false;
	}

	public int getTargetUnitIndex() {
		return this.targetUnitIndex;
	}

	public int getSourceUnitIndex() {
		return this.sourceUnitIndex;
	}
}
