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

import org.w3c.dom.Element;

public class HalfPhoneTarget extends Target {
	protected boolean isLeftHalf;

	/**
	 * Create a target associated to the given segment item.
	 * 
	 * @param name
	 *            a name for the target, which may or may not coincide with the segment name.
	 * @param maryxmlElement
	 *            the phone segment item in the Utterance structure, to be associated to this target
	 * @param isLeftHalf
	 *            true if this target represents the left half of the phone, false if it represents the right half of the phone
	 */
	public HalfPhoneTarget(String name, Element maryxmlElement, boolean isLeftHalf) {
		super(name, maryxmlElement);
		this.isLeftHalf = isLeftHalf;
	}

	/**
	 * Is this target the left half of a phone?
	 * 
	 * @return isLeftHalf
	 */
	public boolean isLeftHalf() {
		return isLeftHalf;
	}

	/**
	 * Is this target the right half of a phone?
	 * 
	 * @return !isLeftHalf
	 */
	public boolean isRightHalf() {
		return !isLeftHalf;
	}

}
