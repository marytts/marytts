/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.unitselection.data;

/**
 * Representation of a unit from a unit database. This gives access to everything that is known about a given unit, including all
 * sorts of features and the actual audio data.
 * 
 * @author Marc Schr&ouml;der
 *
 */
public class Unit {

	/**
	 * Unit start time, expressed in samples. To convert into time, divide by UnitFileReader.getSampleRate().
	 */
	public final long startTime;

	/**
	 * Unit duration, expressed in samples. To convert into time, divide by UnitFileReader.getSampleRate().
	 */
	public final int duration;

	/**
	 * Index position of this unit in the unit file.
	 */
	public final int index;

	public Unit(long startTime, int duration, int index) {
		this.startTime = startTime;
		this.duration = duration;
		this.index = index;
	}

	/**
	 * Determine whether the unit is an "edge" unit, i.e. a unit marking the start or the end of an utterance.
	 * 
	 * The index of the considered unit.
	 * 
	 * @return true if the unit is an edge unit, false otherwise
	 */
	public boolean isEdgeUnit() {
		return duration == -1;
	}

	public String toString() {
		return "unit " + index + " start: " + startTime + ", duration: " + duration;
	}

	/**
	 * inspired by http://www.artima.com/lejava/articles/equality.html
	 * 
	 * @param other
	 *            other
	 */
	@Override
	public boolean equals(Object other) {
		boolean result = false;
		if (other instanceof Unit) {
			Unit that = (Unit) other;
			result = (this.index == that.index);
		}
		return result;
	}

	@Override
	public int hashCode() {
		return this.index;
	}

}
