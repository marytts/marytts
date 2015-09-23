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
package marytts.modules;

import java.util.ArrayList;
import java.util.List;

import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Element;

public class HalfPhoneTargetFeatureLister extends TargetFeatureLister {

	@Deprecated
	public HalfPhoneTargetFeatureLister() {
		super(MaryDataType.HALFPHONE_TARGETFEATURES);
	}

	/**
	 * Access the code from within the our own code so that a subclass can override it. Use this rather than the public static
	 * method in local code.
	 * 
	 * @param segmentsAndBoundaries
	 *            segmentsAndBoundaries
	 * @param pauseSymbol
	 *            pauseSymbol
	 * @return HalfPhoneTargetFeatureLister.createTargetsWithPauses(segmentsAndBoundaries, pauseSymbol)
	 */
	@Override
	protected List<Target> overridableCreateTargetsWithPauses(List<Element> segmentsAndBoundaries, String pauseSymbol) {
		return HalfPhoneTargetFeatureLister.createTargetsWithPauses(segmentsAndBoundaries, pauseSymbol);
	}

	/**
	 * Create the list of targets from the segments to be synthesized Prepend and append pauses if necessary
	 * 
	 * @param segmentsAndBoundaries
	 *            a list of MaryXML phone and boundary elements
	 * @param silenceSymbol
	 *            silenceSymbol
	 * @return a list of Target objects
	 */
	public static List<Target> createTargetsWithPauses(List<Element> segmentsAndBoundaries, String silenceSymbol) {
		List<Target> targets = new ArrayList<Target>();
		if (segmentsAndBoundaries.size() == 0)
			return targets;
		Element last = segmentsAndBoundaries.get(segmentsAndBoundaries.size() - 1);
		if (!last.getTagName().equals(MaryXML.BOUNDARY)) {
			Element finalPause = MaryXML.createElement(last.getOwnerDocument(), MaryXML.BOUNDARY);
			Element token = (Element) MaryDomUtils.getAncestor(last, MaryXML.TOKEN);
			Element parent = (Element) token.getParentNode();
			parent.appendChild(finalPause);
			segmentsAndBoundaries.add(finalPause);
		}
		for (Element sOrB : segmentsAndBoundaries) {
			String phone = UnitSelector.getPhoneSymbol(sOrB);
			targets.add(new HalfPhoneTarget(phone + "_L", sOrB, true));
			targets.add(new HalfPhoneTarget(phone + "_R", sOrB, false));
		}
		return targets;
	}

}
