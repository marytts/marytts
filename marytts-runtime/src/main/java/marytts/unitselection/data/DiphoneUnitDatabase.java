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

import gnu.trove.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

import marytts.features.FeatureVector;
import marytts.unitselection.select.DiphoneTarget;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.viterbi.ViterbiCandidate;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;

import org.w3c.dom.Element;

public class DiphoneUnitDatabase extends UnitDatabase {

	public DiphoneUnitDatabase() {
		super();
		logger = MaryUtils.getLogger("DiphoneUnitDatabase");
	}

	/**
	 * Preselect a set of candidates that could be used to realise the given target.
	 * 
	 * @param target
	 *            a Target object representing an optimal unit
	 * @return an <span style="color:red;">unsorted</span> ArrayList of ViterbiCandidates, each containing the (same) target and a
	 *         (different) Unit object
	 */
	@Override
	public List<ViterbiCandidate> getCandidates(Target target) {
		if (!(target instanceof DiphoneTarget))
			return super.getCandidates(target);
		// Basic idea: get the candidates for each half phone separately,
		// but retain only those that are part of a suitable diphone
		DiphoneTarget diphoneTarget = (DiphoneTarget) target;
		HalfPhoneTarget left = diphoneTarget.left;
		HalfPhoneTarget right = diphoneTarget.right;

		// BEGIN blacklisting
		// The point of this is to get the value of the "blacklist" attribute in the first child element of the MaryXML
		// and store it in the blacklist String variable.
		// This code seems rather inelegant; perhaps there is a better way to access the MaryXML from this method?
		String blacklist = "";
		String unitBasename = "This must never be null or the empty string!"; // otherwise candidate selection fails!
		Element targetElement = left.getMaryxmlElement();
		if (targetElement == null) {
			targetElement = right.getMaryxmlElement();
		}
		blacklist = DomUtils.getAttributeFromClosestAncestorOfAnyKind(targetElement, "blacklist");
		// END blacklisting

		// TODO shouldn't leftName and rightName just call appropriate methods of DiphoneTarget?
		String leftName = left.getName().substring(0, left.getName().lastIndexOf("_"));
		String rightName = right.getName().substring(0, right.getName().lastIndexOf("_"));
		int iPhoneme = targetCostFunction.getFeatureDefinition().getFeatureIndex("phone");
		byte bleftName = targetCostFunction.getFeatureDefinition().getFeatureValueAsByte(iPhoneme, leftName);
		byte brightName = targetCostFunction.getFeatureDefinition().getFeatureValueAsByte(iPhoneme, rightName);
		FeatureVector[] fvs = targetCostFunction.getFeatureVectors();

		// HashSet<DiphoneUnit> candidateUnitSet = new HashSet<DiphoneUnit>();
		TIntHashSet candidateUnitSet = new TIntHashSet();

		// Pre-select candidates for the left half, but retain only
		// those that belong to appropriate diphones:
		int[] clist = (int[]) preselectionCART.interpret(left, backtrace);
		logger.debug("For target " + target + ", selected " + clist.length + " units");

		// Now, clist is an array of halfphone unit indexes.
		for (int i = 0; i < clist.length; i++) {
			Unit unit = unitReader.units[clist[i]];
			FeatureVector fv = fvs != null ? fvs[unit.index] : targetCostFunction.getFeatureVector(unit);
			byte bunitName = fv.byteValuedDiscreteFeatures[iPhoneme];
			// force correct phone symbol:
			if (bunitName != bleftName)
				continue;
			int iRightNeighbour = clist[i] + 1;
			if (iRightNeighbour < numUnits) {
				Unit rightNeighbour = unitReader.units[iRightNeighbour];
				FeatureVector rfv = fvs != null ? fvs[iRightNeighbour] : targetCostFunction.getFeatureVector(rightNeighbour);
				byte brightUnitName = rfv.byteValuedDiscreteFeatures[iPhoneme];
				if (brightUnitName == brightName) {
					// Found a diphone -- add it to candidates
					// DiphoneUnit diphoneUnit = new DiphoneUnit(unit, rightNeighbour);
					// candidateUnitSet.add(diphoneUnit);
					candidateUnitSet.add(unit.index);
				}
			}
		}
		// Pre-select candidates for the right half, but retain only
		// those that belong to appropriate diphones:
		clist = (int[]) preselectionCART.interpret(right, backtrace);
		logger.debug("For target " + target + ", selected " + clist.length + " units");

		// Now, clist is an array of halfphone unit indexes.
		for (int i = 0; i < clist.length; i++) {
			Unit unit = unitReader.units[clist[i]];
			FeatureVector fv = fvs != null ? fvs[unit.index] : targetCostFunction.getFeatureVector(unit);
			byte bunitName = fv.byteValuedDiscreteFeatures[iPhoneme];
			// force correct phone symbol:
			if (bunitName != brightName)
				continue;
			int iLeftNeighbour = clist[i] - 1;
			if (iLeftNeighbour >= 0) {
				Unit leftNeighbour = unitReader.units[iLeftNeighbour];
				FeatureVector lfv = fvs != null ? fvs[iLeftNeighbour] : targetCostFunction.getFeatureVector(leftNeighbour);
				byte bleftUnitName = lfv.byteValuedDiscreteFeatures[iPhoneme];
				if (bleftUnitName == bleftName) {
					// Found a diphone -- add it to candidates
					// DiphoneUnit diphoneUnit = new DiphoneUnit(leftNeighbour, unit);
					// candidateUnitSet.add(diphoneUnit);
					candidateUnitSet.add(leftNeighbour.index);
				}
			}
		}

		// now create ArrayList of ViterbiCandidates from the candidateUnitSet, blacklisting along the way:
		ArrayList<ViterbiCandidate> candidates = new ArrayList<ViterbiCandidate>(candidateUnitSet.size());
		for (int leftIndex : candidateUnitSet.toArray()) {
			DiphoneUnit diphoneUnit = new DiphoneUnit(unitReader.units[leftIndex], unitReader.units[leftIndex + 1]);
			ViterbiCandidate candidate = new ViterbiCandidate(diphoneTarget, diphoneUnit, targetCostFunction);
			// Blacklisting:
			if (blacklist.equals("")) { // no blacklist
				candidates.add(candidate);
			} else { // maybe exclude candidate
				unitBasename = getFilename(diphoneUnit);
				if (!blacklist.contains(unitBasename)) {
					candidates.add(candidate);
				}
			}
		}

		logger.debug("Preselected " + candidateUnitSet.size() + " diphone candidates for target " + target);
		return candidates;
	}

}
