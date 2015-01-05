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

public class DiphoneUnit extends Unit {
	public final Unit left;
	public final Unit right;

	public DiphoneUnit(Unit left, Unit right) {
		super(left.startTime, left.duration + right.duration, left.index);
		this.left = left;
		this.right = right;
	}

	public int getIndex() {
		throw new IllegalStateException("This method should not be called for DiphoneUnits.");
	}

	public boolean isEdgeUnit() {
		throw new IllegalStateException("This method should not be called for DiphoneUnits.");
	}

	public String toString() {
		return "diphoneunit " + index + " start: " + startTime + ", duration: " + duration;
	}

	/**
	 * inspired by http://www.artima.com/lejava/articles/equality.html
	 */
	@Override
	public boolean equals(Object other) {
		boolean result = false;
		if (other instanceof DiphoneUnit) {
			DiphoneUnit that = (DiphoneUnit) other;
			result = (this.left.equals(that.left) && this.right.equals(that.right));
		}
		return result;
	}

	/**
	 * inspired by http://www.artima.com/lejava/articles/equality.html
	 */
	@Override
	public int hashCode() {
		return (41 * (41 + this.left.hashCode()) + this.right.hashCode());
	}

}
