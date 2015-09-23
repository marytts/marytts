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

import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.unitselection.data.UnitDatabase;
import marytts.unitselection.select.viterbi.Viterbi;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Selects the units for an utterance
 * 
 * @author Marc Schr&ouml;der
 *
 */
public class UnitSelector {
	protected UnitDatabase database;
	protected Logger logger;
	protected float targetCostWeight;
	protected float sCostWeight = -1;
	protected int beamSize;

	/**
	 * Initialise the unit selector. Need to call load() separately.
	 * 
	 * @see #load(UnitDatabase unitDatabase, float targetCostWeight, int beamSize)
	 * @throws Exception
	 *             Exception
	 */
	public UnitSelector() throws Exception {
		logger = MaryUtils.getLogger(this.getClass());
	}

	public void load(UnitDatabase unitDatabase, float targetCostWeight, int beamSize) {
		this.database = unitDatabase;
		this.targetCostWeight = targetCostWeight;
		this.beamSize = beamSize;
	}

	public void load(UnitDatabase unitDatabase, float targetCostWeight, float sCostWeight, int beamSize) {
		this.database = unitDatabase;
		this.targetCostWeight = targetCostWeight;
		this.sCostWeight = sCostWeight;
		this.beamSize = beamSize;
	}

	/**
	 * Select the units for the targets in the given list of tokens and boundaries. Collect them in a list and return it.
	 * 
	 * @param tokensAndBoundaries
	 *            the token and boundary MaryXML elements representing an utterance.
	 * @param voice
	 *            the voice with which to synthesize
	 * @return a list of SelectedUnit objects
	 * @throws SynthesisException
	 *             if no path for generating the target utterance could be found
	 */
	public List<SelectedUnit> selectUnits(List<Element> tokensAndBoundaries, marytts.modules.synthesis.Voice voice)
			throws SynthesisException {
		long time = System.currentTimeMillis();

		List<Element> segmentsAndBoundaries = new ArrayList<Element>();
		for (Element tOrB : tokensAndBoundaries) {
			if (tOrB.getTagName().equals(MaryXML.BOUNDARY)) {
				segmentsAndBoundaries.add(tOrB);
			} else {
				assert tOrB.getTagName().equals(MaryXML.TOKEN) : "Expected token, got " + tOrB.getTagName();
				NodeList segs = tOrB.getElementsByTagName(MaryXML.PHONE);
				for (int i = 0, max = segs.getLength(); i < max; i++) {
					segmentsAndBoundaries.add((Element) segs.item(i));
				}
			}
		}

		List<Target> targets = createTargets(segmentsAndBoundaries);
		// compute target features for each target in the chain
		TargetCostFunction tcf = database.getTargetCostFunction();
		for (Target target : targets) {
			tcf.computeTargetFeatures(target);
		}

		Viterbi viterbi;
		// Select the best candidates using Viterbi and the join cost function.
		if (sCostWeight < 0) {
			viterbi = new Viterbi(targets, database, targetCostWeight, beamSize);
		} else {
			viterbi = new Viterbi(targets, database, targetCostWeight, sCostWeight, beamSize);
		}

		viterbi.apply();
		List<SelectedUnit> selectedUnits = viterbi.getSelectedUnits();
		// If you can not associate the candidate units in the best path
		// with the items in the segment relation, there is no best path
		if (selectedUnits == null) {
			throw new IllegalStateException("Viterbi: can't find path");
		}
		long newtime = System.currentTimeMillis() - time;
		logger.debug("Selection took " + newtime + " milliseconds");
		return selectedUnits;
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
			targets.add(new Target(phone, sOrB));
		}
		return targets;
	}

	public static String getPhoneSymbol(Element segmentOrBoundary) {
		String phone;
		if (segmentOrBoundary.getTagName().equals(MaryXML.PHONE)) {
			phone = segmentOrBoundary.getAttribute("p");
		} else {
			assert segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY) : "Expected boundary element, but got "
					+ segmentOrBoundary.getTagName();
			// TODO: how can we know the silence symbol here?
			phone = "_";
		}
		return phone;
	}

}
