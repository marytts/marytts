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

import java.util.ArrayList;
import java.util.List;

import marytts.unitselection.select.DiphoneTarget;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.ViterbiCandidate;

import org.apache.log4j.Logger;


public class DiphoneUnitDatabase extends UnitDatabase {

    public DiphoneUnitDatabase()
    {
        super();
        logger = Logger.getLogger("DiphoneUnitDatabase");
    }

    /**
     * Preselect a set of candidates that could be used to realise the
     * given target.
     * @param target a Target object representing an optimal unit
     * @return an array of ViterbiCandidates, each containing the (same) target and a (different) Unit object
     */
    public ViterbiCandidate[] getCandidates(Target target)
    {
        if (!(target instanceof DiphoneTarget))
            return super.getCandidates(target);
        // Basic idea: get the candidates for each half phone separately,
        // but retain only those that are part of a suitable diphone
        DiphoneTarget diphoneTarget = (DiphoneTarget) target;
        HalfPhoneTarget left = diphoneTarget.getLeft();
        HalfPhoneTarget right = diphoneTarget.getRight();
        String leftName = left.getName().substring(0, left.getName().lastIndexOf("_"));
        String rightName = right.getName().substring(0, right.getName().lastIndexOf("_"));
        int iPhoneme = targetCostFunction.getFeatureDefinition().getFeatureIndex("phone");
        byte bleftName = targetCostFunction.getFeatureDefinition().getFeatureValueAsByte(iPhoneme, leftName);
        byte brightName = targetCostFunction.getFeatureDefinition().getFeatureValueAsByte(iPhoneme, rightName);

        List candidates = new ArrayList();
        // Pre-select candidates for the left half, but retain only
        // those that belong to appropriate diphones:
        int[] clist = (int[]) preselectionCART.interpret(left,backtrace);
        logger.debug("For target "+target+", selected " + clist.length + " units");

        // Now, clist is an array of halfphone unit indexes.
        for (int i = 0; i < clist.length; i++) {
            Unit unit = unitReader.getUnit(clist[i]);
            byte bunitName = targetCostFunction.getFeatureVector(unit).getByteFeature(iPhoneme);
            // force correct phone symbol:
            if (bunitName != bleftName) continue;
            int iRightNeighbour = clist[i]+1;
            if (iRightNeighbour < unitReader.getNumberOfUnits()) {
                Unit rightNeighbour = unitReader.getUnit(iRightNeighbour);
                byte brightUnitName = targetCostFunction.getFeatureVector(rightNeighbour).getByteFeature(iPhoneme);
                if (brightUnitName == brightName) {
                    // Found a diphone -- add it to candidates
                    DiphoneUnit diphoneUnit = new DiphoneUnit(unit, rightNeighbour);
                    ViterbiCandidate c = new ViterbiCandidate();
                    c.setTarget(diphoneTarget);
                    c.setUnit(diphoneUnit);
                    candidates.add(c);
                }
            }
        }
        // Pre-select candidates for the right half, but retain only
        // those that belong to appropriate diphones:
        clist = (int[]) preselectionCART.interpret(right,backtrace);
        logger.debug("For target "+target+", selected " + clist.length + " units");

        // Now, clist is an array of halfphone unit indexes.
        for (int i = 0; i < clist.length; i++) {
            Unit unit = unitReader.getUnit(clist[i]);
            String unitName = targetCostFunction.getFeature(unit, "phone");
            // force correct phone symbol:
            if (!unitName.equals(rightName)) continue;
            int iLeftNeighbour = clist[i]-1;
            if (iLeftNeighbour >= 0) {
                Unit leftNeighbour = unitReader.getUnit(iLeftNeighbour);
                String leftUnitName = targetCostFunction.getFeature(leftNeighbour, "phone");
                if (leftUnitName.equals(leftName)) {
                    // Found a diphone -- add it to candidates
                    DiphoneUnit diphoneUnit = new DiphoneUnit(leftNeighbour, unit);
                    ViterbiCandidate c = new ViterbiCandidate();
                    c.setTarget(diphoneTarget);
                    c.setUnit(diphoneUnit);
                    candidates.add(c);
                }
            }
        }
        logger.debug("Preselected "+candidates.size()+" diphone candidates for target "+target);
        return (ViterbiCandidate[]) candidates.toArray(new ViterbiCandidate[0]);
    }

}

