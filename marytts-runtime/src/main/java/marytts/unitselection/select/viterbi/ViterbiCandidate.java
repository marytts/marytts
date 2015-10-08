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
package marytts.unitselection.select.viterbi;

import marytts.unitselection.data.Unit;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.TargetCostFunction;

/**
 * Represents a candidate for the Viterbi algorthm. Each candidate knows about its next candidate, i.e. they can form a queue.
 */
public class ViterbiCandidate implements Comparable<ViterbiCandidate> {
	final Target target;
	final Unit unit;
	final double targetCost;
	ViterbiPath bestPath = null;
	ViterbiCandidate next = null;

	public ViterbiCandidate(Target target, Unit unit, TargetCostFunction tcf) {
		this.target = target;
		this.unit = unit;
		this.targetCost = tcf.cost(target, unit);
	}

	/**
	 * Calculates and returns the target cost for this candidate
	 * 
	 * @return the target cost
	 */
	public double getTargetCost() {
		return targetCost;
	}

	/**
	 * Gets the next candidate in the queue
	 * 
	 * @return the next candidate
	 */
	public ViterbiCandidate getNext() {
		return next;
	}

	/**
	 * Sets the next candidate in the queue
	 * 
	 * @param next
	 *            the next candidate
	 */
	public void setNext(ViterbiCandidate next) {
		this.next = next;
	}

	/**
	 * Gets the target of this candidate
	 * 
	 * @return the target
	 */
	public Target getTarget() {
		return target;
	}

	/**
	 * Gets the index of this
	 * 
	 * @return the unit index
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * Sets the currently best path leading to this candidate. Each path leads to exactly one candidate; in the candidate, we only
	 * remember the best path leading to it.
	 * 
	 * @param bestPath
	 *            bestPath
	 */
	public void setBestPath(ViterbiPath bestPath) {
		this.bestPath = bestPath;
	}

	/**
	 * Gets the best path leading to this candidate
	 * 
	 * @return the best path, or null
	 */
	public ViterbiPath getBestPath() {
		return bestPath;
	}

	/**
	 * Converts this object to a string.
	 *
	 * @return the string form of this object
	 */
	public String toString() {
		return "ViterbiCandidate: target " + target + ", unit " + unit
				+ (bestPath != null ? ", best path score " + bestPath.score : ", no best path");
	}

	/**
	 * Compare two candidates so that the one with the smaller target cost is considered smaller.
	 */
	public int compareTo(ViterbiCandidate o) {
		if (targetCost < o.targetCost) {
			return -1;
		} else if (targetCost > o.targetCost) {
			return 1;
		}
		return 0;
	}
}
