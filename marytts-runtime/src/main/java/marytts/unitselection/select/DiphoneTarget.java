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

import marytts.features.FeatureVector;
import marytts.modules.phonemiser.Allophone;

import org.w3c.dom.Element;

public class DiphoneTarget extends Target {
	public final HalfPhoneTarget left;
	public final HalfPhoneTarget right;

	public DiphoneTarget(HalfPhoneTarget left, HalfPhoneTarget right) {
		super(null, null);
		this.name = left.name.substring(0, left.name.lastIndexOf("_")) + "-"
				+ right.name.substring(0, right.name.lastIndexOf("_"));
		assert left.isRightHalf(); // the left half of this diphone must be the right half of a phone
		assert right.isLeftHalf();
		this.left = left;
		this.right = right;
	}

	@Override
	public Element getMaryxmlElement() {
		throw new IllegalStateException("This method should not be called for DiphoneTargets.");
	}

	public FeatureVector getFeatureVector() {
		throw new IllegalStateException("This method should not be called for DiphoneTargets.");
	}

	public void setFeatureVector(FeatureVector featureVector) {
		throw new IllegalStateException("This method should not be called for DiphoneTargets.");
	}

	public float getTargetDurationInSeconds() {
		throw new IllegalStateException("This method should not be called for DiphoneTargets.");
	}

	/**
	 * Determine whether this target is a silence target
	 * 
	 * @return true if the target represents silence, false otherwise
	 */
	public boolean isSilence() {
		throw new IllegalStateException("This method should not be called for DiphoneTargets.");
	}

	public Allophone getAllophone() {
		throw new IllegalStateException("This method should not be called for DiphoneTargets.");
	}

}
