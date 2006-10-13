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

import java.util.LinkedHashMap;
 /**
  * Describes a Viterbi path.
  */
 public class ViterbiPath {
	private double score = 0;
	private ViterbiCandidate candidate = null;
	private LinkedHashMap f = null;
	private ViterbiPath previous = null;
	private ViterbiPath next = null;
	
	/**
	 * Get the score of this path
	 * @return the score
	 */
	public double getScore(){
	    return score;
	}
	
	/**
	 * Set the score of this path
	 * @param score the new score
	 */
	public void setScore(double score){
	    this.score = score;
	}
	
	/**
	 * Get the candidate of this path.
     * Each path leads to exactly one candidate.
	 * @return the candidate
	 */
	public ViterbiCandidate getCandidate(){
	    return candidate;
	}
	
	/**
	 * Set the candidate of this path.
     * Each path leads to exactly one candidate.
	 * @param candidate the new candidate
	 */
	public void setCandidate(ViterbiCandidate candidate){
	    this.candidate = candidate;
	}
	
	/**
	 * Get the next path
	 * @return the next path
	 */
	public ViterbiPath getNext(){
	    return next;
	}
	
	/**
	 * Set the next path
	 * @param next the next path
	 */
	public void setNext(ViterbiPath next){
	    this.next = next;
	}
	
	/** 
	 * Get the previous path
	 * @return the previous path
	 */
	public ViterbiPath getPrevious(){
	    return previous;
	}
	
	/**
	 * Set the previous path
	 * @param previous the previous path
	 */
	public void setPrevious(ViterbiPath previous){
	    this.previous = previous;
	}
	
	/**
	 * Sets a feature with the given name to the given value.
	 *
	 * @param name the name of the feature
	 * @param value the new value for the feature
	 */
	public void setFeature(String name, Object value) {
	    if (f == null) {
		f = new LinkedHashMap();
	    }
	    f.put(name, value);
	}
	
	/**
	 * Retrieves a feature.
	 *
	 * @param name the name of the feature
	 * 
	 * @return the feature
	 */
	public Object getFeature(String name) {
	    Object value = null;
	    if (f != null) {
		value = f.get(name);
	    }
	    return value;
	}
	
	/**
	 * Determines if the feature with the given name
	 * exists.
	 *
	 * @param name the feature to look for
	 *
	 * @return <code>true</code> if the feature is present;
	 * 	otherwise <code>false</code>.
	 */
	public boolean isPresent(String name) {
	    if (f == null) {
		return false;
	    } else {
		return getFeature(name) != null;
	    }
	}
	
	/**
	 * Converts this object to a string.
	 *
	 * @return the string form of this object
	 */
	public String toString() {
	    return "ViterbiPath score " + score + " leads to candidate unit " + candidate.getUnit();
	}
 }
   