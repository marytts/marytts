/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.unitselection;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.viterbi.ViterbiCandidate;

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
        int iPhoneme = targetCostFunction.getFeatureDefinition().getFeatureIndex("mary_phoneme");
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
            // force correct phoneme symbol:
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
            String unitName = targetCostFunction.getFeature(unit, "mary_phoneme");
            // force correct phoneme symbol:
            if (!unitName.equals(rightName)) continue;
            int iLeftNeighbour = clist[i]-1;
            if (iLeftNeighbour >= 0) {
                Unit leftNeighbour = unitReader.getUnit(iLeftNeighbour);
                String leftUnitName = targetCostFunction.getFeature(leftNeighbour, "mary_phoneme");
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
