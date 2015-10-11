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

import java.util.ArrayList;
import java.util.List;

import marytts.unitselection.data.UnitDatabase;

import org.w3c.dom.Element;

public class HalfPhoneUnitSelector extends UnitSelector {

	/**
	 * Initialise the unit selector. Need to call load() separately.
	 * 
	 * @throws Exception
	 *             Exception
	 */
	public HalfPhoneUnitSelector() throws Exception {
		super();
	}

	/**
	 * Create the list of targets from the XML elements to synthesize.
	 * 
	 * @param segmentsAndBoundaries
	 *            a list of MaryXML phone and boundary elements
	 * @return a list of Target objects
	 */
	protected List<Target> createTargets(List<Element> segmentsAndBoundaries) {
		List<Target> targets = new ArrayList<Target>();
		for (Element sOrB : segmentsAndBoundaries) {
			String phone = getPhoneSymbol(sOrB);
			targets.add(new HalfPhoneTarget(phone + "_L", sOrB, true)); // left half
			targets.add(new HalfPhoneTarget(phone + "_R", sOrB, false)); // right half
		}
		return targets;
	}

}
