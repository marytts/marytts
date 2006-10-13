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
package de.dfki.lt.mary.unitselection.viterbi;

import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.TargetCostFunction;
import de.dfki.lt.mary.unitselection.Unit;
 /**
  * Represents a candidate for the Viterbi algorthm.
  * Each candidate knows about its next candidate, i.e. they can form
  * a queue.
  */
public class ViterbiCandidate {
	private Unit unit;
    private ViterbiPath bestPath = null;
	private Target target = null;
	private ViterbiCandidate next = null;
	private double targetCost = -1;
	
	/**
	 * Calculates and returns the target cost for this candidate
	 * @param tcf the target cost function 
	 * @return the target cost
	 */
	public double getTargetCost(TargetCostFunction tcf){
	    if (targetCost == -1){
	        targetCost = tcf.cost(target,unit);
	        //System.out.println("Candidate with target "+target.getName()
	          //      	+": Set target cost to "+targetCost);
	    }
	    return targetCost;
	}
	/**
	 * Gets the next candidate in the queue
	 * @return the next candidate
	 */
	public ViterbiCandidate getNext(){
	    return next;
	}
	
	/**
	 * Sets the next candidate in the queue
	 * @param next the next candidate
	 */
	public void setNext(ViterbiCandidate next){
	    this.next = next;
	}
	
	/**
	 * Gets the target of this candidate
	 * @return the target
	 */
	public Target getTarget(){
	    return target;
	}
	
	/**
	 * Sets the target of this candidate
	 * @param target the new target
	 */
	public void setTarget(Target target){
	    this.target = target;
	}
	
	/**
	 * Gets the index of this 
	 * @return the unit index
	 */
	public Unit getUnit(){
	    return unit;
	}
	
	/**
	 * Sets the currently best path leading to this candidate.
     * Each path leads to exactly one candidate; in the candidate,
     * we only remember the best path leading to it.
	 * @param bestPath
	 */
	public void setBestPath(ViterbiPath bestPath){
	    this.bestPath = bestPath;
	}
	
	/**
	 * Gets the best path leading to this candidate
	 * @return the best path, or null
	 */
	public ViterbiPath getBestPath(){
	    return bestPath;
	}
	
	/**
	 * Sets the candidate unit represented by this candidate.
	 * 
	 * @param unit the unit
	 */
	public void setUnit(Unit unit) {
	    this.unit = unit;
	}
	
	/**
	 * Converts this object to a string.
	 *
	 * @return the string form of this object
	 */
	public String toString() {
	    return "ViterbiCandidate: target "+ target + ", unit " + unit + (bestPath != null ? ", best path score "+bestPath.getScore() : ", no best path");
	}
 }
 