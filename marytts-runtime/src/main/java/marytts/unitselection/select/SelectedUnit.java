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
package marytts.unitselection.select;

import marytts.unitselection.data.Unit;

/**
 * A unit selected from Viterbi
 * 
 * @author Marc Schr&ouml;der
 * 
 */
public class SelectedUnit {
	protected Unit unit;
	protected Target target;
	protected Object concatenationData;
	protected double[] audio;

	public SelectedUnit(Unit unit, Target target) {
		this.unit = unit;
		this.target = target;
		this.audio = null;
	}

	public Unit getUnit() {
		return unit;
	}

	public Target getTarget() {
		return target;
	}

	/**
	 * Remember data about this selected unit which is relevant for unit concatenation. What type of data is saved here depends on
	 * the UnitConcatenator used.
	 * 
	 * @param concatenationData
	 *            concatenationData
	 */
	public void setConcatenationData(Object concatenationData) {
		this.concatenationData = concatenationData;
	}

	public Object getConcatenationData() {
		return concatenationData;
	}

	public void setAudio(double[] audio) {
		this.audio = audio;
	}

	public double[] getAudio() {
		return audio;
	}

	public String toString() {
		return "Target: " + target.toString() + " Unit: " + unit.toString() + " target duration "
				+ target.getTargetDurationInSeconds();
	}
}
