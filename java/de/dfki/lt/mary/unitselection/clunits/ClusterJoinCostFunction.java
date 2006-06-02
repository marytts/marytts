/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package de.dfki.lt.mary.unitselection.clunits;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.JoinCostFunction;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.viterbi.Cost;
import de.dfki.lt.mary.unitselection.viterbi.ViterbiPath;
import de.dfki.lt.mary.util.MaryUtils;

/**
 * A join cost function for evaluating the goodness-of-fit of 
 * a given pair of left and right unit.
 * @author Marc Schr&ouml;der
 *
 */
public class ClusterJoinCostFunction implements JoinCostFunction
{
    private ClusterUnitDatabase unitDB;
    private Logger logger;
    
    public ClusterJoinCostFunction()
    {
        this.logger = Logger.getLogger("ClusterJoinCostFunction");
    }
    public void setDatabase(ClusterUnitDatabase db){
        unitDB = db;
    }
    
    /**
     * Compute the goodness-of-fit of joining two units. 
     * @param left the proposed left unit
     * @param right the proposed right unit
     * @return a non-negative integer; smaller values mean better fit, i.e. smaller cost.
     */
    public int cost(Unit left, Unit right, ViterbiPath newPath){
        ClusterUnit u0 = (ClusterUnit)left;
        ClusterUnit u1 = (ClusterUnit)right;
        int cost;
        //logger.debug("Join costs for left "+left+" right "+right);
        if (unitDB.getOptimalCoupling() == 1) {
            //logger.debug("Optimal coupling 1");
    		Cost oCost = getOptimalCouple(u0, u1);
    		if (oCost.u0Move != 0) {
    		    newPath.setFeature("unit_prev_move", 
    				               new Integer(oCost.u0Move));
                //logger.debug("unit "+u0+" shifted end by "+oCost.u0Move);
    		}
    		if (oCost.u1Move != 0) { 
    		    newPath.setFeature("unit_this_move", 
    				               new Integer(oCost.u1Move));
                //logger.debug("unit "+u1+" shifted start by "+oCost.u1Move);
    		}
    		cost = oCost.cost;
    	} else if (unitDB.getOptimalCoupling() == 2) {
            //logger.debug("Optimal coupling 2");
            cost = getOptimalCoupleFrame(u0, u1);
    	} else {
            //logger.debug("No optimal coupling");
    		cost = 0;
    	}
    
        return cost;
    }

    /**
     * Find the optimal coupling frame for a pair of units.
     *
     * @param u0  first unit to try
     * @param u1  second unit to try
     *
     * @return the cost for this coupling, including the best coupling frame
     */
    private Cost getOptimalCouple(ClusterUnit u0, ClusterUnit u1) {
	
        int a,b;
        int i, fcount;
        int best_u0, best_u1_prev;
        int dist, best_val; 
        Cost cost = new Cost();
        ClusterUnit u1_prev = (ClusterUnit) u1.getPrevious();
        //if (u1_prev == null) return cost;
	
        // If u0 precedes u1, the cost is 0, and we're finished.
        if (u1_prev != null && u1_prev.equals(u0)) {
            return cost;
        }
	
        // If u1 does not have a previous unit, or that previous
        // unit does not belong to the same unit type, the optimal
        // couple frame must be found between u0 and u1.
        if (u1_prev == null || u0.type != u1_prev.type ) {
            cost.cost = 10 * getOptimalCoupleFrame(u0, u1);
            return cost;
        }
     	
        // If u1 has a valid previous unit, try to find the optimal
        // couple point between u0 and that previous unit, u1_prev.
        
        //logger.debug("Searching for optimal couple frame between "+u0+" and left neighbor of "+u1);
        
        // Find out which of u1_prev and u0 is shorter.
        // In both units, we plan to start from one third of the unit length,
        // and to compare frame coupling frame by frame until the end of the
        // shorter unit is reached.
	
        int u0_length = u0.getNumberOfFrames();
        int u1_prev_length = u1_prev.getNumberOfFrames();
        double fractionLookback = 8./10;
        int u0_start = (int) (u0_length * fractionLookback);
        int u1_prev_start = (int) (u1_prev_length * fractionLookback);
	
        if (u0_length < u1_prev_length) {
            fcount = u0_length - u0_start;
            // We could now shift the starting point for coupling in the longer unit
            // so that the distance from the end is the same in both units:
            //u1_prev_start = u1_prev_length - fcount;
        } 
        else {
            fcount = u1_prev_length - u1_prev_start;
            // We could now shift the starting point for coupling in the longer unit
            // so that the distance from the end is the same in both units:
            // u0_start = u0_length - fcount;
        }
	
        // Now go through the two units, and search for the frame pair where
        // the acoustic distance is smallest.
        best_u0 = u0_length;
        best_u1_prev = u1_prev_length;
        best_val = Integer.MAX_VALUE;
	
        for (i = 0; i < fcount; ++i) {
            a = u0_start + i;
            b = u1_prev_start + i;
            int frameDist = getFrameDistance(u0.getJoinCostFeatureVector(a),
                    u1_prev.getJoinCostFeatureVector(b),
                    unitDB.getJoinWeights());
            int f0Dist = Math.abs( unitDB.getAudioFrames().getFrameSize(a) - 
				            unitDB.getAudioFrames().getFrameSize(b)) * 
				            5000;//unitDB.getContinuityWeight();
            //logger.debug("Frames "+a+" and "+b+": frameDist "+frameDist+", f0Dist "+f0Dist);
            dist = frameDist + f0Dist;
            if (dist < best_val) {
                best_val = dist;
                best_u0 = a;
                best_u1_prev = b;
            }
        }
        //logger.debug("Best frame distance: "+best_val+" between "+best_u0+" and "+best_u1_prev);
	
        // u0Move is the change end for u0
        // u1Move is the change to the start for u1 -- as this is negative,
        // it will mean looking into the frames for the previous unit of u1
        cost.u0Move = -(u0_length - best_u0);
        cost.u1Move = -(u1_prev_length - best_u1_prev);
        cost.cost = best_val;
        return cost;
    }
    
    
    /** 
     * Returns the distance between the successive potential
     * frames.
     *
     * @param u0 the first unit to try
     * @param u1 the second unit to try
     *
     * @return the distance between the two units
     */
    int getOptimalCoupleFrame(ClusterUnit u0, ClusterUnit u1)
    {
        Frame u0JoinCostFeatures;
    	if (u0.getNext() != null) {
    	    u0JoinCostFeatures = u0.getNext().getJoinCostFeatureVector(0);
    	} else if (u0.getNumberOfFrames() > 0) {
    	    u0JoinCostFeatures = u0.getJoinCostFeatureVector(u0.getNumberOfFrames()-1);
    	} else {
    	    // we have a zero-length unit -- this fits badly in any case
            return 65535; // very bad
        }
    	Frame u1JoinCostFeatures = u1.getJoinCostFeatureVector(0);
        int frameDist = getFrameDistance(u0JoinCostFeatures, u1JoinCostFeatures,
                unitDB.getJoinWeights());
        // For f0 join costs, compute median f0 over several periods:
        int nPeriods = 5;
        int[] u0Periods = new int[Math.min(nPeriods, u0.getNumberOfFrames())];
        int[] u1Periods = new int[Math.min(nPeriods, u1.getNumberOfFrames())];
        int u0N = u0.getNumberOfFrames()-1;
        for (int i=0; i<u0Periods.length; i++) {
            u0Periods[i] = u0.getAudioFrameSize(u0N-i);
        }
        for (int i=0; i<u1Periods.length; i++) {
            u1Periods[i] = u1.getAudioFrameSize(i);
        }
        double u0med = MaryUtils.median(u0Periods);
        double u1med = MaryUtils.median(u1Periods);
        // f0 delta is the relation of the frequencies: 
        // f0 delta = f0_u0 / f0_u1 - 1 = u0med / u1med - 1
        double percentDist = Math.abs(u0med/u1med - 1);
        // Low penalty for f0 distances below 10%; high penalty for higher f0 distances:
        int f0Dist;
        if (percentDist < .2) f0Dist = 0;
        else f0Dist = 50000;
        //logger.debug("Units "+u0+" and "+u1+": frameDist "+frameDist+", f0Dist "+f0Dist);
        return 2*(frameDist + f0Dist);
    }
    
    /**
    * Get the 'distance' between the frames a and b.
    *
    * @param a first frame
    * @param b second frame
    * @param joinWeights the weights used in comparison
    *
    * @return the distance between the frames, as a non-negative integer. Smaller
    * values mean better fit.
    */
   public int getFrameDistance(Frame a, Frame b, int[] joinWeights)
   {
    	int r, i;
    	
    	short[] bv = b.getCoefficients();
    	short[] av = a.getCoefficients();
    	
    	for (r = 0, i = 0; i < av.length; i++) {
    	    	int diff = av[i] - bv[i];
    	    	r += Math.abs(diff) * joinWeights[i] / 65536;
    	}
    	return r;
   }
    
}
